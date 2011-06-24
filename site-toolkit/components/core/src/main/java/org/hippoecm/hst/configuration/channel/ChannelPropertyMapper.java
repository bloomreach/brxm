/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.configuration.channel;

import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

public class ChannelPropertyMapper {

    private ChannelPropertyMapper() {
    }

    public static Map<String, Object> getProperties(Node mountNode, List<HstPropertyDefinition> propertyDefinitions) throws RepositoryException {
        Map<String, Object> properties = new HashMap<String, Object>();
        if (propertyDefinitions != null) {
            for (HstPropertyDefinition pd : propertyDefinitions) {
                Property property = mountNode.getProperty(pd.getName());
                if (pd.isMultiValued() != property.isMultiple()) {
                    throw new RepositoryException();
                }
                Object value = getHstValueFromJcr(pd, property);
                properties.put(pd.getName(), value);
            }
        } else {
            for (PropertyIterator propertyIterator = mountNode.getProperties(); propertyIterator.hasNext(); ) {
                Property prop = propertyIterator.nextProperty();
                if (prop.getDefinition().isProtected()) {
                    continue;
                }
                HstPropertyDefinition hpd = new HstPropertyDefinitionService(prop, false);
                properties.put(prop.getName(), getHstValueFromJcr(hpd, prop));
            }
        }
        return properties;
    }

    public static void saveProperties(Node mountNode, Map<String, Object> properties) throws RepositoryException {
        for (PropertyIterator propertyIterator = mountNode.getProperties(); propertyIterator.hasNext(); ) {
            Property prop = propertyIterator.nextProperty();
            if (prop.getDefinition().isProtected()) {
                continue;
            }
            prop.remove();
        }
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            setHstValueToJcr(mountNode, entry.getKey(), entry.getValue());
        }
    }

    private static Object getHstValueFromJcr(final HstPropertyDefinition pd, final Property property) throws RepositoryException {
        Object value;
        if (property.isMultiple()) {
            List values = (List) (value = new LinkedList());
            for (Value jcrValue : property.getValues()) {
                values.add(jcrToJava(jcrValue, pd.getType()));
            }
        } else {
            value = jcrToJava(property.getValue(), pd.getType());
        }
        return value;
    }

    public static Object jcrToJava(final Value value, final HstPropertyType type) throws RepositoryException {
        if (type == null) {
            switch (value.getType()) {
                case PropertyType.STRING:
                    return value.getString();
                case PropertyType.BOOLEAN:
                    return value.getBoolean();
                case PropertyType.DATE:
                    return value.getDate();
                case PropertyType.LONG:
                    return value.getLong();
                case PropertyType.DOUBLE:
                    return value.getDouble();
            }
            return null;
        }
        switch (type) {
            case STRING:
                return value.getString();
            case BOOLEAN:
                return value.getBoolean();
            case DATE:
                return value.getDate();
            case DOUBLE:
                return value.getDouble();
            case INTEGER:
                return value.getLong();
            default:
                return null;
        }
    }

    private static void setHstValueToJcr(Node node, String name, Object value) throws RepositoryException {
        ValueFactory vf = node.getSession().getValueFactory();
        if (value instanceof List) {
            Value[] values = new Value[((List) value).size()];
            int i = 0;
            for (Object val : (List) value) {
                values[i++] = javaToJcr(vf, val);
            }
            node.setProperty(name, values);
        } else {
            node.setProperty(name, javaToJcr(vf, value));
        }
    }

    private static Value javaToJcr(ValueFactory vf, Object value) throws RepositoryException {
        if (value instanceof String) {
            return vf.createValue((String) value);
        } else if (value instanceof Boolean) {
            return vf.createValue((Boolean) value);
        } else if (value instanceof Integer) {
            return vf.createValue((Long) value);
        } else if (value instanceof Double) {
            return vf.createValue((Double) value);
        } else if (value instanceof Calendar) {
            return vf.createValue((Calendar) value);
        } else {
            throw new RepositoryException("Unable to find valid value type for " + value);
        }
    }
}
