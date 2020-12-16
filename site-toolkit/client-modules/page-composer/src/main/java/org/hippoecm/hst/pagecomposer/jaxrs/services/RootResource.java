/*
*  Copyright 2010-2020 Hippo B.V. (http://www.onehippo.com)
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

import javax.annotation.security.PermitAll;
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

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.exceptions.ChannelNotFoundException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentManagerAware;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.BeforeChannelDeleteEventImpl;
import org.hippoecm.hst.pagecomposer.jaxrs.api.annotation.ChannelAgnostic;
import org.hippoecm.hst.pagecomposer.jaxrs.api.annotation.IgnoreLock;
import org.hippoecm.hst.pagecomposer.jaxrs.api.annotation.PrivilegesAllowed;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ChannelInfoDescription;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils;
import org.hippoecm.hst.platform.model.HstModel;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.pagecomposer.jaxrs.services.HstConfigurationServiceImpl.PREVIEW_SUFFIX;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.CHANNEL_ADMIN_PRIVILEGE_NAME;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.CHANNEL_VIEWER_PRIVILEGE_NAME;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.CHANNEL_WEBMASTER_PRIVILEGE_NAME;
import static org.hippoecm.hst.util.JcrSessionUtils.isInRole;

@Path("/rep:root/")
public class RootResource extends AbstractConfigResource implements ComponentManagerAware {

    private static final Logger log = LoggerFactory.getLogger(RootResource.class);
    private boolean isCrossChannelPageCopySupported;

    private ChannelService channelService;

    public void setChannelService(final ChannelService channelService) {
        this.channelService = channelService;
    }

    @Override
    public void setComponentManager(ComponentManager componentManager) {
        final ContainerConfiguration config = componentManager.getContainerConfiguration();
        isCrossChannelPageCopySupported = config.getBoolean("cross.channel.page.copy.supported", false);
    }

    /**
     * This method returns only the channels from a single hst webapp which is on purpose since it is used for the
     * cross channel page copy which is not supported over cross webapp
     * NOTE #getChannels for something like 'page copy' to a different channel, then
     * privilegeAllowed=hippo:channel-webmaster should be in the query string since channels the user is not webmaster
     * on (s)he cannot copy pages to.
     *
     * The param @QueryParam("privilegeAllowed") final String privilegeAllowed can be used to only return channels
     * for which the current user has the privilege 'privilegeAllowed'. If missing, the minimal privilege
     * ChannelManagerPrivileges#CHANNEL_VIEWER_PRIVILEGE_NAME is assumed
     */
    @GET
    @Path("/channels")
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    @ChannelAgnostic
    public Response getChannels(@HeaderParam("hostGroup") final String hostGroup,
                                @QueryParam("previewConfigRequired") final boolean previewConfigRequired,
                                @QueryParam("workspaceRequired") final boolean workspaceRequired,
                                @QueryParam("skipBranches") final boolean skipBranches,
                                @QueryParam("skipConfigurationLocked") final boolean skipConfigurationLocked,
                                @QueryParam("privilegeAllowed") final String privilegeAllowed) {

        try {
            // TODO HSTTWO-4667 filter the channels like HstModel does: thus also filter on read-access for content!
            final List<Channel> channels = this.channelService.getChannels(previewConfigRequired,
                    workspaceRequired,
                    skipBranches,
                    skipConfigurationLocked,
                    hostGroup,
                    privilegeAllowed == null ? CHANNEL_VIEWER_PRIVILEGE_NAME : privilegeAllowed);
            return ok("Fetched channels successful", channels);
        } catch (RuntimeRepositoryException e) {
            log.warn("Could not determine authorization", e);
            return error("Could not determine authorization", e);
        }
    }

    @GET
    @Path("/channels/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    // PermitAll because this method is invoked before a channel has been set on the cms session context. Cleaner
    // would be if the cms-user would have 'viewer' privilege on target 'channelId' but then it becomes quite
    // complex to find which channel to check in PrivilegesAllowedInvokerPreprocessor
    @PermitAll
    @ChannelAgnostic
    public Response getChannel(@HeaderParam("contextPath") final String contextPath,
                               @HeaderParam("hostGroup") final String hostGroup,
                               @PathParam("id") String channelId) {
        try {

            HstModelRegistry hstModelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);
            final HstModel hstModel = hstModelRegistry.getHstModel(contextPath);

            if (hstModel == null) {
                throw new IllegalArgumentException(String.format("No HST Model present for context path '%s'", contextPath));
            }
            final Channel channel = hstModel.getVirtualHosts().getChannelById(hostGroup, channelId);
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
    @PrivilegesAllowed(CHANNEL_VIEWER_PRIVILEGE_NAME)
    public Response getChannelInfoDescription(@HeaderParam("hostGroup") final String hostGroup,
                                              @PathParam("id") String channelId, @QueryParam("locale") String locale) {
        if (StringUtils.isEmpty(locale)) {
            locale = Locale.ENGLISH.getLanguage();
        }
        try {
            final ChannelInfoDescription channelInfoDescription =
                    channelService.getChannelInfoDescription(channelId, locale, hostGroup);
            return Response.ok().entity(channelInfoDescription).build();
        } catch (ChannelException | RepositoryException e) {
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
    @PrivilegesAllowed(CHANNEL_WEBMASTER_PRIVILEGE_NAME)
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
    @PrivilegesAllowed(CHANNEL_ADMIN_PRIVILEGE_NAME)
    public Response deleteChannel(@HeaderParam("hostGroup") final String hostGroup,
                                  @PathParam("id") String channelId) {
        if (StringUtils.endsWith(channelId, PREVIEW_SUFFIX)) {
            // strip the preview suffix
            channelId = StringUtils.removeEnd(channelId, PREVIEW_SUFFIX);
        }

        try {
            final HstRequestContext hstRequestContext = RequestContextProvider.get();
            final Session session = hstRequestContext.getSession();
            final Channel channel = channelService.getChannel(RequestContextProvider.get().getSession(), channelId, hostGroup);
            final List<Mount> mountsOfChannel = channelService.findMounts(channel);

            channelService.preDeleteChannel(session, channel, mountsOfChannel);

            BeforeChannelDeleteEventImpl event = new BeforeChannelDeleteEventImpl(channel, mountsOfChannel, hstRequestContext);
            publishSynchronousEvent(event);

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
    // PermitAll because this method is invoked before a channel has been set on the cms session context, hence
    // PrivilegesAllowedInvokerPreprocessor does not yet 'know' which channel to check privileges for
    @PermitAll
    @ChannelAgnostic
    public Response composerModeGet(@HeaderParam("hostGroup") final String hostGroup,
                                    @Context HttpServletRequest servletRequest,
                                    @PathParam("renderingHost") String renderingHost,
                                    @PathParam("mountId") String mountId) {
        // session should never be null here
        final HttpSession session = servletRequest.getSession(false);
        if (session == null) {
            throw new IllegalStateException("Session should never be null here");
        }

        final CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(session);
        if (cmsSessionContext == null) {
            throw new IllegalStateException("CmsSessionContext should never be null here");
        }

        final Map<String, Serializable> contextPayload = cmsSessionContext.getContextPayload();

        contextPayload.put(ContainerConstants.RENDERING_HOST, renderingHost);
        contextPayload.put(ContainerConstants.CMS_REQUEST_RENDERING_MOUNT_ID, mountId);

        final HstRequestContext requestContext = getPageComposerContextService().getRequestContext();

        final boolean isChannelDeletionSupported = isChannelDeletionSupported(mountId, hostGroup);
        final boolean isConfigurationLocked = isConfigurationLocked(mountId, hostGroup);
        try {

            final Session jcrSession = requestContext.getSession();

            final boolean isChannelAdmin;
            final boolean isWebmaster;

            final String liveConfigPath = getPageComposerContextService().getEditingLiveConfigurationPath();
            if (getPageComposerContextService().hasPreviewConfiguration()) {

                isWebmaster = isInRole(jcrSession, liveConfigPath, CHANNEL_WEBMASTER_PRIVILEGE_NAME) &&
                        isInRole(jcrSession, getPreviewConfigurationPath(), CHANNEL_WEBMASTER_PRIVILEGE_NAME);

                isChannelAdmin = isInRole(jcrSession, liveConfigPath, CHANNEL_ADMIN_PRIVILEGE_NAME) &&
                        isInRole(jcrSession, getPreviewConfigurationPath(), CHANNEL_ADMIN_PRIVILEGE_NAME);

            } else {
                isWebmaster = isInRole(jcrSession, liveConfigPath, CHANNEL_WEBMASTER_PRIVILEGE_NAME);

                isChannelAdmin = isInRole(jcrSession, liveConfigPath, CHANNEL_ADMIN_PRIVILEGE_NAME);
            }

            final boolean canDeleteChannel = isChannelDeletionSupported && isChannelAdmin && !isConfigurationLocked;
            final boolean canManageChanges = isChannelAdmin && !isConfigurationLocked;

            HandshakeResponse response = new HandshakeResponse();
            response.setCanWriteHstConfig(isWebmaster);
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

    @IgnoreLock
    @POST
    @Path("/setvariant/{variantId}/")
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(CHANNEL_VIEWER_PRIVILEGE_NAME)
    public Response setVariant(@Context HttpServletRequest servletRequest, @PathParam("variantId") String variant) {
        final CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(servletRequest.getSession());
        cmsSessionContext.getContextPayload().put(ContainerConstants.RENDER_VARIANT, variant);
        log.info("Variant '{}' set", variant);
        return ok("Variant set");
    }

    @IgnoreLock
    @POST
    @Path("/clearvariant/")
    @Produces(MediaType.APPLICATION_JSON)
    @PrivilegesAllowed(CHANNEL_VIEWER_PRIVILEGE_NAME)
    public Response clearVariant(@Context HttpServletRequest servletRequest) {
        final CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(servletRequest.getSession());
        cmsSessionContext.getContextPayload().remove(ContainerConstants.RENDER_VARIANT);
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
     * Note: Override the AbstractConfigResource#logAndReturnClientError() to remove ResponseRepresentation wrapper
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

        private boolean canWriteHstConfig;
        private boolean canManageChanges;
        private boolean canDeleteChannel;
        private boolean isCrossChannelPageCopySupported;
        private String sessionId;

        public boolean isCanWriteHstConfig() {
            return canWriteHstConfig;
        }

        public void setCanWriteHstConfig(final boolean canWriteHstConfig) {
            this.canWriteHstConfig = canWriteHstConfig;
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
