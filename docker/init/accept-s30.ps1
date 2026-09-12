# S30 acceptance script (repeatable; reused by S50 regression)
# Prints explicit PASS/FAIL verdicts per item and a final summary.
# NOTE: keep this file ASCII-only (console encoding lesson, STATE risk #16)

$pass = 0
$fail = 0
function Verdict([string]$name, [bool]$ok, [string]$evidence) {
    if ($ok) {
        $script:pass++
        Write-Output ("[PASS] {0}  |  {1}" -f $name, $evidence)
    } else {
        $script:fail++
        Write-Output ("[FAIL] {0}  |  {1}" -f $name, $evidence)
    }
}

Write-Output "===== S30 ACCEPTANCE START ====="

# --- baseline counts ---
$m0 = (docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('inspection').alarm.countDocuments({})" 2>&1 | Select-Object -Last 1).Trim()
$e0 = (docker exec elasticsearch curl -s "http://localhost:9200/inspection_alarm_v1/_count" 2>&1 | ConvertFrom-Json).count
Write-Output ("baseline: mongo_alarms={0} es_alarms={1}" -f $m0, $e0)

# --- trigger alarms ---
docker exec nginx wget -qO- --post-data="" "http://dog-sim-1:8080/sim/fault?type=OVERHEAT" 2>&1 | Out-Null
docker exec nginx wget -qO- --post-data="" "http://uav-sim-2:8080/sim/fault?type=COMM_OFFLINE" 2>&1 | Out-Null
Start-Sleep -Seconds 25

# --- 1. Mongo alarm docs (alarm three-write step 2) ---
$m1 = (docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('inspection').alarm.countDocuments({})" 2>&1 | Select-Object -Last 1).Trim()
Verdict "1. alarm -> MongoDB" ([int]$m1 -gt [int]$m0) ("count {0} -> {1}" -f $m0, $m1)

# --- 2. ES index copy (alarm three-write step 3) ---
$e1 = (docker exec elasticsearch curl -s "http://localhost:9200/inspection_alarm_v1/_count" 2>&1 | ConvertFrom-Json).count
Verdict "2. alarm -> Elasticsearch" ([int]$e1 -gt [int]$e0) ("count {0} -> {1}" -f $e0, $e1)

# --- 3. HDFS evidence images (alarm three-write step 1) ---
$hdfs = docker exec namenode bash -c "hdfs dfs -ls -R /inspection 2>/dev/null" 2>&1
Verdict "3. evidence image -> HDFS" ($hdfs -match "\.png" -or $hdfs -match "\.txt") ("files: " + (($hdfs | Select-String "^-").Count))

# --- 4. offline detection ---
$uav2 = (docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('inspection').device.findOne({_id:'UAV-002'},{_id:1,status:1}).status" 2>&1 | Select-Object -Last 1).Trim()
Verdict "4. offline detection (UAV-002)" ($uav2 -eq "OFFLINE") ("status={0}" -f $uav2)

# --- 5. idempotent replay ---
$mBefore = (docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('inspection').alarm.countDocuments({})" 2>&1 | Select-Object -Last 1).Trim()
$one = (docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('inspection').alarm.findOne({},{_id:1})._id" 2>&1 | Select-Object -Last 1).Trim()
$payload = '{"alarmId":"' + $one + '","deviceId":"UAV-001","deviceType":"UAV","alarmType":"PERIMETER_BREACH","level":"CRITICAL","description":"replay","lng":116.397,"lat":39.909,"occurredTime":1789173400000}'
1..2 | ForEach-Object {
    $payload | docker exec -i kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic inspection.alarm 2>&1 | Out-Null
    Start-Sleep -Seconds 4
}
Start-Sleep -Seconds 8
$mAfter = (docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('inspection').alarm.countDocuments({})" 2>&1 | Select-Object -Last 1).Trim()
Verdict "5. idempotent replay" ([int]$mAfter -eq [int]$mBefore) ("count {0} -> {1}" -f $mBefore, $mAfter)

# --- 6. dead letter queue ---
"this-is-not-json" | docker exec -i kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic inspection.alarm 2>&1 | Out-Null
Start-Sleep -Seconds 20
$dlq = docker exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic inspection.dlq --from-beginning --max-messages 5 --timeout-ms 8000 2>&1
Verdict "6. dead letter queue" ($dlq -match "not-json" -or $dlq -match "this-is") ("dlq messages: " + (($dlq | Select-String "this-is|not-json" | Measure-Object).Count))

Write-Output "===== S30 ACCEPTANCE SUMMARY: PASS=$pass FAIL=$fail ====="
if ($fail -gt 0) { exit 1 }
