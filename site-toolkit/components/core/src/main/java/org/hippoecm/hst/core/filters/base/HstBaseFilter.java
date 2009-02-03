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
package org.hippoecm.hst.core.filters.base;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.context.ContextBase;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for HST related filters.
 */
public abstract class HstBaseFilter implements Filter {

    public static final String TEMPLATE_CONFIGURATION_LOCATION = "/";
    public static final String SITEMAP_RELATIVE_LOCATION = "hst:sitemap";
    public static final String ATTRIBUTENAME_INIT_PARAMETER = "attributeName";
    
 
    private static final Logger log = LoggerFactory.getLogger(HstBaseFilter.class);

    public void init(FilterConfig filterConfig) throws ServletException {
        
    }

    public void destroy() {
      
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        // if there is no HstRequestContext obj on the request the request can be ignored
        if(request.getAttribute(HstRequestContext.class.getName()) == null ){
            chain.doFilter(request, response);
        }  else {
            HstRequestContext hstRequestContext = (HstRequestContext)request.getAttribute(HstRequestContext.class.getName());
            handleRequestForThisFilter((HttpServletRequest)request, response,chain, hstRequestContext);
        }
    }
    
    // To be implemented by filter which should handle a request
    public abstract void handleRequestForThisFilter(HttpServletRequest request, ServletResponse response, FilterChain chain, HstRequestContext hstRequestContext) throws IOException, ServletException;
        
    protected String getUrlPrefix(HttpServletRequest request) {
        String urlPrefix = (String) request.getAttribute(ATTRIBUTENAME_INIT_PARAMETER);
        urlPrefix = (urlPrefix == null) ? "" : urlPrefix;
        return urlPrefix;
    }

//    protected Map<String, PageNode> getURLMappingNodes(ContextBase templateContextBase) throws RepositoryException {
//        Map<String, PageNode> siteMapNodes = new HashMap<String, PageNode>();
//
//        Node siteMapRootNode = templateContextBase.getRelativeNode(SITEMAP_RELATIVE_LOCATION);
//        NodeIterator subNodes = siteMapRootNode.getNodes();
//        while (subNodes.hasNext()) {
//            Node subNode = (Node) subNodes.next();
//            if (subNode == null) {
//                continue;
//            }
//            if (subNode.hasProperty("hst:urlmapping")) {
//                Property urlMappingProperty = subNode.getProperty("hst:urlmapping");
//                siteMapNodes.put(urlMappingProperty.getValue().getString(), new PageNodeImpl(templateContextBase, subNode));
//            } else {
//                log.debug("hst:sitemapitem sitemap item missing 'hst:ulrmapping' property. " +
//                        "Item not meant for mapping, but only for binaries");
//            }
//        }
//        return siteMapNodes;
//    }
  
}
