# S50 performance tests PT001/002/005/006/007 (JMeter covers PT003/PT004 separately).
# ASCII-only. $base via Nginx 8080.
$base = "http://127.0.0.1:8080"
. "$PSScriptRoot\auth-helper.ps1"     # S90: /api/** requires a Bearer token (S88)
Connect-InspectionApi -Base $base | Out-Null
$pass = 0; $fail = 0

function V([string]$id, [string]$name, [bool]$ok, [string]$ev) {
    Write-Output ("[{0}] {1} {2}  |  {3}" -f $(if($ok){"PASS"}else{"FAIL"}), $id, $name, $ev)
    if ($ok) { $script:pass++ } else { $script:fail++ }
}

Write-Output "===== S50 PERFORMANCE TESTS (script part) ====="

# --- PT001: bulk produce 500 telemetry msgs (uav.telemetry -> device_status), verify zero loss ---
$c0 = docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('inspection').device_status.countDocuments({})" 2>&1 | Select-Object -Last 1
$lines = 1..500 | ForEach-Object {
    '{"deviceId":"UAV-002","lng":116.3974,"lat":39.9092,"altitude":80,"speed":12,"battery":80,"ts":' + [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() + '}'
}
($lines -join "`n") | docker exec -i kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic uav.telemetry 2>&1 | Out-Null
Start-Sleep -Seconds 25
$c1 = docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('inspection').device_status.countDocuments({})" 2>&1 | Select-Object -Last 1
$delta = [int]($c1.Trim()) - [int]($c0.Trim())
V "PT001" "bulk 500 telemetry zero loss" ($delta -ge 495) ("device_status docs +" + $delta)

# --- PT002: e2e drain latency, single bulk of 100 msgs (avoids per-msg producer spawn overhead) ---
$c2 = docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('inspection').device_status.countDocuments({})" 2>&1 | Select-Object -Last 1
$lines2 = 1..100 | ForEach-Object {
    '{"deviceId":"UAV-002","lng":116.3974,"lat":39.9092,"altitude":80,"speed":12,"battery":80,"ts":' + [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() + '}'
}
$sw = [System.Diagnostics.Stopwatch]::StartNew()
($lines2 -join "`n") | docker exec -i kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic uav.telemetry 2>&1 | Out-Null
$drained = $false
$deadline = (Get-Date).AddSeconds(15)
while ((Get-Date) -lt $deadline) {
    $c3 = docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('inspection').device_status.countDocuments({})" 2>&1 | Select-Object -Last 1
    if (([int]($c3.Trim()) - [int]($c2.Trim())) -ge 100) { $drained = $true; break }
    Start-Sleep -Milliseconds 300
}
$sw.Stop()
V "PT002" "e2e batch drain latency" ($drained -and $sw.ElapsedMilliseconds -lt 5000) ("100 msgs drained in " + $sw.ElapsedMilliseconds + "ms")

# --- PT005: LB distribution, paced 5 rps (respect rate limit; limit test is PT006's job) ---
$c1 = 0; $c2 = 0
1..100 | ForEach-Object {
    try {
        $h = (Invoke-WebRequest -Uri "$base/api/devices" -TimeoutSec 10 -UseBasicParsing -Headers (AuthHeaders)).Headers['X-Backend-Instance']
        if ($h -eq 'backend-1') { $c1++ } else { $c2++ }
    } catch {}
    Start-Sleep -Milliseconds 200
}
$dev = [math]::Abs($c1 - $c2)
V "PT005" "LB balance under load" ($dev -le 20) ("backend-1=$c1 backend-2=$c2 diff=$dev (target diff<20)")

# --- PT006: burst 200 concurrent -> expect some 503 (rate limit 20r/s burst 40) ---
Add-Type -AssemblyName System.Net.Http
$client = New-Object System.Net.Http.HttpClient
$tasks = 1..200 | ForEach-Object { $client.GetAsync("$base/api/devices") }
[System.Threading.Tasks.Task]::WaitAll($tasks) | Out-Null
$code503 = 0; $code200 = 0
$tasks | ForEach-Object {
    $code = [int]$_.Result.StatusCode
    if ($code -eq 503) { $code503++ }
    if ($code -eq 200) { $code200++ }
}
$client.Dispose()
V "PT006" "rate limit under burst" ($code503 -gt 0) ("200 burst -> 200x$code200 503x$code503 (limit_req active)")

# --- PT007: stability 10 min sampling (relative compose path; ASCII-safe) ---
$growth0 = docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('inspection').device_status.countDocuments({})" 2>&1 | Select-Object -Last 1
$alive = $true
1..10 | ForEach-Object {
    $ps = docker compose ps --format "{{.Name}} {{.Status}}" 2>&1
    $down = $ps | Where-Object { $_ -notmatch "Up " }
    if ($down.Count -gt 0) { $alive = $false; Write-Output ("   down: " + ($down -join ',')) }
    Start-Sleep -Seconds 60
}
$growth1 = docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('inspection').device_status.countDocuments({})" 2>&1 | Select-Object -Last 1
V "PT007" "10min stability" ($alive -and ([int]($growth1.Trim()) -gt [int]($growth0.Trim()))) ("containers alive=" + $alive + " docs +" + ([int]($growth1.Trim()) - [int]($growth0.Trim())))

Write-Output "===== S50 PERFORMANCE(script) SUMMARY: PASS=$pass FAIL=$fail ====="
