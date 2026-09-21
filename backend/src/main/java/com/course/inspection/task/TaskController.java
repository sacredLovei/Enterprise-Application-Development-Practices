package com.course.inspection.task;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** 任务接口（设计报告 4.4.3 契约）。 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @PostMapping
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    public TaskDoc create(@jakarta.validation.Valid @RequestBody TaskService.CreateRequest req) {
        return service.create(req);
    }

    @GetMapping
    public TaskService.TaskPage list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return service.list(page, size);
    }

    @PostMapping("/{taskId}/cancel")
    public String cancel(@PathVariable String taskId) {
        if (!service.cancel(taskId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "task not cancelable: " + taskId);
        }
        return "cancelled: " + taskId;
    }

    /** S85（课程任务项 2）：任务执行日志归档查询——按时间升序返回该任务的全部回执记录。 */
    @GetMapping("/{taskId}/logs")
    public java.util.List<TaskLogDoc> logs(@PathVariable String taskId) {
        return service.logs(taskId);
    }
}
