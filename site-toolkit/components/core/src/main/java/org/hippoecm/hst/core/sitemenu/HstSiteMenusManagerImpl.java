package org.hippoecm.hst.core.sitemenu;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.sitemenu.HstSiteMenus;
import org.hippoecm.hst.core.sitemenu.HstSiteMenusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSiteMenusManagerImpl implements HstSiteMenusManager{

    private static final Logger log = LoggerFactory.getLogger(HstSiteMenusManagerImpl.class);
    
    public HstSiteMenusManagerImpl() {
        // TODO parameterize caches? 
        System.out.println(" --------- HstSiteMenusManagerImpl -------------");
    }
    
    public HstSiteMenus getSiteMenus(HstRequestContext hstRequestContext){
        try {
            return new HstSiteMenusImpl(hstRequestContext.getResolvedSiteMapItem(), hstRequestContext.getSession());
        } catch (LoginException e) {
            log.warn("LoginException, cannot create SiteMenu. Return null : '{}'", e);
        } catch (RepositoryException e) {
            log.warn("RepositoryException, cannot create SiteMenu. Return null : '{}'", e);
        }
        return null;
    }
}
