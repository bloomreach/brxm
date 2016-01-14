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

import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.contentrestapi.ContentRestApiResource;
import org.hippoecm.hst.contentrestapi.ResourceContext;
import org.hippoecm.repository.api.HippoNodeType;

class HtmlNodeVisitor extends AbstractBaseNodeVisitor {

    public HtmlNodeVisitor(VisitorFactory visitorFactory) {
        super(visitorFactory);
    }

    @Override
    protected void visitDescendants(final ResourceContext context, final Node node, final Map<String, Object> destination) throws RepositoryException {
        final PropertyIterator propertyIterator = node.getProperties();
        while (propertyIterator.hasNext()) {
            final Property childProperty = (Property) propertyIterator.next();
            if (childProperty.getName().equals("hippostd:content")) {
                // TODO link rewriting - the href of the binary links needs to be altered
            }
            final Visitor visitor = getVisitorFactory().getVisitor(context, childProperty);
            visitor.visit(context, childProperty, destination);
        }

        final Map<String, Object> linksOutput = new TreeMap<>();
        destination.put(ContentRestApiResource.NAMESPACE_PREFIX + ":links", linksOutput);

        final NodeIterator nodeIterator = node.getNodes();
        while (nodeIterator.hasNext()) {
            final Node childNode = (Node) nodeIterator.next();
            final Visitor visitor = getVisitorFactory().getVisitor(context, childNode);
            switch (childNode.getPrimaryNodeType().getName()) {
                case HippoNodeType.NT_FACETSELECT:
                    visitor.visit(context, childNode, linksOutput);
                    break;
                default:
                    visitor.visit(context, childNode, destination);
                    break;
            }
        }
    }
}