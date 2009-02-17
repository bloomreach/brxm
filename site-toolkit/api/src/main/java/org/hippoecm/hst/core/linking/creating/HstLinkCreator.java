package org.hippoecm.hst.core.linking.creating;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.linking.HstLink;

public interface HstLinkCreator {
   
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
