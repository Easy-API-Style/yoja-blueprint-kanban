/*
 * Copyright 2026 easy api <easy.api.contact@gmail.com>
 * https://easygoingapi.com
 * https://github.com/Easy-API-Style/yoja-framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package yoja.blueprint.kanban.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.worker.Worker;

import io.vertx.core.json.JsonArray;
import yoja.blueprint.kanban.model.Task;

/**
 * In-memory CRUD store for {@link Task} entries plus two side-effect helpers
 * that double as demonstrations of the framework's worker model:
 * <ul>
 *   <li>{@link #logStats()} is scheduled by {@link yoja.blueprint.kanban.Main}
 *       through a {@code Timer} every 30 seconds — it runs on the event loop and
 *       only emits log lines, so no blocking is involved;</li>
 *   <li>{@link #exportCsv()} offloads filesystem I/O to a {@code Worker.singleThread}
 *       so the event loop is never blocked by disk writes.</li>
 * </ul>
 * The backing list is a {@link CopyOnWriteArrayList} so that the event loop
 * can iterate it (e.g. through {@link #toJsonArray()}) while WebSocket
 * broadcasts mutate it from another handler.
 * <p>
 * Seeded with three example tasks at construction time so the UI has something
 * to display on first login.
 */
public class TaskService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskService.class);

    /** Thread-safe live task list (snapshot-on-write semantics). */
    private final List<Task> tasks = new CopyOnWriteArrayList<>();

    /** Builds the service and seeds it with three sample tasks. */
    public TaskService() {
        tasks.add(Task.of("Setup project structure",
                          "Alice"));
        tasks.add(new Task(UUID.randomUUID().toString(),
                           "Write REST API",
                           "in-progress",
                           "Bob"));
        tasks.add(new Task(UUID.randomUUID().toString(),
                           "Deploy to server",
                           "done",
                           "Alice"));
    }

    /** @return an unmodifiable snapshot of every task. */
    public List<Task> findAll() {
        return List.copyOf(tasks);
    }

    /**
     * @param id task identifier
     * @return the matching task, or {@code null} when none
     */
    public Task findById(String id) {
        return tasks.stream()
                    .filter(t -> t.id().equals(id))
                    .findFirst()
                    .orElse(null);
    }

    /**
     * Adds a new task to the store. A blank assignee is rewritten to
     * {@code "unassigned"} so the UI always has a non-empty value to render.
     *
     * @param title    task title
     * @param assignee task assignee (may be blank)
     * @return the newly created task
     */
    public Task add(String title,
                    String assignee) {
        Task task = Task.of(title, assignee.isBlank() ? "unassigned" : assignee);
        tasks.add(task);
        return task;
    }

    /**
     * Replaces the status of the task with the given id; no-op when the task
     * does not exist.
     *
     * @param id     task identifier
     * @param status new status
     * @return the updated task, or {@code null} when none was found
     */
    public Task updateStatus(String id,
                             String status) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).id().equals(id)) {
                Task updated = tasks.get(i).withStatus(status);
                tasks.set(i, updated);
                return updated;
            }
        }
        return null;
    }

    /**
     * Removes the task with the given id.
     *
     * @param id task identifier
     * @return {@code true} when a task was removed, {@code false} otherwise
     */
    public boolean delete(String id) {
        return tasks.removeIf(t -> t.id().equals(id));
    }

    /** @return the entire task list serialized as a {@link JsonArray} of {@link Task#toJson()} entries. */
    public JsonArray toJsonArray() {
        JsonArray arr = new JsonArray();
        tasks.forEach(t -> arr.add(t.toJson()));
        return arr;
    }

    /**
     * Logs a one-line summary of how many tasks sit in each status bucket.
     * Called by {@link yoja.blueprint.kanban.Main}'s {@code Timer}
     * every 30 seconds; runs on the event loop and only emits log lines.
     */
    public void logStats() {
        long todo = tasks.stream().filter(t -> "todo".equals(t.status())).count();
        long inProgress = tasks.stream().filter(t -> "in-progress".equals(t.status())).count();
        long done = tasks.stream().filter(t -> "done".equals(t.status())).count();
        LOGGER.info("Tasks — todo: {}, in-progress: {}, done: {}", todo, inProgress, done);
    }

    /**
     * Writes the task list to {@code build/tasks-export.csv} on a worker
     * thread so the event loop is never blocked by filesystem I/O. The work is
     * keyed by the string {@code "csv-export"} so concurrent triggers
     * deduplicate.
     */
    public void exportCsv() {
        Worker.singleThread.once("csv-export", () -> {
            StringBuilder sb = new StringBuilder("id,title,status,assignee\n");
            tasks.forEach(t -> sb.append(t.id())      .append(",")
                                 .append(t.title())   .append(",")
                                 .append(t.status())  .append(",")
                                 .append(t.assignee()).append("\n"));
            try {
                Path csvFile = Path.of("build/tasks-export.csv");
                Files.createDirectories(csvFile.getParent());
                Files.writeString(csvFile, sb.toString());
                LOGGER.info("CSV exported to " + csvFile.toAbsolutePath().toString());
            }
            catch (Exception e) {
                LOGGER.error("CSV export failed", e);
            }
        });
    }

}
