# S86 (course task item 3): concurrent reporting stress test — 4 devices (2 UAV + 2 ROBOT_DOG)
# simultaneously reporting telemetry at ~12.5 msg/s each for 60s => ~3000 msgs total (~50 msg/s).
# Measures: produced / drained / lost / per-device split / consumer LAG peak + recall / batch drain time.
# ASCII-only (PS 5.1 lesson, STATE risk #16).
$base = "http://127.0.0.1:8080"
$perDevice = 750
$intervalMs = 80          # 12.5 msg/s per device
$pass = 0; $fail = 0

function V([string]$id, [string]$name, [bool]$ok, [string]$ev) {
    Write-Output ("[{0}] {1} {2} | {3}" -f $(if($ok){"PASS"}else{"FAIL"}), $id, $name, $ev)
    if ($ok) { $script:pass++ } else { $script:fail++ }
}

function Mongo([string]$js) {
    $r = docker exec mongodb mongosh --quiet --eval $js 2>&1 | Where-Object { $_ -notmatch '^\s*rs0 ' } | Select-Object -Last 1
    if ($r -is [string]) { return $r.Trim() }
    return ""
}

function Lag() {
    $out = docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group biz-storage-consumer 2>&1
    $sum = 0
    $out | Select-Object -Skip 1 | ForEach-Object {
        $cols = ($_ -split '\s+') | Where-Object { $_ -ne '' }
        if ($cols.Count -ge 6 -and $cols[5] -match '^\d+$') { $sum += [int]$cols[5] }
    }
    return $sum
}

Write-Output "===== S86 CONCURRENT REPORTING STRESS TEST ====="

# --- pre-step: all devices online ---
$devs = ((Invoke-WebRequest -Uri "$base/api/devices" -UseBasicParsing -TimeoutSec 15).Content) | ConvertFrom-Json
foreach ($d in $devs) {
    if ($d.status -ne 'ONLINE') {
        Invoke-WebRequest -Uri "$base/api/devices/$($d.deviceId)/online" -Method POST -UseBasicParsing -TimeoutSec 15 | Out-Null
        Write-Output ("pre-step: restore online -> " + $d.deviceId)
    }
}
$deadline = (Get-Date).AddSeconds(60)
while ((Get-Date) -lt $deadline) {
    $still = (((Invoke-WebRequest -Uri "$base/api/devices" -UseBasicParsing -TimeoutSec 15).Content) | ConvertFrom-Json) | Where-Object { $_.status -ne 'ONLINE' }
    if (@($still).Count -eq 0) { break }
    Start-Sleep -Seconds 5
}

$targets = @('UAV-001','UAV-002','ROBOT-001','ROBOT-002')
$testStart = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
# marker: injected telemetry carries a distinguishable sensor value so exact injected volume can be counted
#   UAV -> altitude = 999.5 ; ROBOT_DOG -> irMaxTemp = 999.5  (natural values never reach these)
$c0 = 0
$mark0 = 0
foreach ($dev in $targets) {
    if ($dev -like 'UAV*') {
        $mark0 += [int](Mongo ('db.getSiblingDB(''inspection'').device_status.countDocuments({deviceId:''' + $dev + ''',altitude:999.5})'))
    } else {
        $mark0 += [int](Mongo ('db.getSiblingDB(''inspection'').device_status.countDocuments({deviceId:''' + $dev + ''',irMaxTemp:999.5})'))
    }
}
Write-Output ("baseline marker docs=" + $mark0 + " testStartMs=" + $testStart)

# --- concurrent producers: one long-lived console-producer per device, paced line stream on stdin ---
$jobs = @()
foreach ($dev in $targets) {
    $topic = if ($dev -like 'UAV*') { 'uav.telemetry' } else { 'robot.telemetry' }
    $jobs += Start-Job -ArgumentList $dev, $topic, $perDevice, $intervalMs -ScriptBlock {
        param($dev, $topic, $count, $intervalMs)
        $lng = [math]::Round(116.3974 + (Get-Random -Minimum -20 -Maximum 20) / 100000.0, 6)
        # single producer process; generator streams paced lines into its stdin
        & {
            for ($i = 0; $i -lt $count; $i++) {
                $ts = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
                if ($dev -like 'UAV*') {
                    '{"deviceId":"' + $dev + '","lng":' + $lng + ',"lat":39.9092,"altitude":999.5,"speed":12,"battery":80,"ts":' + $ts + '}'
                } else {
                    '{"deviceId":"' + $dev + '","lng":' + $lng + ',"lat":39.9092,"irMaxTemp":999.5,"ambientTemp":28.0,"humidity":0.5,"smoke":0.02,"gas":0.01,"speed":3.5,"battery":80,"ts":' + $ts + '}'
                }
                Start-Sleep -Milliseconds $intervalMs
            }
        } | docker exec -i kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic $topic 2>&1 | Out-Null
    }
}
Write-Output ("started " + $jobs.Count + " concurrent producers (per-device " + $perDevice + " msgs, " + $intervalMs + "ms interval => ~" + [math]::Round(1000 / $intervalMs, 1) + " msg/s each)")
Start-Sleep -Seconds 30
$lagPeak = Lag
Write-Output ("mid-test LAG(biz-storage-consumer)=" + $lagPeak)
Wait-Job -Job $jobs -Timeout 240 | Out-Null
Remove-Job -Job $jobs -Force
Write-Output ("producers finished (intended=" + ($perDevice * $targets.Count) + ")")

# --- wait until injected markers all landed ---
$produced = 0
$sw = [System.Diagnostics.Stopwatch]::StartNew()
$deadline = (Get-Date).AddSeconds(180)
while ((Get-Date) -lt $deadline) {
    $produced = 0
    foreach ($dev in $targets) {
        if ($dev -like 'UAV*') {
            $produced += [int](Mongo ('db.getSiblingDB(''inspection'').device_status.countDocuments({deviceId:''' + $dev + ''',altitude:999.5})'))
        } else {
            $produced += [int](Mongo ('db.getSiblingDB(''inspection'').device_status.countDocuments({deviceId:''' + $dev + ''',irMaxTemp:999.5})'))
        }
    }
    if ($produced -ge $perDevice * $targets.Count) { break }
    Start-Sleep -Seconds 2
}
$sw.Stop()
$lagAfter = Lag
$injected = $produced - $mark0
$intended = $perDevice * $targets.Count
$lost = $intended - $injected

# --- per-device injected split ---
$splitParts = @()
foreach ($dev in $targets) {
    if ($dev -like 'UAV*') {
        $n = [int](Mongo ('db.getSiblingDB(''inspection'').device_status.countDocuments({deviceId:''' + $dev + ''',altitude:999.5})'))
    } else {
        $n = [int](Mongo ('db.getSiblingDB(''inspection'').device_status.countDocuments({deviceId:''' + $dev + ''',irMaxTemp:999.5})'))
    }
    $splitParts += ($dev + "=" + $n + "/" + $perDevice)
}
Write-Output ("per-device injected: " + ($splitParts -join " "))

V "PT008a" "concurrent reporting zero loss" ($lost -le 0) ("intended=" + $intended + " injected=" + $injected + " lost=" + $lost + " (" + ($targets.Count) + " devices concurrent)")
V "PT008b" "consumer LAG recalled to 0" ($lagAfter -eq 0) ("lagPeak=" + $lagPeak + " lagAfter=" + $lagAfter)
V "PT008c" "injection drains within budget" ($sw.Elapsed.TotalSeconds -lt 120) ("drain=" + [math]::Round($sw.Elapsed.TotalSeconds, 1) + "s for " + $injected + " msgs")

Write-Output ("===== S86 SUMMARY: PASS=" + $pass + " FAIL=" + $fail + " =====")
