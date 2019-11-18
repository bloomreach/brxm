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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hippoecm.frontend.service.NavAppResource;
import org.hippoecm.frontend.service.ResourceType;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toMap;
import static org.hippoecm.frontend.service.ResourceType.IFRAME;
import static org.hippoecm.frontend.service.ResourceType.REST;

final class NavAppResourceServiceImpl implements NavAppResourceService {

    private static final Logger log = LoggerFactory.getLogger(NavAppResourceServiceImpl.class);

    static final String NAVAPP_LOCATION_KEY = "brx.navapp.location";
    static final String NAVIGATION_ITEMS_KEY_PREFIX = "brx.navapp.resource.navigationitems";
    static final String LOGIN_KEY_PREFIX = "brx.navapp.resource.login";
    static final String LOGOUT_KEY_PREFIX = "brx.navapp.resource.logout";

    private static final String URL = "url";
    private static final String TYPE = "type";

    private final ContainerConfiguration containerConfiguration;
    private final Set<NavAppResource> loginResources;
    private final Set<NavAppResource> logoutResources;
    private final Set<NavAppResource> navigationItemsResources;

    NavAppResourceServiceImpl(ContainerConfiguration containerConfiguration) {
        this.containerConfiguration = containerConfiguration;
        final Map<String, NavAppResource> loginResourceMap = getResources(LOGIN_KEY_PREFIX);
        final Map<String, NavAppResource> logoutResourceMap = getResources(LOGOUT_KEY_PREFIX);
        final Map<String, NavAppResource> navigationItemsResourceMap = getResources(NAVIGATION_ITEMS_KEY_PREFIX);

        if (!loginResourceMap.isEmpty() || !logoutResourceMap.isEmpty()) {
            final Set<String> loginKeys = new HashSet<>(loginResourceMap.keySet());
            final Set<String> logoutKeys = new HashSet<>(logoutResourceMap.keySet());

            final StringBuilder loginLogoutMissing = new StringBuilder();
            final String missing = " is missing" + System.lineSeparator();
            logoutKeys.stream()
                    .filter(k -> !loginKeys.contains(k))
                    .forEach(logoutKey -> loginLogoutMissing.append(LOGIN_KEY_PREFIX).append(".").append(logoutKey).append(missing));
            loginKeys.stream()
                    .filter(k -> !logoutKeys.contains(k))
                    .forEach(logoutKey -> loginLogoutMissing.append(LOGOUT_KEY_PREFIX).append(".").append(logoutKey).append(missing));
            loginKeys.addAll(logoutKeys);
            if (loginLogoutMissing.length() > 0) {
                loginLogoutMissing.append("Please configure a logout resource for every login resource and vice versa.")
                        .append(System.lineSeparator());
            }

            final Set<String> navigationItemKeys = navigationItemsResourceMap.keySet();
            final StringBuilder navigationItemResourceMissing = new StringBuilder();
            loginKeys.stream()
                    .filter(k -> !navigationItemKeys.contains(k))
                    .forEach(logoutKey -> navigationItemResourceMissing.append(NAVIGATION_ITEMS_KEY_PREFIX).append(".").append(logoutKey).append(missing));
            if (navigationItemResourceMissing.length() > 0) {
                navigationItemResourceMissing
                        .append("Please configure a navigationitems resource for every login/logout resource pair")
                        .append(System.lineSeparator());
            }

            loginLogoutMissing.append(navigationItemResourceMissing);
            if (loginLogoutMissing.length() > 0) {
                loginLogoutMissing.insert(0, "Invalid property values detected" + System.lineSeparator());
                throw new IllegalArgumentException(loginLogoutMissing.toString());
            }
        }

        this.loginResources = new HashSet<>(loginResourceMap.values());
        this.logoutResources = new HashSet<>(logoutResourceMap.values());
        this.navigationItemsResources = new HashSet<>(navigationItemsResourceMap.values());
    }

    @Override
    public Optional<URI> getNavAppResourceUrl() {
        return Optional.ofNullable(
                containerConfiguration.getString(NAVAPP_LOCATION_KEY,
                        System.getProperty(NAVAPP_LOCATION_KEY, null)))
                .map(URI::create);
    }

    @Override
    public Set<NavAppResource> getNavigationItemsResources() {
        return navigationItemsResources;
    }

    @Override
    public Set<NavAppResource> getLoginResources() {
        return loginResources;
    }

    @Override
    public Set<NavAppResource> getLogoutResources() {
        return logoutResources;
    }

    private Map<String, NavAppResource> getResources(String keyPrefix) {
        final Map<NavAppResource, List<String>> resourceToKeyMap = new HashMap<>();
        final Map<String, NavAppResource> keyToResourceMap = new HashMap<>();
        final Iterable<String> keys = containerConfiguration::getKeys;
        for (String key : keys) {
            if (key.startsWith(keyPrefix)) {
                final NavAppResource navAppResource = createResource(key);
                keyToResourceMap.put(key.substring(keyPrefix.length() + 1), navAppResource);
                resourceToKeyMap.computeIfAbsent(navAppResource, r -> new ArrayList<>()).add(key);
            }
        }
        final Map<NavAppResource, List<String>> duplicates = resourceToKeyMap.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (duplicates.isEmpty()) {
            return keyToResourceMap;
        }
        throw new IllegalArgumentException(
                "Duplicate values found: " + resourceToKeyMap + "\n" +
                        "Please make sure that all values for keys with prefix " + keyPrefix + " are unique.");
    }

    private NavAppResource createResource(String key) {

        final List<String> values = containerConfiguration.getList(key);
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
