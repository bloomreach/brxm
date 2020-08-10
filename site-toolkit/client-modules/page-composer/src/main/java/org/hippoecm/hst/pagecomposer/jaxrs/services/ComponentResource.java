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

import java.io.Serializable;
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
import org.hippoecm.hst.pagecomposer.jaxrs.services.action.Action;
import org.hippoecm.hst.pagecomposer.jaxrs.services.action.ActionContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.action.ActionService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.state.State;
import org.hippoecm.hst.pagecomposer.jaxrs.services.state.StateContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.state.StateService;
import org.hippoecm.hst.pagecomposer.jaxrs.util.UUIDUtils;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;

import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.CHANNEL_VIEWER_PRIVILEGE_NAME;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.XPAGE_REQUIRED_PRIVILEGE_NAME;

@Path("/hst:component/")
public class ComponentResource extends AbstractConfigResource {

    private ActionService actionService;
    private StateService stateService;

    public void setActionService(final ActionService actionService) {
        this.actionService = actionService;
    }

    public void setStateService(final StateService stateService) {
        this.stateService = stateService;
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
            final Map<String, Serializable> contextPayload = cmsSessionContext.getContextPayload();
            final PageComposerContextService pageComposerContextService = getPageComposerContextService();
            final ActionContext actionContext = new ActionContext(
                    pageComposerContextService,
                    siteMapItemUuid,
                    contextPayload,
                    hostGroup
            );
            final Map<String, Set<Action>> actionsByCategory = actionService.getActionsByCategory(actionContext);

            final StateContext stateContext = new StateContext(pageComposerContextService);
            final Map<String, Set<State>> statesByCategory = stateService.getStatesByCategory(stateContext);

            return ok("", ActionsAndStatesRepresentation.represent(actionsByCategory, statesByCategory), false);
        });
    }
}
