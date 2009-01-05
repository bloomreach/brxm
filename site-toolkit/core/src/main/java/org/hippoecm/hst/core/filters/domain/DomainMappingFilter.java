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
package org.hippoecm.hst.core.filters.domain;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.caching.Cache;
import org.hippoecm.hst.caching.CacheKey;
import org.hippoecm.hst.caching.CacheManagerImpl;
import org.hippoecm.hst.caching.CachedResponse;
import org.hippoecm.hst.caching.CachedResponseImpl;
import org.hippoecm.hst.caching.EventCacheImpl;
import org.hippoecm.hst.caching.NamedEvent;
import org.hippoecm.hst.caching.validity.EventValidity;
import org.hippoecm.hst.caching.validity.SourceValidity;
import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.context.ContextBase;
import org.hippoecm.hst.core.exception.ContextBaseException;
import org.hippoecm.hst.core.filters.base.HstBaseFilter;
import org.hippoecm.hst.core.filters.base.HstRequestContext;
import org.hippoecm.hst.core.mapping.UrlUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomainMappingFilter extends HstBaseFilter implements Filter {
    
    private static final Logger log = LoggerFactory.getLogger(DomainMappingFilter.class);
    /**
     * repository location where the domain mapping is stored
     */
    private static final String REPOSITORY_DOMAIN_MAPPING_LOCATION_PARAM = "domainmapping";
    
    private String domainMappingLocation;
    
    private DomainMapping domainMapping;
    private FilterConfig filterConfig;

    public void init(FilterConfig filterConfig) throws ServletException {
        domainMappingLocation = filterConfig.getInitParameter(REPOSITORY_DOMAIN_MAPPING_LOCATION_PARAM);
        if(domainMappingLocation == null || "".equals(domainMappingLocation)) {
            log.warn("No '{}' defined in web.xml for filter DomainMappingFilter. Skipping domain mapping" , REPOSITORY_DOMAIN_MAPPING_LOCATION_PARAM);
            return;
        }
        this.filterConfig = filterConfig;
    }

    
    public void destroy() {
        
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
       
            super.doFilter(request, response, chain);
          
    }


    @Override
    public void handleRequestForThisFilter(HttpServletRequest req, ServletResponse response, FilterChain chain,
            HstRequestContext hstRequestContext) throws IOException, ServletException {
        
        CacheKey cacheKey = new CacheKey("DomainMapping", this.getClass());
        Cache cache = CacheManagerImpl.getCache(this.getClass().getName(), EventCacheImpl.class.getName());
        
        Object o = cache.get(cacheKey);
        if(o != null && o instanceof CachedResponse) {
            CachedResponse cachedResponse = (CachedResponse)o;
            domainMapping = (DomainMapping)cachedResponse.getResponse();
        } else {
            // if domain mapping is recreated, flush all caches (to avoid cached wrong links)
            CacheManagerImpl.getCaches().clear();
            cache = CacheManagerImpl.getCache(this.getClass().getName(), EventCacheImpl.class.getName());
            domainMapping = new DomainMappingImpl(domainMappingLocation, filterConfig);
            // put it in the eventcache
            SourceValidity sourceValidity = new EventValidity(new NamedEvent(domainMappingLocation));
            CachedResponse cachedResponse = new CachedResponseImpl(sourceValidity, domainMapping);
            cache.store(cacheKey, cachedResponse);
        }
        
        if(!this.domainMapping.isInitialized()) {
            try {
                
                domainMapping.setServletContextPath(req.getContextPath());
                domainMapping.setScheme(req.getScheme());
                domainMapping.setPort(req.getServerPort());
                
                domainMapping.init();
                
            } catch (DomainMappingException e) {            	
                log.error("Exception during initializing domainMapping", e);
                ((HttpServletResponse)response).sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }
        
        Domain matchingDomain = domainMapping.match(req.getServerName());
        
        if(matchingDomain == null) {
            if(domainMapping.getPrimaryDomain() != null) {
                log.warn("No domain matched. Redirecting to primary domain '{}'", domainMapping.getPrimaryDomain().getPattern());
                String redirect = domainMapping.getScheme()+"://"+domainMapping.getPrimaryDomain().getPattern();
                if(domainMapping.isPortInUrl()) {
                    redirect = redirect + ":"+domainMapping.getPort();
                }
                ((HttpServletResponse)response).sendRedirect(redirect);
                return;
            } else {
                log.warn("No domain matched and no primary domain configured. Send error page");
                ((HttpServletResponse)response).sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        } else {
            if(matchingDomain.isRedirect()) {
                log.debug("Matching domain is a redirect. Redirect '{}' --> '{}'", req.getServerName(), matchingDomain.getRedirect());
                ((HttpServletResponse)response).sendRedirect(matchingDomain.getRedirect());
                return;
            } else {
                log.debug("Matching domain found. Wrapping the request.");
                String ctxStrippedUri =  req.getRequestURI().replaceFirst(req.getContextPath(), "");
                RepositoryMapping repositoryMapping = matchingDomain.getRepositoryMapping(ctxStrippedUri, this.filterConfig);
                
                if(repositoryMapping == null) {
                    log.warn("repositoryMapping is null. Cannot process request further");
                    ((HttpServletResponse)response).sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                
                // set the repositoryMapping on the hstRequestContext. This repositoryMapping gives access to the entire domain + domainMapping
                hstRequestContext.setRepositoryMapping(repositoryMapping);
                
                /*
                 * the repository mapping has knowledge of the two main context's that are needed in the hst. One context for the 'hst:configuration' 
                 * and one context for the 'hst content'. We'll create these context here and put them on the hstRequestContext 
                 */ 

                try {
                    ContextBase contentContextBase = new ContextBase(repositoryMapping.getContentPath(), hstRequestContext.getJcrSession());
                    ContextBase hstConfigurationContextBase = new ContextBase(repositoryMapping.getHstConfigPath(), hstRequestContext.getJcrSession());
                    hstRequestContext.setContentContextBase(contentContextBase);
                    hstRequestContext.setHstConfigurationContextBase(hstConfigurationContextBase);
                    
                    // TODO for backwards compatability for now put the context ctx base also seperately on the request attr. Classes needing the 
                    // content ctx base should not use this anymore but access it through the hstRequestContext
                    req.setAttribute(HSTHttpAttributes.CURRENT_CONTENT_CONTEXTBASE_REQ_ATTRIBUTE, contentContextBase);
                } catch (ContextBaseException e) {
                    log.warn("Unable to create context base : {}. Cannot compleet request.", e.getMessage());
                    ((HttpServletResponse)response).sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                
                // set original request on the HstRequestContext and the request uri that matters for the hst
                String hstRequestUri = getHstRequestUri(req.getRequestURI(), repositoryMapping, req.getContextPath());
                hstRequestContext.setHstRequestUri(hstRequestUri);
                hstRequestContext.setRequest(req);
                
                if(hstRequestUri.startsWith("/binaries")) {
                    HttpServletRequest request = new BinariesRequestWrapper(req, repositoryMapping);
                    log.debug("Forwarding orginal request to binaries servlet: '{}' --> '{}'", req.getRequestURI(),request.getRequestURI());
                    RequestDispatcher dispatcher = request.getRequestDispatcher(request.getRequestURI());
                    dispatcher.forward(request, response);
                    return;
                }
                chain.doFilter(req, response);
            }
        }
        
    }
    
    public String getHstRequestUri(String origRequestUri, RepositoryMapping repositoryMapping, String contextPath){
        
            if(repositoryMapping == null ) {
                log.warn("No repository mapping found for request uri '{}'. Try to process request without mapping", origRequestUri);
                return origRequestUri;
            } else {
                
                String uri = origRequestUri;
                uri = uri.substring(contextPath.length());
                // replace the prefix with the repository path in the mapping
                if(repositoryMapping.getPrefix() != null ) {
                    uri = uri.substring(repositoryMapping.getPrefix().length());
                }
                /*
                 * we forward the url without the context path and without the repository prefix.
                 * On the hstRequestContext we have the RepositoryMapping object available
                 */  
                uri = UrlUtilities.decodeUrl(uri);
                log.debug("wrapped request uri to internal uri '{}' --> '{}'", origRequestUri, uri);
                return uri;   
            }
            
        
    }

}
