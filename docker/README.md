# 六组件 Docker 环境（S20）

> 依据：设计报告 4.6 节（部署拓扑图 4-3、容器清单表 4-2、关键部署决策 4.6.3、验证顺序 4.6.4）。
> 镜像版本与 `STATE.md` 术语注册表一致，改动版本必须先改注册表（治理纪律 §6.2）。

## 0. 首次拉取镜像（本机 Docker Hub DNS 被污染，必须走镜像源）

```powershell
# 本机默认 DNS 将 registry-1.docker.io 解析为 127.0.0.1（STATE 风险 #12），直接 compose up 会失败。
# 先经镜像源拉取并重标记为官方名（2026-09-11 实测可用源：docker.1ms.run / docker.xuanyuan.me / docker.m.daocloud.io / hub.rat.dev）
powershell -ExecutionPolicy Bypass -File init\pull-images.ps1
# ES/Kibana 官方源 docker.elastic.co 未受污染，脚本内直连拉取
```

## 1. 启动

```powershell
cd <项目根目录>/docker
docker compose up -d            # 镜像已就绪时不再触发拉取
docker compose ps               # 期望：六服务全部 healthy
```

> 首次启动 NameNode 会自动格式化（格式化守卫：`/data/name/current` 存在时跳过，防 P-2）。

## 2. 初始化（仅首次）

```powershell
# ① MongoDB 副本集
docker exec -i mongodb mongosh < init/mongo-rs-init.js

# ② Kafka 主题（8 个，分区数与 STATE 注册表一致）
bash init/kafka-topics.sh
# 或逐个执行脚本内命令；Windows 无 bash 时用 Git Bash / 逐条 docker exec
```

## 3. 验收步骤（设计报告 4.6.4 自下而上验证顺序）

```powershell
# ① 六组件健康
docker compose ps
# ② HDFS：上传/下载验证（WebHDFS 两步写入）
docker exec namenode bash -c 'echo "s20-check" > /tmp/s20.txt && hdfs dfs -put /tmp/s20.txt /s20-check.txt && hdfs dfs -cat /s20-check.txt'
# ③ MongoDB：副本集状态
docker exec mongodb mongosh --quiet --eval "rs.status().ok"
# ④ Kafka：主题列表 + 生产消费自检
docker exec kafka /opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
echo "ping" | docker exec -i kafka /opt/bitnami/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic device.heartbeat
docker exec kafka /opt/bitnami/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic device.heartbeat --from-beginning --max-messages 1 --timeout-ms 10000
# ⑤ ES：集群健康 + 建索引
curl.exe http://localhost:9200/_cluster/health
curl.exe -X PUT "http://localhost:9200/inspection_alarm_v1" -H "Content-Type: application/json" -d "{\"settings\":{\"number_of_shards\":1,\"number_of_replicas\":0},\"mappings\":{\"dynamic\":\"strict\",\"properties\":{\"alarmId\":{\"type\":\"keyword\"},\"deviceId\":{\"type\":\"keyword\"},\"location\":{\"type\":\"geo_point\"},\"occurredTime\":{\"type\":\"date\"}}}}"
# ⑥ Kibana 经 Nginx 网关可达
curl.exe http://localhost:8080/           # 网关存活页 OK
curl.exe -I http://localhost:8080/kibana/ # 200/302
```

> 沙箱环境下 docker 命令需升级授权执行；上述命令亦可在用户系统终端直接运行。

## 4. 常用运维

```powershell
docker compose logs -f <服务名>          # 看日志排错（治理纪律：先看日志再动手）
docker compose stop kibana               # 内存紧张时临时停 Kibana（D-14 预案）
docker compose down                      # 停止并删除容器（保留数据卷）
# 严禁：docker compose down -v            # 会删除全部数据卷（危险动作黑名单）
```
