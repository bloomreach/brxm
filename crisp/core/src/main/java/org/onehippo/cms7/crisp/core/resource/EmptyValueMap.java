/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.core.resource;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.onehippo.cms7.crisp.api.resource.AbstractValueMap;
import org.onehippo.cms7.crisp.api.resource.ValueMap;

/**
 * An empty, immutable {@link ValueMap} implementation.
 */
public class EmptyValueMap extends AbstractValueMap {

    private static final long serialVersionUID = 1L;

    /**
     * Singleton immutable {@link EmptyValueMap} instance.
     */
    private static final ValueMap INSTANCE = new EmptyValueMap();

    /**
     * Returns the singleton immutable {@link EmptyValueMap} instance.
     * @return the singleton immutable {@link EmptyValueMap} instance
     */
    public static ValueMap getInstance() {
        return INSTANCE;
    }

    /**
     * Default constructor.
     */
    private EmptyValueMap() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(Object key) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(Object name) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object remove(Object key) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> keySet() {
        return Collections.emptySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Object> values() {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        return Collections.emptySet();
    }
}