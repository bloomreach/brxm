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
package org.hippoecm.hst.core.linking;

import java.util.List;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.LocationMapTree;
import org.hippoecm.hst.configuration.LocationMapTreeItem;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItemUtitlites;
import org.hippoecm.hst.core.linking.HstPathConvertor;
import org.hippoecm.hst.core.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicHstPathConvertor implements HstPathConvertor{

    private static final Logger log = LoggerFactory.getLogger(HstPathConvertor.class);
    
    public ConversionResult convert(String path, HstSite hstSite) {
        
        log.debug("Trying to convert path '{}' for hstSite '{}'", path, hstSite.getName());
        
        LocationMapTree locationMap = hstSite.getLocationMap();
        
        if(!path.startsWith(locationMap.getCanonicalSiteContentPath())){
            log.debug("Cannot convert path '{}' for hstSite '{}' because the path does not start " +
            		"with the content path of the hstSite. Return null", path, hstSite.getName());
            return null;
        }
        path = path.substring(locationMap.getCanonicalSiteContentPath().length());
        // normalize leading and trailing slashes
        path = PathUtils.normalizePath(path);
        
        LocationMapTreeItem bestLocationMapTreeItem = locationMap.find(path);
        
        if(bestLocationMapTreeItem == null) {
            log.warn("No best LocationMapTreeItem found. The path cannot be converted. Return null.");
            return null;
        }
        List<HstSiteMapItem> hstSiteMapItems = bestLocationMapTreeItem.getHstSiteMapItems();
        if(hstSiteMapItems.size() != 1) {
            if(hstSiteMapItems.size() == 0) {
                log.warn("Found best LocationMapTreeItem does not contain any SiteMapItem. The path cannot be converted. Return null.");
                return null;
            } 
            else {
                log.warn("The best matching LocationMapTreeItem contains '{}' SiteMapItems. Result is ambiguous. Return first HstSiteMapItem");
            }
        }
        
        HstSiteMapItem matchingSiteMapItem = hstSiteMapItems.get(0);
        
        log.debug("Best matching HstSiteMapItem '{}' found for path '{}'. Return ConversionResult", matchingSiteMapItem.getId() ,path);
        
        if(!path.startsWith(matchingSiteMapItem.getRelativeContentPath())) {
            if (log.isWarnEnabled()) {
                log.warn("The path must start with the relativeContentPath of he matching HstSiteMapItem. This is not the case for '{}' <--> '{}'. Return null.", path , matchingSiteMapItem.getRelativeContentPath());
            }
            return null;
        }
        String relativeToSiteMapItem = PathUtils.normalizePath(path.substring(matchingSiteMapItem.getRelativeContentPath().length()));
        
        String siteMapItemPath = HstSiteMapItemUtitlites.getPath(matchingSiteMapItem);
        
        if(relativeToSiteMapItem == null || "".equals(relativeToSiteMapItem)){
            return new ConversionResultImpl(siteMapItemPath, matchingSiteMapItem.getId()); 
        } else {
            if(!relativeToSiteMapItem.startsWith("/")) {
                relativeToSiteMapItem = "/"+relativeToSiteMapItem;
            }
            return new ConversionResultImpl(siteMapItemPath + relativeToSiteMapItem, matchingSiteMapItem.getId());
        }
         
    }
    
    public class ConversionResultImpl implements ConversionResult {

        private static final long serialVersionUID = 1L;
        
        private String path;
        private String hstSiteMapItemId;
        
        public ConversionResultImpl(String path, String hstSiteMapItemId){
            this.path = path;
            this.hstSiteMapItemId = hstSiteMapItemId;
        }
        
        public String getPath() {
            return this.path;
        }

        public String getSiteMapItemId() {
            return this.hstSiteMapItemId;
        }
        
    }
}
