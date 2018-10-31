/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.api.resource;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Default {@link ValueMap} implementation.
 */
public class DefaultValueMap extends AbstractValueMap {

    private static final long serialVersionUID = 1L;

    /**
     * Delegating map instance.
     */
    private final Map<String, Object> delegatedMap;

    /**
     * Default constructor.
     */
    public DefaultValueMap() {
        this(null);
    }

    /**
     * Constructs with a map to delegate.
     * @param delegatedMap a map to delegate
     */
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
    public Object get(Object name) {
        return delegatedMap.get(name);
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
    public boolean equals(Object o) {
        if (!(o instanceof DefaultValueMap)) {
            return false;
        }

        return Objects.equals(delegatedMap, ((DefaultValueMap) o).delegatedMap);
    }


    @Override
    public int hashCode() {
        return delegatedMap.hashCode();
    }

    /**
     * Returns an unmodifiable view of this <code>ValueMap</code>
     * @return an unmodifiable view of this <code>ValueMap</code>
     */
    public ValueMap toUnmodifiable() {
        return new DefaultValueMap(Collections.unmodifiableMap(delegatedMap));
    }
}
