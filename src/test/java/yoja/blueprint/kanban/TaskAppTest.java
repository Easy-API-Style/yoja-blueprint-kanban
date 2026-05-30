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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.core.http.HttpProtocole;
import com.easygoingapi.yoja.core.http.HttpUrl;
import com.easygoingapi.yoja.http.server.HttpServer;
import com.easygoingapi.yoja.selenium.Browser;
import com.easygoingapi.yoja.selenium.SeleniumService;

import yoja.blueprint.kanban.Main;

/**
 * End-to-end Selenium scenario covering the example app's UI: authentication,
 * task CRUD on the board, and the task-detail view.
 * <p>
 * The whole class shares a single {@link HttpServer} (started once in
 * {@link #startApp()}, stopped once in {@link #stopApp()}); each {@code @Test}
 * runs against a fresh Chrome session built in {@link #setUp()} and torn down
 * in {@link #tearDown()}, which keeps the tests independent without paying
 * the server start-up cost per case.
 * <p>
 * Test methods are explicitly ordered via {@link Order} so the read-only
 * scenarios (login page, valid/invalid login, logout) run before the
 * mutating ones (add, move, delete) and the detail-view ones.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TaskAppTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskAppTest.class);
    /** Port the example server is started on for the duration of the test class. */
    private static final int PORT = 9090;

    /** Shared HTTP server instance reused by every test. */
    private static HttpServer server;

    /** Per-test Chrome session (fresh in each {@code @BeforeEach}). */
    private SeleniumService selenium;

    /**
     * Starts the example app once for the whole class. Failures are logged so
     * the test report carries a usable stack trace.
     */
    @BeforeAll
    static void startApp() {
        server = awaitValue(Main.start(PORT, false)
                                .onFailure(e -> LOGGER.error("Server start failed", e)));
    }

    /** Stops the shared server once every test has run. */
    @AfterAll
    static void stopApp() {
        if (server != null) {
            await(server.stop());
        }
    }

    /** Allocates a fresh headful Chrome session before each test. */
    @BeforeEach
    void setUp() {
        selenium = SeleniumService.newInstance(Browser.builder(Browser.CHROME)
                                                      .mode(Browser.Mode.HEADFUL)
                                                      .build());
    }

    /** Closes the per-test Chrome session. */
    @AfterEach
    void tearDown() {
        if (selenium != null) {
            selenium.close();
        }
    }

    // -------------------------------------------------------------------------
    // Auth
    // -------------------------------------------------------------------------

    /** Opens the index page and asserts the login form is rendered. */
    @Test
    @Order(1)
    void loginPageIsDisplayed() {
        selenium.getHttpPage(appUrl("/index.html"));
        assertNotNull(selenium.firstTag("form"));
    }

    /** Submits the form with wrong credentials and asserts an error message appears. */
    @Test
    @Order(2)
    void loginWithInvalidCredentials() {
        selenium.getHttpPage(appUrl("/index.html"));
        selenium.firstTag("[name=username]").sendKeys("demo");
        selenium.firstTag("[name=password]").sendKeys("wrong-password");
        selenium.firstTag("[type=submit]").click();
        String errorText = selenium.repeatScript(Duration.ofSeconds(10), """
            const el = yojaWeb.firstTag('.error-msg')
            return el && el.textContent.trim().length > 0 ? el.textContent : null
        """);
        assertFalse(errorText.isBlank(),
                    "Error message should be displayed for invalid credentials");
    }

    /** Logs in with the seeded demo credentials and asserts seed task cards are visible. */
    @Test
    @Order(3)
    void loginWithValidCredentials() {
        login();
        assertTrue(selenium.findTags(".card").size() > 0,
                   "Seed tasks should be visible after login");
    }

    /** Logs in, clicks the logout button and asserts the board is hidden and the login form is visible again. */
    @Test
    @Order(4)
    void logoutHidesTaskBoard() {
        login();
        selenium.firstTag(".btn-logout").click();
        selenium.repeatScript(Duration.ofSeconds(10000), """
            const board = window.yojaWeb.firstTag('#taskboard-section');
            console.log(board && board.style.display === '')
            return board && board.style.display === 'none' ? true : null;
        """);
        assertEquals("none",
                     selenium.firstTag("#taskboard-section").getCssValue("display"),
                     "Task board should be hidden after logout");
        assertEquals("flex",
                     selenium.firstTag("#login-section").getCssValue("display"),
                     "Login section should be visible after logout");
    }

    // -------------------------------------------------------------------------
    // Task CRUD
    // -------------------------------------------------------------------------

    /** Submits the new-task form and asserts the freshly added card appears on the board. */
    @Test
    @Order(5)
    void addTaskAppearsOnBoard() {
        login();
        selenium.firstTag("[name=title]").sendKeys("Test task from Selenium");
        WebElement taskformSection = selenium.firstTag("#taskform-section");
        selenium.firstTagFrom(taskformSection, "[type=submit]").click();
        selenium.repeatScript(Duration.ofSeconds(10), """
            return yojaWeb.findTags('.task-title')
                          .some(t => t.textContent.includes('Test task from Selenium')) ? true : null
        """);
        long count = selenium.findTags(".task-title").stream()
                             .filter(el -> el.getText().contains("Test task from Selenium"))
                             .count();
        assertEquals(1, count);
    }

    /** Moves a "todo" task to "in-progress" via the next-status button and asserts the column populated. */
    @Test
    @Order(6)
    void moveTaskToNextStatus() {
        login();
        int initialInProgress = selenium.findTags(".column[data-status='in-progress'] .card").size();
        selenium.firstTag(".column[data-status='todo'] .btn-next").click();
        selenium.repeatScript(Duration.ofSeconds(10), """
            const cards = yojaWeb.findTags(".column[data-status='in-progress'] .card")
            return cards.length > arguments[0] ? true : null
        """, initialInProgress);
        assertEquals(initialInProgress + 1,
                     selenium.findTags(".column[data-status='in-progress'] .card").size());
    }

    /** Clicks the delete button on the first card and asserts the card count decreases by one. */
    @Test
    @Order(7)
    void deleteTask() {
        login();
        int initialCount = selenium.findTags(".card").size();
        assertTrue(initialCount > 0, "There should be seed tasks to delete");
        selenium.firstTag(".btn-delete").click();
        selenium.repeatScript(Duration.ofSeconds(10), """
            return yojaWeb.findTags('.card').length < arguments[0] ? true : null
        """, initialCount);
        assertEquals(initialCount - 1, selenium.findTags(".card").size());
    }

    // -------------------------------------------------------------------------
    // Task detail
    // -------------------------------------------------------------------------

    /** Opens the detail view of the first card and asserts its title and the three detail rows are rendered. */
    @Test
    @Order(8)
    void openTaskDetail() {
        login();
        selenium.firstTag(".btn-detail").click();
        String titleText = selenium.repeatScript(Duration.ofSeconds(10), """
            const el = yojaWeb.firstTag('.detail-title')
            return el && el.textContent.trim().length > 0 ? el.textContent : null
        """);
        assertFalse(titleText.isBlank(), "Detail title should not be empty");
        List<WebElement> rows = selenium.findTags(".detail-row");
        assertEquals(3, rows.size(), "Detail should show status, assignee and id rows");
    }

    /** From the detail view, clicks "back" and asserts the detail section is hidden again. */
    @Test
    @Order(9)
    void taskDetailBackButtonReturnsToBoard() {
        login();
        selenium.firstTag(".btn-detail").click();
        selenium.repeatScript(Duration.ofSeconds(10), """
            const el = yojaWeb.firstTag('.detail-title')
            return el && el.textContent.trim().length > 0 ? true : null
        """);
        selenium.firstTag(".btn-back").click();
        selenium.repeatScript(Duration.ofSeconds(10), """
            const board = yojaWeb.firstTag('#taskboard-section')
            return board && board.style.display !== 'none' ? true : null
        """);
        assertEquals("none",
                     selenium.executeScript("return yojaWeb.firstTag('#taskdetail-section').style.display"),
                     "Detail section should be hidden after clicking back");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Performs the canonical login flow used by every gated test: navigates to
     * the index page, fills the seeded {@code demo}/{@code demo} credentials,
     * submits the form, and waits until at least one task card has rendered.
     */
    private void login() {
        selenium.getHttpPage(appUrl("/index.html"));
        selenium.firstTag("[name=username]").sendKeys("demo");
        selenium.firstTag("[name=password]").sendKeys("demo");
        selenium.firstTag("[type=submit]").click();
        selenium.repeatScript(Duration.ofSeconds(10), """
            return yojaWeb.findTags('.card').length > 0 ? true : null
        """);
    }

    /**
     * @param path absolute path on the example server (must start with {@code /})
     * @return the {@link HttpUrl} pointing at {@code http://localhost:9090<path>}
     */
    private HttpUrl appUrl(String path) {
        return HttpUrl.builder("localhost")
                      .protocol(HttpProtocole.http)
                      .port(PORT)
                      .path(path)
                      .build();
    }

}
