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
package org.hippoecm.hst.container;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.core.hosting.VirtualHosts;
import org.hippoecm.hst.core.hosting.VirtualHostsManager;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.logging.Logger;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.HstRequestUtils;

public class HstVirtualHostsFilter implements Filter {

    private static final long serialVersionUID = 1L;
    
    private static final String LOGGER_CATEGORY_NAME = HstVirtualHostsFilter.class.getName();

    private final static String FILTER_DONE_KEY = "filter.done_"+HstVirtualHostsFilter.class.getName();
    private final static String REQUEST_START_TICK_KEY = "request.start_"+HstVirtualHostsFilter.class.getName();
    private final static String DEFAULT_WELCOME_PAGE = "home";
    private final static String DEFAULT_PREVIEW_PREFIX = "/preview";
    private final static String WELCOME_PAGE_INIT_PARAM = "welcome-page";
    private final static String PREVIEW_PREFIX_INIT_PARAM = "preview-prefix";
    private String welcome_page = DEFAULT_WELCOME_PAGE;
    private String preview_prefix  = DEFAULT_PREVIEW_PREFIX;
    private FilterConfig filterConfig;

    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        String customWelcomePage = filterConfig.getInitParameter(WELCOME_PAGE_INIT_PARAM);
        if(customWelcomePage != null) {
            welcome_page = stripSlashes(customWelcomePage);
        }
        String customPreviewPrefix = filterConfig.getInitParameter(PREVIEW_PREFIX_INIT_PARAM);
        if(customPreviewPrefix != null) {
            preview_prefix = "/" + stripSlashes(customPreviewPrefix);
        }
    }

    private String stripSlashes(String value) {
        if(value.startsWith("/")) {
            value = value.substring(1);
        }
        if(value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        
        HttpServletRequest req = (HttpServletRequest)request;
        
        Logger logger = HstServices.getLogger(LOGGER_CATEGORY_NAME);
        
        try {
            if (!HstServices.isAvailable()) {
                ((HttpServletResponse)response).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                logger.error("The HST Container Services are not initialized yet.");
                return;
            }
            
            if (logger.isDebugEnabled()) {request.setAttribute(REQUEST_START_TICK_KEY, System.nanoTime());}
            
            String pathInfo = req.getRequestURI().substring(req.getContextPath().length());
            
            if(pathInfo.equals("")) {
                pathInfo += "/"+welcome_page;
            }
            else if(pathInfo.equals("/")) {
                pathInfo += welcome_page;
            } else if (pathInfo.equals(preview_prefix)) {
                pathInfo += "/"+welcome_page; 
            }
            else if (pathInfo.equals(preview_prefix+"/")) {
                pathInfo += welcome_page; 
            }
            
            VirtualHostsManager virtualHostManager = HstServices.getComponentManager().getComponent(VirtualHostsManager.class.getName());
            VirtualHosts vHosts = virtualHostManager.getVirtualHosts();
            if(vHosts == null || vHosts.isExcluded(pathInfo)) {
                chain.doFilter(request, response);
                return;
            }
            
            if (request.getAttribute(FILTER_DONE_KEY) == null) {
                request.setAttribute(FILTER_DONE_KEY, Boolean.TRUE);
                
                ResolvedSiteMapItem resolvedSiteMapItem = null; // vHosts.match((HttpServletRequest)request);
                
                if(resolvedSiteMapItem != null) {
                    if (resolvedSiteMapItem.getErrorCode() > 0) {
                        try {
                            if (logger.isDebugEnabled()) {
                                logger.debug("The resolved sitemap item for {} has error status: {}", pathInfo, Integer.valueOf(resolvedSiteMapItem.getErrorCode()));
                            }           
                            ((HttpServletResponse)response).sendError(resolvedSiteMapItem.getErrorCode());
                            
                        } catch (IOException e) {
                            if (logger.isDebugEnabled()) {
                                logger.warn("Exception invocation on sendError().", e);
                            } else if (logger.isWarnEnabled()) {
                                logger.warn("Exception invocation on sendError().");
                            }
                        }
                        // we're done:
                        return;
                    } 
                    /*
                     * put the matched RESOLVED_SITEMAP_ITEM temporarily on the request, as we do not yet have a HstRequestContext. When the
                     * HstRequestContext is created, and there is a RESOLVED_SITEMAP_ITEM on the request, we put it on the HstRequestContext.
                     */  
                    request.setAttribute(ContainerConstants.RESOLVED_SITEMAP_ITEM, resolvedSiteMapItem);
                    
                    // TODO  NOW, INVOKE THE HstRequestProcessor !!!
                    
                } else {
                    
                    chain.doFilter(request, response);
                }
                
            } else {
                chain.doFilter(request, response);
            }
        } catch(RepositoryNotAvailableException e) {
            final String msg = "Fatal error encountered while processing request: " + e.toString();
            throw new ServletException(msg, e);
        } finally {
            if (logger != null && logger.isDebugEnabled()) {
                long starttick = request.getAttribute(REQUEST_START_TICK_KEY) == null ? 0 : (Long)request.getAttribute(REQUEST_START_TICK_KEY);
                if(starttick != 0) {
                    logger.debug( "Handling request took --({})-- ms for '{}'.", (System.nanoTime() - starttick)/1000000, req.getRequestURI());
                }
            }
        }
    }

    public void destroy() {

    }

}
