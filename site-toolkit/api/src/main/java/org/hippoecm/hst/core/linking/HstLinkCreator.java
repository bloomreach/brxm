package org.hippoecm.hst.core.linking;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;

public interface HstLinkCreator {
   
    /**
     * Rewrite a jcr Node to a HstLink wrt its current HstSiteMapItem
     * @param node
     * @param siteMapItem
     * @return HstLink 
     */
    HstLink rewrite(Node node, HstSiteMapItem siteMapItem);
    
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
