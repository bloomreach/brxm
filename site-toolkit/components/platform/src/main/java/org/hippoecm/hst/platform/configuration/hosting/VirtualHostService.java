/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.hosting;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.google.common.net.InetAddresses;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.MutablePortMount;
import org.hippoecm.hst.configuration.hosting.MutableVirtualHost;
import org.hippoecm.hst.configuration.hosting.PortMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.core.internal.StringPool;
import org.hippoecm.hst.platform.configuration.cache.HstConfigurationLoadingCache;
import org.hippoecm.hst.platform.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.platform.configuration.model.ModelLoadingException;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.HttpHeaderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.unmodifiableList;
import static org.hippoecm.hst.configuration.ConfigurationUtils.isSupportedSchemeNotMatchingResponseCode;
import static org.hippoecm.hst.configuration.ConfigurationUtils.supportedSchemeNotMatchingResponseCodesAsString;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_HST_LINK_URL_PREFIX;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PAGE_MODEL_API;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_RESPONSE_HEADERS;
import static org.hippoecm.hst.configuration.HstNodeTypes.VIRTUALHOST_ALLOWED_ORIGINS;

public class VirtualHostService implements MutableVirtualHost {

    private static final Logger log = LoggerFactory.getLogger(VirtualHostService.class);

    private Map<String, VirtualHost> childVirtualHosts = VirtualHostsService.virtualHostHashMap();

    private String name;
    private String hostName;
    /**
     * The homepage for this VirtualHost. When the backing configuration does not contain a homepage, then, the homepage from the backing {@link VirtualHosts} is
     * taken (which still might be <code>null</code> though)
     */
    private String homepage;
    /**
     * The pageNotFound for this VirtualHost. When the backing configuration does not contain a pageNotFound, then, the pageNotFound from the backing {@link VirtualHosts} is
     * taken (which still might be <code>null</code> though)
     */
    private String pageNotFound;

    /**
     * The locale configured on this Virtual host. When the backing configuration does not contain a locale, it is taken from the parent {@link VirtualHost}. When there is no parent {@link VirtualHost},
     * the value is taken from {@link VirtualHosts#getLocale()}. The locale can be <code>null</code>
     */
    private String locale;

    /**
     * Whether the {@link Mount}'s contained by this VirtualHostService should show the hst version as a response header when they are a preview {@link Mount}
     */
    private boolean versionInPreviewHeader;

    private VirtualHosts virtualHosts;

    /**
     * The name of the host group this virtualhost belongs to, for example, dev, acct or prod
     */
    private String hostGroupName;
    private VirtualHostService parentHost;

    private Map<Integer, MutablePortMount> portMounts = new HashMap<Integer, MutablePortMount>();

    private boolean contextPathInUrl;

    private boolean showPort;
    private String scheme;
    private boolean schemeAgnostic;
    private int schemeNotMatchingResponseCode = -1;
    private final String pageModelApi;
    private String linkUrlPrefix;
    private Integer defaultPort;
    private final boolean cacheable;
    private String [] defaultResourceBundleIds;
    private String cdnHost;
    private boolean customHttpsSupported;
    private Map<String, String> responseHeaders;
    private final Collection<String> allowedOrigins;

    public VirtualHostService(final VirtualHostsService virtualHosts,
                              final HstNode virtualHostNode,
                              final VirtualHostService parentHost,
                              final String hostGroupName,
                              final int defaultPort,
                              final HstNodeLoadingCache hstNodeLoadingCache,
                              final HstConfigurationLoadingCache hstConfigurationLoadingCache) throws ModelLoadingException {

        this.parentHost = parentHost;
        this.virtualHosts = virtualHosts;
        this.hostGroupName = StringPool.get(hostGroupName);
        this.defaultPort = defaultPort;

        if(virtualHostNode.getValueProvider().hasProperty(HstNodeTypes.VIRTUALHOST_PROPERTY_SHOWCONTEXTPATH)) {
            this.contextPathInUrl = virtualHostNode.getValueProvider().getBoolean(HstNodeTypes.VIRTUALHOST_PROPERTY_SHOWCONTEXTPATH);
        } else {
            // try to get the one from the parent
            if(parentHost != null) {
                this.contextPathInUrl = parentHost.contextPathInUrl;
            } else {
                this.contextPathInUrl = virtualHosts.isContextPathInUrl();
            }
        }

        if(virtualHostNode.getValueProvider().hasProperty(HstNodeTypes.VIRTUALHOST_PROPERTY_SHOWPORT)) {
            this.showPort = virtualHostNode.getValueProvider().getBoolean(HstNodeTypes.VIRTUALHOST_PROPERTY_SHOWPORT);
        } else {
            // try to get the one from the parent
            if(parentHost != null) {
                this.showPort = parentHost.showPort;
            } else {
                this.showPort = virtualHosts.isPortInUrl();
            }
        }

        if(virtualHostNode.getValueProvider().hasProperty(HstNodeTypes.VIRTUALHOST_PROPERTY_SCHEME)) {
            scheme = StringPool.get(virtualHostNode.getValueProvider().getString(HstNodeTypes.VIRTUALHOST_PROPERTY_SCHEME));
        }
        if (StringUtils.isBlank(scheme)) {
            scheme = parentHost != null ? parentHost.getScheme() : virtualHosts.getScheme();
        }

        if (virtualHostNode.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROEPRTY_SCHEME_AGNOSTIC)) {
            schemeAgnostic = virtualHostNode.getValueProvider().getBoolean(HstNodeTypes.GENERAL_PROEPRTY_SCHEME_AGNOSTIC);
        } else if (parentHost != null) {
            schemeAgnostic = parentHost.isSchemeAgnostic();
        }

        if(virtualHostNode.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE)) {
            schemeNotMatchingResponseCode = (int)virtualHostNode.getValueProvider().getLong(HstNodeTypes.GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE).longValue();
            if (!isSupportedSchemeNotMatchingResponseCode(schemeNotMatchingResponseCode)) {
                log.warn("Invalid '{}' configured on '{}'. Use inherited value. Supported values are '{}'", new String[]{HstNodeTypes.GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE,
                        virtualHostNode.getValueProvider().getPath(), supportedSchemeNotMatchingResponseCodesAsString()});
                schemeNotMatchingResponseCode = -1;
            }
        }

        if (schemeNotMatchingResponseCode == -1) {
            schemeNotMatchingResponseCode = parentHost != null ?
                    parentHost.getSchemeNotMatchingResponseCode() : virtualHosts.getSchemeNotMatchingResponseCode();
        }

        if(virtualHostNode.getValueProvider().hasProperty(HstNodeTypes.VIRTUALHOST_PROPERTY_CUSTOM_HTTPS_SUPPORT)) {
            customHttpsSupported = virtualHostNode.getValueProvider().getBoolean(HstNodeTypes.VIRTUALHOST_PROPERTY_CUSTOM_HTTPS_SUPPORT);
        } else {
            customHttpsSupported = parentHost != null ? parentHost.isCustomHttpsSupported() : false ;
        }

        if(virtualHostNode.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCALE)) {
            this.locale = virtualHostNode.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_LOCALE);
        } else {
            // try to get the one from the parent
            if(parentHost != null) {
                this.locale = parentHost.locale;
            } else {
                this.locale = virtualHosts.getLocale();
            }
        }
        locale = StringPool.get(locale);

        if(virtualHostNode.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_HOMEPAGE)) {
            this.homepage = virtualHostNode.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_HOMEPAGE);
        } else {
           // try to get the one from the parent
            if(parentHost != null) {
                this.homepage = parentHost.homepage;
            } else {
                this.homepage = virtualHosts.getHomePage();
            }
        }

        homepage = StringPool.get(homepage);

        if(virtualHostNode.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_PAGE_NOT_FOUND)) {
            this.pageNotFound = virtualHostNode.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_PAGE_NOT_FOUND);
        } else {
           // try to get the one from the parent
            if(parentHost != null) {
                this.pageNotFound = parentHost.pageNotFound;
            } else {
                this.pageNotFound = virtualHosts.getPageNotFound();
            }
        }

        pageNotFound = StringPool.get(pageNotFound);

        if(virtualHostNode.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER)) {
            this.versionInPreviewHeader = virtualHostNode.getValueProvider().getBoolean(HstNodeTypes.GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER);
        } else {
           // try to get the one from the parent
            if(parentHost != null) {
                this.versionInPreviewHeader = parentHost.versionInPreviewHeader;
            } else {
                this.versionInPreviewHeader = virtualHosts.isVersionInPreviewHeader();
            }
        }

        if(virtualHostNode.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_CACHEABLE)) {
            this.cacheable = virtualHostNode.getValueProvider().getBoolean(HstNodeTypes.GENERAL_PROPERTY_CACHEABLE);
        } else if(parentHost != null) {
            this.cacheable = parentHost.isCacheable();
        } else {
            this.cacheable =  virtualHosts.isCacheable();
        }

        if(virtualHostNode.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_DEFAULT_RESOURCE_BUNDLE_ID)) {
            this.defaultResourceBundleIds = StringUtils.split(virtualHostNode.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_DEFAULT_RESOURCE_BUNDLE_ID), " ,\t\f\r\n");
        } else if(parentHost != null) {
            this.defaultResourceBundleIds = parentHost.getDefaultResourceBundleIds();
        } else {
            this.defaultResourceBundleIds =  virtualHosts.getDefaultResourceBundleIds();
        }

        if(virtualHostNode.getValueProvider().hasProperty(HstNodeTypes.VIRTUALHOST_PROPERTY_CDN_HOST)) {
            cdnHost = virtualHostNode.getValueProvider().getString(HstNodeTypes.VIRTUALHOST_PROPERTY_CDN_HOST);
        } else if(parentHost != null) {
            cdnHost = parentHost.getCdnHost();
        }

        if (cdnHost != null && !validURLPrefix(cdnHost)) {
            log.error("Ignoring invalid CDN host '{}'. Supported formats are http://hostname, https://hostname or " +
                    "//hostname (hostname can include portnumber). It is not allowed to end with a '/'. " +
                    "Querystring or fragment is not allowed", cdnHost);
            cdnHost = null;
        }

        if (virtualHostNode.getValueProvider().hasProperty(GENERAL_PROPERTY_RESPONSE_HEADERS)) {
            String[] resHeaders = virtualHostNode.getValueProvider().getStrings(GENERAL_PROPERTY_RESPONSE_HEADERS);
            if (resHeaders.length != 0) {
                responseHeaders = HttpHeaderUtils.parseHeaderLines(resHeaders);
            }
        } else if (parentHost != null) {
            Map<String, String> resHeaderMap = parentHost.getResponseHeaders();
            if (resHeaderMap != null && !resHeaderMap.isEmpty()) {
                responseHeaders = new LinkedHashMap<>(resHeaderMap);
            }
        }

        if (virtualHostNode.getValueProvider().hasProperty(VIRTUALHOST_ALLOWED_ORIGINS)) {
            allowedOrigins = unmodifiableList(
                    Arrays.stream(virtualHostNode.getValueProvider().getStrings(VIRTUALHOST_ALLOWED_ORIGINS))
                            // prevent tricky/failing behavior caused by accidental surrounding whitespaces
                            .map(StringUtils::trim)
                            .collect(Collectors.toList())
            );
        } else if (parentHost != null) {
            allowedOrigins = parentHost.getAllowedOrigins();
        } else {
            allowedOrigins = Collections.emptyList();
        }

        if (virtualHostNode.getValueProvider().hasProperty(GENERAL_PROPERTY_PAGE_MODEL_API)) {
            pageModelApi = virtualHostNode.getValueProvider().getString(GENERAL_PROPERTY_PAGE_MODEL_API);
        } else if (parentHost != null) {
            pageModelApi = parentHost.getPageModelApi();
        } else {
            pageModelApi = null;
        }


        if (virtualHostNode.getValueProvider().hasProperty(GENERAL_PROPERTY_HST_LINK_URL_PREFIX)) {
            linkUrlPrefix = virtualHostNode.getValueProvider().getString(GENERAL_PROPERTY_HST_LINK_URL_PREFIX);
        } else if (parentHost != null) {
            linkUrlPrefix = parentHost.getHstLinkUrlPrefix();
        } else {
            linkUrlPrefix = null;
        }

        if (linkUrlPrefix != null && !validURLPrefix(linkUrlPrefix)) {
            log.error("Ignoring invalid link URL prefix '{}'. Supported formats are http://hostname, https://hostname or " +
                    "//hostname (hostname can include portnumber). After the hostname a path info is allowed which is not" +
                    "allowed to end with a '/'. Querystring or fragment is not allowed", linkUrlPrefix);
            linkUrlPrefix = null;
        }

        final String substitutedName = virtualHostNode.getSubstitutedName();
        final String[] nameSegments = substitutedName.split("[.]");

        VirtualHostService attachPortMountToHost = this;

        if (nameSegments.length > 1) {
            if (!InetAddresses.isInetAddress(substitutedName)) {
                throw new ModelLoadingException("Node hst:virtualhost is not allowed to be '" + substitutedName +
                        "'. Only ip-addresses are allowed to have a '.' in the nodename. Re-configure the host to a hierarchical structure");
            }

            // fullName represents an IP address. Recursively build a hierarchy of VirtualHostServices.
            // e.g. "127.0.0.1" gets translated into virtual child hosts 1/0/0/127.
            this.name = StringPool.get(nameSegments[nameSegments.length - 1]);
            // add child host services
            int depth = nameSegments.length - 2;
            if(depth > -1 ) {
                VirtualHostService childHost = new VirtualHostService(this, nameSegments, depth, hostGroupName, defaultPort);
                this.childVirtualHosts.put(childHost.name, childHost);
                // we need to switch the attachPortMountToHost to the last host
            }
            while(depth > -1) {
                if(attachPortMountToHost == null) {
                    throw new ModelLoadingException("Something went wrong because attachMountToHost should never be possible to be null.");
                }
                attachPortMountToHost = (VirtualHostService)attachPortMountToHost.getChildHost(nameSegments[depth]);
                depth--;
            }
        } else {
            this.name = substitutedName.toLowerCase();
        }

        hostName = StringPool.get(buildHostName());

        HstNode mountRoot = virtualHostNode.getNode(HstNodeTypes.MOUNT_HST_ROOTNAME);
        if(mountRoot != null) {
            log.info("Host '{}' does have a root Mount configured without PortMount. This Mount is port agnostic ", this.getHostName());
            // we have a configured root Mount node without portmount. Let's populate this Mount. This Mount will be added to
            // a portmount service with portnumber 0, which means any port
            HstNode mountNode = virtualHostNode.getNode(HstNodeTypes.MOUNT_HST_ROOTNAME);
            if(HstNodeTypes.NODETYPE_HST_MOUNT.equals(mountNode.getNodeTypeName())) {
                try {
                    Mount mount = new MountService(mountNode, null, attachPortMountToHost, hstNodeLoadingCache, hstConfigurationLoadingCache, defaultPort);
                    MutablePortMount portMount = new PortMountService(mount);
                    attachPortMountToHost.portMounts.put(portMount.getPortNumber(), portMount);
                } catch (ModelLoadingException e) {
                    String path = mountNode.getValueProvider().getPath();
                    log.error("Skipping incorrect mount or port mount for mount node '"+path+"'. " ,e);
                }
            } else {
                log.error("Expected a node of type '{}' at '{}' but was of type '"+mountNode.getNodeTypeName()+"'", HstNodeTypes.NODETYPE_HST_MOUNT, mountNode.getValueProvider().getPath());
            }
        }

        for(HstNode child : virtualHostNode.getNodes()) {
            if(HstNodeTypes.NODETYPE_HST_VIRTUALHOST.equals(child.getNodeTypeName())) {
                try {
                    VirtualHostService childHost = new VirtualHostService(virtualHosts, child, attachPortMountToHost, hostGroupName, defaultPort, hstNodeLoadingCache, hstConfigurationLoadingCache);
                    attachPortMountToHost.childVirtualHosts.put(childHost.name, childHost);
                } catch (ModelLoadingException e) {
                    if (e.isMissingEnvironmentVariable()) {
                        log.info("Skip virtualhost with name '{}'because missing environment variable.",
                                virtualHostNode.getValueProvider().getName(), e);
                    } else {
                        log.error("Skipping incorrect virtual host for node '{}'", child.getValueProvider().getPath(), e);
                    }
                }

            } else if (HstNodeTypes.NODETYPE_HST_PORTMOUNT.equals(child.getNodeTypeName())){
                try {
                MutablePortMount portMount = new PortMountService(child, attachPortMountToHost, hstNodeLoadingCache, hstConfigurationLoadingCache);
                attachPortMountToHost.portMounts.put(portMount.getPortNumber(), portMount);
                } catch (ModelLoadingException e) {
                    log.error("The host '"+attachPortMountToHost.getHostName()+"' for port '"+child.getName()+"' contains an incorrect configured Mount. The host with port cannot be used for hst request processing", e);
                }
            }
        }

    }

    public VirtualHostService(final VirtualHostService parent,
                              final String[] nameSegments,
                              final int position,
                              final String hostGroupName,
                              final Integer defaultPort) {
        this.parentHost = parent;
        this.virtualHosts = parent.virtualHosts;
        this.hostGroupName = hostGroupName;
        this.pageModelApi = parent.pageModelApi;
        this.linkUrlPrefix = parent.linkUrlPrefix;
        this.defaultPort = defaultPort;
        this.scheme = parent.scheme;
        this.schemeAgnostic = parent.schemeAgnostic;
        this.schemeNotMatchingResponseCode = parent.schemeNotMatchingResponseCode;
        this.locale = parent.locale;
        this.homepage = parent.homepage;
        this.pageNotFound = parent.pageNotFound;
        this.versionInPreviewHeader = parent.versionInPreviewHeader;
        this.contextPathInUrl = parent.contextPathInUrl;
        this.showPort = parent.showPort;
        this.cacheable = parent.cacheable;
        this.defaultResourceBundleIds = parent.defaultResourceBundleIds;
        this.cdnHost = parent.cdnHost;
        this.customHttpsSupported = parent.customHttpsSupported;
        this.name = nameSegments[position];
        this.allowedOrigins = parent.allowedOrigins;

        if (parent.responseHeaders == null) {
            this.responseHeaders = null;
        } else {
            this.responseHeaders = new LinkedHashMap<String, String>(parent.responseHeaders);
        }

        // add child host services
        int nextPosition = position - 1;
        if(nextPosition > -1 ) {
            VirtualHostService childHost = new VirtualHostService(this,nameSegments, nextPosition, hostGroupName, defaultPort);
            this.childVirtualHosts.put(childHost.name, childHost);
        }
        hostName = StringPool.get(buildHostName());
    }

    @Override
    public void addVirtualHost(MutableVirtualHost virtualHost) throws IllegalArgumentException {
        // when the virtualhost already exists, the childVirtualHosts.put will throw an IllegalArgumentException, so we do not
        // need a separate check
        childVirtualHosts.put(virtualHost.getName(), virtualHost);
    }


    @Override
    public void addPortMount(MutablePortMount portMount) throws IllegalArgumentException {
        if(portMounts.containsKey(portMount.getPortNumber())) {
            throw new IllegalArgumentException("Cannot add a portMount for port '"+portMount.getPortNumber()+"' because portMount already exists.");
        }
        portMounts.put(portMount.getPortNumber(), portMount);
    }

    static boolean validURLPrefix(final String host) {
        if (StringUtils.isBlank(host)) {
            return false;
        }
        try {
            final URI uri = new URI(host);
            if (uri.getScheme() != null && !"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme())) {
                // if scheme is present it should be http or https
                return false;
            }
            if (uri.getPath() != null && uri.getPath().endsWith("/")) {
                return false;
            }
            if (uri.getQuery() != null || uri.getFragment() != null) {
                return false;
            }
        } catch (URISyntaxException e) {
            log.warn("'{}' is not a valid host : ", host, e.getMessage());
            return false;
        }
        return true;
    }

    public String getName(){
        return name;
    }

    public String getHostName(){
        return hostName;
    }

    public String getHostGroupName() {
        return hostGroupName;
    }

    public boolean isContextPathInUrl() {
        return contextPathInUrl;
    }

    public String getContextPath() {
        return virtualHosts.getContextPath();
    }

    public boolean isPortInUrl() {
        return showPort;
    }

    public String getScheme(){
        return this.scheme;
    }

    public boolean isSchemeAgnostic() {
        return schemeAgnostic;
    }

    public int getSchemeNotMatchingResponseCode() {
       return schemeNotMatchingResponseCode;
    }

    public String getLocale() {
        return locale;
    }


    public String getHomePage() {
        return homepage;
    }

    public String getPageNotFound() {
        return pageNotFound;
    }

    public boolean isVersionInPreviewHeader() {
        return versionInPreviewHeader;
    }

    String getPageModelApi() {
        return pageModelApi;
    }

    public String getHstLinkUrlPrefix() {
        return linkUrlPrefix;
    }

    public Integer getDefaultPort() {
        return defaultPort;
    }

    public VirtualHosts getVirtualHosts() {
        return this.virtualHosts;
    }

    public PortMount getPortMount(int portNumber) {
        return portMounts.get(portNumber);
    }

    public VirtualHost getChildHost(String name) {
        return childVirtualHosts.get(name);
    }

    public String getBaseURL(HttpServletRequest request) {
        final StringBuilder builder = new StringBuilder();
        final String scheme = HstRequestUtils.getFarthestRequestScheme(request);
        final String serverName = HstRequestUtils.getFarthestRequestHost(request, false);

        builder.append(scheme);
        builder.append("://").append(serverName);

        return builder.toString();
    }


    public List<VirtualHost> getChildHosts() {
        return new ArrayList<VirtualHost>(childVirtualHosts.values());
    }


    @Override
    public boolean isCacheable() {
        return cacheable;
    }

    @Override
    public String [] getDefaultResourceBundleIds() {
        if (defaultResourceBundleIds == null) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }

        return (String[]) ArrayUtils.clone(defaultResourceBundleIds);
    }

    @Override
    public String getCdnHost() {
        return this.cdnHost;
    }

    @Override
    public boolean isCustomHttpsSupported() {
        return customHttpsSupported;
    }

    @Override
    public Map<String, String> getResponseHeaders() {
        if (responseHeaders == null) {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(responseHeaders);
    }

    @Override
    public Collection<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    private String buildHostName() {
        StringBuilder builder = new StringBuilder(name);
        VirtualHostService ancestor = this.parentHost;
        while(ancestor != null) {
            builder.append(".").append(ancestor.name);
            ancestor = ancestor.parentHost;
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return "VirtualHostService [name=" + name + ", hostName =" + hostName + ", hostGroupName=" + hostGroupName + "]";
    }

}
