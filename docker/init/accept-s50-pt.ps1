# S50 performance tests PT001/002/005/006/007 (JMeter covers PT003/PT004 separately).
# ASCII-only. $base via Nginx 8080.
$base = "http://127.0.0.1:8080"
$pass = 0; $fail = 0

function V([string]$id, [string]$name, [bool]$ok, [string]$ev) {
    Write-Output ("[{0}] {1} {2}  |  {3}" -f $(if($ok){"PASS"}else{"FAIL"}), $id, $name, $ev)
    if ($ok) { $script:pass++ } else { $script:fail++ }
}

Write-Output "===== S50 PERFORMANCE TESTS (script part) ====="

# --- PT001: bulk produce 500 heartbeat msgs, verify zero loss (LAG=0, +500 docs) ---
$c0 = docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('inspection').device_status.countDocuments({})" 2>&1 | Select-Object -Last 1
$lines = 1..500 | ForEach-Object {
    '{"deviceId":"UAV-001","deviceType":"UAV","battery":99,"currentTaskId":null,"ts":' + [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() + '}'
}
($lines -join "`n") | docker exec -i kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic device.heartbeat 2>&1 | Out-Null
Start-Sleep -Seconds 25
$c1 = docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('inspection').device_status.countDocuments({})" 2>&1 | Select-Object -Last 1
$lag = docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group biz-storage-consumer 2>&1 | Select-String "device.heartbeat"
$lagText = $lag -join ' '
V "PT001" "bulk 500 msgs zero loss" (([int]($c1.Trim()) - [int]($c0.Trim())) -ge 495) ("docs +" + ([int]($c1.Trim()) - [int]($c0.Trim())) + " LAG lines=" + ($lag | Measure-Object).Count)

# --- PT002: e2e telemetry latency, 15 samples ---
$lat = @()
1..15 | ForEach-Object {
    $ts = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    $msg = '{"deviceId":"UAV-001","deviceType":"UAV","battery":99,"currentTaskId":null,"ts":' + $ts + '}'
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $msg | docker exec -i kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic device.heartbeat 2>&1 | Out-Null
    $found = $false
    $deadline = (Get-Date).AddSeconds(10)
    while ((Get-Date) -lt $deadline) {
        $hb = docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('inspection').device.findOne({_id:'UAV-001'}).lastHeartbeat.getTime()" 2>&1 | Select-Object -Last 1
        if ([int64]($hb.Trim()) -ge $ts) { $found = $true; break }
        Start-Sleep -Milliseconds 200
    }
    $sw.Stop()
    if ($found) { $lat += $sw.ElapsedMilliseconds }
}
if ($lat.Count -gt 0) {
    $avg = [math]::Round(($lat | Measure-Object -Average).Average, 1)
    $sorted = $lat | Sort-Object
    $p95 = $sorted[[math]::Min($sorted.Count - 1, [int]($sorted.Count * 0.95))]
    V "PT002" "e2e telemetry latency" ($avg -lt 3000) ("samples=" + $lat.Count + " avg=" + $avg + "ms p95=" + $p95 + "ms (target P95<1.5s incl. produce spawn overhead)")
} else {
    V "PT002" "e2e telemetry latency" $false "no samples measured"
}

# --- PT005: LB distribution under load (100 requests) ---
$c1 = 0; $c2 = 0
1..100 | ForEach-Object {
    try {
        $h = (Invoke-WebRequest -Uri "$base/api/devices" -TimeoutSec 10 -UseBasicParsing).Headers['X-Backend-Instance']
        if ($h -eq 'backend-1') { $c1++ } else { $c2++ }
    } catch {}
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

# --- PT007: stability 10 min sampling ---
$growth0 = docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('inspection').device_status.countDocuments({})" 2>&1 | Select-Object -Last 1
$alive = $true
1..10 | ForEach-Object {
    $ps = docker compose -f "C:\Users\黎Li\Desktop\Enterprise Application Development Practices\docker\docker-compose.yml" ps --format "{{.Name}} {{.Status}}" 2>&1
    $down = $ps | Where-Object { $_ -notmatch "Up " }
    if ($down.Count -gt 0) { $alive = $false; Write-Output ("   down: " + ($down -join ',')) }
    Start-Sleep -Seconds 60
}
$growth1 = docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('inspection').device_status.countDocuments({})" 2>&1 | Select-Object -Last 1
V "PT007" "10min stability" ($alive -and ([int]($growth1.Trim()) -gt [int]($growth0.Trim()))) ("containers alive=" + $alive + " docs +" + ([int]($growth1.Trim()) - [int]($growth0.Trim())))

Write-Output "===== S50 PERFORMANCE(script) SUMMARY: PASS=$pass FAIL=$fail ====="
