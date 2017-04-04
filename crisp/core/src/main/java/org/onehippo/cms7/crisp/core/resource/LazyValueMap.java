package org.onehippo.cms7.crisp.core.resource;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.Factory;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.map.LazyMap;
import org.onehippo.cms7.crisp.api.resource.ValueMap;

public class LazyValueMap implements ValueMap {

    public static LazyValueMap decorate(Map<String, Object> map, Factory factory) {
        final Map lazyMap = LazyMap.decorate(map, factory);
        return new LazyValueMap(lazyMap);
    }

    public static LazyValueMap decorate(Map<String, Object> map, Transformer transformer) {
        final Map lazyMap = LazyMap.decorate(map, transformer);
        return new LazyValueMap(lazyMap);
    }

    private final Map<String, Object> lazyMap;

    private LazyValueMap(final Map<String, Object> lazyMap) {
        this.lazyMap = lazyMap;
    }

    @Override
    public int size() {
        return lazyMap.size();
    }

    @Override
    public boolean isEmpty() {
        return lazyMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return lazyMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return lazyMap.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return lazyMap.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return lazyMap.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return lazyMap.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        lazyMap.putAll(m);
    }

    @Override
    public void clear() {
        lazyMap.clear();
    }

    @Override
    public Set<String> keySet() {
        return lazyMap.keySet();
    }

    @Override
    public Collection<Object> values() {
        return lazyMap.values();
    }

    @Override
    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        return lazyMap.entrySet();
    }

    @Override
    public <T> T get(String name, Class<T> type) {
        return (T) lazyMap.get(name);
    }

    @Override
    public <T> T get(String name, T defaultValue) {
        Object value = get(name);
        if (value == null) {
            return defaultValue;
        }
        return (T) value;
    }

}
