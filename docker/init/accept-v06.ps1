# v0.6 acceptance (S62): TC021 nearest dispatch, review backfill, review image download, IT009, TC032 API.
# ASCII-only (PS 5.1 lesson, STATE risk #16).
$base = "http://127.0.0.1:8080"
$pass = 0; $fail = 0

function V([string]$id, [string]$name, [bool]$ok, [string]$ev) {
    Write-Output ("[{0}] {1} {2}  |  {3}" -f $(if($ok){"PASS"}else{"FAIL"}), $id, $name, $ev)
    if ($ok) { $script:pass++ } else { $script:fail++ }
}

function Req([string]$method, [string]$url, [string]$body) {
    try {
        $p = @{ Uri = $url; Method = $method; TimeoutSec = 20; UseBasicParsing = $true }
        if ($body) { $p.ContentType = "application/json; charset=utf-8"; $p.Body = $body }
        $r = Invoke-WebRequest @p
        return @{ code = [int]$r.StatusCode; body = $r.Content }
    } catch {
        $code = 0
        if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
        return @{ code = $code; body = "" }
    }
}

function Devs() {
    return (Req "GET" "$base/api/devices" $null).body | ConvertFrom-Json
}

function TaskByAlarm([string]$alarmId) {
    $list = (Req "GET" "$base/api/tasks?page=0&size=50" $null).body | ConvertFrom-Json
    return ($list.records | Where-Object { $_.alarmId -eq $alarmId } | Select-Object -First 1)
}

function Alarm([string]$alarmId) {
    $r = Req "GET" "$base/api/alarms/$alarmId" $null
    if ($r.code -eq 200) { return $r.body | ConvertFrom-Json }
    return $null
}

Write-Output "===== V0.6 ACCEPTANCE (S62) ====="

# --- pre-step: all devices online ---
$devs = Devs
foreach ($dv in $devs) {
    if ($dv.status -ne "ONLINE") {
        Req "POST" "$base/api/devices/$($dv.deviceId)/online" $null | Out-Null
        Write-Output ("pre-step: restore online -> " + $dv.deviceId)
    }
}
$deadline = (Get-Date).AddSeconds(90)
while ((Get-Date) -lt $deadline) {
    $still = (Devs) | Where-Object { $_.status -ne "ONLINE" }
    if ($still.Count -eq 0) { break }
    Start-Sleep -Seconds 5
}

# --- pre-step: wait robots idle (queue drained: no DISPATCHED/RUNNING robot tasks) ---
$deadline = (Get-Date).AddMinutes(5)
while ((Get-Date) -lt $deadline) {
    $list = (Req "GET" "$base/api/tasks?page=0&size=50" $null).body | ConvertFrom-Json
    $busy = $list.records | Where-Object { ($_.deviceId -match "ROBOT") -and ($_.status -eq "DISPATCHED" -or $_.status -eq "RUNNING") }
    if ($busy.Count -eq 0) { break }
    Write-Output ("pre-step: robots busy=" + $busy.Count + " waiting queue drain")
    Start-Sleep -Seconds 10
}

# --- V-1: device location populated (2dsphere data source) ---
$eval = 'db.getSiblingDB(''inspection'').device.countDocuments({location:{$exists:true}})'
$locCount = docker exec mongodb mongosh --quiet --eval $eval 2>&1 | Select-Object -Last 1
$locN = -1
if ($locCount -is [string]) { $locN = [int]($locCount.Trim()) }
V "V-1" "device.location populated" ($locN -ge 4) ("devices with location=" + $locN)

# --- V-2 TC021: inject alarm at ROBOT-001 exact position, expect dispatch to ROBOT-001 ---
$devs = Devs
$r1 = $devs | Where-Object { $_.deviceId -eq 'ROBOT-001' } | Select-Object -First 1
$aid1 = "v06-" + [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$p1 = '{"alarmId":"' + $aid1 + '","deviceId":"UAV-001","deviceType":"UAV","alarmType":"PERIMETER_BREACH","level":"CRITICAL","description":"v06 nearest-dispatch test","lng":' + $r1.lng + ',"lat":' + $r1.lat + ',"occurredTime":' + [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() + '}'
$p1 | docker exec -i kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic inspection.alarm 2>&1 | Out-Null
$t1 = $null
$deadline = (Get-Date).AddSeconds(30)
while ((Get-Date) -lt $deadline) {
    $t1 = TaskByAlarm $aid1
    if ($t1) { break }
    Start-Sleep -Seconds 2
}
V "V-2" "TC021 nearest dispatch (alarm at ROBOT-001 pos)" ($t1 -and $t1.deviceId -eq 'ROBOT-001') ("task=" + $(if($t1){$t1.taskId + " device=" + $t1.deviceId}else{"none"}))

# --- V-2b: inject alarm at ROBOT-002 exact position ---
$r2 = $devs | Where-Object { $_.deviceId -eq 'ROBOT-002' } | Select-Object -First 1
$aid2 = "v06-" + ([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() + 1)
$p2 = '{"alarmId":"' + $aid2 + '","deviceId":"UAV-002","deviceType":"UAV","alarmType":"INTRUSION","level":"CRITICAL","description":"v06 nearest-dispatch test b","lng":' + $r2.lng + ',"lat":' + $r2.lat + ',"occurredTime":' + [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() + '}'
$p2 | docker exec -i kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic inspection.alarm 2>&1 | Out-Null
$t2 = $null
$deadline = (Get-Date).AddSeconds(30)
while ((Get-Date) -lt $deadline) {
    $t2 = TaskByAlarm $aid2
    if ($t2) { break }
    Start-Sleep -Seconds 2
}
V "V-2b" "TC021 nearest dispatch (alarm at ROBOT-002 pos)" ($t2 -and $t2.deviceId -eq 'ROBOT-002') ("task=" + $(if($t2){$t2.taskId + " device=" + $t2.deviceId}else{"none"}))

# --- V-3: review backfill after DONE (poll task DONE, then poll alarm review - HDFS upload takes ~0.5s) ---
$deadline = (Get-Date).AddMinutes(3)
$done1 = $false
while ((Get-Date) -lt $deadline) {
    $t = TaskByAlarm $aid1
    if ($t -and $t.status -eq 'DONE') { $done1 = $true; break }
    Start-Sleep -Seconds 3
}
$a1 = $null
if ($done1) {
    $deadline = (Get-Date).AddSeconds(30)
    while ((Get-Date) -lt $deadline) {
        $a1 = Alarm $aid1
        if ($a1 -and $a1.review -and $a1.review.imagePath) { break }
        Start-Sleep -Seconds 2
    }
}
$ok3 = ($done1 -and $a1 -ne $null -and $a1.review -ne $null -and $a1.review.imagePath -ne $null -and ($a1.status -eq 'CONFIRMED' -or $a1.status -eq 'FALSE_ALARM'))
$ev3 = "taskDone=" + $done1 + " status=" + $(if($a1){$a1.status}else{"?"}) + " conclusion=" + $(if($a1 -and $a1.review){$a1.review.conclusion}else{"?"}) + " image=" + $(if($a1 -and $a1.review){$a1.review.imagePath}else{"?"})
V "V-3" "review backfill after DONE" $ok3 $ev3

# --- V-4: review image download (file-based, avoids binary string mangle) ---
$tmp = "$env:TEMP\v06-review.png"
Remove-Item $tmp -ErrorAction SilentlyContinue
$r = Req "GET" "$base/api/files/$aid1/review" $null
$bytes = 0
if ($r.code -eq 200) {
    try {
        Invoke-WebRequest -Uri "$base/api/files/$aid1/review" -OutFile $tmp -UseBasicParsing -TimeoutSec 20 | Out-Null
        if (Test-Path $tmp) { $bytes = (Get-Item $tmp).Length }
    } catch { $bytes = -1 }
}
V "V-4" "review image download" ($r.code -eq 200 -and $bytes -gt 0) ("code=" + $r.code + " bytes=" + $bytes)
Remove-Item $tmp -ErrorAction SilentlyContinue

# --- V-5: IT009 manual review (regression) ---
$r = Req "POST" "$base/api/alarms/pt3k-00002/review" '{"conclusion":"FALSE_ALARM","note":"v06 manual review"}'
$a5 = Alarm "pt3k-00002"
$ok5 = ($r.code -eq 200 -and $a5 -ne $null -and $a5.status -eq 'FALSE_ALARM' -and $a5.review -ne $null -and $a5.review.reviewerDeviceId -eq 'MANUAL')
V "V-5" "IT009 manual review" $ok5 ("code=" + $r.code + " status=" + $(if($a5){$a5.status}else{"?"}))

# --- V-6: TC032 detail API (snapshot + review both present) ---
$a6 = Alarm $aid1
$ok6 = ($a6 -ne $null -and $a6.snapshotPath -ne $null -and $a6.review -ne $null -and $a6.review.imagePath -ne $null)
V "V-6" "TC032 detail evidence chain API" $ok6 ("snapshot=" + $(if($a6){$a6.snapshotPath}else{"?"}) + " reviewImage=" + $(if($a6 -and $a6.review){$a6.review.imagePath}else{"?"}))

Write-Output "===== V0.6 SUMMARY: PASS=$pass FAIL=$fail ====="
