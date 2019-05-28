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

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.StringHeaderItem;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.frontend.util.WebApplicationHelper;
import org.onehippo.cms.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Contains the javascript needed to start the Navigation Application
 */
public class NavAppHeaderItem extends HeaderItem {

    private static final Logger log = LoggerFactory.getLogger(NavAppHeaderItem.class);

    private final NavAppSettingFactory navAppSettingFactory;

    public NavAppHeaderItem(final NavAppSettingFactory navAppSettingFactory) {
        this.navAppSettingFactory = navAppSettingFactory;
    }

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singleton("nav-app-header-item");
    }

    @Override
    public void render(final Response response) {

        final NavAppSettings navAppSettings = navAppSettingFactory.newInstance(RequestCycle.get().getRequest(), PluginUserSession.get());
        final URI navAppLocation = navAppSettings.getAppSettings().getNavAppLocation();
        final URI brXmLocation = navAppSettings.getAppSettings().getBrXmLocation();

        final HeaderItem navAppSettingsHeaderItem = getNavAppSettingsHeaderItem(navAppSettings);
        final HeaderItem baseTagHeaderItem = getBaseTagHeaderItem(navAppLocation);

        Stream.concat(
                Stream.of(navAppSettingsHeaderItem, baseTagHeaderItem),
                getScrTagHeaderItems(brXmLocation, navAppLocation)
        ).forEach(item -> item.render(response));

    }

    private Stream<HeaderItem> getScrTagHeaderItems(URI brXmLocation, URI navAppLocation) {

        final Function<Url, UrlResourceReference> urlResourceReferenceFactory;
        final String navAppResourcePrefix;
        if (brXmLocation.equals(navAppLocation)) {
            // If these two URIs are the same then the resources are being served by
            // the CMS itself so we must add the antiCache query parameter
            urlResourceReferenceFactory = WebApplicationHelper::createUniqueUrlResourceReference;
            // It is assumed that the web.xml contains a servlet mapping for /navapp and that
            // the ResourceServlet is being used to serve the resources inside of that directory.
            // When running mvn package the files needed for the navapp (and navigation-communication)
            // are copied into  the target directory. See copy-files.js
            navAppResourcePrefix = "navapp/";
        } else {
            urlResourceReferenceFactory = UrlResourceReference::new;
            navAppResourcePrefix = "";
        }

        final Function<String, Url> urlGenerator = resourceName -> Url.parse(String.format("%s/%s%s", navAppLocation, navAppResourcePrefix, resourceName));
        return Stream.concat(
                getCssSrcTagNames()
                        .map(urlGenerator.andThen(urlResourceReferenceFactory))
                        .map(CssHeaderItem::forReference),
                getJavascriptSrcTagNames()
                        .map(urlGenerator.andThen(urlResourceReferenceFactory))
                        .map(JavaScriptHeaderItem::forReference)
        );
    }

    private Stream<String> getJavascriptSrcTagNames() {
        final List<String> javascriptResources = Arrays.asList("runtime.js", "es2015-polyfills.js", "polyfills.js", "main.js");
        if (WebApplication.get().usesDevelopmentConfig()) {
            return Stream.concat(
                    javascriptResources.stream(),
                    Stream.of("styles.js", "vendor.js"));
        }
        return javascriptResources.stream();
    }

    private Stream<String> getCssSrcTagNames() {
        if (WebApplication.get().usesDevelopmentConfig()) {
            Stream.empty();
        }
        return Stream.of("styles.css");
    }

    private HeaderItem getBaseTagHeaderItem(URI navAppLocation) {
        return StringHeaderItem.forString(String.format("<base href=%s/>", navAppLocation));
    }

    private HeaderItem getNavAppSettingsHeaderItem(NavAppSettings navAppSettings) {
        final String javascript = String.format("NavAppSettings = %s;", parse(navAppSettings));
        return JavaScriptHeaderItem.forScript(javascript, "hippo-nav-app-settings");
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
