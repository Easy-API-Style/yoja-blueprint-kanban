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

import yoja.blueprint.kanban.Main;

/**
 * Minimal demo of the two ES6 module entry points exposed by yoja-selenium:
 *
 *   - testModule       → sync: imports the module and calls module.default(arguments).
 *   - testAsyncModule  → async: imports the module and calls
 *                        module.default(args, resolve, reject).
 *
 * Each method targets a single default export, which is the difference with
 * testJsUnit (multiple named exports per file).
 */
public class TestModuleDemoTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestModuleDemoTest.class);
    private static final int PORT = 9092;
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
    Stream<DynamicNode> testModuleDemo() {
        return TestBuilder.builder()
            .browser(Browser.builder(Browser.CHROME)
                            .mode(Browser.Mode.HEADLESS)
                            .build())
            // 1. Open the running app so module imports and fetches resolve
            //    against http://localhost:9092.
            .test("open login page",
                  ctx -> ctx.seleniumService().getHttpPage(INDEX_URL))
            // 2. Inject ywAssert so the default exports below can use it.
            .loadYwAssert()
            // 3. Sync module — module.default(arguments) is called immediately;
            //    a throw fails this step.
            .testModule("/moduleSyncTest.js")
            // 4. Async module — module.default(args, resolve, reject) is called;
            //    the step ends only when resolve() or reject() fires.
            .testAsyncModule("/moduleAsyncTest.js")
            .stream();
    }
    
}
