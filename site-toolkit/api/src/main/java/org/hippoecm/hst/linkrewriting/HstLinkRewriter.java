package org.hippoecm.hst.linkrewriting;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;

public interface HstLinkRewriter {

    /**
     * Rewrite a jcr Node to a HstLink wrt its current HstSiteMapItem
     * @param node
     * @param siteMapItem
     * @return HstLink 
     */
    HstLink rewrite(Node node, HstSiteMapItem siteMapItem);
    
    /**
     * For rewriting a link from a HstSiteMapItem to a HstSiteMapItem with toSiteMapItemId within the same Site
     * @param toSiteMapItemId
     * @param currentSiteMapItem
     * @return HstLink
     */
    HstLink rewrite(String toSiteMapItemId, HstSiteMapItem currentSiteMapItem);
    
    /**
     * Regardless the current context, create a HstLink to the HstSiteMapItem that you use as argument
     * @param toHstSiteMapItem
     * @return HstLink
     */
    HstLink rewrite(HstSiteMapItem toHstSiteMapItem);
}
