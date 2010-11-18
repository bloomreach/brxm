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
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Credentials;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.security.AuthenticationProvider;
import org.hippoecm.hst.security.HstSubject;
import org.hippoecm.hst.security.PolicyContextWrapper;
import org.hippoecm.hst.security.Role;
import org.hippoecm.hst.security.TransientUser;
import org.hippoecm.hst.security.User;

/**
 * SecurityValve
 * 
 * @version $Id$
 */
public class SecurityValve extends AbstractValve {
    
    public static final String DESTINATION_ATTR_NAME = "org.hippoecm.hst.security.servlet.destination";
    
    protected AuthenticationProvider authProvider;
    
    public void setAuthenticationProvider(AuthenticationProvider authProvider) {
        this.authProvider = authProvider;
    }
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HttpServletRequest servletRequest = (HttpServletRequest) context.getServletRequest();
        HttpServletResponse servletResponse = (HttpServletResponse) context.getServletResponse();
        HstRequestContext requestContext = context.getRequestContext();
        ResolvedMount resolvedMount = requestContext.getResolvedMount();
        
        boolean accessAllowed = false;
        boolean authenticationRequired = false;
        String securityExceptionMessage = null;
        
        try {
            checkAccess(servletRequest);
            accessAllowed = true;
        } catch (ContainerSecurityNotAuthenticatedException e) {
            authenticationRequired = true;
            securityExceptionMessage = e.getLocalizedMessage();
        } catch (ContainerSecurityException e) {
            securityExceptionMessage = e.getLocalizedMessage();
        }
        
        if (!accessAllowed) {
            HstLink destinationLink = null;
            String formLoginPage = resolvedMount.getFormLoginPage();
            
            try {
                Mount siteMount = requestContext.getMount(ContainerConstants.MOUNT_ALIAS_SITE);
                
                if (siteMount == null) {
                    siteMount = resolvedMount.getMount();
                }
                
                if (StringUtils.isBlank(formLoginPage)) {
                    formLoginPage = siteMount.getFormLoginPage();
                }
                
                ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
                String pathInfo = (resolvedSiteMapItem == null ? "" : resolvedSiteMapItem.getPathInfo());
                destinationLink = requestContext.getHstLinkCreator().create(pathInfo, siteMount);
            } catch (Exception linkEx) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to create destination link.", linkEx);
                } else if (log.isWarnEnabled()) {
                    log.warn("Failed to create destination link. {}", linkEx.toString());
                }
            }
            
            if (authenticationRequired) {
                if (!StringUtils.isBlank(formLoginPage)) {
                    try {
                        HttpSession httpSession = servletRequest.getSession(true);
                        httpSession.setAttribute(DESTINATION_ATTR_NAME, destinationLink.toUrlForm(requestContext, false));
                        servletResponse.sendRedirect(servletResponse.encodeURL(servletRequest.getContextPath() + formLoginPage));
                        return;
                    } catch (IOException ioe) {
                        if (log.isDebugEnabled()) {
                            log.warn("Failed to redirect to form login page. " + formLoginPage, ioe);
                        } else if (log.isWarnEnabled()) {
                            log.warn("Failed to redirect to form login page. " + formLoginPage + ". {}", ioe.toString());
                        }
                    }
                }
            }
            
            try {
                HttpSession httpSession = servletRequest.getSession(resolvedMount.isSessionStateful());
                
                if (httpSession != null) {
                    httpSession.setAttribute(DESTINATION_ATTR_NAME, destinationLink.toUrlForm(requestContext, false));
                }
                
                servletResponse.sendError(403, securityExceptionMessage);
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
        
        ContainerException ce = (ContainerException) HstSubject.doAsPrivileged(subject, new PrivilegedAction<ContainerException>() {
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
    
    protected void checkAccess(HttpServletRequest servletRequest) throws ContainerSecurityException {
        HstRequestContext requestContext = (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
        ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
        Set<String> roles = null;
        Set<String> users = null;

        boolean secured = (resolvedSiteMapItem != null && resolvedSiteMapItem.isSecured());

        if (secured) {
            roles = resolvedSiteMapItem.getRoles();
            users = resolvedSiteMapItem.getUsers();
        } else {
            ResolvedMount mount = requestContext.getResolvedMount();
            secured = (mount != null && mount.isSecured());
            
            if (secured) {
                roles = mount.getRoles();
                users = mount.getUsers();
            }
        }
        
        if (!secured) {
            if (log.isDebugEnabled()) {
                log.debug("The sitemap item or site mount is non-secured.");
            }
            
            return;
        }
        
        Principal userPrincipal = servletRequest.getUserPrincipal();
        
        if (userPrincipal == null) {
            if (log.isDebugEnabled()) {
                log.debug("The user has not been authenticated yet.");
            }
            
            throw new ContainerSecurityNotAuthenticatedException("Not authenticated yet.");
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
        
        // In a container that supports JACC Providers, the following line will return the container subject.
        Subject subject = (Subject) PolicyContextWrapper.getContext("javax.security.auth.Subject.container");
        
        if (subject == null) {
            HttpSession session = request.getSession(false);
            
            if (session != null) {
                subject = (Subject) session.getAttribute(ContainerConstants.SUBJECT_ATTR_NAME);
            }
        }
        
        if (subject == null) {
            User user = new TransientUser(userPrincipal.getName());
            
            Set<Principal> principals = new HashSet<Principal>();
            principals.add(userPrincipal);
            principals.add(user);
            
            if (authProvider != null) {
                Set<Role> roleSet = authProvider.getRolesByUsername(userPrincipal.getName());
                principals.addAll(roleSet);
            }
            
            Set<Object> pubCred = new HashSet<Object>();
            Set<Object> privCred = new HashSet<Object>();
            
            HttpSession session = request.getSession(false);
            
            if (session != null) {
                Credentials subjectRepoCreds = (Credentials) session.getAttribute(ContainerConstants.SUBJECT_REPO_CREDS_ATTR_NAME);
                
                if (subjectRepoCreds != null) {
                    session.removeAttribute(ContainerConstants.SUBJECT_REPO_CREDS_ATTR_NAME);
                    privCred.add(subjectRepoCreds);
                }
                
            }
            
            subject = new Subject(true, principals, pubCred, privCred);
            session.setAttribute(ContainerConstants.SUBJECT_ATTR_NAME, subject);
        }
        
        if (subject == null) {
            log.warn("Failed to find subjct.");
        } else {
            HstRequestContext requestContext = (HstRequestContext) request.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
            ((HstMutableRequestContext) requestContext).setSubject(subject);
        }
        
        return subject;
    }

}
