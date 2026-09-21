// S96 probe: how many alarms actually carry review images / manual photos?
var d = db.getSiblingDB('inspection');
print('alarm.total=' + d.alarm.countDocuments({}));
print('review.conclusion=' + d.alarm.countDocuments({ 'review.conclusion': { $exists: true } }));
print('review.imagePath=' + d.alarm.countDocuments({ 'review.imagePath': { $exists: true, $ne: null } }));
print('review.manualPhotoPath=' + d.alarm.countDocuments({ 'review.manualPhotoPath': { $exists: true, $ne: null } }));
print('snapshotPath=' + d.alarm.countDocuments({ snapshotPath: { $exists: true, $ne: null } }));
var a = d.alarm.find({ 'review.imagePath': { $exists: true, $ne: null } }).sort({ occurredTime: -1 }).limit(3).toArray();
a.forEach(function (x) { print('  sample ' + x._id + ' img=' + x.review.imagePath + ' status=' + x.status); });
var b = d.alarm.find({ 'review.conclusion': { $exists: true } }).sort({ occurredTime: -1 }).limit(3).toArray();
b.forEach(function (x) {
    print('  reviewed ' + x._id + ' status=' + x.status + ' concl=' + x.review.conclusion + ' img=' + (x.review.imagePath || '-') + ' reviewer=' + x.review.reviewerDeviceId);
});
