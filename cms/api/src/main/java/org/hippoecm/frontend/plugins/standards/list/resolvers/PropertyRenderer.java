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

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;

public class PropertyRenderer extends AbstractNodeRenderer {

    private static final long serialVersionUID = 1L;

    private String property;

    public PropertyRenderer(String property) {
        this.property = property;
    }

    @Override
    protected Component getViewer(String id, Node node) throws RepositoryException {
        String value = getValue(node);
        return new Label(id, value);
    }

    private String getValue(Node node) throws RepositoryException {
        Property p = node.getProperty(property);
        switch (p.getType()) {
        case 1: //javax.jcr.PropertyType.STRING:
            return String.valueOf(p.getString());
        case 2: //javax.jcr.PropertyType.BINARY:
            return "[binary data]";
        case 3: //javax.jcr.PropertyType.LONG:
            return String.valueOf(p.getLong());
        case 4: //javax.jcr.PropertyType.DOUBLE:
            return String.valueOf(p.getDouble());
        case 5: //javax.jcr.PropertyType.DATE:
            return String.valueOf(p.getDate());
        case 6: //javax.jcr.PropertyType.BOOLEAN:
            return String.valueOf(p.getBoolean());
        case 7: //javax.jcr.PropertyType.NAME:
            return String.valueOf(p.getName());
        case 8: //javax.jcr.PropertyType.PATH:
            return String.valueOf(p.getPath());
        case 9: //javax.jcr.PropertyType.REFERENCE:
            return "[reference]";
        default:
            throw new IllegalArgumentException("illegal internal value type");
        }
    }

}
