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
package org.hippoecm.hst.core.mapping;

import java.util.Iterator;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.caching.Cache;
import org.hippoecm.hst.caching.CacheKey;
import org.hippoecm.hst.caching.CacheManager;
import org.hippoecm.hst.caching.CachedResponse;
import org.hippoecm.hst.caching.CachedResponseImpl;
import org.hippoecm.hst.caching.Event;
import org.hippoecm.hst.caching.EventCacheImpl;
import org.hippoecm.hst.caching.NamedEvent;
import org.hippoecm.hst.caching.validity.AggregatedValidity;
import org.hippoecm.hst.caching.validity.EventValidity;
import org.hippoecm.hst.jcr.ReadOnlyPooledSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class URLMappingManager {

    private static final Logger log = LoggerFactory.getLogger(URLMapping.class);
    private final static int TTL = 60;
   
    
    public static URLMapping getUrlMapping(Session session, HttpServletRequest request, String uriPrefix, String hst_configuration_path,
            int uriLevels) {
        
        String userId = session.getUserID();
        if(userId == null) {
            userId = "anonymous";
        }
        String key = userId+"_"+hst_configuration_path;
        CacheKey cacheKey = new CacheKey(key, URLMapping.class);
        Cache cache = CacheManager.getCache(request, EventCacheImpl.class.getName());
        synchronized (cache) {
            CachedResponse urlMapping = cache.get(cacheKey);
            if(urlMapping != null && urlMapping.getResponse() instanceof URLMapping) {
                log.debug("return found urlmapping for user and context");
                return (URLMapping)urlMapping.getResponse();
            } else {
                log.debug("no urlmapping found for user and context. Create a new one");
                URLMapping newUrlMapping = new URLMappingImpl(session, request.getContextPath(), uriPrefix, hst_configuration_path,uriLevels);
                
                AggregatedValidity aggrVal = new AggregatedValidity();
                Iterator<String> paths = newUrlMapping.getCanonicalPathsConfiguration().iterator();
                while(paths.hasNext()) {
                    aggrVal.add(new EventValidity(new NamedEvent(paths.next())));
                }
                CachedResponse cr = new CachedResponseImpl(aggrVal, newUrlMapping);
                
                if(session instanceof ReadOnlyPooledSession && cache instanceof EventCacheImpl){
                    ReadOnlyPooledSession ropSession = (ReadOnlyPooledSession) session;
                    if(ropSession.getLastRefreshTime() > ((EventCacheImpl)cache).getLastEvictionTime()) {
                        log.debug("Cache new url mapping with key '" + cacheKey + "'");
                        cache.store(cacheKey, cr);
                    } else {
                        log.debug("Do not cache urlmapping with this session, because the session's refresh time is " +
                        		"before the last cache eviction time, so the mapping might be an old result");
                    }
                }
                return newUrlMapping;
            }
        }
    }

}
