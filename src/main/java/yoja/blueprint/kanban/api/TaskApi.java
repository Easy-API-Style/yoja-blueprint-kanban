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
package yoja.blueprint.kanban.api;

import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.http.server.HttpRouting;
import com.easygoingapi.yoja.http.server.WebService;
import com.easygoingapi.yoja.http.server.WebSocket;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import yoja.blueprint.kanban.model.Task;
import yoja.blueprint.kanban.service.TaskService;

/**
 * REST endpoints for the task board, all gated by the auth handler installed
 * in {@link yoja.blueprint.kanban.Main}.
 * <p>
 * Mutations ({@link #create}, {@link #update}, {@link #delete}) also broadcast
 * a {@code {event, task | id}} payload over the {@code /ws/tasks} WebSocket
 * so every connected client can refresh its UI without polling.
 */
public class TaskApi {

    /** Not instantiable; only static factories are exposed. */
    private TaskApi() {
        super();
    }

    /**
     * @param taskService   backing store
     * @param authHandler   auth gate inserted before the route handler
     * @return a {@code GET /api/tasks} service returning the full task list as JSON
     */
    public static WebService getAll(TaskService taskService,
                                    Handler<HttpRouting> authHandler) {
        return new WebService(HttpMethod.GET, "/api/tasks", authHandler, routing ->
            routing.response().send(taskService.toJsonArray())
        );
    }

    /**
     * @param taskService    backing store
     * @param taskWebSocket  WebSocket endpoint to broadcast the {@code "created"} event on
     * @param authHandler    auth gate
     * @return a {@code POST /api/tasks} service that creates a task from the
     *         JSON {@code {title, assignee}} body, returns {@code 201} with
     *         the new task as JSON, and broadcasts the event
     */
    public static WebService create(TaskService taskService,
                                    WebSocket taskWebSocket,
                                    Handler<HttpRouting> authHandler) {
        return new WebService(HttpMethod.POST, "/api/tasks", authHandler, routing -> {
            JsonObject body = routing.request().bodyAsJsonObject();
            Task task = taskService.add(body.getString("title", ""),
                                        body.getString("assignee", ""));
            taskWebSocket.send(new JsonObject().put("event", "created")
                                               .put("task",  task.toJson())
                                               .encode());
            routing.response()
                   .statusCode(201);
            routing.response()
                   .send(task.toJson());
        });
    }

    /**
     * @param taskService    backing store
     * @param taskWebSocket  WebSocket endpoint to broadcast the {@code "updated"} event on
     * @param authHandler    auth gate
     * @return a {@code POST /api/tasks/:id/status} service that mutates the
     *         status field of the matching task; returns 404 when the task
     *         does not exist
     */
    public static WebService update(TaskService taskService,
                                    WebSocket taskWebSocket,
                                    Handler<HttpRouting> authHandler) {
        return new WebService(HttpMethod.POST, "/api/tasks/:id/status", authHandler, routing -> {
            String id = routing.request().firstParameter("id");
            JsonObject body = routing.request().bodyAsJsonObject();
            Task updated = taskService.updateStatus(id, body.getString("status"));
            if (updated == null) {
                routing.fail(404);
                return;
            }
            taskWebSocket.send(new JsonObject().put("event", "updated")
                                               .put("task",  updated.toJson())
                                               .encode());
            routing.response()
                   .send(updated.toJson());
        });
    }

    /**
     * @param taskService    backing store
     * @param taskWebSocket  WebSocket endpoint to broadcast the {@code "deleted"} event on
     * @param authHandler    auth gate
     * @return a {@code GET /api/tasks/:id/delete} service that removes the
     *         matching task; returns 404 when the task does not exist
     */
    public static WebService delete(TaskService taskService,
                                    WebSocket taskWebSocket,
                                    Handler<HttpRouting> authHandler) {
        return new WebService(HttpMethod.GET, "/api/tasks/:id/delete", authHandler, routing -> {
            String id = routing.request().firstParameter("id");
            boolean deleted = taskService.delete(id);
            if (!deleted) {
                routing.fail(404);
                return;
            }
            taskWebSocket.send(new JsonObject().put("event", "deleted")
                                               .put("id",    id)
                                               .encode());
            routing.response()
                   .send(new JsonObject().put("ok", true));
        });
    }

    /**
     * @param taskService backing store
     * @param authHandler auth gate
     * @return a {@code GET /api/tasks/:id} service returning the matching
     *         task as JSON, or 404 when none
     */
    public static WebService getById(TaskService taskService,
                                     Handler<HttpRouting> authHandler) {
        return new WebService(HttpMethod.GET, "/api/tasks/:id", authHandler, routing -> {
            String id = routing.request().firstParameter("id");
            Task task = taskService.findById(id);
            if (task == null) {
                routing.fail(404);
                return;
            }
            routing.response().send(task.toJson());
        });
    }

    /**
     * @param taskService backing store
     * @param authHandler auth gate
     * @return a {@code GET /api/tasks/export} service that triggers an
     *         asynchronous CSV export on a worker thread and immediately
     *         responds with {@code {status: "export started"}}
     */
    public static WebService exportCsv(TaskService taskService,
                                       Handler<HttpRouting> authHandler) {
        return new WebService(HttpMethod.GET, "/api/tasks/export", authHandler, routing -> {
            taskService.exportCsv();
            routing.response()
                   .send(new JsonObject().put("status", "export started"));
        });
    }

}
