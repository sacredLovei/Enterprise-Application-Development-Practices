# HTML -> DOCX via Word COM (S91). Reusable: regenerating the report no longer needs manual Word steps.
# ASCII-only script (STATE risk #16); it does not print or embed Chinese literals itself.
# Usage:
#   powershell -NoProfile -ExecutionPolicy Bypass -File .\to-docx.ps1 -Html <abs html> -Docx <abs docx>
#   optional -Verify <string>  : substring to look for in the produced docx (encoded as Unicode escapes
#                                to keep this script ASCII; pass code points via -VerifyCodes)
param(
    [Parameter(Mandatory = $true)][string]$Html,
    [Parameter(Mandatory = $true)][string]$Docx,
    # comma separated Unicode code points of a marker phrase, e.g. "32479,19968" (ASCII-only script)
    [string]$VerifyCodes = ""
)
$ErrorActionPreference = "Stop"

if (-not (Test-Path $Html)) { throw "html not found: $Html" }
$Html = (Resolve-Path $Html).Path
# Word resolves relative paths against ITS OWN working directory (not the shell's) - always pass
# absolute paths, otherwise the output silently lands in Documents (S91 gotcha).
$DocxAbs = if ([System.IO.Path]::IsPathRooted($Docx)) { $Docx } else { Join-Path (Get-Location).Path $Docx }
$DocxDir = Split-Path -Parent $DocxAbs
if (-not (Test-Path $DocxDir)) { New-Item -ItemType Directory -Force -Path $DocxDir | Out-Null }
if (Test-Path $DocxAbs) { Remove-Item $DocxAbs -Force }

$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0
$paras = 0; $tables = 0; $found = $null
try {
    $doc = $word.Documents.Open($Html, $false, $false)
    $doc.SaveAs2($DocxAbs, 16)          # 16 = wdFormatDocumentDefault (.docx)
    $paras = $doc.Paragraphs.Count
    $tables = $doc.Tables.Count
    $doc.Close(0)

    # reopen the produced docx and verify it is readable + contains an expected marker
    $check = $word.Documents.Open($DocxAbs, $false, $true)
    $checkParas = $check.Paragraphs.Count
    $checkTables = $check.Tables.Count
    $codes = @()
    if ($VerifyCodes) { $codes = @($VerifyCodes -split ',' | Where-Object { $_ -match '^\d+$' } | ForEach-Object { [int]$_ }) }
    if ($codes.Count -gt 0) {
        $needle = -join ($codes | ForEach-Object { [char]$_ })
        $rng = $check.Content
        $found = $rng.Find.Execute($needle)
    }
    $check.Close(0)
    Write-Output ("docx written: " + $DocxAbs)
    Write-Output ("  source (html render): paragraphs=" + $paras + " tables=" + $tables)
    Write-Output ("  produced docx       : paragraphs=" + $checkParas + " tables=" + $checkTables)
    Write-Output ("  size_bytes=" + (Get-Item $DocxAbs).Length)
    if ($codes.Count -gt 0) { Write-Output ("  marker_found=" + $found) }
} catch {
    Write-Output ("FAILED: " + $_.Exception.Message)
    exit 1
} finally {
    try { $word.Quit() } catch { }
    try { [System.Runtime.InteropServices.Marshal]::ReleaseComObject($word) | Out-Null } catch { }
}
