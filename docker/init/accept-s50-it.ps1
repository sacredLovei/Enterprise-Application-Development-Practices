# S50 interface tests IT001~IT015 (design doc 6.4). Prints PASS/FAIL/UNIMPL per case.
# ASCII-only. Uses live stack via Nginx 8080.
$base = "http://127.0.0.1:8080"
$pass = 0; $fail = 0; $unimpl = 0

function Verdict([string]$name, [string]$status, [string]$evidence) {
    Write-Output ("[{0}] {1}  |  {2}" -f $status, $name, $evidence)
    if ($status -eq "PASS") { $script:pass++ }
    elseif ($status -eq "UNIMPL") { $script:unimpl++ }
    else { $script:fail++ }
}

function Req([string]$method, [string]$url, [string]$body) {
    try {
        $p = @{ Uri = $url; Method = $method; TimeoutSec = 20; UseBasicParsing = $true }
        if ($body) {
            $p.ContentType = "application/json; charset=utf-8"
            $p.Body = $body
        }
        $r = Invoke-WebRequest @p
        return @{ code = [int]$r.StatusCode; body = $r.Content; headers = $r.Headers }
    } catch {
        $code = [int]$_.Exception.Response.StatusCode
        $msg = ""
        try { $msg = $_.ErrorDetails.Message } catch {}
        return @{ code = $code; body = $msg; headers = @{} }
    }
}

Write-Output "===== S50 INTERFACE TESTS ====="

# IT001 register endpoint: not implemented by design (registration is heartbeat-driven)
Verdict "IT001 POST /api/devices/register" "UNIMPL" "design deviation: registration driven by heartbeat upsert (TC001 covers)"

# IT002 device list with status filter
$r = Req "GET" "$base/api/devices?status=ONLINE" $null
$ok = ($r.code -eq 200) -and ($r.body -notmatch 'OFFLINE')
Verdict "IT002 GET /api/devices?status=ONLINE" $(if($ok){"PASS"}else{"FAIL"}) ("HTTP " + $r.code)

# IT003 device detail
$r = Req "GET" "$base/api/devices/UAV-001" $null
Verdict "IT003 GET /api/devices/UAV-001" $(if($r.code -eq 200){"PASS"}else{"FAIL"}) ("HTTP " + $r.code)

# IT004 device detail 404
$r = Req "GET" "$base/api/devices/NOT-EXIST-999" $null
Verdict "IT004 GET /api/devices/NOT-EXIST" $(if($r.code -eq 404){"PASS"}else{"FAIL"}) ("HTTP " + $r.code)

# IT005 create task 201
$r = Req "POST" "$base/api/tasks" '{"taskType":"RETURN_HOME","deviceId":"ROBOT-001","priority":2,"remark":"IT005","targetLng":null,"targetLat":null}'
$t5 = ""
if ($r.code -eq 201) { $t5 = ($r.body | ConvertFrom-Json).taskId }
Verdict "IT005 POST /api/tasks" $(if($r.code -eq 201){"PASS"}else{"FAIL"}) ("HTTP " + $r.code + " task=" + $t5)

# IT006 create task missing fields 400
$r = Req "POST" "$base/api/tasks" '{"taskType":"POINT_REVIEW","priority":1}'
Verdict "IT006 POST /api/tasks missing fields" $(if($r.code -eq 400){"PASS"}else{"FAIL"}) ("HTTP " + $r.code)

# IT007 cancel task
Start-Sleep -Seconds 4
$r = Req "POST" "$base/api/tasks/$t5/cancel" $null
$cancelled = $false
if ($r.code -eq 200) {
    $deadline = (Get-Date).AddSeconds(30)
    while ((Get-Date) -lt $deadline) {
        $list = (Req "GET" "$base/api/tasks?page=0&size=50" $null).body | ConvertFrom-Json
        $t = $list.records | Where-Object { $_.taskId -eq $t5 } | Select-Object -First 1
        if ($t -and $t.status -eq "CANCELLED") { $cancelled = $true; break }
        Start-Sleep -Seconds 5
    }
}
Verdict "IT007 POST /api/tasks/{id}/cancel" $(if($cancelled){"PASS"}else{"FAIL"}) ("HTTP " + $r.code + " cancelled=" + $cancelled)

# IT008 recent alarms paged
$r = Req "GET" "$base/api/alarms?page=0&size=15" $null
Verdict "IT008 GET /api/alarms" $(if($r.code -eq 200){"PASS"}else{"FAIL"}) ("HTTP " + $r.code)

# IT009 alarm review: not implemented (recorded as defect)
Verdict "IT009 POST /api/alarms/{id}/review" "UNIMPL" "review dispatch not implemented (deferred, recorded as defect)"

# IT010 search valid
$r = Req "POST" "$base/api/search/alarms" '{"alarmType":"BATTERY_LOW","from":"now-24h","to":"now","page":0,"size":5}'
Verdict "IT010 POST /api/search/alarms valid" $(if($r.code -eq 200){"PASS"}else{"FAIL"}) ("HTTP " + $r.code)

# IT011 search invalid time 400
$r = Req "POST" "$base/api/search/alarms" '{"alarmType":null,"from":"2026-13-45","to":"now","page":0,"size":5}'
Verdict "IT011 POST /api/search/alarms bad time" $(if($r.code -eq 400){"PASS"}else{"FAIL"}) ("HTTP " + $r.code)

# IT012 stats
$r = Req "GET" "$base/api/search/stats" $null
Verdict "IT012 GET /api/search/stats" $(if($r.code -eq 200){"PASS"}else{"FAIL"}) ("HTTP " + $r.code)

# IT013 file download by alarmId
$alarmList = (Req "GET" "$base/api/alarms?page=0&size=1" $null).body | ConvertFrom-Json
$alarmId = ""
if ($alarmList.records -and $alarmList.records.Count -gt 0) { $alarmId = $alarmList.records[0].alarmId }
$r = Req "GET" "$base/api/files/$alarmId" $null
Verdict "IT013 GET /api/files/{alarmId}" $(if($r.code -eq 200){"PASS"}else{"FAIL"}) ("HTTP " + $r.code + " alarm=" + $alarmId)

# IT014 overview stats
$r = Req "GET" "$base/api/stats/overview" $null
Verdict "IT014 GET /api/stats/overview" $(if($r.code -eq 200){"PASS"}else{"FAIL"}) ("HTTP " + $r.code)

# IT015 via nginx with instance header
$r = Req "POST" "$base/api/search/alarms" '{"from":"now-1h","to":"now","page":0,"size":5}'
$hdr = $null
if ($r.headers -and $r.headers.ContainsKey('X-Backend-Instance')) { $hdr = $r.headers['X-Backend-Instance'] }
Verdict "IT015 search via Nginx (instance header)" $(if($r.code -eq 200 -and $hdr){"PASS"}else{"FAIL"}) ("HTTP " + $r.code + " instance=" + $hdr)

Write-Output "===== S50 INTERFACE SUMMARY: PASS=$pass FAIL=$fail UNIMPL=$unimpl ====="
