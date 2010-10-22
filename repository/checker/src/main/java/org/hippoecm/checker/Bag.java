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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public interface Bag<K, V> extends Iterable<Map.Entry<K, V>> {
    @SuppressWarnings("unused")
    final static String SVN_ID = "$Id$";

    public void put(K k, V v);

    public void addAll(K k, Collection<V> collection);

    public Iterable<? extends V> get(K k);

    public V getFirst(K k);

    public void remove(K k);

    public void remove(K k, V v);

    public int size();

    public boolean isEmpty();

    public boolean contains(K key, V value);

    public boolean containsKey(K key);

    public void clear();

    public Set<K> keySet();
}
