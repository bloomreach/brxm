/*
 *  Copyright 2008-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.linking;

import java.io.UnsupportedEncodingException;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.core.util.PathEncoder;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.request.ResolvedMountImpl;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.HstSiteMapUtils;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.hippoecm.hst.configuration.ConfigurationConstants.CDN_SUPPORTED_PIPELINES;
import static org.hippoecm.hst.util.PathUtils.FULLY_QUALIFIED_URL_PREFIXES;

public class HstLinkImpl implements HstLink {

    private final static Logger log = LoggerFactory.getLogger(HstLinkImpl.class);

    private String path;
    private String subPath;
    private Mount mount;
    private Optional<HstSiteMapItem> siteMapItem;
    private boolean notFound = false;

    /**
     * <p>
     * The {@link Type} of the {@link HstLink}. An {@link HstLink} can be of type <i>container resource</i> or of
     * type <i>mount resource</i>. When not yet known, the type is set to {@link Type#UNKNOWN}. The meaning of
     * <i>container resource</i> vs <i>mount resource</i> is:
     * </p>
     * <p>
     * <ol>
     * <li>{@link Type#CONTAINER_RESOURCE} : The resulting URL will be webapp relative and not
     * relative to {@link Mount#getMountPath()}</li>
     * <li>{@link Type#MOUNT_RESOURCE}: The resulting URL <b>WILL</b> include the {@link Mount#getMountPath()} after
     * the webapp relative part (context path). For sub mounts below the root mount (/)
     * the {@link Mount#getMountPath()} is the path to the sub mount, for example /fr</li>
     * </ol>
     * </p>
     */
    private Type type;

    enum Type {
        CONTAINER_RESOURCE,
        MOUNT_RESOURCE,
        UNKNOWN
    }

    /**
     * <p>
     * Indicates whether this {@link org.hippoecm.hst.core.linking.HstLink} instance was created from a jcr node
     * representing a document, a folder, or whether unknown. Default is {@link ContentType#UNKNOWN}
     * </p>
     */
    ContentType contentType;

    enum ContentType {
        FOLDER,
        DOCUMENT,
        UNKNOWN
    }

    boolean representsIndex;

    public HstLinkImpl(String path, Mount mount) {
        this(path, mount, null, Type.UNKNOWN, true);
    }

    public HstLinkImpl(String path, Mount mount, boolean containerResource) {
        this(path, mount, null, containerResource, true);
    }

    public HstLinkImpl(ResolvedLocationMapTreeItem resolvedLocationMapTreeItem, Mount mount, boolean containerResource) {
        this(resolvedLocationMapTreeItem.getPath(), mount, resolvedLocationMapTreeItem.getSiteMapItem(), containerResource, true);
        if (resolvedLocationMapTreeItem.representsDocument()) {
            contentType = ContentType.DOCUMENT;
        } else {
            contentType = ContentType.FOLDER;
        }
        if (resolvedLocationMapTreeItem.representsIndex()) {
            representsIndex = true;
        }
    }

    public HstLinkImpl(String path, Mount mount, boolean containerResource, boolean rewriteHomePagePath) {
        this(path, mount, null, containerResource, rewriteHomePagePath);
    }

    public HstLinkImpl(String path, Mount mount, HstSiteMapItem siteMapItem, boolean containerResource, boolean rewriteHomePagePath) {
        this(path, mount, siteMapItem, containerResource ? Type.CONTAINER_RESOURCE : Type.MOUNT_RESOURCE, rewriteHomePagePath);
    }

    private HstLinkImpl(final String path, final Mount mount, final HstSiteMapItem siteMapItem, final Type type, boolean rewriteHomePagePath) {

        if (path != null && path.startsWith("//")) {
            // fully qualified path
            this.path = "//" + PathUtils.normalizePath(path);
        } else {
            this.path = PathUtils.normalizePath(path);
        }
        this.mount = mount;
        if (siteMapItem != null) {
            this.siteMapItem = Optional.of(siteMapItem);
        }
        this.type = type;

        if (type == Type.UNKNOWN && mount != null) {
            if (mount.getVirtualHost().getVirtualHosts().isHstFilterExcludedPath("/" + path)) {
                this.type = Type.CONTAINER_RESOURCE;
            }
        }

        if (rewriteHomePagePath) {
            // check whether path is equal to homepage : if so, replace with ""
            if (this.path != null && mount != null) {
                // get the homePagePath : the mount.getHomePage can be the homepage path OR the sitemap item refId
                // with HstSiteMapUtils.getPath we get the homepage path regardless whether mount.getHomePage() is the path of the refId
                String homePagePath = HstSiteMapUtils.getPath(mount, mount.getHomePage());
                if (path.equals(homePagePath) || ("/" + path).equals(homePagePath)) {
                    // homepage link : Set path to "";
                    this.path = "";
                }
            }
        }
    }


    public Mount getMount() {
        return mount;
    }

    @Override
    public boolean representsIndex() {
        return representsIndex;
    }

    @Override
    public HstSiteMapItem getHstSiteMapItem() {
        if (siteMapItem != null) {
            return siteMapItem.orElse(null);
        }

        final HstRequestContext requestContext = RequestContextProvider.get();
        if (requestContext == null) {
            return null;
        }
        return resolveSiteMapItem(requestContext);
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

    ContentType getContentType() {
        return contentType;
    }

    @Override
    public boolean isContainerResource() {
        if (Type.UNKNOWN == type) {
            if (RequestContextProvider.get() == null || mount == null) {
                type = Type.CONTAINER_RESOURCE;
            } else if (!mount.isMapped()) {
                type = Type.MOUNT_RESOURCE;
            } else {
                final HstSiteMapItem hstSiteMapItem = resolveSiteMapItem(RequestContextProvider.get());
                if (hstSiteMapItem == null || hstSiteMapItem.isContainerResource()) {
                    type = Type.CONTAINER_RESOURCE;
                } else {
                    type = Type.MOUNT_RESOURCE;
                }
            }
        }
        return Type.CONTAINER_RESOURCE == type;
    }

    public void setContainerResource(boolean containerResource) {
        if (containerResource) {
            type = Type.CONTAINER_RESOURCE;
        } else {
            type = Type.MOUNT_RESOURCE;
        }
    }

    public String[] getPathElements() {
        if (this.path == null) {
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
                try {
                    final String encoded = PathEncoder.encode(path,
                            requestContext.getBaseURL().getURIEncoding(),
                            FULLY_QUALIFIED_URL_PREFIXES);
                    return encoded;
                } catch (UnsupportedEncodingException e) {
                    // same exception as used by org.hippoecm.hst.core.component.HstURLImpl.toString() although
                    // not perse from a HstComponent invocation, but this is not the case for HstURLImpl either.
                    throw new HstComponentException(e);
                }
            }
        }

        Mount requestMount = requestContext.getResolvedMount().getMount();

        // check if we need to set an explicit contextPath 
        String explicitContextPath = null;
        if (requestContext.isChannelManagerPreviewRequest() && !requestContext.isPageModelApiRequest()) {
            if (mount != null) {
                explicitContextPath = mount.getContextPath();
            } else {
                // mount is null or contextpath agnostic: use contextpath from current request
                explicitContextPath = requestContext.getServletRequest().getContextPath();
            }
        } else if (mount != null && requestMount != mount) {
            if (mount.isContextPathInUrl()) {
                explicitContextPath = mount.getContextPath();
            }
        }

        String urlString;
        if (isContainerResource()) {
            HstURL hstUrl = requestContext.getURLFactory().createURL(HstURL.RESOURCE_TYPE, ContainerConstants.CONTAINER_REFERENCE_NAMESPACE, null, requestContext, explicitContextPath);
            hstUrl.setResourceID(path);
            urlString = hstUrl.toString(true);
        } else {

            HstManager mngr = HstServices.getComponentManager().getComponent(HstManager.class.getName());
            String subPathDelimeter = mngr.getPathSuffixDelimiter();
            if (subPath != null) {
                // subPath is allowed to be empty ""
                path += subPathDelimeter + subPath;
            }

            HstContainerURL navURL = requestContext.getContainerURLProvider().createURL(mount, requestContext.getBaseURL(), path);
            urlString = requestContext.getURLFactory().createURL(HstURL.RENDER_TYPE, null, navURL, requestContext, explicitContextPath).toString();
            if (StringUtils.isEmpty(path) && StringUtils.isEmpty(urlString)) {
                // homepage with no contextpath : replace urlString with /
                urlString = "/";
            }
        }

        if (mount != null) {
            // for requests that are a channel manager preview request, we always return absolute links (starting with
            // a '/') and never fully qualified links UNLESS the request is a channel mgr preview Page Model API request:
            // Since these requests are consumed by an SPA, they will need to follow the 'normal' URL procssing
            if (requestContext.isChannelManagerPreviewRequest() && !requestContext.isPageModelApiRequest()) {
                if (requestContext.getRenderHost() == null) {
                    // TODO See HSTTWO-4587 : Because we still have the issue HSTTWO-4587, the render host can be null
                    // TODO HSTTWO-4587 needs to be fixed for that, for now, do not throw IllegalStateException
                    // TODO ENABLE:    throw new IllegalStateException("RequestContext#getRenderHost() cannot be null for channel manager " +
                    // TODO         "preview requests");
                }
                // check whether the urlString is equal to the contextPath of the mount. If so,
                // we need to append an extra / to the urlString : This is to avoid a link like 
                // '/site' in cms preview context: It must there be '/site/'
                if (urlString.equals(requestMount.getContextPath())) {
                    urlString += "/";
                }

                if (requestMount != mount) {
                    String urlRenderHost = null;
                    // the link is cross-domain, so set the render host if the render host is different than the current host
                    if (!isHostSame(requestContext.getRenderHost(), mount.getVirtualHost().getHostName())) {
                        urlRenderHost = mount.getVirtualHost().getHostName();
                    } else if (!StringUtils.equals(requestMount.getContextPath(), mount.getContextPath())) {
                        // cross webapp link : Since the other webapp might not yet have an 'sso' in the channel mgr, we need
                        // to include the renderHost
                        urlRenderHost = mount.getVirtualHost().getHostName();
                    }

                    if (urlRenderHost != null && !isContainerResource()) {
                        // we need to append the render host as a request parameter but it is never needed for resources
                        if (urlString.contains("?")) {
                            urlString += "&";
                        } else {
                            urlString += "?";
                        }
                        urlString += ContainerConstants.RENDERING_HOST + '=' + urlRenderHost;
                    }
                }
            } else {
                // the above requestContext.isChannelManagerPreviewRequest() check is to avoid fully qualified links in CMS channel manager:
                // for the cms, we never want a fully qualified URLs for links as that is managed through the 'renderHost'

                if (isNotBlank(mount.getVirtualHost().getCdnHost())) {
                    final HstSiteMapItem siteMapItem = resolveSiteMapItem(requestContext);
                    if (siteMapItem != null && siteMapItem.isContainerResource()
                            && isCdnSupportedPipeline(siteMapItem.getNamedPipeline())) {
                        log.debug("Using CDN host '{}' for container resource '{}'", mount.getVirtualHost().getCdnHost(), urlString);
                        return mount.getVirtualHost().getCdnHost() + urlString;
                    }
                }

                if (isContainerResource()) {
                    // baseUrlPrefix can be different for the root mount which handles resources than for the current
                    // 'mount' which is present for a container resource link, hence first get root mount
                    final Mount root = getRootMount(mount);
                    if (isNotBlank(root.getHstLinkUrlPrefix())) {
                        return root.getHstLinkUrlPrefix() + urlString;
                    }
                } else if (isNotBlank(mount.getHstLinkUrlPrefix())) {
                    return mount.getHstLinkUrlPrefix() + urlString;
                }

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

        return urlString;
    }

    private Mount getRootMount(final Mount mount) {
        if (mount.getParent() == null) {
            return mount;
        }
        return getRootMount(mount.getParent());
    }

    // one host can contain port number and the other not
    private boolean isHostSame(String renderHost, String hostName) {
        return Objects.equals(substringBefore(renderHost, ":"), substringBefore(hostName, ":"));
    }

    private boolean isCdnSupportedPipeline(final String pipeline) {
        return CDN_SUPPORTED_PIPELINES.contains(pipeline);
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
            if (mount == null) {
                return false;
            }

            final Mount requestMount = requestContext.getResolvedMount().getMount();

            if (!requestMount.getVirtualHost().getHostName().equals(mount.getVirtualHost().getHostName())) {
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
            if (siteMapItem != null && siteMapItem.isPresent()) {
                if (siteMapItem.get().isSchemeAgnostic()) {
                    scheme = SCHEME_AGNOSTIC;
                    return false;
                }
                if (!farthestRequestScheme.equals(siteMapItem.get().getScheme())) {
                    scheme = siteMapItem.get().getScheme();
                    return true;
                } else {
                    return false;
                }
            }

            if (mount.isMapped() && (mount.containsMultipleSchemes()
                    || requestMount.containsMultipleSchemes()
                    || (requestMount.getVirtualHost().isCustomHttpsSupported() && farthestRequestScheme.equals("https")))) {
                // in case (requestMount.getVirtualHost().isCustomHttpsSupported() && farthestRequestScheme.equals("https"))
                // is true: currently link is over https. This might be the result of custom https support. Hence, create
                // http link in case the mount/sitemap item indicates http
                final HstSiteMapItem siteMapItem = resolveSiteMapItem(requestContext);
                if (siteMapItem != null) {
                    if (siteMapItem.isSchemeAgnostic()) {
                        scheme = SCHEME_AGNOSTIC;
                        return false;
                    }
                    if (!farthestRequestScheme.equals(siteMapItem.getScheme())) {
                        scheme = siteMapItem.getScheme();
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
            if (mount == null) {
                scheme = requestContext.getResolvedMount().getMount().getScheme();
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
            final HstSiteMapItem siteMapItem = resolveSiteMapItem(requestContext);
            if (siteMapItem != null) {
                if (siteMapItem.isSchemeAgnostic()) {
                    scheme = SCHEME_AGNOSTIC;
                    return scheme;
                }
                scheme = siteMapItem.getScheme();
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


    }

    private HstSiteMapItem resolveSiteMapItem(HstRequestContext requestContext) {
        if (siteMapItem != null) {
            return siteMapItem.orElse(null);
        }
        if (mount == null) {
            return null;
        }
        ResolvedSiteMapItem resolved = null;
        try {
            MutableResolvedVirtualHost resolvedHostForLink = new MutableResolvedVirtualHost() {
                private ResolvedMount resolvedMountForLink = null;

                @Override
                public VirtualHost getVirtualHost() {
                    return mount.getVirtualHost();
                }

                @Deprecated
                @Override
                public ResolvedMount matchMount(final String contextPath, final String requestPath) throws MatchException {
                    return resolvedMountForLink;
                }

                @Override
                public ResolvedMount matchMount(final String requestPath) throws MatchException {
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

            if ("".equals(path) || "/".equals(path)) {
                log.debug("siteMapPathInfo is '' or '/'. If there is a homepage path configured, we try to map this path to the sitemap");
                resolved = mount.getHstSiteMapMatcher().match(resMount.getMount().getHomePage(), resMount);
            } else {
                resolved = mount.getHstSiteMapMatcher().match(path, resMount);
            }

        } catch (Exception e) {
            // cannot match a sitemap item
            if (log.isDebugEnabled()) {
                log.info("Could not match a sitemap item ", e);
            } else {
                log.info("Could not match a sitemap item ", e.toString());
            }
        }

        if (resolved != null) {
            siteMapItem = Optional.of(resolved.getHstSiteMapItem());
        } else {
            siteMapItem = Optional.empty();
        }
        return siteMapItem.orElse(null);
    }
}
