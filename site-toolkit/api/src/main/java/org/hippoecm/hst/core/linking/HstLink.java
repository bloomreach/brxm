package org.hippoecm.hst.core.linking;

import org.hippoecm.hst.configuration.HstSite;

/**
 * HstLink is the object representing a link. The getPath returns you a path to a sitemapitem plus the remainder. 
 * 
 * Furthermore, the HstSite that the link is meant for is accessible through this HstLink, because it is needed if the link it
 * out of the scope of the current HstSite
 *
 */
public interface HstLink {

    String getPath();
    
    HstSite getHstSite();
    
}
