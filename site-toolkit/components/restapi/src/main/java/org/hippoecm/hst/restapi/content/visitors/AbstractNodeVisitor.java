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

package org.hippoecm.hst.restapi.content.visitors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.hst.restapi.NodeVisitor;
import org.hippoecm.hst.restapi.ResourceContext;
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

    /**
     * Visit all supported properties of the current node
     * <p>
     *     Properties are only visited when they are:
     *     <ul>
     *       <li>defined in a (inherited) document type (not-derived) with an explicit path (not-residual)</li>
     *       <li>are not marked to be skipped by this (or inherited) visitor</li>
     *     </ul>
     * </p>
     * <p>
     *     Note that no same-named child will be visited through {@link #visitChildren(ResourceContext, Node, Map)}
     *     as the content type service will ignore/hide properties for which such same-named child is defined.
     * </p>
     * @param context ResourceContent
     * @param node the current node
     * @param response the response
     * @throws RepositoryException
     */
    protected void visitProperties(final ResourceContext context, final Node node, final Map<String, Object> response)
            throws RepositoryException {
        for (Property property : new PropertyIterable(node.getProperties())) {
            final ContentType parentContentType = context.getContentTypes().getContentTypeForNode(property.getParent());
            final ContentTypeProperty propertyType = parentContentType.getProperties().get(property.getName());

            if (propertyType != null                                  // explicit and non-residual property type
                    && !propertyType.isDerivedItem()                  // defined in a (inherited) document type
                    && !skipProperty(context, propertyType, property) // not marked to be skipped
            ) {
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

    /**
     * Visit all supported child nodes of the current node
     * <p>
     *     Child nodes are only visited when they are:
     *     <ul>
     *       <li>defined in a (inherited) document type (not-derived) with an explicit path (not-residual) or</li>
     *       <li>or for the derived document type an explicit matching PrimaryNodeTypeNodeVisitor is available,</li>
     *       <li>and are not marked to be skipped by this (or inherited) visitor</li>
     *     </ul>
     * </p>
     * <p>
     *     Note that no same-named property will be visited through {@link #visitProperties(ResourceContext, Node, Map)}
     *     as the content type service will ignore/hide properties for which such same-named child is defined.
     * </p>
     * @param context ResourceContent
     * @param node the current node
     * @param response the response
     * @throws RepositoryException
     */
    protected void visitChildren(final ResourceContext context, final Node node, final Map<String, Object> response)
            throws RepositoryException {
        for (Node child : new NodeIterable(node.getNodes())) {
            final ContentType nodeContentType = context.getContentTypes().getContentTypeForNode(node);
            final ContentTypeChild childType = nodeContentType.getChildren().get(child.getName());

            // test explicit and non-residual child type
            if (childType == null) {
                continue;
            }
            // test defined in a (inherited) document type or has primary visitor
            if (childType.isDerivedItem() && context.getPrimaryNodeTypeVisitor(child) == null) {
                continue;
            }
            // test whether marked to be skipped
            if (skipChild(context, childType, child)) {
                continue;
            }

            NodeVisitor childVisitor = context.getVisitor(child);
            childVisitor.visit(context, child, response);
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