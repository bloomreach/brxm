/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.core.sitemapitemhandler;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.SiteMapItemHandlerConfiguration;

/**
 * TODO A HstSiteMapItemHandler can be invoked by .... 
 * 
 */
public interface HstSiteMapItemHandler {
    
    /**
     * Allows the HstSiteMapItemHandler to initialize itself
     * 
     * @param servletContext the servletContext of the HST container servlet
     * @param handlerConfig the componentConfigBean configuration
     * @throws HstSiteMapItemHandlerException
     */
    void init(ServletContext servletContext, SiteMapItemHandlerConfiguration handlerConfig) throws HstSiteMapItemHandlerException;
    
    /**
     * 
     * @param resolvedSiteMapItem
     * @param request
     * @param response
     * @return a new or the original {@link ResolvedSiteMapItem}, or <code>null</code> when the handler did for example already write the entire <code>response</code> and request processing can be stopped
     * @throws HstSiteMapItemHandlerException
     */
    ResolvedSiteMapItem process(ResolvedSiteMapItem resolvedSiteMapItem, HttpServletRequest request, HttpServletResponse response) throws HstSiteMapItemHandlerException;
    
    /**
     * Through the {@link SiteMapItemHandlerConfiguration} all (resolved) configuration properties can be accessed. 
     * @return the SiteMapItemHandlerConfiguration backing this HstSiteMapItemHandler
     * @deprecated  this method is deprecated since 2.25.02 as it should not be needed. The {@link SiteMapItemHandlerConfiguration} can
     * be accessed by storing the SiteMapItemHandlerConfiguration during the {@link #init(javax.servlet.ServletContext, org.hippoecm.hst.core.request.SiteMapItemHandlerConfiguration)}
     */
    @Deprecated
    SiteMapItemHandlerConfiguration getSiteMapItemHandlerConfiguration();
    
    /**
     * @return Retrieves the servletContext to which this HstSiteMapItemHandler is bound
     * @deprecated  this method is deprecated since 2.25.02
     */
    @Deprecated
    ServletContext getServletContext();
    
    /**
     * Allows the sitemap handler to destroy itself
     * 
     * @throws HstSiteMapItemHandlerException
     */
    void destroy() throws HstSiteMapItemHandlerException;
    
}
