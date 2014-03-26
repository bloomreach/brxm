/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.io.UnsupportedEncodingException;
import java.security.SignatureException;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.MutableMount;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.jcr.SessionSecurityDelegation;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.sso.CredentialCipher;

import static org.hippoecm.hst.core.container.ContainerConstants.CMS_USER_ID_ATTR;

/**
 * CmsSecurityValve responsible for authenticating the user using CMS. 
 * 
 * <p> This valve check if the CMS has provided encrypted credentials or not if and only if the 
 * page request is done from the CMS context. This valve checks if the CMS has provided encrypted credentials or not. If the credentials are _not_ available
 * with the URL, this valve will redirect to the CMS auth URL with a secret. If the credentials are  available with the
 * URL, this valve will try to get the session for the credentials and continue. </p>
 * 
 * <p>
 * The check whether the page request originates from a CMS context is done by checking whether the {@link HstRequestContext#getRenderHost()}
 * is not <code>null</code> : A non-null render host implies that the CMS requested the page.
 * </p>
 */
public class CmsSecurityValve extends AbstractBaseOrderableValve {

    private static final String HSTSESSIONID_COOKIE_NAME = "HSTSESSIONID";

    private SessionSecurityDelegation sessionSecurityDelegation;

    public void setSessionSecurityDelegation(SessionSecurityDelegation sessionSecurityDelegation) {
        this.sessionSecurityDelegation = sessionSecurityDelegation;
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HttpServletRequest servletRequest = context.getServletRequest();
        HttpServletResponse servletResponse = context.getServletResponse();
        HstRequestContext requestContext = context.getRequestContext();

        if(servletRequest.getHeader("CMS-User") == null && !requestContext.isCmsRequest()) {
            String ignoredPrefix = requestContext.getResolvedMount().getMatchingIgnoredPrefix();
            if(!StringUtils.isEmpty(ignoredPrefix) && ignoredPrefix.equals(requestContext.getResolvedMount().getResolvedVirtualHost().getVirtualHost().getVirtualHosts().getCmsPreviewPrefix())) {
                // When the ignoredPrefix is not equal cmsPreviewPrefix the request is only allowed in the CMS CONTEXT
                try {
                    servletResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
                } catch (IOException e) {
                    log.error("Exception while sending error response", e);
                }
                return;
            }
            context.invokeNext();
            return;
        }

        log.debug("Request '{}' is invoked from CMS context. Check whether the SSO handshake is done.", servletRequest.getRequestURL());
        HttpSession httpSession = servletRequest.getSession(true);

        // Verify that cms user in request header is same as the one associated
        // with the credentials on the HttpSession
        String cmsUser = servletRequest.getHeader("CMS-User");
        if (cmsUser != null) {
            final String currentCmsUser = getCurrentCmsUser(httpSession);
            if (currentCmsUser != null && !currentCmsUser.equals(cmsUser)) {
                resetAuthentication(httpSession);
            }
        }

        if (!authenticate(servletRequest, servletResponse, requestContext, httpSession)) {
            return;
        }

        updateHstSessionCookie(servletRequest, servletResponse, httpSession);

        // we need to synchronize on a http session as a jcr session which is tied to it is not thread safe. Also, virtual states will be lost
        // if another thread flushes this session.
        synchronized (httpSession) {
            Session jcrSession = null;
            try {
                if (isCmsRestOrPageComposerRequest(servletRequest)) {
                    jcrSession = createCmsChannelManagerRestSession(httpSession);
                } else {
                    // request preview website, for example in channel manager. The request is not
                    // a REST call
                    if (sessionSecurityDelegation.sessionSecurityDelegationEnabled()) {
                        jcrSession = createCmsPreviewSession(httpSession);
                    } else {
                        // do not yet create a session. just use the one that the HST container will create later
                    }
                }

                if (jcrSession != null) {
                    ((HstMutableRequestContext) requestContext).setSession(jcrSession);
                }
                context.invokeNext();
            } catch (LoginException e) {
                // the credentials of the current CMS user have changed, so reset the current authentication
                log.info("Credentials of CMS user '{}' are no longer valid, resetting its HTTP session and starting the SSO handshake again.", getCurrentCmsUser(httpSession));
                resetAuthentication(httpSession);
                authenticate(servletRequest, servletResponse, requestContext, httpSession);
                return;
            } finally {
                if (jcrSession != null) {
                    jcrSession.logout();
                }
            }
        }
    }

    private static String getCurrentCmsUser(final HttpSession httpSession) {
        return (String) httpSession.getAttribute(CMS_USER_ID_ATTR);
    }

    private static boolean authenticate(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse, final HstRequestContext requestContext, final HttpSession httpSession) throws ContainerException {
        if (!isAuthenticated(httpSession)) {
            final String key = httpSession.getId();
            final String encryptedCredentials = servletRequest.getParameter("cred");

            if (encryptedCredentials == null) {
                // no secret or credentials; if possible, add the secret and request the
                // encrypted credentials by redirecting back to CMS

                if(!(requestContext.getResolvedMount().getMount() instanceof MutableMount)) {
                    throw new ContainerException("CmsSecurityValve is only available for mounts that are of type MutableMount.");
                }

                final String method = servletRequest.getMethod();
                if (!"GET".equals(method) && !"HEAD".equals(method)) {
                    try {
                        servletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    } catch (IOException e) {
                        log.error("Unable to send unauthorized (404) response to client", e);
                    }
                } else {
                    redirectToCms(servletRequest, servletResponse, requestContext, key);
                }
                return false;
            } else {
                storeAuthentication(httpSession, key, encryptedCredentials);
            }
        }
        return true;
    }

    private static boolean isAuthenticated(final HttpSession httpSession) {
        return httpSession.getAttribute(ContainerConstants.CMS_SSO_REPO_CREDS_ATTR_NAME) != null;
    }

    private static void storeAuthentication(final HttpSession httpSession, final String key, final String credentialParam) throws ContainerException {
        final CredentialCipher credentialCipher = CredentialCipher.getInstance();
        try {
            final Credentials cred = credentialCipher.decryptFromString(key, credentialParam);
            httpSession.setAttribute(CMS_USER_ID_ATTR, ((SimpleCredentials) cred).getUserID());
            httpSession.setAttribute(ContainerConstants.CMS_SSO_REPO_CREDS_ATTR_NAME, cred);
            httpSession.setAttribute(ContainerConstants.CMS_SSO_AUTHENTICATED, true);
        } catch (SignatureException se) {
            throw new ContainerException(se);
        }
    }

    private static void resetAuthentication(final HttpSession httpSession) {
        httpSession.removeAttribute(CMS_USER_ID_ATTR);
        httpSession.removeAttribute(ContainerConstants.CMS_SSO_REPO_CREDS_ATTR_NAME);
        httpSession.removeAttribute(ContainerConstants.CMS_SSO_AUTHENTICATED);
    }

    private static void redirectToCms(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse, final HstRequestContext requestContext, final String key) throws ContainerException {
        try {
            final String cmsAuthUrl = createCmsAuthenticationUrl(servletRequest, requestContext, key);
            servletResponse.sendRedirect(cmsAuthUrl);
        } catch (UnsupportedEncodingException e) {
            log.error("Unable to encode the destination url with UTF8 encoding " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("Something gone wrong so stopping valve invocation fall through: " + e.getMessage(), e);
        }
    }

    private static String createCmsAuthenticationUrl(final HttpServletRequest servletRequest, final HstRequestContext requestContext, final String key) throws ContainerException {
        String cmsUrl = servletRequest.getHeader("Referer");
        if (cmsUrl == null) {
            throw new ContainerException("Could not establish a SSO between CMS & site application because there is no 'Referer' header on the request");
        }
        if (cmsUrl.indexOf("?") > -1) {
            // we do not need the query String for the cms url.
            cmsUrl = cmsUrl.substring(0, cmsUrl.indexOf("?"));
        }
        if (!cmsUrl.endsWith("/")) {
            cmsUrl += "/";
        }

        final StringBuilder destinationURL = new StringBuilder();
        final String cmsBaseUrl = getBaseUrl(cmsUrl);
        destinationURL.append(cmsBaseUrl);

        // we append the request uri including the context path (normally this is /site/...)
        destinationURL.append(servletRequest.getRequestURI());

        if(requestContext.getPathSuffix() != null) {
            String subPathDelimeter = requestContext.getVirtualHost().getVirtualHosts().getHstManager().getPathSuffixDelimiter();
            destinationURL.append(subPathDelimeter).append(requestContext.getPathSuffix());
        }

        String qString =  servletRequest.getQueryString();
        if(qString != null) {
            destinationURL.append("?").append(qString);
        }

        return cmsUrl + "auth?destinationUrl=" + destinationURL.toString() + "&key=" + key;
    }

    private static void updateHstSessionCookie(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse, final HttpSession session) {
        final Cookie[] cookies = servletRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (HSTSESSIONID_COOKIE_NAME.equals(cookie.getName()) && session.getId().equals(cookie.getValue())) {
                    // HSTSESSIONID_COOKIE_NAME cookie already present and correct
                    return;
                }
            }
        }
        // (java) session cookie may not be available to the client-side javascript code,
        // as the cookie may be secured by the container (useHttpOnly=true).
        Cookie sessionIdCookie = new Cookie(HSTSESSIONID_COOKIE_NAME, session.getId());
        sessionIdCookie.setMaxAge(-1);
        servletResponse.addCookie(sessionIdCookie);
    }

    private static boolean isCmsRestOrPageComposerRequest(final HttpServletRequest servletRequest) {
        return Boolean.TRUE.equals(servletRequest.getAttribute(ContainerConstants.CMS_REST_REQUEST_CONTEXT));
    }

    /**
     * from a url, return everything up to the contextpath : Thus,
     * scheme + host + port
     * @param url the URL string to get the base from
     * @return the scheme + host + port without trailing / at the end
     * @throws ContainerException if the URL does not contain // after the scheme or does not contain a / after the host (+port)
     */
    private static String getBaseUrl(final String url) throws ContainerException {
        int indexOfDoubleSlash = url.indexOf("//");
        if (indexOfDoubleSlash == -1) {
            throw new ContainerException("Could not establish a SSO between CMS & site application because cannot get a cms url from the referer '"+url+"'");
        }
        int indexOfRequestURI = url.substring(indexOfDoubleSlash +2).indexOf("/") + indexOfDoubleSlash +2;
        if (indexOfRequestURI == -1) {
            throw new ContainerException("Could not establish a SSO between CMS & site application because cannot get a cms url from the referer '"+url+"'");
        }
        return url.substring(0, indexOfRequestURI);
    }

    private Session createCmsChannelManagerRestSession(final HttpSession httpSession) throws LoginException, ContainerException {
        long start = System.currentTimeMillis();
        try {
            final Credentials credentials = (Credentials) httpSession.getAttribute(ContainerConstants.CMS_SSO_REPO_CREDS_ATTR_NAME);
            // This returns a plain session for credentials where access is not merged with for example preview user session
            // For cms rest calls to page composer or cms-rest we must *NEVER* combine the security with other sessions
            Session session = sessionSecurityDelegation.getDelegatedSession(credentials);
            log.debug("Acquiring cms rest session took '{}' ms.", (System.currentTimeMillis() - start));
            return session;
        } catch (LoginException e) {
            throw e;
        } catch (Exception e) {
            throw new ContainerException("Failed to create session based on SSO.", e);
        }
    }

    private Session createCmsPreviewSession(final HttpSession httpSession) throws LoginException, ContainerException {
        long start = System.currentTimeMillis();
        Credentials cmsUserCred = (Credentials) httpSession.getAttribute(ContainerConstants.CMS_SSO_REPO_CREDS_ATTR_NAME);
        try {
            Session session = sessionSecurityDelegation.createPreviewSecurityDelegate(cmsUserCred, false);
            log.debug("Acquiring security delegate session took '{}' ms.", (System.currentTimeMillis() - start));
            return session;
        } catch (LoginException e) {
            throw e;
        } catch (Exception e) {
            throw new ContainerException("Failed to create Session based on SSO.", e);
        }
    }
}
