package org.hippoecm.hst.core.container;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.MountService;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.MutableResolvedMount;
import org.hippoecm.hst.core.jcr.LazySession;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
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

    private boolean renderHostCheck = true;
    
    private Repository repository;

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setRenderHostCheck(boolean renderHostCheck) {
        this.renderHostCheck = renderHostCheck;
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HttpServletRequest servletRequest = context.getServletRequest();
        HttpServletResponse servletResponse = context.getServletResponse();
        HstRequestContext requestContext = context.getRequestContext();
        if(renderHostCheck && requestContext.getRenderHost() == null) {
            context.invokeNext();
            return;
        } 
        log.debug("Request '{}' is invoked from CMS context. Check whether the sso handshake is done.", servletRequest.getRequestURL());
        ResolvedMount resolvedMount = requestContext.getResolvedMount();
        HttpSession session = servletRequest.getSession(true);

        if (session.getAttribute(ContainerConstants.CMS_SSO_REPO_CREDS_ATTR_NAME) == null) {
            String key = session.getId();
            String credentialParam = servletRequest.getParameter("cred");

            //If there is no secret or credentialParam, add the secret and request for credentialParam by redirecting back to CMS.
            if (credentialParam == null) {
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

                // generate key; redirect to cms
                try {
                    String cmsAuthUrl = null;
                    if (destinationLink != null) {
                        String cmsBaseUrl = requestContext.getContainerConfiguration().getString(ContainerConstants.CMS_LOCATION);
                        if (!cmsBaseUrl.endsWith("/")) {
                            cmsBaseUrl += "/";
                        }
                        cmsAuthUrl = cmsBaseUrl + "auth?destinationUrl=" + URLEncoder.encode(destinationLink.toUrlForm(requestContext, true), "UTF8") + "&key=" + key;
                    } else {
                        log.error("No destinationUrl specified");
                    }
                    
                    if (cmsAuthUrl != null) {
                        //Everything seems to be fine, redirect to destination url and return
                        servletResponse.sendRedirect(cmsAuthUrl);
                        context.completePipeline();
                    } else {
                        log.error("No cmsAuthUrl specified");
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

        // we are in a CMS SSO context. 
        if(resolvedMount instanceof MutableResolvedMount) {
            Mount mount = resolvedMount.getMount();
            if(mount.getType().equals(Mount.PREVIEW_NAME)) {
                // already preview, nothing to do
            } else {
                Mount previewMount = new MountAsPreviewDecorator(mount);
                ((MutableResolvedMount)resolvedMount).setMount(previewMount);
            }
        } else {
            throw new ContainerException("ResolvedMount must be an instance of MutableResolvedMount to be usable in CMS SSO environment. Cannot proceed request");
        }
        
        LazySession lazySession = (LazySession) session.getAttribute(SSO_BASED_SESSION_ATTR_NAME);
        ((HstMutableRequestContext) requestContext).setSession(lazySession);

        context.invokeNext();
    }

    protected void setSSOSession(HttpSession httpSession, Credentials credentials) throws ContainerException {
        LazySession lazySession;
        try {
            lazySession = (LazySession) repository.login(credentials);
            httpSession.setAttribute(SSO_BASED_SESSION_ATTR_NAME, lazySession);
        } catch (Exception e) {
            throw new ContainerException("Failed to create session based on subject.", e);
        }
    }

    private class MountAsPreviewDecorator implements Mount {
        
        private Mount delegatee; 
        private List<String> types;
        private Mount parentAsPreview;
        private Map<String,Mount> childAsPreview = new HashMap<String, Mount>();
        
        public MountAsPreviewDecorator(Mount delegatee) {
            this.delegatee = delegatee;
        }

        @Override
        public HstSite getHstSite() {
            if(delegatee.isPreview()) {
                return delegatee.getHstSite();
            }
            if(delegatee instanceof MountService) {
                return ((MountService)delegatee).getPreviewHstSite();
            } else {
                log.warn("Don't know how to get the preview of a Mount that is not an instanceof MountService. Unable for mount '{}' of type '{}'." +
                		"Return the current mount and not preview version. ", 
                        delegatee.getMountPath(), delegatee.getClass().getName());
                return delegatee.getHstSite();
            }
        }

        @Override
        public String getMountPoint() {
            if(delegatee.isPreview()) {
                return delegatee.getMountPoint();
            }
            return delegatee.getMountPoint();
        }

        @Override
        public String getCanonicalContentPath() {
            if(delegatee.isPreview()) {
                return delegatee.getCanonicalContentPath();
            }
            if(delegatee instanceof MountService) {
                return ((MountService)delegatee).getPreviewCanonicalContentPath();
            } else {
                log.warn("Don't know how to get the canonical content path of a Mount that is not an instanceof MountService. Unable for mount '{}' of type '{}'." +
                        "Return the current mount and not preview version. ", 
                        delegatee.getMountPath(), delegatee.getClass().getName());
                return delegatee.getCanonicalContentPath();
            }
            
        }

        @Override
        public String getContentPath() {
            if(delegatee.isPreview()) {
                return delegatee.getContentPath();
            }
            if(delegatee instanceof MountService) {
                return ((MountService)delegatee).getPreviewContentPath();
            } else {
                log.warn("Don't know how to get the content path of a Mount that is not an instanceof MountService. Unable for mount '{}' of type '{}'." +
                        "Return the current mount and not preview version. ", 
                        delegatee.getMountPath(), delegatee.getClass().getName());
                return delegatee.getContentPath();
            }
        }
        
        @Override
        public boolean isPreview() {
            return true;
        }
        
        @Override
        public Mount getParent() {
            if(delegatee.isPreview()) {
                return delegatee.getParent();
            }
            if(delegatee.getParent() == null) {
                return null;
            }
            parentAsPreview = new MountAsPreviewDecorator(delegatee.getParent());
            return parentAsPreview;
        }

        @Override
        public Mount getChildMount(String name) {
            if(delegatee.isPreview()) {
                return delegatee.getChildMount(name);
            }
            Mount child = childAsPreview.get(name);
            if(child != null) {
                return child;
            }
            if(delegatee.getChildMount(name) == null) {
                return null;
            }
            
            child = new MountAsPreviewDecorator(delegatee.getChildMount(name));
            childAsPreview.put(name, child);
            return child;
        }
        
        // TODO return preview channel id?
        @Override
        public String getChannelId() {
            return delegatee.getChannelId();
        }

        /*
         * below delegate everything to original mount
         */
        
        /*
         * NOTE For getType and getTypes the 'preview' version still returns the value the original mount had. So, for a live mount, it will still
         * be live. This is because otherwise cross mount links will fail (you cannot link from preview to live and vice versa). 
         * 
         * This means, implementation should always check isPreview() to check whether the mount is preview, and not isOfType("preview")
         */
        @Override
        public String getType() {
            return delegatee.getType();
        }

        @Override
        public List<String> getTypes() {
            return delegatee.getTypes();
        }
        
        @Override
        public <T> T getChannelInfo() {
            return (T) delegatee.getChannelInfo();
        }
        
        @Override
        public String getAlias() {
            return delegatee.getAlias();
        }
        
        @Override
        public String[] getDefaultSiteMapItemHandlerIds() {
            return delegatee.getDefaultSiteMapItemHandlerIds();
        }

        @Override
        public String getEmbeddedMountPath() {
            return delegatee.getEmbeddedMountPath();
        }

        @Override
        public String getFormLoginPage() {
            return delegatee.getFormLoginPage();
        }

        @Override
        public String getHomePage() {
            return delegatee.getHomePage();
        }

        @Override
        public HstSiteMapMatcher getHstSiteMapMatcher() {
            return delegatee.getHstSiteMapMatcher();
        }

        @Override
        public String getIdentifier() {
            return delegatee.getIdentifier();
        }

        @Override
        public String getLocale() {
            return delegatee.getLocale();
        }

        @Override
        public String getMountPath() {
            return delegatee.getMountPath();
        }

        @Override
        public Map<String, String> getMountProperties() {
            return delegatee.getMountProperties();
        }

        @Override
        public String getName() {
            return delegatee.getName();
        }

        @Override
        public String getNamedPipeline() {
            return delegatee.getNamedPipeline();
        }

        @Override
        public String getPageNotFound() {
            return delegatee.getPageNotFound();
        }

        @Override
        public int getPort() {
            return delegatee.getPort();
        }

        @Override
        public String getProperty(String name) {
            return delegatee.getProperty(name);
        }

        @Override
        public Set<String> getRoles() {
            return delegatee.getRoles();
        }

        @Override
        public String getScheme() {
            return delegatee.getScheme();
        }

        @Override
        public Set<String> getUsers() {
            return delegatee.getUsers();
        }

        @Override
        public VirtualHost getVirtualHost() {
            return delegatee.getVirtualHost();
        }

        @Override
        public boolean isAuthenticated() {
            return delegatee.isAuthenticated();
        }

        @Override
        public boolean isContextPathInUrl() {
            return delegatee.isContextPathInUrl();
        }

        @Override
        public boolean isMapped() {
            return delegatee.isMapped();
        }

        @Override
        public boolean isOfType(String type) {
            return delegatee.isOfType(type);
        }

        @Override
        public boolean isPortInUrl() {
            return delegatee.isPortInUrl();
        }

        @Override
        public boolean isSessionStateful() {
            return delegatee.isSessionStateful();
        }

        @Override
        public boolean isSite() {
            return delegatee.isSite();
        }

        @Override
        public boolean isSubjectBasedSession() {
            return delegatee.isSubjectBasedSession();
        }

        @Override
        public boolean isVersionInPreviewHeader() {
            return delegatee.isVersionInPreviewHeader();
        }

        @Override
        public String onlyForContextPath() {
            return delegatee.onlyForContextPath();
        }
        
    }
    
}
