/*
*  Copyright 2010-2015 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.FeaturesRepresentation;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_PAGES;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_SITEMAP;

@Path("/rep:root/")
public class RootResource extends AbstractConfigResource {

    private static final Logger log = LoggerFactory.getLogger(RootResource.class);
    private String rootPath;

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
                                @QueryParam("workspacePagesRequired") final boolean workspacePagesRequired,
                                @QueryParam("workspaceSiteMapRequired") final boolean workspaceSiteMapRequired) {
        final HstRequestContext requestContext = RequestContextProvider.get();
        final VirtualHost virtualHost = requestContext.getResolvedMount().getMount().getVirtualHost();
        try {
            final List<Channel> channels = virtualHost.getVirtualHosts().getChannels(virtualHost.getHostGroupName())
                    .values()
                    .stream()
                    .filter(channel -> previewConfigRequiredFiltered(channel, previewConfigRequired))
                    .filter(channel -> workspacePagesFiltered(channel, workspacePagesRequired))
                    .filter(channel -> workspaceSiteMapFiltered(channel, workspaceSiteMapRequired))
                    .collect(toList());
            return ok("Fetched channels successful", channels);
        } catch (RuntimeRepositoryException e) {
            log.warn("Could not determine authorization", e);
            return error("Could not determine authorization", e);
        }
    }

    private boolean previewConfigRequiredFiltered(final Channel channel, final boolean previewConfigRequired) {
        if (!previewConfigRequired) {
            return true;
        }
        return channel.isPreview();
    }

    @GET
    @Path("/features")
    public Response getFeatures() {
        final Boolean crossChannelPageCopySupported = HstServices.getComponentManager().getContainerConfiguration().getBoolean("cross.channel.page.copy.supported", false);
        final FeaturesRepresentation featuresRepresentation = new FeaturesRepresentation();
        featuresRepresentation.setCrossChannelPageCopySupported(crossChannelPageCopySupported);
        final String msg = String.format("Fetched features");
        return ok(msg, featuresRepresentation);
    }

    private boolean workspacePagesFiltered(final Channel channel, final boolean required) throws RuntimeRepositoryException {
        return workspaceRequiredConfig(channel, required, NODENAME_HST_PAGES);
    }

    private boolean workspaceSiteMapFiltered(final Channel channel, final boolean required) throws RuntimeRepositoryException {
        return workspaceRequiredConfig(channel, required, NODENAME_HST_SITEMAP);
    }

    private boolean workspaceRequiredConfig(final Channel channel, final boolean required, final String nodeName) {
        if (!required) {
            return true;
        }
        final HstRequestContext requestContext = RequestContextProvider.get();
        final Mount mount = requestContext.getVirtualHost().getVirtualHosts().getMountByIdentifier(channel.getMountId());
        final String workspacePath = mount.getHstSite().getConfigurationPath() + "/" + HstNodeTypes.NODENAME_HST_WORKSPACE;
        final String requiredPath = workspacePath + "/" + nodeName;
        try {
            final boolean requiredPathExists = RequestContextProvider.get().getSession().nodeExists(requiredPath);
            return requiredPathExists;
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException(e);
        }
    }

    @GET
    @Path("/composermode/{renderingHost}/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response composerModeGet(@Context HttpServletRequest servletRequest,
                                    @PathParam("renderingHost") String renderingHost) {
        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(ContainerConstants.RENDERING_HOST, renderingHost);
        session.setAttribute(ContainerConstants.COMPOSER_MODE_ATTR_NAME, Boolean.TRUE);
        boolean canWrite;
        try {
            HstRequestContext requestContext = getPageComposerContextService().getRequestContext();
            canWrite = requestContext.getSession().hasPermission(rootPath + "/accesstest", Session.ACTION_SET_PROPERTY);
        } catch (RepositoryException e) {
            log.warn("Could not determine authorization", e);
            return error("Could not determine authorization", e);
        }

        HandshakeResponse response = new HandshakeResponse();
        response.setCanWrite(canWrite);
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
    @Path("/setvariant/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setVariant(@Context HttpServletRequest servletRequest, @FormParam("variant") String variant) {
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

    private static class HandshakeResponse {

        private boolean canWrite;
        private String sessionId;

        public boolean isCanWrite() {
            return canWrite;
        }

        public void setCanWrite(final boolean canWrite) {
            this.canWrite = canWrite;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(final String sessionId) {
            this.sessionId = sessionId;
        }
    }

}
