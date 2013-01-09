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
package org.hippoecm.frontend.plugin.config.impl;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.StringValueConversionException;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.time.Time;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.FrontendNodeType;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.JcrEventListener;
import org.hippoecm.frontend.model.map.AbstractValueMap;
import org.hippoecm.frontend.model.map.IHippoMap;
import org.hippoecm.frontend.model.map.JcrMap;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.PluginConfigEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The JCR backed implementation of the plugin configuration.  It will try to do type conversion
 * (i.e. string to boolean and vice versa) and single to multiple (and the reverse).
 * <p>
 * It allows wrapping, so that subclasses can intercept the retrieval of values to e.g. interpolate
 * these.
 */
public class JcrPluginConfig extends AbstractValueMap implements IPluginConfig, IDetachable {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrPluginConfig.class);

    private class EntryDecorator implements Map.Entry<String, Object> {

        private final String key;

        EntryDecorator(Map.Entry<String, Object> upstream) {
            this.key = upstream.getKey();
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Object getValue() {
            return JcrPluginConfig.this.get(key);
        }

        @Override
        public Object setValue(Object value) {
            return JcrPluginConfig.this.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    private class SerializableList extends AbstractList implements Serializable {
        private static final long serialVersionUID = 1L;

        private List upstream;

        SerializableList(List upstream) {
            this.upstream = upstream;
        }

        @Override
        public Object get(int index) {
            return wrap(upstream.get(index));
        }

        @Override
        public int size() {
            return upstream.size();
        }

    }

    private class ConfigMap extends AbstractMap<String, Object> implements IHippoMap {

        private IPluginConfig upstream;

        ConfigMap(IPluginConfig config) {
            this.upstream = config;
        }

        @Override
        public Set<java.util.Map.Entry<String, Object>> entrySet() {
            final Set<Map.Entry<String, Object>> base = upstream.entrySet();
            return new AbstractSet<Map.Entry<String, Object>>() {

                @Override
                public Iterator<Map.Entry<String, Object>> iterator() {
                    final Iterator<Map.Entry<String, Object>> iter = base.iterator();
                    return new Iterator<Map.Entry<String, Object>>() {

                        @Override
                        public boolean hasNext() {
                            return iter.hasNext();
                        }

                        public Map.Entry<String, Object> next() {
                            final Map.Entry<String, Object> entry = iter.next();
                            return new Map.Entry<String, Object>() {

                                @Override
                                public String getKey() {
                                    return entry.getKey();
                                }

                                @Override
                                public Object getValue() {
                                    return unwrap(entry.getValue());
                                }

                                @Override
                                public Object setValue(Object value) {
                                    return upstream.put(entry.getKey(), wrap(value));
                                }

                            };
                        }

                        @Override
                        public void remove() {
                            iter.remove();
                        }

                    };
                }

                @Override
                public int size() {
                    return base.size();
                }

            };

        }

        public List<String> getMixinTypes() {
            return Collections.emptyList();
        }

        public String getPrimaryType() {
            return FrontendNodeType.NT_PLUGINCONFIG;
        }

        public void reset() {
        }

        public void save() {
        }

    }

    protected JcrNodeModel nodeModel;
    private IObservationContext<IPluginConfig> obContext;
    private JcrEventListener listener;
    private JcrMap map;
    private Map<JcrNodeModel, JcrPluginConfig> childConfigs;
    private transient Set<Map.Entry<String, Object>> entries;

    public JcrPluginConfig(JcrNodeModel nodeModel) {
        if (nodeModel == null) {
            throw new RuntimeException("Invalid node model <null>");
        }
        this.nodeModel = nodeModel;
        this.map = new JcrMap(nodeModel);
        this.childConfigs = new HashMap<JcrNodeModel, JcrPluginConfig>();
    }

    protected void sync() {
        entries = null;
        Iterator<Map.Entry<JcrNodeModel, JcrPluginConfig>> childIter = childConfigs.entrySet().iterator();
        while (childIter.hasNext()) {
            Map.Entry<JcrNodeModel, JcrPluginConfig> entry = childIter.next();
            JcrNodeModel model = entry.getKey();
            if (!model.getItemModel().exists()) {
                childIter.remove();
                entry.getValue();
            }
        }
    }

    public JcrNodeModel getNodeModel() {
        return nodeModel;
    }

    @Override
    public String getName() {
        try {
            Node node = nodeModel.getNode();
            if (node != null) {
                return node.getName() + (node.getIndex() > 1 ? "[" + node.getIndex() + "]" : "");
            } else {
                log.warn("Node model is not valid");
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    @Override
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

    @Override
    public double getDouble(String key) throws StringValueConversionException {
        return getDouble(key, 0.0);
    }

    @Override
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

    @Override
    public int getInt(String key) throws StringValueConversionException {
        return getInt(key, 0);
    }

    @Override
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

    @Override
    public long getLong(String key) throws StringValueConversionException {
        return getLong(key, 0);
    }

    @Override
    public long getLong(String key, long defaultValue) throws StringValueConversionException {
        try {
            Property property = getProperty(key);
            if (property != null) {
                return property.getLong();
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return defaultValue;
    }

    @Override
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

    @Override
    public String getString(String key) {
        return getKey(key);
    }

    @Override
    public String getString(String key, String defaultValue) {
        String result = getKey(key);
        if (result == null) {
            return defaultValue;
        }
        return result;
    }

    @Override
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

    @Override
    public StringValue getStringValue(String key) {
        return StringValue.valueOf(getString(key));
    }

    @Override
    public IPluginConfig getPluginConfig(Object key) {
        if (key instanceof String) {
            String strKey = (String) key;
            try {
                Node node = nodeModel.getNode();
                if (node.hasNode(strKey)) {
                    Node child = node.getNode(strKey);
                    return wrapConfig(child);
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        } else {
            log.warn("Key " + key + " is not a String");
        }
        return null;
    }

    @Override
    public Set<IPluginConfig> getPluginConfigSet() {
        Set<IPluginConfig> configs = new LinkedHashSet<IPluginConfig>();
        try {
            NodeIterator children = nodeModel.getNode().getNodes();
            while (children.hasNext()) {
                Node child = children.nextNode();
                if (child != null) {
                    configs.add(wrapConfig(child));
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return configs;
    }

    @Override
    public CharSequence getCharSequence(String key) {
        // TODO implement me
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public Duration getDuration(String key) throws StringValueConversionException {
        // TODO implement me
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public Time getTime(String key) throws StringValueConversionException {
        // TODO implement me
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public boolean isImmutable() {
        return false;
    }

    @Override
    public IValueMap makeImmutable() {
        // TODO implement me
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void detach() {
        nodeModel.detach();
        map.detach();
        entries = null;
        for (Map.Entry<JcrNodeModel, JcrPluginConfig> entry : childConfigs.entrySet()) {
            entry.getKey().detach();
            entry.getValue().detach();
        }
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
                Array.set(result, i, wrap(Array.get(obj, i)));
            }
        } else {
            result = wrap(obj);
        }
        return result;
    }

    @Override
    public Object put(String key, Object value) {
        entries = null;
        return map.put(key, unwrap(value));
    }

    @Override
    public void clear() {
        entries = null;
        map.clear();
        childConfigs.clear();
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
        return other instanceof JcrPluginConfig && ((JcrPluginConfig) other).nodeModel.equals(nodeModel);
    }

    @Override
    public int hashCode() {
        return 521 * nodeModel.hashCode();
    }

    @Override
    public String toString() {
        return "JcrPluginConfig:" + nodeModel.getItemModel().getPath();
    }

    protected JcrPluginConfig wrapConfig(Node node) {
        JcrNodeModel nodeModel = new JcrNodeModel(node);
        if (!childConfigs.containsKey(nodeModel)) {
            JcrPluginConfig childConfig = new JcrPluginConfig(nodeModel);
            childConfigs.put(nodeModel, childConfig);
        }
        JcrPluginConfig result = childConfigs.get(nodeModel);
        result.nodeModel.detach();
        return result;
    }

    protected IHippoMap unwrapConfig(IPluginConfig value) {
        return new ConfigMap(value);
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

    @SuppressWarnings("unchecked")
    protected Object wrap(Object value) {
        if (value instanceof JcrMap) {
            JcrMap map = (JcrMap) value;
            return wrapConfig(map.getNode());
        } else if (value instanceof List) {
            return new SerializableList((List) value);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    protected Object unwrap(Object value) {
        if (value instanceof IPluginConfig) {
            return unwrapConfig((IPluginConfig) value);
        } else if (value instanceof List) {
            List<IHippoMap> list = new ArrayList<IHippoMap>(((List<IPluginConfig>) value).size());
            for (IPluginConfig entry : (List<IPluginConfig>) value) {
                list.add(unwrapConfig(entry));
            }
            return list;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setObservationContext(IObservationContext<? extends IObservable> context) {
        this.obContext = (IObservationContext<IPluginConfig>) context;
    }

    protected IObservationContext<? extends IPluginConfig> getObservationContext() {
        return obContext;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void startObservation() {
        IObservationContext obContext = getObservationContext();
        String path = getNodeModel().getItemModel().getPath();
        int events = Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED
                | Event.PROPERTY_REMOVED;
        listener = new JcrEventListener(obContext, events, path, true, null, null) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onEvent(EventIterator events) {
                IObservationContext obContext = getObservationContext();
                sync();
                EventCollection<PluginConfigEvent> coll = new EventCollection<PluginConfigEvent>();
                coll.add(new PluginConfigEvent(JcrPluginConfig.this, PluginConfigEvent.EventType.CONFIG_CHANGED));
                obContext.notifyObservers(coll);
            }
        };

        listener.start();
    }

    @Override
    public void stopObservation() {
        if (listener != null) {
            listener.stop();
            listener = null;
        }
    }

}
