package com.course.inspection.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** S77：消息 JSON 序列化往返（含中文描述——口径：UTF-8 全链路）。 */
class JsonTest {

    @Test
    void alarmMsgRoundTripWithChinese() {
        AlarmMsg msg = new AlarmMsg("ALM-test01", "UAV-001", "UAV", "PERIMETER_BREACH",
                "CRITICAL", "周界检测到疑似人员活动", 116.397, 39.909, 1789200000000L);
        String json = Json.toJson(msg);
        AlarmMsg back = Json.fromJson(json, AlarmMsg.class);
        assertEquals(msg.alarmId(), back.alarmId());
        assertEquals(msg.description(), back.description());
        assertEquals(msg.lng(), back.lng(), 1e-9);
    }

    @Test
    void heartbeatRoundTripWithOptionalCoords() {
        HeartbeatMsg m = new HeartbeatMsg("ROBOT-001", "ROBOT_DOG", 80, null, 116.398, 39.909, 1789200000000L);
        HeartbeatMsg back = Json.fromJson(Json.toJson(m), HeartbeatMsg.class);
        assertEquals(m.deviceId(), back.deviceId());
        assertEquals(m.lng(), back.lng(), 1e-9);
        assertEquals(m.battery(), back.battery());
    }
}
