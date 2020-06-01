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

package org.hippoecm.frontend.navitems;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.navigation.NavigationItem;
import org.hippoecm.frontend.navigation.NavigationItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class NavigationItemServiceImpl implements NavigationItemService {

    static final String APP_IFRAME_URL = String.format("/?%s", Main.CMS_AS_IFRAME_QUERY_PARAMETER);
    private static final Logger log = LoggerFactory.getLogger(NavigationItemServiceImpl.class);

    private NavigationItemStore navigationItemStore;
    private NavigationItemLocalizer navigationItemLocalizer;

    void setNavigationItemStore(NavigationItemStore navigationItemStore) {
        this.navigationItemStore = navigationItemStore;
    }

    void setNavigationItemLocalizer(NavigationItemLocalizer navigationItemLocalizer) {
        this.navigationItemLocalizer = navigationItemLocalizer;
    }

    @Override
    public List<NavigationItem> getNavigationItems(Session userSession, Locale locale) {
        try {
            final List<NavigationItem> navigationItems = navigationItemStore.getNavigationItems(userSession);
            navigationItems.forEach(navigationItem -> decorate(locale, navigationItem));
            return navigationItems;
        } catch (RepositoryException e) {
            log.error("Failed to get navigation items for user with id '{}'", userSession.getUserID(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public NavigationItem getNavigationItem(Session userSession, String pluginClass, Locale locale) {
        try {
            final NavigationItem navigationItem = navigationItemStore.getNavigationItem(pluginClass, userSession);
            decorate(locale, navigationItem);
            return navigationItem;
        } catch (RepositoryException e) {
            log.error("Failed to get navigation items for user with id '{}'", userSession.getUserID(), e);
            return null;
        }
    }

    private void decorate(Locale locale, NavigationItem navigationItem) {
        if (navigationItem != null) {
            navigationItem.setAppIframeUrl(APP_IFRAME_URL);
            navigationItemLocalizer.localize(navigationItem, locale);
        }
    }
}
