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
import java.util.Locale;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.hippoecm.frontend.navigation.NavigationItem;
import org.hippoecm.frontend.navigation.NavigationItemService;
import org.onehippo.repository.jaxrs.api.SessionRequestContextProvider;

@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public final class NavigationItemResource {

    private SessionRequestContextProvider sessionRequestContextProvider;
    private NavigationItemService navigationItemService;

    void setSessionRequestContextProvider(final SessionRequestContextProvider sessionRequestContextProvider) {
        this.sessionRequestContextProvider = sessionRequestContextProvider;
    }

    void setNavigationItemService(final NavigationItemService navigationItemService) {
        this.navigationItemService = navigationItemService;
    }

    @GET
    public List<NavigationItem> getNavigationItems(@Context HttpServletRequest request) {
        return navigationItemService.getNavigationItems(getUserSession(request), getLocale(request));
    }

    private Session getUserSession(HttpServletRequest request) {
        return sessionRequestContextProvider.getJcrSession(request);
    }

    private Locale getLocale(final HttpServletRequest request) {
        return sessionRequestContextProvider.getLocale(request);
    }
}
