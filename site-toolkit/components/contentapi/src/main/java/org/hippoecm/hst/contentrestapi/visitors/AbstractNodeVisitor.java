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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.hst.contentrestapi.ResourceContext;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeChild;

import static javax.jcr.PropertyType.BINARY;
import static javax.jcr.PropertyType.BOOLEAN;
import static javax.jcr.PropertyType.DECIMAL;
import static javax.jcr.PropertyType.DOUBLE;
import static javax.jcr.PropertyType.LONG;

public abstract class AbstractNodeVisitor implements NodeVisitor {


    private final VisitorFactory visitorFactory;

    protected AbstractNodeVisitor(final VisitorFactory visitorFactory) {
        this.visitorFactory = visitorFactory;
    }

    @Override
    public VisitorFactory getVisitorFactory() {
        return visitorFactory;
    }

    @Override
    public void visit(final ResourceContext context, final Iterator<Node> iterator, final Map<String, Object> destination) throws RepositoryException {
        while (iterator.hasNext()) {
            final Node node = iterator.next();
            final NodeVisitor visitor = getVisitorFactory().getVisitor(context, node);
            visitor.visit(context, node, destination);
        }
    }

    @Override
    public void visit(final ResourceContext context, final Node node, final Map<String, Object> destination) throws RepositoryException {
        final ContentType parentContentType = context.getContentTypes().getContentTypeForNode(node.getParent());
        final ContentTypeChild nodeType = parentContentType.getChildren().get(node.getName());

        // skip nodes that either are unknown or for which a property with the same name is also defined
        if (nodeType == null || parentContentType.getProperties().get(node.getName()) != null) {
            return;
        }

        final Map<String, Object> descendantsOutput = new TreeMap<>();
        if (nodeType.isMultiple()) {
            List<Object> siblings = (List<Object>)destination.get(node.getName());
            if (siblings == null) {
                siblings = new ArrayList<>();
                destination.put(node.getName(), siblings);
            }
            siblings.add(descendantsOutput);
        } else {
            if (destination.get(node.getName()) != null) {
                return;
            }
            destination.put(node.getName(), descendantsOutput);
        }

        visitChildren(context, node, descendantsOutput);
    }

    protected abstract void visitChildren(final ResourceContext context, final Node node, final Map<String, Object> destination) throws RepositoryException;

    public Object getValueRepresentation(final Value jcrValue) throws RepositoryException {
        // Date is rendered by JackRabbit including timezone, no need for special handling

        switch (jcrValue.getType()) {
            case BINARY:
                return "Retrieving the content of binary property values is not yet supported in the Content REST API. Use images and assets instead.";
            case LONG:
                return jcrValue.getLong();
            case DOUBLE:
                return jcrValue.getDouble();
            case BOOLEAN:
                return jcrValue.getBoolean();
            case DECIMAL:
                return jcrValue.getDecimal();
            default:
                return jcrValue.getString();
        }
    }

}