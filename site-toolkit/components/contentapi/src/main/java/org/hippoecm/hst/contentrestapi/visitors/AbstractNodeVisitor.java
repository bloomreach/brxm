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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.hst.contentrestapi.ResourceContext;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeChild;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;

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
    public void visit(final ResourceContext context, final Node node, final Map<String, Object> response) throws RepositoryException {
        final ContentType parentContentType = context.getContentTypes().getContentTypeForNode(node.getParent());
        final ContentTypeChild nodeType = parentContentType.getChildren().get(node.getName());

        final Map<String, Object> nodeResponse = new LinkedHashMap<>();
        if (nodeType != null && nodeType.isMultiple() || nodeType == null && node.getDefinition().allowsSameNameSiblings()) {
            List<Object> siblings = (List<Object>) response.get(node.getName());
            if (siblings == null) {
                siblings = new ArrayList<>();
                response.put(node.getName(), siblings);
            }
            siblings.add(nodeResponse);
        } else {
            if (response.get(node.getName()) != null) {
                return;
            }
            response.put(node.getName(), nodeResponse);
        }

        visitNode(context, node, nodeResponse);
        visitNodeItems(context, node, nodeResponse);
    }

    protected void visitNode(final ResourceContext context, final Node node, final Map<String, Object> response)
            throws RepositoryException {
    }

    protected void visitNodeItems(final ResourceContext context, final Node node, final Map<String, Object> response)
            throws RepositoryException {
        final Map<String, Object> content = new LinkedHashMap<>();
        visitProperties(context, node, content);
        visitChildren(context, node, content);
        if (!content.isEmpty()) {
            response.put("items", content);
        }
    }

    protected void visitProperties(final ResourceContext context, final Node node, final Map<String, Object> response)
            throws RepositoryException {
        for (Property property : new PropertyIterable(node.getProperties())) {
            final ContentType parentContentType = context.getContentTypes().getContentTypeForNode(property.getParent());
            final ContentTypeProperty propertyType = parentContentType.getProperties().get(property.getName());

            // skip properties that either are unknown or for which a node with the same name is also defined
            if (propertyType == null || parentContentType.getChildren().get(property.getName()) != null ||
                    parentContentType.getEffectiveNodeType().getChildren().get(property.getName()) != null ||
                    node.hasNode(property.getName())) {
                return;
            }

            if (!skipProperty(context, propertyType, property)) {
                visitProperty(context, propertyType, property, response);
            }
        }
    }

    protected void visitProperty(final ResourceContext context, final ContentTypeProperty propertyType,
                                 final Property property, final Map<String, Object> response)
            throws RepositoryException {
        if ((propertyType != null && propertyType.isMultiple()) || propertyType == null && property.getDefinition().isMultiple()) {
            final Value[] jcrValues = property.getValues();
            final Object[] representations = new Object[jcrValues.length];
            for (int i = 0; i < jcrValues.length; i++) {
                final Value jcrValue = jcrValues[i];
                representations[i] = getValueRepresentation(jcrValue);
            }
            response.put(property.getName(), representations);
        } else {
            response.put(property.getName(), getValueRepresentation(property.getValue()));
        }
    }

    protected void visitChildren(final ResourceContext context, final Node node, final Map<String, Object> response)
            throws RepositoryException {
        for (Node child : new NodeIterable(node.getNodes())) {
            final ContentType nodeContentType = context.getContentTypes().getContentTypeForNode(node);
            final ContentTypeChild childType = nodeContentType.getChildren().get(child.getName());

            // skip nodes that are known for which a property with the same name is also defined
            if (childType != null && nodeContentType.getProperties().get(child.getName()) != null) {
                return;
            }

            if (!skipChild(context, childType, child)) {
                NodeVisitor childVisitor = getVisitorFactory().getVisitor(context, child);
                childVisitor.visit(context, child, response);
            }
        }
    }

    protected Object getValueRepresentation(final Value jcrValue) throws RepositoryException {
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
                // Date is rendered by JackRabbit including timezone, no need for special handling
                return jcrValue.getString();
        }
    }

    protected boolean skipProperty(final ResourceContext context, final ContentTypeProperty propertyType,
                                            final Property property) throws RepositoryException {
        return false;
    }

    protected boolean skipChild(final ResourceContext context, final ContentTypeChild childType,
                                         final Node child) throws RepositoryException {
        return false;
    }
}