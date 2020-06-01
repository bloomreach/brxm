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
package org.hippoecm.hst.core.sitemapitemhandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.core.container.HstContainerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HstSiteMapItemHandlerRegistryImpl
 * 
 */
public class HstSiteMapItemHandlerRegistryImpl implements HstSiteMapItemHandlerRegistry {
    
    static Logger log = LoggerFactory.getLogger(HstSiteMapItemHandlerRegistryImpl.class);
    
    protected Map<HstContainerConfig, Map<String, HstSiteMapItemHandlerHolder>> servletConfigSiteMapItemHandlersMap = 
        Collections.synchronizedMap(new HashMap<HstContainerConfig, Map<String, HstSiteMapItemHandlerHolder>>());

    public HstSiteMapItemHandler getSiteMapItemHandler(HstContainerConfig requestContainerConfig, String handlerId) {
        HstSiteMapItemHandlerHolder holder = getServletConfigSiteMapItemHandlersMap(requestContainerConfig, true).get(handlerId);
        
        if (holder != null) {
            return holder.getHstSiteMapItemHandler();
        }
        
        return null;
    }

    public void registerSiteMapItemHandler(HstContainerConfig requestContainerConfig, String handlerId, HstSiteMapItemHandler hstSiteMapItemHandler) {
        getServletConfigSiteMapItemHandlersMap(requestContainerConfig, true).put(handlerId, new HstSiteMapItemHandlerHolder(hstSiteMapItemHandler));
    }
                
    public void unregisterSiteMapItemHandler(HstContainerConfig requestContainerConfig, String handlerId) {
        HstSiteMapItemHandlerHolder holder = getServletConfigSiteMapItemHandlersMap(requestContainerConfig, true).remove(handlerId);
        
        if (holder != null) {
            try {
                holder.getHstSiteMapItemHandler().destroy();
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.warn("Exception occurred during destroying component: {}", e.toString(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Exception occurred during destroying component: {}", e.toString());
                }
            }
        }
    }
    
    public void unregisterAllSiteMapItemHandlers() {
        if (this.servletConfigSiteMapItemHandlersMap.isEmpty()) {
            return;
        }
        
        Map<HstContainerConfig, Map<String, HstSiteMapItemHandlerHolder>> copiedMap = Collections.synchronizedMap(new HashMap<HstContainerConfig, Map<String, HstSiteMapItemHandlerHolder>>());
        
        synchronized (this.servletConfigSiteMapItemHandlersMap) {
            for (HstContainerConfig requestContainerConfig : this.servletConfigSiteMapItemHandlersMap.keySet()) {
                copiedMap.put(requestContainerConfig, new HashMap<String, HstSiteMapItemHandlerHolder>());
            }
        }
        
        for (HstContainerConfig requestContainerConfig : copiedMap.keySet()) {
            Map<String, HstSiteMapItemHandlerHolder> siteMapItemHandlersMap = getServletConfigSiteMapItemHandlersMap(requestContainerConfig, false);
            
            if (siteMapItemHandlersMap != null) {
                Map<String, HstSiteMapItemHandlerHolder> copiedCompMap = new HashMap<String, HstSiteMapItemHandlerHolder>();
                
                synchronized (siteMapItemHandlersMap) {
                    for (Map.Entry<String, HstSiteMapItemHandlerHolder> compEntry : siteMapItemHandlersMap.entrySet()) {
                        copiedCompMap.put(compEntry.getKey(), compEntry.getValue());
                    }
                }

                copiedMap.put(requestContainerConfig, copiedCompMap);
            }
        }
        
        for (Map.Entry<HstContainerConfig, Map<String, HstSiteMapItemHandlerHolder>> entry : copiedMap.entrySet()) {
            for (Map.Entry<String, HstSiteMapItemHandlerHolder> compEntry : entry.getValue().entrySet()) {
                unregisterSiteMapItemHandler(entry.getKey(), compEntry.getKey());
            }
        }
        
        this.servletConfigSiteMapItemHandlersMap.clear();
    }
    
    protected Map<String, HstSiteMapItemHandlerHolder> getServletConfigSiteMapItemHandlersMap(HstContainerConfig requestContainerConfig, boolean create) {
        Map<String, HstSiteMapItemHandlerHolder> siteMapItemHandlersMap = this.servletConfigSiteMapItemHandlersMap.get(requestContainerConfig);
        
        if (siteMapItemHandlersMap == null && create) {
            siteMapItemHandlersMap = Collections.synchronizedMap(new HashMap<String, HstSiteMapItemHandlerHolder>());
            this.servletConfigSiteMapItemHandlersMap.put(requestContainerConfig, siteMapItemHandlersMap);
        }
        
        return this.servletConfigSiteMapItemHandlersMap.get(requestContainerConfig);
    }
    
    private static class HstSiteMapItemHandlerHolder {
        
        private HstSiteMapItemHandler hstSiteMapItemHandler;
        
        private HstSiteMapItemHandlerHolder(final HstSiteMapItemHandler hstSiteMapItemHandler) {
            this.hstSiteMapItemHandler = hstSiteMapItemHandler;
        }
        
        public HstSiteMapItemHandler getHstSiteMapItemHandler() {
            return hstSiteMapItemHandler;
        }
        
    }
    
}
