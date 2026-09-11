# 拉取六组件镜像：官方源直连优先（需 Docker Desktop 已配置代理，见 STATE D-17），
# docker.io 直连失败时自动降级国内镜像源 + 重标记（D-16）。
# 用法：在项目根目录执行  powershell -ExecutionPolicy Bypass -File docker\init\pull-images.ps1
$ErrorActionPreference = "Continue"
$mirrors = @("docker.1ms.run", "docker.xuanyuan.me", "docker.m.daocloud.io", "hub.rat.dev")
$dockerIoImages = @("apache/hadoop:3.3.6", "mongo:6.0", "apache/kafka:4.0.0", "nginx:1.25-alpine")

foreach ($img in $dockerIoImages) {
    $exists = docker image inspect $img 2>$null
    if ($LASTEXITCODE -eq 0) { Write-Output "Already exists, skip: $img"; continue }

    Write-Output "Try direct pull: $img"
    docker pull $img
    if ($LASTEXITCODE -eq 0) { Write-Output "OK (direct): $img"; continue }

    Write-Output "Direct pull failed, falling back to mirrors..."
    $ok = $false
    foreach ($m in $mirrors) {
        Write-Output "Try mirror $m/$img ..."
        docker pull "$m/$img"
        if ($LASTEXITCODE -eq 0) {
            docker tag "$m/$img" $img
            Write-Output "OK (via $m, retagged): $img"
            $ok = $true
            break
        }
        Write-Output "Failed, next mirror..."
    }
    if (-not $ok) { Write-Output "All sources failed: $img"; exit 1 }
}

Write-Output "Pull elastic official images (direct)..."
docker pull docker.elastic.co/elasticsearch/elasticsearch:8.13.0
docker pull docker.elastic.co/kibana/kibana:8.13.0

Write-Output ""
Write-Output "===== Image inventory ====="
docker images --format "{{.Repository}}:{{.Tag}}  {{.Size}}"
