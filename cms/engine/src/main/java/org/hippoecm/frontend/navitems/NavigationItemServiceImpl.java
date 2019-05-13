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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

final class NavigationItemServiceImpl implements NavigationItemService {

    private static final Logger log = LoggerFactory.getLogger(NavigationItemServiceImpl.class);

    private PerspectiveStore perspectiveStore;
    private NavigationItemFactory navigationItemFactory;

    public void setPerspectiveStore(PerspectiveStore perspectiveStore) {
        this.perspectiveStore = perspectiveStore;
    }

    public void setNavigationItemFactory(NavigationItemFactory navigationItemFactory) {
        this.navigationItemFactory = navigationItemFactory;
    }

    @Override
    public List<NavigationItem> getNavigationItems(Session userSession, String appIframeUrl) {
        try {
            return perspectiveStore
                    .getPerspectiveClassNames(userSession)
                    .stream()
                    .map(name -> navigationItemFactory.newInstance(name, appIframeUrl))
                    .collect(toList());
        } catch (RepositoryException e) {
            log.error("Failed to get perspectives for user with id '{}'", userSession.getUserID(), e);
            return Collections.emptyList();
        }
    }

}
