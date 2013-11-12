/**
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.cache.ehcache;

import com.google.common.eventbus.Subscribe;

import org.hippoecm.hst.container.event.ComponentManagerBeforeReplacedEvent;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentManagerAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.ehcache.CacheManager;

/**
 * EhCacheManagerShuttingDownListener
 * <P>
 * A component bean, responsible for shutting down the existing cacheManager before the component manager is replaced by a new one.
 * </P>
 */
public class EhCacheManagerShuttingDownListener implements ComponentManagerAware {

    private static Logger log = LoggerFactory.getLogger(EhCacheManagerShuttingDownListener.class);

    private ComponentManager componentManager;
    private CacheManager cacheManager;

    @Override
    public void setComponentManager(ComponentManager componentManager) {
        this.componentManager = componentManager;
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void init() {
        componentManager.registerEventSubscriber(this);
    }

    public void destroy() {
        componentManager.unregisterEventSubscriber(this);
    }

    @Subscribe
    public void onComponentManagerBeforeReloadedEvent(ComponentManagerBeforeReplacedEvent event) {
        if (cacheManager != null) {
            log.info("Shutting down cacheManager: {}", cacheManager.getName());
            cacheManager.shutdown();
        }
    }
}
