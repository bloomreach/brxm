/*
 *  Copyright 2008-2023 Bloomreach
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.StringValueConversionException;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.util.DurationUtils;

public abstract class AbstractValueMap extends AbstractMap<String, Object> implements IValueMap {

    private boolean immutable = false;

    @Override
    public boolean isImmutable() {
        return immutable;
    }

    @Override
    public IValueMap makeImmutable() {
        return null;
    }

    @Override
    public void clear() {
        checkMutability();
        super.clear();
    }

    /**
     * @see IValueMap#getBoolean(String)
     */
    @Override
    public boolean getBoolean(final String key) throws StringValueConversionException {
        return getStringValue(key).toBoolean();
    }

    /**
     * @see IValueMap#getDouble(String)
     */
    @Override
    public double getDouble(final String key) throws StringValueConversionException {
        return getStringValue(key).toDouble();
    }

    /**
     * @see IValueMap#getDouble(String, double)
     */
    @Override
    public double getDouble(String key, double defaultValue) throws StringValueConversionException {
        return getStringValue(key).toDouble(defaultValue);
    }

    /**
     * @see IValueMap#getDuration(String)
     */
    @Override
    public Duration getDuration(String key) throws StringValueConversionException {
        return DurationUtils.parse(getString(key));
    }

    /**
     * @see IValueMap#getInt(String)
     */
    @Override
    public int getInt(String key) throws StringValueConversionException {
        return getStringValue(key).toInt();
    }

    /**
     * @see IValueMap#getInt(String, int)
     */
    @Override
    public int getInt(String key, int defaultValue) throws StringValueConversionException {
        return getStringValue(key).toInt(defaultValue);
    }

    /**
     * @see IValueMap#getLong(String)
     */
    @Override
    public long getLong(String key) throws StringValueConversionException {
        return getStringValue(key).toLong();
    }

    /**
     * @see IValueMap#getLong(String, long)
     */
    @Override
    public long getLong(String key, long defaultValue) throws StringValueConversionException {
        return getStringValue(key).toLong(defaultValue);
    }

    /**
     * @see IValueMap#getString(String, String)
     */
    @Override
    public String getString(String key, String defaultValue) {
        String value = getString(key);
        return value != null ? value : defaultValue;
    }

    /**
     * @see IValueMap#getString(String)
     */
    @Override
    public String getString(String key) {
        Object o = get(key);
        if (o == null) {
            return null;
        } else if (o.getClass().isArray() && Array.getLength(o) > 0) {
            // if it is an array just get the first value
            Object arrayValue = Array.get(o, 0);
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
    @Override
    public CharSequence getCharSequence(String key) {
        Object o = get(key);
        if (o == null) {
            return null;
        } else if (o.getClass().isArray() && Array.getLength(o) > 0) {
            // if it is an array just get the first value
            Object arrayValue = Array.get(o, 0);
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
    @Override
    public String[] getStringArray(String key) {
        Object o = get(key);
        if (o == null) {
            return null;
        } else if (o instanceof String[]) {
            return (String[]) o;
        } else if (o.getClass().isArray()) {
            int length = Array.getLength(o);
            String[] array = new String[length];
            for (int i = 0; i < length; i++) {
                Object arrayValue = Array.get(o, i);
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
    @Override
    public StringValue getStringValue(String key) {
        return StringValue.valueOf(getString(key));
    }

    /**
     * @see IValueMap#getInstant(String)
     */
    @Override
    public Instant getInstant(String key) throws StringValueConversionException {
        return getStringValue(key).toInstant();
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
    public Object add(String key, String value) {
        checkMutability();
        Object o = get(key);
        if (o == null) {
            return put(key, value);
        } else if (o.getClass().isArray()) {
            int length = Array.getLength(o);
            String[] destArray = new String[length + 1];
            for (int i = 0; i < length; i++) {
                Object arrayValue = Array.get(o, i);
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

    /**
     * @see java.util.Map#putAll(java.util.Map)
     */
    @Override
    public void putAll(Map<? extends String, ? extends Object> map) {
        checkMutability();
        super.putAll(map);
    }

    /**
     * @see java.util.Map#remove(java.lang.Object)
     */
    @Override
    public Object remove(Object key) {
        checkMutability();
        return super.remove(key);
    }

    @Override
    public String getKey(String key) {
        for (final String keyString : keySet()) {
            if (key.equalsIgnoreCase(keyString)) {
                return keyString;
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
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        for (Iterator<Map.Entry<String, Object>> iterator = entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, Object> entry = iterator.next();
            buffer.append(entry.getKey());
            buffer.append(" = \"");
            Object value = entry.getValue();
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
    private void checkMutability() {
        if (immutable) {
            throw new UnsupportedOperationException("Map is immutable");
        }
    }

    // //
    // // getAs convenience methods
    // //

    /**
     * @see IValueMap#getAsBoolean(String)
     * 
     */
    @Override
    public Boolean getAsBoolean(String key) {
        if (!containsKey(key)) {
            return null;
        }

        try {
            return getBoolean(key);
        } catch (StringValueConversionException ignored) {
            return null;
        }
    }

    /**
     * @see IValueMap#getAsBoolean(String, boolean)
     * 
     */
    @Override
    public boolean getAsBoolean(String key, boolean defaultValue) {
        if (!containsKey(key)) {
            return defaultValue;    
        }

        try {
            return getBoolean(key);
        } catch (StringValueConversionException ignored) {
            return defaultValue;
        }
    }

    /**
     * @see IValueMap#getAsInteger(String)
     */
    @Override
    public Integer getAsInteger(String key) {
        if (!containsKey(key)) {
            return null;
        }

        try {
            return getInt(key);
        } catch (StringValueConversionException ignored) {
            return null;
        }
    }

    /**
     * @see IValueMap#getAsInteger(String, int)
     */
    @Override
    public int getAsInteger(String key, int defaultValue) {
        try {
            return getInt(key, defaultValue);
        } catch (StringValueConversionException ignored) {
            return defaultValue;
        }
    }

    /**
     * @see IValueMap#getAsLong(String)
     */
    @Override
    public Long getAsLong(String key) {
        if (!containsKey(key)) {
            return null;
        }

        try {
            return getLong(key);
        } catch (StringValueConversionException ignored) {
            return null;
        }
    }

    /**
     * @see IValueMap#getAsLong(String, long)
     */
    @Override
    public long getAsLong(String key, long defaultValue) {
        try {
            return getLong(key, defaultValue);
        } catch (StringValueConversionException ignored) {
            return defaultValue;
        }
    }

    /**
     * @see IValueMap#getAsDouble(String)
     */
    @Override
    public Double getAsDouble(String key) {
        if (!containsKey(key)) {
            return null;
        }

        try {
            return getDouble(key);
        } catch (StringValueConversionException ignored) {
            return null;
        }
    }

    /**
     * @see IValueMap#getAsDouble(String, double)
     */
    @Override
    public double getAsDouble(String key, double defaultValue) {
        try {
            return getDouble(key, defaultValue);
        } catch (StringValueConversionException ignored) {
            return defaultValue;
        }
    }

    /**
     * @see IValueMap#getAsDuration(String)
     */
    @Override
    public Duration getAsDuration(String key) {
        return getAsDuration(key, null);
    }

    /**
     * @see IValueMap#getAsDuration(String, Duration)
     */
    @Override
    public Duration getAsDuration(String key, Duration defaultValue) {
        if (!containsKey(key)) {
            return defaultValue;
        }

        try {
            return getDuration(key);
        } catch (StringValueConversionException ignored) {
            return defaultValue;
        }
    }

    /**
     * @see IValueMap#getAsInstant(String)
     */
    @Override
    public Instant getAsInstant(String key) {
        return getAsTime(key, null);
    }

    /**
     * @see IValueMap#getAsTime(String, Instant)
     */
    @Override
    public Instant getAsTime(String key, Instant defaultValue) {
        if (!containsKey(key)) {
            return defaultValue;
        }

        try {
            return getInstant(key);
        } catch (StringValueConversionException ignored) {
            return defaultValue;
        }
    }

    /**
     * @see org.apache.wicket.util.value.IValueMap#getAsEnum(java.lang.String, java.lang.Class)
     */
    @Override
    public <T extends Enum<T>> T getAsEnum(String key, Class<T> eClass) {
        return getEnumImpl(key, eClass, (T) null);
    }

    /**
     * @see org.apache.wicket.util.value.IValueMap#getAsEnum(java.lang.String, java.lang.Enum)
     */
    @Override
    public <T extends Enum<T>> T getAsEnum(String key, T defaultValue) {
        if (defaultValue == null) {
            throw new IllegalArgumentException("Default value cannot be null");
        }

        return getEnumImpl(key, defaultValue.getClass(), defaultValue);
    }

    /**
     * @see org.apache.wicket.util.value.IValueMap#getAsEnum(java.lang.String, java.lang.Class,
     *      java.lang.Enum)
     */
    @Override
    public <T extends Enum<T>> T getAsEnum(String key, Class<T> eClass, T defaultValue) {
        return getEnumImpl(key, eClass, defaultValue);
    }

    /**
     * get enum implementation
     * 
     * @param key
     * @param eClass
     * @param defaultValue
     * @param <T>
     * @return Enum
     */
    @SuppressWarnings( { "unchecked" })
    private <T extends Enum<T>> T getEnumImpl(String key, Class<?> eClass, T defaultValue) {
        if (eClass == null) {
            throw new IllegalArgumentException("eClass value cannot be null");
        }

        String value = getString(key);
        if (value == null) {
            return defaultValue;
        }

        Method valueOf;
        try {
            valueOf = eClass.getMethod("valueOf", String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not find method valueOf(String s) for " + eClass.getName(), e);
        }

        try {
            return (T) valueOf.invoke(eClass, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not invoke method valueOf(String s) on " + eClass.getName(), e);
        } catch (InvocationTargetException e) {
            // IllegalArgumentException thrown if enum isn't defined - just return default
            if (e.getCause() instanceof IllegalArgumentException) {
                return defaultValue;
            }
            throw new RuntimeException(e); // shouldn't happen
        }
    }

}
