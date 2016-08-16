/*
*  Copyright 2010-2016 Hippo B.V. (http://www.onehippo.com)
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/
package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.apache.cxf.common.i18n.Exception;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.exceptions.ChannelNotFoundException;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelEvent;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ChannelInfoDescription;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.FeaturesRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/rep:root/")
public class RootResource extends AbstractConfigResource {

    private static final Logger log = LoggerFactory.getLogger(RootResource.class);
    private String rootPath;

    private ChannelService channelService;

    public void setChannelService(final ChannelService channelService) {
        this.channelService = channelService;
    }

    public void setRootPath(final String rootPath) {
        this.rootPath = rootPath;
    }

    @GET
    @Path("/keepalive")
    @Produces(MediaType.APPLICATION_JSON)
    public Response keepalive(@Context HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            return ok("OK", session.getMaxInactiveInterval());
        } else {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Error: No http session on the request found.").build();
        }
    }

    @GET
    @Path("/channels")
    public Response getChannels(@QueryParam("previewConfigRequired") final boolean previewConfigRequired,
                                @QueryParam("workspaceRequired") final boolean workspaceRequired) {
        try {
          final List<Channel> channels = this.channelService.getChannels(previewConfigRequired, workspaceRequired);
            return ok("Fetched channels successful", channels);
        } catch (RuntimeRepositoryException e) {
            log.warn("Could not determine authorization", e);
            return error("Could not determine authorization", e);
        }
    }

    @GET
    @Path("/channels/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannel(@PathParam("id") String channelId) {
        try {
            final Channel channel = channelService.getChannel(channelId);
            return Response.ok().entity(channel).build();
        } catch (RuntimeRepositoryException e) {
            final String error = "Could not determine authorization";
            log.warn(error, e);
            return Response.serverError().entity(error).build();
        }
    }

    @GET
    @Path("/channels/{id}/info")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannelInfoDescription(@PathParam("id") String channelId, @QueryParam("locale") String locale) {
        if (StringUtils.isEmpty(locale)) {
            locale = Locale.ENGLISH.getLanguage();
        }
        try {
            final ChannelInfoDescription channelInfoDescription = channelService.getChannelInfoDescription(channelId, locale);
            return Response.ok().entity(channelInfoDescription).build();
        } catch (ChannelException e) {
            final String error = "Could not get channel setting information";
            log.warn(error, e);
            return Response.serverError().entity(error).build();
        }
    }

    /**
     * Update field setting properties of a channel
     */
    @PUT
    @Path("/channels/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveChannel(@PathParam("id") String channelId, final Channel channel) {
        try {
            final Session session = RequestContextProvider.get().getSession();
            this.channelService.saveChannel(session, channelId, channel);
            HstConfigurationUtils.persistChanges(session);

            return Response.ok().entity(channel).build();
        } catch (RepositoryException | IllegalStateException | ChannelException e) {
            log.error("Failed to saveChannel channel", e);
            return Response.serverError().build();
        }
    }

    @DELETE
    @Path("/channels/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteChannel(@PathParam("id") String channelId) {
        try {
            final Session session = RequestContextProvider.get().getSession();
            this.channelService.deleteChannel(session, channelId);

            publishSynchronousEvent(new ChannelEvent(
                    ChannelEvent.ChannelEventType.DELETE,
                    Collections.emptyList(),
                    null,
                    null,
                    getPageComposerContextService().getRequestContext()));

            HstConfigurationUtils.persistChanges(session);

            return Response.ok().build();
        } catch (ChannelNotFoundException e) {
            log.error(e.getMessage());
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(e).build();
        } catch (ClientException e) {
            resetSession();
            return logAndReturnClientError(e);
        } catch (RepositoryException | ChannelException e) {
            log.error(e.getMessage());
            resetSession();
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/features")
    public Response getFeatures() {
        final boolean crossChannelPageCopySupported = HstServices.getComponentManager().getContainerConfiguration().getBoolean("cross.channel.page.copy.supported", false);
        final FeaturesRepresentation featuresRepresentation = new FeaturesRepresentation();
        featuresRepresentation.setCrossChannelPageCopySupported(crossChannelPageCopySupported);
        return ok("Fetched features", featuresRepresentation);
    }

    @GET
    @Path("/composermode/{renderingHost}/{mountId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response composerModeGet(@Context HttpServletRequest servletRequest,
                                    @PathParam("renderingHost") String renderingHost,
                                    @PathParam("mountId") String mountId) {
        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(ContainerConstants.RENDERING_HOST, renderingHost);
        session.setAttribute(ContainerConstants.COMPOSER_MODE_ATTR_NAME, Boolean.TRUE);
        session.setAttribute(ContainerConstants.CMS_REQUEST_RENDERING_MOUNT_ID, mountId);

        boolean canWrite;
        try {
            HstRequestContext requestContext = getPageComposerContextService().getRequestContext();
            canWrite = requestContext.getSession().hasPermission(rootPath + "/accesstest", Session.ACTION_SET_PROPERTY);
        } catch (RepositoryException e) {
            log.warn("Could not determine authorization", e);
            return error("Could not determine authorization", e);
        }

        final boolean channelDeletionSupported = HstServices.getComponentManager().getContainerConfiguration().getBoolean("channel.deletion.supported", false);

        // TODO: test whether the user has admin privileges
        final boolean canDeleteChannel = channelDeletionSupported;
        final boolean canManageChanges = true;

        HandshakeResponse response = new HandshakeResponse();
        response.setCanWrite(canWrite);
        response.setCanManageChanges(canManageChanges);
        response.setCanDeleteChannel(canDeleteChannel);
        response.setSessionId(session.getId());
        log.info("Composer-Mode successful");
        return ok("Composer-Mode successful", response);
    }

    @GET
    @Path("/previewmode/{renderingHost}/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response previewMode(@Context HttpServletRequest servletRequest,
                                @PathParam("renderingHost") String renderingHost) {
        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(ContainerConstants.RENDERING_HOST, renderingHost);
        session.setAttribute(ContainerConstants.COMPOSER_MODE_ATTR_NAME, Boolean.FALSE);
        log.info("Preview-Mode successful");
        return ok("Preview-Mode successful", null);
    }

    @POST
    @Path("/setvariant/{variantId}/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setVariant(@Context HttpServletRequest servletRequest, @PathParam("variantId") String variant) {
        servletRequest.getSession().setAttribute(ContainerConstants.RENDER_VARIANT, variant);
        log.info("Variant '{}' set", variant);
        return ok("Variant set");
    }

    @POST
    @Path("/clearvariant/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearVariant(@Context HttpServletRequest servletRequest) {
        servletRequest.getSession().removeAttribute(ContainerConstants.RENDER_VARIANT);
        log.info("Variant cleared");
        return ok("Variant cleared");
    }

    /**
     * Note: Override the AbstractConfigResource#logAndReturnClientError() to remove ExtResponseRepresentation wrapper in the
     * body response.
     */
    @Override
    protected Response logAndReturnClientError(final ClientException e) {
        final String formattedMessage = e.getMessage();
        if (log.isDebugEnabled()) {
            log.info(formattedMessage, e);
        } else {
            log.info(formattedMessage);
        }
        return Response.status(Response.Status.BAD_REQUEST).entity(e.getErrorStatus()).build();
    }

    private static class HandshakeResponse {

        private boolean canWrite;
        private boolean canManageChanges;
        private boolean canDeleteChannel;
        private String sessionId;

        public boolean isCanWrite() {
            return canWrite;
        }

        public void setCanWrite(final boolean canWrite) {
            this.canWrite = canWrite;
        }

        public boolean isCanManageChanges() {
            return canManageChanges;
        }

        public void setCanManageChanges(final boolean canManageChanges) {
            this.canManageChanges = canManageChanges;
        }

        public boolean isCanDeleteChannel() {
            return canDeleteChannel;
        }

        public void setCanDeleteChannel(final boolean canDeleteChannel) {
            this.canDeleteChannel = canDeleteChannel;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(final String sessionId) {
            this.sessionId = sessionId;
        }
    }

}
