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
 * Controler for the shared header section.
 *
 * Responsibilities:
 *  - language switching: every {.btn-lang} button changes the active locale
 *    via languageService when clicked, reading the target code from its
 *    {data-lang} attribute;
 *  - logout button: hidden by default, made visible on the {user:logged-in}
 *    event and hidden again on {user:logged-out}; clicking it fires a
 *    GET /api/logout and broadcasts {user:logged-out} on completion;
 *  - responsive marker: a {data-media} attribute is mirrored on the section
 *    root every time the responsiveService changes media, so CSS can target
 *    {[data-media="mobile"]} etc.
 */
export default class HeaderControler {

    /**
     * @param {object} section section handle injected by the runtime, used to
     *                         query and mutate the header DOM subtree
     */
    constructor(section) {
        const lang = yojaWebApi.languageService;
        const events = yojaWebApi.eventService;
        const http = yojaWebApi.httpClient;
        const responsive = yojaWebApi.responsiveService;

        // Language switch buttons
        section.findTags('.btn-lang').forEach(btn => {
            btn.addEventListener('click', () => {
                const code = btn.dataset.lang;
                lang.setLanguage(code);
            });
        });
        // Logout button
        const btnLogout = section.firstTag('.btn-logout');
        btnLogout.style.display = 'none';

        events.on('user:logged-in', () => { btnLogout.style.display = 'inline-block'; });
        events.on('user:logged-out', () => { btnLogout.style.display = 'none'; });

        btnLogout.addEventListener('click', () => {
            http.get({ url: '/api/logout' }).then(() => {
                events.trigger('user:logged-out');
            });
        });
        // Responsive: adapt layout on small screens
        responsive.onMedia(media => {
            section.tag.dataset.media = media;
            console.info('onMedia event: ', media)
        });
    }

}
