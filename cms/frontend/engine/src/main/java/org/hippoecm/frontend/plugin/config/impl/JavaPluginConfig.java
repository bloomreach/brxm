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
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.util.collections.MiniMap;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.StringValueConversionException;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.time.Time;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.model.event.ListenerList;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigListener;

public class JavaPluginConfig extends LinkedHashMap implements IPluginConfig {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private List<IPluginConfigListener> listeners = new ListenerList<IPluginConfigListener>();
    private Set<IPluginConfig> configSet = new LinkedHashSet<IPluginConfig>();

    private final int hashCode = new Object().hashCode();
    private String pluginInstanceName = null;

    public JavaPluginConfig() {
        super();
    }

    public JavaPluginConfig(String name) {
        super();
        pluginInstanceName = name;
    }

    public JavaPluginConfig(IPluginConfig parentConfig) {
        super(parentConfig == null ? new MiniMap(0) : parentConfig);
        for (IPluginConfig config : parentConfig.getPluginConfigSet()) {
            configSet.add(newPluginConfig(config));
        }
        pluginInstanceName = parentConfig.getName();
    }

    protected IPluginConfig newPluginConfig(IPluginConfig config) {
        return new JavaPluginConfig(config);
    }

    public String getName() {
        return pluginInstanceName;
    }

    public Set<IPluginConfig> getPluginConfigSet() {
        return configSet;
    }

    public IPluginConfig getPluginConfig(Object key) {
        Object value = get(key);
        if (value instanceof IPluginConfig) {
            return newPluginConfig((IPluginConfig) value);
        } else if (value instanceof List && ((List) value).size() > 0) {
            return newPluginConfig((IPluginConfig) ((List) value).get(0));
        } else {
            return new JavaPluginConfig();
        }
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
        return getStringValue(key).toDouble();
    }

    /**
     * @see IValueMap#getDouble(String, double)
     */
    public final double getDouble(final String key, final double defaultValue) throws StringValueConversionException {
        return getStringValue(key).toDouble(defaultValue);
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

    /**
     * Adds the value to this <code>ValueMap</code> with the given key. If the key already is in
     * the <code>ValueMap</code> it will combine the values into a <code>String</code> array,
     * else it will just store the value itself.
     *
     * @param key
     *            the key to store the value under
     * @param value
     *            the value that must be added/merged to the <code>ValueMap</code>
     * @return the value itself if there was no previous value, or a <code>String</code> array
     *         with the combined values
     */
    public final Object add(final String key, final String value) {
        final Object o = get(key);
        if (o == null) {
            return put(key, value);
        } else if (o.getClass().isArray()) {
            int length = Array.getLength(o);
            String destArray[] = new String[length + 1];
            for (int i = 0; i < length; i++) {
                final Object arrayValue = Array.get(o, i);
                if (arrayValue != null) {
                    destArray[i] = arrayValue.toString();
                }
            }
            destArray[length] = value;

            return put(key, destArray);
        } else {
            return put(key, new String[] { o.toString(), value });
        }
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

    /**
     * Generates a <code>String</code> representation of this object.
     *
     * @return <code>String</code> representation of this <code>ValueMap</code> consistent with
     *         the tag-attribute style of markup elements. For example:
     *         <code>a="x" b="y" c="z"</code>.
     */
    public String toString() {
        final StringBuffer buffer = new StringBuffer();
        for (final Iterator iterator = entrySet().iterator(); iterator.hasNext();) {
            final Map.Entry entry = (Map.Entry) iterator.next();
            buffer.append(entry.getKey());
            buffer.append(" = \"");
            final Object value = entry.getValue();
            if (value == null) {
                buffer.append("null");
            } else if (value.getClass().isArray()) {
                buffer.append(Arrays.asList((Object[]) value));
            } else {
                buffer.append(value);
            }

            buffer.append("\"");
            if (iterator.hasNext()) {
                buffer.append(' ');
            }
        }
        return buffer.toString();
    }

    public Object put(String key, Object value) {
        Object oldValue = super.put(key, value);
        for (IPluginConfigListener listener : listeners) {
            listener.onPluginConfigChanged();
        }
        return oldValue;
    }

    @Override
    public Set entrySet() {
        final Set<Map.Entry> entries = super.entrySet();
        return new AbstractSet<Map.Entry>() {

            @Override
            public Iterator<Map.Entry> iterator() {
                final Iterator<Map.Entry> orig = entries.iterator();
                return new Iterator<Map.Entry>() {

                    public boolean hasNext() {
                        return orig.hasNext();
                    }

                    public Map.Entry next() {
                        final Map.Entry entry = orig.next();
                        return new Map.Entry() {

                            public Object getKey() {
                                return entry.getKey();
                            }

                            public Object getValue() {
                                return entry.getValue();
                            }

                            public Object setValue(Object value) {
                                return JavaPluginConfig.this.put(entry.getKey(), value);
                            }

                        };
                    }

                    public void remove() {
                        // TODO Auto-generated method stub

                    }

                };
            }

            @Override
            public int size() {
                return entries.size();
            }

        };
    }

    public void addPluginConfigListener(IPluginConfigListener listener) {
        listeners.add(listener);
    }

    public void removePluginConfigListener(IPluginConfigListener listener) {
        listeners.remove(listener);
    }

    // override super equals(), hashCode() as they depend on entry equivalence

    @Override
    public final boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

}
