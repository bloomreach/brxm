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
package org.hippoecm.frontend.plugin.config.impl;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.StringValueConversionException;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.time.Time;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.map.JcrMap;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrPluginConfig extends AbstractMap implements IPluginConfig {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrPluginConfig.class);

    protected final JcrNodeModel nodeModel;
    private transient JcrMap map;
    private transient Set<Entry<String, Object>> entries;

    public JcrPluginConfig(JcrNodeModel nodeModel) {
        this.nodeModel = nodeModel;
    }

    public JcrNodeModel getNodeModel() {
        return nodeModel;
    }

    public String getName() {
        try {
            Node node = nodeModel.getNode();
            if (node != null) {
                return node.getName();
            } else {
                log.warn("Node model is not valid");
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
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
                    return new String[] { property.getValue().getString() };
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

    public IPluginConfig getPluginConfig(Object key) {
        if (key instanceof String) {
            String strKey = (String) key;
            try {
                Node node = nodeModel.getNode();
                if (node.hasNode(strKey)) {
                    Node child = node.getNode(strKey);
                    return new JcrPluginConfig(new JcrNodeModel(child));
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        } else {
            log.warn("Key " + key + " is not a String");
        }
        return null;
    }

    public Set<IPluginConfig> getPluginConfigSet() {
        Set<IPluginConfig> configs = new LinkedHashSet<IPluginConfig>();
        try {
            NodeIterator children = nodeModel.getNode().getNodes();
            for (int i = 0; children.hasNext(); i++) {
                Node child = children.nextNode();
                if (child != null) {
                    configs.add(new JcrPluginConfig(new JcrNodeModel(child)));
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return configs;
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

    public void detach() {
        nodeModel.detach();
        entries = null;
    }

    @Override
    public Object get(Object key) {
        JcrMap jcrMap = getMap();
        Object obj = jcrMap.get(key);
        if (obj == null) {
            return null;
        }
        Object result;
        if (obj.getClass().isArray()) {
            int size = Array.getLength(obj);
            Class<?> componentType = obj.getClass().getComponentType();
            result = Array.newInstance(componentType, size);
            for (int i = 0; i < size; i++) {
                Array.set(result, i, filter(Array.get(obj, i)));
            }
        } else {
            result = filter(obj);
        }
        return result;
    }

    @Override
    public Object put(Object key, Object value) {
        JcrMap jcrMap = getMap();
        return jcrMap.put((String) key, value);
    }
    
    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        if (entries == null) {
            final JcrMap jcrMap = getMap();
            final Set<Map.Entry<String, Object>> orig = jcrMap.entrySet();
            entries = new LinkedHashSet<Map.Entry<String, Object>>();
            for (final Map.Entry<String, Object> entry : orig) {
                entries.add(new Map.Entry<String, Object>() {

                    public String getKey() {
                        return entry.getKey();
                    }

                    public Object getValue() {
                        return JcrPluginConfig.this.get(entry.getKey());
                    }

                    public Object setValue(Object value) {
                        return jcrMap.put(entry.getKey(), value);
                    }
                });
            }
        }
        return entries;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof JcrPluginConfig) {
            return ((JcrPluginConfig) other).nodeModel.equals(nodeModel);
        }
        return false;
    }

    public int hashCode() {
        return 521 * nodeModel.hashCode();
    }

    private Property getProperty(String key) throws RepositoryException {
        Node node = nodeModel.getNode();
        if (node != null) {
            if (node.hasProperty(key)) {
                return node.getProperty(key);
            }
        } else {
            log.warn("Node model is not valid");
        }
        return null;
    }

    private JcrMap getMap() {
        if (map == null) {
            map = new JcrMap(nodeModel.getNode());
        }
        return map;
    }
    
    private Object filter(Object value) {
        if (value instanceof JcrMap) {
            JcrMap map = (JcrMap) value;
            return new JcrPluginConfig(new JcrNodeModel(map.getNode()));
        } else if (value instanceof List) {
            final List list = (List) value;
            return new AbstractList() {

                @Override
                public Object get(int index) {
                    return filter(list.get(index));
                }

                @Override
                public int size() {
                    return list.size();
                }
            };
        }
        return value;
    }
}
