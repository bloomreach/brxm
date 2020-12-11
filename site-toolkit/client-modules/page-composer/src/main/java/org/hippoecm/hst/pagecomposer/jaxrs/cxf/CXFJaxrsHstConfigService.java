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
package org.hippoecm.hst.pagecomposer.jaxrs.cxf;

import java.util.Map;
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

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.internal.PreviewDecorator;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.jaxrs.cxf.CXFJaxrsService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.platform.utils.UUIDUtils;
import org.hippoecm.hst.platform.api.model.EventPathsInvalidator;
import org.hippoecm.hst.platform.api.model.InternalHstModel;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jackrabbit.JcrConstants.JCR_FROZENUUID;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_MOUNT;
import static org.hippoecm.hst.core.container.ContainerConstants.CMS_REQUEST_RENDERING_MOUNT_ID;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService.EDITING_HST_MODEL_LINK_CREATOR_ATTR;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService.LIVE_EDITING_HST_MODEL_ATTR;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService.PREVIEW_EDITING_HST_MODEL_ATTR;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils.getXPageUnpublishedVariant;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATE;
import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;
import static org.onehippo.repository.util.JcrConstants.JCR_FROZEN_PRIMARY_TYPE;
import static org.onehippo.repository.util.JcrConstants.NT_FROZEN_NODE;

public class CXFJaxrsHstConfigService extends CXFJaxrsService {

    private static Logger log = LoggerFactory.getLogger(CXFJaxrsHstConfigService.class);

    public static final String REQUEST_CONFIG_NODE_IDENTIFIER = "org.hippoecm.hst.pagecomposer.jaxrs.cxf.contentNode.identifier";
    public static final String REQUEST_ERROR_MESSAGE_ATTRIBUTE = "org.hippoecm.hst.pagecomposer.jaxrs.cxf.exception.message";
    public static final String REQUEST_CLIENT_EXCEPTION_ATTRIBUTE = "org.hippoecm.hst.pagecomposer.jaxrs.cxf.client.exception";
    public static final String REQUEST_IS_EXPERIENCE_PAGE_ATRRIBUTE = "org.hippoecm.hst.pagecomposer.jaxrs.cxf.isExpPage";
    public static final String REQUEST_EXPERIENCE_PAGE_UNPUBLISHED_UUID_VARIANT_ATRRIBUTE = "org.hippoecm.hst.pagecomposer.jaxrs.cxf.expPageHandleUUID";
    public static final String REQUEST_EXPERIENCE_PAGE_HANDLE_UUID_ATRRIBUTE = "org.hippoecm.hst.pagecomposer.jaxrs.cxf.expPageUnpulbishedUUID";


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

        final String uuid = PathUtils.normalizePath(requestContext.getBaseURL().getPathInfo());
        if (!UUIDUtils.isValidUUID(uuid)) {
            throw new ContainerException("CXFJaxrsHstConfigService expects a 'uuid' as pathInfo but was '" + uuid + "'. Cannot process REST call");
        }

        final String contextPath = requestContext.getServletRequest().getHeader("contextPath");
        if (contextPath == null) {
            throw new IllegalArgumentException("'contextPath' header is missing");
        }

        // note you CANNOT use HstModelProvider spring bean since that will always give you the platform HstModel
        // instead of for the contextPath for the current request
        final HstModelRegistry hstModelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);
        final InternalHstModel liveHstModel = (InternalHstModel) hstModelRegistry.getHstModel(contextPath);
        if (liveHstModel == null) {
            throw new IllegalArgumentException(String.format("Cannot find an hst model for context path '%s'", contextPath));
        }

        Session session = null;
        try {
            // we need the HST configuration user jcr session since some CMS user sessions (for example authors) typically
            // don't have read access on the hst configuration nodes. So we cannot use the session from the request context here
            // session below will be pooled hst config reader session
            session = repository.login(credentials);
            final Node node = session.getNodeByIdentifier(uuid);

            final String method = request.getMethod();
            final String adjustedPath = adjustEndpointPath(node, requestContext);
            setRequestContextAttributes(node, requestContext, liveHstModel);

            log.debug("Invoking JAX-RS endpoint {}: {} for uuid {}", method, adjustedPath, uuid);
            return new PathsAdjustedHttpServletRequestWrapper(request, getJaxrsServletPath(requestContext), adjustedPath);

        } catch (ClientException e) {
            log.info("Client Exception happened, delegate to exception resource : {}", uuid, e);
            request.setAttribute(REQUEST_CLIENT_EXCEPTION_ATTRIBUTE, e);
            return new PathsAdjustedHttpServletRequestWrapper(request, getJaxrsServletPath(requestContext), "/hst:exception/clientexception/");
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
    }

    private void setRequestContextAttributes(Node node, HstRequestContext requestContext, InternalHstModel liveHstModel) throws RepositoryException {
        requestContext.setAttribute(EDITING_HST_MODEL_LINK_CREATOR_ATTR, liveHstModel.getHstLinkCreator());

        final InternalHstModel liveHstModelSnapshot = new HstModelSnapshot(liveHstModel);
        final InternalHstModel previewHstModelSnapshot = new HstModelSnapshot(liveHstModelSnapshot, previewDecorator);

        requestContext.setAttribute(LIVE_EDITING_HST_MODEL_ATTR, liveHstModelSnapshot);
        requestContext.setAttribute(PREVIEW_EDITING_HST_MODEL_ATTR, previewHstModelSnapshot);

        final String uuid = node.getIdentifier();
        if (node.isNodeType(NODETYPE_HST_MOUNT)) {
            final HttpSession httpSession = requestContext.getServletRequest().getSession();
            CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(httpSession);
            cmsSessionContext.getContextPayload().put(CMS_REQUEST_RENDERING_MOUNT_ID, uuid);
        }
        requestContext.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, uuid);
    }

    private String adjustEndpointPath(Node node, HstRequestContext requestContext) throws RepositoryException {
        final StringBuilder builder = new StringBuilder();
        Node xPageUnpublishedVariant = getXPageUnpublishedVariant(node);
        final String nodeType = getNodeType(node);
        if (xPageUnpublishedVariant != null) {
            requestContext.setAttribute(REQUEST_IS_EXPERIENCE_PAGE_ATRRIBUTE, Boolean.TRUE);
            // never store the Node since backed by an hst config user which can be returned to the pool after this work
            requestContext.setAttribute(REQUEST_EXPERIENCE_PAGE_UNPUBLISHED_UUID_VARIANT_ATRRIBUTE, xPageUnpublishedVariant.getIdentifier());
            requestContext.setAttribute(REQUEST_EXPERIENCE_PAGE_HANDLE_UUID_ATRRIBUTE, xPageUnpublishedVariant.getParent().getIdentifier());
            builder.append("/experiencepage/");
        } else {
            builder.append("/");
        }
        builder.append(nodeType);
        builder.append("/");
        final String optionalPathSuffix = requestContext.getPathSuffix();
        if (optionalPathSuffix != null) {
            builder.append(optionalPathSuffix);
        }
        return builder.toString();
    }

    private String getNodeType(final Node node) throws RepositoryException {
        if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            return HippoNodeType.NT_DOCUMENT;
        }
        if (node.isNodeType(NT_FROZEN_NODE)) {
            // map to the nodetype of the 'non-frozen' version
            return node.getProperty(JCR_FROZEN_PRIMARY_TYPE).getString();
        }
        return node.getPrimaryNodeType().getName();
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
        public void invalidate() {
            delegatee.invalidate();
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
