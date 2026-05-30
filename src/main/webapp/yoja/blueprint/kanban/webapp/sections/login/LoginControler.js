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
const lang = yojaWebApi.languageService;
const langPath = import.meta.resolve(yojaWeb.path('./login.xml'))
const translator = await lang.loadTranslator(langPath)

/**
 * Controler for the login section.
 *
 * Responsibilities:
 *  - submit handler: POSTs {username, password} to /api/login; on 200 hides
 *    the section, broadcasts {user:logged-in} with the response body and
 *    stamps the current date in localStorage under {login-date} (which the
 *    IndexControler echoes); otherwise renders the {login.error} translation
 *    in the {.error-msg} slot;
 *  - language-change listener: clears any stale error message when the user
 *    switches locale (the message itself is locale-dependent);
 *  - logout listener: re-displays the section, clears the inputs and the
 *    error slot, leaving it ready for a fresh attempt;
 *  - page-ready hook: calls GET /api/me to detect an existing session — on
 *    200 it skips straight to {user:logged-in}, on anything else it
 *    explicitly broadcasts {user:logged-out} so other sections enter their
 *    "no user" state.
 */
export default class LoginControler {

    /**
     * @param {object} section section handle injected by the runtime
     */
    constructor(section) {
        const http = yojaWebApi.httpClient;
        const events = yojaWebApi.eventService;

        const form = section.firstTag('form');
        const error = section.firstTag('.error-msg');

        // submit
        form.addEventListener('submit', e => {
            e.preventDefault();
            error.textContent = '';

            const username = section.firstTag('[name=username]').value;
            const password = section.firstTag('[name=password]').value;
            http.post({ url: '/api/login' }, { username, password }).then(res => {
                if (res.status === 200) {
                    section.tag.style.display = 'none';
                    events.trigger('user:logged-in', res.body);
                    const storage = yojaWebApi.storageService;
                    storage.setLocalItem('login-date', new Date());
                }
                else {
                    error.textContent = translator('login.error') ?? 'Invalid credentials';
                }
            });
        });
        // lang event
        lang.onLanguageChange(e => {
            error.textContent = '';
            console.info('onLanguageChange event: ', e)
        });
        // Show login form again on logout
        events.on('user:logged-out', () => {
            section.tag.style.display = '';
            section.firstTag('[name=username]').value = '';
            section.firstTag('[name=password]').value = '';
            error.textContent = '';
        });
        // Check if already authenticated
        section.pageReady(() => {
            http.get({ url: '/api/me' }).then(check => {
                if (check.status === 200) {
                    section.tag.style.display = 'none';
                    events.trigger('user:logged-in', check.body);
                }
                else {
                    events.trigger('user:logged-out');
                }
            });
        });
    }

}
