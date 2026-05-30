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
const STATUS_NEXT = { 'todo': 'in-progress', 'in-progress': 'done' };

/**
 * Controler for the kanban-style task board section.
 *
 * Lifecycle:
 *  - hidden by default; revealed on {user:logged-in} once the task list and
 *    the "quote of the moment" have loaded, hidden again on {user:logged-out};
 *  - once visible, opens a WebSocket on /ws/tasks and reacts to server-side
 *    {created}, {updated} and {deleted} events to keep the board in sync
 *    with other connected clients.
 *
 * DOM contract:
 *  - {.cards} elements inside {.column[data-status="..."]} act as the per-status drop zones;
 *  - cards are minted by #addCard with {.card-actions} grouping the detail/next/delete buttons;
 *  - {.quote-bar} carries the quote string when /api/quote responds 200.
 */
export default class TaskBoardControler {

    /** @type {object} */ #section;
    /** @type {object} */ #http;
    /** @type {object} */ #ws;
    /** @type {object} */ #events;
    /** @type {object} */ #params;

    /**
     * @param {object} section section handle injected by the runtime
     */
    constructor(section) {
        this.#section = section;
        this.#http = yojaWebApi.httpClient;
        this.#ws = yojaWebApi.webSocketService;
        this.#events = yojaWebApi.eventService;
        this.#params = yojaWebApi.urlParameterService;

        this.hide();
        // Show on login, hide on logout
        this.#events.on('user:logged-in', () => {
            this.#loadTasks()
                .then(() => this.show())
                .then(() => this.#connectWebSocket());
            this.#loadQuote();
        });
        this.#events.on('user:logged-out', () => {
            this.hide();
        });
    }

    /** Hides the board section. */
    hide() {
        this.#section.tag.style.display = 'none';
    }

    /** Reveals the board section. */
    show() {
        this.#section.tag.style.display = 'block';
    }

    /**
     * Fetches a fresh quote from /api/quote and renders it in {.quote-bar}.
     * Failures silently leave the bar untouched (the backend already falls
     * back to a baked-in quote — see QuoteService.FALLBACK).
     *
     * @returns {Promise<void>} resolves once the rendering attempt completed
     */
    #loadQuote() {
        return this.#http.get({ url: '/api/quote' }).then(res => {
            if (res.status === 200) {
                const q = res.body;
                const quoteBar = this.#section.firstTag('.quote-bar');
                quoteBar.textContent = `"${q.quote}" — ${q.author}`;
                quoteBar.style.display = 'block';
            }
        });
    }

    /**
     * Loads the current task list and rebuilds the columns.
     *
     * @returns {Promise<void>} resolves once the rendering attempt completed
     */
    #loadTasks() {
        return this.#http.get({ url: '/api/tasks' }).then(res => {
            if (res.status === 200) {
                this.#renderTasks(res.body);
            }
        });
    }

    /**
     * Clears every column then re-creates a card per task. Called on initial
     * load only; later mutations are applied incrementally through
     * {@link #addCard}.
     *
     * @param {Array<object>} tasks task list from /api/tasks
     */
    #renderTasks(tasks) {
        this.#section.findTags('.cards')
                     .forEach(col => (col.innerHTML = ''));
        tasks.forEach(task => this.#addCard(task));
    }

    /**
     * Builds a card DOM node for the supplied task and appends it to the
     * column matching {task.status}. Wires the {.btn-detail},
     * {.btn-next} (when a successor status exists) and {.btn-delete} buttons.
     *
     * @param {object} task task DTO matching {id, title, status, assignee}
     */
    #addCard(task) {
        const col = this.#section.firstTag(`.column[data-status="${task.status}"] .cards`);
        if (!col) return;

        const card = document.createElement('div');
        card.className = 'card';
        card.dataset.id = task.id;
        card.innerHTML = `
            <span class="task-title">${task.title}</span>
            <span class="task-assignee">${task.assignee}</span>
            <div class="card-actions">
                <button class="btn-detail">···</button>
                ${STATUS_NEXT[task.status]
                    ? `<button class="btn-next">→</button>` : ''}
                <button class="btn-delete">✕</button>
            </div>`;

        card.querySelector('.btn-detail').addEventListener('click', () => {
            this.#params.set('task', task.id);
            this.#params.push();
        });
        card.querySelector('.btn-delete')?.addEventListener('click', () => {
            this.#http.get({ url: `/api/tasks/${task.id}/delete` });
        });
        card.querySelector('.btn-next')?.addEventListener('click', () => {
            this.#http.post({ url: `/api/tasks/${task.id}/status` },
                            { status: STATUS_NEXT[task.status] });
        });
        col.appendChild(card);
    }

    /**
     * Opens /ws/tasks and dispatches the three event types broadcast by the
     * backend: {created} adds a card, {updated} replaces the matching one,
     * {deleted} removes it. The board stays consistent across multiple
     * connected clients without polling.
     */
    #connectWebSocket() {
        const socket = this.#ws.webSocket('/ws/tasks');
        socket.onMessage(message => {
            const data = JSON.parse(message.data);
            if (data.event === 'created') {
                this.#addCard(data.task);
            }
            else if (data.event === 'updated') {
                const card = this.#section.firstTag(`.card[data-id="${data.task.id}"]`);
                if (card) card.remove();
                this.#addCard(data.task);
            }
            else if (data.event === 'deleted') {
                const card = this.#section.firstTag(`.card[data-id="${data.id}"]`);
                if (card) card.remove();
            }
        });
    }

}
