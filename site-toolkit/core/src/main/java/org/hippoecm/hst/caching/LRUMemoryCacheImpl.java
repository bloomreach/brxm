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

import org.apache.commons.collections.map.LRUMap;
import org.hippoecm.hst.caching.validity.SourceValidity;

public class LRUMemoryCacheImpl implements Cache{

    private LRUMap cache;
   
    public LRUMemoryCacheImpl(int size) {
        this.cache = new LRUMap(size);
    }
    
    public void clear() {
        cache.clear();
    }

    public boolean containsKey(Object key) {
        CachedResponse cachedResponse =null ;
        synchronized (cache) {
            cachedResponse = (CachedResponse)this.cache.get(key);
        }
        
        if(cachedResponse == null) {
            return false;
        }
        
        // check the source validities which are possible to check. Only when 0 is returned, you still are not sure whether the cached
        // value is still valid
        return checkValdidity(cachedResponse, key);
       
    }

    public CachedResponse get(Object key) {
        CachedResponse cachedResponse =null ;
        synchronized (cache) {
            cachedResponse = (CachedResponse)this.cache.get(key);
        }
        if(cachedResponse == null) {
            return null;
        }
        if(checkValdidity(cachedResponse, key)) {
            return cachedResponse;
        }
        return null;
    }

    public void remove(Object key) {
        cache.remove(key);
    }

    public void store(Object key, CachedResponse value) {
        cache.put(key, value);
    }
    
    private boolean checkValdidity(CachedResponse cachedResponse, Object key) {
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

}
