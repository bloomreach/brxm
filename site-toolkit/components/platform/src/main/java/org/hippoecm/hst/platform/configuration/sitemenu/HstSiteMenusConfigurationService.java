/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.sitemenu;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.platform.configuration.cache.CompositeConfigurationNodes;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenusConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSiteMenusConfigurationService implements HstSiteMenusConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(HstSiteMenusConfigurationService.class);
    
    private HstSite hstSite;
    private Map<String, HstSiteMenuConfiguration> hstSiteMenuConfigurations = new HashMap<>();
    
    public HstSiteMenusConfigurationService(final HstSite hstSite,
                                            final CompositeConfigurationNodes.CompositeConfigurationNode siteMenusNode) {
        this.hstSite = hstSite;
        Map<String, HstSiteMenuConfiguration> menus = new HashMap<>();
        for(HstNode siteMenu: siteMenusNode.getCompositeChildren().values()) {
            if(HstNodeTypes.NODETYPE_HST_SITEMENU.equals(siteMenu.getNodeTypeName())) {
                HstSiteMenuConfiguration hstSiteMenuConfiguration = new HstSiteMenuConfigurationService(this, siteMenu);
                HstSiteMenuConfiguration old = menus.put(hstSiteMenuConfiguration.getName(), hstSiteMenuConfiguration);
                if(old != null) {
                    log.error("Duplicate name for HstSiteMenuConfiguration found. The first one is replaced");
                }
            } else {
                log.error("Skipping siteMenu '{}' because not of type '{}'", siteMenu.getValueProvider().getPath(), HstNodeTypes.NODETYPE_HST_SITEMENU);
            }
        }
        hstSiteMenuConfigurations = Collections.unmodifiableMap(menus);
    }

    public HstSite getSite() {
        return this.hstSite;
    }

    public Map<String, HstSiteMenuConfiguration> getSiteMenuConfigurations() {
        return hstSiteMenuConfigurations;
    }

    public HstSiteMenuConfiguration getSiteMenuConfiguration(String name) {
        return this.hstSiteMenuConfigurations.get(name);
    }


}
