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
package org.hippoecm.hst.mock.core.container;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hippoecm.hst.core.container.ContainerConfiguration;

/**
 * Mock implementation of (@link ContainerConfiguration).
 */
public class MockContainerConfiguration implements ContainerConfiguration {

    private Map<Class<?>, Map<String, Object>> properties = new HashMap<Class<?>, Map<String, Object>>();

    public boolean isEmpty() {
        return properties.isEmpty();
    }

    public boolean containsKey(String key) {
        for (Class<?> aClass : properties.keySet()) {
            if (properties.get(aClass).containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    public boolean getBoolean(String key) {
        Boolean value = getValue(Boolean.class, key);
        return value;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return getValue(Boolean.class, key, defaultValue);
    }

    public String getString(String key) {
        return getValue(String.class, key);
    }

    public String getString(String key, String defaultValue) {
        return getValue(String.class, defaultValue);
    }

    public double getDouble(String key) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public double getDouble(String key, double defaultValue) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public float getFloat(String key) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public float getFloat(String key, float defaultValue) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public int getInt(String key) {
        return this.<Integer>getValue(Integer.class, key);
    }

    public int getInt(String key, int defaultValue) {
        return getValue(Integer.class, key, new Integer(defaultValue));
    }

    public List getList(String key) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public long getLong(String key) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public long getLong(String key, long defaultValue) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String[] getStringArray(String key) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Iterator getKeys() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean isDevelopmentMode() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Properties toProperties() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void setProperty(Class<?> valueClass, String key, Object value) {
        Map<String, Object> keysToValues = properties.get(valueClass);
        if (keysToValues == null) {
            keysToValues = new LinkedHashMap<String, Object>();
            properties.put(valueClass, keysToValues);
        }
        keysToValues.put(key, value);
    }

    private <T> T getValue(Class<?> valueClass, String key, T defaultValue) {
        T value = this.<T>getValue(valueClass, key);
        return value == null ? defaultValue : value;
    }

    private <T> T getValue(Class<?> valueClass, String key) {
        Map<String, Object> map = properties.get(valueClass);
        if (map == null) {
            return null;
        }
        //noinspection unchecked
        return (T) map.get(key);
    }
}
