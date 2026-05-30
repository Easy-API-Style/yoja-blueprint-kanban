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
package yoja.blueprint.kanban;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.YojaApp;
import com.easygoingapi.yoja.core.http.HttpMethod;
import com.easygoingapi.yoja.core.worker.Timer;
import com.easygoingapi.yoja.http.server.HttpRouter;
import com.easygoingapi.yoja.http.server.HttpRouting;
import com.easygoingapi.yoja.http.server.HttpServer;
import com.easygoingapi.yoja.http.server.HttpSessionStore;
import com.easygoingapi.yoja.http.server.WebApp;
import com.easygoingapi.yoja.http.server.WebService;
import com.easygoingapi.yoja.http.server.WebSocket;
import com.easygoingapi.yoja.http.server.WebSocketService;

import io.vertx.core.Handler;
import yoja.blueprint.kanban.api.AuthApi;
import yoja.blueprint.kanban.api.TaskApi;
import yoja.blueprint.kanban.service.QuoteService;
import yoja.blueprint.kanban.service.TaskService;

/**
 * Entry point of the Yoja example app: a minimal task-board backed by an
 * in-memory store, exposing a REST API, a WebSocket broadcaster, and serving
 * two web apps from the classpath (the Yoja-Web runtime under {@code /yoja}
 * and the example webapp under {@code /}).
 * <p>
 * The {@link #main(String[])} entry point parses two CLI flags
 * ({@code -p <port>} and {@code -ssl <true|false>}), boots the Yoja runtime
 * and delegates to {@link #start(int, boolean)}, which wires everything
 * together. Demo credentials are printed to the log on startup
 * ({@code login: demo} / {@code password: demo}).
 * <p>
 * Architectural highlights demonstrated here:
 * <ul>
 *   <li>session-cookie authentication ({@code SID}, 30-minute idle timeout)
 *       gating every {@code TaskApi} endpoint via a single auth handler;</li>
 *   <li>WebSocket fan-out: each task mutation echoes a {@code {event, task}}
 *       payload to every client subscribed to {@code /ws/tasks};</li>
 *   <li>a recurring {@code Timer} emitting board statistics every 30 seconds;</li>
 *   <li>cross-cutting response header set on every reply ({@code X-Powered-By: yoja}).</li>
 * </ul>
 */
public class Main {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    /** Not instantiable; only the {@link #main(String[])} entry point is exposed. */
    private Main() {
        super();
    }

    /**
     * Parses the {@code -p <port>} / {@code -ssl <true|false>} flags, boots
     * the Yoja runtime via {@link YojaApp#start()} and starts the example
     * server; logs the public URL and the demo credentials on success.
     *
     * @param args CLI arguments
     */
    public static void main(String[] args) {
        int port = 8080;
        final AtomicBoolean ssl = new AtomicBoolean(false);
        for (int i = 0; i < args.length - 1; i++) {
            if ("-p".equals(args[i])) {
                port = Integer.parseInt(args[i + 1]);
            }
            if ("-ssl".equals(args[i])) {
                ssl.set(Boolean.parseBoolean(args[i + 1]));
            }
        }
        start(port, ssl.get())
            .onSuccess(server -> {
                LOGGER.info("Yoja kanban — {}://localhost:{}",
                            ssl.get() ? "https" : "http",
                            server.port());
                LOGGER.info("Yoja Task Manager\n    login:demo\n password:demo");
            })
            .onFailure(Throwable::printStackTrace);
    }

    /**
     * Wires the services, router, session store, WebSocket dispatcher and
     * starts the {@link HttpServer} on the given port (with a self-signed
     * certificate when {@code ssl} is {@code true}).
     * <p>
     * Layout of the assembled router:
     * <ul>
     *   <li>session handler bound to the {@code SID} cookie (30 minutes);</li>
     *   <li>extension-driven Content-Type mappings for {@code .js}, {@code .css},
     *       {@code .html}, {@code .xml};</li>
     *   <li>two classpath web apps: the Yoja-Web runtime under {@code /yoja/*}
     *       and the example UI under {@code /*};</li>
     *   <li>three auth endpoints from {@link AuthApi};</li>
     *   <li>a {@code /api/quote} endpoint backed by {@link QuoteService};</li>
     *   <li>six task endpoints from {@link TaskApi}, all gated by an auth
     *       handler that fails with 401 when no session user is present;</li>
     *   <li>a response hook stamping {@code X-Powered-By: yoja} on every reply.</li>
     * </ul>
     * The {@link WebSocketService} carries a single endpoint {@code /ws/tasks}
     * used by the task mutations to broadcast events.
     *
     * @param port TCP port to bind
     * @param ssl  whether to enable TLS with a self-signed certificate
     * @return a future resolving to the started server
     */
    public static io.vertx.core.Future<HttpServer> start(int port, boolean ssl) {
        TaskService taskService   = new TaskService();
        QuoteService quoteService = new QuoteService();
        WebSocket taskWebSocket   = new WebSocket("/ws/tasks");
        WebSocketService webSocketService = new WebSocketService();
        webSocketService.add(taskWebSocket);
        HttpSessionStore httpSessionStore = new HttpSessionStore("SID", Duration.ofMinutes(30));
        Timer.schedule("logs", t -> taskService.logStats())
             .period(Duration.ofSeconds(30))
             .build();
        Handler<HttpRouting> authHandler = routing -> {
            if (routing.session() == null
                    || routing.session().get("user") == null) {
                AuthApi.unauthorized(routing);
            }
            routing.nextHandler();
        };
        HttpRouter router =
            HttpRouter.builder()
                      .session(httpSessionStore)
                      .contentType("js",  "application/javascript")
                      .contentType("css",  "text/css")
                      .contentType("html", "text/html")
                      .contentType("xml",  "application/xml")
                      .webResource(WebApp.of(WebApp.Type.jar, "com.easygoingapi.yoja.web", "/yoja"), "/*")
                      .webResource(WebApp.jar("yoja.blueprint.kanban.webapp"), "/*")
                      .webService(HttpMethod.GET, "/favicon.ico", r -> r.response())
                      .webService(HttpMethod.GET, "/", r -> r.redirect("/index.html"))
                      .webService(AuthApi.login())
                      .webService(AuthApi.logout())
                      .webService(AuthApi.me())
                      .webService(new WebService(HttpMethod.GET, "/api/quote", routing ->
                          quoteService.fetch()
                                      .onSuccess(json -> routing.response().send(json))
                                      .onFailure(e -> routing.response().send(QuoteService.FALLBACK))
                      ))
                      .webService(TaskApi.getAll(taskService, authHandler))
                      .webService(TaskApi.create(taskService, taskWebSocket, authHandler))
                      .webService(TaskApi.update(taskService, taskWebSocket, authHandler))
                      .webService(TaskApi.delete(taskService, taskWebSocket, authHandler))
                      .webService(TaskApi.exportCsv(taskService, authHandler))
                      .webService(TaskApi.getById(taskService, authHandler))
                      .onResponse(event -> event.putHeader("X-Powered-By", "yoja"))
                      .build();
        HttpServer.Builder builder = HttpServer.builder(router, port)
                                               .webSocketService(webSocketService);
        if (ssl) {
            builder.sslSelfSigned();
        }
        return builder.start();
    }

}
