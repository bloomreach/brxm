/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
 */
package org.hippoecm.hst.restapi;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.restapi.content.linking.RestApiLinkCreator;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.cms7.services.contenttype.ContentTypes;

public class ResourceContextFactory {

    private RestApiLinkCreator restApiLinkCreator;
    private List<NodeVisitor> explicitNodeVisitors;
    private List<NodeVisitor> fallbackNodeVisitors = Collections.EMPTY_LIST;


    public void setRestApiLinkCreator(final RestApiLinkCreator restApiLinkCreator) {
        this.restApiLinkCreator = restApiLinkCreator;
    }

    public void setFallbackNodeVisitors(final List<NodeVisitor> fallbackNodeVisitors) {
        this.fallbackNodeVisitors = fallbackNodeVisitors;
    }

    public void setAnnotationBasedNodeVisitors(List<NodeVisitor> explicitNodeVisitors) {
        this.explicitNodeVisitors = explicitNodeVisitors;
    }


    public ResourceContext createResourceContext()  throws RepositoryException {
        return new ResourceContextImpl(restApiLinkCreator, explicitNodeVisitors, fallbackNodeVisitors);
    }

    private static class ResourceContextImpl implements ResourceContext {

        private final ContentTypes contentTypes;
        private RestApiLinkCreator restApiLinkCreator;
        private final List<NodeVisitor> explicitNodeVisitors;
        private final List<NodeVisitor> fallbackNodeVisitors;

        public ResourceContextImpl(final RestApiLinkCreator restApiLinkCreator,
                                   final List<NodeVisitor> explicitNodeVisitors,
                                   final List<NodeVisitor> fallbackNodeVisitors) throws RepositoryException {
            this.restApiLinkCreator = restApiLinkCreator;
            this.explicitNodeVisitors = explicitNodeVisitors;
            this.fallbackNodeVisitors = fallbackNodeVisitors;
            contentTypes = HippoServiceRegistry.getService(ContentTypeService.class).getContentTypes();

        }

        @Override
        public HstRequestContext getRequestContext() {
            return RequestContextProvider.get();
        }

        @Override
        public ContentTypes getContentTypes() {
            return contentTypes;
        }

        @Override
        public RestApiLinkCreator getRestApiLinkCreator() {
            return restApiLinkCreator;
        }

        public NodeVisitor getVisitor(final Node node) throws RepositoryException {

            NodeVisitor primary = getVisitorPrimaryVisitor(node);
            if (primary != null) {
                return primary;
            }

            // if not explicit match, try a fallback match
            for (NodeVisitor nodeVisitor : fallbackNodeVisitors) {
                if (node.isNodeType(nodeVisitor.getNodeType())) {
                    return nodeVisitor;
                }
            }

            // apparently the DefaultNodeVisitor is removed from fallbackNodeVisitors otherwise this can never happen. Return a noop visitor
            return new NodeVisitor() {

                @Override
                public String getNodeType() {
                    return null;
                }

                @Override
                public void visit(final ResourceContext context, final Node node, final Map<String, Object> response) throws RepositoryException {

                }
            };
        }

        @Override
        public NodeVisitor getVisitorPrimaryVisitor(final Node node) throws RepositoryException {
            if (explicitNodeVisitors != null) {
                for (NodeVisitor nodeVisitor : explicitNodeVisitors) {
                    if (node.getPrimaryNodeType().getName().equals(nodeVisitor.getNodeType())) {
                        return nodeVisitor;
                    }

                }
            }
            return null;
        }
    }

}