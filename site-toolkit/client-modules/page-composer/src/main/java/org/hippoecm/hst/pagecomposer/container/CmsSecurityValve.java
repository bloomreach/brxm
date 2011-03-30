package org.hippoecm.hst.pagecomposer.container;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.container.AbstractValve;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;
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
import org.onehippo.sso.CredentialCipher;

import javax.jcr.Credentials;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Set;

/**
 * CmsSecurityValve responsible for authenticating the user using CMS.
 * <p>
 * When CMS invokes the mount configured with this valve, HST checks if the CMS has provided encrypted credentials or not.
 * If the credentials are _not_ available with the URL, this valve will redirect to the CMS auth URL with a secret.
 * If the credentials are  available with the URL, this valve will try to get the session for the credentials and continue.
 * </p>
 */
public class CmsSecurityValve extends AbstractValve {
    protected AuthenticationProvider authProvider;

    public void setAuthenticationProvider(AuthenticationProvider authProvider) {
        this.authProvider = authProvider;
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HttpServletRequest servletRequest = context.getServletRequest();
        HttpServletResponse servletResponse = context.getServletResponse();
        HstRequestContext requestContext = context.getRequestContext();
        ResolvedMount resolvedMount = requestContext.getResolvedMount();
        //First check if the user has access to the requested mount
        System.out.println("before has access");
        if (!hasAccess(servletRequest)) {
            HstLink destinationLink = null;

            try {
                Mount destLinkMount = resolvedMount.getMount();

                if (!destLinkMount.isSite()) {
                    Mount siteMount = requestContext.getMount(ContainerConstants.MOUNT_ALIAS_SITE);

                    if (siteMount != null) {
                        destLinkMount = siteMount;
                    }
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

            HttpSession session = servletRequest.getSession(false);

            if (session == null || session.getAttribute(ContainerConstants.SUBJECT_REPO_CREDS_ATTR_NAME) == null) {

                String keyParam = servletRequest.getParameter("key");
                String credentialParam = servletRequest.getParameter("cred");

                //If there is no secret or credentialParam, add the secret and request for credentialParam by redirecting back to CMS.
                if (keyParam == null || credentialParam == null) {
                    // generate key; redirect to cms
                    String key = "VK999981";
                    try {
                        String cmsAuthUrl = null;
                        if (destinationLink != null) {
                            String cmsBaseUrl = requestContext.getContainerConfiguration().getString(ContainerConstants.CMS_LOCATION);
                            if(!cmsBaseUrl.endsWith("/")) {
                                cmsBaseUrl += "/";
                            }
                            cmsAuthUrl = cmsBaseUrl + "auth?destinationUrl=" +
                                    URLEncoder.encode(destinationLink.toUrlForm(requestContext, true), "UTF8")
                                    + "&key=" + key;
                        } else {
                            log.error("No destinationUrl specified");
                        }
                        //Everything seems to be fine, redirect to destination url and return
                        servletResponse.sendRedirect(cmsAuthUrl);

                    } catch (UnsupportedEncodingException e) {
                        log.error("Unable to encode the destination url with utf8 encoding" + e.getMessage(), e);
                    } catch (IOException e) {
                        log.error("Something gone wrong so stopping valve invocation fall through:" + e.getMessage(), e);

                    }

                    return;
                } else {
                    System.out.println("decode the request: " + credentialParam);
                    CredentialCipher credentialCipher = CredentialCipher.getInstance();
                    Credentials cred = credentialCipher.decryptFromString(credentialParam);
                    servletRequest.getSession(true).setAttribute(ContainerConstants.SUBJECT_REPO_CREDS_ATTR_NAME, cred);

                }
            }

        }
        Subject subject = getSubject(servletRequest);

        if (subject == null) {
            context.invokeNext();
            return;
        }

        final ValveContext valveContext = context;

        ContainerException ce = HstSubject.doAsPrivileged(subject, new PrivilegedAction<ContainerException>() {
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
     * Checks if the user has access to the specific mount on the request
     *
     * @param servletRequest - The ServletRequest
     * @return true if the user has access.
     */
    protected boolean hasAccess(HttpServletRequest servletRequest) {
        //TODO Vijay: Method with multiple returns - bad - Fix it!!!
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
            if (log.isDebugEnabled()) {
                log.debug("The sitemap item or site mount is non-authenticated.");
            }

            return true;
        }

        Principal userPrincipal = servletRequest.getUserPrincipal();

        if (userPrincipal == null) {
            if (log.isDebugEnabled()) {
                log.debug("The user has not been authenticated yet.");
            }

            return false;
        }

        if (users.isEmpty() && roles.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("The roles or users are not configured.");
            }
        }

        if (!users.isEmpty()) {
            if (users.contains(userPrincipal.getName())) {
                return true;
            }

            if (log.isDebugEnabled()) {
                log.debug("The user is not assigned to users, {}", users);
            }
        }

        if (!roles.isEmpty()) {
            for (String role : roles) {
                if (servletRequest.isUserInRole(role)) {
                    return true;
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("The user is not assigned to roles, {}", roles);
            }
        }

        return false;
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

        if (subject == null) {
            log.warn("Failed to find subject.");
        } else {
            HstRequestContext requestContext = (HstRequestContext) request.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
            ((HstMutableRequestContext) requestContext).setSubject(subject);
        }

        return subject;
    }

}
