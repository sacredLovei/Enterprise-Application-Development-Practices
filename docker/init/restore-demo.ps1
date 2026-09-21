# S87 (course task item 4): backup & restore DEMO with real verification.
#   1) mongodump the inspection db, restore into a TEMP db (inspection_restore) via --nsFrom/--nsTo,
#      compare per-collection document counts, then drop the temp db.
#   2) git bundle: create bundle, clone into a temp dir, verify HEAD == repo HEAD and tag count.
#   3) write RAW results to docs/_restore-demo-raw.txt (Chinese report is composed separately).
# ASCII-only, NO Chinese, NO emoji (PS 5.1 GBK parse lesson, STATE risk #16).
param([string]$Tag = (Get-Date -Format "yyyyMMdd-HHmmss"))
# NOTE: keep ErrorActionPreference at Continue -- docker/mongodump writes progress to stderr and
# PowerShell would otherwise treat it as a terminating NativeCommandError.
$ErrorActionPreference = "Continue"
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)   # docker/init -> project root
$utf8 = New-Object System.Text.UTF8Encoding($false)
$cols = @('device','device_status','task','task_log','alarm','image_meta')
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

Write-Output "===== S87 BACKUP & RESTORE DEMO ====="
$composeArgs = @('-f', (Join-Path $root 'docker\docker-compose.yml'))

# ---------- 0) pause writers (simulators + backends) so the dataset is fully static ----------
# NOTE: pausing simulators alone is not enough -- OfflineDetector would emit DEVICE_OFFLINE alarms
# during the dump window, making the comparison drift by a doc or two.
Write-Output "=== 0/2 pause writers (simulators + backends) for a static dataset ==="
docker compose @composeArgs stop uav-sim-1 uav-sim-2 dog-sim-1 dog-sim-2 backend-1 backend-2 2>&1 | Out-Null
Start-Sleep -Seconds 8
Write-Output "  writers paused"

# ---------- 1) MongoDB dump -> restore into temp db -> compare ----------
Write-Output "=== 1/2 mongodump + restore into temp db ==="
docker exec mongodb sh -c "rm -rf /tmp/s87 && mongodump --db inspection --out /tmp/s87" 2>&1 | Out-Null
docker exec mongodb sh -c "mongorestore --nsFrom 'inspection.*' --nsTo 'inspection_restore.*' /tmp/s87" 2>&1 | Out-Null
Write-Output "  dump + restore done"

$rows = @()
$allMatch = $true
foreach ($c in $cols) {
    $src = [int](Mongo ('db.getSiblingDB(''inspection'').getCollection(''' + $c + ''').countDocuments({})'))
    $dst = [int](Mongo ('db.getSiblingDB(''inspection_restore'').getCollection(''' + $c + ''').countDocuments({})'))
    $ok = ($src -eq $dst -and $src -ge 0)
    if (-not $ok) { $allMatch = $false }
    $rows += ($c + "," + $src + "," + $dst + "," + $(if($ok){"MATCH"}else{"MISMATCH"}))
    Write-Output ("  " + $c + ": src=" + $src + " restored=" + $dst + " " + $(if($ok){"OK"}else{"MISMATCH"}))
}
V "S87a" "mongodb restore parity" $allMatch ("collections=" + $cols.Count + " all-counts-match=" + $allMatch)

Write-Output "=== drop temp db ==="
$dropped = Mongo 'db.getSiblingDB(''inspection_restore'').dropDatabase().ok'
Write-Output ("  dropDatabase ok=" + $dropped)
docker exec mongodb sh -c "rm -rf /tmp/s87" 2>&1 | Out-Null

# ---------- resume writers ----------
Write-Output "=== resume writers ==="
docker compose @composeArgs start backend-1 backend-2 uav-sim-1 uav-sim-2 dog-sim-1 dog-sim-2 2>&1 | Out-Null
$deadline = (Get-Date).AddMinutes(3)
while ((Get-Date) -lt $deadline) {
    $ps = docker compose @composeArgs ps --format "{{.Name}} {{.Health}}" 2>&1
    $bad = $ps | Where-Object { $_ -notmatch 'healthy$' -and $_ -notmatch 'sim' }
    if (@($ps).Count -gt 0 -and @($bad).Count -eq 0) { break }
    Start-Sleep -Seconds 10
}
docker compose @composeArgs restart nginx 2>&1 | Out-Null
Start-Sleep -Seconds 8
Write-Output "  writers resumed"

# ---------- 2) git bundle -> clone -> verify ----------
Write-Output "=== 2/2 git bundle clone verify ==="
$backupDir = Join-Path $root "_backups\$Tag"
New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
$bundle = Join-Path $backupDir "repo.bundle"
git -C $root bundle create $bundle --all 2>&1 | Select-Object -Last 1
$tmp = Join-Path $env:TEMP "s87-restore"
if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
git clone $bundle $tmp 2>&1 | Out-Null
$headRepo = (git -C $root rev-parse HEAD).Trim()
$headClone = (git -C $tmp rev-parse HEAD).Trim()
$tagsRepo = @(git -C $root tag -l).Count
$tagsClone = @(git -C $tmp tag -l).Count
$bundleMB = [math]::Round((Get-Item $bundle).Length / 1MB, 2)
V "S87b" "git bundle restore" ($headRepo -eq $headClone -and $tagsClone -eq $tagsRepo) ("head_match=" + ($headRepo -eq $headClone) + " tags=" + $tagsClone + "/" + $tagsRepo + " bundle=" + $bundleMB + "MB")
Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue

# ---------- 3) raw results for report composition ----------
$raw = @()
$raw += "TS=" + (Get-Date -Format 'yyyy-MM-dd HH:mm')
$raw += "TAG=" + $Tag
$raw += "MONGO_ROWS_BEGIN"
$raw += $rows
$raw += "MONGO_ROWS_END"
$raw += "MONGO_ALL_MATCH=" + $allMatch
$raw += "BUNDLE_MB=" + $bundleMB
$raw += "HEAD_REPO=" + $headRepo
$raw += "HEAD_CLONE=" + $headClone
$raw += "TAGS_REPO=" + $tagsRepo
$raw += "TAGS_CLONE=" + $tagsClone
$raw += "PASS=" + $pass
$raw += "FAIL=" + $fail
$rawFile = Join-Path $root "docs\_restore-demo-raw.txt"
[System.IO.File]::WriteAllText($rawFile, ($raw -join "`r`n"), $utf8)
Write-Output ("raw written: " + $rawFile)

Write-Output ("===== S87 SUMMARY: PASS=" + $pass + " FAIL=" + $fail + " =====")
