// Init MongoDB single-node replica set rs0 (design doc 2.2.3 / 4.6.3)
// NOTE: keep this file pure ASCII — Chinese comments break the script when
// piped through docker exec under a GBK console (see STATE risk #16)
// Usage: docker exec -i mongodb mongosh < init/mongo-rs-init.js
try {
  rs.initiate({
    _id: "rs0",
    members: [{ _id: 0, host: "mongodb:27017" }]
  });
  print("[mongo-init] rs0 initiated");
} catch (e) {
  // re-running on an initialized set throws "already initialized" — expected & idempotent
  print("[mongo-init] maybe already initialized: " + e.message);
}
