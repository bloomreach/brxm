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

package org.hippoecm.frontend.navigation;

import java.util.List;
import java.util.Locale;

import javax.jcr.Session;

public interface NavigationItemService {

    /**
     * Returns the list of {@link NavigationItem}s that the logged in user (identified by the userSession) is allowed to see.
     *
     * @param userSession jcr session of the logged in user
     * @param locale      the locale to use for localizing the displayName of a navigation item.
     * @return list of navigation items
     */
    List<NavigationItem> getNavigationItems(Session userSession, Locale locale);

    /**
     * Returns the navigation item for the given plugin class.
     *
     * @param userSession jcr session of the logged in user
     * @param pluginClass fully qualified name of the plugin class that belongs to the returned item.
     * @param locale      the locale to use for localizing the displayName of a navigation item.
     * @return
     */
    NavigationItem getNavigationItem(Session userSession, String pluginClass, Locale locale);
}
