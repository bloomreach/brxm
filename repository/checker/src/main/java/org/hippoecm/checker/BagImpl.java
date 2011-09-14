/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.checker;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

public class BagImpl<K, V> implements Bag<K, V> {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    Map<K, Collection<V>> backingMap;
    Class<? extends Collection<V>> backingValuesClass;

    BagImpl(Map<K, Collection<V>> backingMap, Class<? extends Collection> backingValuesClass) {
        this.backingMap = backingMap;
        this.backingValuesClass = (Class<? extends Collection<V>>) backingValuesClass;
    }

    BagImpl(Map<K, Collection<V>> backingMap, Class<? extends Collection> backingValuesClass, Bag<? extends K, ? extends V> copy) {
        this.backingMap = backingMap;
        this.backingValuesClass = (Class<? extends Collection<V>>) backingValuesClass;
        for(Map.Entry<? extends K,? extends V> item : copy) {
            put(item.getKey(), item.getValue());
        }
    }

    public void put(K k, V v) {
        Collection<V> values = backingMap.get(k);
        if (values == null) {
            try {
                values = backingValuesClass.newInstance();
                backingMap.put(k, values);
            } catch (InstantiationException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
        values.add(v);
    }

    public void addAll(K k, Collection<V> collection) {
        Collection<V> values = backingMap.get(k);
        if (values == null) {
            try {
                values = backingValuesClass.newInstance();
                backingMap.put(k, values);
            } catch (InstantiationException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
        values.addAll(collection);
    }

    public Iterable<? extends V> get(K k) {
        return backingMap.get(k);
    }

    public void remove(K key, V value) {
        Collection<? extends V> values = backingMap.get(key);
        for (Iterator<? extends V> iter = values.iterator(); iter.hasNext(); ) {
            if (iter.next().equals(value)) {
                iter.remove();
                return;
            }
        }
    }

    public void remove(K key) {
        backingMap.remove(key);
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    public int size() {
        return backingMap.size();
    }

    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    public boolean containsKey(Object key) {
        return backingMap.containsKey(key);
    }

    public void clear() {
        backingMap.clear();
    }

    public Set<K> keySet() {
        return backingMap.keySet();
    }

    public Iterator<Entry<K, V>> iterator() {
        final Iterator<Map.Entry<K, Collection<V>>> keyIterator = backingMap.entrySet().iterator();
        return new Iterator<Entry<K, V>>() {
            K key;
            Iterator<V> valueIterator = null;

            public boolean hasNext() {
                while ((valueIterator == null || !valueIterator.hasNext()) && keyIterator.hasNext()) {
                    Map.Entry<K, Collection<V>> entry = keyIterator.next();
                    key = entry.getKey();
                    valueIterator = entry.getValue() != null ? entry.getValue().iterator() : null;
                }
                return valueIterator != null && valueIterator.hasNext();
            }

            public Entry<K, V> next() {
                while ((valueIterator == null || !valueIterator.hasNext()) && keyIterator.hasNext()) {
                    Map.Entry<K, Collection<V>> entry = keyIterator.next();
                    key = entry.getKey();
                    valueIterator = entry.getValue() != null ? entry.getValue().iterator() : null;
                }
                if (valueIterator != null && valueIterator.hasNext()) {
                    final V value = valueIterator.next();
                    return new Map.Entry<K, V>() {

                        public K getKey() {
                            return key;
                        }

                        public V getValue() {
                            return value;
                        }

                        public V setValue(V value) {
                            throw new UnsupportedOperationException();
                        }
                    };
                } else {
                    throw new NoSuchElementException();
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

    public boolean contains(K k, V v) {
        Collection<V> values = backingMap.get(k);
        return values != null && values.contains(v);
    }

    public V getFirst(K k) {
        Collection<V> values = backingMap.get(k);
        if (values != null && !values.isEmpty()) {
            return values.iterator().next();
        } else {
            return null;
        }
    }
}
