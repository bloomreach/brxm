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
package org.hippoecm.hst.core.filters.jcrsession;

import java.io.IOException;

import javax.jcr.Session;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.filters.base.HstBaseFilter;
import org.hippoecm.hst.core.filters.base.HstRequestContext;
import org.hippoecm.hst.jcr.JcrSessionPoolManager;
import org.hippoecm.hst.jcr.ReadOnlyPooledSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrSessionFilter extends HstBaseFilter implements Filter {
    
    private static final Logger log = LoggerFactory.getLogger(JcrSessionFilter.class);
    public static final String JCR_SESSION_POOL_MANAGER_ATTR = JcrSessionFilter.class.getName() + "_ctxAttr";

    private final JcrSessionPoolManager jcrSessionPoolManager = new JcrSessionPoolManager();

    public void init(FilterConfig filterConfig) throws ServletException {
        filterConfig.getServletContext().setAttribute(JCR_SESSION_POOL_MANAGER_ATTR, jcrSessionPoolManager);
    }

    public void destroy() {
        // dispose all pools and logs out all session in all pools
        jcrSessionPoolManager.dispose();
    }

    public void doFilter(ServletRequest req, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        
        super.doFilter(req, response, chain);
        
    }
 
    @Override 
    public void handleRequestForThisFilter(HttpServletRequest request, ServletResponse response, FilterChain chain, HstRequestContext hstRequestContext)
            throws IOException, ServletException {
        
        if( hstRequestContext == null) {
            log.warn("hstRequestContext is null. Cannot process filter");
            chain.doFilter(request, response);
            return;
        }
        long requesttime = System.currentTimeMillis();
        Session session = null;
        try {
            session = jcrSessionPoolManager.getSession(request);
            
            hstRequestContext.setJcrSession(session);
            
            // TODO remove below: below deprecated, kept for now for 
            
            request.setAttribute(HSTHttpAttributes.JCRSESSION_MAPPING_ATTR, session);
            
            // ** //
            
            chain.doFilter(request, response);
        } finally {
            if (session != null && session instanceof ReadOnlyPooledSession) {
                ((ReadOnlyPooledSession) session).getJcrSessionPool().release((request).getSession());
            }
            log.debug("Handling request took " + (System.currentTimeMillis() - requesttime));
        }

    }
         
    
}
