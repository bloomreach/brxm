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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.jcr.JcrSessionPoolManager;

public class CacheManager {
 
    private static final String APPLICATION_CACHES = "org.hippoecm.hst.caching.applicationcache";
    
    public static final Map<String,Cache> caches = new HashMap<String, Cache>();
    private static boolean isApplicationScopeSet = false;
    
    public static Cache getCache(PageContext ctx) {
        if(!isApplicationScopeSet) {
            ctx.setAttribute(APPLICATION_CACHES, caches);
            isApplicationScopeSet = true;
        }
        
        HttpServletRequest request = (HttpServletRequest)ctx.getRequest();
        return getCache(request);
    }
    
    public static Cache getCache(HttpServletRequest request) {
        Session session = JcrSessionPoolManager.getSession(request);
        String cacheName = session.getUserID();
        if(cacheName == null) {
            cacheName = "anonymous";
        }
        synchronized(caches){
            Cache cache = caches.get(cacheName);
            if( cache != null) {
                return cache;
            } else {
                cache = new LRUMemoryCacheImpl(1000);
                caches.put(cacheName, cache);
                return cache;
            }
        }
        
    }

   
}
