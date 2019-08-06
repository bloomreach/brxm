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
import java.util.function.Function;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.filter.FilteredHeaderItem;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.hippoecm.frontend.service.AppSettings;
import org.hippoecm.frontend.util.WebApplicationHelper;

import static org.hippoecm.frontend.NavAppPanel.NAVAPP_JAVASCRIPT_HEADER_ITEM;

final class NavAppUtils {

    private NavAppUtils() {
    }

    /**
     * Returns a mapper that maps a resource string into an {@link ResourceReference}.
     * If the NavApp resources are served by the CMS then the resources will contain an
     * anti-cache query parameter. Otherwise it's up to the provider (a CDN probably) to
     * take of browser cache expiration.
     *
     * @param appSettings the app settings
     * @return function that turns a string into a resource reference
     */
    static Function<String, ResourceReference> getMapper(AppSettings appSettings) {

        final Function<Url, UrlResourceReference> urlResourceReferenceFactory;
        final String navAppResourcePrefix;
        if (isCmsServingNavAppResources(appSettings)) {
            // Will add anti-cache query parameter
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

        final URI navAppLocation = appSettings.getNavAppLocation();
        final Function<String, Url> urlGenerator = resourceName -> Url.parse(String.format("%s/%s%s", navAppLocation, navAppResourcePrefix, resourceName));
        return urlGenerator.andThen(urlResourceReferenceFactory);
    }

    private static boolean isCmsServingNavAppResources(AppSettings appSettings) {
        // If these two URIs are the same then the resources are being served by
        // the CMS itself.
        return appSettings.getBrXmLocation().equals(appSettings.getNavAppLocation());
    }

    /**
     * return {@code true} if the given {@link HeaderItem} is a NavApp header item and {@code false} otherwise.
     *
     * @param headerItem a header item
     * @return if the item belongs to the NavApp or not
     */
    static boolean isNavAppHeaderItem(HeaderItem headerItem) {
        return headerItem instanceof NavAppBaseTagHeaderItem
                || (headerItem instanceof FilteredHeaderItem && (NAVAPP_JAVASCRIPT_HEADER_ITEM.equals(((FilteredHeaderItem) headerItem).getFilterName())));
    }
}
