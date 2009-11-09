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

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.StringValueConversionException;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.time.Time;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.PluginConfigEvent;

public abstract class AbstractPluginDecorator extends AbstractMap implements IPluginConfig, IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    private class ListWrapper extends AbstractList implements Serializable {
        private static final long serialVersionUID = 1L;

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
    protected IObservationContext obContext;
    private IObserver observer;
    private Map<IPluginConfig, IPluginConfig> wrapped;

    public AbstractPluginDecorator(IPluginConfig upstream) {
        this.upstream = upstream;
        this.wrapped = new HashMap<IPluginConfig, IPluginConfig>();
    }

    public String getName() {
        return upstream.getName();
    }

    public IPluginConfig getPluginConfig(Object key) {
        return wrapConfig(upstream.getPluginConfig(key));
    }

    public Set<IPluginConfig> getPluginConfigSet() {
        Set<IPluginConfig> configSet = new LinkedHashSet<IPluginConfig>();
        for (IPluginConfig config : upstream.getPluginConfigSet()) {
            configSet.add(wrapConfig(config));
        }
        return configSet;
    }

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
    public Object get(Object key) {
        Object obj = upstream.get(key);
        if (obj == null) {
            return obj;
        }
        return wrap(obj);
    }

    @Override
    public final Set entrySet() {
        final Set orig = upstream.entrySet();
        return new AbstractSet() {

            @Override
            public Iterator iterator() {
                final Iterator origIter = orig.iterator();
                return new Iterator() {

                    public boolean hasNext() {
                        return origIter.hasNext();
                    }

                    public Object next() {
                        final Entry entry = (Map.Entry) origIter.next();
                        if (entry != null) {
                            return new Map.Entry() {

                                public Object getKey() {
                                    return entry.getKey();
                                }

                                public Object getValue() {
                                    return AbstractPluginDecorator.this.get(entry.getKey());
                                }

                                public Object setValue(Object value) {
                                    return AbstractPluginDecorator.this.put(entry.getKey(), value);
                                }

                            };
                        }
                        return null;
                    }

                    public void remove() {
                        origIter.remove();
                    }

                };
            }

            @Override
            public int size() {
                return orig.size();
            }

        };
    }

    public boolean isImmutable() {
        return false;
    }

    public IValueMap makeImmutable() {
        throw new UnsupportedOperationException("JavaPluginConfig is always mutable");
    }

    /**
     * @see IValueMap#getBoolean(String)
     */
    public final boolean getBoolean(final String key) throws StringValueConversionException {
        return getStringValue(key).toBoolean();
    }

    /**
     * @see IValueMap#getDouble(String)
     */
    public final double getDouble(final String key) throws StringValueConversionException {
        return getDouble(key, 0d);
    }

    /**
     * @see IValueMap#getDouble(String, double)
     */
    public final double getDouble(final String key, final double defaultValue) throws StringValueConversionException {
        final String value = getString(key);
        return value != null ? parseDouble(value) : defaultValue;
    }

    private double parseDouble(String value) throws StringValueConversionException{
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException nfe) {
            throw new StringValueConversionException("Failed to convert String to Double", nfe);
        }
    }

    /**
     * @see IValueMap#getDuration(String)
     */
    public final Duration getDuration(final String key) throws StringValueConversionException {
        return getStringValue(key).toDuration();
    }

    /**
     * @see IValueMap#getInt(String)
     */
    public final int getInt(final String key) throws StringValueConversionException {
        return getStringValue(key).toInt();
    }

    /**
     * @see IValueMap#getInt(String, int)
     */
    public final int getInt(final String key, final int defaultValue) throws StringValueConversionException {
        return getStringValue(key).toInt(defaultValue);
    }

    /**
     * @see IValueMap#getLong(String)
     */
    public final long getLong(final String key) throws StringValueConversionException {
        return getStringValue(key).toLong();
    }

    /**
     * @see IValueMap#getLong(String, long)
     */
    public final long getLong(final String key, final long defaultValue) throws StringValueConversionException {
        return getStringValue(key).toLong(defaultValue);
    }

    /**
     * @see IValueMap#getString(String, String)
     */
    public final String getString(final String key, final String defaultValue) {
        final String value = getString(key);
        return value != null ? value : defaultValue;
    }

    /**
     * @see IValueMap#getString(String)
     */
    public final String getString(final String key) {
        final Object o = get(key);
        if (o == null) {
            return null;
        } else if (o.getClass().isArray() && Array.getLength(o) > 0) {
            // if it is an array just get the first value
            final Object arrayValue = Array.get(o, 0);
            if (arrayValue == null) {
                return null;
            } else {
                return arrayValue.toString();
            }

        } else {
            return o.toString();
        }
    }

    /**
     * @see IValueMap#getCharSequence(String)
     */
    public final CharSequence getCharSequence(final String key) {
        final Object o = get(key);
        if (o == null) {
            return null;
        } else if (o.getClass().isArray() && Array.getLength(o) > 0) {
            // if it is an array just get the first value
            final Object arrayValue = Array.get(o, 0);
            if (arrayValue == null) {
                return null;
            } else {
                if (arrayValue instanceof CharSequence) {
                    return (CharSequence) arrayValue;
                }
                return arrayValue.toString();
            }

        } else {
            if (o instanceof CharSequence) {
                return (CharSequence) o;
            }
            return o.toString();
        }
    }

    /**
     * @see IValueMap#getStringArray(String)
     */
    public String[] getStringArray(final String key) {
        final Object o = get(key);
        if (o == null) {
            return null;
        } else if (o instanceof String[]) {
            return (String[]) o;
        } else if (o.getClass().isArray()) {
            int length = Array.getLength(o);
            String[] array = new String[length];
            for (int i = 0; i < length; i++) {
                final Object arrayValue = Array.get(o, i);
                if (arrayValue != null) {
                    array[i] = arrayValue.toString();
                }
            }
            return array;
        }
        return new String[] { o.toString() };
    }

    /**
     * @see IValueMap#getStringValue(String)
     */
    public StringValue getStringValue(final String key) {
        return StringValue.valueOf(getString(key));
    }

    /**
     * @see IValueMap#getTime(String)
     */
    public final Time getTime(final String key) throws StringValueConversionException {
        return getStringValue(key).toTime();
    }

    public String getKey(final String key) {
        Iterator iter = keySet().iterator();
        while (iter.hasNext()) {
            Object keyValue = iter.next();
            if (keyValue instanceof String) {
                String keyString = (String) keyValue;
                if (key.equalsIgnoreCase(keyString)) {
                    return keyString;
                }
            }
        }
        return null;
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
        } else if (obj instanceof List) {
            return new ListWrapper((List) obj);
        } else if (obj instanceof IPluginConfig) {
            return wrapConfig((IPluginConfig) obj);
        } else {
            return decorate(obj);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AbstractPluginDecorator) {
            return ((AbstractPluginDecorator) o).upstream.equals(upstream);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return upstream.hashCode() ^ 34603;
    }

    protected abstract Object decorate(Object object);

    public void setObservationContext(IObservationContext context) {
        this.obContext = context;
    }

    protected IObservationContext getObservationContext() {
        return obContext;
    }
    
    public void startObservation() {
        obContext.registerObserver(observer = new IObserver() {
            private static final long serialVersionUID = 1L;

            public IObservable getObservable() {
                return upstream;
            }

            public void onEvent(Iterator<? extends IEvent> events) {
                EventCollection<PluginConfigEvent> collection = new EventCollection<PluginConfigEvent>();
                while (events.hasNext()) {
                    IEvent event = events.next();
                    if (event instanceof PluginConfigEvent) {
                        PluginConfigEvent pce = (PluginConfigEvent) event;
                        collection.add(new PluginConfigEvent(AbstractPluginDecorator.this, PluginConfigEvent.EventType.CONFIG_CHANGED));
                    }
                }
                obContext.notifyObservers(collection);
            }
            
        });
    }

    public void stopObservation() {
        obContext.unregisterObserver(observer);
    }

}
