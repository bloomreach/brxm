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
package org.hippoecm.hst.configuration;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.util.PathUtils;

public class BasicHstSiteMapMatcher implements HstSiteMapMatcher{

    public MatchResult match(String link, HstSite hstSite) {
        link = PathUtils.normalizePath(link);
        String[] elements = link.split("/"); 
        
        HstSiteMapItem hstSiteMapItem = hstSite.getSiteMap().getSiteMapItem(elements[0]);
        if(hstSiteMapItem == null) {
            // return a MatchResult with HstSiteMapItem = null
            return new MatchResultImpl(link, null, null);
        }
        
        int i = 1;
        while(i < elements.length && hstSiteMapItem.getChild(elements[i]) != null){
            hstSiteMapItem = hstSiteMapItem.getChild(elements[i]);
            i++;
        }
        
        StringBuffer remainder = new StringBuffer();
        while(i < elements.length) {
            if(remainder.length() > 0) {
                remainder.append("/");
            }
            remainder.append(elements[i]);
            i++;
        }
        
        
        return new MatchResultImpl(remainder.toString(), hstSiteMapItem, hstSite.getComponentsConfiguration().getComponentConfiguration(hstSiteMapItem.getComponentConfigurationId()));
    }
    
    public class MatchResultImpl implements MatchResult {

        private String remainder;
        private HstSiteMapItem hstSiteMapItem;
        private HstComponentConfiguration hstComponentConfiguration;
        
        public MatchResultImpl(String remainder, HstSiteMapItem hstSiteMapItem, HstComponentConfiguration hstComponentConfiguration){
            this.remainder = remainder;
            this.hstSiteMapItem = hstSiteMapItem;
            this.hstComponentConfiguration = hstComponentConfiguration;
        }
        public String getRemainder() {
            return this.remainder;
        }

        public HstSiteMapItem getSiteMapItem() {
            return this.hstSiteMapItem;
        }
        
        public HstComponentConfiguration getCompontentConfiguration() {
            return hstComponentConfiguration;
        }
        
    }

}
