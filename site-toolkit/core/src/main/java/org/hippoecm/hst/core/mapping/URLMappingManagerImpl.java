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

import org.hippoecm.hst.caching.Cache;
import org.hippoecm.hst.caching.CacheKey;
import org.hippoecm.hst.caching.CacheManagerImpl;
import org.hippoecm.hst.caching.CachedResponse;
import org.hippoecm.hst.caching.CachedResponseImpl;
import org.hippoecm.hst.caching.EventCacheImpl;
import org.hippoecm.hst.caching.NamedEvent;
import org.hippoecm.hst.caching.validity.AggregatedValidity;
import org.hippoecm.hst.caching.validity.EventValidity;
import org.hippoecm.hst.core.filters.base.HstRequestContext;
import org.hippoecm.hst.core.filters.domain.DomainMapping;
import org.hippoecm.hst.core.filters.domain.RepositoryMapping;
import org.hippoecm.hst.jcr.JcrSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class URLMappingManagerImpl implements URLMappingManager {

    private static final Logger log = LoggerFactory.getLogger(URLMapping.class);
    private JcrSessionFactory jcrSessionFactory;
    
    public URLMappingManagerImpl(JcrSessionFactory jcrSessionFactory) {
       this.jcrSessionFactory = jcrSessionFactory;
    }

    public URLMapping getUrlMapping(HstRequestContext hstRequestContext) throws URLMappingException {
       return this.getUrlMapping(hstRequestContext.getRepositoryMapping(), hstRequestContext.getURLMappingManager(),hstRequestContext.getJcrSession());
    }

    public URLMapping getUrlMapping(RepositoryMapping repositoryMapping, URLMappingManager urlMappingManager, Session jcrSession) throws URLMappingException{
        String userId = jcrSession.getUserID();
        if(userId == null) {
            userId = "anonymous";
        }
        String key = userId+"_"+repositoryMapping.getHstConfigPath() +"_"+repositoryMapping.getDomain().getPattern();
        CacheKey cacheKey = new CacheKey(key, URLMappingImpl.class);
        
        // TODO do not access through static but through the CacheManagerImpl instance
        Cache cache = CacheManagerImpl.getCache(userId, EventCacheImpl.class.getName());
        
        CachedResponse urlMapping = cache.get(cacheKey);
        if(urlMapping != null && urlMapping.getResponse() instanceof URLMapping) {
            log.debug("return found urlmapping for user and context");
            return (URLMapping)urlMapping.getResponse();
        } else {
            log.debug("no urlmapping found for user and context. Create a new one with a clean session");
            Session session = null;
            URLMapping newUrlMapping = null;
            try {
                session = jcrSessionFactory.getSession();
                newUrlMapping = new URLMappingImpl(repositoryMapping, urlMappingManager, session);
                AggregatedValidity aggrVal = new AggregatedValidity();
                Iterator<String> paths = newUrlMapping.getCanonicalPathsConfiguration().iterator();
                while(paths.hasNext()) {
                    aggrVal.add(new EventValidity(new NamedEvent(paths.next())));
                }
                CachedResponse cr = new CachedResponseImpl(aggrVal, newUrlMapping);
                cache.store(cacheKey, cr);
            } finally {
                if(session != null) {
                session.logout();
                }
            }
            
            return newUrlMapping;
        }
    }
}
