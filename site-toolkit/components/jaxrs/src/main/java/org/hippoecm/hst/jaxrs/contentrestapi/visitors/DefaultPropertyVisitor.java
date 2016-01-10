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

import javax.jcr.Item;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

class DefaultPropertyVisitor extends Visitor {

    public DefaultPropertyVisitor(VisitorFactory factory) {
        super(factory);
    }

    public void visit(final Item sourceItem, final Map<String, Object> destination) throws RepositoryException {
        final Property sourceProperty = (Property) sourceItem;

        if (sourceProperty.isMultiple()) {
            Value[] jcrValues = sourceProperty.getValues();
            String[] stringValues = new String[jcrValues.length];
            for (int i = 0; i < jcrValues.length; i++) {
                stringValues[i] = jcrValues[i].getString();
            }
            destination.put(sourceProperty.getName(), stringValues);
        } else {
            destination.put(sourceProperty.getName(), sourceProperty.getValue().getString());
        }
    }

}