/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.core.resource;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.onehippo.cms7.crisp.api.resource.ValueMap;

public class DefaultValueMap implements ValueMap {

    private static final long serialVersionUID = 1L;

    private final Map<String, Object> delegatedMap;

    public DefaultValueMap() {
        this(null);
    }

    public DefaultValueMap(Map<String, Object> delegatedMap) {
        if (delegatedMap == null) {
            delegatedMap = new LinkedHashMap<>();
        }

        this.delegatedMap = delegatedMap;
    }

    @Override
    public void clear() {
        delegatedMap.clear();
    }

    @Override
    public Object put(String key, Object value) {
        return (delegatedMap.put(key, value));
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> map) {
        delegatedMap.putAll(map);
    }

    @Override
    public Object remove(Object key) {
        return (delegatedMap.remove(key));
    }

    @Override
    public int size() {
        return delegatedMap.size();
    }

    @Override
    public boolean isEmpty() {
        return delegatedMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return delegatedMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegatedMap.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return delegatedMap.get(key);
    }

    @Override
    public Set<String> keySet() {
        return delegatedMap.keySet();
    }

    @Override
    public Collection<Object> values() {
        return delegatedMap.values();
    }

    @Override
    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        return delegatedMap.entrySet();
    }

    @Override
    public <T> T get(String name, Class<T> type) {
        return (T) get(name);
    }

    @Override
    public <T> T get(String name, T defaultValue) {
        if (containsKey(name)) {
            return (T) get(name);
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof DefaultValueMap)) {
            return false;
        }

        return Objects.equals(delegatedMap, ((DefaultValueMap) o).delegatedMap);
    }

    @Override
    public int hashCode() {
        return delegatedMap.hashCode();
    }

    public ValueMap toUnmodifiable() {
        return new DefaultValueMap(Collections.unmodifiableMap(delegatedMap));
    }
}
