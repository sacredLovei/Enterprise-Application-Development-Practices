# Compute JMeter JTL statistics: exact percentiles via streaming histogram (ASCII-only).
# Usage: powershell -File jtl-stats.ps1 <jtl> [<jtl2> ...]
$Paths = $args
if ($Paths.Count -eq 0) { Write-Output "usage: jtl-stats.ps1 <jtl>..."; exit 1 }

foreach ($Path in $Paths) {
    Write-Output "===== $Path ====="
    if (-not (Test-Path $Path)) { Write-Output "MISSING"; continue }
    $total = [int64]0; $err = [int64]0; $min = [long]::MaxValue; $max = [long]0; $sum = [double]0
    $hist = New-Object int[] 100001   # 1ms bins, elapsed >= 100s clamped to last bin
    $reader = [System.IO.StreamReader]::new($Path)
    $null = $reader.ReadLine()        # CSV header
    while (($line = $reader.ReadLine()) -ne $null) {
        $cols = $line.Split(',')
        if ($cols.Count -lt 8) { continue }
        $e = [long]$cols[1]
        $total++
        $sum += $e
        if ($e -lt $min) { $min = $e }
        if ($e -gt $max) { $max = $e }
        if ($cols[7] -eq 'false') { $err++ }
        if ($e -ge $hist.Length) { $e = $hist.Length - 1 }
        $hist[$e]++
    }
    $reader.Close()
    if ($total -eq 0) { Write-Output "EMPTY"; continue }
    $needP50 = [int64]([math]::Ceiling($total * 0.50))
    $needP90 = [int64]([math]::Ceiling($total * 0.90))
    $needP95 = [int64]([math]::Ceiling($total * 0.95))
    $needP99 = [int64]([math]::Ceiling($total * 0.99))
    $acc = [int64]0; $p50 = $max; $p90 = $max; $p95 = $max; $p99 = $max
    for ($i = 0; $i -lt $hist.Length; $i++) {
        $acc += $hist[$i]
        if ($p50 -eq $max -and $acc -ge $needP50) { $p50 = $i }
        if ($p90 -eq $max -and $acc -ge $needP90) { $p90 = $i }
        if ($p95 -eq $max -and $acc -ge $needP95) { $p95 = $i }
        if ($p99 -eq $max -and $acc -ge $needP99) { $p99 = $i }
    }
    Write-Output ("samples=" + $total + " errors=" + $err + " errPct=" + [math]::Round(100.0 * $err / $total, 4) + "%")
    Write-Output ("avg=" + [math]::Round($sum / $total, 1) + "ms  min=" + $min + "ms  max=" + $max + "ms")
    Write-Output ("p50=" + $p50 + "ms  p90=" + $p90 + "ms  p95=" + $p95 + "ms  p99=" + $p99 + "ms")
}
