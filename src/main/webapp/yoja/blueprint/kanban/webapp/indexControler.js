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
 * Entry-point script of the example app, loaded by index.html.
 *
 * Two responsibilities:
 *  1. Prepend the shared header fragment into <body> as soon as the document
 *     is ready, then reveal the body once that fragment has been resolved.
 *     The reveal-after-prepend pattern hides the FOUC while sections wire up.
 *  2. Expose IndexControler as the default export; the runtime instantiates it
 *     once and uses it as the page-level controler. It is the place where
 *     cross-section concerns (here: persisting and reading the last-login
 *     date) live.
 */
yojaWeb.onDocumentReady(() => {
    const events = yojaWebApi.eventService;
    yojaWeb.prepend(yojaWeb.firstTag('body'),
                    import.meta.resolve(yojaWeb.path('./sections/header/header.html')))
           .then(children => yojaWeb.firstTag('body').style.display = 'block');
    console.info('onDocumentReady all events: ', events.events())
});

/**
 * Page-level controler.
 *
 * Wires a single side-effect: every time the {login-date} key is written to
 * localStorage (see LoginControler), log both the previous value (when any)
 * and the new one. This demonstrates the storageService's "set" hook.
 */
export default class IndexControler {

    /**
     * @param {object} section page-level section handle injected by the runtime;
     *                         unused here but kept for parity with section
     *                         controlers
     */
    constructor(section) {
        const storage = yojaWebApi.storageService;
        storage.on('local', 'set', 'login-date', date => {
            let previousDate = storage.getLocalItem('login-date');
            if (previousDate) {
                console.log('last time connected:', previousDate);
            }
            console.log('login date:', date);
        });
    }

}
