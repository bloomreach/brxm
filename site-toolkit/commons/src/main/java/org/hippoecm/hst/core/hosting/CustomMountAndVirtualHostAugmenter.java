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
package org.hippoecm.hst.core.hosting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.MutableMount;
import org.hippoecm.hst.configuration.hosting.MutablePortMount;
import org.hippoecm.hst.configuration.hosting.MutableVirtualHost;
import org.hippoecm.hst.configuration.hosting.MutableVirtualHosts;
import org.hippoecm.hst.configuration.hosting.PortMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstConfigurationAugmenter;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomMountAndVirtualHostAugmenter implements HstConfigurationAugmenter {

    private static final Logger log = LoggerFactory.getLogger(CustomMountAndVirtualHostAugmenter.class);

    private final static String DEFAULT_CUSTOM_HOST_NAME = "127.0.0.1";
    private final static String DEFAULT_NOOP_NAMED_PIPELINE = "NoopPipeline";

    private static final String [] EMPTY_ARRAY = {};

    private String customMountName = null;
    private String customMountNamedPipeline = null;
    private String customMountType = Mount.LIVE_NAME;
    private String customHostName = DEFAULT_CUSTOM_HOST_NAME;
    private String noopPipeline = DEFAULT_NOOP_NAMED_PIPELINE;


    public void setCustomMountName(String customMountName) {
        this.customMountName = customMountName;
    }

    public void setCustomMountNamedPipeline(String customMountNamedPipeline) {
        this.customMountNamedPipeline = customMountNamedPipeline;
    }

    public void setCustomMountType(String customMountType) {
        this.customMountType = customMountType;
    }

    public void setCustomHostName(String customHostName) {
        this.customHostName = customHostName;
    }

    public void setNoopPipeline(String noopPipeline) {
        this.noopPipeline = noopPipeline;
    }

    @Override
    public void augment(final MutableVirtualHosts hosts) throws ContainerException {
        try {
            if (customMountName == null || customMountName.isEmpty()) {
                log.error("{} can only work when the customMountName is not null or empty.", this.getClass().getName());
                return;
            }
            if (customMountNamedPipeline == null || customMountNamedPipeline.isEmpty()) {
                log.error("{} can only work when the customMountNamedPipeline is not null or empty.", this.getClass().getName());
                return;
            }
            if (customHostName == null || customHostName.isEmpty()) {
                log.error("{} can only work when the customHostName is not null or empty.", this.getClass().getName());
                return;
            }

            log.info("Trying to augment custom hostName '{}' with mount '{}' and pipeline '{}'",
                    new String[]{customHostName,customMountName,customMountNamedPipeline});
            // get the host segments in reversed order. For example 127.0.0.1 --> {"1", "0", "0", "127"}
            String[] hostSegments = customHostName.split("\\.");
            reverse(hostSegments);
            VirtualHost customHost = null;

            // try to find the customHostName host. If not present, it needs to be added entirely
            for (Map<String, MutableVirtualHost> rootVirtualHostMap : hosts.getRootVirtualHostsByGroup().values()) {
                int i = 0;
                customHost = null;
                while (i < hostSegments.length) {
                    if (i == 0) {
                        customHost = rootVirtualHostMap.get(hostSegments[i]);
                    } else {
                        customHost = customHost.getChildHost(hostSegments[i]);
                    }
                    if (customHost == null) {
                        // customHostName does not yet exist in this hostGroup
                        break;
                    }
                    i++;
                }
                if (customHost != null) {
                    // We have found the correct custom host; stop
                    break;
                }
            }

            if (customHost == null) {
                // add the cmsRestHostName + mount
                MutableVirtualHost cmsVirtualHost = new CustomVirtualHost(hosts, hostSegments, 0);
                hosts.addVirtualHost(cmsVirtualHost);
            } else if (customHost instanceof MutableVirtualHost) {
                // only add the needed portMount / hst:root mount / _cmsrest mount
                // check portMount for port 0
                PortMount portMount = customHost.getPortMount(0);
                if (portMount == null) {
                    MutablePortMount cmsRestPortMount = new CustomPortMount(customHost);
                    ((MutableVirtualHost) customHost).addPortMount(cmsRestPortMount);
                } else if (portMount instanceof MutablePortMount) {
                    Mount rootMount = portMount.getRootMount();
                    if (rootMount == null) {
                        MutableMount customRootMount = new CustomMount(customHost, noopPipeline);
                        ((MutablePortMount) portMount).setRootMount(customRootMount);
                    } else {
                        Mount customMount = rootMount.getChildMount(customMountName);
                        if (customMount != null) {
                            log.info("There is an implicit '{}' mount configured, hence no programmatic added custom mount", customMountName);
                        } else if (rootMount instanceof MutableMount) {
                            // add a customMount to the root mount
                            MutableMount mountToAugment = new CustomMount(customMountName, customMountNamedPipeline, customMountType, rootMount, customHost);
                            ((MutableMount) rootMount).addMount(mountToAugment);
                            log.info("Successfully augmented mount {}", mountToAugment,toString());
                        } else {
                            log.error("Unable to add the custom mount {} for pipeline {}.", customMountName, customMountNamedPipeline);
                        }
                    }
                } else {
                    log.error("Unable to add the custom mount {} for pipeline {}.", customMountName, customMountNamedPipeline);
                }
            } else {
                log.error("Unable to add the custom mount {} for pipeline {}.", customMountName, customMountNamedPipeline);
            }
        } catch (IllegalArgumentException e) {
            log.error("Unable to add the custom mount " + customMountName + "  for pipeline " + customMountNamedPipeline + ". It might already be explicitly configured.", e);
        }

    }

    static void reverse(String[] s) {
        List<String> l = Arrays.asList(s);
        Collections.reverse(l);
    }

    private class CustomVirtualHost implements MutableVirtualHost {
        private VirtualHosts virtualHosts;
        private Map<String, VirtualHost> childs = new HashMap<String, VirtualHost>();
        private String name;
        private String hostName;
        private MutablePortMount portMount;

        private CustomVirtualHost(VirtualHosts virtualHosts, String[] hostSegments, int position) {
            this.virtualHosts = virtualHosts;
            name = hostSegments[position];
            int i = position;
            while (i > -1) {
                if (hostName != null) {
                    hostName = hostName + "." + hostSegments[i];
                } else {
                    hostName = hostSegments[i];
                }
                i--;
            }
            position++;
            if (position == hostSegments.length) {
                // done with hosts. We now need to add the PortMount, hst root mount and the <customMountName> mount
                portMount = new CustomPortMount(this);
                setPortMount(portMount);
            } else {
                childs.put(hostSegments[position], new CustomVirtualHost(virtualHosts, hostSegments, position));
            }
        }

        @Override
        public void addVirtualHost(MutableVirtualHost virtualHost) throws IllegalArgumentException {
            if (childs.containsKey(virtualHost.getName())) {
                throw new IllegalArgumentException("virtualHost '" + virtualHost.getName() + "' already exists");
            }
            childs.put(virtualHost.getName(), virtualHost);
        }

        @Override
        public PortMount getPortMount(int portNumber) {
            // the portMount for the custom host is port agnostic
            return portMount;
        }

        public void setPortMount(MutablePortMount portMount) {
            this.portMount = portMount;
        }

        @Override
        public void addPortMount(MutablePortMount portMount) throws IllegalArgumentException {
            log.warn("Cannot add a portMount to a CustomVirtualHost");
        }

        @Deprecated
        @Override
        public String onlyForContextPath() {
            return null;
        }

        @Override
        public String getContextPath() {
            // the cms host mounts must be contextpath agnostic!
            return null;
        }


        @Override
        public boolean isPortInUrl() {
            // do not set to true : for a custom mount, the port and context path must not be taken from the mount
            return false;
        }

        @Override
        public boolean isContextPathInUrl() {
            // do not set to true : for a custom mount, the port and context path must not be taken from the mount
            return false;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getHostName() {
            return hostName;
        }

        @Override
        public String getHostGroupName() {
            // as long as it is unique for the custom host
            return CustomMountAndVirtualHostAugmenter.class.getName();
        }


        @Override
        public List<VirtualHost> getChildHosts() {
            return Collections.unmodifiableList(new ArrayList<VirtualHost>(childs.values()));
        }

        @Override
        public VirtualHost getChildHost(String name) {
            return childs.get(name);
        }

        @Override
        public VirtualHosts getVirtualHosts() {
            return virtualHosts;
        }

        @Override
        public String getScheme() {
            return "http";
        }

        @Override
        public boolean isSchemeAgnostic() {
            return true;
        }

        @Override
        public int getSchemeNotMatchingResponseCode() {
            return HttpServletResponse.SC_OK;
        }

        @Override
        public String getPageNotFound() {
            return null;
        }

        @Override
        public String getLocale() {
            return null;
        }

        @Override
        public String getHomePage() {
            return null;
        }

        @Override
        public String getBaseURL(HttpServletRequest request) {
            final StringBuilder builder = new StringBuilder();
            builder.append(HstRequestUtils.getFarthestRequestScheme(request));
            builder.append("://").append(HstRequestUtils.getFarthestRequestHost(request, false));
            return builder.toString();
        }

        @Override
        public boolean isVersionInPreviewHeader() {
            return false;
        }

        @Deprecated
        @Override
        public String getCmsLocation() {
            return null;
        }

        @Override
        public List<String> getCmsLocations() {
            return Collections.emptyList();
        }

        @Override
        public boolean isCacheable() {
            return false;
        }

        @Override
        public String getDefaultResourceBundleId() {
            return null;
        }

        @Override
        public String [] getDefaultResourceBundleIds() {
            return EMPTY_ARRAY;
        }

        @Override
        public boolean isCustomHttpsSupported() {
            return false;
        }

        @Override
        public String toString() {
            return "CustomVirtualHost [name=" + name + ", hostName=" + hostName + ", hostGroupName=" + getHostGroupName() + "]";
        }

    }

    private class CustomPortMount implements MutablePortMount {

        private static final int PORT = 0;
        private Mount rootMount;

        private CustomPortMount(VirtualHost virtualHost) {
            rootMount = new CustomMount(virtualHost, noopPipeline);
        }

        @Override
        public int getPortNumber() {
            return PORT;
        }

        @Override
        public Mount getRootMount() {
            return rootMount;
        }

        @Override
        public void setRootMount(MutableMount mount) {
            this.rootMount = mount;
        }

        @Override
        public String toString() {
            return "CustomPortMount [port=" + PORT + "]";
        }

    }

    private final static String fakeNonExistingPath = "/fakePath/" + UUID.randomUUID().toString();

    private class CustomMount implements MutableMount {

        private VirtualHost virtualHost;
        private Mount parent;
        // just a unique alias
        private String alias = "randomAlias" + UUID.randomUUID().toString();
        private String identifier = "randomIdentifier" + UUID.randomUUID().toString();
        private String name;
        private String namedPipeline;
        private String type;
        private Map<String, Mount> childs = new HashMap<String, Mount>();
        private String mountPath;
        private List<String> types;

        // the hst:root mount constructor
        private CustomMount(VirtualHost virtualHost, String namedPipeline) {
            this.virtualHost = virtualHost;
            name = HstNodeTypes.MOUNT_HST_ROOTNAME;
            mountPath = "";
            this.namedPipeline = namedPipeline;
            this.type = customMountType;
            this.types = Collections.singletonList(customMountType);
            // the hst:root mount has a namedPipeline equal to null and can never be used
            Mount customMount = new CustomMount(customMountName, customMountNamedPipeline, customMountType, this, virtualHost);
            childs.put(customMount.getName(), customMount);
            ((MutableVirtualHosts) virtualHost.getVirtualHosts()).addMount(this);
        }

        // the custom mount constructor
        public CustomMount(String name, String namedPipeline, String type, Mount parent, VirtualHost virtualHost) {
            this.name = name;
            this.namedPipeline = namedPipeline;
            this.type = type;
            this.types = Collections.singletonList(type);
            this.parent = parent;
            this.virtualHost = virtualHost;
            ((MutableVirtualHosts) virtualHost.getVirtualHosts()).addMount(this);
            mountPath = parent.getMountPath() + "/" + name;
        }

        @Override
        public void addMount(MutableMount mount) throws IllegalArgumentException {
            if (childs.containsKey(mount.getName())) {
                throw new IllegalArgumentException("Cannot add Mount with name '" + mount.getName() + "' because it already exists for " + this.toString());
            }
            childs.put(mount.getName(), mount);
            ((MutableVirtualHosts) virtualHost.getVirtualHosts()).addMount(mount);
        }

        @Override
        public String getNamedPipeline() {
            return namedPipeline;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getAlias() {
            return alias;
        }

        @Override
        public boolean isMapped() {
            // must be false for custom mounts
            return false;
        }

        @Override
        public Mount getParent() {
            return parent;
        }

        @Override
        public List<Mount> getChildMounts() {
            return Collections.unmodifiableList(new ArrayList<Mount>(childs.values()));
        }

        @Override
        public Mount getChildMount(String name) {
            return childs.get(name);
        }

        @Override
        public VirtualHost getVirtualHost() {
            return virtualHost;
        }


        @Override
        public HstSite getHstSite() {
            // no hst site
            return null;
        }

        @Override
        public boolean isContextPathInUrl() {
            // must be false for custom mounts
            return false;
        }

        @Override
        public boolean isPortInUrl() {
            // must be false for custom mounts
            return false;
        }

        @Override
        public boolean isSite() {
            return false;
        }

        @Override
        public int getPort() {
            return 0;
        }

        @Deprecated
        @Override
        public String onlyForContextPath() {
            return null;
        }

        @Override
        public String getContextPath() {
            // the cms host mounts must be contextpath agnostic!
            return null;
        }


        @Override
        public boolean isAuthenticated() {
            return false;
        }

        @Override
        public Set<String> getRoles() {
            return null;
        }

        @Override
        public Set<String> getUsers() {
            return null;
        }

        @Override
        public boolean isSubjectBasedSession() {
            return false;
        }

        @Override
        public boolean isSessionStateful() {
            return false;
        }

        @Override
        public String getMountPoint() {
            return null;
        }

        @Override
        public String getContentPath() {
            return fakeNonExistingPath;
        }

        @Override
        @Deprecated
        public String getCanonicalContentPath() {
            return fakeNonExistingPath;
        }

        @Override
        public String getMountPath() {
            return mountPath;
        }

        @Override
        public String getHomePage() {
            return null;
        }


        @Override
        public String getPageNotFound() {
            return null;
        }

        @Override
        public String getScheme() {
            return "http";
        }

        @Override
        public boolean isSchemeAgnostic() {
            return true;
        }

        @Override
        public boolean containsMultipleSchemes() {
            return false;
        }

        @Override
        public int getSchemeNotMatchingResponseCode() {
            return HttpServletResponse.SC_OK;
        }

        @Override
        public boolean isPreview() {
            return false;
        }

        @Override
        public boolean isOfType(String type) {
            return getTypes().contains(type);
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public List<String> getTypes() {
            return types;
        }

        @Override
        public boolean isVersionInPreviewHeader() {
            return false;
        }


        @Override
        public String getLocale() {
            return null;
        }

        @Override
        public HstSiteMapMatcher getHstSiteMapMatcher() {
            return null;
        }

        @Override
        public String getFormLoginPage() {
            return null;
        }

        @Override
        public String getProperty(String name) {
            return null;
        }

        @Override
        public Map<String, String> getMountProperties() {
            return Collections.emptyMap();
        }

        @Override
        public String getParameter(String name) {
            return null;
        }

        @Override
        public Map<String, String> getParameters() {
            return Collections.emptyMap();
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }

        @Override
        public <T extends ChannelInfo> T getChannelInfo() {
            return null;
        }

        @Override
        public String getChannelPath() {
            return null;
        }

        @Override
        public Channel getChannel() {
            return null;
        }

        @Override
        public String[] getDefaultSiteMapItemHandlerIds() {
            return null;
        }

        @Override
        public boolean isCacheable() {
            return false;
        }

        @Deprecated
        @Override
        public String getDefaultResourceBundleId() {
            return null;
        }

        @Override
        public String [] getDefaultResourceBundleIds() {
            return EMPTY_ARRAY;
        }

        @Override
        public void setChannelInfo(final ChannelInfo info, final ChannelInfo previewInfo) {
            // nothing
        }

        @Override
        public void setChannel(final Channel channel, final Channel previewChannel) throws UnsupportedOperationException {
            throw new UnsupportedOperationException(this.getClass().getName() + " does not support setChannel");
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("CustomMount [hostName=").append(virtualHost.getHostName())
                    .append(", mountPath = ").append(mountPath).append("]");
            return builder.toString();
        }

        @Deprecated
        @Override
        public String getCmsLocation() {
            // nothing to return for the custom mount
            return null;
        }

        @Override
        public List<String> getCmsLocations() {
            // nothing to return for the custom mount
            return Collections.emptyList();
        }

    }

}

