# S30 验收脚本（可重复执行，S50 回归测试复用）
# 覆盖：告警三写（Mongo/ES/HDFS）、离线检测、幂等重放、死信投递
Write-Output "=== 1) 基线计数 ==="
docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('inspection').alarm.countDocuments({})" 2>&1
docker exec elasticsearch curl -s "http://localhost:9200/inspection_alarm_v1/_count" 2>&1

Write-Output "=== 2) 触发告警：OVERHEAT(dog-sim-1) + COMM_OFFLINE(uav-sim-2) ==="
docker exec nginx wget -qO- --post-data="" "http://dog-sim-1:8080/sim/fault?type=OVERHEAT" 2>&1
docker exec nginx wget -qO- --post-data="" "http://uav-sim-2:8080/sim/fault?type=COMM_OFFLINE" 2>&1
Start-Sleep -Seconds 25

Write-Output "=== 3) Mongo alarm 最近 6 条 ==="
docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('inspection').alarm.find({},{_id:1,alarmType:1,status:1,snapshotPath:1}).sort({occurredTime:-1}).limit(6).toArray()" 2>&1

Write-Output "=== 4) UAV-002 设备状态（期望 OFFLINE）==="
docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('inspection').device.findOne({_id:'UAV-002'},{_id:1,status:1})" 2>&1

Write-Output "=== 5) HDFS 证据图目录 ==="
docker exec namenode bash -c "hdfs dfs -ls -R /inspection 2>/dev/null | tail -6" 2>&1

Write-Output "=== 6) ES 计数（应增长）==="
docker exec elasticsearch curl -s "http://localhost:9200/inspection_alarm_v1/_count" 2>&1

Write-Output "=== 7) 幂等重放 ==="
$one = docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('inspection').alarm.findOne({},{_id:1})._id" 2>&1
$one = ($one | Select-Object -Last 1).Trim()
Write-Output "重放 alarmId: $one"
if ($one -match 'ALM-') {
  $payload = '{"alarmId":"' + $one + '","deviceId":"UAV-001","deviceType":"UAV","alarmType":"PERIMETER_BREACH","level":"CRITICAL","description":"replay","lng":116.397,"lat":39.909,"occurredTime":1789173400000}'
  $payload | docker exec -i kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic inspection.alarm 2>&1 | Out-Null
  Start-Sleep -Seconds 8
  Write-Output "重放后 Mongo 计数（应与基线一致）:"
  docker exec mongodb mongosh --quiet --eval "db.getSiblingDB('inspection').alarm.countDocuments({})" 2>&1
}

Write-Output "=== 8) 死信：非法消息 → inspection.dlq ==="
"this-is-not-json" | docker exec -i kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic inspection.alarm 2>&1 | Out-Null
Start-Sleep -Seconds 20
docker exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic inspection.dlq --from-beginning --max-messages 2 --timeout-ms 8000 2>&1
