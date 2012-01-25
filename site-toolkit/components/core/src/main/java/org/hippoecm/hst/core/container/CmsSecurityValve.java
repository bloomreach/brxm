/*
 *  Copyright 2011 Hippo.
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
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.MutableMount;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.jcr.LazySession;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.HstRequestUtils;
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
    private static final String SSO_BASED_SESSION_ATTR_NAME = CmsSecurityValve.class.getName() + ".jcrSession";

    private final static String DEFAULT_PATH_SUFFIX = "./";
    
    private Repository repository;
    private String pathSuffixDelimiter = DEFAULT_PATH_SUFFIX;
    // default max refresh interval for refresh on lazy session is 5 minutes
    protected long maxRefreshIntervalOnLazySession = 300000;
    
    @SuppressWarnings("deprecation")
    private final static String CMS_LOCATION = ContainerConstants.CMS_LOCATION;
    
    public void setMaxRefreshIntervalOnLazySession(long maxRefreshIntervalOnLazySession) {
        this.maxRefreshIntervalOnLazySession = maxRefreshIntervalOnLazySession;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setPathSuffixDelimiter(String pathSuffixDelimiter) {
        this.pathSuffixDelimiter = pathSuffixDelimiter;
    }
    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HttpServletRequest servletRequest = context.getServletRequest();
        HttpServletResponse servletResponse = context.getServletResponse();
        HstRequestContext requestContext = context.getRequestContext();
        /*
         * we invoke the next valve if the call is not from the cms. A call is *not* from the cms template composer if:
         * 1) The renderHost == null AND
         * 2) ContainerConstants.CMS_HOST_CONTEXT attribute is not TRUE
         */
        if(requestContext.getRenderHost() == null && !Boolean.TRUE.equals(servletRequest.getAttribute(ContainerConstants.CMS_HOST_CONTEXT))) {
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
        // with the jcr session, already bound to the http session.
        String cmsUser = servletRequest.getHeader("CMS-User");
        if (cmsUser != null) {
            Session jcrSession = (Session) session.getAttribute(SSO_BASED_SESSION_ATTR_NAME);
            if (jcrSession != null && !jcrSession.getUserID().equals(cmsUser)) {
                jcrSession.logout();

                session.setAttribute(SSO_BASED_SESSION_ATTR_NAME, null);
                session.setAttribute(ContainerConstants.CMS_SSO_REPO_CREDS_ATTR_NAME, null);
                session.setAttribute(ContainerConstants.CMS_SSO_AUTHENTICATED, false);
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
                
                MutableMount mount = (MutableMount)requestContext.getResolvedMount().getMount();
                StringBuilder destinationURL = new StringBuilder();
                destinationURL.append(servletRequest.getScheme()).append("://");
                destinationURL.append(HstRequestUtils.getRequestHosts(servletRequest, false)[0]);
                // for SSO, we also go through the CMS. We always need the contextpath in the requests to the HST
                //destinationURL.append(servletRequest.getContextPath());
               
                destinationURL.append(servletRequest.getRequestURI());
               
                if(requestContext.getPathSuffix() != null) {
                    destinationURL.append(pathSuffixDelimiter).append(requestContext.getPathSuffix());
                }
                
                String qString =  servletRequest.getQueryString();
                if(qString != null) {
                    destinationURL.append("?").append(qString);
                }
                
                // generate key; redirect to cms
                try {
                    String cmsAuthUrl = null;
                    String cmsBaseUrl;
                    if (StringUtils.isEmpty(mount.getCmsLocation())) {
                        log.warn("Using deprecated hst-config.property 'cms.location' . Configure the correct 'hst:cmslocation' property on the hst:virtualhostgroup to get rid of this warning");
                        cmsBaseUrl = requestContext.getContainerConfiguration().getString(CMS_LOCATION);
                    } else {
                        cmsBaseUrl = mount.getCmsLocation();
                    }
                    
                    if(cmsBaseUrl == null) {
                        log.error("No cmsAuthUrl specified");
                    } else {
                        if (!cmsBaseUrl.endsWith("/")) {
                            cmsBaseUrl += "/";
                        }
                        cmsAuthUrl = cmsBaseUrl + "auth?destinationUrl=" + destinationURL.toString() + "&key=" + key;
                        servletResponse.sendRedirect(cmsAuthUrl);
                    }  
                } catch (UnsupportedEncodingException e) {
                    log.error("Unable to encode the destination url with utf8 encoding" + e.getMessage(), e);
                } catch (IOException e) {
                    log.error("Something gone wrong so stopping valve invocation fall through:" + e.getMessage(), e);
                }

                return;
            } else {
                CredentialCipher credentialCipher = CredentialCipher.getInstance();
                try {
                    Credentials cred = credentialCipher.decryptFromString(key, credentialParam);
                    session.setAttribute(ContainerConstants.CMS_SSO_REPO_CREDS_ATTR_NAME, cred);
                    session.setAttribute(ContainerConstants.CMS_SSO_AUTHENTICATED, true);
                    setSSOSession(session, cred);
                } catch (SignatureException se) {
                    throw new ContainerException(se);
                }
            } 
        } 

        // we need to synchronize on a http session as a jcr session which is tied to it is not thread safe. Also, virtual states will be lost
        // if another thread flushes this session. 
        if(Boolean.TRUE.equals(servletRequest.getAttribute(ContainerConstants.CMS_HOST_CONTEXT))) {
            // we are in a request for the REST template composer 
            synchronized (session) {
                LazySession lazySession = (LazySession) session.getAttribute(SSO_BASED_SESSION_ATTR_NAME);
                if(!lazySession.isLive()) {
                    log.debug("SSO jcr session is not live. Try to get a new one.");
                    setSSOSession(session, (Credentials)session.getAttribute(ContainerConstants.CMS_SSO_REPO_CREDS_ATTR_NAME));
                    lazySession = (LazySession) session.getAttribute(SSO_BASED_SESSION_ATTR_NAME);
                }
                try {
                    if (maxRefreshIntervalOnLazySession > 0L) {
                        // First check whether the maxRefreshInterval has passed
                        if (System.currentTimeMillis() - lazySession.lastRefreshed() > maxRefreshIntervalOnLazySession) {
                            lazySession.refresh(false);
                        } else {
                            // if not refreshed, check whether there was a repository event that marked the lazySession as 'dirty'
                            long refreshPendingTimeMillis = lazySession.getRefreshPendingAfter();
                            if (refreshPendingTimeMillis > 0L && lazySession.lastRefreshed() < refreshPendingTimeMillis) {
                                lazySession.refresh(false);
                            }
                        }
                    } else {
                        // if maxRefreshIntervalOnLazySession <= 0, we always instantly refresh. This is bad for performance
                        lazySession.refresh(false);
                    }
                } catch (RepositoryException e) {
                    throw new ContainerException("Failed to refresh jcr session.", e);
                }
                ((HstMutableRequestContext) requestContext).setSession(lazySession);
                context.invokeNext();
            }
            // only set the cms based lazySession on the request context when the context is the cms context
        } else {
            context.invokeNext();
        }
    }

    protected void setSSOSession(HttpSession httpSession, Credentials credentials) throws ContainerException {
        LazySession lazySession;
        try {
            lazySession = (LazySession) repository.login(credentials);
            httpSession.setAttribute(SSO_BASED_SESSION_ATTR_NAME, lazySession);
        } catch (Exception e) {
            throw new ContainerException("Failed to create session based on SSO.", e);
        }
    }

}
