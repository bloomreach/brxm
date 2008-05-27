/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.sa.plugin.config.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.StringValueConversionException;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.time.Time;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrPluginConfig extends NodeModelWrapper implements IPluginConfig {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrPluginConfig.class);

    public JcrPluginConfig(JcrNodeModel nodeModel) {
        super(nodeModel);
    }

    public void clear() {
        try {
            Node node = getNodeModel().getNode();
            if (node != null) {
                NodeIterator children = node.getNodes();
                while (children.hasNext()) {
                    Node child = children.nextNode();
                    if (!child.getDefinition().isProtected()) {
                        child.remove();
                    }
                }

                PropertyIterator properties = node.getProperties();
                while (properties.hasNext()) {
                    Property property = properties.nextProperty();
                    if (!property.getDefinition().isProtected()) {
                        property.remove();
                    }
                }
            } else {
                log.warn("Node model is not valid");
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    public boolean getBoolean(String key) throws StringValueConversionException {
        try {
            Property property = getProperty(key);
            if (property != null) {
                return property.getBoolean();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return false;
    }

    public double getDouble(String key) throws StringValueConversionException {
        return getDouble(key, 0.0);
    }

    public double getDouble(String key, double defaultValue) throws StringValueConversionException {
        try {
            Property property = getProperty(key);
            if (property != null) {
                return property.getDouble();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return defaultValue;
    }

    public int getInt(String key) throws StringValueConversionException {
        return getInt(key, 0);
    }

    public int getInt(String key, int defaultValue) throws StringValueConversionException {
        try {
            Property property = getProperty(key);
            if (property != null) {
                return (int) property.getLong();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return defaultValue;
    }

    public String getKey(String key) {
        try {
            Property property = getProperty(key);
            if (property != null) {
                if (!property.getDefinition().isMultiple()) {
                    return property.getString();
                } else {
                    log.warn("Property is multiple");
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    public String getString(String key) {
        return getKey(key);
    }

    public String getString(String key, String defaultValue) {
        String result = getKey(key);
        if (result == null) {
            return defaultValue;
        }
        return result;
    }

    public String[] getStringArray(String key) {
        try {
            Property property = getProperty(key);
            if (property != null) {
                if (property.getDefinition().isMultiple()) {
                    Value[] values = property.getValues();
                    String[] result = new String[values.length];
                    int i = 0;
                    for (Value value : values) {
                        result[i++] = value.getString();
                    }
                    return result;
                } else {
                    log.warn("Property is not multiple");
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    public StringValue getStringValue(String key) {
        return StringValue.valueOf(getString(key));
    }

    public Object put(Object key, Object value) {
        Object result = get(key);
        if (key instanceof String) {
            String strKey = (String) key;
            try {
                Node node = getNodeModel().getNode();
                if (value instanceof IPluginConfig[]) {
                    JcrPluginConfig[] current = (JcrPluginConfig[]) result;
                    HashMap<String, Node> paths = new HashMap<String, Node>();
                    for (JcrPluginConfig config : current) {
                        paths.put(config.getNodeModel().getItemModel().getPath(), config.getNodeModel().getNode());
                    }
                    for (IPluginConfig config : (IPluginConfig[]) value) {
                        if (config instanceof JcrPluginConfig) {
                            paths.remove(((JcrPluginConfig) config).getNodeModel().getItemModel().getPath());
                        }
                    }
                    for (Map.Entry<String, Node> entry : paths.entrySet()) {
                        entry.getValue().remove();
                    }
                    for (IPluginConfig config : (IPluginConfig[]) value) {
                        if (!(config instanceof JcrPluginConfig)) {
                            Node child = node.addNode(strKey);
                            JcrPluginConfig model = new JcrPluginConfig(new JcrNodeModel(child));
                            for (Map.Entry<String, Object> entry : (Set<Map.Entry<String, Object>>) config.entrySet()) {
                                model.put(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                    for (JcrPluginConfig config : (JcrPluginConfig[]) get(key)) {
                        config.detach();
                    }
                } else {
                    if (value instanceof Boolean) {
                        node.setProperty(strKey, (Boolean) value);
                    } else if (value instanceof String) {
                        node.setProperty(strKey, (String) value);
                    } else if (value instanceof String[]) {
                        node.setProperty(strKey, (String[]) value);
                    } else if (value instanceof Double) {
                        node.setProperty(strKey, (Double) value);
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        } else {
            log.warn("Key " + key + " is not a String");
        }
        return result;
    }

    public void putAll(Map map) {
        Iterator<Map.Entry> iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = iter.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    public Object remove(Object key) {
        if (key instanceof String) {
            String strKey = (String) key;
            try {
                Object result = get(key);
                Node node = getNodeModel().getNode();
                if (node != null) {
                    if (node.hasProperty(strKey)) {
                        node.getProperty(strKey).remove();
                    } else if (node.getNodes(strKey).getSize() > 0) {
                        NodeIterator nodes = node.getNodes(strKey);
                        while (nodes.hasNext()) {
                            Node child = nodes.nextNode();
                            child.remove();
                        }
                    }
                } else {
                    log.warn("Node model is invalid");
                }
                return result;
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        } else {
            log.error("Key " + key + " is not a String");
        }
        return null;
    }

    public boolean containsKey(Object key) {
        if (key instanceof String) {
            String strKey = (String) key;
            try {
                Node node = getNodeModel().getNode();
                if (node != null) {
                    if (node.hasProperty(strKey)) {
                        return true;
                    } else if (node.getNodes(strKey).getSize() > 0) {
                        return true;
                    }
                } else {
                    log.error("Node model is invalid");
                }
                return false;
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        } else {
            log.error("Key " + key + " is not a String");
        }
        return false;
    }

    public boolean containsValue(Object value) {
        for (Map.Entry entry : (Set<Map.Entry>) entrySet()) {
            if (entry.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }

    public Set entrySet() {
        HashSet<Map.Entry<String, Object>> entries = new HashSet<Map.Entry<String, Object>>();
        for (final String key : (Set<String>) keySet()) {
            entries.add(new Map.Entry<String, Object>() {

                public String getKey() {
                    return key;
                }

                public Object getValue() {
                    return get(key);
                }

                public Object setValue(Object value) {
                    return put(key, value);
                }
            });
        }
        return entries;
    }

    public Object get(Object key) {
        if (key instanceof String) {
            String strKey = (String) key;
            try {
                Node node = getNodeModel().getNode();
                if (node.hasProperty(strKey)) {
                    Property property = node.getProperty(strKey);
                    int type = property.getDefinition().getRequiredType();

                    Object[] result = null;
                    if (property.getDefinition().isMultiple()) {
                        Value[] values = property.getValues();
                        switch (type) {
                        case PropertyType.BOOLEAN:
                            result = new Boolean[values.length];
                            break;
                        case PropertyType.STRING:
                        default:
                            result = new String[values.length];
                            break;
                        }

                        int i = 0;
                        for (Value current : values) {
                            result[i++] = getValue(current);
                        }

                        return result;
                    } else {
                        return getValue(property.getValue());
                    }
                } else if (node.hasNode(strKey)) {
                    NodeIterator children = node.getNodes(strKey);
                    Object[] result = new JcrPluginConfig[(int) children.getSize()];

                    int i = 0;
                    while (children.hasNext()) {
                        Node child = children.nextNode();
                        result[i++] = new JcrPluginConfig(new JcrNodeModel(child));
                    }
                    return result;
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        } else {
            log.warn("Key " + key + " is not a String");
        }
        return null;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public Set keySet() {
        HashSet<String> result = new HashSet<String>();
        try {
            Node node = getNodeModel().getNode();
            if (node != null) {
                PropertyIterator properties = node.getProperties();
                while (properties.hasNext()) {
                    Property property = properties.nextProperty();
                    result.add(property.getName());
                }

                NodeIterator nodes = node.getNodes();
                while (nodes.hasNext()) {
                    Node child = nodes.nextNode();
                    result.add(child.getName());
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return result;
    }

    public int size() {
        try {
            Node node = getNodeModel().getNode();
            if (node != null) {
                HashSet<String> names = new HashSet<String>();
                NodeIterator nodes = node.getNodes();
                while (nodes.hasNext()) {
                    Node child = nodes.nextNode();
                    names.add(child.getName());
                }
                return names.size() + (int) node.getProperties().getSize();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return 0;
    }

    public Collection values() {
        HashSet<Object> result = new HashSet<Object>();
        try {
            Node node = getNodeModel().getNode();
            if (node != null) {
                PropertyIterator properties = node.getProperties();
                while (properties.hasNext()) {
                    Property property = properties.nextProperty();
                    if (property.getDefinition().isMultiple()) {
                        Value[] values = property.getValues();
                        Object[] entry = new Object[values.length];
                        int i = 0;
                        for (Value value : values) {
                            entry[i++] = getValue(value);
                        }
                        result.add(entry);
                    } else {
                        result.add(getValue(property.getValue()));
                    }
                }

                HashMap<String, List<JcrPluginConfig>> map = new HashMap<String, List<JcrPluginConfig>>();
                NodeIterator nodes = node.getNodes();
                while (nodes.hasNext()) {
                    Node child = nodes.nextNode();
                    List<JcrPluginConfig> list = map.get(child.getName());
                    if (list == null) {
                        list = new LinkedList<JcrPluginConfig>();
                        map.put(child.getName(), list);
                    }
                    list.add(new JcrPluginConfig(new JcrNodeModel(child)));
                }

                for (Map.Entry<String, List<JcrPluginConfig>> entry : map.entrySet()) {
                    JcrPluginConfig[] array = new JcrPluginConfig[entry.getValue().size()];
                    int i = 0;
                    for (JcrPluginConfig config : entry.getValue()) {
                        array[i++] = config;
                    }
                    result.add(array);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return result;
    }

    public CharSequence getCharSequence(String key) {
        // TODO implement me
        throw new UnsupportedOperationException("not implemented yet");
    }

    public Duration getDuration(String key) throws StringValueConversionException {
        // TODO implement me
        throw new UnsupportedOperationException("not implemented yet");
    }

    public long getLong(String key) throws StringValueConversionException {
        // TODO implement me
        throw new UnsupportedOperationException("not implemented yet");
    }

    public long getLong(String key, long defaultValue) throws StringValueConversionException {
        // TODO implement me
        throw new UnsupportedOperationException("not implemented yet");
    }

    public Time getTime(String key) throws StringValueConversionException {
        // TODO implement me
        throw new UnsupportedOperationException("not implemented yet");
    }

    public boolean isImmutable() {
        return false;
    }

    public IValueMap makeImmutable() {
        // TODO implement me
        throw new UnsupportedOperationException("not implemented yet");
    }

    private Property getProperty(String key) throws RepositoryException {
        Node node = getNodeModel().getNode();
        if (node != null) {
            if (node.hasProperty(key)) {
                return node.getProperty(key);
            }
        } else {
            log.warn("Node model is not valid");
        }
        return null;
    }

    private Object getValue(Value value) throws RepositoryException {
        switch (value.getType()) {
        case PropertyType.BOOLEAN:
            return new Boolean(value.getBoolean());
        case PropertyType.STRING:
        default:
            return value.getString();
        }
    }
}
