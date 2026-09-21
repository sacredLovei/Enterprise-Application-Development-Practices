# S90: run a JMeter plan against the live stack with a valid Bearer token.
# Since S88 (FR-5.7) all /api/** calls need a token; both plans read it from the JMeter property
# "token" (header value: Bearer ${__P(token)}), threads/duration are overridable too.
# ASCII-only (PS 5.1 lesson, STATE risk #16).
#
# Examples:
#   .\jmeter-run.ps1 -Plan ..\..\test\jmeter\gateway-plan.jmx -Threads 2 -Duration 10   # smoke run
#   .\jmeter-run.ps1 -Plan ..\..\test\jmeter\search-plan.jmx                            # full PT003
param(
    [Parameter(Mandatory = $true)][string]$Plan,
    [int]$Threads = 0,
    [int]$Duration = 0,
    [string]$ResultFile = "",
    [string]$Jmeter = ""
)

$ErrorActionPreference = "Stop"
. "$PSScriptRoot\auth-helper.ps1"
$token = Connect-InspectionApi
if (-not $token -or $token -isnot [string]) { throw "login failed - cannot run an authenticated JMeter plan" }
$token = "$token".Trim()

if (-not (Test-Path $Plan)) { throw "plan not found: $Plan" }

# Locate jmeter.bat: explicit param > JMETER_HOME > search under the user profile (path may contain
# non-ASCII characters, so it is never hardcoded in this ASCII-only script).
if (-not $Jmeter) {
    $cands = @()
    if ($env:JMETER_HOME) { $cands += (Join-Path $env:JMETER_HOME "bin\jmeter.bat") }
    $found = Get-ChildItem -Path $env:USERPROFILE -Filter "jmeter.bat" -Recurse -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
    if ($found) { $cands += $found }
    $Jmeter = $cands | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1
}
if (-not $Jmeter) { throw "jmeter not found - pass -Jmeter <path to jmeter.bat> or set JMETER_HOME" }
if (-not $ResultFile) {
    $ResultFile = Join-Path (Split-Path -Parent $Plan) ("result-" + (Get-Date -Format "yyyyMMdd-HHmmss") + ".jtl")
}

$jmArgs = @("-n", "-t", $Plan, "-l", $ResultFile, "-Jtoken=$token")
if ($Threads -gt 0) { $jmArgs += "-Jthreads=$Threads" }
if ($Duration -gt 0) { $jmArgs += "-Jduration=$Duration" }

Write-Output ("[jmeter] plan=" + $Plan)
Write-Output ("[jmeter] result=" + $ResultFile)
Write-Output ("[jmeter] extra=" + (($jmArgs | Where-Object { $_ -like '-J*' }) -join " "))
& $Jmeter @jmArgs
Write-Output ("[jmeter] exit=" + $LASTEXITCODE + " result=" + $ResultFile)
