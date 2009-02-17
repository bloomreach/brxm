package org.hippoecm.hst.linkrewriting;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.HstSites;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicHstLinkRewriter implements HstLinkRewriter{

    private static final Logger log = LoggerFactory.getLogger(HstLinkRewriter.class);
    
    public HstLink rewrite(Node node, HstSiteMapItem siteMapItem) {
        // TODO Auto-generated method stub
        return null;
    }

    public HstLink rewrite(String toSiteMapItemId, HstSiteMapItem currentSiteMapItem) {
        HstSiteMap hstSiteMap = currentSiteMapItem.getHstSiteMap();
        HstSite hstSite;
        HstSiteMapItem toSiteMapItem = hstSiteMap.getSiteMapItemById(toSiteMapItemId);
        if(toSiteMapItem == null) {
            // search in different sites
            HstSites hstSites = hstSiteMap.getSite().getHstSites();
            for(HstSite site : hstSites.getSites().values()) {
                toSiteMapItem = site.getSiteMap().getSiteMapItemById(toSiteMapItemId);
                if(toSiteMapItem != null) {
                    log.debug("SiteMapItemId found in different Site. Linking to different site");
                    break;
                }
            }
        } 
        if(toSiteMapItem == null) {
            log.warn("No site found with a siteMap containing id '{}'. Cannot rewrite link.", toSiteMapItemId );
            return null;
        }
        
        StringBuffer path = new StringBuffer(toSiteMapItem.getValue());
        while(toSiteMapItem.getParentItem() != null) {
            toSiteMapItem = toSiteMapItem.getParentItem();
            path.insert(0, "/").insert(0, toSiteMapItem.getValue());
        }
        
        return new HstLinkImpl(path.toString(), hstSiteMap.getSite());
    }

    public HstLink rewrite(HstSiteMapItem toHstSiteMapItem) {
        // TODO Auto-generated method stub
        return null;
    }

}
