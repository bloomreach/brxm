/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
 */
public class SecurityValve extends AbstractBaseOrderableValve {

    public static final String DESTINATION_ATTR_NAME = "org.hippoecm.hst.security.servlet.destination";

    public static final String SECURITY_EXCEPTION_ATTR_NAME = "org.hippoecm.hst.security.servlet.exception";

    protected AuthenticationProvider authProvider;

    public void setAuthenticationProvider(AuthenticationProvider authProvider) {
        this.authProvider = authProvider;
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HstRequestContext requestContext = context.getRequestContext();

        // If the request comes for CMS application (e.g, Channel Manager) and it's configured to skip authentication for those requests,
        // just bypass this security valve.
        if (requestContext.isCmsRequest()) {
            if (requestContext.getResolvedMount().getMount().getVirtualHost().getVirtualHosts().isChannelMngrSiteAuthenticationSkipped()) {
                log.debug("Bypassing security valve because the request comes fo a CMS application and it's configured to skip authentication for those requests.");
                context.invokeNext();
                return;
            }
        }

        HttpServletRequest servletRequest =  context.getServletRequest();
        HttpServletResponse servletResponse =  context.getServletResponse();
        ResolvedMount resolvedMount = requestContext.getResolvedMount();

        boolean accessAllowed = false;
        boolean authenticationRequired = false;
        ContainerSecurityException securityException = null;

        try {
            checkAccess(servletRequest);
            accessAllowed = true;
        } catch (ContainerSecurityNotAuthenticatedException e) {
            authenticationRequired = true;
            securityException = e;
        } catch (ContainerSecurityNotAuthorizedException e) {
            securityException = e;
        } catch (ContainerSecurityException e) {
            securityException = e;
        }

        if (!accessAllowed) {
            HstLink destinationLink = null;
            String formLoginPage = resolvedMount.getFormLoginPage();

            Mount destLinkMount = resolvedMount.getMount();

            try {
                if (!destLinkMount.isSite()) {
                    Mount siteMount = requestContext.getMount(ContainerConstants.MOUNT_ALIAS_SITE);

                    if (siteMount != null) {
                        destLinkMount = siteMount;
                    }
                }

                if (StringUtils.isBlank(formLoginPage)) {
                    formLoginPage = destLinkMount.getFormLoginPage();
                }

                ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
                String pathInfo = (resolvedSiteMapItem == null ? "" : resolvedSiteMapItem.getPathInfo());
                destinationLink = requestContext.getHstLinkCreator().create(pathInfo, destLinkMount);
            } catch (Exception linkEx) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to create destination link.", linkEx);
                } else if (log.isWarnEnabled()) {
                    log.warn("Failed to create destination link. {}", linkEx.toString());
                }
            }

            if (authenticationRequired && !StringUtils.isBlank(formLoginPage)) {
                try {
                    HttpSession httpSession = servletRequest.getSession(true);
                    httpSession.setAttribute(DESTINATION_ATTR_NAME, destinationLink.toUrlForm(requestContext, true));
                    httpSession.setAttribute(SECURITY_EXCEPTION_ATTR_NAME, securityException);
                    String formLoginURL = requestContext.getHstLinkCreator().create(formLoginPage, destLinkMount).toUrlForm(requestContext, true);
                    servletResponse.sendRedirect(formLoginURL);
                    return;
                } catch (IOException ioe) {
                    if (log.isDebugEnabled()) {
                        log.warn("Failed to redirect to form login page. " + formLoginPage, ioe);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Failed to redirect to form login page. " + formLoginPage + ". {}", ioe.toString());
                    }
                }
            }

            try {
                HttpSession httpSession = servletRequest.getSession(true);
                httpSession.setAttribute(DESTINATION_ATTR_NAME, destinationLink.toUrlForm(requestContext, true));
                if(authenticationRequired) {
                    servletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, (securityException != null ? securityException.getLocalizedMessage() : null));
                } else {
                    // forbidden thus 403
                    servletResponse.sendError(HttpServletResponse.SC_FORBIDDEN, (securityException != null ? securityException.getLocalizedMessage() : null));
                }
                return;
            } catch (IOException ioe) {
                if (log.isDebugEnabled()) {
                    log.warn("Failed to send error code.", ioe);
                } else if (log.isWarnEnabled()) {
                    log.warn("Failed to send error code. {}", ioe.toString());
                }
            }
        }

        // NOTE: We need to check if subject is injected from somewhere into request context first.
        //       For example, a custom security integration valve could do that before securityValve.
        Subject subject = requestContext.getSubject();

        if (subject == null) {
            subject = getSubject(servletRequest);
        }

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
                } finally {
                    HstSubject.clearSubject();
                }
            }
        }, null);

        if (ce != null) {
            throw ce;
        }
    }

    /**
     * Check authority granted accesses on resolved sitemap item or resolved mount for the request.
     * If no access check is needed or allowed to access, then it returns without <code>ContainerSecurityException</code>.
     * Otherwise, this method should throw a <code>ContainerSecurityException</code>.
     * @param servletRequest
     * @throws ContainerSecurityException
     */
    protected void checkAccess(HttpServletRequest servletRequest) throws ContainerSecurityException {
        HstRequestContext requestContext = (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
        ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
        Set<String> roles = null;
        Set<String> users = null;

        boolean authenticated = (resolvedSiteMapItem != null && resolvedSiteMapItem.isAuthenticated());

        if (authenticated) {
            roles = resolvedSiteMapItem.getRoles();
            users = resolvedSiteMapItem.getUsers();
        } else {
            ResolvedMount mount = requestContext.getResolvedMount();
            authenticated = (mount != null && mount.isAuthenticated());
            
            if (authenticated) {
                roles = mount.getRoles();
                users = mount.getUsers();
            }
        }

        if (!authenticated) {
            log.debug("The sitemap item or site mount is non-authenticated.");

            return;
        }

        Principal userPrincipal = servletRequest.getUserPrincipal();

        if (userPrincipal == null) {
            log.debug("The user has not been authenticated yet.");

            throw new ContainerSecurityNotAuthenticatedException("Not authenticated yet.");
        }

        if (users.isEmpty() && roles.isEmpty()) {
            log.debug("The roles or users are not configured.");
        }

        if (!users.isEmpty()) {
            if (users.contains(userPrincipal.getName())) {
                return;
            }

            log.debug("The user is not assigned to users, {}", users);
        }

        if (!roles.isEmpty()) {
            for (String role : roles) {
                if (servletRequest.isUserInRole(role)) {
                    return;
                }
            }

            log.debug("The user is not assigned to roles, {}", roles);
        }   

        throw new ContainerSecurityNotAuthorizedException("Not authorized.");
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

            if (session != null) {
                session.setAttribute(ContainerConstants.SUBJECT_ATTR_NAME, subject);
            }
        }

        HstRequestContext requestContext = (HstRequestContext) request.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
        ((HstMutableRequestContext) requestContext).setSubject(subject);

        return subject;
    }

}
