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
package yoja.blueprint.kanban.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.easygoingapi.yoja.http.client.HttpClient;
import com.easygoingapi.yoja.http.client.HttpEngine;
import com.easygoingapi.yoja.http.client.HttpGet;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Backend service fetching a "quote of the moment" from the public
 * <a href="https://zenquotes.io/api/random">zenquotes.io</a> REST API.
 * <p>
 * Wraps the call in a Yoja {@link HttpClient} dialed through a single shared
 * {@link HttpEngine}; on success the upstream response — a JSON array whose
 * first entry carries {@code q} (quote) and {@code a} (author) — is reshaped
 * into a {@code {quote, author}} object. On failure (non-200 status,
 * network error) the {@link #FALLBACK} value is returned so the UI always has
 * something to display.
 * <p>
 * This class is the example's demonstration of consuming an external REST
 * API from the backend.
 */
public class QuoteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuoteService.class);

    /** Default quote returned when the upstream API is unreachable or misbehaves. */
    public static final JsonObject FALLBACK = new JsonObject().put("quote", "The best way to predict the future is to invent it.")
                                                              .put("author", "Alan Kay");

    /** Yoja HTTP client pinned to {@code zenquotes.io:443} over TLS. */
    private final HttpClient client;

    /**
     * Builds the service with a fresh {@link HttpEngine} and a TLS client
     * pointed at {@code zenquotes.io:443}.
     */
    public QuoteService() {
        HttpEngine engine = new HttpEngine();
        this.client = HttpClient.builder(engine)
                                .host("zenquotes.io")
                                .port(443)
                                .ssl(true)
                                .build();
    }

    /**
     * Asks the upstream for a fresh quote and reshapes the response into a
     * {@code {quote, author}} JSON object. The returned future never fails
     * loudly: any transport or shape error yields the {@link #FALLBACK}
     * value, with the underlying cause logged.
     *
     * @return a future resolving to a {@code {quote, author}} JSON object
     */
    public Future<JsonObject> fetch() {
       return client.send(HttpGet.of("/api/random"))
                    .map(res -> {
                        final JsonObject result;
                        if (res.statusCode() == 200
                                && res.bodyAsJsonArray() != null) {
                            JsonArray arr = res.bodyAsJsonArray();
                            JsonObject item = arr.getJsonObject(0);
                            result = new JsonObject().put("quote", item.getString("q"))
                                                     .put("author", item.getString("a"));
                        }
                        else {
                            LOGGER.warn("External quote API unavailable, using fallback [status{}]", res.statusCode());
                            result = FALLBACK;
                        }
                        return result;
                    })
                    .otherwise(e -> {
                        LOGGER.error("External quote API unavailable failed {}", e.getMessage());
                        return FALLBACK;
                    });
    }

}
