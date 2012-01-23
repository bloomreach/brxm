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
package org.hippoecm.hst.cmsrest.configuration.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
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

public class CmsRestMountAndVirtualHostAugmenter implements HstConfigurationAugmenter {

    private static final Logger log = LoggerFactory.getLogger(CmsRestMountAndVirtualHostAugmenter.class);

    private final static String DEFAULT_CMS_REST_MOUNT_NAME = "_cmsrest";
    private final static String DEFAULT_CMS_REST_HOST_NAME = "127.0.0.1";
    private final static String DEFAULT_CMS_REST_MOUNT_NAMED_PIPELINE = "CmsRestPipeline";
    private final static String DEFAULT_NOOP_NAMED_PIPELINE =  "NoopPipeline";
    
    private String cmsRestMountName = DEFAULT_CMS_REST_MOUNT_NAME;
    private String cmsRestMountNamedPipeline = DEFAULT_CMS_REST_MOUNT_NAMED_PIPELINE;
    private String cmsRestHostName = DEFAULT_CMS_REST_HOST_NAME;
    private String noopPipeline = DEFAULT_NOOP_NAMED_PIPELINE;
    

    public void setCmsRestMountName(String cmsRestMountName) {
        this.cmsRestMountName = cmsRestMountName;
    }

    public void setCmsRestMountNamedPipeline(String cmsRestMountNamedPipeline) {
        this.cmsRestMountNamedPipeline = cmsRestMountNamedPipeline;
    }

    public void setCmsRestHostName(String cmsRestHostName) {
        this.cmsRestHostName = cmsRestHostName;
    }
    
    public void setNoopPipeline(String noopPipeline) {
        this.noopPipeline = noopPipeline;
    }

    @Override
    public void augment(HstManager manager) throws RepositoryNotAvailableException {
        try {
           if(!(manager.getVirtualHosts() instanceof MutableVirtualHosts )) {
               log.error("{} can only work when the hosts is an instanceof MutableVirtualHosts. The VIEW / GOTO button in cms will not work", this.getClass().getName());
               return;
           } 
           if(StringUtils.isEmpty(cmsRestHostName)) {
               log.error("{} can only work when the cmsRestHostName is not null or empty. The VIEW / GOTO button in cms will not work", this.getClass().getName());
               return;
           }
           MutableVirtualHosts hosts = (MutableVirtualHosts) manager.getVirtualHosts();
            
           // get the host segments in reversed order. For example 127.0.0.1 --> {"1", "0", "0", "127"}
           String[] hostSegments = cmsRestHostName.split("\\.");
           ArrayUtils.reverse(hostSegments);
           VirtualHost cmsHost = null;
           // try to find the cmsRestHostName host. If not present, it needs to be added entirely
           for(Map<String, MutableVirtualHost> rootVirtualHostMap :  hosts.getRootVirtualHostsByGroup().values()) {
               int i = 0;
               cmsHost = null;
               while(i < hostSegments.length) {
                   if (i == 0) {
                       cmsHost = rootVirtualHostMap.get(hostSegments[i]);
                   } else {
                       cmsHost = cmsHost.getChildHost(hostSegments[i]); 
                   }
                   if (cmsHost == null) {
                       // cmsRestHostName does not yet exist in this hostGroup
                       break;
                   }
                   i++;
               }
               
               if(cmsHost != null) {
                   // We have found the correct cmsHost Stop
                   break;
               }
           }
          
           if (cmsHost == null) {
               // add the cmsRestHostName + mount
               MutableVirtualHost cmsVirtualHost = new CmsRestVirtualHost(manager.getVirtualHosts(), hostSegments, 0);
               hosts.addVirtualHost(cmsVirtualHost);
           } else if (cmsHost instanceof MutableVirtualHost){
               // only add the needed portMount / hst:root mount / _cmsrest mount
               // check portMount for port 0
               PortMount portMount = cmsHost.getPortMount(0);
               if (portMount == null) {
                   MutablePortMount cmsRestPortMount = new CmsRestPortMount(cmsHost);
                   ((MutableVirtualHost)cmsHost).addPortMount(cmsRestPortMount);
               } else if (portMount instanceof MutablePortMount) {
                   Mount rootMount = portMount.getRootMount();
                   if (rootMount == null) {
                      MutableMount cmsRestRootMount = new CmsRestMount(cmsHost, noopPipeline);
                      ((MutablePortMount)portMount).setRootMount(cmsRestRootMount);
                   } else {
                      Mount cmsRestMount = rootMount.getChildMount(cmsRestMountName);
                      if (cmsRestMount != null) {
                          log.info("There is an implicit '{}' mount configured, hence no programmatic added CMS REST MOUNT", cmsRestMountName);
                      } else if (rootMount instanceof MutableMount) {
                          // add a cmsRestMount to the root mount
                          MutableMount newCmsRestMount = new CmsRestMount(cmsRestMountName, cmsRestMountNamedPipeline, rootMount, cmsHost);
                          ((MutableMount)rootMount).addMount(newCmsRestMount);
                      } else {
                          log.error("Unable to add the cms rest mount. The cms document VIEW / GOTO button might not work");
                      }
                   }
               } else {
                   log.error("Unable to add the cms rest mount. The cms document VIEW / GOTO button might not work");
               }
           } else {
               log.error("Unable to add the cms rest mount. The cms document VIEW / GOTO button might not work");
           }
        } catch (ServiceException e) {
            log.error("Unable to add the cms rest mount. The cms document VIEW / GOTO button might not work", e);
        } catch (IllegalArgumentException e) {
            log.warn("Could not add a CMS REST MOUNT. It might already be explicitly configured. If not, the cms document VIEW / GOTO button might not work ");
        }
       
    }

    private class CmsRestVirtualHost implements MutableVirtualHost {
        private VirtualHosts virtualHosts;
        private Map<String,VirtualHost> childs = new HashMap<String, VirtualHost>();
        private String name;
        private String hostName;
        private MutablePortMount portMount;
        
        private CmsRestVirtualHost(VirtualHosts virtualHosts, String[] hostSegments, int position) throws ServiceException {
            this.virtualHosts = virtualHosts;
            name = hostSegments[position];
            int i = position;
            while(i > -1) {
                if(hostName != null) {
                    hostName = hostSegments[position] + "." + hostName;
                } else {
                    hostName = hostSegments[position];
                }
                i--;
            }
            position++;
            if(position == hostSegments.length) {
                // done with hosts. We now need to add the PortMount, hst root mount and the _cmsrest mount
                portMount = new CmsRestPortMount(this);
                setPortMount(portMount);
            } else {
                 childs.put(hostSegments[position], new CmsRestVirtualHost(virtualHosts, hostSegments, position));
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
            return CmsRestMountAndVirtualHostAugmenter.class.getName();
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
            return null;
        }

        @Override
        public String toString() {
            return "CmsRestVirtualHost [name=" + name + ", hostName=" + hostName + ", hostGroupName=" + getHostGroupName() + "]";
        }
        
    }
    
    private class CmsRestPortMount implements MutablePortMount {
        
        private static final int PORT = 0;
        Mount rootMount;

        private CmsRestPortMount(VirtualHost virtualHost) throws ServiceException {
            rootMount = new CmsRestMount(virtualHost, noopPipeline);
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
            return "CmsRestPortMount [port=" + PORT + "]";
        }
        
    }

    private final static String fakeNonExistingPath = "/fakePath/" + UUID.randomUUID().toString();
    
    private class CmsRestMount implements MutableMount {

        private VirtualHost virtualHost;
        private Mount parent;
        // just a unique alias
        private String alias = "randomAlias" + UUID.randomUUID().toString();
        private String identifier = "randomIdentifier" + UUID.randomUUID().toString();
        private String name;
        private String namedPipeline;
        private Map<String, Mount> childs = new HashMap<String, Mount>();
        private String mountPath;
        private List<String> types = Arrays.asList(Mount.LIVE_NAME);
        
        // the hst:root mount constructor
        private CmsRestMount(VirtualHost virtualHost, String namedPipeline) throws ServiceException {
            this.virtualHost = virtualHost;
            name = HstNodeTypes.MOUNT_HST_ROOTNAME;
            mountPath = "";
            this.namedPipeline = namedPipeline;
            // the hst:root mount has a namedPipeline equal to null and can never be used
            // TODO make _cmsrest and CmsRestPipeline configurable
            Mount cmsRestMount = new CmsRestMount(cmsRestMountName, cmsRestMountNamedPipeline, this, virtualHost);
            childs.put(cmsRestMount.getName(), cmsRestMount);
            ((MutableVirtualHosts)virtualHost.getVirtualHosts()).addMount(this);
        }
        
        // the _cmsrest mount constructor
        public CmsRestMount(String name, String namedPipeline, Mount parent, VirtualHost virtualHost) throws ServiceException {
           this.name = name;
           this.namedPipeline = namedPipeline;
           this.parent = parent;
           this.virtualHost = virtualHost;
           ((MutableVirtualHosts)virtualHost.getVirtualHosts()).addMount(this);
            mountPath = parent.getMountPath() + "/" + name;
        }

        @Override
        public void addMount(MutableMount mount) throws IllegalArgumentException, ServiceException {
           if(childs.containsKey(mount.getName())) {
                throw new IllegalArgumentException("Cannot add Mount with name '"+mount.getName()+"' because already exists for " + this.toString());
            }
           childs.put(mount.getName(), mount);
           ((MutableVirtualHosts)virtualHost.getVirtualHosts()).addMount(mount);
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
           return Mount.LIVE_NAME;
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
            return "ComposerMount [virtualHost=" + virtualHost.getHostName() + ", alias=" + alias
                    + ", identifier=" + identifier + ", name=" + name + ", namedPipeline=" + namedPipeline
                    + ", childs=" + childs + ", mountPath=" + mountPath + ", types=" + types + ", getAlias()="
                    + getAlias() + ", isMapped()=" + isMapped() + ", isPortInUrl()=" + isPortInUrl() + ", isSite()="
                    + isSite() + ", getPort()=" + getPort() + ", onlyForContextPath()=" + onlyForContextPath()
                    + ", getType()=" + getType() + ", getIdentifier()="+ getIdentifier() + "]";
        }

        @Override
        public String getCmsLocation() {
            // nothing to return for the cms rest mount
            return null;
        }

    }

}

