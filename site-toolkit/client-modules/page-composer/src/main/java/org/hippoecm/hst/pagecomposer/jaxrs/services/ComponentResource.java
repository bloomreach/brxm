/*
 * Copyright 2020 Bloomreach
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.hippoecm.hst.pagecomposer.jaxrs.api.annotation.PrivilegesAllowed;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ActionsAndStatesRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.component.Action;
import org.hippoecm.hst.pagecomposer.jaxrs.services.component.ActionState;
import org.hippoecm.hst.pagecomposer.jaxrs.services.component.ActionStateContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.component.ActionStateService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.component.NamedCategory;
import org.hippoecm.hst.pagecomposer.jaxrs.services.component.State;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.platform.utils.UUIDUtils;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.CHANNEL_VIEWER_PRIVILEGE_NAME;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.XPAGE_REQUIRED_PRIVILEGE_NAME;

@Path("/hst:component/")
public class ComponentResource extends AbstractConfigResource {

    private ActionStateService actionStateService;

    public void setActionStateService(final ActionStateService actionStateService) {
        this.actionStateService = actionStateService;
    }

    @GET
    @Path("/item/{siteMapItemUuid}")
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed({CHANNEL_VIEWER_PRIVILEGE_NAME, XPAGE_REQUIRED_PRIVILEGE_NAME})
    public Object getActionsAndStates(
            @HeaderParam("hostGroup") String hostGroup,
            @Context HttpServletRequest servletRequest,
            @PathParam("siteMapItemUuid") String siteMapItemUuid
    ) {
        return tryGet(() -> {
            if (!UUIDUtils.isValidUUID(siteMapItemUuid)) {
                final String message = String.format("path parameter 'siteMapItemUuid'%s is not a valid UUID", siteMapItemUuid);
                throw new ClientException(message, ClientError.INVALID_UUID);
            }
            final HttpSession session = servletRequest.getSession(false);
            if (session == null) {
                throw new IllegalStateException("Session should never be null here");
            }
            final CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(session);
            if (cmsSessionContext == null) {
                throw new IllegalStateException("CmsSessionContext should never be null here");
            }
            final ActionStateContext context = new ActionStateContext(
                    getPageComposerContextService(),
                    cmsSessionContext,
                    siteMapItemUuid,
                    hostGroup
            );
            final ActionState actionState = actionStateService.getActionState(context);
            final Map<String, Set<Action>> actionsByCategory = actionState.getActions().entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .map(this::toAction)
                    .collect(groupingBy(Action::getCategory, toSet()));
            final Map<String, Set<State>> statesByCategory = actionState.getStates().entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .map(this::toState)
                    .collect(groupingBy(State::getCategory, toSet()));
            return ok("", ActionsAndStatesRepresentation.represent(actionsByCategory, statesByCategory), false);
        });
    }

    private Action toAction(Map.Entry<NamedCategory, Boolean> entry) {
        final NamedCategory namedCategory = entry.getKey();
        final Boolean enabled = entry.getValue();
        return new Action(namedCategory.getName(), namedCategory.getCategory().getName(), enabled);
    }

    private State toState(Map.Entry<NamedCategory, Object> entry) {
        final NamedCategory namedCategory = entry.getKey();
        final Object value = entry.getValue();
        return new State(namedCategory.getName(), namedCategory.getCategory().getName(), value);
    }
}
