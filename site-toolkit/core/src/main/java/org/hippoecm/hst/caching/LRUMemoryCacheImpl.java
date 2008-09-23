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

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;
import org.hippoecm.hst.caching.validity.SourceValidity;

public class LRUMemoryCacheImpl implements Cache{

    private LRUMap cache;
    private boolean active = true;
    
    // stats
    private long misses = 0;
    private long hits = 0;
    private long puts = 0;
    
    public LRUMemoryCacheImpl(int size) {
        this.cache = new LRUMap(size);
    }
    
    public void clear() {
        synchronized(cache) {
            cache.clear();
        }
    }

    public boolean containsKey(CacheKey key) {
        if(!active) {return false;}
        CachedResponse cachedResponse =null ;
        synchronized (cache) {
            cachedResponse = (CachedResponse)this.cache.get(key);
        }
        
        if(cachedResponse == null) {
            misses++;
            return false;
        }
        
        // check the source validities which are possible to check. Only when 0 is returned, you still are not sure whether the cached
        // value is still valid
        boolean val = checkValdidity(cachedResponse, key);
        if(val) {
            hits++;
        } else {
            misses++;
        }
        return val;
       
    }

    public CachedResponse get(CacheKey key) {
        if(!active) {return null;}
        CachedResponse cachedResponse =null ;
        synchronized (cache) {
            cachedResponse = (CachedResponse)this.cache.get(key);
        }
        if(cachedResponse == null) {
            misses++;
            return null;
        }
        if(checkValdidity(cachedResponse, key)) {
            hits++;
            return cachedResponse;
        }
        misses++;
        return null;
    }

    public void remove(CacheKey key) {
        if(!active) {return;}
        cache.remove(key);
    }

    public void store(CacheKey key, CachedResponse cr) {
        if(!active) {return;}
        puts++;
        cache.put(key, cr);
    }
    
    protected boolean checkValdidity(CachedResponse cachedResponse, CacheKey key) {
        if(!active) {return false;}
        if( cachedResponse == null){
            return false;
        }
        SourceValidity[] sourceValidities = cachedResponse.getValidityObjects();
        if(sourceValidities == null) {
            synchronized (cache) {
                this.cache.remove(key);
            }
            return false;
        } else {
            boolean isValid = true;
            for(SourceValidity sourceVal : sourceValidities) {
                int valid = sourceVal.isValid();
                switch (valid) {
                case 1 : break;
                case 0 : break;
                case -1: 
                    isValid = false;
                    break;
                }
            }
            if(!isValid) {
                synchronized (cache) {
                    this.cache.remove(key);
                }
            }
            return isValid;
        }
    }

    public Map<String, String> getStatistics() {
        
        Map<String, String> stats = new LinkedHashMap<String, String>();
        synchronized(this) {
            stats.put("Type ", this.getClass().getSimpleName());
            stats.put("Max size ", String.valueOf(cache.maxSize()));
            stats.put("Current size ", String.valueOf(cache.size()));
            stats.put("Cache put" , String.valueOf(puts));
            stats.put("Cache hits" , String.valueOf(hits));
            stats.put("Cache misses" , String.valueOf(misses));
            String ratio = String.valueOf((hits*100)/(misses+hits+0.00000001));
            if(ratio.length() > 2) {
                ratio = ratio.substring(0,2);
            }
            stats.put("Hit ratio" , ratio.concat(" %"));
        }
        return stats;
    }

    public void setActive(boolean active) {
       this.active = active;
    }

    public boolean isActive() {
        return active;
    }


}
