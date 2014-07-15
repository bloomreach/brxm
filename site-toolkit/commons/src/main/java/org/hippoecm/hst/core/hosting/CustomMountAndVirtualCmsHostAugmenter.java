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

import java.net.URI;
import java.net.URISyntaxException;
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


public class CustomMountAndVirtualCmsHostAugmenter implements HstConfigurationAugmenter {

    private static final Logger log = LoggerFactory.getLogger(CustomMountAndVirtualCmsHostAugmenter.class);

    private static final String DEFAULT_NOOP_NAMED_PIPELINE = "NoopPipeline";

    private static final String[] EMPTY_ARRAY = {};

    private String mountName;
    private String mountType;
    private String mountNamedPipeline;
    private String noopPipeline = DEFAULT_NOOP_NAMED_PIPELINE;

    public void setMountName(String mountName) {
        this.mountName = mountName;
    }

    public void setMountNamedPipeline(String mountNamedPipeline) {
        this.mountNamedPipeline = mountNamedPipeline;
    }

    public void setMountType(final String mountType) {
        this.mountType = mountType;
    }

    public void setNoopPipeline(String noopPipeline) {
        this.noopPipeline = noopPipeline;
    }

    /**
     * Every virtual hostgroup that has a hst:cmslocation property defined we try to add the correct mount for.
     */
    @Override
    public void augment(final MutableVirtualHosts hosts) throws ContainerException {
        if (!validateState()) {
            return;
        }
        log.info("Trying to augment cms host custom mount '{}' of type '{}'and pipeline '{}'",
                new String[]{mountName, mountType, mountNamedPipeline});

        for (Map.Entry<String, Map<String, MutableVirtualHost>> entry : hosts.getRootVirtualHostsByGroup().entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }

            final String hostGroup = entry.getKey();

            // every host within one hostgroup has all the cms locations
            final List<String> cmsLocationsForHostGroup = entry.getValue().values().iterator().next().getCmsLocations();
            if (cmsLocationsForHostGroup.isEmpty()) {
                log.info("No cms locations configured on hst:hostgroup {}.", hostGroup);
                continue;
            }

            for (String cmsLocation : cmsLocationsForHostGroup) {
                try {
                    addCmsLocationToHostGroup(cmsLocation, hostGroup, cmsLocationsForHostGroup, hosts);
                } catch (Exception e) {
                    log.error("Exception while trying to add cmsLocation '" + cmsLocation +
                            "' to hostGroup '" + hostGroup + "'. Skip cms location.", e);
                }
            }

        }

    }

    private void addCmsLocationToHostGroup(final String cmsLocation,
                                           final String hostGroup,
                                           final List<String> hostGroupCmsLocations,
                                           final MutableVirtualHosts hosts) {
        try {
            URI uri = new URI(cmsLocation);
            String cmsCustomMountHostName = uri.getHost();

            // get the host segments in reversed order. For example 127.0.0.1 --> {"1", "0", "0", "127"}
            String[] hostSegments = cmsCustomMountHostName.split("\\.");
            reverse(hostSegments);

            VirtualHost host;

            // if there is already a host for the cms host we want to add, and this host is part of a different
            // hostgroup, we CANNOT add the cms host and log a warning
            for (Map<String, MutableVirtualHost> rootVirtualHostMap : hosts.getRootVirtualHostsByGroup().values()) {
                int i = 0;
                host = null;
                while (i < hostSegments.length) {
                    if (i == 0) {
                        host = rootVirtualHostMap.get(hostSegments[i]);
                    } else {
                        host = host.getChildHost(hostSegments[i]);
                    }
                    if (host == null) {
                        // cmsRestHostName does not yet exist in this hostGroup
                        break;
                    }
                    i++;
                }

                if (host != null) {
                    // We have found the correct host. Now check whether also has portMount and that
                    // portMount must contain a hst:root
                    PortMount portMount = null;
                    int port = uri.getPort();
                    if (port != 0) {
                        portMount = host.getPortMount(port);
                    }
                    if (portMount == null) {
                        // check default port 0
                        portMount = host.getPortMount(0);
                    }

                    if (portMount == null) {
                        continue;
                    }
                    if (portMount.getRootMount() == null) {
                        continue;
                    }
                    // we have a host for cms location including a portmount & rootmount.
                    // if this host belongs to the wrong hostgroup, we cannot add the host
                    if (!host.getHostGroupName().equals(hostGroup)) {
                        log.warn("Cannot add cms location {} to hostGroup {} because the exact cms host is already " +
                                "available below the hostGroup {}. That is not allowed", cmsLocation,
                                hostGroup, host.getHostGroupName());
                        return;
                    }
                    break;
                }
            }
            final Map<String, MutableVirtualHost> virtualHostsForHostGroup = hosts.getRootVirtualHostsByGroup().get(hostGroup);
            int i = 0;
            MutableVirtualHost ancestorHost = null;
            // now find the (partial) cms host for the <code>hostGroup</code>
            host = null;
            while (i < hostSegments.length) {
                if (i == 0) {
                    host = virtualHostsForHostGroup.get(hostSegments[i]);
                } else {
                    host = host.getChildHost(hostSegments[i]);
                }
                if (host == null) {
                    // cmsRestHostName does not yet exist in this hostGroup
                    break;
                }
                ancestorHost = (MutableVirtualHost)host;
                i++;
            }

            VirtualHost cmsLocationHost;
            if (ancestorHost == null) {
                // host completely missing
                VirtualHost newHost = new CustomVirtualHost(hosts, hostSegments, hostGroupCmsLocations, 0, hostGroup);
                // get the last one added
                hosts.addVirtualHost((MutableVirtualHost) newHost);
                cmsLocationHost = newHost;
                while (!cmsLocationHost.getChildHosts().isEmpty()) {
                    cmsLocationHost = cmsLocationHost.getChildHosts().get(0);
                }
            } else if (i == hostSegments.length) {
                // entire host is present
                cmsLocationHost = ancestorHost;
            } else if (i < hostSegments.length){
                // not entire cms host is available. Add missing host parts
                // partial add
                String[] missingHostSegments = Arrays.copyOfRange(hostSegments, i, hostSegments.length);
                cmsLocationHost = new CustomVirtualHost(hosts, ancestorHost, missingHostSegments, hostGroupCmsLocations, 0, hostGroup);
                while (!cmsLocationHost.getChildHosts().isEmpty()) {
                    cmsLocationHost = cmsLocationHost.getChildHosts().get(0);
                }
                log.info("Added cms host '{}' for host group '{}'", cmsLocationHost.getHomePage(), cmsLocationHost.getHostGroupName());
            } else {
                throw new IllegalStateException("Cannot add cms location '"+cmsLocation+"'");
            }

            // now check whether to add a portMount
            // first check portMount of the cmsLocation. then a port agnostic PortMount with port 0
            PortMount portMount = null;
            int port = uri.getPort();
            if (port != 0) {
                portMount = cmsLocationHost.getPortMount(port);
            }
            if (portMount == null) {
                // check default port 0
                portMount = cmsLocationHost.getPortMount(0);
            }

            if (portMount == null) {
                // add a port Mount with port equal to the configured port. If port is -1, we add the default 0 port
                if (port == -1) {
                    portMount = new CustomPortMount(0);
                } else {
                    portMount = new CustomPortMount(port);
                }
                if (cmsLocationHost instanceof CustomVirtualHost) {
                    ((CustomVirtualHost) cmsLocationHost).setPortMount((MutablePortMount) portMount);
                } else {
                    ((MutableVirtualHost) cmsLocationHost).addPortMount((MutablePortMount) portMount);
                }
            }

            // now check the hst:root presence on the portMount. If not add a hst:root + custom mount
            Mount rootMount = portMount.getRootMount();
            Mount customMount = null;
            if (rootMount == null) {
                MutableMount rootMountPlusCustomMount = new CustomMount(cmsLocationHost, mountType, noopPipeline);
                if (!(portMount instanceof MutablePortMount)) {
                    log.error("Unable to add custom mount '{}' to the host group with CMS location '{}' because " +
                            "found portMount not of type MutablePortMount.", mountName, cmsLocation);
                    return;
                }
                ((MutablePortMount) portMount).setRootMount(rootMountPlusCustomMount);
            } else {
                customMount = rootMount.getChildMount(mountName);
                if (customMount == null) {
                    if (!(rootMount instanceof MutableMount)) {
                        log.error("Unable to add custom mount '{}' to the host group with CMS location '{}' " +
                                "because found rootMount not of type MutableMount.", mountName, cmsLocation);
                        return;
                    }
                    customMount = new CustomMount(mountName, mountType, mountNamedPipeline, rootMount, cmsLocationHost);
                    ((MutableMount) rootMount).addMount((MutableMount) customMount);
                    log.info("Successfully augmented mount {}", customMount, toString());
                } else {
                    log.info("There is an explicit custom Mount '{}' for CMS location '{}'. This mount can be removed from configuration" +
                            " as it will be auto-created by the HST", mountName, cmsLocation);
                    return;
                }
            }
            if (customMount != null) {
                log.info("Successfully automatically created custom mount for cmsLocation '{}'. Created Mount = {}", cmsLocation, customMount);
            }

        } catch (URISyntaxException e) {
            log.warn("'{}' is an invalid cmsLocation. The mount '{}' won't be available for hosts in that hostGroup.",
                    cmsLocation, mountName);
            return;
        } catch (IllegalArgumentException e) {
            log.error("Unable to add custom cms host mount '" + mountName + "'.", e);
        }
    }

    private boolean validateState() throws ContainerException {
        if (mountName == null || mountName.isEmpty()) {
            log.error("No mount name set for {}", this.getClass().getName());
            return false;
        }
        if (mountType == null || mountType.isEmpty()) {
            log.error("No mount type set for {}", this.getClass().getName());
            return false;
        }
        if (mountNamedPipeline == null || mountNamedPipeline.isEmpty()) {
            log.error("No mount named pipeline set for {}", this.getClass().getName());
            return false;
        }
        return true;
    }

    static void reverse(String[] s) {
        List<String> l = Arrays.asList(s);
        Collections.reverse(l);
    }

    private class CustomVirtualHost implements MutableVirtualHost {
        private VirtualHosts virtualHosts;
        private Map<String, VirtualHost> children = new HashMap<>();
        private String name;
        private String hostName;
        private MutablePortMount portMount;
        private final List<String> cmsLocations;
        private final String hostGroupName;

        private CustomVirtualHost(final VirtualHosts virtualHosts,
                                  final String[] hostSegments,
                                  final List<String> cmsLocations,
                                  int position,
                                  final String hostGroupName) {
            this.virtualHosts = virtualHosts;
            this.hostGroupName = hostGroupName;
            name = hostSegments[position];
            this.cmsLocations = cmsLocations;
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
                // done with adding hosts
            } else {
                children.put(hostSegments[position], new CustomVirtualHost(virtualHosts, hostSegments, cmsLocations, position, hostGroupName));
            }
        }

        private CustomVirtualHost(final VirtualHosts virtualHosts,
                                  final MutableVirtualHost ancestor,
                                  final String[] hostSegments,
                                  final List<String> cmsLocations,
                                  int position,
                                  final String hostGroupName) {
            this.virtualHosts = virtualHosts;
            this.hostGroupName = hostGroupName;
            name = hostSegments[position];
            this.cmsLocations = cmsLocations;
            hostName = hostSegments[position] + "." + ancestor.getHostName();
            position++;
            ancestor.addVirtualHost(this);
            if (position < hostSegments.length) {
                new CustomVirtualHost(virtualHosts, this, hostSegments, cmsLocations, position, hostGroupName);
            }
        }

        @Override
        public void addVirtualHost(MutableVirtualHost virtualHost) throws IllegalArgumentException {
            if (children.containsKey(virtualHost.getName())) {
                throw new IllegalArgumentException("virtualHost '" + virtualHost.getName() + "' already exists");
            }
            children.put(virtualHost.getName(), virtualHost);
        }

        @Override
        public PortMount getPortMount(int portNumber) {
            // the portMount for the cms rest host is port agnostic
            return portMount;
        }

        public void setPortMount(MutablePortMount portMount) {
            this.portMount = portMount;
        }

        @Override
        public void addPortMount(MutablePortMount portMount) throws IllegalArgumentException {
            log.warn("Cannot add a portMount to a CmsRestVirtualHost");
            return;
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
            // do not set to true : for _cmsrest, the port and contextpath must not be taken from the mount
            return false;
        }

        @Override
        public boolean isContextPathInUrl() {
            // do not set to true : for _cmsrest, the port and contextpath must not be taken from the mount
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
            return hostGroupName;
        }


        @Override
        public List<VirtualHost> getChildHosts() {
            return Collections.unmodifiableList(new ArrayList<VirtualHost>(children.values()));
        }

        @Override
        public VirtualHost getChildHost(String name) {
            return children.get(name);
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
            final String scheme = HstRequestUtils.getFarthestRequestScheme(request);
            final String serverName = HstRequestUtils.getFarthestRequestHost(request, false);
            builder.append(scheme);
            builder.append("://").append(serverName);
            return builder.toString();
        }

        @Override
        public boolean isVersionInPreviewHeader() {
            return false;
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
        public String[] getDefaultResourceBundleIds() {
            return EMPTY_ARRAY;
        }

        @Override
        public boolean isCustomHttpsSupported() {
            return false;
        }

        @Deprecated
        @Override
        public String getCmsLocation() {
            if (!cmsLocations.isEmpty()) {
                return cmsLocations.get(0);
            }
            return null;
        }

        public List<String> getCmsLocations() {
            return cmsLocations;
        }

        @Override
        public String toString() {
            return "CustomVirtualHost [name=" + name + ", hostName=" + hostName + "]";
        }

    }

    private class CustomPortMount implements MutablePortMount {

        private int port;
        private Mount rootMount;

        private CustomPortMount(int port) {
            this.port = port;
        }

        @Override
        public int getPortNumber() {
            return port;
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
            return "CustomPortMount [port=" + port + "]";
        }

    }

    private final static String fakeNonExistingPath = "/fakePath/" + UUID.randomUUID().toString();

    private class CustomMount implements MutableMount {

        private VirtualHost virtualHost;
        private Mount parent;
        // just a unique alias
        private String alias = "randomAlias" + UUID.randomUUID().toString();
        private String identifier = "randomIdentifer" + UUID.randomUUID().toString();
        private String name;
        private String namedPipeline;
        private Map<String, Mount> childs = new HashMap<String, Mount>();
        private String mountPath;
        private String type;
        private List<String> types;

        /**
         * Creates a hst:root Mount + the child custom mount
         *
         * @param virtualHost
         * @param namedPipeline
         */
        private CustomMount(VirtualHost virtualHost, String type, String namedPipeline) {
            this.virtualHost = virtualHost;
            name = HstNodeTypes.MOUNT_HST_ROOTNAME;
            mountPath = "";
            this.type = type;
            types = Arrays.asList(type);
            this.namedPipeline = namedPipeline;
            // the hst:root mount has a namedPipeline equal to null and can never be used
            Mount customRootMount = new CustomMount(mountName, type, mountNamedPipeline, this, virtualHost);
            childs.put(customRootMount.getName(), customRootMount);
            ((MutableVirtualHosts) virtualHost.getVirtualHosts()).addMount(this);
        }

        /**
         * Creates only the custom mount
         *
         * @param name
         * @param namedPipeline
         * @param parent
         * @param virtualHost
         */
        public CustomMount(String name, String type, String namedPipeline, Mount parent, VirtualHost virtualHost) {
            this.name = name;
            this.namedPipeline = namedPipeline;
            this.parent = parent;
            this.type = type;
            types = Arrays.asList(type);
            this.virtualHost = virtualHost;
            mountPath = parent.getMountPath() + "/" + name;
        }

        @Override
        public void addMount(MutableMount mount) throws IllegalArgumentException {
            if (childs.containsKey(mount.getName())) {
                throw new IllegalArgumentException("Cannot add Mount with name '" + mount.getName() + "' because already exists for " + this.toString());
            }
            childs.put(mount.getName(), mount);
            ((MutableVirtualHosts) virtualHost.getVirtualHosts()).addMount(this);
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
            // must be false for CMS REST MOUNT
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
            // must be false for CMS REST MOUNT
            return false;
        }

        @Override
        public boolean isPortInUrl() {
            // must be false for CMS REST MOUNT
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
            // this has to be LIVE : the links that are created through the CMS REST MOUNT
            // need to be 'live' links. The CMS will decorate these live links to preview
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
        public List<String> getPropertyNames() {
            return Collections.emptyList();
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
        public String[] getDefaultResourceBundleIds() {
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
            if (virtualHost instanceof MutableVirtualHost) {
                if (((MutableVirtualHost) virtualHost).getCmsLocations().isEmpty()) {
                    return null;
                }
                return ((MutableVirtualHost) virtualHost).getCmsLocations().get(0);
            } else {
                log.warn("Can only get cms location of a MutableVirtualHost. '{}' is not a MutableVirtualHost", virtualHost);
                return null;
            }
        }

        @Override
        public List<String> getCmsLocations() {
            if (virtualHost instanceof MutableVirtualHost) {
                return ((MutableVirtualHost) virtualHost).getCmsLocations();
            } else {
                log.warn("Can only get cms locations of a MutableVirtualHost. '{}' is not a MutableVirtualHost", virtualHost);
                return Collections.emptyList();
            }
        }
    }

}
