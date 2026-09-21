# S88 acceptance: simple authentication (course task item 1).
# Cases: no token 401 / bad password 401 / empty body 400 / login 200 / token works /
#        tampered token 401 / me 200 / logout 200 then token revoked 401 / revoke doc in mongo /
#        second account role / token valid across BOTH backend instances / swagger whitelist /
#        frontend dist contains login page.
# ASCII-only (PS 5.1 lesson, STATE risk #16).
$base = "http://127.0.0.1:8080"
$pass = 0; $fail = 0

function V([string]$id, [string]$name, [bool]$ok, [string]$ev) {
    Write-Output ("[{0}] {1} {2}  |  {3}" -f $(if($ok){"PASS"}else{"FAIL"}), $id, $name, $ev)
    if ($ok) { $script:pass++ } else { $script:fail++ }
}

function Req([string]$method, [string]$url, [string]$body, [string]$token) {
    try {
        $p = @{ Uri = $url; Method = $method; TimeoutSec = 20; UseBasicParsing = $true }
        if ($token) { $p.Headers = @{ Authorization = "Bearer $token" } }
        if ($body) { $p.ContentType = "application/json; charset=utf-8"; $p.Body = $body }
        $r = Invoke-WebRequest @p
        $inst = ""
        try { $inst = [string]$r.Headers['x-backend-instance'] } catch { }
        return @{ code = [int]$r.StatusCode; body = $r.Content; inst = $inst }
    } catch {
        $code = 0
        if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
        return @{ code = $code; body = ""; inst = "" }
    }
}

function MongoCount([string]$collection) {
    $js = "print(db.getSiblingDB('inspection')." + $collection + ".countDocuments({}))"
    $out = docker exec mongodb mongosh --quiet --eval $js 2>&1
    $line = ($out | Where-Object { $_ -match '^\d+$' } | Select-Object -First 1)
    if ($line) { return [int]$line }
    return -1
}

Write-Output "===== S88 ACCEPTANCE (simple authentication) ====="

# --- A-1: protected API without token -> 401 ---
$r = Req "GET" "$base/api/devices" $null $null
V "A-1" "no-token request rejected" ($r.code -eq 401) ("GET /api/devices -> " + $r.code)

# --- A-2: wrong password -> 401 ---
$r = Req "POST" "$base/api/auth/login" '{"username":"admin","password":"wrong-password"}' $null
V "A-2" "wrong password rejected" ($r.code -eq 401) ("login -> " + $r.code)

# --- A-3: empty credentials -> 400 ---
$r = Req "POST" "$base/api/auth/login" '{"username":"","password":""}' $null
V "A-3" "empty credentials rejected" ($r.code -eq 400) ("login -> " + $r.code)

# --- A-4: correct login -> 200 with token ---
$r = Req "POST" "$base/api/auth/login" '{"username":"admin","password":"admin123"}' $null
$token = $null; $role = ""; $exp = ""
if ($r.code -eq 200) {
    $j = $r.body | ConvertFrom-Json
    $token = $j.token; $role = $j.role; $exp = $j.expiresAt
}
V "A-4" "login returns bearer token" ($r.code -eq 200 -and $token.Length -gt 40 -and $role -eq "ADMIN") `
    ("code=" + $r.code + " role=" + $role + " tokenLen=" + $(if($token){$token.Length}else{0}) + " expiresAt=" + $exp)

# --- A-5: token grants access to business APIs ---
$paths = @("/api/devices", "/api/alarms?page=0&size=5", "/api/tasks?page=0&size=5", "/api/stats/overview", "/api/system/health", "/api/images/meta-stats")
$okAll = $true; $detail = @()
foreach ($p in $paths) {
    $rr = Req "GET" ($base + $p) $null $token
    if ($rr.code -ne 200) { $okAll = $false }
    $detail += ($p + "=" + $rr.code)
    Start-Sleep -Milliseconds 80
}
V "A-5" "token grants business APIs" $okAll (($detail -join " "))

# --- A-6: tampered token -> 401 ---
$r = Req "GET" "$base/api/devices" $null ($token + "x")
V "A-6" "tampered token rejected" ($r.code -eq 401) ("devices with tampered token -> " + $r.code)

# --- A-7: forged signature (payload kept, signature replaced) -> 401 ---
$parts = $token.Split('.')
$forged = $parts[0] + "." + ("A" * $parts[1].Length)
$r = Req "GET" "$base/api/devices" $null $forged
V "A-7" "forged signature rejected" ($r.code -eq 401) ("devices with forged sig -> " + $r.code)

# --- A-8: /api/auth/me -> 200 with same user ---
$r = Req "GET" "$base/api/auth/me" $null $token
$me = $null
if ($r.code -eq 200) { $me = $r.body | ConvertFrom-Json }
V "A-8" "me returns current user" ($r.code -eq 200 -and $me.username -eq "admin") ("code=" + $r.code + " user=" + $(if($me){$me.username}else{"?"}) + " role=" + $(if($me){$me.role}else{"?"}))

# --- A-9: second account has its own role ---
$r = Req "POST" "$base/api/auth/login" '{"username":"operator","password":"operator123"}' $null
$ok9 = $false
if ($r.code -eq 200) { $ok9 = (($r.body | ConvertFrom-Json).role -eq "OPERATOR") }
V "A-9" "second account role" $ok9 ("login operator -> " + $r.code)

# --- A-10: same token accepted by BOTH backend instances (load-balanced) ---
$seen = @()
for ($i = 0; $i -lt 16; $i++) {
    $rr = Req "GET" "$base/api/auth/me" $null $token
    if ($rr.inst) { $seen += $rr.inst }
    Start-Sleep -Milliseconds 90
}
$insts = ($seen | Sort-Object -Unique)
V "A-10" "token valid across both instances" ($insts.Count -ge 2) ("instances=" + ($insts -join ",") + " samples=" + $seen.Count)

# --- A-11: swagger/openapi reachable without a token (whitelist evidence) ---
$r = Req "GET" "$base/v3/api-docs" $null $null
V "A-11" "openapi docs whitelisted" ($r.code -eq 200) ("GET /v3/api-docs -> " + $r.code)

# --- A-12: logout revokes the token (document written to mongo, then 401) ---
$before = MongoCount "auth_revoked"
$r = Req "POST" "$base/api/auth/logout" $null $token
$code12 = $r.code
$after = MongoCount "auth_revoked"
Start-Sleep -Milliseconds 300
$r2 = Req "GET" "$base/api/devices" $null $token
V "A-12" "logout revokes token" ($code12 -eq 200 -and $after -eq ($before + 1) -and $r2.code -eq 401) `
    ("logout=" + $code12 + " revokedDocs=" + $before + "->" + $after + " reuse=" + $r2.code)

# --- A-13: frontend build contains the login page and token handling ---
$dist = Join-Path (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)) "web\dist\assets"
$hitLogin = $false; $hitAuth = $false
if (Test-Path $dist) {
    $hitLogin = [bool](Select-String -Path (Join-Path $dist "*.js") -Pattern "/auth/login" -SimpleMatch -List -ErrorAction SilentlyContinue)
    $hitAuth = [bool](Select-String -Path (Join-Path $dist "*.js") -Pattern "inspection_token" -SimpleMatch -List -ErrorAction SilentlyContinue)
}
V "A-13" "frontend build has login page" ($hitLogin -and $hitAuth) ("dist assets: loginRef=" + $hitLogin + " tokenRef=" + $hitAuth)

Write-Output "===== S88 SUMMARY: PASS=$pass FAIL=$fail ====="
