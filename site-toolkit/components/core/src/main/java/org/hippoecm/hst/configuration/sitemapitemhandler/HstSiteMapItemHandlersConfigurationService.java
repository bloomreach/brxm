/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.sitemapitemhandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.cache.CompositeConfigurationNodes;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.model.ModelLoadingException;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlerConfiguration;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlersConfiguration;
import org.slf4j.LoggerFactory;

public class HstSiteMapItemHandlersConfigurationService implements HstSiteMapItemHandlersConfiguration {

    private static final long serialVersionUID = 1L;
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstSiteMapItemHandlersConfigurationService.class);

    private Map<String, HstSiteMapItemHandlerConfiguration> siteMapItemHanderConfigurations = new HashMap<String, HstSiteMapItemHandlerConfiguration>();
    
    public HstSiteMapItemHandlersConfigurationService(final CompositeConfigurationNodes.CompositeConfigurationNode ccn) throws ModelLoadingException {
        for(HstNode handlerNode : ccn.getCompositeChildren().values()) {
            if(HstNodeTypes.NODETYPE_HST_SITEMAPITEMHANDLER.equals(handlerNode.getNodeTypeName())) {
                try {
                    HstSiteMapItemHandlerConfiguration siteMapItemHandler = new HstSiteMapItemHandlerConfigurationService(handlerNode);
                    siteMapItemHanderConfigurations.put(siteMapItemHandler.getId(), siteMapItemHandler);
                } catch (ModelLoadingException e) {
                    log.warn("Skipping handle '{}' because '{}'", handlerNode.getValueProvider().getPath(), e.getMessage());
                }
            }else {
               log.warn("Skipping node '{}' because is not of type {}", handlerNode.getValueProvider().getPath(), HstNodeTypes.NODETYPE_HST_SITEMAPITEMHANDLER); 
            }
        }
    }

    public HstSiteMapItemHandlerConfiguration getSiteMapItemHandlerConfiguration(String id) {
        return siteMapItemHanderConfigurations.get(id);
    }

    public Map<String, HstSiteMapItemHandlerConfiguration> getSiteMapItemHandlerConfigurations() {
        return Collections.unmodifiableMap(siteMapItemHanderConfigurations);
    }

}
