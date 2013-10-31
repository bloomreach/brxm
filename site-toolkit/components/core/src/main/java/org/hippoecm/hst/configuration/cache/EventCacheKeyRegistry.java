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
public class EventCacheKeyRegistry<Event, CacheKey> {

    private static final Logger log = LoggerFactory.getLogger(EventCacheKeyRegistry.class);

    final Map<Event, List<WeakReference<CacheKey>>> eventCacheKeysMap = new HashMap<>();
    final Map<WeakReference<CacheKey>, Event> cacheKeyEventMap = new IdentityMap();
    private final ReferenceQueue<CacheKey> cleanupQueue = new ReferenceQueue<>();

    public void put(Event key, CacheKey value) {
        put((Event[])new Object[]{key}, value);
    }

    public void put(Event[] events, CacheKey value) {
        WeakReference<CacheKey> weakCacheKey = new WeakReference<>(value, cleanupQueue);
        for (Event event : events) {
            log.debug("Register event '{}' to cachekey '{}'", event, value);
            List<WeakReference<CacheKey>> cacheKeys = eventCacheKeysMap.get(event);
            if (cacheKeys == null) {
                cacheKeys = new ArrayList<>();
                cacheKeys.add(weakCacheKey);
                eventCacheKeysMap.put(event, cacheKeys);
            } else {
                cacheKeys.add(weakCacheKey);
            }
            cacheKeyEventMap.put(weakCacheKey, event);
        }
        cleanup();
    }

    public List<CacheKey> get(Event event) {
        cleanup();
        final List<WeakReference<CacheKey>> weakReferences = eventCacheKeysMap.get(event);
        if (weakReferences == null) {
            return Collections.emptyList();
        }
        List<CacheKey> result = new ArrayList<>(weakReferences.size());
        for (WeakReference<CacheKey> weakReference : weakReferences) {
            final CacheKey v = weakReference.get();
            if (v != null) {
                result.add(v);
            }
        }
        return result;
    }

    private void cleanup() {
        Reference<? extends CacheKey> garbaged;
        while ((garbaged = cleanupQueue.poll()) != null) {
            // remove gc-ed weak reference from maps
            Event event = cacheKeyEventMap.remove(garbaged);
            if (event != null) {
                List<WeakReference<CacheKey>> list = eventCacheKeysMap.get(event);
                if (list != null) {
                    list.remove(garbaged);
                    if (list.isEmpty()) {
                        eventCacheKeysMap.remove(event);
                    }
                }
            }
        }
    }

}
