package com.course.inspection.task;

import com.course.inspection.alarm.AlarmSearchService;
import com.course.inspection.alarm.AlarmStore;
import com.course.inspection.common.TaskLogMsg;
import com.course.inspection.device.DeviceDoc;
import com.course.inspection.storage.EvidenceImageGenerator;
import com.course.inspection.storage.HdfsClient;
import com.course.inspection.storage.PathBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.mongodb.client.result.UpdateResult;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** S77：任务状态机单测（Mockito 打桩 Mongo/Kafka，无容器依赖）。 */
class TaskServiceTest {

    private MongoTemplate mongo;
    private KafkaTemplate<String, String> kafka;
    private TaskService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        mongo = mock(MongoTemplate.class);
        kafka = mock(KafkaTemplate.class);
        service = new TaskService(mongo, kafka, mock(HdfsClient.class), new PathBuilder(),
                mock(EvidenceImageGenerator.class), mock(AlarmStore.class), mock(AlarmSearchService.class));
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
    }

    private DeviceDoc onlineRobot() {
        DeviceDoc d = new DeviceDoc();
        d.setDeviceId("ROBOT-001");
        d.setDeviceType("ROBOT_DOG");
        d.setStatus("ONLINE");
        return d;
    }

    private TaskDoc task(String id, String status) {
        TaskDoc t = new TaskDoc();
        t.setTaskId(id);
        t.setDeviceId("ROBOT-001");
        t.setTaskType("POINT_REVIEW");
        t.setStatus(status);
        return t;
    }

    @Test
    void createDispatchesToOnlineDevice() {
        when(mongo.findById("ROBOT-001", DeviceDoc.class)).thenReturn(onlineRobot());
        when(mongo.insert(any(TaskDoc.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskDoc doc = service.create(new TaskService.CreateRequest(
                "POINT_REVIEW", "ROBOT-001", 1, "单测", 116.397, 39.909));

        assertEquals("DISPATCHED", doc.getStatus());
        assertTrue(doc.getTaskId().startsWith("TASK-"));
        verify(kafka).send(eq("task.command"), eq("ROBOT-001"), anyString());
    }

    @Test
    void createRejectsOfflineDevice() {
        DeviceDoc offline = onlineRobot();
        offline.setStatus("OFFLINE");
        when(mongo.findById("ROBOT-001", DeviceDoc.class)).thenReturn(offline);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.create(new TaskService.CreateRequest(
                        "POINT_REVIEW", "ROBOT-001", 1, null, null, null)));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void cancelTwiceOnlyFirstSucceeds() {
        when(mongo.findById("TASK-1", TaskDoc.class))
                .thenReturn(task("TASK-1", "DISPATCHED"))
                .thenReturn(task("TASK-1", "CANCELLED"));
        when(mongo.findById("ROBOT-001", DeviceDoc.class)).thenReturn(onlineRobot());
        UpdateResult ok = mock(UpdateResult.class);
        when(ok.getModifiedCount()).thenReturn(1L);
        when(mongo.updateFirst(any(), any(Update.class), eq(TaskDoc.class))).thenReturn(ok);

        assertTrue(service.cancel("TASK-1"));     // 第一次：受理并同步置 CANCELLED（BUG-007 修复语义）
        assertFalse(service.cancel("TASK-1"));    // 第二次：状态已终态，拒绝
    }

    @Test
    void doneReceiptTransitionsAndSkipsReviewWithoutAlarmLink() {
        when(mongo.findById("TASK-2", TaskDoc.class)).thenReturn(task("TASK-2", "RUNNING"));
        UpdateResult ok = mock(UpdateResult.class);
        when(ok.getModifiedCount()).thenReturn(1L);
        when(mongo.updateFirst(any(), any(Update.class), eq(TaskDoc.class))).thenReturn(ok);

        service.applyReceipt(new TaskLogMsg("TASK-2", "ROBOT-001", "DONE", System.currentTimeMillis()));

        verify(mongo).insert(any(TaskLogDoc.class));   // 回执归档
        // POINT_REVIEW 且 alarmId 为 null：不触发复核回填（completeReview 早退），无 HDFS 交互
    }
}
