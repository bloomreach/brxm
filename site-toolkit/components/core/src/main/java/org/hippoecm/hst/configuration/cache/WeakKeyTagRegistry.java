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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
public class WeakKeyTagRegistry<U, K> {

    private static final Logger log = LoggerFactory.getLogger(WeakKeyTagRegistry.class);

    final Map<U, List<WeakReference<K>>> tagKeysMap = new HashMap<>();
    final Map<WeakReference<K>, List<U>> keyTagsMap = new IdentityMap();
    private final ReferenceQueue<K> cleanupQueue = new ReferenceQueue<>();

    public void put(U tag, K key) {
        put((U[])new Object[]{tag}, key);
    }

    public void put(U[] tags, K key) {
        expungeStaleEntries();
        WeakReference<K> weakCacheKey = new WeakReference<>(key, cleanupQueue);
        for (U tag : tags) {
            log.debug("Register tag '{}' to key '{}'", tag, key);
            List<WeakReference<K>> cacheKeys = tagKeysMap.get(tag);
            if (cacheKeys == null) {
                cacheKeys = new ArrayList<>();
                cacheKeys.add(weakCacheKey);
                tagKeysMap.put(tag, cacheKeys);
            } else {
                cacheKeys.add(weakCacheKey);
            }
        }
        keyTagsMap.put(weakCacheKey, Arrays.asList(tags));
    }

    public List<K> get(U tag) {
        expungeStaleEntries();
        final List<WeakReference<K>> weakReferences = tagKeysMap.get(tag);
        if (weakReferences == null) {
            return Collections.emptyList();
        }
        List<K> result = new ArrayList<>(weakReferences.size());
        for (WeakReference<K> weakReference : weakReferences) {
            final K v = weakReference.get();
            if (v != null) {
                result.add(v);
            }
        }
        return result;
    }

    private void expungeStaleEntries() {
        Reference<? extends K> garbaged;
        while ((garbaged = cleanupQueue.poll()) != null) {
            // remove gc-ed weak reference from maps
            List<U> tags = keyTagsMap.remove(garbaged);
            if (tags != null) {
                for (U tag : tags) {
                    List<WeakReference<K>> list = tagKeysMap.get(tag);
                    if (list != null) {
                        list.remove(garbaged);
                        if (list.isEmpty()) {
                            tagKeysMap.remove(tag);
                        }
                    }
                }
            }
        }
    }

}
