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
package org.hippoecm.frontend.model.map;

import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.StringValueConversionException;
import org.apache.wicket.util.time.Duration;
import org.apache.wicket.util.time.Time;
import org.apache.wicket.util.value.IValueMap;

public abstract class AbstractValueMap extends AbstractMap implements IValueMap {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private boolean immutable = false;

    public boolean isImmutable() {
        return immutable;
    }

    public IValueMap makeImmutable() {
        return null;
    }

    public final void clear() {
        checkMutability();
        super.clear();
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
    public final double getDouble(final String key, final double defaultValue)
            throws StringValueConversionException {
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
    public final int getInt(final String key, final int defaultValue)
            throws StringValueConversionException {
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
    public final long getLong(final String key, final long defaultValue)
            throws StringValueConversionException {
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
        return new String[]{o.toString()};
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
        checkMutability();
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
            return put(key, new String[]{o.toString(), value});
        }
    }

    /**
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(final Map map) {
        checkMutability();
        super.putAll(map);
    }

    /**
     * @see java.util.Map#remove(java.lang.Object)
     */
    public Object remove(final Object key) {
        checkMutability();
        return super.remove(key);
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

    /**
     * Throws an exception if <code>ValueMap</code> is immutable.
     */
    private final void checkMutability() {
        if (immutable) {
            throw new UnsupportedOperationException("Map is immutable");
        }
    }

}
