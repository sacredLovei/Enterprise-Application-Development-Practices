# S33 acceptance: real task behaviors (patrol / point review / return home / area cover)
# ASCII-only output. Runs against the live stack.
$base = "http://127.0.0.1:8080"

function NewTask([string]$type, [string]$device, $lng, $lat) {
    $lngStr = if ($null -ne $lng) { "$lng" } else { "null" }
    $latStr = if ($null -ne $lat) { "$lat" } else { "null" }
    $body = '{"taskType":"' + $type + '","deviceId":"' + $device + '","priority":1,"remark":"S33-accept","targetLng":' + $lngStr + ',"targetLat":' + $latStr + '}'
    $r = Invoke-WebRequest -Uri "$base/api/tasks" -Method Post -ContentType "application/json; charset=utf-8" -Body $body -TimeoutSec 15 -UseBasicParsing
    return ($r.Content | ConvertFrom-Json).taskId
}

function TaskStatus([string]$id) {
    $list = (Invoke-WebRequest -Uri "$base/api/tasks" -TimeoutSec 15 -UseBasicParsing).Content | ConvertFrom-Json
    $t = $list | Where-Object { $_.taskId -eq $id } | Select-Object -First 1
    if ($null -eq $t) { return "?" }
    return $t.status
}

function WaitDone([string]$id, [int]$seconds) {
    $deadline = (Get-Date).AddSeconds($seconds)
    $last = "?"
    while ((Get-Date) -lt $deadline) {
        $last = TaskStatus $id
        if ($last -eq "DONE") { return $true }
        Start-Sleep -Seconds 10
    }
    Write-Output ("   last status: " + $last)
    return $false
}

function LastPos([string]$device) {
    $out = docker exec mongodb mongosh --quiet --eval "const d=db.getSiblingDB('inspection').device_status.find({deviceId:'$device'}).sort({ts:-1}).limit(1).toArray(); if(d.length){print(d[0].lng+','+d[0].lat)}" 2>&1
    return ($out | Select-Object -Last 1).Trim()
}

Write-Output "=== wait devices online ==="
Start-Sleep -Seconds 10

Write-Output "=== T1: PERIMETER_PATROL -> UAV-001 ==="
$t1 = NewTask "PERIMETER_PATROL" "UAV-001" $null $null
Write-Output "task=$t1"

Write-Output "=== T2: POINT_REVIEW -> ROBOT-001 target(116.3970,39.9099) ==="
$t2 = NewTask "POINT_REVIEW" "ROBOT-001" 116.3970 39.9099
Write-Output "task=$t2"

$r1 = WaitDone $t1 180
Write-Output ("T1 patrol DONE=" + $r1)

$r2 = WaitDone $t2 200
Write-Output ("T2 review DONE=" + $r2)
Write-Output ("ROBOT-001 last pos: " + (LastPos "ROBOT-001") + "  (target=116.3970,39.9099)")

Write-Output "=== T3: RETURN_HOME -> ROBOT-001 ==="
$t3 = NewTask "RETURN_HOME" "ROBOT-001" $null $null
Write-Output "task=$t3"

Write-Output "=== T4: AREA_COVER -> UAV-001 center(116.3974,39.9092) ==="
$t4 = NewTask "AREA_COVER" "UAV-001" 116.3974 39.9092
Write-Output "task=$t4"

$r3 = WaitDone $t3 240
Write-Output ("T3 home DONE=" + $r3)
Write-Output ("ROBOT-001 final pos: " + (LastPos "ROBOT-001") + "  (home=116.3974,39.9092)")

$r4 = WaitDone $t4 240
Write-Output ("T4 sweep DONE=" + $r4)

Write-Output "=== SUMMARY: T1=$r1 T2=$r2 T3=$r3 T4=$r4 ==="
