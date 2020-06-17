/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.pagecomposer.jaxrs.api.annotation.PrivilegesAllowed;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ChannelMenuItem;
import org.hippoecm.hst.pagecomposer.jaxrs.model.MenuItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.MenuRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.PageMenuItem;
import org.hippoecm.hst.pagecomposer.jaxrs.model.XPageMenuItem;

import static java.util.stream.Collectors.toMap;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.CHANNEL_VIEWER_PRIVILEGE_NAME;

@Path("/hst:component/")
public class ComponentResource extends AbstractConfigResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(CHANNEL_VIEWER_PRIVILEGE_NAME)
    public Response getMenu() {
        return tryGet(() -> ok("", getMenuRepresentation(), false));
    }

    private MenuRepresentation getMenuRepresentation() {
        final MenuRepresentation menu = new MenuRepresentation();

        menu.setChannel(new MenuItemRepresentation(true, subItems(ChannelMenuItem.values())));
        menu.setPage(new MenuItemRepresentation(true, subItems(PageMenuItem.values())));
        menu.setXpage(new MenuItemRepresentation(true, subItems(XPageMenuItem.values())));
        return menu;
    }

    private Map<Enum<?>, MenuItemRepresentation> subItems(Enum<?>... items) {
        return Stream.of(items)
                .collect(toMap(Function.identity(), n -> new MenuItemRepresentation(true)));
    }
}
