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

import static com.easygoingapi.yoja.core.util.FutureUtil.await;
import static com.easygoingapi.yoja.core.util.FutureUtil.awaitValue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.http.server.HttpServer;
import com.easygoingapi.yoja.selenium.Browser;
import com.easygoingapi.yoja.selenium.TestBuilder;

/**
 * Minimal demo of the two JS-side test mechanisms exposed by yoja-selenium:
 *
 *  - testJsUnit       → synchronous: each named export is called as a test step.
 *  - testAsyncModule  → asynchronous: the default export receives (args, resolve, reject).
 *
 * Both rely on {@code ywAssert} being loaded into the page beforehand.
 */
public class JsUnitDemoTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsUnitDemoTest.class);
    private static final int    PORT = 9091;
    private static final String INDEX_URL = "http://localhost:" + PORT + "/index.html";

    private static HttpServer server;

    /** Starts the example app once for the whole class. */
    @BeforeAll
    static void startApp() {
        server = awaitValue(Main.start(PORT, false)
                                .onFailure(e -> LOGGER.error("Server start failed", e)));
    }

    /** Stops the shared server once every dynamic test has run. */
    @AfterAll
    static void stopApp() {
        if (server != null) {
            await(server.stop());
        }
    }

    /**
     * @return the JUnit dynamic-test stream produced by the {@link TestBuilder}
     *         scenario described in this class' Javadoc
     */
    @TestFactory
    Stream<DynamicNode> jsUnitDemo() {
        return TestBuilder.builder()
            .browser(Browser.builder(Browser.CHROME)
                            .mode(Browser.Mode.HEADLESS)
                            .build())
            // 1. Open the running app so all imports/fetches resolve against
            //    http://localhost:9091.
            .test("open login page",
                  ctx -> ctx.seleniumService().getHttpPage(INDEX_URL))
            // 2. Inject ywAssert into the page (required by both JS modules).
            .loadYwAssert()
            // 3. Synchronous jsUnit — runs each named export back-to-back.
            //    A throw inside a function fails just that step.
            .testJsUnit("/jsUnitSyncTest.js",
                        List.of("titleIsTaskManager", "loginFormIsPresent"))
            .stream();
    }
    
}
