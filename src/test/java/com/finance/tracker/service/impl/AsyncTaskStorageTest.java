package com.finance.tracker.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.finance.tracker.domain.AsyncTask;
import com.finance.tracker.domain.AsyncTaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AsyncTaskStorageTest {

    private AsyncTaskStorage storage;

    @BeforeEach
    void setUp() {
        storage = new AsyncTaskStorage();
    }

    @Test
    void saveTaskShouldStoreTaskById() {
        AsyncTask task = task("task-1");

        storage.saveTask(task);

        assertSame(task, storage.getTask("task-1"));
    }

    @Test
    void getAllTasksShouldReturnAllSavedTasks() {
        AsyncTask firstTask = task("task-1");
        AsyncTask secondTask = task("task-2");
        storage.saveTask(firstTask);
        storage.saveTask(secondTask);

        var tasks = storage.getAllTasks();

        assertEquals(2, tasks.size());
        assertSame(firstTask, tasks.get("task-1"));
        assertSame(secondTask, tasks.get("task-2"));
    }

    private AsyncTask task(String taskId) {
        return AsyncTask.builder()
                .taskId(taskId)
                .status(AsyncTaskStatus.PENDING)
                .progress(0)
                .build();
    }
}
