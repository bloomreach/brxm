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
package org.hippoecm.hst.pagecomposer.configuration.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
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
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.service.ServiceException;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ComposerMountAndVirtualHostAugmenter implements HstConfigurationAugmenter {

    private static final Logger log = LoggerFactory.getLogger(ComposerMountAndVirtualHostAugmenter.class);

    private final static String DEFAULT_COMPOSER_MOUNT_NAME = "_rp";
    private final static String DEFAULT_COMPOSER_MOUNT_NAMED_PIPELINE = "ComposerPipeline";
    private final static String COMPOSER_MOUNT_TYPE = "composer";
    private final static String DEFAULT_NOOP_NAMED_PIPELINE =  "NoopPipeline";

    private String springConfiguredCmsLocation;
    private String composerMountName = DEFAULT_COMPOSER_MOUNT_NAME;
    private String composerMountNamedPipeline = DEFAULT_COMPOSER_MOUNT_NAMED_PIPELINE;
    private String noopPipeline = DEFAULT_NOOP_NAMED_PIPELINE;
  
    public void setSpringConfiguredCmsLocation(String springConfiguredCmsLocation) {
        // the deprecated way how a cmsLocation used to be set in spring configuration. It now should be configured on the hst:virtualhostgroup node
        this.springConfiguredCmsLocation = springConfiguredCmsLocation;
    }

    public void setComposerMountName(String composerMountName) {
        this.composerMountName = composerMountName;
    }

    public void setComposerMountNamedPipeline(String composerMountNamedPipeline) {
        this.composerMountNamedPipeline = composerMountNamedPipeline;
    }
    
    public void setNoopPipeline(String noopPipeline) {
        this.noopPipeline = noopPipeline;
    }

   
    /**
     * Every virtual hostgroup that has a hst:cmslocation property defined we try to add the correct composerMount for.
     * If there is no hst:cmslocation defined on the virtual hostgroup, then we check whether there is cmslocation define by
     * ContainerConfiguration#getString(ContainerConstants.CMS_LOCATION)  : This is a deprecated way to configure the cmslocation
     */
    @Override
    public void augment(HstManager manager) throws RepositoryNotAvailableException {
            if(!(manager.getVirtualHosts() instanceof MutableVirtualHosts )) {
                log.error("{} can only work when the hosts is an instanceof MutableVirtualHosts. The VIEW / GOTO button in cms will not work", this.getClass().getName());
                return;
            }   
            MutableVirtualHosts hosts = (MutableVirtualHosts) manager.getVirtualHosts();
            
            // first we try to find all the cmsLocations that need to be added.
            // for every host group, we fetch just virtualhost and ask for its cmsLocation: Although it is configured 
            // on the hst:virtualhostgroup node, it is inherited in every virtualhost
            Set<String> cmsLocations = new HashSet<String>();
            for(Map<String, MutableVirtualHost> rootVirtualHosts : hosts.getRootVirtualHostsByGroup().values()) {
                if(rootVirtualHosts.isEmpty()) {
                    continue;
                }
                MutableVirtualHost host = rootVirtualHosts.values().iterator().next();
                if(host.getCmsLocation() != null) {
                    cmsLocations.add(host.getCmsLocation());
                }
            }
            
            if(cmsLocations.isEmpty()) {
                if(springConfiguredCmsLocation != null) {
                    log.info("No cms locations configured on hst:hostgroup nodes. Use the cms location '{}' from Spring configuration", cmsLocations);
                    cmsLocations.add(springConfiguredCmsLocation);
                }
            }
            
            
            for(String cmsLocation : cmsLocations) {
                try {
                    URI uri = new URI(cmsLocation);
                    String cmsComposerMountHostName = uri.getHost();
                    
                    // get the host segments in reversed order. For example 127.0.0.1 --> {"1", "0", "0", "127"}
                    String[] hostSegments = cmsComposerMountHostName.split("\\.");
                    ArrayUtils.reverse(hostSegments);
                    VirtualHost composerHost = null;
                    // try to find the cmsRestHostName host. If not present, we add a complete new one to a unique new host group
                    // It can map in multiple host groups to a 'partial' hostName, that does not contain a portMount with a rootMount.
                    // Hence, we *only* count the composerHost if it also contains a portMount on port 0 or on the 
                    // port of the cmsLocation AND a hst:root Mount at least in that p
                    for(Map<String, MutableVirtualHost> rootVirtualHostMap :  hosts.getRootVirtualHostsByGroup().values()) {
                        int i = 0;
                        composerHost = null;
                        while(i < hostSegments.length) {
                            if (i == 0) {
                                composerHost = rootVirtualHostMap.get(hostSegments[i]);
                            } else {
                                composerHost = composerHost.getChildHost(hostSegments[i]); 
                            }
                            if (composerHost == null) {
                                // cmsRestHostName does not yet exist in this hostGroup
                                break;
                            }
                            i++;
                        }
                        
                        if(composerHost != null) {
                            // We have found the correct composerHost. Now check whether also has portMount and that
                            // portMount must contain a hstRoot
                            PortMount portMount = null;
                            int port = uri.getPort();
                            if(port != 0) {
                                portMount = composerHost.getPortMount(port);
                            } 
                            if(portMount == null) {
                                // check default port 0
                                portMount = composerHost.getPortMount(0);
                            }
                            
                            if(portMount == null) {
                                continue;
                            }
                            if(portMount.getRootMount() == null) {
                                continue;
                            }
                            // we have a correct composerHost. Stop
                            break;
                        }
                    }
                    
                    if (composerHost == null) {
                        // add the cmsRestHostName + mount
                        composerHost = new ComposerVirtualHost(hosts,hostSegments,cmsLocation, 0);
                        hosts.addVirtualHost((MutableVirtualHost)composerHost);
                    } 
                    
                    // now check whether to add a portMount
                    // first check portMount of the cmsLocation. then a port agnostic PortMount with port 0
                    PortMount portMount = null;
                    int port = uri.getPort();
                    if(port != 0) {
                        portMount = composerHost.getPortMount(port);
                    } 
                    if(portMount == null) {
                        // check default port 0
                        portMount = composerHost.getPortMount(0);
                    }
                    
                    if(portMount == null) {
                        // add a port Mount with port equal to the configured port. If port is -1, we add the default 0 port
                        if(port == -1) {
                            portMount = new ComposerPortMount(composerHost, 0);
                        } else {
                            portMount = new ComposerPortMount(composerHost, port);
                        }
                        if(composerHost instanceof ComposerVirtualHost) {
                            ((ComposerVirtualHost)composerHost).setPortMount((MutablePortMount)portMount);
                        } else {
                            ((MutableVirtualHost)composerHost).addPortMount((MutablePortMount)portMount);
                        }
                    }
                    
                    // now check the hst:root presence on the portMount. If not add a hst:root + composerMount 
                    Mount rootMount = portMount.getRootMount();
                    Mount composerMount = null;
                    if (rootMount == null) {
                        MutableMount rootMountPlusComposerMount = new ComposerMount(composerHost, noopPipeline);
                        if (!(portMount instanceof MutablePortMount)) {
                            log.error("Unable to add composer mount for '{}' because found portMount not of type MutablePortMount. The template composer " +
                                    "will not work for hostGroup", cmsLocation);
                            continue;
                        }
                        ((MutablePortMount)portMount).setRootMount(rootMountPlusComposerMount);
                    } else {
                        composerMount = rootMount.getChildMount(composerMountName);
                        if (composerMount == null) {
                            if (!(rootMount instanceof MutableMount)) {
                                log.error("Unable to add composer mount for '{}' because found rootMount not of type MutableMount. The template composer " +
                                        "will not work for hostGroup ", cmsLocation);
                                continue;
                            }
                            composerMount = new ComposerMount(composerMountName, composerMountNamedPipeline, rootMount, composerHost);
                            ((MutableMount)rootMount).addMount((MutableMount)composerMount);
                        } else {
                            log.info("There is an explicit composer Mount '{}' for hostGroup . This mount can be removed from configuration" +
                                    " as it will be auto-created by the HST",composerMountName);
                            continue;  
                        }
                    }
                    if(composerMount != null) {
                        log.info("Succesfull automatically created composer mount for cmsLocation '{}'. Created Mount = {}", cmsLocation, composerMount);
                    }
                    
                } catch (URISyntaxException e) {
                    log.warn("'{}' is an invalid cmsLocation. The template composer " +
                            "won't be available for hosts in the hostGroup.", cmsLocation);
                    continue;
                } catch (ServiceException e) {
                    log.error("Unable to add composer mount. The template composer will not work for hostGroup", e);
                } catch (IllegalArgumentException e) {
                    log.warn("Unable to add composer mount. The template composer will not work for hostGroup");
                }
            }
            
    }
    
    private class ComposerVirtualHost implements MutableVirtualHost {
        private VirtualHosts virtualHosts;
        private Map<String,VirtualHost> childs = new HashMap<String, VirtualHost>();
        private String name;
        private String hostName;
        private MutablePortMount portMount;
        private String cmsLocation;
        
        private ComposerVirtualHost(VirtualHosts virtualHosts, String[] hostSegments, String cmsLocation, int position) throws ServiceException {
            this.virtualHosts = virtualHosts;
            name = hostSegments[position];
            this.cmsLocation = cmsLocation;
            int i = position;
            while(i > 0) {
                if(hostName != null) {
                    hostName = hostSegments[position] + "." + hostName;
                } else {
                    hostName = hostSegments[position];
                }
                i--;
            }
            position++;
            if(position == hostSegments.length) {
                // done with adding hosts 
            } else {
                 childs.put(hostSegments[position], new ComposerVirtualHost(virtualHosts, hostSegments, cmsLocation,  position));
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
            // as long as it is unique for ths cms host
            return ComposerMountAndVirtualHostAugmenter.class.getName() + "-" + UUID.randomUUID();
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
        public String getCmsLocation() {
            return cmsLocation;
        }

    }
    
    private class ComposerPortMount implements MutablePortMount {

        private int port;
        private Mount rootMount;
        
        private ComposerPortMount(VirtualHost virtualHost, int port) throws ServiceException {
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
        
    }

    private final static String fakeNonExistingPath = "/fakePath/"+UUID.randomUUID().toString();
    
    private class ComposerMount implements MutableMount {

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
         * Creates a hst:root Mount + the child composerMount
         * @param virtualHost
         * @param namedPipeline 
         * @throws ServiceException
         */
        private ComposerMount(VirtualHost virtualHost, String namedPipeline) throws ServiceException {
            this.virtualHost = virtualHost;
            name = HstNodeTypes.MOUNT_HST_ROOTNAME;
            mountPath = "";
            type = Mount.LIVE_NAME;
            types = Arrays.asList(type);
            this.namedPipeline = namedPipeline;
            // the hst:root mount has a namedPipeline equal to null and can never be used
            // TODO make _cmsrest and CmsRestPipeline configurable
            Mount composerRootMount = new ComposerMount(composerMountName , composerMountNamedPipeline , this, virtualHost);
            childs.put(composerRootMount.getName(), composerRootMount);
            ((MutableVirtualHosts)virtualHost.getVirtualHosts()).addMount(this);
        }
        
        /**
         * Creates only the composerMount
         * @param name
         * @param namedPipeline
         * @param parent
         * @param virtualHost
         * @throws ServiceException
         */
        public ComposerMount(String name, String namedPipeline, Mount parent, VirtualHost virtualHost) throws ServiceException {
           this.name = name;
           this.namedPipeline = namedPipeline;
           this.parent = parent;
           type = COMPOSER_MOUNT_TYPE;
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
        public String[] getDefaultSiteMapItemHandlerIds() {
            return null;
        }

        @Override
        public void setChannelInfo(ChannelInfo info) {
            // nothing
        }

        @Override
        public String toString() {
            return "ComposerMount [virtualHost=" + virtualHost.getHostName() + ", parent=" + parent.getName() + ", alias=" + alias
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
        
    }

    
}
