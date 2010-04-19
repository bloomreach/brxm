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
package org.hippoecm.hst.container;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.constructs.web.filter.SimplePageCachingFilter;

/**
 * TODO this is a work in progress HST Page Cache filter, which is an HST optimization for high traffic / large concurrency environments, where
 * throughput must be extremely large. A cached valid page won't hit the hst2 application at all, and will be served from cache. Easily thousands to tens of
 * of thousands page request per second can be handled by this filter. 
 * 
 * in the web.xml you can use this filter with 
 * 
 * <dispatchet>FORWARD</dispatcher> 
 * 
 * as the request will be forwarded from the HstVirtualHostsFilter
 * 
 * TODO 
 * <UL>
 *    <LI> Configure the cache characteristics in the repository / web.xml / hst config properties </LI>
 *    <LI> Add correct headers to response</LI>
 *    <LI> Repository invalidation when a change happens</LI>
 *    <LI> Seperate caches for preview / live </LI>
 *    <LI> Allow for exclusions (for example everything below /bar)</LI>
 *    <LI> Do not cache actions (can be set on the HstResponse to not cache)</LI>
 *    <LI> Send a 304 for not changed pages, and handle a ctrl-refresh</LI>
 *    <LI> Make concurrent calls for the exact same page wait until the first request returns, and serve the other requests from cache</LI>
 *    <LI> Investigate a clustered cache where all HST instances share their cache</LI>
 * </UL>
 */
public class PageCachingFilter extends SimplePageCachingFilter {

    
    @Override
    protected String getCacheName() {
        return "PageCachingFilter";
    }

    @Override
    public void doInit() throws CacheException {
        CacheManager cacheMngr = this.getCacheManager();
        // 5 minutes ttl and idle
        Cache memCache = new Cache(getCacheName(), 5000, false, false, 300 , 300);
        cacheMngr.addCache(memCache);
        super.doInit();
    }
     
}
