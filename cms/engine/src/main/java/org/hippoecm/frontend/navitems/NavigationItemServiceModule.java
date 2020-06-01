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

import java.util.function.Predicate;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.frontend.navigation.NavigationItemService;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.jaxrs.api.JsonResourceServiceModule;
import org.onehippo.repository.jaxrs.api.SessionRequestContextProvider;
import org.onehippo.repository.l10n.LocalizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_USERROLE;

public class NavigationItemServiceModule extends JsonResourceServiceModule {

    private static final Logger log = LoggerFactory.getLogger(NavigationItemServiceModule.class);

    private NavigationItemService navigationItemService;

    @Override
    protected Object getRestResource(SessionRequestContextProvider sessionRequestContextProvider) {
        final Predicate<Node> hasAccess =
                node -> {
                    try {
                        return node.hasProperty(HIPPO_USERROLE) && ((HippoSession) node.getSession()).isUserInRole(node.getProperty(HIPPO_USERROLE).getString());
                    } catch (RepositoryException e) {
                        log.error("Cannot determine if user is in role, returning false as default.", e);
                        return false;
                    }
                };
        navigationItemService = createNavigationItemService(hasAccess);

        final NavigationItemResource navigationItemResource = new NavigationItemResource();
        navigationItemResource.setSessionRequestContextProvider(sessionRequestContextProvider);
        navigationItemResource.setNavigationItemService(navigationItemService);

        return navigationItemResource;
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        super.doInitialize(session);
        HippoServiceRegistry.register(navigationItemService, NavigationItemService.class);
    }

    @Override
    protected void doShutdown() {
        HippoServiceRegistry.unregister(navigationItemService, NavigationItemService.class);
        super.doShutdown();
    }

    static NavigationItemService createNavigationItemService(Predicate<Node> hasAccess) {
        final LocalizationService localizationService = HippoServiceRegistry.getService(LocalizationService.class);
        final NavigationItemLocalizerImpl navigationItemLocalizer = new NavigationItemLocalizerImpl(localizationService);
        final NavigationItemServiceImpl navigationItemService = new NavigationItemServiceImpl();
        navigationItemService.setNavigationItemLocalizer(navigationItemLocalizer);
        navigationItemService.setNavigationItemStore(new NavigationItemStoreImpl(hasAccess));
        return navigationItemService;
    }

}
