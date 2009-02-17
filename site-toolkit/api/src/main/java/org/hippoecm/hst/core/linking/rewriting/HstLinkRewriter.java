package org.hippoecm.hst.core.linking.rewriting;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.linking.HstLink;

public interface HstLinkRewriter {

    /**
     * Rewrite a jcr Node to a HstLink wrt its current HstSiteMapItem
     * @param node
     * @param siteMapItem
     * @return HstLink 
     */
    HstLink rewrite(Node node, HstSiteMapItem siteMapItem);
    
    
}
