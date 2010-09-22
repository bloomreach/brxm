package org.hippoecm.hst.sitemapitemhandler;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.SiteMapItemHandlerConfiguration;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandler;
import org.hippoecm.hst.core.sitemapitemhandler.HstSiteMapItemHandlerException;

/**
 * The abstract implementation of the HstSiteMapItemHandler interface. Note that HstSiteMapItemHandler's are used concurrent and are 
 * not thread-safe. Implementations should take care of concurrency.
 */
public abstract class AbstractHstSiteMapHandler implements HstSiteMapItemHandler{

    private ServletContext servletContext;
    private SiteMapItemHandlerConfiguration handlerConfig;
    
    public void init(ServletContext servletContext, SiteMapItemHandlerConfiguration handlerConfig) throws HstSiteMapItemHandlerException {
        this.handlerConfig = handlerConfig;
        this.servletContext = servletContext;
    }
    
    /**
     * Override this method when you are implementing your own real HstSiteMapHandler. By default, the AbstractHstSiteMapHandler returns 
     * the <code>resolvedSiteMapItem</code> directly.
     */
    public ResolvedSiteMapItem process(ResolvedSiteMapItem resolvedSiteMapItem, HttpServletRequest request,
            HttpServletResponse response) throws HstSiteMapItemHandlerException {
        return resolvedSiteMapItem;
    }

    
    /**
     * Override this method when the destroy of this HstSiteMapItemHandler should invoke some processing, for example clear a cache
     */
    public void destroy() throws HstSiteMapItemHandlerException {
    }
    
    public SiteMapItemHandlerConfiguration getSiteMapItemHandlerConfiguration() {
        return handlerConfig;
    }
    public ServletContext getServletContext() {
        return servletContext;
    }

    /**
     * Resolves with the help of the current resolvedSiteMapItem to a new sitemap item with path <code>pathInfo</code>
     * @param request
     * @param response
     * @param currentResolvedSiteMapItem
     * @param pathInfo
     * @throws MatchException when the <code>pathInfo</code> cannot be matched
     * @return a new ResolvedSiteMapItem
     */
    public ResolvedSiteMapItem matchSiteMapItem(HttpServletRequest request,
            HttpServletResponse response, ResolvedSiteMapItem currentResolvedSiteMapItem, String pathInfo) throws MatchException {
        
        return currentResolvedSiteMapItem.getResolvedSiteMount().matchSiteMapItem(pathInfo);
    }
    
/* TODO
     * creates a new website HstContainerURL for <code>pathInfo</code>.
     * @param request
     * @param response
     * @param resolvedSiteMapItem
     * @param pathInfo
     * @return HstContainerURL 
     *
    public HstContainerURL createContainerURL(HttpServletRequest request, HttpServletResponse response, ResolvedSiteMapItem resolvedSiteMapItem, String pathInfo) {
        HstURLFactory factory = getURLFactory(resolvedSiteMapItem);
        return factory.getContainerURLProvider().parseURL(request, response, null, pathInfo);
    }
*/    
    
    /**
     * @param resolvedSiteMapItem
     * @return the HstURLFactory 
     */
    public HstURLFactory getURLFactory(ResolvedSiteMapItem resolvedSiteMapItem) {
        HstURLFactory factory = resolvedSiteMapItem.getResolvedSiteMount().getResolvedVirtualHost().getVirtualHost().getVirtualHosts().getHstManager().getUrlFactory();
        return factory;
    }
}
