/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Response;
import org.hippoecm.frontend.service.NavAppSettings;
import org.onehippo.cms.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Contains the javascript needed to start the Navigation Application
 */
public class NavAppJavascriptHeaderItem extends HeaderItem {

    private static final Logger log = LoggerFactory.getLogger(NavAppJavascriptHeaderItem.class);

    private final transient NavAppSettings navAppSettings;
    private final transient Function<String, JavaScriptHeaderItem> mapper;

    public NavAppJavascriptHeaderItem(final NavAppSettings navAppSettings, Function<String, JavaScriptHeaderItem> mapper) {
        this.navAppSettings = navAppSettings;
        this.mapper = mapper;
    }

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singleton("nav-app-javascript-header-item");
    }

    @Override
    public void render(final Response response) {

        Stream.concat(
                Stream.of(getNavAppSettingsHeaderItem(navAppSettings)),
                getJavascriptSrcTagNames().map(mapper)
        ).forEach(item -> item.render(response));

    }

    private Stream<String> getJavascriptSrcTagNames() {
        return Stream.of("loader.js");
    }

    private HeaderItem getNavAppSettingsHeaderItem(NavAppSettings navAppSettings) {
        final String javascript = String.format("NavAppSettings = %s;", parse(navAppSettings));
        return JavaScriptHeaderItem.forScript(javascript, "hippo-nav-app-settings");
    }

    private String parse(NavAppSettings navAppSettings) {
        try {
            return Json.getMapper().writeValueAsString(navAppSettings);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse navAppSettings, returning empty javascript object instead", e);
            return "{}";
        }
    }
}
