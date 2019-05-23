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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.hippoecm.frontend.util.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.frontend.NavAppHeaderItem.getCmsLocation;
import static org.hippoecm.frontend.NavAppHeaderItem.getUrlResourceReference;

/**
 * Constructs and injects a javascript implementation of the nav-app to app API.
 */
public class NavAppToAppHeaderItem extends HippoHeaderItem {

    private static final String NAVIGATION_API_JS = "nav-app-to-app.js";
    private static final String PARENT_ORIGIN = "parentOrigin";

    private static final Logger log = LoggerFactory.getLogger(NavAppToAppHeaderItem.class);
    private static final String NAVAPP_COMMUNICATION_LOCATION = "navapp-communication.location";

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singleton("navigation-header-item");
    }

    @Override
    public void render(final Response response) {

        final String contextPath = WebApplication.get().getServletContext().getContextPath();
        final String cmsLocation = getCmsLocation(contextPath);
        final URL navAppCommunicationLocation = getNavAppCommunicationLocation(cmsLocation);
        final String navAppCommunicationResourcePrefix = getNavAppCommunicationResourcePrefix();

        final Function<String, ResourceReference> toHeaderItem = name -> getUrlResourceReference(navAppCommunicationLocation.toString(), name);
        Stream.of("penpal.js", "bloomreach-navapp-communication.umd.js")
                .map(name -> String.format("%s%s", navAppCommunicationResourcePrefix, name))
                .map(toHeaderItem.andThen(JavaScriptHeaderItem::forReference))
                .forEach(item -> item.render(response));
        OnDomReadyHeaderItem.forScript(createScript()).render(response);

    }

    private String createScript() {

        final Map<String, String> variables = new HashMap<>();
        variables.put(PARENT_ORIGIN, RequestUtils.getFarthestUrlPrefix(RequestCycle.get().getRequest()));

        try (final PackageTextTemplate javaScript = new PackageTextTemplate(NavAppToAppHeaderItem.class, NAVIGATION_API_JS)) {
            return javaScript.asString(variables);
        } catch (IOException e) {
            log.error("Failed to create script for resource {}, returning empty string instead.", NAVIGATION_API_JS, e);
            return "";
        }
    }

    private URL getNavAppCommunicationLocation(String cmsLocation) {
        try {
            return new URL(System.getProperty(NAVAPP_COMMUNICATION_LOCATION, cmsLocation));
        } catch (MalformedURLException e) {
            throw new WicketRuntimeException(e);
        }
    }

    private String getNavAppCommunicationResourcePrefix() {
        return System.getProperty(NAVAPP_COMMUNICATION_LOCATION, null) == null ? "navapp/" : "";
    }


}
