// S93: delete ONLY test/stress-injected samples, keep all natural simulation data.
// ASCII-only (piped via stdin, STATE risk #16). Prints KEY=VALUE lines consumed by the PS wrapper.
var d = db.getSiblingDB('inspection');

function n(name, q) { return d.getCollection(name).countDocuments(q || {}); }
function show(tag) {
    print(tag + '.device_status=' + n('device_status'));
    print(tag + '.alarm=' + n('alarm'));
    print(tag + '.task=' + n('task'));
    print(tag + '.task_log=' + n('task_log'));
    print(tag + '.image_meta=' + n('image_meta'));
    print(tag + '.injected_ds=' + d.device_status.countDocuments(INJECTED_DS));
    print(tag + '.injected_alarm=' + d.alarm.countDocuments(INJECTED_ALARM));
    print(tag + '.script_task=' + d.task.countDocuments(SCRIPT_TASK));
}

var INJECTED_DS = { $or: [
    { altitude: 999.5 },                 // S86 stress marker (UAV)
    { irMaxTemp: 999.5 },                // S86 stress marker (ROBOT_DOG)
    { lng: 116.3974, lat: 39.9092, altitude: 80, speed: 12, battery: 80 }   // PT001/002/007 fingerprint
] };
var INJECTED_ALARM = { $or: [{ _id: /^v06-/ }, { _id: /^tc017-/ }] };
var SCRIPT_TASK = { remark: { $in: ['S33-accept', 'tc-p1', 'tc-p2', 'tc-p3', 'tc022', 'TC004', 'IT005', 'IT009', 'test', 'prio-1', 'prio-2', 'prio-3'] } };

print('=== BEFORE ===');
show('before');

// collect the injected alarm chain
var alarmIds = d.alarm.find(INJECTED_ALARM, { _id: 1 }).toArray().map(function (a) { return a._id; });
var paths = [];
d.alarm.find(INJECTED_ALARM).forEach(function (a) {
    if (a.snapshotPath) { paths.push(a.snapshotPath); }
    if (a.review && a.review.imagePath) { paths.push(a.review.imagePath); }
    if (a.review && a.review.manualPhotoPath) { paths.push(a.review.manualPhotoPath); }
});
var taskIds = d.task.find({ $or: [{ alarmId: { $in: alarmIds } }, SCRIPT_TASK] }, { _id: 1 }).toArray().map(function (t) { return t._id; });
print('target.alarms=' + alarmIds.length);
print('target.tasks=' + taskIds.length);
print('target.hdfs_paths=' + paths.length);
print('target.task_logs=' + d.task_log.countDocuments({ taskId: { $in: taskIds } }));

// image_meta: detect the path field actually used, then delete only rows pointing at deleted images
var sample = d.image_meta.findOne({});
print('image_meta.keys=' + (sample ? Object.keys(sample).join(',') : 'none'));
var pathFields = ['hdfsPath', 'path', 'filePath', 'storagePath', 'imagePath'];
var metaQuery = { $or: pathFields.map(function (f) { var q = {}; q[f] = { $in: paths }; return q; }) };
print('target.image_meta=' + d.image_meta.countDocuments(metaQuery));

// ---- delete (Mongo is authoritative; ES copies are removed by the PS wrapper) ----
var r1 = d.device_status.deleteMany(INJECTED_DS);
var r2 = d.alarm.deleteMany({ _id: { $in: alarmIds } });
var r3 = d.task_log.deleteMany({ taskId: { $in: taskIds } });
var r4 = d.task.deleteMany({ _id: { $in: taskIds } });
var r5 = d.image_meta.deleteMany(metaQuery);
print('deleted.device_status=' + r1.deletedCount);
print('deleted.alarm=' + r2.deletedCount);
print('deleted.task_log=' + r3.deletedCount);
print('deleted.task=' + r4.deletedCount);
print('deleted.image_meta=' + r5.deletedCount);

print('=== AFTER ===');
show('after');

// hand the HDFS paths to the wrapper (space separated, no spaces exist in these paths)
print('PATHS=' + paths.join(' '));
print('ALARMIDS=' + alarmIds.join(' '));
