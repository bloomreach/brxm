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
package org.hippoecm.hst.core.sitemenu;

import java.util.Map;

import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;

public interface CommonMenuItem {

    /**
     * 
     * @return the name of this SiteMenuItem
     */
    String getName();
    
    /**
     * 
     * @return a HstLink that contains a link for this SiteMenuItem
     */
    HstLink getHstLink();
    

    /**
     * @return the <code>{@link ResolvedSiteMapItem}</code> belonging to this SiteMenuItem or <code>null</code> if it cannot be resolved 
     * in the <code>{@link HstSiteMap}</code>
     */
    ResolvedSiteMapItem resolveToSiteMapItem(HstRequest request);
    
    
    /**
     * A sitemenu item is expanded if one of its descendants is selected or if it is selected itself
     * @return <code>true</code> if the SiteMenuItem is expanded
     */
    boolean isExpanded();
    
    /**
     * When developers have customized SiteMenuItem configuration with extra properties, these properties can be 
     * accessed through this Map
     * 
     * @return a Map containing the value for every property in the backing content provider for this SiteMenuItem
     */
     Map<String, Object> getProperties(); 
     
    /**
     * @return <code>true</code> when below this sitemenu item repository based navigation is expected
     */
    boolean isRepositoryBased();
    
    /**
     * 
     * @return the depth of repository based items in case of repository based navigation
     */
    int getDepth(); 
    
    /**
     * @return <code>true</code> is the SiteMenuItem is selected
     */
    boolean isSelected();
    
    
}
