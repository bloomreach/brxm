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
package org.hippoecm.hst.core.filters;

import java.io.IOException;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.filters.base.HstBaseFilter;
import org.hippoecm.hst.core.mapping.URLMapping;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstRequestContextImpl;
import org.hippoecm.hst.core.template.node.PageNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class URLMappingTemplateContextFilter extends HstBaseFilter implements Filter {

	public static final String SITEMAP_CONTEXTBASE_NAME = "siteMapContextBase";
	
	
	public static final String JCRSESSION_REQUEST_ATTRIBUTE = "jcrSession";
	//public static final String NAVIGATION_REQUEST_ATTRIBUTE = "hstNavigationMapLocation";
	
	//public static final String NAVIGATION_CONTEXTBASE_REQUEST_ATTRIBUTE = "navigationContextBase";
	//public static final String NAVIGATION_CONTEXTBASE_NAME = "navigationContext";	
	
	public static final String REPOSITORY_LOCATION_FILTER_INIT_PARAM = "hstConfigurationUrl";
	
	private static final Logger log = LoggerFactory.getLogger(URLMappingTemplateContextFilter.class);
	

	/**
     * The filter determines the templates to use and the content location from the request URI.
     * If a template is found the request is forwarded to that template. If not, the filterchain is
     * continued with the original request.
     */
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        
            super.doFilter(request, response, chain);
    }
	
    @Override
    public void handleRequestForThisFilter(HttpServletRequest request, ServletResponse response, FilterChain chain, HstRequestContext hstRequestContext) throws IOException, ServletException {
        if( hstRequestContext == null) {
            log.warn("hstRequestContext is null. Cannot process filter");
            chain.doFilter(request, response);
            return;
        }
        log.debug("process " + hstRequestContext.getHstRequestUri());
        try {
            URLMapping urlMapping = hstRequestContext.getUrlMapping();
            if(urlMapping == null) {
                log.warn("URLMapping is null, cannot get a matching page node");
                chain.doFilter(request, response);
            } 
            PageNode matchPageNode = urlMapping.getMatchingPageNode(hstRequestContext.getHstRequestUri(), hstRequestContext);
            if (matchPageNode != null) {
                String urlPrefix = getUrlPrefix(request);
                RequestDispatcher dispatcher = request.getRequestDispatcher(urlPrefix + matchPageNode.getLayoutNode().getTemplatePage());
                //set attributes
                ((HstRequestContextImpl) hstRequestContext).setPageNode(matchPageNode);
                
                // below deprecated: take it from the hstRequestContext
                request.setAttribute(HSTHttpAttributes.CURRENT_PAGE_NODE_REQ_ATTRIBUTE, matchPageNode);
                
                dispatcher.forward(request, response);
            } else {
                log.warn("no matching template found for url '{}'", hstRequestContext.getHstRequestUri());
                //what to do? no matching pattern found... lets continue the filter chain...
                chain.doFilter(request, response);
            }
        } catch (PathNotFoundException e) {
            log.warn("PathNotFoundException: " + e.getMessage());
            log.debug("PathNotFoundException: " , e);
            chain.doFilter(request, response);
        } catch (ValueFormatException e) {
            log.warn("ValueFormatException: " + e.getMessage());
            log.debug("ValueFormatException: " , e);
            chain.doFilter(request, response);
        } catch (RepositoryException e) {
            log.warn("RepositoryException: " + e.getMessage());
            log.debug("RepositoryException: " , e);
            chain.doFilter(request, response);
        }
    }
	

}
