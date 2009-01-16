/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.repository.proxyrepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class PositionMap<K, V> implements Map<K, V> {
    LinkedHashMap<K, Integer> forward;
    ArrayList<V> index;

    public PositionMap() {
        forward = new LinkedHashMap<K, Integer>();
        index = new ArrayList<V>();
    }

    public Set<Map.Entry<K, V>> entrySet() {
        return null;
    }

    public Set<K> keySet() {
        return forward.keySet();
    }

    public Collection<V> values() {
        return index;
    }

    public void clear() {
    }

    public void putAll(Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public boolean isEmpty() {
        return index.isEmpty();
    }

    public int size() {
        return index.size();
    }

    public V put(K key, V value) {
        forward.put(key, new Integer(index.size()));
        index.add(value);
        return null;
    }

    public boolean containsValue(Object value) {
        return index.contains(value);
    }

    public boolean containsKey(Object value) {
        return forward.containsKey(value);
    }

    public V remove(Object key) {
        return null;
    }

    public V get(Object key) {
        return index.get(forward.get(key).intValue());
    }

    public int indexOf(K key) {
        return forward.get(key).intValue();
    }

    public int reverseIndexOf(V value) {
        return indexOf(reverseGet(value));
    }

    public K reverseGet(V value) {
        int i = 0;
        for(V entry : index) {
            if(entry == value)
                break;
            else
                ++i;
        }
        for(Map.Entry<K,Integer> entry : forward.entrySet()) {
            if(entry.getValue().intValue() == i)
                return entry.getKey();
        }
        return null;
    }

    public V get(int position) {
        if (position < index.size())
            return index.get(position);
        else
            return null;
    }
}