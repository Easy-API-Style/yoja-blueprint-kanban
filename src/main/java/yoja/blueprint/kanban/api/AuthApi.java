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
import com.easygoingapi.yoja.http.server.HttpResponse;
import com.easygoingapi.yoja.http.server.HttpRouting;
import com.easygoingapi.yoja.http.server.HttpSession;
import com.easygoingapi.yoja.http.server.WebService;

import io.vertx.core.json.JsonObject;

/**
 * Authentication endpoints for the demo app.
 * <p>
 * The example ships a single hard-coded {@code demo}/{@code demo} account; on
 * successful login the user name is stored in the Yoja {@link HttpSession}
 * keyed by {@code "user"}, and downstream endpoints (in {@link TaskApi}) read
 * that key to gate every task operation.
 * <p>
 * Each public factory returns a fresh {@link WebService} that the
 * {@link yoja.blueprint.kanban.Main} router builder wires up.
 */
public class AuthApi {

    /** Not instantiable; only static factories are exposed. */
    private AuthApi() {
        super();
    }

    /** Hard-coded demo user name. */
    private static final String USERNAME = "demo";
    /** Hard-coded demo password. */
    private static final String PASSWORD = "demo";

    /**
     * @return a {@code POST /api/login} service that accepts a JSON
     *         {@code {username, password}} body, stores the user name in the
     *         session on success and responds with {@code {user}}, or 401
     *         otherwise
     */
    public static WebService login() {
        return new WebService(HttpMethod.POST, "/api/login", routing -> {
            JsonObject body = routing.request().bodyAsJsonObject();
            if (USERNAME.equals(body.getString("username"))
                    && PASSWORD.equals(body.getString("password"))) {
                routing.session()
                       .put("user", USERNAME);
                routing.response()
                       .send(new JsonObject().put("user", USERNAME));
            }
            else {
                unauthorized(routing);
            }
        });
    }

    /**
     * @return a {@code GET /api/logout} service that destroys the current
     *         session (when any) and responds with {@code {ok: true}}
     */
    public static WebService logout() {
        return new WebService(HttpMethod.GET, "/api/logout", routing -> {
            HttpSession session = routing.session();
            if (session != null) {
                session.destroy();
            }
            routing.response()
                   .send(new JsonObject().put("ok", true));
        });
    }

    /**
     * @return a {@code GET /api/me} service that echoes the logged-in user
     *         name as {@code {user}}, or 401 when no session exists
     */
    public static WebService me() {
        return new WebService(HttpMethod.GET, "/api/me", routing -> {
            HttpSession session = routing.session();
            if (session != null
                    && session.get("user") != null) {
                routing.response()
                       .send(new JsonObject().put("user", session.get("user").toString()));
            }
            else {
                unauthorized(routing);
            }
        });
    }

    /**
     * Ends the response with {@code 401 Unauthorized} and the body
     * {@code "unauthorized"}. Used by both {@link #login()}, {@link #me()} and
     * the auth-handler installed in {@link yoja.blueprint.kanban.Main}
     * to gate {@link TaskApi} endpoints.
     *
     * @param httpRouting routing context to respond on
     */
    public static void unauthorized(HttpRouting httpRouting) {
        HttpResponse httpResponse = httpRouting.response();
        httpResponse.statusCode(401);
        httpResponse.send("unauthorized");
    }

}
