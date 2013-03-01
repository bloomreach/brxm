/**
 * Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.core.sitemapitemhandler;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.request.ResolvedSiteMapItem;

/**
 * FilterChainAwareHstSiteMapItemHandler extends HstSiteMapItemHandler interface.
 * If a sitemap item handler implements this interface, then {{@link #process(ResolvedSiteMapItem, HttpServletRequest, HttpServletResponse, FilterChain)} is always invoked,
 * instead of {@link #process(ResolvedSiteMapItem, HttpServletRequest, HttpServletResponse)}, by HstFilter.
 */
public interface FilterChainAwareHstSiteMapItemHandler extends HstSiteMapItemHandler {

    /**
     * Does custom request processing.
     * <P>
     * This method can return the original resolvedSiteMapItem or a new resolved sitemap item to serve a different one.
     * Or it can return null when it completes the custom request processing by itself so HstFilter needs to stop the request processing.
     * </P>
     * <P>
     * This method also receives FilterChain instance so it can continue the request processing by invoking filterChain.doFilter(..) method.
     * </P>
     * 
     * @param resolvedSiteMapItem
     * @param request
     * @param response
     * @param filterChain
     * @return a new or the original {@link ResolvedSiteMapItem}, or <code>null</code> when the handler did for example already write the entire <code>response</code> and request processing can be stopped
     * @throws HstSiteMapItemHandlerException
     */
    ResolvedSiteMapItem process(ResolvedSiteMapItem resolvedSiteMapItem, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws HstSiteMapItemHandlerException;

}
