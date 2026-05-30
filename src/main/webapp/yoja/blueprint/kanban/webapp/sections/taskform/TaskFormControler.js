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
/**
 * Controler for the "add task" form section.
 *
 * Responsibilities:
 *  - visibility tied to authentication: hidden by default, shown on
 *    {user:logged-in}, hidden again on {user:logged-out};
 *  - submit handler: POSTs {title, assignee} to /api/tasks (silently dropping
 *    submissions with an empty title), and resets the form once the request
 *    resolves — the WebSocket fan-out from TaskBoardControler handles
 *    rendering the resulting card;
 *  - {.btn-export} button: triggers GET /api/tasks/export which kicks off a
 *    backend worker that dumps the task list to CSV; the response carries
 *    only a {status} string, logged to the console.
 */
export default class TaskFormControler {

    /** @type {object} */ #section;

    /**
     * @param {object} section section handle injected by the runtime
     */
    constructor(section) {
        this.#section = section;
        const http = yojaWebApi.httpClient;
        const events = yojaWebApi.eventService;

        this.hide();
        // events
        events.on('user:logged-in', () => {
            this.show();
        });
        events.on('user:logged-out', () => {
            this.hide();
        });
        // submit
        const form = section.firstTag('form');
        form.addEventListener('submit', e => {
            e.preventDefault();
            const title = section.firstTag('[name=title]').value.trim();
            const assignee = section.firstTag('[name=assignee]').value.trim();
            if (!title) return;

            http.post({ url: '/api/tasks' },
                      { title, assignee })
                .then(() => {
                   form.reset();
                });
        });
        // CSV export (runs a Worker thread on the backend)
        section.firstTag('.btn-export').addEventListener('click', () => {
            http.get({ url: '/api/tasks/export' }).then(res => {
                console.info('Export:', res.body?.status);
            });
        });
    }

    /** Hides the form section. */
    hide() {
       this.#section.tag.style.display = 'none';
    }

    /** Reveals the form section. */
    show() {
       this.#section.tag.style.display = 'block';
    }

}
