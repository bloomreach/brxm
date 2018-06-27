/*
*  Copyright 2010-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
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
import org.hippoecm.hst.channelmanager.security.SecurityModel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.exceptions.ChannelNotFoundException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.BeforeChannelDeleteEvent;
import org.hippoecm.hst.pagecomposer.jaxrs.api.annotation.IgnoreLock;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ChannelInfoDescription;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.channelmanager.security.SecurityModel.CHANNEL_MANAGER_ADMIN_ROLE;
import static org.hippoecm.hst.channelmanager.security.SecurityModel.CHANNEL_WEBMASTER_ROLE;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.HstConfigurationServiceImpl.PREVIEW_SUFFIX;

@Path("/rep:root/")
public class RootResource extends AbstractConfigResource {

    private static final Logger log = LoggerFactory.getLogger(RootResource.class);
    private boolean isCrossChannelPageCopySupported;

    // TODO get rid of this channel service! We already have one, see org.hippoecm.hst.platform.api.ChannelService
    private ChannelService channelService;
    private SecurityModel securityModel;

    public void setChannelService(final ChannelService channelService) {
        this.channelService = channelService;
    }

    public void setSecurityModel(final SecurityModel securityModel) {
        this.securityModel = securityModel;
    }

    @Override
    public void setComponentManager(ComponentManager componentManager) {
        super.setComponentManager(componentManager);

        final ContainerConfiguration config = componentManager.getContainerConfiguration();
        isCrossChannelPageCopySupported = config.getBoolean("cross.channel.page.copy.supported", false);
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
    public Response getChannels(@HeaderParam("hostGroup") final String hostGroup,
                                @QueryParam("previewConfigRequired") final boolean previewConfigRequired,
                                @QueryParam("workspaceRequired") final boolean workspaceRequired,
                                @QueryParam("skipBranches") final boolean skipBranches,
                                @QueryParam("skipConfigurationLocked") final boolean skipConfigurationLocked) {
        try {
            final List<Channel> channels = this.channelService.getChannels(previewConfigRequired,
                    workspaceRequired,
                    skipBranches,
                    skipConfigurationLocked,
                    hostGroup);
            return ok("Fetched channels successful", channels);
        } catch (RuntimeRepositoryException e) {
            log.warn("Could not determine authorization", e);
            return error("Could not determine authorization", e);
        }
    }

    @GET
    @Path("/channels/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannel(@HeaderParam("hostGroup") final String hostGroup,
                               @PathParam("id") String channelId) {
        try {
            final Channel channel = getPageComposerContextService().getEditingPreviewVirtualHosts().getChannelById(hostGroup, channelId);
            if (channel == null) {
                throw new ChannelNotFoundException(channelId);
            }
            return Response.ok().entity(channel).build();
        } catch (ChannelNotFoundException e) {
            log.warn(e.getMessage());
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .build();
        } catch (RuntimeRepositoryException | ChannelException e) {
            final String error = "Could not determine authorization";
            log.warn(error, e);
            return Response.serverError().entity(error).build();
        }
    }

    @GET
    @Path("/channels/{id}/info")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannelInfoDescription(@HeaderParam("hostGroup") final String hostGroup,
                                              @PathParam("id") String channelId, @QueryParam("locale") String locale) {
        if (StringUtils.isEmpty(locale)) {
            locale = Locale.ENGLISH.getLanguage();
        }
        try {
            final ChannelInfoDescription channelInfoDescription = channelService.getChannelInfoDescription(channelId, locale, hostGroup);
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
    public Response saveChannel(@HeaderParam("hostGroup") final String hostGroup,
                                @PathParam("id") final String channelId,
                                final Channel channel) {
        try {
            final Session session = RequestContextProvider.get().getSession();
            this.channelService.saveChannel(session, channelId, channel, hostGroup);
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
    @RolesAllowed(CHANNEL_MANAGER_ADMIN_ROLE)
    public Response deleteChannel(@HeaderParam("hostGroup") final String hostGroup,
                                  @PathParam("id") String channelId) {
        if (StringUtils.endsWith(channelId, PREVIEW_SUFFIX)) {
            // strip the preview suffix
            channelId = StringUtils.removeEnd(channelId, PREVIEW_SUFFIX);
        }

        try {
            final HstRequestContext hstRequestContext = RequestContextProvider.get();
            final Session session = hstRequestContext.getSession();
            final Channel channel = channelService.getChannel(channelId, hostGroup);
            final List<Mount> mountsOfChannel = channelService.findMounts(channel);

            channelService.preDeleteChannel(session, channel, mountsOfChannel);

            publishSynchronousEvent(new BeforeChannelDeleteEvent(channel, mountsOfChannel, hstRequestContext));

            channelService.deleteChannel(session, channel, mountsOfChannel);
            removeRenderingMountId();
            HstConfigurationUtils.persistChanges(session);

            return Response.ok().build();
        } catch (ChannelNotFoundException e) {
            printErrorLog("Failed to delete channel", e);

            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (ClientException e) {
            resetSession();
            return logAndReturnClientError(e);
        } catch (RepositoryException | ChannelException e) {
            printErrorLog("Failed to delete channel", e);
            resetSession();
            return Response.serverError().build();
        }
    }

    protected void removeRenderingMountId() {
        getPageComposerContextService().removeRenderingMountId();
    }

    @GET
    @Path("/composermode/{renderingHost}/{mountId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response composerModeGet(@HeaderParam("hostGroup") final String hostGroup,
                                    @Context HttpServletRequest servletRequest,
                                    @PathParam("renderingHost") String renderingHost,
                                    @PathParam("mountId") String mountId) {
        HttpSession session = servletRequest.getSession(true);


        // TODO HSTTWO-4374 can we share this information cleaner between platform webapp and site webapps?
        final CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(session);
        final Map<String, Serializable> contextPayload = cmsSessionContext.getContextPayload();
        contextPayload.put(ContainerConstants.RENDERING_HOST, renderingHost);
        contextPayload.put(ContainerConstants.COMPOSER_MODE_ATTR_NAME, Boolean.TRUE);
        contextPayload.put(ContainerConstants.CMS_REQUEST_RENDERING_MOUNT_ID, mountId);

        final HstRequestContext requestContext = getPageComposerContextService().getRequestContext();

        // HSTTWO-4365 TODO below does not get the correct channel and does not check for the correct webapp (platform at this moment!)
        final boolean isChannelDeletionSupported = isChannelDeletionSupported(mountId, hostGroup);
        final boolean isConfigurationLocked = isConfigurationLocked(mountId, hostGroup);
        try {
            final boolean hasAdminRole = securityModel.isUserInRole(requestContext.getSession(), CHANNEL_MANAGER_ADMIN_ROLE);
            final boolean isWebmaster = securityModel.isUserInRole(requestContext.getSession(), CHANNEL_WEBMASTER_ROLE);
            final boolean canDeleteChannel = isChannelDeletionSupported && hasAdminRole && !isConfigurationLocked;
            final boolean canManageChanges = hasAdminRole && !isConfigurationLocked;

            HandshakeResponse response = new HandshakeResponse();
            response.setCanWrite(isWebmaster);
            response.setCanManageChanges(canManageChanges);
            response.setCanDeleteChannel(canDeleteChannel);
            response.setCrossChannelPageCopySupported(isCrossChannelPageCopySupported);
            response.setSessionId(session.getId());
            log.info("Composer-Mode successful");
            return ok("Composer-Mode successful", response);
        } catch (IllegalStateException | RepositoryException e) {
            return error("Could not determine authorization or role", e);
        }
    }

    private boolean isChannelDeletionSupported(final String mountId, final String hostGroup) {
        return channelService.getChannelByMountId(mountId, hostGroup)
                .map(channel -> channelService.canChannelBeDeleted(channel) && channelService.isMaster(channel))
                .orElse(false);
    }

    private boolean isConfigurationLocked(final String mountId, final String hostGroup) {
        return channelService.getChannelByMountId(mountId, hostGroup)
                .map(channel -> channel.isConfigurationLocked())
                .orElse(false);
    }

    @GET
    @Path("/previewmode/{renderingHost}/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response previewMode(@Context HttpServletRequest servletRequest,
                                @PathParam("renderingHost") String renderingHost) {
        HttpSession session = servletRequest.getSession(true);

        // TODO HSTTWO-4374 can we share this information cleaner between platform webapp and site webapps?
        CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(session);
        final Map<String, Serializable> contextPayload = cmsSessionContext.getContextPayload();
        contextPayload.put(ContainerConstants.RENDERING_HOST, renderingHost);
        contextPayload.put(ContainerConstants.COMPOSER_MODE_ATTR_NAME, Boolean.FALSE);
        log.info("Preview-Mode successful");
        return ok("Preview-Mode successful", null);
    }

    @IgnoreLock
    @POST
    @Path("/setvariant/{variantId}/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setVariant(@Context HttpServletRequest servletRequest, @PathParam("variantId") String variant) {
        servletRequest.getSession().setAttribute(ContainerConstants.RENDER_VARIANT, variant);
        log.info("Variant '{}' set", variant);
        return ok("Variant set");
    }

    @IgnoreLock
    @POST
    @Path("/clearvariant/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearVariant(@Context HttpServletRequest servletRequest) {
        servletRequest.getSession().removeAttribute(ContainerConstants.RENDER_VARIANT);
        log.info("Variant cleared");
        return ok("Variant cleared");
    }

    private void printErrorLog(final String errorMessage, final Exception e) {
        if (log.isDebugEnabled()) {
            log.error(errorMessage, e);
        } else {
            log.error(errorMessage, e.getMessage());
        }
    }

    /**
     * Note: Override the AbstractConfigResource#logAndReturnClientError() to remove ExtResponseRepresentation wrapper
     * in the body response.
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
        private boolean isCrossChannelPageCopySupported;
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

        public boolean isCrossChannelPageCopySupported() {
            return isCrossChannelPageCopySupported;
        }

        public void setCrossChannelPageCopySupported(final boolean crossChannelPageCopySupported) {
            isCrossChannelPageCopySupported = crossChannelPageCopySupported;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(final String sessionId) {
            this.sessionId = sessionId;
        }
    }

}
