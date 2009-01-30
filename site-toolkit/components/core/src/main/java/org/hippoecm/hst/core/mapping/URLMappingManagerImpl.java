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

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
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
import org.hippoecm.hst.core.domain.RepositoryMapping;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class URLMappingManagerImpl implements URLMappingManager {

    private static final Logger log = LoggerFactory.getLogger(URLMapping.class);
    private Repository repository;
    
    public URLMappingManagerImpl(Repository repository) {
       this.repository = repository;
    }

    public URLMapping getUrlMapping(HstRequestContext hstRequestContext) throws URLMappingException {
       return this.getUrlMapping(hstRequestContext.getRepositoryMapping(), hstRequestContext.getURLMappingManager(), hstRequestContext.getUserID());
    }

    public URLMapping getUrlMapping(RepositoryMapping repositoryMapping, URLMappingManager urlMappingManager, String userId) throws URLMappingException{
        if(userId == null) {
            userId = "anonymous";
        }
        String key = userId+"_"+repositoryMapping.getHstConfigPath()+ "_" +  repositoryMapping.getPrefix() +"_"+repositoryMapping.getDomain().getPattern();
        CacheKey cacheKey = new CacheKey(key, URLMappingImpl.class);
        
        // TODO do not access through static but through the CacheManagerImpl instance
        Cache cache = CacheManagerImpl.getCache(userId, EventCacheImpl.class.getName());
        
        CachedResponse urlMapping = cache.get(cacheKey);
        
        if(urlMapping != null && urlMapping.getResponse() instanceof URLMapping) 
        {
            log.debug("return found urlmapping for user and context");
            return (URLMapping)urlMapping.getResponse();
        } 
        else 
        {
            URLMapping newUrlMapping = new URLMappingImpl(repository, repositoryMapping, urlMappingManager);
            AggregatedValidity aggrVal = new AggregatedValidity();
            Iterator<String> paths = newUrlMapping.getCanonicalPathsConfiguration().iterator();
            while(paths.hasNext()) {
                aggrVal.add(new EventValidity(new NamedEvent(paths.next())));
            }
            CachedResponse cr = new CachedResponseImpl(aggrVal, newUrlMapping);
            cache.store(cacheKey, cr);
            
            return newUrlMapping;
        }
    }
}
