/*
 *  Copyright 2010-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.cxf;

import java.util.Map;
import java.util.UUID;
import java.util.function.BiPredicate;

import javax.jcr.Credentials;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.platform.api.model.EventPathsInvalidator;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.internal.PreviewDecorator;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.jaxrs.cxf.CXFJaxrsService;
import org.hippoecm.hst.platform.api.model.InternalHstModel;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_MOUNT;
import static org.hippoecm.hst.core.container.ContainerConstants.CMS_REQUEST_RENDERING_MOUNT_ID;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService.EDITING_HST_MODEL_LINK_CREATOR_ATTR;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService.LIVE_EDITING_HST_MODEL_ATTR;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService.PREVIEW_EDITING_HST_MODEL_ATTR;

public class CXFJaxrsHstConfigService extends CXFJaxrsService {

    private static Logger log = LoggerFactory.getLogger(CXFJaxrsHstConfigService.class);

    public final static String REQUEST_CONFIG_NODE_IDENTIFIER = "org.hippoecm.hst.pagecomposer.jaxrs.cxf.contentNode.identifier";
    public final static String REQUEST_ERROR_MESSAGE_ATTRIBUTE = "org.hippoecm.hst.pagecomposer.jaxrs.cxf.exception.message";

    private Repository repository;
    private Credentials credentials;
    private PreviewDecorator previewDecorator;

    public CXFJaxrsHstConfigService(String serviceName) {
        super(serviceName);
    }

    public CXFJaxrsHstConfigService(String serviceName, Map<String, String> jaxrsConfigParameters) {
        super(serviceName, jaxrsConfigParameters);
    }

    public void setRepository(final Repository repository) {
        this.repository = repository;
    }

    public void setCredentials(final Credentials credentials) {
        this.credentials = credentials;
    }

    public void setPreviewDecorator(final PreviewDecorator previewDecorator) {
        this.previewDecorator = previewDecorator;
    }


    @Override
    /*
     * temporarily splitting off and saving suffix from pathInfo until this is generally handled with HSTTWO-1189
	 */
    protected String getJaxrsPathInfo(HstRequestContext requestContext, HttpServletRequest request) throws ContainerException {
        return requestContext.getPathSuffix();
    }

    @Override
    protected HttpServletRequest getJaxrsRequest(HstRequestContext requestContext, HttpServletRequest request) throws ContainerException {
        String uuid = PathUtils.normalizePath(requestContext.getBaseURL().getPathInfo());
        if (uuid == null) {
            throw new ContainerException("CXFJaxrsHstConfigService expects a 'uuid' as pathInfo but pathInfo was null. Cannot process REST call");
        }
        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throw new ContainerException("CXFJaxrsHstConfigService expects a 'uuid' as pathInfo but was '" + uuid + "'. Cannot process REST call");
        }

        String resourceType = "";

        Session session = null;
        try {
            // set the correct 'editing virtual hosts object' on the HstRequestContext : This hst request context is for the
            // platform webapp but the 'rest' endpoints need to interact with the hst model of the site webapps.
            final String contextPath = requestContext.getServletRequest().getHeader("contextPath");
            if (contextPath == null) {
                throw new IllegalArgumentException("'contextPath' header is missing");
            }

            // note you CANNOT use HstModelProvider spring bean since that will always give you the platform HstModel
            // instead of for the contextPath for the current request
            final HstModelRegistry hstModelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);
            final InternalHstModel liveHstModel = (InternalHstModel)hstModelRegistry.getHstModel(contextPath);
            if (liveHstModel == null) {
                throw new IllegalArgumentException(String.format("Cannot find an hst model for context path '%s'", contextPath));
            }
            requestContext.setAttribute(EDITING_HST_MODEL_LINK_CREATOR_ATTR, liveHstModel.getHstLinkCreator());

            final InternalHstModel liveHstModelSnapshot = new HstModelSnapshot(liveHstModel);
            final InternalHstModel previewHstModelSnapshot = new HstModelSnapshot(liveHstModelSnapshot, previewDecorator);

            requestContext.setAttribute(LIVE_EDITING_HST_MODEL_ATTR, liveHstModelSnapshot);
            requestContext.setAttribute(PREVIEW_EDITING_HST_MODEL_ATTR, previewHstModelSnapshot);


            // we need the HST configuration user jcr session since some CMS user sessions (for example authors) typically
            // don't have read access on the hst configuration nodes. So we cannot use the session from the request context here
            // session below will be pooled hst config reader session
            session = repository.login(credentials);
            Node node = session.getNodeByIdentifier(uuid);

            if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                resourceType = HippoNodeType.NT_DOCUMENT;
            } else {
                resourceType = node.getPrimaryNodeType().getName();
            }

            if (node.isNodeType(NODETYPE_HST_MOUNT)) {
                final HttpSession httpSession = requestContext.getServletRequest().getSession();
                CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(httpSession);
                cmsSessionContext.getContextPayload().put(CMS_REQUEST_RENDERING_MOUNT_ID, uuid);
            }

        } catch (ItemNotFoundException e) {
            log.info("Configuration node with uuid {} does not exist any more : {}", uuid, e);
            return setErrorMessageAndReturn(requestContext, request, e.toString());
        } catch (RepositoryException e) {
            log.warn("RepositoryException ", e);
            return setErrorMessageAndReturn(requestContext, request, e.toString());
        } finally {
            if (session != null) {
                // logout in this case means return to pool
                session.logout();
            }
        }

        requestContext.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, uuid);

        // use JAX-RS service endpoint url-template: /{resourceType}/{suffix}
        StringBuilder jaxrsEndpointRequestPath = new StringBuilder("/").append(resourceType).append("/");
        if (requestContext.getPathSuffix() != null) {
            jaxrsEndpointRequestPath.append(requestContext.getPathSuffix());
        }

        log.debug("Invoking JAX-RS endpoint {}: {} for uuid {}", new Object[]{request.getMethod(), jaxrsEndpointRequestPath.toString(), uuid});
        return new PathsAdjustedHttpServletRequestWrapper(request, getJaxrsServletPath(requestContext), jaxrsEndpointRequestPath.toString());
    }

    private HttpServletRequest setErrorMessageAndReturn(HstRequestContext requestContext, HttpServletRequest request, String message) throws ContainerException {
        request.setAttribute(REQUEST_ERROR_MESSAGE_ATTRIBUTE, message);
        String jaxrsEndpointRequestPath = "/hst:exception/";
        return new PathsAdjustedHttpServletRequestWrapper(request, getJaxrsServletPath(requestContext), jaxrsEndpointRequestPath);
    }

    @Override
    public void invoke(HstRequestContext requestContext, HttpServletRequest request, HttpServletResponse response) throws ContainerException {
        super.invoke(requestContext, request, response);
    }

    public static class HstModelSnapshot implements InternalHstModel {

        private final InternalHstModel delegatee;
        private PreviewDecorator previewDecorator;
        private VirtualHosts cache;

        public HstModelSnapshot(final InternalHstModel delegatee) {
            this.delegatee = delegatee;
        }
        public HstModelSnapshot(final InternalHstModel delegatee, final PreviewDecorator previewDecorator) {
            this.delegatee = delegatee;
            this.previewDecorator = previewDecorator;
        }

        @Override
        public VirtualHosts getVirtualHosts() {
            if (cache != null) {
                return cache;
            }
            if (previewDecorator == null) {
                cache = delegatee.getVirtualHosts();
            } else {
                cache = previewDecorator.decorateVirtualHostsAsPreview(delegatee.getVirtualHosts());
            }
            return cache;
        }

        @Override
        public HstSiteMapMatcher getHstSiteMapMatcher() {
            return delegatee.getHstSiteMapMatcher();
        }

        @Override
        public HstLinkCreator getHstLinkCreator() {
            return delegatee.getHstLinkCreator();
        }

        @Override
        public BiPredicate<Session, Channel> getChannelFilter() {
            return delegatee.getChannelFilter();
        }

        @Override
        public ChannelManager getChannelManager() {
            return delegatee.getChannelManager();
        }

        @Override
        public String getConfigurationRootPath() {
            return delegatee.getConfigurationRootPath();
        }

        @Override
        public EventPathsInvalidator getEventPathsInvalidator() {
            return delegatee.getEventPathsInvalidator();
        }

        @Override
        public boolean isHstConfigurationNodesLoaded() {
            return delegatee.isHstConfigurationNodesLoaded();
        }

        @Override
        public ComponentManager getComponentManager() {
            return delegatee.getComponentManager();
        }
    }
}
