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

package org.hippoecm.frontend.service.navappsettings;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hippoecm.frontend.service.NavAppResource;
import org.hippoecm.frontend.service.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toMap;
import static org.hippoecm.frontend.service.ResourceType.IFRAME;
import static org.hippoecm.frontend.service.ResourceType.REST;

final class NavAppResourceServiceImpl implements NavAppResourceService {

    private static final Logger log = LoggerFactory.getLogger(NavAppResourceServiceImpl.class);

    static final String NAVIGATION_ITEMS_KEY_PREFIX = "brx.navapp.resource.navigationitems";
    static final String LOGIN_KEY_PREFIX = "brx.navapp.resource.login";
    static final String LOGOUT_KEY_PREFIX = "brx.navapp.resource.logout";

    private static final String URL = "url";
    private static final String TYPE = "type";

    private final Properties properties;

    NavAppResourceServiceImpl(Properties properties) {
        this.properties = properties;
    }

    @Override
    public Set<NavAppResource> getNavigationItemsResources() {
        return getResources(NAVIGATION_ITEMS_KEY_PREFIX);
    }

    @Override
    public Set<NavAppResource> getLoginResources() {
        return getResources(LOGIN_KEY_PREFIX);
    }

    @Override
    public Set<NavAppResource> getLogoutResources() {
        return getResources(LOGOUT_KEY_PREFIX);
    }

    private Set<NavAppResource> getResources(String keyPrefix) {
        final Map<NavAppResource, List<String>> resourceToKeyMap = new HashMap<>();
        for (Object key : properties.keySet()) {
            if (key.toString().startsWith(keyPrefix)) {
                final NavAppResource navAppResource = createResource(key.toString());
                resourceToKeyMap.computeIfAbsent(navAppResource, r -> new ArrayList<>()).add(key.toString());
            }
        }
        final Map<NavAppResource, List<String>> duplicates = resourceToKeyMap.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (duplicates.isEmpty()) {
            return resourceToKeyMap.keySet();
        }
        throw new IllegalArgumentException(
                "Duplicate values found: " + resourceToKeyMap + "\n" +
                        "Please make sure that all values for keys with prefix " + keyPrefix + " are unique.");
    }

    private NavAppResource createResource(String key) {

        final List<String> values = Arrays.asList(properties.getProperty(key).split(","));
        log.debug("{} = {} ", key, values);

        if (values.size() < 2) {
            throw invalidValue(key, values.isEmpty() ? "" : values.get(0), key);
        }

        final ResourceType resourceType = getResourceType(key, values.get(0));
        final URI resourceUrl = getResourceUrl(key, values.get(1));
        return new NavAppResourceBuilder()
                .resourceType(resourceType)
                .resourceUrl(resourceUrl)
                .build();
    }

    private URI getResourceUrl(String key, String urlString) {
        try {
            return URI.create(urlString.trim());
        } catch (IllegalArgumentException e) {
            throw invalidValue(URL, urlString, key);
        }
    }

    private ResourceType getResourceType(String key, String resourceTypeString) {
        try {
            return ResourceType.valueOf(resourceTypeString.trim());
        } catch (IllegalArgumentException e) {
            throw invalidValue(TYPE, resourceTypeString, key);
        }
    }

    private IllegalArgumentException invalidValue(String prop, Object value, String key) {
        final String message = String.format(
                "Cannot create a NavAppResource from property value with key %s.%n"
                        + "%s is not a valid value for %s.%n"
                        + "Please use the following format:%n"
                        + "%s = " + TYPE + ", " + URL + "%n"
                        + "where " + TYPE + " is one of " + REST + ", " + IFRAME + "%n"
                        + "and " + URL + " is a valid url.%n"
                , key, value, prop, key);
        return new IllegalArgumentException(message);
    }

}
