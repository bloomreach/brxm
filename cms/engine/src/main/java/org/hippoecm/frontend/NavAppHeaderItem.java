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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.StringHeaderItem;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.frontend.util.RequestUtils;
import org.onehippo.cms.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Contains the javascript needed to start the Navigation Application
 */
public class NavAppHeaderItem extends HeaderItem {

    private static final Logger log = LoggerFactory.getLogger(NavAppHeaderItem.class);
    private static final String NAVAPP_LOCATION = "navapp.location";

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singleton("nav-app-header-item");
    }

    @Override
    public void render(final Response response) {

        final String contextPath = WebApplication.get().getServletContext().getContextPath();
        final String cmsLocation = getCmsLocation(contextPath);
        final URL navAppLocation = getNavAppLocation(cmsLocation);
        final String navAppResourcePrefix = getNavappResourcePrefix();
        StringHeaderItem.forString(String.format("<base href=%s/>", navAppLocation)).render(response);

        final NavAppSettings navAppSettings = getSettings(navAppLocation, cmsLocation, contextPath, PluginUserSession.get());
        final String javascript = String.format("NavAppSettings = %s;", parse(navAppSettings));

        JavaScriptHeaderItem.forScript(javascript, "hippo-nav-app-settings").render(response);

        final List<String> resourcesList = new ArrayList<>(Arrays.asList("runtime.js", "es2015-polyfills.js", "polyfills.js", "main.js"));
        if (isLocalDevelopment()) {
            resourcesList.add("styles.js");
            resourcesList.add("vendor.js");
        } else {
            CssHeaderItem.forReference(getUrlResourceReference(navAppLocation.toString(), navAppResourcePrefix + "styles.css")).render(response);
        }

        final Function<String, ResourceReference> toHeaderItem = name -> getUrlResourceReference(navAppLocation.toString(), name);
        resourcesList.stream()
                .map(name -> String.format("%s%s", navAppResourcePrefix, name))
                .map(toHeaderItem.andThen(JavaScriptHeaderItem::forReference))
                .forEach(item -> item.render(response));
    }

    private URL getNavAppLocation(String cmsLocation) {
        try {
            return new URL(System.getProperty(NAVAPP_LOCATION, cmsLocation));
        } catch (MalformedURLException e) {
            throw new WicketRuntimeException(e);
        }
    }

    private String getNavappResourcePrefix() {
        return System.getProperty(NAVAPP_LOCATION, null) == null ? "navapp/" : "";
    }

    static String getCmsLocation(String contextPath) {
        final Request wicketRequest = RequestCycle.get().getRequest();
        final HttpServletRequest request = (HttpServletRequest) wicketRequest.getContainerRequest();
        final String scheme = RequestUtils.getFarthestRequestScheme(request);
        final String remoteAddr = RequestUtils.getFarthestRequestHost(request);
        return String.format("%s://%s%s", scheme, remoteAddr, contextPath);
    }

    private boolean isLocalDevelopment() {
        return "development".equals(System.getProperty("wicket.configuration"));
    }

    static ResourceReference getUrlResourceReference(final String location, final String resourceName) {
        final Url parsedUrl = Url.parse(String.format("%s/%s", location, resourceName));
        return new UrlResourceReference(parsedUrl);
    }

    private NavAppSettings getSettings(final URL navAppLocation, final String cmsLocation, final String contextPath, final PluginUserSession userSession) {
        final NavAppSettings navAppSettings = new NavAppSettings();
        navAppSettings.setUserSettings(getUserSettings(userSession));
        navAppSettings.setAppSettings(getAppSettings(navAppLocation, cmsLocation, contextPath));
        return navAppSettings;
    }

    private NavAppSettings.AppSettings getAppSettings(final URL navAppLocation, final String cmsLocation, final String contextPath) {
        final List<NavAppSettings.NavConfigResource> navConfigResources = new ArrayList<>();
        final NavAppSettings.NavConfigResource brXmResource = new NavAppSettings.NavConfigResource();
        brXmResource.setResourceType(NavAppSettings.ResourceType.REST);

        final String endpointUrl = "navigationitems";
        brXmResource.setUrl(String.format("%s/ws/%s", cmsLocation, endpointUrl));
        navConfigResources.add(brXmResource);

        final NavAppSettings.AppSettings appSettings = new NavAppSettings.AppSettings();
        appSettings.setNavConfigResources(navConfigResources);
        appSettings.setNavAppLocation(navAppLocation);
        appSettings.setContextPath(contextPath);
        return appSettings;
    }

    private NavAppSettings.UserSettings getUserSettings(final PluginUserSession userSession) {
        final NavAppSettings.UserSettings userSettings = new NavAppSettings.UserSettings();
        userSettings.setLanguage(userSession.getLocale().getLanguage());
        userSettings.setTimeZone(userSession.getTimeZone());
        userSettings.setUserName(userSession.getUserName());
        return userSettings;
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
