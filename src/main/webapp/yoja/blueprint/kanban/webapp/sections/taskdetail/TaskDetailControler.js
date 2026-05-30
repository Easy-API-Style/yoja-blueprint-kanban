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
 * Controler for the per-task detail section.
 *
 * Drives the section visibility from a single URL parameter ({task}):
 *  - whenever {task} appears in the URL (either via {after-push} from
 *    TaskBoardControler clicking ···, or via {pop} when the browser
 *    navigates back to a deep link), GET /api/tasks/:id fills the four
 *    {.detail-*} fields and the section is shown;
 *  - when {task} is absent or the request fails, the section is hidden and
 *    the URL parameter is cleaned up.
 *
 * To keep navigation reversible, {show()} / {hide()} also coordinate with
 * sibling controlers (any object exposing a {show}/{hide} pair): showing the
 * detail hides all the others; hiding the detail shows them again. This is
 * what makes the {.btn-back} button truly return to the board.
 */
export default class TaskDetailControler {

    /** @type {object} */ #section;
    /** @type {object} */ #http;
    /** @type {object} */ #params;
    /** @type {object} */ #events;

    /**
     * @param {object} section section handle injected by the runtime
     */
    constructor(section) {
        this.#section = section;
        this.#http = yojaWebApi.httpClient;
        this.#params = yojaWebApi.urlParameterService;
        this.#events = yojaWebApi.eventService;

        this.hide();

        this.#params.onChange(handler => {
            if ('after-push' == handler.event
                  || 'pop' == handler.event) {
                this.#onParamChange(handler);
            }
        });

        this.#section.firstTag('.btn-back')
                     .addEventListener('click', () => {
            this.#params.remove('task');
            this.#params.push();
        });
        this.#events.on('user:logged-out', () => {
            this.#params.remove('task');
            this.#section.tag.style.display = 'none';
        });
        section.pageReady(() => {
            if (this.#params.has('task')) this.#onParamChange();
        });
    }

    /**
     * Hides the detail section and asks every sibling controler that exposes
     * a {show()} method to re-display itself (returning the user to the board).
     */
    hide() {
       this.#section.tag.style.display = 'none';
       let otherControlers = yojaWeb.controlerService
                                    .find(document, c => c.show && c !== this);
       for (let controler of otherControlers) {
           controler.show();
       }
    }

    /**
     * Shows the detail section and asks every sibling controler that exposes
     * a {hide()} method to disappear, so the detail takes over the viewport.
     */
    show() {
       this.#section.tag.style.display = 'block';
       let otherControlers = yojaWeb.controlerService
                                    .find(document, c => c.hide && c !== this);
       for (let controler of otherControlers) {
           controler.hide();
       }
    }

    /**
     * Reacts to the {task} URL-parameter change: loads the task via
     * /api/tasks/:id and renders it on success, otherwise cleans up and hides.
     */
    #onParamChange() {
        const taskId = this.#params.get('task');
        if (!taskId) {
            this.hide();
            return;
        }
        this.#http.get({ url: `/api/tasks/${taskId}` })
                  .then(res => {
            if (res.status === 200) {
                this.#render(res.body);
                this.show();
            }
            else {
                this.#params.remove('task');
                this.#params.replace();
                this.hide();
            }
        });
    }

    /**
     * Fills the four {.detail-*} slots with the task's fields.
     *
     * @param {object} task task DTO matching {id, title, status, assignee}
     */
    #render(task) {
        this.#section.firstTag('.detail-title').textContent = task.title;
        this.#section.firstTag('.detail-assignee').textContent = task.assignee;
        this.#section.firstTag('.detail-status').textContent = task.status;
        this.#section.firstTag('.detail-id').textContent = task.id;
    }

}
