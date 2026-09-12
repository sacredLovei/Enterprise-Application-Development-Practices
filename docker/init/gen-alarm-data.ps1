# S50 test data prep: inject 10,000 alarms over last 24h (design doc 6.1.1 target volume).
# ASCII-only. Unique alarmId prefix pt3k- (filterable). Piped via stdin (PS5.1 quote-safe).
# Usage: powershell -File gen-alarm-data.ps1 [count] [waitSec]
param(
    [int]$Count = 10000,
    [int]$WaitSec = 1800
)
$ErrorActionPreference = "Continue"
$types  = @("PERIMETER_BREACH","INTRUSION","FIRE_SMOKE","DEVICE_OVERHEAT","BATTERY_LOW","DEVICE_OFFLINE")
$levels = @{ "PERIMETER_BREACH"="CRITICAL"; "INTRUSION"="CRITICAL"; "FIRE_SMOKE"="CRITICAL";
             "DEVICE_OVERHEAT"="MAJOR"; "BATTERY_LOW"="WARN"; "DEVICE_OFFLINE"="CRITICAL" }
$devices = @("UAV-001","UAV-002","ROBOT-001","ROBOT-002")
$nowMs = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

Write-Output ("generating " + $Count + " alarm payloads ...")
$lines = New-Object System.Collections.Generic.List[string]
for ($i = 1; $i -le $Count; $i++) {
    $type = $types[$i % 6]
    $dev  = $devices[$i % 4]
    $lng  = [math]::Round(116.3969 + (Get-Random -Minimum 0 -Maximum 1100) / 1000000.0, 6)
    $lat  = [math]::Round(39.9089  + (Get-Random -Minimum 0 -Maximum 1200) / 1000000.0, 6)
    $ts   = $nowMs - (Get-Random -Minimum 0 -Maximum 86400000)
    $lines.Add('{"alarmId":"pt3k-' + $i.ToString("00000") + '","deviceId":"' + $dev + '","deviceType":"' + $(if($dev -match "UAV"){"UAV"}else{"ROBOT"}) + '","alarmType":"' + $type + '","level":"' + $levels[$type] + '","description":"pt3k-bulk","lng":' + $lng + ',"lat":' + $lat + ',"occurredTime":' + $ts + '}')
}
Write-Output ("producing to inspection.alarm ...")
($lines -join "`n") | docker exec -i kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic inspection.alarm 2>&1 | Out-Null
Write-Output "produced. waiting for drain into Mongo ..."
$deadline = (Get-Date).AddSeconds($WaitSec)
$target = $Count
while ((Get-Date) -lt $deadline) {
    $eval = 'db.getSiblingDB(''inspection'').alarm.countDocuments({_id:{$regex:''^pt3k-''}})'
    $js = docker exec mongodb mongosh --quiet --eval $eval 2>&1 | Select-Object -Last 1
    $done = 0
    if ($js -is [string]) { $done = [int]($js.Trim()) }
    Write-Output ("drained " + $done + "/" + $target)
    if ($done -ge $target) { Write-Output "DRAIN-COMPLETE"; exit 0 }
    Start-Sleep -Seconds 15
}
Write-Output "DRAIN-TIMEOUT"
exit 1
