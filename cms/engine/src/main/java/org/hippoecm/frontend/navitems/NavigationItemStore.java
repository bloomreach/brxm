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

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.frontend.navigation.NavigationItem;

public interface NavigationItemStore {

    /**
     * Returns the list of partially populated navigation items for the given session.
     *
     * @param session a jcr session.
     * @return list of fully qualified perspective class names.
     * @throws RepositoryException if querying the repository fails
     */
    List<NavigationItem> getNavigationItems(Session session) throws RepositoryException;

    NavigationItem getNavigationItem(String pluginClass, Session session) throws RepositoryException;
}
