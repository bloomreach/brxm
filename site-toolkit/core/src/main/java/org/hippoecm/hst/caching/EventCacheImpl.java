/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.caching;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.hippoecm.hst.caching.validity.AggregatedValidity;
import org.hippoecm.hst.caching.validity.EventValidity;
import org.hippoecm.hst.caching.validity.SourceValidity;

public class EventCacheImpl extends LRUMemoryCacheImpl implements EventCache {

    private long lastEvictionTime = 0;
    private Map<Event, List<WeakReference<CacheKey>>> eventMap; // This is the event registry
    private long registerCounter;
    private long registerCleanUpLimit;
    
    private long eventsProcessed = 0;
    private long cachekeyEvictions = 0;

    public EventCacheImpl(int size) {
        super(size);
        // heuristic argument: start the eventMap evenly large as the cache size, and cleanup after
        // registerCounter exceeds cache size
        eventMap = new HashMap<Event, List<WeakReference<CacheKey>>>(size);
        registerCleanUpLimit = size;
    }

    @Override
    public void store(CacheKey key, CachedResponse cr) {
        if(!isActive()) {return;}
        // examine the validities for any event validities which should be registered before storing the value.
        examineMineValidities(key, cr.getValidityObjects());
        super.store(key, cr);
    }

    private void examineMineValidities(CacheKey key, SourceValidity[] sourceValidities) {
        for (SourceValidity val : sourceValidities) {
            if (val instanceof AggregatedValidity) {
                AggregatedValidity aggrVal = ((AggregatedValidity) val);
                examineMineValidities(key, aggrVal.getValidities().toArray(
                        new SourceValidity[aggrVal.getValidities().size()]));
            }
            if (val instanceof EventValidity) {
                this.register(key, ((EventValidity) val).getEvent());
            }
        }

    }

    public void processEvent(Event event) {
        if(!isActive()) {return;}
        eventsProcessed++;
        this.lastEvictionTime = System.currentTimeMillis();
        synchronized (this) {
            List<WeakReference<CacheKey>> objects = eventMap.get(event);
            if (objects != null) {
                for (WeakReference<CacheKey> weakKey : objects) {
                    CacheKey key = weakKey.get();
                    if (key != null) {
                        this.remove(key);
                        cachekeyEvictions++;
                    }
                }
                eventMap.remove(event);
            }
        }
    }

    private void register(CacheKey object, Event eventKey) {
        if(!isActive()) {return;}
        /*
         * The event registry maps an event --> {key1, key2, key3}.
         * 
         * When an event arrives, all entries with key1, key2, key3 are evicted from this cache.
         * For memory reason, the CacheKey's are hold in WeakReferences. 
         */
        registerCounter++;
        synchronized (eventMap) {
            List<WeakReference<CacheKey>> objects = eventMap.get(eventKey);
            if (objects == null) {
                objects = new ArrayList<WeakReference<CacheKey>>();
                objects.add(new WeakReference<CacheKey>(object));
                eventMap.put(eventKey, objects);
                return;
            } else {
                objects.add(new WeakReference<CacheKey>(object));
            }
        }
        if (registerCounter > registerCleanUpLimit) {
            synchronized (eventMap) {
                registerCounter = 0;
                Set<Entry<Event, List<WeakReference<CacheKey>>>> entries = eventMap.entrySet();
                Iterator<Entry<Event, List<WeakReference<CacheKey>>>> entriesIt = entries.iterator();
                List<Event> removeFromMap = new ArrayList<Event>();
                while (entriesIt.hasNext()) {
                    Entry<Event, List<WeakReference<CacheKey>>> entry = entriesIt.next();
                    List<WeakReference<CacheKey>> weakRefs = entry.getValue();
                    List<WeakReference<CacheKey>> tobeRemoved = new ArrayList<WeakReference<CacheKey>>(); 
                    Iterator<WeakReference<CacheKey>> weakRefsIt = weakRefs.iterator();
                    Boolean removeEntry = true;
                    while(weakRefsIt.hasNext()) {
                        WeakReference<CacheKey> weakRef = weakRefsIt.next();
                        if(weakRef.get() == null) {
                            // weakRef is garbage collected, though still need to remove the null WeakRef from the map
                            tobeRemoved.add(weakRef);
                        } else {
                            // when at least one non null weakref is found, keep the entry
                            removeEntry = false;
                        }
                    }
                    if(removeEntry) {
                        // remove entire entry
                        removeFromMap.add(entry.getKey());  
                    } else {
                        // remove the null WeakRefs
                        weakRefs.removeAll(tobeRemoved);
                    }
                }
                Iterator<Event> removeIt = removeFromMap.iterator();
                while(removeIt.hasNext()) {
                    eventMap.remove(removeIt.next());
                }
            }
        }
    }

    public long getLastEvictionTime() {
        return lastEvictionTime;
    }

    public Map<String, String> getStatistics() {
        Map<String, String> stats = super.getStatistics();
        synchronized(this) {
            stats.put("Registry size ", String.valueOf(this.eventMap.size()));
            stats.put("Last evition time ", String.valueOf(getLastEvictionTime()));
            stats.put("Processed events ", String.valueOf(this.eventsProcessed));
            stats.put("Registered events ", String.valueOf(this.eventMap.size()));
            stats.put("Number of keys evicted by events ", String.valueOf(this.cachekeyEvictions));
        }
        return stats;
    }
}
