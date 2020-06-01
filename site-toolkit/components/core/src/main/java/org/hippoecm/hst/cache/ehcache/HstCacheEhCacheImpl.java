/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.cache.ehcache;

import org.hippoecm.hst.cache.CompositeHstCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.PersistenceConfiguration;

/**
 * @deprecated since HST 4.2.0 (CMS 11.2.0). Use {@link CompositeHstCache} instead.
 */
@Deprecated
public class HstCacheEhCacheImpl extends CompositeHstCache {

    private static final Logger log = LoggerFactory.getLogger(HstCacheEhCacheImpl.class);

    public HstCacheEhCacheImpl(final Ehcache ehcache) {
        this(ehcache, new PersistenceConfiguration().strategy(PersistenceConfiguration.Strategy.NONE));
    }

    public HstCacheEhCacheImpl(final Ehcache ehcache, final PersistenceConfiguration persistenceConfiguration) {
        super(ehcache, persistenceConfiguration);
        log.warn(String.format("This class '%s' has been deprecated, use '%s' instead.", HstCacheEhCacheImpl.class.getName(),
                CompositeHstCache.class.getName()));
    }


}
