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
package org.hippoecm.hst.core.container;

import java.io.IOException;
import java.security.Principal;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedSiteMount;

/**
 * SecurityValve
 * 
 * @version $Id$
 */
public class SecurityValve extends AbstractValve {
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HttpServletRequest servletRequest = (HttpServletRequest) context.getServletRequest();
        HttpServletResponse servletResponse = (HttpServletResponse) context.getServletResponse();
        
        try {
            checkAuthorized(servletRequest);
        } catch (ContainerSecurityException e) {
            try {
                servletResponse.sendError(403, e.getMessage());
            } catch (IOException ioe) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to send error code.", ioe);
                } else if (log.isWarnEnabled()) {
                    log.warn("Failed to send error code. {}", ioe.toString());
                }
            }
        }
        
        // continue
        context.invokeNext();
    }
    
    protected void checkAuthorized(HttpServletRequest servletRequest) throws ContainerSecurityException {
        HstRequestContext requestContext = (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
        ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
        Set<String> roles = null;
        Set<String> users = null;

        boolean secured = (resolvedSiteMapItem != null && resolvedSiteMapItem.isSecured());

        if (secured) {
            roles = resolvedSiteMapItem.getRoles();
            users = resolvedSiteMapItem.getUsers();
        } else {
            ResolvedSiteMount mount = requestContext.getResolvedSiteMount();
            secured = (mount != null && mount.isSecured());
            
            if (secured) {
                roles = mount.getRoles();
                users = mount.getUsers();
            }
        }
        
        if (!secured) {
            if (log.isDebugEnabled()) {
                log.debug("The sitemap item is non-secured.");
            }
            
            return;
        }
        
        Principal userPrincipal = servletRequest.getUserPrincipal();
        
        if (userPrincipal == null) {
            if (log.isDebugEnabled()) {
                log.debug("The user has not been authenticated yet.");
            }
            
            throw new ContainerSecurityException("Not authenticated yet.");
        }
        
        if (users.isEmpty() && roles.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("The roles or users are not configured.");
            }
            
            throw new ContainerSecurityException("No role or user is not configured for the secured content.");
        }
        
        if (!users.isEmpty()) {
            if (users.contains(userPrincipal.getName())) {
                return;
            }
            
            if (log.isDebugEnabled()) {
                log.debug("The user is not assigned to users, {}", users);
            }
        }
        
        if (!roles.isEmpty()) {
            for (String role : roles) {
                if (servletRequest.isUserInRole(role)) {
                    return;
                }
            }
            
            if (log.isDebugEnabled()) {
                log.debug("The user is not assigned to roles, {}", roles);
            }
        }
        
        throw new ContainerSecurityException("Not authorized.");
    }
    
}
