package org.hippoecm.hst.core.linking;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.configuration.HstSites;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.service.jcr.JCRService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicHstLinkCreator implements HstLinkCreator{

    private static final Logger log = LoggerFactory.getLogger(HstLinkCreator.class);
  
    public HstLink create(JCRService jcrService, HstSiteMapItem siteMapItem) {
        Node n = jcrService.getValueProvider().getJcrNode();
        if(n != null) {
          return this.create(n, siteMapItem);
        } else {
            log.warn("JCRValueProvider is detached. Trying to create link with detached node path");
            // TODO create link with nodepath
        }
        
        return null;
    }
    
    public HstLink create(Node node, HstSiteMapItem siteMapItem) {
        
        return null;
    }

    public HstLink create(String toSiteMapItemId, HstSiteMapItem currentSiteMapItem) {
        HstSiteMap hstSiteMap = currentSiteMapItem.getHstSiteMap();
        HstSiteMapItem toSiteMapItem = hstSiteMap.getSiteMapItemById(toSiteMapItemId);
        if(toSiteMapItem == null) {
            // search in different sites
            HstSites hstSites = hstSiteMap.getSite().getHstSites();
            for(HstSite site : hstSites.getSites().values()) {
                toSiteMapItem = site.getSiteMap().getSiteMapItemById(toSiteMapItemId);
                if(toSiteMapItem != null) {
                    log.debug("SiteMapItemId found in different Site. Create link to different site");
                    break;
                }
            }
        } 
        if(toSiteMapItem == null) {
            log.warn("No site found with a siteMap containing id '{}'. Cannot create link.", toSiteMapItemId );
            return null;
        }
        
        String path = getPath(toSiteMapItem);
        
        return new HstLinkImpl(path, hstSiteMap.getSite());
    }


    public HstLink create(HstSiteMapItem toHstSiteMapItem) {
        return new HstLinkImpl(getPath(toHstSiteMapItem), toHstSiteMapItem.getHstSiteMap().getSite());
    }

    public HstLink create(HstSite hstSite, String toSiteMapItemId) {
        HstSiteMapItem siteMapItem = hstSite.getSiteMap().getSiteMapItemById(toSiteMapItemId);
       
        if(siteMapItem == null) {
            log.warn("No sitemap item found for id '{}' within Site '{}'. Cannot create link.", toSiteMapItemId, hstSite.getName() );
            return null;
        }
        
        return new HstLinkImpl(getPath(siteMapItem), hstSite);
    }

    private String getPath(HstSiteMapItem siteMapItem) {
        StringBuffer path = new StringBuffer(siteMapItem.getValue());
        while(siteMapItem.getParentItem() != null) {
            siteMapItem = siteMapItem.getParentItem();
            path.insert(0, "/").insert(0, siteMapItem.getValue());
        }
        return path.toString();
    }

   

}
