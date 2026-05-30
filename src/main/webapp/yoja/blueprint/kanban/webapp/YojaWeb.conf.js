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
 * Yoja-Web configuration consumed by the runtime on bootstrap.
 *
 * Fields:
 *  - version: app version string surfaced by the runtime in debug helpers.
 *  - defaultLanguage: BCP-47-like language tag used when the browser does not
 *    advertise a supported one (LoginControler loads its translator from this).
 *  - mediaDescriptions: ordered list of named breakpoints fed to the
 *    responsiveService. Entries are matched in declaration order against the
 *    viewport width; the first whose {maxWidth} is satisfied wins. The last
 *    entry typically has no {maxWidth} and acts as the fallback bucket.
 */
export default {
    version: '1.0.1',
    defaultLanguage: 'fr',
    mediaDescriptions: [
        { name: 'mobile', maxWidth: 767 },
        { name: 'tablet', maxWidth: 1023 },
        { name: 'desktop' }
    ]
};
