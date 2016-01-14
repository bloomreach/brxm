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

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.hst.contentrestapi.ResourceContext;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;

import static javax.jcr.PropertyType.BINARY;
import static javax.jcr.PropertyType.BOOLEAN;
import static javax.jcr.PropertyType.DECIMAL;
import static javax.jcr.PropertyType.DOUBLE;
import static javax.jcr.PropertyType.LONG;

class DefaultPropertyVisitor extends AbstractBaseVisitor {

    public DefaultPropertyVisitor(VisitorFactory visitorfactory) {
        super(visitorfactory);
    }

    @Override
    public void visit(final ResourceContext context, final Property property, final Map<String, Object> destination) throws RepositoryException {
        final ContentType contentType = context.getContentTypes().getContentTypeForNode(property.getParent());
        final ContentTypeProperty propertyType = contentType.getProperties().get(property.getName());

        if (propertyType == null) {
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

    private Object getValueRepresentation(final Value jcrValue) throws RepositoryException {
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