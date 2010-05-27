package org.hippoecm.hst.test.sitemapitemhandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerException;

public class SiteMapItemRedirectHandler extends AbstractTestHstSiteMapHandler {

  
    public ResolvedSiteMapItem process(ResolvedSiteMapItem resolvedSiteMapItem, HttpServletRequest request,
            HttpServletResponse response) throws HstSiteMapItemHandlerException {
        
       String redirect =  getSiteMapItemHandlerConfiguration().getProperty("unittestproject:redirecttopath", resolvedSiteMapItem, String.class);
       if(redirect == null) {
           throw new HstSiteMapItemHandlerException("Cannot redirect because the property 'unittestproject:redirectto' returns null");
       }
       
       ResolvedSiteMapItem newResolvedSiteMapItem = resolveToNewSiteMapItem(request, response, resolvedSiteMapItem, redirect);
       
       if(newResolvedSiteMapItem == null) {
           throw new HstSiteMapItemHandlerException("Cannot redirect to '"+redirect+"' because this cannot be resolved to a sitemapitem");
       }
       
       return newResolvedSiteMapItem;
    }


}
