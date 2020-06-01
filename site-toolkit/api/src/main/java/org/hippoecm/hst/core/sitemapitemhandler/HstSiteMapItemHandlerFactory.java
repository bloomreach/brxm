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

import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlerConfiguration;
import org.hippoecm.hst.core.container.HstContainerConfig;

/**
 * The factory interface which is responsible for creating HstSiteMapItemHandler instances.
 * 
 */
public interface HstSiteMapItemHandlerFactory {
    
    /**
     * Returns the HstSiteMapItemHandler instance.
     * 
     * @param requestContainerConfig the HstContainer configuration
     * @param handlerConfig the HstSiteMapItemHandlerConfiguration configuration
     * @return the instance of the HstSiteMapItemHandler
     * @throws HstSiteMapItemHandlerException
     */
    HstSiteMapItemHandler getSiteMapItemHandlerInstance(HstContainerConfig requestContainerConfig, HstSiteMapItemHandlerConfiguration handlerConfig) throws HstSiteMapItemHandlerException;
   
    
}
