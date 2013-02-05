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
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

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
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.logging.Logger;
import org.hippoecm.hst.service.ServiceException;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.HstRequestUtils;


public class CustomMountAndVirtualCmsHostAugmenter implements HstConfigurationAugmenter {

    private static final Logger log = HstServices.getLogger(CustomMountAndVirtualCmsHostAugmenter.class.getName());

    private static final String DEFAULT_NOOP_NAMED_PIPELINE =  "NoopPipeline";

    // as long as it is unique for the cms host
    private static final String AUGMENTED_CMS_VIRTUAL_HOST_GROUP =
            CustomMountAndVirtualCmsHostAugmenter.class.getName() + "-" + UUID.randomUUID();

    private String springConfiguredCmsLocation;
    private String mountName;
    private String mountType;
    private String mountNamedPipeline;
    private String noopPipeline = DEFAULT_NOOP_NAMED_PIPELINE;

    public void setSpringConfiguredCmsLocation(String springConfiguredCmsLocation) {
        // the deprecated way how a cmsLocation used to be set in spring configuration. It now should be configured on the hst:virtualhostgroup node
        this.springConfiguredCmsLocation = springConfiguredCmsLocation;
    }

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
        // first we try to find all the cmsLocations that need to be added.
        // for every host group, we fetch just virtualhost and ask for its cmsLocation: Although it is configured
        // on the hst:virtualhostgroup node, it is inherited in every virtualhost
        Set<String> cmsLocations = new HashSet<String>();
        for (Map<String, MutableVirtualHost> rootVirtualHosts : hosts.getRootVirtualHostsByGroup().values()) {
            if (rootVirtualHosts.isEmpty()) {
                continue;
            }
            MutableVirtualHost host = rootVirtualHosts.values().iterator().next();
            if (host.getCmsLocation() != null) {
                cmsLocations.add(host.getCmsLocation());
            }
        }

        if (cmsLocations.isEmpty()) {
            if (springConfiguredCmsLocation != null) {
                log.info("No cms locations configured on hst:hostgroup nodes. Use the cms location '{}' from Spring configuration", cmsLocations);
                cmsLocations.add(springConfiguredCmsLocation);
            }
        }

        for (String cmsLocation : cmsLocations) {
            try {
                URI uri = new URI(cmsLocation);
                String cmsCustomMountHostName = uri.getHost();

                // get the host segments in reversed order. For example 127.0.0.1 --> {"1", "0", "0", "127"}
                String[] hostSegments = cmsCustomMountHostName.split("\\.");
                reverse(hostSegments);
                VirtualHost host = null;
                // try to find the cmsRestHostName host. If not present, we add a complete new one to a unique new host group
                // It can map in multiple host groups to a 'partial' hostName, that does not contain a portMount with a rootMount.
                // Hence, we *only* count the host if it also contains a portMount on port 0 or on the
                // port of the cmsLocation AND a hst:root Mount at least in that p
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
                        // portMount must contain a hstRoot
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
                        // we have a correct host. Stop
                        break;
                    }
                }

                if (host == null) {
                    // add the hostName + mount
                    VirtualHost newHost = new CustomVirtualHost(hosts, hostSegments, cmsLocation, 0);
                    // get the last one added
                    hosts.addVirtualHost((MutableVirtualHost) newHost);
                    host = newHost;
                    while (!host.getChildHosts().isEmpty()) {
                        host = host.getChildHosts().get(0);
                    }
                }

                // now check whether to add a portMount
                // first check portMount of the cmsLocation. then a port agnostic PortMount with port 0
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
                    // add a port Mount with port equal to the configured port. If port is -1, we add the default 0 port
                    if (port == -1) {
                        portMount = new CustomPortMount(0);
                    } else {
                        portMount = new CustomPortMount(port);
                    }
                    if (host instanceof CustomVirtualHost) {
                        ((CustomVirtualHost) host).setPortMount((MutablePortMount) portMount);
                    } else {
                        ((MutableVirtualHost) host).addPortMount((MutablePortMount) portMount);
                    }
                }

                // now check the hst:root presence on the portMount. If not add a hst:root + custom mount
                Mount rootMount = portMount.getRootMount();
                Mount customMount = null;
                if (rootMount == null) {
                    MutableMount rootMountPlusCustomMount = new CustomMount(host, mountType, noopPipeline);
                    if (!(portMount instanceof MutablePortMount)) {
                        log.error("Unable to add custom mount '{}' to the host group with CMS location '{}' because found portMount not of type MutablePortMount.", mountName, cmsLocation);
                        continue;
                    }
                    ((MutablePortMount) portMount).setRootMount(rootMountPlusCustomMount);
                } else {
                    customMount = rootMount.getChildMount(mountName);
                    if (customMount == null) {
                        if (!(rootMount instanceof MutableMount)) {
                            log.error("Unable to add custom mount '{}' to the host group with CMS location '{}' because found rootMount not of type MutableMount.", mountName, cmsLocation);
                            continue;
                        }
                        customMount = new CustomMount(mountName, mountType, mountNamedPipeline, rootMount, host);
                        ((MutableMount) rootMount).addMount((MutableMount) customMount);
                    } else {
                        log.info("There is an explicit custom Mount '{}' for CMS location '{}'. This mount can be removed from configuration" +
                                " as it will be auto-created by the HST", mountName, cmsLocation);
                        continue;
                    }
                }
                if (customMount != null) {
                    log.info("Successfully automatically created custom mount for cmsLocation '{}'. Created Mount = {}", cmsLocation, customMount);
                }

            } catch (URISyntaxException e) {
                log.warn("'{}' is an invalid cmsLocation. The mount '{}' won't be available for hosts in that hostGroup.",
                        cmsLocation, mountName);
                continue;
            } catch (ServiceException e) {
                log.error("Unable to add custom cms host mount '" + mountName + "'.", e);
            } catch (IllegalArgumentException e) {
                log.warn("Unable to add custom cms host mount '" + mountName + "'. Illegal argument: " + e.getMessage());
            }
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
        private Map<String,VirtualHost> childs = new HashMap<String, VirtualHost>();
        private String name;
        private String hostName;
        private MutablePortMount portMount;
        private String cmsLocation;

        private CustomVirtualHost(VirtualHosts virtualHosts, String[] hostSegments, String cmsLocation, int position) throws ServiceException {
            this.virtualHosts = virtualHosts;
            name = hostSegments[position];
            this.cmsLocation = cmsLocation;
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
            if(position == hostSegments.length) {
                // done with adding hosts 
            } else {
                 childs.put(hostSegments[position], new CustomVirtualHost(virtualHosts, hostSegments, cmsLocation,  position));
            }
        }
        
        @Override
        public void addVirtualHost(MutableVirtualHost virtualHost) throws IllegalArgumentException {
            if(childs.containsKey(virtualHost.getName())) {
                throw new IllegalArgumentException("virtualHost '"+virtualHost.getName()+"' already exists");
            }
            childs.put(virtualHost.getName(), virtualHost);
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
        
        @Override
        public String onlyForContextPath() {
            return virtualHosts.getDefaultContextPath();
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
            return AUGMENTED_CMS_VIRTUAL_HOST_GROUP;
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
            StringBuilder builder = new StringBuilder();
            String scheme = this.getScheme();
            if (scheme == null) {
                scheme = "http";
            }
            String serverName = HstRequestUtils.getFarthestRequestHost(request, false);
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
        public String getCmsLocation() {
            return cmsLocation;
        }
        

        @Override
        public String toString() {
            return "CustomVirtualHost [name=" + name + ", hostName=" + hostName + "]";
        }

    }
    
    private class CustomPortMount implements MutablePortMount {

        private int port;
        private Mount rootMount;
        
        private CustomPortMount(int port) throws ServiceException {
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

    private final static String fakeNonExistingPath = "/fakePath/"+UUID.randomUUID().toString();
    
    private class CustomMount implements MutableMount {

        private VirtualHost virtualHost;
        private Mount parent;
        // just a unique alias
        private String alias = "randomAlias"+UUID.randomUUID().toString();
        private String identifier = "randomIdentifer"+UUID.randomUUID().toString();
        private String name;
        private String namedPipeline;
        private Map<String, Mount> childs = new HashMap<String, Mount>();
        private String mountPath;
        private String type;
        private List<String> types;
        
        /**
         * Creates a hst:root Mount + the child custom mount
         * @param virtualHost
         * @param namedPipeline 
         * @throws org.hippoecm.hst.service.ServiceException
         */
        private CustomMount(VirtualHost virtualHost, String type, String namedPipeline) throws ServiceException {
            this.virtualHost = virtualHost;
            name = HstNodeTypes.MOUNT_HST_ROOTNAME;
            mountPath = "";
            type = Mount.LIVE_NAME;
            types = Arrays.asList(type);
            this.namedPipeline = namedPipeline;
            // the hst:root mount has a namedPipeline equal to null and can never be used
            Mount customRootMount = new CustomMount(mountName, type, mountNamedPipeline, this, virtualHost);
            childs.put(customRootMount.getName(), customRootMount);
            ((MutableVirtualHosts)virtualHost.getVirtualHosts()).addMount(this);
        }

        /**
         * Creates only the custom mount
         * @param name
         * @param namedPipeline
         * @param parent
         * @param virtualHost
         * @throws org.hippoecm.hst.service.ServiceException
         */
        public CustomMount(String name, String type, String namedPipeline, Mount parent, VirtualHost virtualHost) throws ServiceException {
           this.name = name;
           this.namedPipeline = namedPipeline;
           this.parent = parent;
           this.type = type;
           types = Arrays.asList(type);
           this.virtualHost = virtualHost;
           mountPath = parent.getMountPath() + "/" + name;
        }

        @Override
        public void addMount(MutableMount mount) throws IllegalArgumentException, ServiceException {
           if(childs.containsKey(mount.getName())) {
                throw new IllegalArgumentException("Cannot add Mount with name '"+mount.getName()+"' because already exists for " + this.toString());
            }
           childs.put(mount.getName(), mount);
           ((MutableVirtualHosts)virtualHost.getVirtualHosts()).addMount(this);
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

        @Override
        public String onlyForContextPath() {
            return getVirtualHost().onlyForContextPath();
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
        public String getEmbeddedMountPath() {
            return fakeNonExistingPath;
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

        @Override
        public String getDefaultResourceBundleId() {
            return null;
        }


        @Override
        public void setChannelInfo(ChannelInfo info) {
            // nothing
        }

        @Override
        public void setChannel(final Channel channel) throws UnsupportedOperationException {
            throw new UnsupportedOperationException(this.getClass().getName() + " does not support setChannel");
        }

        @Override
        public String toString() {
            return "CustomMount [virtualHost=" + virtualHost.getHostName() + ", parent=" + parent.getName() + ", alias=" + alias
                    + ", identifier=" + identifier + ", name=" + name + ", namedPipeline=" + namedPipeline
                    + ", childs=" + childs + ", mountPath=" + mountPath + ", types=" + types + ", getAlias()="
                    + getAlias() + ", isMapped()=" + isMapped() + ", isPortInUrl()=" + isPortInUrl() + ", isSite()="
                    + isSite() + ", getPort()=" + getPort() + ", onlyForContextPath()=" + onlyForContextPath()
                    + ", getType()=" + getType() + ", getIdentifier()="+ getIdentifier() + "]";
        }

        @Override
        public String getCmsLocation() {
            if(virtualHost instanceof MutableVirtualHost) {
                return ((MutableVirtualHost)virtualHost).getCmsLocation();
            } else {
                log.warn("Can only get cms location of a MutableVirtualHost. '{}' is not a MutableVirtualHost", virtualHost);
            }
            return null;
        }

        @Override
        public String getLockedBy() {
            return null;
        }

        @Override
        public void setLockedBy(final String userId) {
            throw new UnsupportedOperationException("CustomMount does not support locking");
        }

        @Override
        public Calendar getLockedOn() {
            return null;
        }

        @Override
        public void setLockedOn(final Calendar lockedOn) {
            throw new UnsupportedOperationException("CustomMount does not support locking");
        }
    }

}
