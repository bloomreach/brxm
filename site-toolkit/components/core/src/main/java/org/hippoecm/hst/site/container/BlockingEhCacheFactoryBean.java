/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.site.container;

import org.springframework.cache.ehcache.EhCacheFactoryBean;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.constructs.blocking.BlockingCache;

public class BlockingEhCacheFactoryBean extends EhCacheFactoryBean {
    
    private int timeoutMillis;
    
    public BlockingEhCacheFactoryBean() {
        super();
        setBlocking(true);
    }
    
    public void setTimeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }
    
    @Override
    protected Ehcache decorateCache(Ehcache cache) {
        Ehcache decorated = super.decorateCache(cache);
        
        if (decorated instanceof BlockingCache) {
            ((BlockingCache) decorated).setTimeoutMillis(timeoutMillis);
        }
        
        return decorated;
    }
}
