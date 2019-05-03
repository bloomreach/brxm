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

import java.util.Collections;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.session.PluginUserSession;
import org.onehippo.cms.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Contains the javascript needed to start the Navigation Application
 */
public class NavAppHeaderItem extends HippoHeaderItem {

    private static final Logger log = LoggerFactory.getLogger(NavAppHeaderItem.class);

    private static final HippoHeaderItem INSTANCE = new NavAppHeaderItem();

    private final transient NavAppSettingsFactory navAppSettingsFactory = new NavAppSettingsFactory();

    public static HeaderItem get() {
        return INSTANCE;
    }

    private NavAppHeaderItem() {
    }

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singleton("hippo-nav-app-header-item");
    }

    @Override
    public void render(final Response response) {
        // TODO (meggermont): this is a temporary solution
        // For now we depend on js files pulled in via npm
        // Later we will use CDN provided resources instead
        getJsItem("bloomreach-navigation-communication.umd").render(response);

        JavaScriptHeaderItem.forScript(createScript(), "hippo-nav-app-settings").render(response);
    }

    private JavaScriptReferenceHeaderItem getJsItem(String jsResourceName) {
        final String resourceName = getJsName(jsResourceName);
        final ResourceReference resourceReference = new JavaScriptResourceReference(NavAppHeaderItem.class, resourceName);
        return JavaScriptReferenceHeaderItem.forReference(resourceReference);
    }

    private String getJsName(String jsResourceName) {
        return String.format("js/%s%s.js", jsResourceName, getProdSuffix());
    }

    private String getProdSuffix() {
        return isDevelopmentMode() ? "" : ".min";
    }


    private String createScript() {
        final NavAppSettings navAppSettings = navAppSettingsFactory.getNavAppSettings(PluginUserSession.get());
        return String.format("HippoNavAppSettings = %s;", parse(navAppSettings));
    }

    private String parse(NavAppSettings navAppSettings) {
        try {
            return Json.writeValueAsString(navAppSettings);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse {}, returning empty javascript object instead", navAppSettings, e);
            return "{}";
        }
    }

}
