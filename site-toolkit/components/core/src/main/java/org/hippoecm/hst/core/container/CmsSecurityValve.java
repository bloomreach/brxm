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
import javax.jcr.Repository;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.MutableMount;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.jcr.LazySession;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.sso.CredentialCipher;

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
public class CmsSecurityValve extends AbstractValve {

    private final static String CMS_USER_ID_ATTR = CmsSecurityValve.class.getName() + ".cms_user_id";

    private Repository repository;

    public void setRepository(Repository repository) {
        this.repository = repository;
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

        log.debug("Request '{}' is invoked from CMS context. Check whether the sso handshake is done.", servletRequest.getRequestURL());
        HttpSession session = servletRequest.getSession(true);

        // Verify that cms user in request header is same as the one associated
        // with the credentials on the HttpSession
        String cmsUser = servletRequest.getHeader("CMS-User");
        if (cmsUser != null) {
            String currentCmsUser = (String) session.getAttribute(CMS_USER_ID_ATTR);
            if (currentCmsUser != null && !currentCmsUser.equals(cmsUser)) {
                session.removeAttribute(CMS_USER_ID_ATTR);
                session.removeAttribute(ContainerConstants.CMS_SSO_REPO_CREDS_ATTR_NAME);
                session.removeAttribute(ContainerConstants.CMS_SSO_AUTHENTICATED);
            }
        }

        if (session.getAttribute(ContainerConstants.CMS_SSO_REPO_CREDS_ATTR_NAME) == null) {
            String key = session.getId();
            String credentialParam = servletRequest.getParameter("cred");
  
            //If there is no secret or credentialParam, add the secret and request for credentialParam by redirecting back to CMS.
            if (credentialParam == null) {
                
                if(!(requestContext.getResolvedMount().getMount() instanceof MutableMount)) {
                    throw new ContainerException("CmsSecurityValve is only available for mounts that are of type MutableMount.");
                }

                StringBuilder destinationURL = new StringBuilder();
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

                String cmsBaseUrl = getBaseUrl(cmsUrl);
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
                // generate key; redirect to cms
                try {
                    String cmsAuthUrl = cmsUrl + "auth?destinationUrl=" + destinationURL.toString() + "&key=" + key;
                    servletResponse.sendRedirect(cmsAuthUrl);
                } catch (UnsupportedEncodingException e) {
                    log.error("Unable to encode the destination url with utf8 encoding " + e.getMessage(), e);
                } catch (IOException e) {
                    log.error("Something gone wrong so stopping valve invocation fall through: " + e.getMessage(), e);
                }

                return;
            } else {
                CredentialCipher credentialCipher = CredentialCipher.getInstance();
                try {
                    Credentials cred = credentialCipher.decryptFromString(key, credentialParam);
                    session.setAttribute(ContainerConstants.CMS_SSO_REPO_CREDS_ATTR_NAME, cred);
                    session.setAttribute(ContainerConstants.CMS_SSO_AUTHENTICATED, true);
                } catch (SignatureException se) {
                    throw new ContainerException(se);
                }
            } 
        } 

        if(isCmsRestRequestContext(servletRequest)) {
            // we are in a request for the REST template composer
            // we need to synchronize on a http session as a jcr session which is tied to it is not thread safe. Also, virtual states will be lost
            // if another thread flushes this session.
            synchronized (session) {
                LazySession lazySession = null;
                try {
                    try {
                        lazySession = (LazySession) repository.login((Credentials) session.getAttribute(ContainerConstants.CMS_SSO_REPO_CREDS_ATTR_NAME));
                        session.setAttribute(CMS_USER_ID_ATTR, lazySession.getUserID());
                    } catch (Exception e) {
                        throw new ContainerException("Failed to create session based on SSO.", e);
                    }
                    // only set the cms based lazySession on the request context when the context is the cms context
                    ((HstMutableRequestContext) requestContext).setSession(lazySession);
                    context.invokeNext();
                } finally {
                    if (lazySession != null) {
                        lazySession.logout();
                    }
                }
            }
        } else {
            context.invokeNext();
        }
    }

    private boolean isCmsRestRequestContext(final HttpServletRequest servletRequest) {
        return Boolean.TRUE.equals(servletRequest.getAttribute(ContainerConstants.CMS_HOST_REST_REQUEST_CONTEXT));
    }

    /**
     * from a url, return everything up to the contextpath : Thus,
     * scheme + host + port
     * @param url the URL string to get the base from
     * @return the scheme + host + port without trailing / at the end
     * @throws ContainerException if the URL does not contain // after the scheme or does not contain a / after the host (+port)
     */
    private String getBaseUrl(final String url) throws ContainerException {
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

}
