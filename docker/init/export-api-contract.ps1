# Export a condensed frontend API contract from the running backend.
# Reads /v3/api-docs (OpenAPI) + samples real responses, injects them into the Chinese template.
# ASCII-only script (STATE risk #16); Chinese text lives in the UTF-8 template file.
$ErrorActionPreference = "Stop"
$base = "http://127.0.0.1:8080"
. "$PSScriptRoot\auth-helper.ps1"     # S90: sample calls under /api/** need a Bearer token (S88); /v3/api-docs is whitelisted
Connect-InspectionApi -Base $base | Out-Null
$ws = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)   # docker/init -> project root
$template = Join-Path $ws "docs\前端接口契约_模板.md"
$outFile = Join-Path $ws "docs\前端接口契约.md"
$utf8 = New-Object System.Text.UTF8Encoding($false)

Write-Output "=== fetch openapi ==="
$spec = (Invoke-WebRequest -Uri "$base/v3/api-docs" -UseBasicParsing -TimeoutSec 30).Content | ConvertFrom-Json

$sb = New-Object System.Text.StringBuilder
[void]$sb.AppendLine("| Method | Path | Params | Responses |")
[void]$sb.AppendLine("|---|---|---|---|")
foreach ($p in $spec.paths.PSObject.Properties) {
    $path = $p.Name
    foreach ($m in $p.Value.PSObject.Properties) {
        $method = $m.Name.ToUpper()
        $op = $m.Value
        $params = @()
        if ($op.parameters) {
            foreach ($pp in $op.parameters) {
                $req = if ($pp.required) { "*" } else { "" }
                $params += ("`" + $pp.name + "`"(" + $pp.in + $req + ")")
            }
        }
        if ($op.requestBody) { $params += "body" }
        $paramStr = if ($params.Count -gt 0) { ($params -join ", ") } else { "-" }
        $codes = @()
        if ($op.responses) { foreach ($r in $op.responses.PSObject.Properties) { $codes += $r.Name } }
        $codeStr = if ($codes.Count -gt 0) { ($codes -join ", ") } else { "-" }
        [void]$sb.AppendLine("| " + $method + " | `" + $path + "`" + " | " + $paramStr + " | " + $codeStr + " |")
    }
}
$endpoints = $sb.ToString()

Write-Output "=== sample responses ==="
$ssb = New-Object System.Text.StringBuilder
function Sample([string]$title, [string]$method, [string]$url, [string]$body) {
    try {
        $p = @{ Uri = $url; Method = $method; TimeoutSec = 30; UseBasicParsing = $true; Headers = (AuthHeaders) }
        if ($body) { $p.ContentType = "application/json"; $p.Body = $body }
        $r = Invoke-WebRequest @p
        $text = $r.Content
        if ($text.Length -gt 700) { $text = $text.Substring(0, 700) + " ...(truncated)" }
        [void]$script:ssb.AppendLine("### " + $title)
        [void]$script:ssb.AppendLine("`" + $method + " " + $url.Replace($base, "") + "`" + " -> HTTP " + [int]$r.StatusCode)
        [void]$script:ssb.AppendLine('```json')
        [void]$script:ssb.AppendLine($text)
        [void]$script:ssb.AppendLine('```')
        [void]$script:ssb.AppendLine("")
        Write-Output ("ok: " + $title)
    } catch {
        [void]$script:ssb.AppendLine("### " + $title + " -> FAILED: " + $_.Exception.Message)
        Write-Output ("fail: " + $title + " " + $_.Exception.Message)
    }
}

Sample "devices list (with last lng/lat)" "GET" "$base/api/devices" $null
Sample "tasks page" "GET" "$base/api/tasks?page=0&size=2" $null
Sample "alarm combo search" "POST" "$base/api/search/alarms" '{"alarmType":"PERIMETER_BREACH","from":"now-24h","to":"now","page":0,"size":2}'
Sample "stats aggregation (charts)" "GET" "$base/api/search/stats" $null
Sample "system health (panel)" "GET" "$base/api/system/health" $null
Sample "device track (replay)" "GET" "$base/api/devices/ROBOT-001/track?minutes=2" $null
Sample "image meta" "GET" "$base/api/images/meta?page=0&size=2" $null
Sample "overview stats" "GET" "$base/api/stats/overview" $null

# alarm detail sample: prefer one that already has a review
try {
    $alarms = ((Invoke-WebRequest -Uri "$base/api/alarms?page=0&size=10" -UseBasicParsing -TimeoutSec 20 -Headers (AuthHeaders)).Content) | ConvertFrom-Json
    $withReview = $null
    foreach ($a in $alarms.records) {
        $d = ((Invoke-WebRequest -Uri "$base/api/alarms/$($a.alarmId)" -UseBasicParsing -TimeoutSec 20 -Headers (AuthHeaders)).Content) | ConvertFrom-Json
        if ($d.review) { $withReview = $d; break }
    }
    if ($withReview) {
        $json = ($withReview | ConvertTo-Json -Depth 6)
        if ($json.Length -gt 900) { $json = $json.Substring(0, 900) + " ...(truncated)" }
        [void]$ssb.AppendLine("### alarm detail (review subdocument + evidence chain)")
        [void]$ssb.AppendLine('```json')
        [void]$ssb.AppendLine($json)
        [void]$ssb.AppendLine('```')
        Write-Output "ok: alarm detail with review"
    } else {
        Write-Output "warn: no alarm with review found"
    }
} catch { Write-Output ("alarm detail failed: " + $_.Exception.Message) }

Write-Output "=== compose final doc ==="
$tpl = [System.IO.File]::ReadAllText($template, [System.Text.Encoding]::UTF8)
$ts = (Get-Date).ToString("yyyy-MM-dd HH:mm")
$doc = $tpl.Replace("{{TS}}", $ts).Replace("{{ENDPOINTS}}", $endpoints).Replace("{{SAMPLES}}", $ssb.ToString())
[System.IO.File]::WriteAllText($outFile, $doc, $utf8)
Write-Output ("written: " + $outFile + " (" + $doc.Length + " chars)")
