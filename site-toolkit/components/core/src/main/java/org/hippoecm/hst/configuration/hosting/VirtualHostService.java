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
package org.hippoecm.hst.configuration.hosting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.ConfigurationUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.model.ModelLoadingException;
import org.hippoecm.hst.core.internal.StringPool;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualHostService implements MutableVirtualHost {

    private static final Logger log = LoggerFactory.getLogger(VirtualHostService.class);

    private Map<String, MutableVirtualHost> childVirtualHosts = VirtualHostsService.virtualHostHashMap();

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

    /**
     *  when this {@link Mount}s for this {@link VirtualHost} are only applicable for certain contextpath,
     *  this property for the contextpath tells which value it must have. If not null, it must start with a '/' and is
     *  not allowed to end with a '/'
     */
    private String contextPath;

    private boolean showPort;
    private String scheme;
    private boolean schemeAgnostic;
    private int schemeNotMatchingResponseCode = -1;
    private final List<String> cmsLocations;
    private Integer defaultPort;
    private final boolean cacheable;
    private String [] defaultResourceBundleIds;
    private boolean customHttpsSupported;

    public VirtualHostService(final VirtualHostsService virtualHosts,
                              final HstNode virtualHostNode,
                              final VirtualHostService parentHost,
                              final String hostGroupName,
                              final List<String> cmsLocations,
                              final int defaultPort,
                              final HstNodeLoadingCache hstNodeLoadingCache) throws ModelLoadingException {

        this.parentHost = parentHost;
        this.virtualHosts = virtualHosts;
        this.hostGroupName = StringPool.get(hostGroupName);
        this.cmsLocations = cmsLocations;
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

        if (virtualHostNode.getValueProvider().hasProperty(HstNodeTypes.VIRTUALHOST_PROPERTY_ONLYFORCONTEXTPATH)) {
            log.warn("Property '{}' on Mount '{}' is deprecated. Use property '{}' instead",
                    HstNodeTypes.VIRTUALHOST_PROPERTY_ONLYFORCONTEXTPATH, virtualHostNode.getValueProvider().getPath(),
                    HstNodeTypes.VIRTUALHOST_PROPERTY_CONTEXTPATH);
            contextPath = virtualHostNode.getValueProvider().getString(HstNodeTypes.VIRTUALHOST_PROPERTY_ONLYFORCONTEXTPATH);
        }

        if (virtualHostNode.getValueProvider().hasProperty(HstNodeTypes.VIRTUALHOST_PROPERTY_CONTEXTPATH)) {
            this.contextPath = virtualHostNode.getValueProvider().getString(HstNodeTypes.VIRTUALHOST_PROPERTY_CONTEXTPATH);
        }
        
        if (contextPath == null) {
            if (parentHost == null) {
                contextPath = virtualHosts.getDefaultContextPath();
            } else {
                contextPath = parentHost.getContextPath();
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

        if(virtualHostNode.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROEPRTY_SCHEME_AGNOSTIC)) {
            schemeAgnostic = virtualHostNode.getValueProvider().getBoolean(HstNodeTypes.GENERAL_PROEPRTY_SCHEME_AGNOSTIC);
        }

        if(virtualHostNode.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE)) {
            schemeNotMatchingResponseCode = (int)virtualHostNode.getValueProvider().getLong(HstNodeTypes.GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE).longValue();
            if (!ConfigurationUtils.isSupportedSchemeNotMatchingResponseCode(schemeNotMatchingResponseCode)) {
                log.warn("Invalid '{}' configured on '{}'. Use inherited value. Supported values are '{}'", new String[]{HstNodeTypes.GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE,
                        virtualHostNode.getValueProvider().getPath(), ConfigurationUtils.suppertedSchemeNotMatchingResponseCodesAsString()});
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

        String fullName = virtualHostNode.getValueProvider().getName();
        String[] nameSegments = fullName.split("\\.");

        VirtualHostService attachPortMountToHost = this;

        if(nameSegments.length > 1) {
            // check whether the hostname is an ip adres as only ip adresses are allowed to contain a "." in their name. For example www.onehippo.org should be configured
            // hierarchically
            for(String segment : nameSegments){
               try {
                   Integer.parseInt(segment);
               } catch (NumberFormatException e) {
                   // one of the segments was not an integer. This is not a valid hostname
                   throw new ModelLoadingException("Node hst:virtualhost is not allowed to be '"+fullName+"'. Only ip-addresses are allowed to have a '.' in the nodename. Re-configure the host to a hierarchical structure");
               }
            }

            // if the fullName is for example 127.0.0.1, then this items name is '1', its child is 0 which has a child 0, which has
            // the last child is '127'
            this.name = StringPool.get(nameSegments[nameSegments.length - 1]);
            // add child host services
            int depth = nameSegments.length - 2;
            if(depth > -1 ) {
                VirtualHostService childHost = new VirtualHostService(this, nameSegments, depth, hostGroupName, cmsLocations, defaultPort);
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
            this.name = virtualHostNode.getValueProvider().getName().toLowerCase();
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
                    Mount mount = new MountService(mountNode, null, attachPortMountToHost, hstNodeLoadingCache, defaultPort);
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
                    VirtualHostService childHost = new VirtualHostService(virtualHosts, child, attachPortMountToHost, hostGroupName, cmsLocations, defaultPort, hstNodeLoadingCache);
                    attachPortMountToHost.childVirtualHosts.put(childHost.name, childHost);
                } catch (ModelLoadingException e) {
                    log.error("Skipping incorrect virtual host for node '"+child.getValueProvider().getPath()+"'" ,e);
                }

            } else if (HstNodeTypes.NODETYPE_HST_PORTMOUNT.equals(child.getNodeTypeName())){
                try {
                MutablePortMount portMount = new PortMountService(child, attachPortMountToHost, hstNodeLoadingCache);
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
                              final List<String> cmsLocations,
                              final Integer defaultPort) {
        this.parentHost = parent;
        this.virtualHosts = parent.virtualHosts;
        this.hostGroupName = hostGroupName;
        this.cmsLocations = cmsLocations;
        this.defaultPort = defaultPort;
        this.scheme = parent.scheme;
        this.schemeAgnostic = parent.schemeAgnostic;
        this.schemeNotMatchingResponseCode = parent.schemeNotMatchingResponseCode;
        this.locale = parent.locale;
        this.homepage = parent.homepage;
        this.pageNotFound = parent.pageNotFound;
        this.versionInPreviewHeader = parent.versionInPreviewHeader;
        this.contextPathInUrl = parent.contextPathInUrl;
        this.contextPath = parent.contextPath;
        this.showPort = parent.showPort;
        this.cacheable = parent.cacheable;
        this.defaultResourceBundleIds = parent.defaultResourceBundleIds;
        this.customHttpsSupported = parent.customHttpsSupported;
        this.name = nameSegments[position];
        // add child host services
        int nextPosition = position - 1;
        if(nextPosition > -1 ) {
            VirtualHostService childHost = new VirtualHostService(this,nameSegments, nextPosition, hostGroupName, cmsLocations, defaultPort);
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

    @Deprecated
    public String onlyForContextPath() {
        return contextPath;
    }

    public String getContextPath() {
        return contextPath;
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

    @Deprecated
    public String getCmsLocation() {
        if (!cmsLocations.isEmpty()) {
            return  cmsLocations.get(0);
        }
        return null;
    }

    public List<String> getCmsLocations() {
        return cmsLocations;
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
    public String getDefaultResourceBundleId() {
        if (defaultResourceBundleIds == null || defaultResourceBundleIds.length == 0) {
            return null;
        }
        return defaultResourceBundleIds[0];
    }

    public String [] getDefaultResourceBundleIds() {
        if (defaultResourceBundleIds == null) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }

        return (String[]) ArrayUtils.clone(defaultResourceBundleIds);
    }

    @Override
    public boolean isCustomHttpsSupported() {
        return customHttpsSupported;
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
