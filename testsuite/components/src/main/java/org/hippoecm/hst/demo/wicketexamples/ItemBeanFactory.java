/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.wicketexamples;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

public class ItemBeanFactory {

    public static ItemBean createItemBean(Item item) throws Exception {
        ItemBean itemBean = null;

        if (item.isNode()) {
            Node node = (Node) item;
            Map<String, Object[]> properties = getProperties(node);
            
            itemBean = new NodeBean(node.getName(), node.getPath(), node.getDepth(), node.isModified(), node.isNew(), node.isNode(), 
                    node.getPrimaryNodeType().getName(),
                    (node.isNodeType("mix:referenceable") ? node.getUUID() : null),
                    properties);
        } else {
            itemBean = new ItemBean(item.getName(), item.getPath(), item.getDepth(), item.isModified(), item.isNew(),
                    item.isNode());
        }

        return itemBean;
    }

    public static Map<String, Object[]> getProperties(Node node) throws Exception {
        Map<String, Object[]> properties = new HashMap<String, Object[]>();
        
        for (PropertyIterator it = node.getProperties(); it.hasNext(); ) {
            Property p = it.nextProperty();
            String name = p.getName();
            properties.put(name, getPropertyValues(p, name));
        }

        return properties;
    }

    public static Object[] getPropertyValues(Property p, String name) throws Exception {
        Object[] propValues = null;
        
        PropertyDefinition def = p.getDefinition();

        switch (p.getType()) {
        case PropertyType.BOOLEAN:
            if (def.isMultiple()) {
                Value[] values = p.getValues();
                propValues = new Boolean[values.length];

                for (int i = 0; i < values.length; i++) {
                    propValues[i] = (values[i].getBoolean() ? Boolean.TRUE : Boolean.FALSE);
                }
            } else {
                propValues = new Boolean[] { p.getBoolean() ? Boolean.TRUE : Boolean.FALSE };
            }
            break;
        case PropertyType.STRING:
            if (def.isMultiple()) {
                Value[] values = p.getValues();
                propValues = new String[values.length];

                for (int i = 0; i < propValues.length; i++) {
                    propValues[i] = values[i].getString();
                }
            } else {
                propValues = new String[] { p.getString() };
            }
            break;
        case PropertyType.LONG:
            if (def.isMultiple()) {
                Value[] values = p.getValues();
                propValues = new Long[values.length];

                for (int i = 0; i < values.length; i++) {
                    propValues[i] = new Long(values[i].getLong());
                }
            } else {
                propValues = new Long[] { new Long(p.getLong()) };
            }
            break;
        case PropertyType.DOUBLE:
            if (def.isMultiple()) {
                Value[] values = p.getValues();
                propValues = new Double[values.length];

                for (int i = 0; i < values.length; i++) {
                    propValues[i] = new Double(values[i].getDouble());
                }
            } else {
                propValues = new Double[] { new Double(p.getDouble()) };
            }
            break;
        case PropertyType.DATE:
            if (def.isMultiple()) {
                Value[] values = p.getValues();
                propValues = new Calendar[values.length];

                for (int i = 0; i < values.length; i++) {
                    propValues[i] = values[i].getDate();
                }
            } else {
                propValues = new Calendar[] { p.getDate() };
            }
            break;
        }

        return propValues;
    }

}
