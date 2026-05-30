# yoja-blueprint-kanban

A full-stack Kanban application built with the yoja framework. It demonstrates its core features in a real-world context: HTTP server, REST API, WebSocket for real-time updates, session management, background workers, and the yoja-web frontend framework with shadow DOM components and i18n.

## Table of Contents

- [Architecture](#architecture)
- [Gradle commands](#gradle-commands)
- [Running the application](#running-the-application)
- [Tests](#tests)
- [yoja API usage map](#yoja-api-usage-map)

---

## Architecture

```
yoja-blueprint-kanban/
├── src/main/java/               # Backend — Java
│   └── yoja/blueprint/kanban/
│       ├── Main.java            # Entry point: server setup and route declaration
│       ├── model/Task.java      # Task record (id, title, status, assignee)
│       ├── service/
│       │   ├── TaskService.java # In-memory task store + CSV export
│       │   └── QuoteService.java# Calls external REST API (zenquotes.io)
│       └── api/
│           ├── AuthApi.java     # POST /api/login, GET /api/logout, GET /api/me
│           └── TaskApi.java     # CRUD endpoints + CSV export
└── src/main/webapp/             # Frontend — JavaScript (yoja-web)
    └── yoja/blueprint/kanban/webapp/
        ├── index.html           # Single page, declares all sections
        ├── YojaWeb.conf.js      # Framework config (language, breakpoints)
        ├── indexControler.js    # Root controller (storage events)
        ├── common.css           # Shared button and input base styles
        └── sections/
            ├── header/          # Language switch + logout button
            ├── login/           # Login form, session check
            ├── taskboard/       # Kanban board (todo / in-progress / done)
            ├── taskdetail/      # Full-page task detail (URL parameter navigation)
            └── taskform/        # New task form + CSV export trigger
```

The frontend is a single HTML page. Each section is a yoja-web component (shadow DOM, scoped CSS, i18n). The backend exposes a REST API and a WebSocket endpoint so all connected browsers see task updates in real time.

---

## Gradle commands

| Command | Description |
|---|---|
| `./gradlew run` | Start the application (development) |
| `./gradlew run --args="-p 9090 -ssl true"` | Start on a custom port with HTTPS |
| `./gradlew clean test` | Run the full test suite |
| `./gradlew distZip` | Package the application into a distribution ZIP |
| `./gradlew clean build` | Clean and build the project |

---

## Running the application

Default credentials: **login** `demo` / **password** `demo`

| Argument | Default | Description |
|---|---|---|
| `-p <port>` | `8080` | HTTP(S) listening port |
| `-ssl <true\|false>` | `false` | Enable HTTPS with a self-signed certificate |

### Gradle (development)

Starts the application directly via Gradle — no packaging step needed. Logback is configured automatically from `src/test/resources/logback-test.xml`.

```bash
./gradlew run
./gradlew run --args="-p 9090 -ssl true"   # custom port + HTTPS
```

### Distribution ZIP (deployment)

The `distZip` task packages the application and all its runtime dependencies into a self-contained archive with platform startup scripts.

```bash
./gradlew distZip
```

The archive is generated at `build/distributions/yoja-blueprint-kanban.zip`. Unzip it and run the startup script:

```bash
unzip build/distributions/yoja-blueprint-kanban.zip -d /opt/yoja-blueprint-kanban
/opt/yoja-blueprint-kanban/yoja-blueprint-kanban/bin/yoja-blueprint-kanban          # Linux / macOS
/opt/yoja-blueprint-kanban/yoja-blueprint-kanban/bin/yoja-blueprint-kanban -p 9090  # custom port
```

On Windows use `yoja-blueprint-kanban\bin\yoja-blueprint-kanban.bat` instead.

### Eclipse

1. Import the project as a **Gradle project** (`File > Import > Gradle > Existing Gradle Project`).
2. Open `src/main/java/yoja/blueprint/kanban/Main.java`.
3. Right-click → **Run As > Java Application**.
4. To enable SSL: **Run Configurations > Arguments > Program arguments** → add `ssl`.

### VS Code

Create `.vscode/launch.json` at the repository root:

```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Yoja Blueprint Kanban",
            "request": "launch",
            "mainClass": "yoja.blueprint.kanban.Main",
            "projectName": "yoja-blueprint-kanban",
            "args": "-p 8080"
        },
        {
            "type": "java",
            "name": "Yoja Blueprint Kanban (SSL)",
            "request": "launch",
            "mainClass": "yoja.blueprint.kanban.Main",
            "projectName": "yoja-blueprint-kanban",
            "args": "-p 8443 -ssl true"
        }
    ]
}
```

Then press **F5** or open the **Run and Debug** panel and select `Yoja Blueprint Kanban`.

---

## Tests

The test suite is in `src/test/java/yoja/blueprint/kanban/TaskAppTest.java`.  
It is an end-to-end test that starts a real HTTP server and drives a headless Chrome browser via Selenium.

### How it works

**Server lifecycle** — `@BeforeAll` starts the server on port 9090. `@AfterAll` stops it. The server is shared across all tests so its in-memory task state accumulates across test methods.

**Browser lifecycle** — `@BeforeEach` creates a fresh headless Chrome session. `@AfterEach` closes it. Each test therefore starts with a clean browser state (no cookies, no session).

**Shadow DOM traversal** — Each yoja-web section uses an open shadow root for CSS isolation. Standard Selenium CSS selectors cannot pierce shadow roots. The tests use `SeleniumService.firstTag(selector)` and `findTags(selector)` which execute `yojaWeb.firstTag()` / `yojaWeb.findTags()` in the browser — these methods traverse all shadow roots recursively.

**Page readiness** — `selenium.getHttpPage(url)` navigates and then polls until the yojaWeb framework is fully initialized.

**Async waiting** — After user interactions (button clicks, form submissions) the test must wait for the async server response before asserting. `selenium.repeatScript(duration, js)` retries the given JavaScript until it returns a non-null value, or until the timeout expires.

### Test order

Tests run in a fixed order (`@TestMethodOrder + @Order`) so that state-mutating tests do not interfere:

| Order | Test | State change |
|---|---|---|
| 1 | `loginPageIsDisplayed` | none |
| 2 | `loginWithInvalidCredentials` | none |
| 3 | `loginWithValidCredentials` | none |
| 4 | `logoutHidesTaskBoard` | none |
| 5 | `addTaskAppearsOnBoard` | +1 task (todo) |
| 6 | `moveTaskToNextStatus` | 1 task todo → in-progress |
| 7 | `deleteTask` | −1 task |
| 8 | `openTaskDetail` | none |
| 9 | `taskDetailBackButtonReturnsToBoard` | none |

### Running the tests

```bash
./gradlew clean test
```

The HTML report is generated at `build/reports/tests/test/index.html`.

> Chrome and ChromeDriver must be installed and on the `PATH`. The tests run in headless mode by default.

---

## yoja API usage map

### Backend

| Feature | File | API used |
|---|---|---|
| HTTP router with session | `Main.java` | `HttpRouter.builder()`, `HttpSessionStore` |
| Serve static frontend resources | `Main.java` | `WebApp.jar(...)`, `HttpRouter.webResource()` |
| Register REST routes | `Main.java` | `HttpRouter.webService()`, `WebService` |
| WebSocket endpoint | `Main.java` | `WebSocket`, `WebSocketService` |
| Periodic background task | `Main.java` | `Timer.schedule().period().build()` |
| Authentication guard | `Main.java` | `Handler<HttpRouting>`, `routing.session()` |
| Session read/write | `AuthApi.java` | `routing.session().put()`, `.get()`, `.destroy()` |
| JSON response | `AuthApi.java`, `TaskApi.java` | `routing.response().send(JsonObject)` |
| Path parameters | `TaskApi.java` | `routing.request().firstParameter("id")` |
| Error response | `TaskApi.java` | `routing.fail(404)` |
| Push WebSocket message | `TaskApi.java` | `taskWebSocket.send(json)` |
| Non-blocking file I/O | `TaskService.java` | `Worker.singleThread.once(...)` |
| Outbound HTTP call | `QuoteService.java` | `HttpClient.builder()`, `HttpGet.of()`, `client.send()` |
| Async result chaining | `QuoteService.java` | `Future.map()`, `.onFailure()` |

### Frontend

| Feature | File | API used |
|---|---|---|
| Framework config | `YojaWeb.conf.js` | `defaultLanguage`, `mediaDescriptions` |
| Storage event listener | `indexControler.js` | `storageService.on('local', 'set', key, cb)` |
| Document ready hook | `indexControler.js` | `yojaWeb.onDocumentReady(cb)` |
| Load a language file | `LoginControler.js` | `languageService.loadTranslator(path)` |
| HTTP POST | `LoginControler.js` | `httpClient.post({ url }, body).then(res => ...)` |
| Application events (publish) | `LoginControler.js` | `eventService.trigger('user:logged-in', data)` |
| Application events (subscribe) | `LoginControler.js` | `eventService.on('user:logged-out', cb)` |
| Section lifecycle hook | `LoginControler.js` | `section.pageReady(cb)` |
| Find element in shadow DOM | `LoginControler.js` | `section.firstTag(selector)` |
| Responsive layout | `HeaderControler.js` | `responsiveService.onMedia(media => ...)` |
| Language switch | `HeaderControler.js` | `languageService.setLanguage(code)` |
| HTTP GET | `TaskBoardControler.js` | `httpClient.get({ url }).then(res => ...)` |
| WebSocket connection | `TaskBoardControler.js` | `webSocketService.webSocket(path)` |
| Real-time message handling | `TaskBoardControler.js` | `socket.onMessage(message => ...)` |
| URL parameter navigation | `TaskBoardControler.js` | `urlParameterService.set(key, value)`, `.push()` |
| Detect URL parameter change | `TaskDetailControler.js` | `urlParameterService.onChange(handler)` |
| Read/remove URL parameter | `TaskDetailControler.js` | `urlParameterService.get(key)`, `.remove(key)` |
| Find all controllers | `TaskDetailControler.js` | `yojaWeb.controlerService.find(document, filter)` |
| Find multiple elements | `TaskFormControler.js` | `section.findTags(selector)` |
| Store typed value | `LoginControler.js` | `storageService.setLocalItem(key, value)` |
