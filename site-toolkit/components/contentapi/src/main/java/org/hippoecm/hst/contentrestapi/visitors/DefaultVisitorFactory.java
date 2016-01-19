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

package org.hippoecm.hst.contentrestapi.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.hst.contentrestapi.ResourceContext;

public class DefaultVisitorFactory implements VisitorFactory {

    private List<NodeVisitor> explicitNodeVisitors = new ArrayList<>();
    private List<NodeVisitor> fallbackNodeVisitors = Collections.EMPTY_LIST;

    public DefaultVisitorFactory() {
        // TODO replace by annotation scanning
        final ResourceBundleVisitor resourceBundleVisitor = new ResourceBundleVisitor(this);
        explicitNodeVisitors.add(resourceBundleVisitor);
    }

    public NodeVisitor getVisitor(final ResourceContext context, final Node node) throws RepositoryException {

        final NodeType nodeType = node.getPrimaryNodeType();
        for (NodeVisitor nodeVisitor : explicitNodeVisitors) {
            if (node.getPrimaryNodeType().getName().equals(nodeVisitor.getNodeType())) {
                return nodeVisitor;
            }
        }

        // if not explicit match, try a fallback match
        for (NodeVisitor nodeVisitor : fallbackNodeVisitors) {
            if (node.isNodeType(nodeVisitor.getNodeType())) {
                return nodeVisitor;
            }
        }

        // apparently the DefaultNodeVisitor is removed from fallbackNodeVisitors otherwise this can never happen
        return new NoopVisitor(this);
    }

    public void setFallbackNodeVisitors(final List<NodeVisitor> fallbackNodeVisitors) {
        this.fallbackNodeVisitors = fallbackNodeVisitors;
    }
}