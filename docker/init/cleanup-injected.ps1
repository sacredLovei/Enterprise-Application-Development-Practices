# S93: remove test/stress-injected samples only (Mongo + ES + HDFS kept in step).
# Keeps all natural simulation data. Idempotent: re-running deletes nothing (counts already 0).
# ASCII-only (PS 5.1 lesson, STATE risk #16).
$ErrorActionPreference = "Continue"
$jsFile = Join-Path $PSScriptRoot "_cleanup-injected.js"
$base = "http://127.0.0.1:8080"

Write-Output "===== S93 CLEANUP OF INJECTED SAMPLES ====="

# ---- 1) Mongo delete (script prints before/after counts and the HDFS paths it orphaned) ----
$out = Get-Content $jsFile -Raw | docker exec -i mongodb mongosh --quiet 2>&1
$out = $out | ForEach-Object { $_ -replace '^rs0 \[direct: primary\] test> ', '' } | Where-Object { $_ -match '\S' }
$out | ForEach-Object { Write-Output ("  " + $_) }

$pathsLine = ($out | Where-Object { $_ -match '^PATHS=' } | Select-Object -First 1)
$idsLine = ($out | Where-Object { $_ -match '^ALARMIDS=' } | Select-Object -First 1)
$paths = @()
if ($pathsLine) { $paths = ($pathsLine -replace '^PATHS=', '').Trim() -split '\s+' | Where-Object { $_ } }
$alarmIds = @()
if ($idsLine) { $alarmIds = ($idsLine -replace '^ALARMIDS=', '').Trim() -split '\s+' | Where-Object { $_ } }

# ---- 2) HDFS: remove the orphaned images of the deleted alarms ----
Write-Output ("=== HDFS: removing " + $paths.Count + " orphaned image(s) ===")
$removed = 0
foreach ($p in $paths) {
    $r = docker exec namenode bash -c "hdfs dfs -rm -f '$p'" 2>&1
    if (($r -join ' ') -match 'Deleted|deleted') { $removed++ }
}
Write-Output ("  hdfs removed=" + $removed + "/" + $paths.Count)

# ---- 3) ES: delete the replica docs of the deleted alarms (D-5: Mongo authoritative, ES replica) ----
Write-Output "=== ES: deleting replica docs ==="
$esRemoved = 0
foreach ($id in $alarmIds) {
    try {
        $body = '{"query":{"term":{"_id":"' + $id + '"}}}'
        $r = Invoke-WebRequest -Uri "http://127.0.0.1:9200/inspection_alarm_v3/_delete_by_query?refresh=true" -Method POST -ContentType "application/json" -Body $body -UseBasicParsing -TimeoutSec 20
        $j = $r.Content | ConvertFrom-Json
        $esRemoved += [int]$j.deleted
    } catch {
        Write-Output ("  ES delete failed for " + $id + ": " + $_.Exception.Message)
    }
}
Write-Output ("  es deleted=" + $esRemoved + "/" + $alarmIds.Count)

# ---- 4) verify: Mongo vs ES convergence over the whole corpus (alarm ids must match 1:1) ----
Start-Sleep -Seconds 2
$mongoAlarms = (docker exec mongodb mongosh --quiet --eval "print(db.getSiblingDB('inspection').alarm.countDocuments({}))" 2>&1 | Where-Object { $_ -match '^\d+$' } | Select-Object -First 1)
$esAlarms = 0
try {
    $r = Invoke-WebRequest -Uri "http://127.0.0.1:9200/inspection_alarm_v3/_count" -UseBasicParsing -TimeoutSec 20
    $esAlarms = [int](($r.Content | ConvertFrom-Json).count)
} catch { }
$mongoDs = (docker exec mongodb mongosh --quiet --eval "print(db.getSiblingDB('inspection').device_status.countDocuments({}))" 2>&1 | Where-Object { $_ -match '^\d+$' } | Select-Object -First 1)
$esMarkers = 0
try {
    $r = Invoke-WebRequest -Uri "http://127.0.0.1:9200/inspection_alarm_v3/_count" -Method POST -ContentType "application/json" -Body '{"query":{"terms":{"_id":["v06-1789963001579"]}}}' -UseBasicParsing -TimeoutSec 20
    $esMarkers = [int](($r.Content | ConvertFrom-Json).count)
} catch { }

Write-Output "=== VERIFY ==="
Write-Output ("  mongo.alarm=" + $mongoAlarms)
Write-Output ("  es.alarm=" + $esAlarms)
Write-Output ("  mongo.device_status=" + $mongoDs)
Write-Output ("  leftover injected alarms in ES (sample probe, expect 0)=" + $esMarkers)

$okA = ([int]$mongoAlarms -eq [int]$esAlarms)
$okB = ([int]$esMarkers -eq 0)
Write-Output ("[PASS?] S93a mongo/es alarm counts converged = " + $okA)
Write-Output ("[PASS?] S93b injected alarm gone from ES = " + $okB)
Write-Output ("===== S93 SUMMARY: " + $(if ($okA -and $okB) { "PASS=2 FAIL=0" } else { "ATTENTION NEEDED" }) + " =====")
