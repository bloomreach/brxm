package org.hippoecm.hst.test.sitemapitemhandler;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerException;

public class BrowserRedirectHandler extends AbstractTestHstSiteMapHandler {

  
    public ResolvedSiteMapItem process(ResolvedSiteMapItem resolvedSiteMapItem, HttpServletRequest request,
            HttpServletResponse response) throws HstSiteMapItemHandlerException {
        
       String redirect =  getSiteMapItemHandlerConfiguration().getProperty("unittestproject:redirectto", resolvedSiteMapItem, String.class);
       try {
          response.sendRedirect(redirect);
       } catch (IOException e) {
          throw new HstSiteMapItemHandlerException(e);
       }
      return null;
    }

}
