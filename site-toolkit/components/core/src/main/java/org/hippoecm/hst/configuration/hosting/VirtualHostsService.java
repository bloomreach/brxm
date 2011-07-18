/*
 *  Copyright 2008 Hippo.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.model.HstManagerImpl;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.service.ServiceException;
import org.hippoecm.hst.site.request.ResolvedVirtualHostImpl;
import org.hippoecm.hst.util.DuplicateKeyNotAllowedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualHostsService implements VirtualHosts {
    
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(VirtualHostsService.class);

    private final static String WILDCARD = "_default_";
    
    public final static String DEFAULT_SCHEME = "http";

    private HstManagerImpl hstManager;
    private Map<String, Map<String, VirtualHostService>> rootVirtualHostsByGroup = new DuplicateKeyNotAllowedHashMap<String, Map<String, VirtualHostService>>();

    private Map<String, List<Mount>> mountByHostGroup = new HashMap<String, List<Mount>>();
    private Map<String, Mount> mountsByIdentifier = new HashMap<String, Mount>();
    private Map<String, Map<String, Mount>> mountByGroupAliasAndType = new HashMap<String, Map<String, Mount>>();
    
  
    private String defaultHostName;
    /**
     * The homepage for this VirtualHosts. When the backing configuration does not contain a homepage, the value is <code>null
     */
    private String homepage;

    /**
     * The pageNotFound for this VirtualHosts. When the backing configuration does not contain a pageNotFound, the value is <code>null</code>
     */
    private String pageNotFound;
    

    /**
     * The general locale configured on VirtualHosts. When the backing configuration does not contain a locale, the value is <code>null</code>
     */
    private String locale;
    
    /**
     * Whether the {@link Mount}'s below this VirtualHostsService should show the hst version as a response header when they are a preview {@link Mount}
     */
    private boolean versionInPreviewHeader = true;
    
    private boolean virtualHostsConfigured;
    private String scheme;
    private boolean contextPathInUrl = true;
    private boolean showPort = true;
    private String[] prefixExclusions;
    private String[] suffixExclusions;
    
    /*
     * Note, this cache does not need to be synchronized at all, because worst case scenario one entry would be computed twice and overriden.
     */
    private Map<String, ResolvedVirtualHost> resolvedMapCache = new HashMap<String, ResolvedVirtualHost>();
    
    public VirtualHostsService(HstNode virtualHostsConfigurationNode, HstManagerImpl hstManager) throws ServiceException {
        this.hstManager = hstManager;
        this.virtualHostsConfigured = true;
        this.contextPathInUrl = virtualHostsConfigurationNode.getValueProvider().getBoolean(HstNodeTypes.VIRTUALHOSTS_PROPERTY_SHOWCONTEXTPATH);
        this.showPort = virtualHostsConfigurationNode.getValueProvider().getBoolean(HstNodeTypes.VIRTUALHOSTS_PROPERTY_SHOWPORT);
        this.prefixExclusions = virtualHostsConfigurationNode.getValueProvider().getStrings(HstNodeTypes.VIRTUALHOSTS_PROPERTY_PREFIXEXCLUSIONS);
        this.suffixExclusions = virtualHostsConfigurationNode.getValueProvider().getStrings(HstNodeTypes.VIRTUALHOSTS_PROPERTY_SUFFIXEXCLUSIONS);
        this.scheme = virtualHostsConfigurationNode.getValueProvider().getString(HstNodeTypes.VIRTUALHOSTS_PROPERTY_SCHEME);
        this.locale = virtualHostsConfigurationNode.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_LOCALE);
        this.homepage = virtualHostsConfigurationNode.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_HOMEPAGE);
        this.pageNotFound = virtualHostsConfigurationNode.getValueProvider().getString(HstNodeTypes.GENERAL_PROPERTY_PAGE_NOT_FOUND);
        if(virtualHostsConfigurationNode.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER)) {
            this.versionInPreviewHeader = virtualHostsConfigurationNode.getValueProvider().getBoolean(HstNodeTypes.GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER);
        }
        this.defaultHostName  = virtualHostsConfigurationNode.getValueProvider().getString(HstNodeTypes.VIRTUALHOSTS_PROPERTY_DEFAULTHOSTNAME);
        if(scheme == null || "".equals(scheme)) {
            this.scheme = DEFAULT_SCHEME;
        }
        
        // now we loop through the hst:hostgroup nodes first:
        for(HstNode hostGroupNode : virtualHostsConfigurationNode.getNodes()) {
            // assert node is of type virtualhostgroup
            if(!HstNodeTypes.NODETYPE_HST_VIRTUALHOSTGROUP.equals(hostGroupNode.getNodeTypeName())) {
                throw new ServiceException("Expected a hostgroup node of type '"+HstNodeTypes.NODETYPE_HST_VIRTUALHOSTGROUP+"' but found a node of type '"+hostGroupNode.getNodeTypeName()+"' at '"+hostGroupNode.getValueProvider().getPath()+"'");
            }
            Map<String, VirtualHostService> rootVirtualHosts =  virtualHostHashMap();
            try {
                rootVirtualHostsByGroup.put(hostGroupNode.getValueProvider().getName(), rootVirtualHosts);
            } catch (IllegalArgumentException e) {
                throw new ServiceException("It should not be possible to have two hostgroups with the same name. We found duplicate group with name '"+hostGroupNode.getValueProvider().getName()+"'");
            }
            for(HstNode virtualHostNode : hostGroupNode.getNodes()) {
                try {
                    VirtualHostService virtualHost = new VirtualHostService(this, virtualHostNode, (VirtualHostService)null, hostGroupNode.getValueProvider().getName() ,hstManager);
                    rootVirtualHosts.put(virtualHost.getName(), virtualHost);
                } catch (ServiceException e) {
                    log.error("Unable to add virtualhost with name '"+virtualHostNode.getValueProvider().getName()+"'. Fix the configuration. This virtualhost will be skipped.", e);
                    // continue to next virtualHost
                } catch (IllegalArgumentException e) {    
                    log.error("VirtualHostMap is not allowed to have duplicate hostnames. This problem might also result from having two hosts configured"
                            + "something like 'preview.mycompany.org' and 'www.mycompany.org'. This results in 'mycompany.org' being a duplicate in a hierarchical presentation which the model makes from hosts splitted by dots. "
                            + "In this case, make sure to configure them hierarchically as org -> mycompany -> (preview , www)");
                   throw e;
               }
            }
        }
        
    }
    

    public HstManager getHstManager() {
        return hstManager;
    }

    
    
    public boolean isExcluded(String pathInfo) {
        // test prefix
        if(prefixExclusions != null) {
            for(String excludePrefix : prefixExclusions) {
                if(pathInfo.startsWith(excludePrefix)) {
                    return true;
                }
            }
        }
        // test suffix
        if(suffixExclusions != null) {
            for(String excludeSuffix : suffixExclusions) {
                if(pathInfo.endsWith(excludeSuffix)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Add this mount for lookup through {@link #getMountByGroupAliasAndType(String, String, String)}
     * @param mount
     */
    public void addMount(Mount mount) throws ServiceException {

        String hostGroup = mount.getVirtualHost().getHostGroupName();

        List<Mount> mountsForGroup = mountByHostGroup.get(hostGroup);
        if (mountsForGroup == null) {
            mountsForGroup = new ArrayList<Mount>();
            mountByHostGroup.put(hostGroup, mountsForGroup);
        }
        mountsForGroup.add(mount);

        Map<String, Mount> aliasTypeMap = mountByGroupAliasAndType.get(hostGroup);
        if (aliasTypeMap == null) {
            // when a duplicate key is tried to be put, an IllegalArgumentException must be thrown, hence the DuplicateKeyNotAllowedHashMap
            aliasTypeMap = new DuplicateKeyNotAllowedHashMap<String, Mount>();
            mountByGroupAliasAndType.put(hostGroup, aliasTypeMap);
        }
        // add the mount for all alias-type combinations:
        for (String type : mount.getTypes()) {
            if(mount.getAlias() == null) {
                continue;
            }
            try {
                aliasTypeMap.put(getAliasTypeKey(mount.getAlias(), type), mount);
            } catch (IllegalArgumentException e) {
                log.error("Incorrect hst:hosts configuration. Not allowed to have multiple mount's having the same 'alias/type/types' combination within a single hst:hostgroup. " +
                		". Failed for mount '{}' in hostgroup '"+mount.getVirtualHost().getHostGroupName()+"' for host '"+mount.getVirtualHost().getHostName()+"'. Make sure that you add a unique 'alias' in combination with the 'types' on the mount within a single hostgroup. The mount '{}' cannot be used for lookup. Change alias for it.", mount.getName(), mount.getName());
            }
        }
        
        mountsByIdentifier.put(mount.getIdentifier(), mount);

    }
    
    public ResolvedSiteMapItem matchSiteMapItem(HstContainerURL hstContainerURL)  throws MatchException {
            
        ResolvedVirtualHost resolvedVirtualHost = matchVirtualHost(hstContainerURL.getHostName());
        if(resolvedVirtualHost == null) {
            throw new MatchException("Unknown host '"+hstContainerURL.getHostName()+"'");
        }
        ResolvedMount resolvedMount  = resolvedVirtualHost.matchMount(hstContainerURL.getContextPath(), hstContainerURL.getRequestPath());
        if(resolvedMount == null) {
            throw new MatchException("resolvedVirtualHost '"+hstContainerURL.getHostName()+"' does not have a mount");
        }
        return resolvedMount.matchSiteMapItem(hstContainerURL.getPathInfo());
    }

    
    public ResolvedMount matchMount(String hostName, String contextPath, String requestPath) throws MatchException {
        ResolvedVirtualHost resolvedVirtualHost = matchVirtualHost(hostName);
        ResolvedMount resolvedMount = null;
        if(resolvedVirtualHost != null) {
            resolvedMount  = resolvedVirtualHost.matchMount(contextPath, requestPath);
        }
        return resolvedMount;
    }
    
    public ResolvedVirtualHost matchVirtualHost(String hostName) throws MatchException {
        if(!virtualHostsConfigured) {
            throw new MatchException("No correct virtual hosts configured. Cannot continue request");
        }
         
        // NOTE : the resolvedMapCache does not need synchronization. Theoretically it would need it as it is used concurrent. 
        // In practice it won't happen ever. Trust me
        ResolvedVirtualHost rvHost = resolvedMapCache.get(hostName);
        if(rvHost != null) {
            return rvHost;
        }
        
    	int portNumber = 0;
        String portStrippedHostName = hostName;
        int offset = portStrippedHostName.indexOf(':');
        if (offset != -1) {
        	try {
        		portNumber = Integer.parseInt(portStrippedHostName.substring(offset+1));
        	}
        	catch (NumberFormatException nfe) {
        		throw new MatchException("The hostName '"+portStrippedHostName+"' contains an invalid portnumber");
        	}
        	// strip off portNumber
           portStrippedHostName = portStrippedHostName.substring(0, offset);
        }
        ResolvedVirtualHost host = findMatchingVirtualHost(portStrippedHostName, portNumber);
        
        // no host found. Let's try the default host, if there is one configured:
        if(host == null && this.getDefaultHostName() != null && !this.getDefaultHostName().equals(portStrippedHostName)) {
            log.debug("Cannot find a mapping for servername '{}'. We try the default servername '{}'", portStrippedHostName, this.getDefaultHostName());
            if (portNumber != 0) {
                host = matchVirtualHost(this.getDefaultHostName()+":"+Integer.toString(portNumber));
            }
            else {
                host = matchVirtualHost(this.getDefaultHostName());
            }
        }
        if(host == null) {
           log.warn("We cannot find a servername mapping for '{}'. Even the default servername '{}' cannot be found. Return null", portStrippedHostName , this.getDefaultHostName());
          
        }
        // store in the resolvedMap
        resolvedMapCache.put(hostName, host);
        
        return host;
    }
    
    
    /**
     * Override this method if you want a different algorithm to resolve hostName
     * @param hostName
     * @param portNumber
     * @return the matched virtual host or <code>null</code> when no host can be matched
     */
    protected ResolvedVirtualHost findMatchingVirtualHost(String hostName, int portNumber) {
        String[] requestServerNameSegments = hostName.split("\\.");
        int depth = requestServerNameSegments.length - 1;
        VirtualHost host = null; 
        PortMount portMount = null; 
        for(Map<String, VirtualHostService> rootVirtualHosts : rootVirtualHostsByGroup.values()) {
            VirtualHost tryHost = rootVirtualHosts.get(requestServerNameSegments[depth]);
            if(tryHost == null) {
              continue;
            }
            tryHost = traverseInToHost(tryHost, requestServerNameSegments, depth);
            if(tryHost != null) {
                // check whether this is a valid host: In other words, whether it has
                // a Mount associated with its portMount 
                PortMount tryPortMount = tryHost.getPortMount(portNumber);
                if(tryPortMount != null && tryPortMount.getRootMount() != null) {
                    // we found a match for the host && port. We are done
                    host = tryHost;
                    portMount = tryPortMount;
                    break;
                }
                if(tryPortMount == null && portNumber != 0) {
                    log.debug("Could not match the request to port '{}'. If there is a default port '0', we'll try this one", String.valueOf(portNumber));
                    tryPortMount = tryHost.getPortMount(0);
                    if(tryPortMount != null && tryPortMount.getRootMount() != null) {
                        // we found a Mount for the default port '0'. This is the host and mount we need to use when we 
                        // do not find a portMount for another host which also matches the correct portNumber!
                        // we'll continue the loop.
                        if(host != null) {
                            log.debug("We already did find a possible matching host ('{}') with not an explicit portnumber match but we'll use host ('{}') as this one is equally suited.", host.getHostName() + " (hostgroup="+host.getHostGroupName()+")", tryHost.getHostName() + " (hostgroup="+tryHost.getHostGroupName()+")");
                        }
                        host = tryHost;
                        portMount = tryPortMount;
                    }
                }
            }
        }
        if(host == null) {
            return null;
        }
        
        return new ResolvedVirtualHostImpl(host, hostName, portMount);
      
    }
    
    
    /**
     * Override this method if you want a different algorithm to resolve requestServerName
     * @param matchedHost
     * @param hostNameSegments
     * @param depth
     * @return
     */
    protected VirtualHost traverseInToHost(VirtualHost matchedHost, String[] hostNameSegments, int depth) {
        if(depth == 0) {
           return matchedHost;
        }
        
        --depth;
        
        VirtualHost vhost = matchedHost.getChildHost(hostNameSegments[depth]);
        if(vhost == null) {
            if( (vhost = matchedHost.getChildHost(WILDCARD)) != null) {
                return vhost;
            }
        } else {
            return traverseInToHost(vhost, hostNameSegments, depth);
        }
        return null;
    }

    public boolean isContextPathInUrl() {
        return contextPathInUrl;
    }

    public boolean isPortInUrl() {
        return showPort;
    }

    public String getScheme(){
        return this.scheme;
    }

    public String getLocale() {
        return locale;
    }

    public String getDefaultHostName() {
        return this.defaultHostName;
    }

    public String getHomePage() {
        return homepage;
    }
    public String getPageNotFound() {
        return pageNotFound;
    }

    public boolean isVersionInPreviewHeader(){
        return versionInPreviewHeader;
    }
    
    public Mount getMountByGroupAliasAndType(String hostGroupName, String alias, String type) {
        Map<String, Mount> aliasTypeMap = mountByGroupAliasAndType.get(hostGroupName);
        if(aliasTypeMap == null) {
            return null;
        }
        if(alias == null || type == null) {
            throw new IllegalArgumentException("Alias and type are not allowed to be null");
        }
        return aliasTypeMap.get(getAliasTypeKey(alias, type));
    }


    public List<Mount> getMountsByHostGroup(String hostGroupName) {
        List<Mount> l = mountByHostGroup.get(hostGroupName);
        if(l == null) {
            l = Collections.emptyList();
        }
        return Collections.unmodifiableList(l);
    }
    
    /**
     * @return a HashMap<String, VirtualHostService> that throws an exception when you put in the same key twice
     */
    public final static HashMap<String, VirtualHostService> virtualHostHashMap(){
        return new DuplicateKeyNotAllowedHashMap<String, VirtualHostService>();
    }
    

    private String getAliasTypeKey(String alias, String type) {
        return alias.toLowerCase() + '\uFFFF' + type;
    }


    @Override
    public Mount getMountByIdentifier(String uuid) {
        return mountsByIdentifier.get(uuid);
    }

    
}
