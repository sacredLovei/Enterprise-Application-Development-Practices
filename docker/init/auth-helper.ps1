# S90: shared auth helper for acceptance scripts.
# Since S88 (FR-5.7) every /api/** call requires a Bearer token, so scripts must log in first.
# Usage at the top of a script:
#     . "$PSScriptRoot\auth-helper.ps1"
#     Connect-InspectionApi | Out-Null
# ... then add the headers to requests:  -Headers (AuthHeaders)   /   $p.Headers = AuthHeaders
# ASCII-only (PS 5.1 lesson, STATE risk #16).
$script:AuthToken = $null

function Connect-InspectionApi {
    param([string]$Base = "http://127.0.0.1:8080")
    $p = @{
        Uri         = "$Base/api/auth/login"
        Method      = "POST"
        TimeoutSec  = 20
        UseBasicParsing = $true
        ContentType = "application/json; charset=utf-8"
        Body        = '{"username":"admin","password":"admin123"}'
    }
    try {
        $r = Invoke-WebRequest @p
        $script:AuthToken = ($r.Content | ConvertFrom-Json).token
        # NOTE: Write-Host (not Write-Output) - output stream text would be captured together with the
        # return value by callers doing `$token = Connect-InspectionApi`, corrupting the token (S90 bug).
        Write-Host ("[auth] logged in (tokenLen=" + $script:AuthToken.Length + ")")
    } catch {
        Write-Host "[auth] LOGIN FAILED - protected calls will return 401"
    }
    return $script:AuthToken
}

function AuthHeaders {
    if ($script:AuthToken) { return @{ Authorization = "Bearer $($script:AuthToken)" } }
    return @{}
}
