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
package org.hippoecm.hst.configuration.sitemapitemhandlers;

import java.util.Collections;
import java.util.Map;

public interface HstSiteMapItemHandlersConfiguration {

    static final HstSiteMapItemHandlersConfiguration NOOP = new HstSiteMapItemHandlersConfiguration() {
        @Override
        public Map<String, HstSiteMapItemHandlerConfiguration> getSiteMapItemHandlerConfigurations() {
            return Collections.EMPTY_MAP;
        }

        @Override
        public HstSiteMapItemHandlerConfiguration getSiteMapItemHandlerConfiguration(final String id) {
            return null;
        }
    };

    /**
     * Return the map of all <code>HstSiteMapItemHandlerConfiguration</code>'s where the keys are the the <code>HstSiteMapItemHandlerConfiguration</code>'s 
     * ({@link HstSiteMapItemHandlerConfiguration#getId()}).
     * Implementations should return an unmodifiable map, for example {@link java.util.Collections$UnmodifiableMap} to avoid 
     * client code changing configuration
     * @return the map of all <code>HstSiteMapItemHandlerConfiguration</code>'s and an empty map if there are no <code>HstSiteMapItemHandlerConfiguration</code>'s
     */
    Map<String, HstSiteMapItemHandlerConfiguration> getSiteMapItemHandlerConfigurations();

    /**
     *  @return the HstSiteMapItemHandlerConfiguration for <code>id</code> and  <code>null</code> if there is no HstSiteMapItemHandlerConfiguration with {@link HstSiteMapItemHandlerConfiguration#getId()} = id 
     */
    HstSiteMapItemHandlerConfiguration getSiteMapItemHandlerConfiguration(String id);
    
}
