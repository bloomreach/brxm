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

import javax.jcr.Node;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.service.Service;

public interface HstLinkCreator {
   
   
    HstLink create(Service service, HstSiteMapItem siteMapItem);
    
    /**
     * Rewrite a jcr Node to a HstLink wrt its current HstSiteMapItem
     * @param node
     * @param siteMapItem
     * @return HstLink 
     */
    HstLink create(Node node, HstSiteMapItem siteMapItem);
    
    /**
     * For creating a link from a HstSiteMapItem to a HstSiteMapItem with toSiteMapItemId within the same Site
     * @param toSiteMapItemId
     * @param currentSiteMapItem
     * @return HstLink
     */
    HstLink create(String toSiteMapItemId, HstSiteMapItem currentSiteMapItem);
    
    /**
     * Regardless the current context, create a HstLink to the HstSiteMapItem that you use as argument
     * @param toHstSiteMapItem
     * @return HstLink
     */
    HstLink create(HstSiteMapItem toHstSiteMapItem);
    
    
    /**
     * create a link to siteMapItem of hstSite 
     * @param hstSite
     * @param toSiteMapItemId
     * @return HstLink
     */
    HstLink create(HstSite hstSite, String toSiteMapItemId);
}
