#!/bin/bash
# 创建全部业务主题（分区数/副本数与 STATE 注册表、设计报告 4.6.4 一致）
# 用法：在项目根目录执行 bash docker/init/kafka-topics.sh
set -e

BOOTSTRAP="localhost:9092"

create_topic() {
  local name=$1 partitions=$2
  echo "[kafka-init] 创建主题 ${name}（${partitions} 分区）"
  docker exec kafka /opt/bitnami/kafka/bin/kafka-topics.sh \
    --bootstrap-server ${BOOTSTRAP} \
    --create --if-not-exists \
    --topic ${name} --partitions ${partitions} --replication-factor 1
}

# 上行主题
create_topic uav.telemetry        3
create_topic robot.telemetry      2
create_topic device.heartbeat     3
create_topic inspection.alarm     3
create_topic inspection.image.meta 2
# 下行与回执主题
create_topic task.command         2
create_topic task.log             2
# 死信主题
create_topic inspection.dlq       1

echo "[kafka-init] 主题创建完毕，当前主题列表："
docker exec kafka /opt/bitnami/kafka/bin/kafka-topics.sh \
  --bootstrap-server ${BOOTSTRAP} --list
