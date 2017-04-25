/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.core.resource;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.onehippo.cms7.crisp.api.resource.AbstractValueMap;
import com.onehippo.cms7.crisp.api.resource.ValueMap;

public class EmptyValueMap extends AbstractValueMap {

    private static final long serialVersionUID = 1L;

    private static final ValueMap INSTANCE = new EmptyValueMap();

    public static ValueMap getInstance() {
        return INSTANCE;
    }

    private EmptyValueMap() {
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean containsKey(Object key) {
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public Object get(Object name) {
        return null;
    }

    @Override
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(Object key) {
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
    }

    @Override
    public Set<String> keySet() {
        return Collections.emptySet();
    }

    @Override
    public Collection<Object> values() {
        return Collections.emptyList();
    }

    @Override
    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        return Collections.emptySet();
    }
}