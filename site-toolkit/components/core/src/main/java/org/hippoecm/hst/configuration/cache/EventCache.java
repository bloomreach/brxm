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
public class EventCache<CacheKey, CachedObj, Event> {

    private static final Logger log = LoggerFactory.getLogger(EventCache.class);

    Map<CacheKey, WeakReference<CachedObj>> keyToValueMap = new HashMap<>();
    Map<WeakReference<CachedObj>, CacheKey> valueKeyMap = new IdentityMap();
    private ReferenceQueue<CachedObj> cleanupQueue = new ReferenceQueue<>();
    EventCacheKeyRegistry<Event, CacheKey> eventCacheKeyRegistry = new EventCacheKeyRegistry();

    public void handleEvent(final Event event) {
        try {
            final List<CacheKey> evictKeys = eventCacheKeyRegistry.get(event);
            for (CacheKey evictKey : evictKeys) {
                final CachedObj remove = remove(evictKey);
                if (remove != null) {
                    log.debug("Succesfully removed '{}' from cache BY event '{}'", remove, event);
                }
            }
        } catch (Exception e) {
            log.warn("Exception during processing event '"+event.toString()+"'. Skip event.", e);
        }
    }

    public void put(CacheKey key, CachedObj value, Event event) {
        expungeStaleEntries();
        store(key, value);
        eventCacheKeyRegistry.put(event, key);
    }


    public void put(CacheKey key, CachedObj value, Event[] events) {
        expungeStaleEntries();
        store(key, value);
        eventCacheKeyRegistry.put(events, key);
    }

    public CachedObj get(CacheKey key) {

        expungeStaleEntries();;
        final WeakReference<CachedObj> weakRef = keyToValueMap.get(key);
        if (weakRef == null) {
            return null;
        }
        return weakRef.get();
    }

    public CachedObj remove (CacheKey key) {
        expungeStaleEntries();
        final WeakReference<CachedObj> weakRef = keyToValueMap.remove(key);
        if (weakRef == null) {
            return null;
        }
        valueKeyMap.remove(weakRef);
        return weakRef.get();
    }


    private void store(final CacheKey key, final CachedObj value) {
        WeakReference<CachedObj> weakCachedObj = new WeakReference<>(value, cleanupQueue);
        keyToValueMap.put(key, weakCachedObj);
        valueKeyMap.put(weakCachedObj, key);
    }

    private void expungeStaleEntries() {
        Reference<? extends CachedObj> cleaned;
        while ((cleaned = cleanupQueue.poll()) != null) {
            // remove gc-ed weak reference from maps
            CacheKey weakRefMapKey = valueKeyMap.remove(cleaned);
            if (weakRefMapKey != null) {
                keyToValueMap.remove(weakRefMapKey);
            }
        }
    }

}
