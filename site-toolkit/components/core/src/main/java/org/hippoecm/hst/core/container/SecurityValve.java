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
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedSiteMount;
import org.hippoecm.hst.security.AuthenticationProvider;
import org.hippoecm.hst.security.Role;
import org.hippoecm.hst.security.TransientUser;
import org.hippoecm.hst.security.User;

/**
 * SecurityValve
 * 
 * @version $Id$
 */
public class SecurityValve extends AbstractValve {
    
    protected AuthenticationProvider authProvider;
    
    public void setAuthenticationProvider(AuthenticationProvider authProvider) {
        this.authProvider = authProvider;
    }
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HttpServletRequest servletRequest = (HttpServletRequest) context.getServletRequest();
        HttpServletResponse servletResponse = (HttpServletResponse) context.getServletResponse();
        
        try {
            checkAuthorized(servletRequest);
        } catch (ContainerSecurityException e) {
            try {
                servletResponse.sendError(403, e.getMessage());
                return;
            } catch (IOException ioe) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to send error code.", ioe);
                } else if (log.isWarnEnabled()) {
                    log.warn("Failed to send error code. {}", ioe.toString());
                }
            }
        }
        
        Subject subject = getSubject(servletRequest);
        
        if (subject == null) {
            context.invokeNext();
            return;
        }

        final ValveContext valveContext = context;
        
        ContainerException ce = (ContainerException) Subject.doAsPrivileged(subject, new PrivilegedAction<ContainerException>() {
            public ContainerException run() {
                try {
                    valveContext.invokeNext();
                    return null;
                } catch (ContainerException e) {
                    return e;
                }
            }
        }, null);

        if (ce != null) {
            throw ce;
        }
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
    
    protected Subject getSubject(HttpServletRequest request) {
        Principal userPrincipal = request.getUserPrincipal();
        
        if (userPrincipal == null) {
            return null;
        }
        
        Subject subject = null;
        
        if (userPrincipal instanceof User) {
            subject = ((User) userPrincipal).getSubject();
            
            if (subject == null) {
                log.warn("Subject is not found in the user principal.");
            }
        }
        
        if (subject == null) {
            HttpSession session = request.getSession(false);
            
            if (session != null) {
                subject = (Subject) session.getAttribute(ContainerConstants.SUBJECT_ATTR_NAME);
            }
        }
        
        if (subject == null) {
            if (authProvider == null) {
                log.warn("Cannot find authentication provider component.");
                User user = new TransientUser(userPrincipal.getName());
                Set<Principal> principals = new HashSet<Principal>();
                principals.add(userPrincipal);
                principals.add(user);
                Set<Object> pubCred = Collections.emptySet();
                Set<Object> privCred = Collections.emptySet();
                subject = new Subject(true, principals, pubCred, privCred);
            } else {
                User user = new TransientUser(userPrincipal.getName());
                Set<Role> roleSet = authProvider.getRolesByUsername(userPrincipal.getName());
                Set<Principal> principals = new HashSet<Principal>();
                principals.add(userPrincipal);
                principals.add(user);
                principals.addAll(roleSet);
                Set<Object> pubCred = Collections.emptySet();
                Set<Object> privCred = Collections.emptySet();
                subject = new Subject(true, principals, pubCred, privCred);
            }
            
            HttpSession session = request.getSession(false);
            
            if (session != null) {
                session.setAttribute(ContainerConstants.SUBJECT_ATTR_NAME, subject);
            }
        }
        
        if (subject == null) {
            log.warn("Failed to find subjct.");
        }
        
        return subject;
    }

}
