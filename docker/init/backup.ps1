# S79 / S60: one-click backup script.
# Produces into _backups/<tag>/:
#   repo.bundle     - full git history (restore: git clone repo.bundle <dir>)
#   mongo/          - mongodump BSON export of inspection db
#   compose.yml     - current docker compose config snapshot
# HDFS data lives in named volumes (hadoop_namenode / hadoop_datanode) and is preserved by Docker
# across restarts; volume-level backup is documented but not automated (course scope).
# ASCII-only (PS 5.1 lesson, STATE risk #16).
param(
    [string]$Tag = (Get-Date -Format "yyyyMMdd-HHmmss")
)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)   # docker/init -> project root
$out = Join-Path $root "_backups\$Tag"
New-Item -ItemType Directory -Force -Path $out | Out-Null

Write-Output "=== 1/3 git bundle ==="
git -C $root bundle create (Join-Path $out "repo.bundle") --all
Write-Output ("bundle size: " + [math]::Round((Get-Item (Join-Path $out "repo.bundle")).Length / 1MB, 2) + " MB")

Write-Output "=== 2/3 mongodump ==="
docker exec mongodb sh -c "rm -rf /tmp/backup && mongodump --db inspection --out /tmp/backup" 2>&1 | Select-Object -Last 2
docker cp mongodb:/tmp/backup (Join-Path $out "mongo") 2>&1 | Out-Null

Write-Output "=== 3/3 compose config ==="
Set-Location (Join-Path $root "docker")
docker compose config 2>&1 | Out-File -FilePath (Join-Path $out "compose.yml") -Encoding utf8

Write-Output "=== backup summary ==="
Get-ChildItem $out -Recurse -File | ForEach-Object { $_.FullName.Replace($out, "") + " | " + $_.Length + "B" }
Write-Output ("NOTE: HDFS data in named volumes (hadoop_namenode/hadoop_datanode/es-data/mongo-data/kafka-data) - preserved by Docker; see STATE S60.")
Write-Output ("restore test: git clone " + (Join-Path $out "repo.bundle") + " <target-dir>")
