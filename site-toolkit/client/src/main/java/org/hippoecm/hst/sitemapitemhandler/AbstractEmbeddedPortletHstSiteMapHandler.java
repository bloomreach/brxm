package org.hippoecm.hst.sitemapitemhandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;

/**
 * The abstract implementation of the HstSiteMapItemHandler interface. Note that HstSiteMapItemHandler's are used concurrent and are 
 * not thread-safe. Implementations should take care of concurrency.
 */
public abstract class AbstractEmbeddedPortletHstSiteMapHandler extends  AbstractHstSiteMapHandler {


    /**
     * creates a new embedded portlet HstContainerURL for <code>pathInfo</code>.
     * @param request
     * @param response
     * @param resolvedSiteMapItem
     * @param pathInfo
     * @return a new embedded portlet HstContainerURL 
     */
    public HstContainerURL createContainerURL(HttpServletRequest request, HttpServletResponse response, ResolvedSiteMapItem resolvedSiteMapItem, String pathInfo) {
        HstURLFactory factory = getURLFactory(resolvedSiteMapItem);
        return factory.getContainerURLProvider(false,true).parseURL(request, response, null, pathInfo);
    }
    
}
