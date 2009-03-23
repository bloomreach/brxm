package org.hippoecm.hst.core.container;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.sitemenu.HstSiteMenus;
import org.hippoecm.hst.site.request.HstRequestContextImpl;

public class SiteMenusResolvingValve extends AbstractValve {

    @Override
    public void invoke(ValveContext context) throws ContainerException {
       
        HttpServletRequest servletRequest = (HttpServletRequest) context.getServletRequest();
        HstRequestContext requestContext = (HstRequestContext) servletRequest.getAttribute(HstRequestContext.class.getName());
        
        HstSiteMenus siteMenus = this.siteMenusManager.getSiteMenus(requestContext);
        ((HstRequestContextImpl)requestContext).setHstSiteMenus(siteMenus);
        
        context.invokeNext();
    }

}
