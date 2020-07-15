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
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.hippoecm.hst.pagecomposer.jaxrs.api.annotation.PrivilegesAllowed;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ActionsRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.action.Action;
import org.hippoecm.hst.pagecomposer.jaxrs.services.action.ActionService;

import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.CHANNEL_VIEWER_PRIVILEGE_NAME;

@Path("/hst:component/")
public class ComponentResource extends AbstractConfigResource {

    private ActionService actionService;

    public void setActionService(final ActionService actionService) {
        this.actionService = actionService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(CHANNEL_VIEWER_PRIVILEGE_NAME)
    public Object getActions(@QueryParam("unwrapped") boolean unwrapped) {
        if (unwrapped) {
            return getActionsRepresentation();
        } else {
            return tryGet(() -> ok("", getActionsRepresentation(), false));
        }
    }

    private ActionsRepresentation getActionsRepresentation() {
        final Map<String, Set<Action>> actionsByCategory = actionService.getActionsByCategory(getPageComposerContextService());
        return ActionsRepresentation.represent(actionsByCategory);
    }
}
