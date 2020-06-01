/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.configuration.cache;


import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.IdentityMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * <p>
 *   Note that this class is <strong>not</strong> thread-safe : It should not be accessed by concurrent threads
 * </p>
 */
public class WeakTaggedCache<K, V, U> {

    private static final Logger log = LoggerFactory.getLogger(WeakTaggedCache.class);

    Map<K, WeakReference<V>> keyValueMap = new HashMap<>();
    Map<WeakReference<V>, K> valueKeyMap = new IdentityMap();
    private ReferenceQueue<V> cleanupQueue = new ReferenceQueue<>();
    WeakKeyTagRegistry<U, K> weakKeyTagRegistry = new WeakKeyTagRegistry();

    public void evictKeysByTag(final U tag) {
        try {
            final List<K> evictKeys = weakKeyTagRegistry.get(tag);
            for (K evictKey : evictKeys) {
                final V remove = remove(evictKey);
                if (remove != null) {
                    log.debug("Succesfully removed '{}' from cache BY tag '{}'", remove, tag);
                }
            }
        } catch (Exception e) {
            log.warn("Exception during processing tag '"+tag.toString()+"'. Skip tag.", e);
        }
    }

    public void put(K key, V value, U tag) {
        expungeStaleEntries();
        store(key, value);
        weakKeyTagRegistry.put(tag, key);
    }


    public void put(K key, V value, U[] tags) {
        expungeStaleEntries();
        store(key, value);
        weakKeyTagRegistry.put(tags, key);
    }

    public V get(K key) {
        expungeStaleEntries();
        final WeakReference<V> weakRef = keyValueMap.get(key);
        if (weakRef == null) {
            return null;
        }
        return weakRef.get();
    }

    public V remove(K key) {
        expungeStaleEntries();
        final WeakReference<V> weakRef = keyValueMap.remove(key);
        if (weakRef == null) {
            return null;
        }
        valueKeyMap.remove(weakRef);
        return weakRef.get();
    }


    private void store(final K key, final V value) {
        WeakReference<V> weakCachedObj = new WeakReference<>(value, cleanupQueue);
        keyValueMap.put(key, weakCachedObj);
        valueKeyMap.put(weakCachedObj, key);
    }

    private void expungeStaleEntries() {
        Reference<? extends V> cleaned;
        while ((cleaned = cleanupQueue.poll()) != null) {
            // remove gc-ed weak reference from maps
            K weakRefMapKey = valueKeyMap.remove(cleaned);
            if (weakRefMapKey != null) {
                keyValueMap.remove(weakRefMapKey);
            }
        }
    }

}
