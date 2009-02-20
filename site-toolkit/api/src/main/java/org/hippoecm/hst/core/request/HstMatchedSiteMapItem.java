package org.hippoecm.hst.core.request;

import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;

public interface HstMatchedSiteMapItem {
    
    HstSiteMapItem getSiteMapItem();
    
    String getRemainder();
}
