// 初始化 MongoDB 单节点副本集 rs0（设计报告 2.2.3 / 4.6.3）
// 用法：docker exec -i mongodb mongosh < docker/init/mongo-rs-init.js
try {
  rs.initiate({
    _id: "rs0",
    members: [{ _id: 0, host: "mongodb:27017" }]
  });
  print("[mongo-init] 副本集 rs0 初始化成功");
} catch (e) {
  // 已初始化时重复执行会报 already initialized，属预期幂等行为
  print("[mongo-init] 副本集可能已初始化：", e.message);
}
