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
package org.hippoecm.hst.core.linking;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.request.ResolvedMountImpl;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.HstSiteMapUtils;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstLinkImpl implements HstLink {

    private final static Logger log = LoggerFactory.getLogger(HstLinkImpl.class);

    private String path;
    private String subPath;
    private Mount mount;
    private HstSiteMapItem siteMapItem;
    private boolean containerResource;
    private boolean notFound = false;
    private final static String[] FULLY_QUALIFIED_URL_PREFIXES = {"//", "http:", "https:"};
    
    public HstLinkImpl(String path, Mount mount) {
        this(path, mount,false);
    }
    
    public HstLinkImpl(String path, Mount mount, boolean containerResource) {
        this(path, mount,null,containerResource);
    }

    public HstLinkImpl(String path, Mount mount, HstSiteMapItem siteMapItem, boolean containerResource) {
        this(path, mount,siteMapItem,containerResource, true);
    }


    public HstLinkImpl(String path, Mount mount, boolean containerResource, boolean rewriteHomePagePath) {
        this(path, mount, null,containerResource, rewriteHomePagePath);
    }

    public HstLinkImpl(String path, Mount mount, HstSiteMapItem siteMapItem, boolean containerResource, boolean rewriteHomePagePath) {
        if (path != null && path.startsWith("//")) {
            // fully qualified path
            this.path = "//" + PathUtils.normalizePath(path);
        } else {
            this.path = PathUtils.normalizePath(path);
        }
        this.mount = mount;
        this.siteMapItem = siteMapItem;
        this.containerResource = containerResource;
        if(rewriteHomePagePath) {
            // check whether path is equal to homepage : if so, replace with ""
            if(this.path != null && !containerResource && mount != null) {
                // get the homePagePath : the mount.getHomePage can be the homepage path OR the sitemap item refId
                // with HstSiteMapUtils.getPath we get the homepage path regardless whether mount.getHomePage() is the path of the refId
                String homePagePath = HstSiteMapUtils.getPath(mount, mount.getHomePage());
                if(path.equals(homePagePath) || ("/"+path).equals(homePagePath)) {
                    // homepage link : Set path to "";
                    this.path = "";
                }
            }
        }
    }

    public Mount getMount() {
        return mount;
    }
    
    public String getPath() {
        return this.path;
    }
    
    public void setPath(String path) {
        this.path = PathUtils.normalizePath(path);
    }
    

    public String getSubPath() {
        return subPath;
    }

    public void setSubPath(String subPath) {
        this.subPath = subPath;
    }

    
    public boolean getContainerResource() {
        return this.containerResource;
    }

    public void setContainerResource(boolean containerResource) {
       this.containerResource = containerResource;
    }
    
    public String[] getPathElements() {
        if(this.path == null) {
            return null;
        }
        return this.path.split("/");
    }


    public String toUrlForm(HstRequestContext requestContext, boolean fullyQualified) {

        if (path == null) {
            log.warn("Unable to rewrite link. Return EVAL_PAGE");
            return null;
        }

        for (String s : FULLY_QUALIFIED_URL_PREFIXES) {
            if (path.startsWith(s)) {
                return path;
            }
        }

        Mount requestMount = requestContext.getResolvedMount().getMount();

        // check if we need to set an explicit contextPath 
        String explicitContextPath = null;
        if (requestContext.isCmsRequest()) {
            if (mount != null && mount.getContextPath() != null) {
                explicitContextPath = mount.getContextPath();
            } else {
                // mount is null or contextpath agnostic: use contextpath from current request
                explicitContextPath = requestContext.getServletRequest().getContextPath();
            }
        } else if (mount != null && requestMount != mount) {
            if (mount.isContextPathInUrl() && mount.getContextPath() != null) {
                explicitContextPath = mount.getContextPath();
            }
        }

        String urlString;
        if (this.containerResource) {
            HstURL hstUrl = requestContext.getURLFactory().createURL(HstURL.RESOURCE_TYPE, ContainerConstants.CONTAINER_REFERENCE_NAMESPACE, null, requestContext, explicitContextPath);
            hstUrl.setResourceID(path);
            urlString = hstUrl.toString();
        } else {

            HstManager mngr = HstServices.getComponentManager().getComponent(HstManager.class.getName());
            String subPathDelimeter =  mngr.getPathSuffixDelimiter();
            if (subPath != null) {
                // subPath is allowed to be empty ""
                path += subPathDelimeter + subPath;
            } else if (mount != null && !mount.isSite()) {
                // mount is configured to support subPath: Include the PATH_SUBPATH_DELIMITER for locations that that would be exclused by virtualhosts configuration
                // like resources ending on .jpg or .pdf etc 
                if (mount.getVirtualHost().getVirtualHosts().isExcluded(path)) {
                    // path should not be excluded for hst request processing because for example it is a REST call for a binary. Add the PATH_SUBPATH_DELIMITER
                    // to avoid this
                    path += subPathDelimeter;
                }
            }

            HstContainerURL navURL = requestContext.getContainerURLProvider().createURL(mount, requestContext.getBaseURL(), path);
            urlString = requestContext.getURLFactory().createURL(HstURL.RENDER_TYPE, null, navURL, requestContext, explicitContextPath).toString();
            if (StringUtils.isEmpty(path) && StringUtils.isEmpty(urlString)) {
                // homepage with no contextpath : replace urlString with /
                urlString = "/";
            }
        }
        
        /*
         * we create a url including http when the Mount is not null and one of the lines below is true
         * 0) requestContext.isFullyQualifiedURLs() = true
         * 1) external = true
         * 2) The virtualhost from current request Mount is different than the Mount for this link
         * 3) The portnumber is in the url, and the current request Mount has a different portnumber than the Mount for this link
         */
        String renderHost = null;

        if (mount != null) {
            if (requestContext.isCmsRequest()) {
                // check whether the urlString is equal to the contextPath of the mount. If so,
                // we need to append an extra / to the urlString : This is to avoid a link like 
                // '/site' in cms preview context: It must there be '/site/'
                if (urlString.equals(requestMount.getContextPath())) {
                    urlString += "/";
                }
            }
            if (requestContext.getRenderHost() != null && requestMount != mount) {
                // the link is cross-domain, so set the render host
                renderHost = mount.getVirtualHost().getHostName();
            } else if (!requestContext.isCmsRequest()) {
                // the above !requestContext.isCmsRequest() check is to avoid fully qualified links in CMS channel manager:
                // for the cms, we never want a fully qualified URLs for links as that is managed through the 'renderHost'
                HstLinkImplCharacteristics hstLinkImplCharacteristics = new HstLinkImplCharacteristics(requestContext, fullyQualified);
                if (hstLinkImplCharacteristics.isFullyQualified()) {

                    final String scheme;
                    if (hstLinkImplCharacteristics.getScheme().equals(hstLinkImplCharacteristics.SCHEME_AGNOSTIC)) {
                        // use scheme from request
                        scheme = HstRequestUtils.getFarthestRequestScheme(requestContext.getServletRequest());
                    } else {
                        scheme = hstLinkImplCharacteristics.getScheme();
                    }

                    String host = scheme + "://" + mount.getVirtualHost().getHostName();
                    if (mount.isPortInUrl()) {
                        int port = mount.getPort();
                        if (port == 0) {
                            // the Mount is port agnostic. Take port from current container url
                            port = requestContext.getBaseURL().getPortNumber();
                        }
                        if (port == 80 || port == 443) {
                            // do not include default ports
                        } else {
                            host += ":" + port;
                        }
                    }

                    urlString = host + urlString;
                }
            }
        }

        if (renderHost != null && !this.containerResource) {
            // we need to append the render host as a request parameter but it is not needed for resources
            if (urlString.contains("?")) {
                urlString += "&";
            } else {
                urlString += "?";
            }
            urlString += ContainerConstants.RENDERING_HOST + '=' + renderHost;
        }

        return urlString;
    }

    public boolean isNotFound() {
        return notFound;
    }

    public void setNotFound(boolean notFound) {
        this.notFound = notFound;
    }

    interface MutableResolvedVirtualHost extends ResolvedVirtualHost {
        void setResolvedMount(ResolvedMount resMount);
    }

    private class HstLinkImplCharacteristics {

        private static final String SCHEME_AGNOSTIC = "scheme_agnostic";
        private final HstRequestContext requestContext;
        private boolean fullyQualified;
        private String scheme;

        public HstLinkImplCharacteristics(final HstRequestContext requestContext, final boolean explicitlyFullyQualified) {
            this.requestContext = requestContext;
            fullyQualified = explicitlyFullyQualified;
        }

        public boolean isFullyQualified() {
            if (fullyQualified) {
                // was explicitly marked to be fully qualified.
                return true;
            }
            // check whether link really is not needed to be fully qualified. It still needs to be fully qualified for example
            // if the link is for a different host, different port, or different scheme than current request.
            if (requestContext.isFullyQualifiedURLs()) {
                return true;
            }

            if (containerResource) {
                // containerResource are only fully qualified if explicitly defined by explicitlyFullyQualified
                // or by  requestContext.isFullyQualifiedURLs()
                return false;
            }

            final Mount requestMount = requestContext.getResolvedMount().getMount();

            if (requestMount.getVirtualHost() != mount.getVirtualHost()) {
                return true;
            }
            if (mount.isPortInUrl() && requestMount.getPort() != mount.getPort()) {
                return true;
            }

            if (requestContext.getServletRequest() == null) {
                // for example in case of unit tests this is possible
                return false;
            }

            /*
             * If this HstLinkImpl is created through linkrewriting using a <code>siteMapItem</code> we use the scheme defined on the
             * <code>siteMapItem</code>.
             * When the <code>siteMapItem</code> is <code>null</code>, we have to deduce the <code>scheme</code>. For efficiency,
             * we first check whether there is a possibility that the <code>scheme</code> can be different than the current request
             * <code>scheme</code>. This is done by checking <code>mount.containsMultipleSchemes()</code> and
             * <code>requestMount.containsMultipleSchemes()</code>. If it is possible that the <code>scheme</code> is different,
             * we need to try to match the pathInfo for the link to a siteMapItem to find out what the <code>scheme</code> on that
             * siteMapItem would be.
             */

            final String farthestRequestScheme = HstRequestUtils.getFarthestRequestScheme(requestContext.getServletRequest());
            if (siteMapItem != null) {
                if (siteMapItem.isSchemeAgnostic()) {
                    scheme = SCHEME_AGNOSTIC;
                    return false;
                }
                if (!farthestRequestScheme.equals(siteMapItem.getScheme())) {
                    scheme = siteMapItem.getScheme();
                    return true;
                } else {
                    return false;
                }
            }

            if (mount.containsMultipleSchemes()
                    || requestMount.containsMultipleSchemes()
                    || (requestMount.getVirtualHost().isCustomHttpsSupported() && farthestRequestScheme.equals("https"))) {
                // in case (requestMount.getVirtualHost().isCustomHttpsSupported() && farthestRequestScheme.equals("https"))
                // is true: currently link is over https. This might be the result of custom https support. Hence, create
                // http link in case the mount/sitemap item indicates http
                final ResolvedSiteMapItem resolvedSiteMapItem = resolveSiteMapItem();
                if (resolvedSiteMapItem != null) {
                    if (resolvedSiteMapItem.getHstSiteMapItem().isSchemeAgnostic()) {
                        scheme = SCHEME_AGNOSTIC;
                        return false;
                    }
                    if (!farthestRequestScheme.equals(resolvedSiteMapItem.getHstSiteMapItem().getScheme())) {
                        scheme = resolvedSiteMapItem.getHstSiteMapItem().getScheme();
                        return true;
                    }
                }
            }
            
            if (mount.isSchemeAgnostic()) {
                scheme = SCHEME_AGNOSTIC;
                return false;
            }

            if (!mount.containsMultipleSchemes() && !requestMount.containsMultipleSchemes() &&
                    !mount.getScheme().equals(requestMount.getScheme())) {
                // all links for 'mount' have different scheme than all links for 'requestMount' so we do not need to test a matching sitemap item
                scheme = mount.getScheme();
                return true;
            }
            return false;
        }

        public String getScheme() {
            if (scheme != null) {
                return scheme;
            }
            // to avoid more expensive resolveSiteMapItem() we first check whether scheme *can* be different than the one
            // for the mount
            if (schemeCannotBeDifferent()) {
                if (mount.isSchemeAgnostic()) {
                    scheme = SCHEME_AGNOSTIC;
                    return scheme;
                }
                scheme = mount.getScheme();
                return scheme;
            }
            final ResolvedSiteMapItem resolvedSiteMapItem = resolveSiteMapItem();
            if (resolvedSiteMapItem != null) {
                if (resolvedSiteMapItem.getHstSiteMapItem().isSchemeAgnostic()) {
                    scheme = SCHEME_AGNOSTIC;
                    return scheme;
                }
                scheme = resolvedSiteMapItem.getHstSiteMapItem().getScheme();
                return scheme;
            }
            if (mount.isSchemeAgnostic()) {
                scheme = SCHEME_AGNOSTIC;
                return scheme;
            }
            scheme = mount.getScheme();
            return scheme;
        }

        private boolean schemeCannotBeDifferent() {
            final Mount requestMount = requestContext.getResolvedMount().getMount();
            if (!mount.containsMultipleSchemes() && !requestMount.containsMultipleSchemes() &&
                    mount.getScheme().equals(requestMount.getScheme())) {
                return true;
            }
            return false;
        }

        private ResolvedSiteMapItem resolveSiteMapItem() {
            try {
                MutableResolvedVirtualHost resolvedHostForLink = new MutableResolvedVirtualHost() {
                    private ResolvedMount resolvedMountForLink = null;

                    @Override
                    public VirtualHost getVirtualHost() {
                        return mount.getVirtualHost();
                    }

                    @Override
                    public String getResolvedHostName() {
                        return mount.getVirtualHost().getHostName();
                    }

                    @Override
                    public int getPortNumber() {
                        return mount.getPort();
                    }

                    @Override
                    public ResolvedMount matchMount(final String contextPath, final String requestPath) throws MatchException {
                        return resolvedMountForLink;
                    }

                    @Override
                    public void setResolvedMount(ResolvedMount resMount) {
                        this.resolvedMountForLink = resMount;
                    }
                };

                ResolvedMount resMount = new ResolvedMountImpl(mount, resolvedHostForLink, mount.getMountPath(),
                        mount.getVirtualHost().getVirtualHosts().getCmsPreviewPrefix(), requestContext.getResolvedMount().getPortNumber());
                resolvedHostForLink.setResolvedMount(resMount);
                return mount.getHstSiteMapMatcher().match(path, resMount);
            } catch (Exception e) {
                // cannot match a sitemap item
                return null;
            }
        }
    }


}
