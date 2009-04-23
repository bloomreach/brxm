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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.StringValueConversionException;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.time.Time;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.event.ListenerList;
import org.hippoecm.frontend.model.map.HippoMap;
import org.hippoecm.frontend.model.map.IHippoMap;
import org.hippoecm.frontend.model.map.JcrMap;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrPluginConfig extends AbstractMap implements IPluginConfig, IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrPluginConfig.class);

    private class EntryDecorator implements Map.Entry<String, Object> {

        String key;

        EntryDecorator(Map.Entry<String, Object> upstream) {
            this.key = upstream.getKey();
        }

        public String getKey() {
            return key;
        }

        public Object getValue() {
            return JcrPluginConfig.this.get(key);
        }

        public Object setValue(Object value) {
            return JcrPluginConfig.this.put(key, value);
        }
    }

    protected final JcrNodeModel nodeModel;
    private IPluginContext context;
    private IObserver observer;
    private List<IPluginConfigListener> listeners = new ListenerList<IPluginConfigListener>();
    private JcrMap map;
    private transient Set<Map.Entry<String, Object>> entries;

    public JcrPluginConfig(JcrNodeModel nodeModel, IPluginContext context) {
        this.nodeModel = nodeModel;
        this.context = context;
        this.map = new JcrMap(nodeModel);
    }

    public JcrNodeModel getNodeModel() {
        return nodeModel;
    }

    protected IPluginContext getPluginContext() {
        return context;
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
                    return new JcrPluginConfig(new JcrNodeModel(child), context);
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
                    configs.add(new JcrPluginConfig(new JcrNodeModel(child), context));
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
        map.detach();
        entries = null;
    }

    @Override
    public Object get(Object key) {
        Object obj = map.get(key);
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
        if (value instanceof IPluginConfig) {
            HippoMap map = new HippoMap();
            map.setPrimaryType("frontend:pluginconfig");
            map.putAll((IPluginConfig) value);
            value = map;
        } else if (value instanceof List) {
            List<IHippoMap> list = new ArrayList<IHippoMap>(((List<IPluginConfig>) value).size());
            for (IPluginConfig entry : (List<IPluginConfig>) value) {
                HippoMap map = new HippoMap();
                map.setPrimaryType("frontend:pluginconfig");
                map.putAll((IPluginConfig) entry);
                list.add(map);
            }
            value = list;
        }
        entries = null;
        return map.put((String) key, value);
    }

    @Override
    public void clear() {
        entries = null;
        map.clear();
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        if (entries == null) {
            final Set<Map.Entry<String, Object>> orig = map.entrySet();
            entries = new LinkedHashSet<Map.Entry<String, Object>>();
            for (final Map.Entry<String, Object> entry : orig) {
                entries.add(new EntryDecorator(entry));
            }
        }
        return Collections.unmodifiableSet(entries);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof JcrPluginConfig) {
            return ((JcrPluginConfig) other).nodeModel.equals(nodeModel);
        }
        return false;
    }

    @Override
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

    private Object filter(Object value) {
        if (value instanceof JcrMap) {
            JcrMap map = (JcrMap) value;
            return new JcrPluginConfig(new JcrNodeModel(map.getNode()), context);
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

    public void addPluginConfigListener(IPluginConfigListener listener) {
        listeners.add(listener);
        if (listeners.size() > 0 && context != null) {
            observer = new IObserver() {
                private static final long serialVersionUID = 1L;

                public IObservable getObservable() {
                    return nodeModel;
                }

                public void onEvent(IEvent event) {
                    entries = null;
                    for (IPluginConfigListener listener : listeners) {
                        listener.onPluginConfigChanged();
                    }
                }

            };
            context.registerService(observer, IObserver.class.getName());
        }
    }

    public void removePluginConfigListener(IPluginConfigListener listener) {
        listeners.remove(listener);
        if (listeners.size() == 0 && context != null) {
            context.unregisterService(observer, IObserver.class.getName());
            observer = null;
        }
    }

}
