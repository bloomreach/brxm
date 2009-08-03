package org.hippoecm.hst.core.sitemenu;

import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * Implementations will be responsible for the creation of context sensitive sitemenu's. As sitemenu's are
 * a common used block on a frontend, implementation should be optimized for frequent and concurrent access. 
 *
 * The implementation of a HstSiteMenusManager can be used to (incrementally) cache created sitemenu items. Implementations must ensure to 
 * be thread safe, as the HstSiteMenusManager is used highly concurrent
 */
public interface HstSiteMenusManager {

    /**
     * 
     * @param hstRequestContext the current hstRequestContext
     * @return returns the HstSiteMenus for this request and <code>null</code> if not available
     */
    HstSiteMenus getSiteMenus(HstRequestContext hstRequestContext);
}
