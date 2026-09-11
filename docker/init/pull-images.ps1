# 经国内镜像源拉取 docker.io 镜像并重标记为官方名
# 背景：本机默认 DNS 将 registry-1.docker.io 污染为 127.0.0.1/::1（STATE 风险 #9/#12），
#       镜像源通道实测可用（2026-09-11 实测：1ms/xuanyuan/daocloud/rat 均通，dockerproxy.net 超时）
# 用法：在项目根目录执行  powershell -ExecutionPolicy Bypass -File docker\init\pull-images.ps1
$ErrorActionPreference = "Continue"
$mirrors = @("docker.1ms.run", "docker.xuanyuan.me", "docker.m.daocloud.io", "hub.rat.dev")
$dockerIoImages = @("apache/hadoop:3.3.6", "mongo:6.0", "bitnami/kafka:3.6", "nginx:1.25-alpine")

foreach ($img in $dockerIoImages) {
    $exists = docker image inspect $img 2>$null
    if ($LASTEXITCODE -eq 0) { Write-Output "已存在，跳过: $img"; continue }
    $ok = $false
    foreach ($m in $mirrors) {
        Write-Output "尝试拉取 $m/$img ..."
        docker pull "$m/$img" 2>&1 | Select-Object -Last 1
        if ($LASTEXITCODE -eq 0) {
            docker tag "$m/$img" $img
            Write-Output "OK（经 $m 拉取并重标记）: $img"
            $ok = $true
            break
        }
        Write-Output "失败，换下一个源: $m"
    }
    if (-not $ok) { Write-Output "全部镜像源均失败: $img"; exit 1 }
}

# elastic 官方源未受 DNS 污染，直连拉取（2026-09-11 实测通过）
Write-Output "拉取 elastic 官方源镜像（直连）..."
docker pull docker.elastic.co/elasticsearch/elasticsearch:8.13.0 2>&1 | Select-Object -Last 1
docker pull docker.elastic.co/kibana/kibana:8.13.0 2>&1 | Select-Object -Last 1

Write-Output ""
Write-Output "===== 镜像就绪清单 ====="
docker images --format "{{.Repository}}:{{.Tag}}  {{.Size}}"
