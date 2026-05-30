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
package yoja.blueprint.kanban.model;

import java.util.UUID;

import io.vertx.core.json.JsonObject;

/**
 * Immutable value carrying the state of a task on the demo board.
 *
 * @param id       opaque UUID identifying the task
 * @param title    short human-readable label
 * @param status   one of {@code "todo"}, {@code "in-progress"}, {@code "done"}
 * @param assignee free-text owner; defaults to {@code "unassigned"} on creation when blank
 */
public record Task(String id, String title, String status, String assignee) {

    /**
     * Creates a brand-new task with a freshly allocated UUID and the initial
     * {@code "todo"} status.
     *
     * @param title    short label
     * @param assignee owner (may be empty)
     * @return the freshly built task
     */
    public static Task of(String title, String assignee) {
        return new Task(UUID.randomUUID().toString(), title, "todo", assignee);
    }

    /**
     * Returns a copy of this task with the supplied status, keeping every
     * other field unchanged.
     *
     * @param status new status (typically {@code "in-progress"} or {@code "done"})
     * @return the updated copy
     */
    public Task withStatus(String status) {
        return new Task(id, title, status, assignee);
    }

    /**
     * Serializes the task as the JSON shape the REST API and WebSocket events
     * carry: {@code {id, title, status, assignee}}.
     *
     * @return the JSON representation
     */
    public JsonObject toJson() {
        return new JsonObject().put("id", id)
                               .put("title", title)
                               .put("status", status)
                               .put("assignee", assignee);
    }

}
