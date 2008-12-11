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

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.context.ContextBase;
import org.hippoecm.hst.core.exception.ContextBaseException;
import org.hippoecm.hst.core.filters.base.HstBaseFilter;
import org.hippoecm.hst.core.filters.base.HstRequestContext;
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

    public void init(FilterConfig filterConfig) throws ServletException {
        domainMappingLocation = filterConfig.getInitParameter(REPOSITORY_DOMAIN_MAPPING_LOCATION_PARAM);
        if(domainMappingLocation == null || "".equals(domainMappingLocation)) {
            log.warn("No '{}' defined in web.xml for filter DomainMappingFilter. Skipping domain mapping" , REPOSITORY_DOMAIN_MAPPING_LOCATION_PARAM);
            return;
        }
        domainMapping = new DomainMappingImpl(domainMappingLocation, filterConfig);
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
      
        if(!this.domainMapping.isInitialized() || true ) {
            try {
                domainMapping.init();
                domainMapping.setServletContextPath(req.getContextPath());
                domainMapping.setServletContextPathInUrl(true);
            } catch (DomainMappingException e) {
                log.warn("Exception during initializing domainMapping", e.getMessage());
                log.debug("Exception during initializing domainMapping", e);
            }
        }
        Domain matchingDomain = domainMapping.match(req.getServerName());
        
        if(matchingDomain == null) {
            if(domainMapping.getPrimaryDomain() != null) {
                log.warn("No domain matched. Redirecting to primary domain '{}'", domainMapping.getPrimaryDomain().getPattern());
                String redirect = req.getScheme()+"://"+domainMapping.getPrimaryDomain().getPattern();
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
                RepositoryMapping repositoryMapping = matchingDomain.getRepositoryMapping(ctxStrippedUri);
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
                
                HttpServletRequest request = new DomainMappingRequestWrapper((HttpServletRequest)req, repositoryMapping);
                if(request.getRequestURI().startsWith("/binaries")) {
                    log.debug("Forwarding orginal request to binaries servlet: '{}' --> '{}'", req.getRequestURI(),request.getRequestURI());
                    RequestDispatcher dispatcher = request.getRequestDispatcher(request.getRequestURI());
                    dispatcher.forward(request, response);
                    return;
                }
                chain.doFilter(request, response);
            }
        }
        
    }

}
