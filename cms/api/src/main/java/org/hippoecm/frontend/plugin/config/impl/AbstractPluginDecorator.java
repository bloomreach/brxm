/*
 *  Copyright 2008-2022 Hippo B.V. (http://www.onehippo.com)
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
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractList;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.util.string.StringValueConversionException;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.map.AbstractValueMap;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.PluginConfigEvent;

public abstract class AbstractPluginDecorator extends AbstractValueMap implements IPluginConfig, IDetachable {

    @SuppressWarnings("unchecked")
    private class ListWrapper extends AbstractList implements Serializable {

        private List list;

        ListWrapper(List list) {
            this.list = list;
        }

        @Override
        public Object get(int index) {
            return wrap(list.get(index));
        }

        @Override
        public int size() {
            return list.size();
        }

    }

    protected IPluginConfig upstream;
    protected IObservationContext<IPluginConfig> obContext;
    private IObserver<IPluginConfig> observer;
    private Map<IPluginConfig, IPluginConfig> wrapped;

    public AbstractPluginDecorator(IPluginConfig upstream) {
        this.upstream = upstream;
        this.wrapped = new HashMap<>();
    }

    @Override
    public String getName() {
        return upstream.getName();
    }

    @Override
    public IPluginConfig getPluginConfig(Object key) {
        return wrapConfig(upstream.getPluginConfig(key));
    }

    @Override
    public Set<IPluginConfig> getPluginConfigSet() {
        Set<IPluginConfig> configSet = new LinkedHashSet<>();
        for (IPluginConfig config : upstream.getPluginConfigSet()) {
            configSet.add(wrapConfig(config));
        }
        return configSet;
    }

    @Override
    public void detach() {
        if (upstream instanceof IDetachable) {
            ((IDetachable) upstream).detach();
        }
        for (IPluginConfig wrapper : wrapped.values()) {
            if (wrapper instanceof IDetachable) {
                ((IDetachable) wrapper).detach();
            }
        }
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public Object get(Object key) {
        Object obj = upstream.get(key);
        if (obj == null) {
            return null;
        }
        return wrap(obj);
    }

    @Override
    public String toString() {
        return upstream.toString();
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        final Set<Map.Entry<String, Object>> orig = upstream.entrySet();
        return new AbstractSet<>() {

            @Override
            public Iterator<Map.Entry<String, Object>> iterator() {
                final Iterator<Map.Entry<String, Object>> origIter = orig.iterator();
                return new Iterator<Map.Entry<String, Object>>() {

                    Entry<String, Object> next = null;

                    @Override
                    public boolean hasNext() {
                        fetchNext();
                        return next != null;
                    }

                    @Override
                    public Map.Entry<String, Object> next() {
                        if (next != null) {
                            Map.Entry<String, Object> ret = next;
                            next = null;
                            return ret;
                        }
                        throw new NoSuchElementException();
                    }

                    protected void fetchNext() {
                        while (next == null && origIter.hasNext()) {
                            final Entry<String, Object> entry = origIter.next();
                            if (entry != null && AbstractPluginDecorator.this.containsKey(entry.getKey())) {
                                next = new Map.Entry<>() {

                                    @Override
                                    public String getKey() {
                                        return entry.getKey();
                                    }

                                    @Override
                                    public Object getValue() {
                                        return AbstractPluginDecorator.this.get(entry.getKey());
                                    }

                                    @Override
                                    public Object setValue(Object value) {
                                        return AbstractPluginDecorator.this.put(entry.getKey(), value);
                                    }

                                };
                            }
                        }
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }

                };
            }

            @Override
            public int size() {
                int count = 0;
                Iterator<?> iter = iterator();
                while (iter.hasNext()) {
                    iter.next();
                    count++;
                }
                return count;
            }

        };
    }

    @Override
    public boolean isImmutable() {
        return true;
    }

    @Override
    public IValueMap makeImmutable() {
        throw new UnsupportedOperationException("JavaPluginConfig is always mutable");
    }

    protected IPluginConfig wrapConfig(IPluginConfig config) {
        if (!wrapped.containsKey(config)) {
            wrapped.put(config, (IPluginConfig) decorate(config));
        }
        return wrapped.get(config);
    }

    protected final Object wrap(Object obj) {
        if (obj.getClass().isArray()) {
            int size = Array.getLength(obj);
            Class<?> componentType = obj.getClass().getComponentType();
            Object array = Array.newInstance(componentType, size);
            for (int i = 0; i < size; i++) {
                Object entry = Array.get(obj, i);
                Array.set(array, i, wrap(entry));
            }
            return array;
        } else if (obj instanceof List<?>) {
            return new ListWrapper((List<?>) obj);
        } else if (obj instanceof IPluginConfig) {
            return wrapConfig((IPluginConfig) obj);
        } else {
            return decorate(obj);
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AbstractPluginDecorator && ((AbstractPluginDecorator) o).upstream.equals(upstream);
    }

    @Override
    public int hashCode() {
        return upstream.hashCode() ^ 34603;
    }

    protected abstract Object decorate(Object object);

    @Override
    @SuppressWarnings("unchecked")
    public void setObservationContext(IObservationContext<? extends IObservable> context) {
        this.obContext = (IObservationContext<IPluginConfig>) context;
    }

    protected IObservationContext<? extends IPluginConfig> getObservationContext() {
        return obContext;
    }

    @Override
    public void startObservation() {
        obContext.registerObserver(observer = new IObserver<>() {

            @Override
            public IPluginConfig getObservable() {
                return upstream;
            }

            @Override
            public void onEvent(Iterator<? extends IEvent<IPluginConfig>> events) {
                EventCollection<IEvent<IPluginConfig>> collection = new EventCollection<>();
                if (events.hasNext()) {
                    collection.add(new PluginConfigEvent(AbstractPluginDecorator.this,
                            PluginConfigEvent.EventType.CONFIG_CHANGED));
                }
                obContext.notifyObservers(collection);
            }

        });
    }

    @Override
    public void stopObservation() {
        obContext.unregisterObserver(observer);
    }

    @Override
    public double getDouble(String key) throws StringValueConversionException {
        Object value = get(key);
        if (value == null) {
            throw new StringValueConversionException("Key '" + key + "' not found");
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        final String s = value.toString();
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new StringValueConversionException("Cannot convert '" + s + "' to double value", e);
        }
    }

    @Override
    public double getDouble(String key, double defaultValue) throws StringValueConversionException {
        Object value = get(key);
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value == null) {
            return defaultValue;
        }
        final String s = value.toString();
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new StringValueConversionException("Cannot convert '" + s + "' to double value", e);
        }
    }

    @Override
    public Duration getDuration(String key) throws StringValueConversionException {
        Object value = get(key);
        if (value instanceof Duration) {
            return (Duration) value;
        }
        return super.getDuration(key);
    }

    @Override
    public Instant getInstant(String key) throws StringValueConversionException {
        Object value = get(key);
        if (value instanceof Instant) {
            return (Instant) value;
        }
        return super.getInstant(key);
    }

}
