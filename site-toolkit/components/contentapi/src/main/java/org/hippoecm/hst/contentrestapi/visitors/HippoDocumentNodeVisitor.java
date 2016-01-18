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

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.hst.contentrestapi.ResourceContext;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;

import static org.hippoecm.repository.api.HippoNodeType.NT_DOCUMENT;

public class HippoDocumentNodeVisitor extends AbstractNodeVisitor {

    public HippoDocumentNodeVisitor(final VisitorFactory visitorFactory) {
        super(visitorFactory);
    }


    @Override
    public void visit(final ResourceContext context, final Node node, final Map<String, Object> destination) throws RepositoryException {

        for (Property property : new PropertyIterable(node.getProperties())) {
            final ContentType parentContentType = context.getContentTypes().getContentTypeForNode(property.getParent());
            final ContentTypeProperty propertyType = parentContentType.getProperties().get(property.getName());

            // skip properties that either are unknown or for which a node with the same name is also defined
            if (propertyType == null || parentContentType.getChildren().get(property.getName()) != null) {
                return;
            }

            if (propertyType.isMultiple()) {
                final Value[] jcrValues = property.getValues();
                final Object[] representations = new Object[jcrValues.length];
                for (int i = 0; i < jcrValues.length; i++) {
                    final Value jcrValue = jcrValues[i];
                    representations[i] = getValueRepresentation(jcrValue);
                }
                destination.put(property.getName(), representations);
            } else {
                destination.put(property.getName(), getValueRepresentation(property.getValue()));
            }
        }

        // visit child nodes
        super.visit(context, node, destination);
    }

    @Override
    protected void visitChildren(final ResourceContext context, final Node node, final Map<String, Object> destination) throws RepositoryException {
        visit(context, new NodeIterable(node.getNodes()).iterator(), destination);
    }

    @Override
    public String getNodeType() {
        return NT_DOCUMENT;
    }
}
