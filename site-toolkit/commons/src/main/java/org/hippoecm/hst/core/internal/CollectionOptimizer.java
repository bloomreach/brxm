/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CollectionOptimizer {

    private CollectionOptimizer() {}

    /**
     * @param map the map to optimize, not allowed to be <code>null</code>
     * @return an immutable optimized map
     */
    public static <K,V> Map<K,V> optimizeHashMap(Map<K,V> map) {
        if (map.isEmpty()) {
            return Collections.emptyMap();
        } else if (map.size() == 1) {
            Map.Entry<K,V> entry = map.entrySet().iterator().next();
            return Collections.singletonMap(entry.getKey(), entry.getValue());
        } else if (map.size() < 6) {
            Map<K,V> newMap = new HashMap<K, V>(map.size() * 4 / 3);
            newMap.putAll(map);
            map = newMap;
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * @param map the map to optimize, not allowed to be <code>null</code>
     * @return an immutable optimized linked hashmap
     */
    public static <K,V> Map<K,V> optimizeLinkedHashMap(Map<K,V> map) {
        if (map.isEmpty()) {
            return Collections.emptyMap();
        } else if (map.size() == 1) {
            Map.Entry<K,V> entry = map.entrySet().iterator().next();
            return Collections.singletonMap(entry.getKey(), entry.getValue());
        } else if (map.size() < 6) {
            Map<K,V> newMap = new LinkedHashMap<K, V>(map.size() * 4 / 3);
            newMap.putAll(map);
            map = newMap;
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * @param set the set to optimize, not allowed to be <code>null</code>
     * @return an immutable optimized set
     */
    public static <V> Set<V> optimizeHashSet(Set<V> set) {
        if (set.isEmpty()) {
            return Collections.emptySet();
        } else if (set.size() == 1) {
            return Collections.singleton(set.iterator().next());
        } else if (set.size() < 6) {
            Set<V> newSet = new HashSet<V>(set.size() * 4 / 3);
            newSet.addAll(set);
            set = newSet;
        }
        return Collections.unmodifiableSet(set);
    }

    /**
     * @param list the list to optimize, not allowed to be <code>null</code>
     * @return an immutable optimized list
     */
    public static <E> List<E> optimizeArrayList(List<E> list) {
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        ((ArrayList<E>)list).trimToSize();
        return Collections.unmodifiableList(list);
    }


}
