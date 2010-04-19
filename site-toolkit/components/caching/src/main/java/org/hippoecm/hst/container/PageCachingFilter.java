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
