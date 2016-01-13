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

package org.hippoecm.hst.jaxrs.contentrestapi.visitors;

import java.util.Map;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.hst.jaxrs.contentrestapi.ResourceContext;

import static javax.jcr.PropertyType.BINARY;

class DefaultPropertyVisitor extends AbstractBaseVisitor {

    public DefaultPropertyVisitor(VisitorFactory visitorfactory) {
        super(visitorfactory);
    }

    @Override
    public void visit(final ResourceContext context, final Property property, final Map<String, Object> destination) throws RepositoryException {
        if (property.isMultiple()) {
            final Value[] jcrValues = property.getValues();
            final String[] stringValues = new String[jcrValues.length];
            for (int i = 0; i < jcrValues.length; i++) {
                final Value jcrValue = jcrValues[i];
                stringValues[i] = getStringRepresentation(jcrValue);
            }
            destination.put(property.getName(), stringValues);
        } else {
            destination.put(property.getName(), getStringRepresentation(property.getValue()));
        }
    }

    private String getStringRepresentation(final Value jcrValue) throws RepositoryException {
        if (jcrValue.getType() == BINARY) {
            return jcrValue.getBinary().getSize() + " bytes.";
        } else {
            return jcrValue.getString();
        }
    }
}