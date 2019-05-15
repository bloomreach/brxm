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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.head.StringHeaderItem;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.hippoecm.frontend.session.PluginUserSession;
import org.onehippo.cms.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Contains the javascript needed to start the Navigation Application
 */
public class NavAppHeaderItem extends HeaderItem {

    private static final Logger log = LoggerFactory.getLogger(NavAppHeaderItem.class);

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singleton("nav-app-header-item");
    }

    @Override
    public void render(final Response response) {

        final String contextPath = WebApplication.get().getServletContext().getContextPath();
        final PluginUserSession userSession = PluginUserSession.get();

        StringHeaderItem.forString("<base href=" + contextPath + "/>").render(response);

        final String javascript = String.format("NavAppSettings = %s;", parse(getSettings(contextPath, userSession)));

        JavaScriptHeaderItem.forScript(javascript, "hippo-nav-app-settings").render(response);

        Stream.of("runtime.js", "es2015-polyfills.js", "polyfills.js", "styles.js", "vendor.js", "main.js")
                .forEach(resourceName -> {
                    final JavaScriptResourceReference reference = new JavaScriptResourceReference(getClass(), resourceName);
                    final JavaScriptReferenceHeaderItem item = JavaScriptHeaderItem.forReference(reference);
                    item.setDefer(true);
                    item.render(response);
                });
    }

    private NavAppSettings getSettings(final String contextPath, final PluginUserSession userSession) {
        final NavAppSettings navAppSettings = new NavAppSettings();

        final NavAppSettings.UserSettings userSettings = new NavAppSettings.UserSettings();
        userSettings.setLanguage(userSession.getLocale().getLanguage());
        userSettings.setTimeZone(userSession.getTimeZone());
        userSettings.setUserName(userSession.getUserName());
        navAppSettings.setUserSettings(userSettings);

        final List<NavAppSettings.NavConfigResource> navConfigResources = new ArrayList<>();
        final NavAppSettings.NavConfigResource brXmResource = new NavAppSettings.NavConfigResource();
        brXmResource.setResourceType(NavAppSettings.ResourceType.REST);
        final String endpointUrl = "navigationitems";
        brXmResource.setUrl(String.format("%s/ws/%s", contextPath, endpointUrl));
        navConfigResources.add(brXmResource);

        final NavAppSettings.AppSettings appSettings = new NavAppSettings.AppSettings();
        appSettings.setNavConfigResources(navConfigResources);
        navAppSettings.setAppSettings(appSettings);
        return navAppSettings;
    }

    private String parse(NavAppSettings navAppSettings) {
        try {
            return Json.getMapper().writeValueAsString(navAppSettings);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse {}, returning empty javascript object instead", navAppSettings, e);
            return "{}";
        }
    }
}
