# S63: install Elasticsearch ik analyzer plugin (analysis-ik) reproducibly.
# Source: infinilabs release server. Version must match the ES image (default 8.13.0).
# Steps: download -> unzip to docker/es-plugins/analysis-ik -> restart ES (compose bind-mounts plugins dir).
# ASCII-only (PS 5.1 lesson, STATE risk #16).
param(
    [string]$EsVersion = "8.13.0"
)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot   # docker/
$dest = Join-Path $root "es-plugins\analysis-ik"
$url = "https://release.infinilabs.com/analysis-ik/stable/elasticsearch-analysis-ik-$EsVersion.zip"
$tmp = Join-Path $env:TEMP "ik-$EsVersion.zip"

Write-Output "downloading $url"
Invoke-WebRequest -Uri $url -OutFile $tmp -TimeoutSec 300
Write-Output "unzip to $dest"
if (Test-Path $dest) { Remove-Item $dest -Recurse -Force }
Expand-Archive -Path $tmp -DestinationPath $dest -Force
Remove-Item $tmp -ErrorAction SilentlyContinue
Write-Output "done. restart elasticsearch to load plugin:"
Write-Output "  docker compose restart elasticsearch"
Write-Output "verify: docker exec elasticsearch /usr/share/elasticsearch/bin/elasticsearch-plugin list"
