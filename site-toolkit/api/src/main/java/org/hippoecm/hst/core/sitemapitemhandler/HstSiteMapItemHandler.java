/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
 * A HstSiteMapItemHandler can be invoked by HstFilter when the resolved sitemap item
 * is configured with custom sitemap item handler IDs in the HST configurations.
 * HstSiteMapItemHandler is provided to enable custom request processing for the resolved sitemap item.
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
     * Does custom request processing.
     * <P>
     * This method can return the original resolvedSiteMapItem or a new resolved sitemap item to serve a different one.
     * Or it can return null when it completes the custom request processing by itself so HstFilter needs to stop the request processing.
     * </P>
     * 
     * @param resolvedSiteMapItem
     * @param request
     * @param response
     * @return a new or the original {@link ResolvedSiteMapItem}, or <code>null</code> when the handler did for example already write the entire <code>response</code> and request processing can be stopped
     * @throws HstSiteMapItemHandlerException
     */
    ResolvedSiteMapItem process(ResolvedSiteMapItem resolvedSiteMapItem, HttpServletRequest request, HttpServletResponse response) throws HstSiteMapItemHandlerException;

    /**
     * Allows the sitemap handler to destroy itself
     * 
     * @throws HstSiteMapItemHandlerException
     * @deprecated since 13.0.0 : #destroy won't be invoked any more and will be removed. On every
     * {@link org.hippoecm.hst.configuration.hosting.VirtualHosts} rebuild (aka after hst config changes),
     * {@link HstSiteMapItemHandler} instances are recreated. Make sure that your implementation of the
     * {@link HstSiteMapItemHandler} does not result in a memory leak, for example because you add objects to a
     * class variable like a static cache. The reason why this has been deprecated is that HstSiteMapItemHandler instances
     * now piggy-back on the lifecycle on the {@link org.hippoecm.hst.configuration.hosting.VirtualHosts} instead of on
     * a separate registry
     */
    @Deprecated
    void destroy() throws HstSiteMapItemHandlerException;


}
