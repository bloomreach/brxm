/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.standards.list.resolvers;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.repository.api.HippoNode;

public class PropertyRenderer extends AbstractNodeRenderer {
    private static final long serialVersionUID = 1L;

    private String property;

    public PropertyRenderer(String property) {
        this.property = property;
    }


    @Override
    protected Component getViewer(String id, HippoNode node) throws RepositoryException {
        String value = getValue(node);
        if (value == null) {
            return new Label(id, value);
        }
        return new Label(id);
    }

    private String getValue(HippoNode node) throws RepositoryException {
        Property p = node.getProperty(property);
        switch (p.getType()) {
        case PropertyType.BINARY:
            return "[binary data]";
        case PropertyType.BOOLEAN:
            return String.valueOf(p.getBoolean());
        case PropertyType.DATE:
            return String.valueOf(p.getDate());
        case PropertyType.DOUBLE:
            return String.valueOf(p.getDouble());
        case PropertyType.LONG:
            return String.valueOf(p.getLong());
        case PropertyType.REFERENCE:
            // do not show references
        case PropertyType.PATH:
            return String.valueOf(p.getPath());
        case PropertyType.STRING:
            return String.valueOf(p.getString());
        case PropertyType.NAME:
            return String.valueOf(p.getName());
        default:
            throw new IllegalArgumentException("illegal internal value type");
        }
    }

}
