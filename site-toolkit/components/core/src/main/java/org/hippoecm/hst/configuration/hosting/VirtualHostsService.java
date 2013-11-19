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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.ConfigurationUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.configuration.channel.Blueprint;
import org.hippoecm.hst.configuration.channel.BlueprintHandler;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.channel.ChannelInfoClassProcessor;
import org.hippoecm.hst.configuration.channel.ChannelLazyLoadingChangedBySet;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.configuration.channel.ChannelPropertyMapper;
import org.hippoecm.hst.configuration.channel.ChannelUtils;
import org.hippoecm.hst.configuration.channel.HstPropertyDefinition;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.model.HstManagerImpl;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.model.ModelLoadingException;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.hippoecm.hst.provider.ValueProvider;
import org.hippoecm.hst.service.ServiceException;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.request.ResolvedVirtualHostImpl;
import org.hippoecm.hst.util.DuplicateKeyNotAllowedHashMap;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualHostsService implements MutableVirtualHosts {

    private static final Logger log = LoggerFactory.getLogger(VirtualHostsService.class);

    private final static String WILDCARD = "_default_";

    private HstManagerImpl hstManager;
    private HstNodeLoadingCache hstNodeLoadingCache;
    private Map<String, Map<String, MutableVirtualHost>> rootVirtualHostsByGroup = new DuplicateKeyNotAllowedHashMap<>();

    private Map<String, List<Mount>> mountByHostGroup = new HashMap<>();
    private Map<String, Mount> mountsByIdentifier = new HashMap<>();
    private Map<String, Map<String, Mount>> mountByGroupAliasAndType = new HashMap<>();

    private List<Mount> registeredMounts = new ArrayList<>();

    private String defaultHostName;
    /**
     * The homepage for this VirtualHosts. When the backing configuration does not contain a homepage, the value is
     * <code>null</code>
     */
    private String homepage;

    /**
     * The pageNotFound for this VirtualHosts. When the backing configuration does not contain a pageNotFound, the
     * value is <code>null</code>
     */
    private String pageNotFound;


    /**
     * The general locale configured on VirtualHosts. When the backing configuration does not contain a locale, the
     * value is <code>null</code>
     */
    private String locale;

    /**
     * Whether the {@link Mount}'s below this VirtualHostsService should show the hst version as a response header
     * when they are a preview {@link Mount}
     */
    private boolean versionInPreviewHeader = true;

    private boolean virtualHostsConfigured;
    private String scheme;
    /**
     * if the request is for example http but https is required, the default response code is SC_MOVED_PERMANENTLY. If nothing
     * needs to be done, set a 200. Not found 404 and not authorized Forbidden
     */
    private int schemeNotMatchingResponseCode = HttpServletResponse.SC_MOVED_PERMANENTLY;
    private boolean contextPathInUrl = true;
    /**
     * if configured, this field will contain the default contextpath through which is hst webapp can be accessed
     */
    private String defaultContextPath = null;
    private boolean showPort = true;
    private String[] prefixExclusions;
    private String[] suffixExclusions;

    /**
     * the cms preview prefix : The prefix all URLs when accessed through the CMS
     */
    private String cmsPreviewPrefix;

    private boolean diagnosticsEnabled;

    private boolean cacheable = false;

    private String [] defaultResourceBundleIds;

    private boolean channelMngrSiteAuthenticationSkipped;

    private Set<String> diagnosticsForIps = new HashSet<String>(0);

    /**
     * The 'active' virtual host group for the current environment. This should not be <code>null</code> and
     * not contains slashes but just the name of the hst:virtualhostgroup node below the hst:hosts node
     */
    private String channelMngrVirtualHostGroupNodeName;

    private final static String DEFAULT_CHANNEL_MNGR_SITES_NODE_NAME = "hst:sites";

    /**
     * The name of the hst:sites that is managed by the {@link ChannelManager}
     */
    private String channelMngrSitesNodeName = DEFAULT_CHANNEL_MNGR_SITES_NODE_NAME;

    /*
     * Note, this cache does not need to be synchronized at all, because worst case scenario one entry would be
     * computed twice and overriden.
     */
    private Map<String, ResolvedVirtualHost> resolvedMapCache = new HashMap<>();

    private String channelsRoot;

    private final Map<String, Channel> channels = new HashMap<>();
    private final Map<String, Blueprint> blueprints = new HashMap<>();
    private boolean bluePrintsPrototypeChecked;

    public VirtualHostsService(final HstManagerImpl hstManager, final HstNodeLoadingCache hstNodeLoadingCache) throws ServiceException {
        long start = System.currentTimeMillis();
        this.hstNodeLoadingCache = hstNodeLoadingCache;
        this.hstManager = hstManager;
        channelsRoot = hstNodeLoadingCache.getRootPath() + "/" + HstNodeTypes.NODENAME_HST_CHANNELS + "/";
        virtualHostsConfigured = true;
        HstNode vhostsNode = hstNodeLoadingCache.getNode(hstNodeLoadingCache.getRootPath()+"/hst:hosts");
        if (vhostsNode == null) {
            throw new ModelLoadingException("No hst node found for '"+hstNodeLoadingCache.getRootPath()+"/hst:hosts'. Cannot load model.'");
        }
        ValueProvider vHostConfValueProvider = vhostsNode.getValueProvider();
        contextPathInUrl = vHostConfValueProvider.getBoolean(HstNodeTypes.VIRTUALHOSTS_PROPERTY_SHOWCONTEXTPATH);
        defaultContextPath = vHostConfValueProvider.getString(HstNodeTypes.VIRTUALHOSTS_PROPERTY_DEFAULTCONTEXTPATH);
        cmsPreviewPrefix = vHostConfValueProvider.getString(HstNodeTypes.VIRTUALHOSTS_PROPERTY_CMSPREVIEWPREFIX);
        diagnosticsEnabled = vHostConfValueProvider.getBoolean(HstNodeTypes.VIRTUALHOSTS_PROPERTY_DIAGNOSTISC_ENABLED);

        String[] ips = vHostConfValueProvider.getStrings(HstNodeTypes.VIRTUALHOSTS_PROPERTY_DIAGNOSTICS_FOR_IPS);
        Collections.addAll(diagnosticsForIps, ips);
        if(cmsPreviewPrefix == null) {
            // there is no explicit cms preview prefix configured. Take the default one from the hstManager
            cmsPreviewPrefix = hstManager.getCmsPreviewPrefix();
            if(cmsPreviewPrefix == null) {
                cmsPreviewPrefix = StringUtils.EMPTY;
            }
        }
        if(StringUtils.isEmpty(cmsPreviewPrefix)) {
            log.info("cmsPreviewPrefix property '{}' on hst:hosts is configured to be empty. This means that when " +
                    "the cms and site run on the same host that a client who visited the preview in cms also" +
            		"sees the preview without the cms where he expected to see the live. ",
                    HstNodeTypes.VIRTUALHOSTS_PROPERTY_CMSPREVIEWPREFIX);
        } else {
            cmsPreviewPrefix =  PathUtils.normalizePath(cmsPreviewPrefix);
        }

        channelMngrVirtualHostGroupNodeName =
                vHostConfValueProvider.getString(HstNodeTypes.VIRTUALHOSTS_PROPERTY_CHANNEL_MNGR_HOSTGROUP);
        if(StringUtils.isEmpty(channelMngrVirtualHostGroupNodeName)) {
            log.warn("On the hst:hosts node there is no '{}' property configured. This means the channel manager " +
                    "won't be able to load channels in the preview / composer",
                    HstNodeTypes.VIRTUALHOSTS_PROPERTY_CHANNEL_MNGR_HOSTGROUP);
        } else {
            log.info("Channel manager will load channels for hostgroup = '{}'", channelMngrVirtualHostGroupNodeName);
        }
        String sites = vHostConfValueProvider.getString(HstNodeTypes.VIRTUALHOSTS_PROPERTY_CHANNEL_MNGR_SITES);
        if(!StringUtils.isEmpty(sites)) {
            log.info("Channel manager will load work with hst:sites node '{}' instead of the default '{}'.",
                    sites, DEFAULT_CHANNEL_MNGR_SITES_NODE_NAME);
            channelMngrSitesNodeName = sites;
        }
        showPort = vHostConfValueProvider.getBoolean(HstNodeTypes.VIRTUALHOSTS_PROPERTY_SHOWPORT);
        prefixExclusions = vHostConfValueProvider.getStrings(HstNodeTypes.VIRTUALHOSTS_PROPERTY_PREFIXEXCLUSIONS);
        suffixExclusions = vHostConfValueProvider.getStrings(HstNodeTypes.VIRTUALHOSTS_PROPERTY_SUFFIXEXCLUSIONS);
        scheme = vHostConfValueProvider.getString(HstNodeTypes.VIRTUALHOSTS_PROPERTY_SCHEME);
        locale = vHostConfValueProvider.getString(HstNodeTypes.GENERAL_PROPERTY_LOCALE);
        homepage = vHostConfValueProvider.getString(HstNodeTypes.GENERAL_PROPERTY_HOMEPAGE);
        pageNotFound = vHostConfValueProvider.getString(HstNodeTypes.GENERAL_PROPERTY_PAGE_NOT_FOUND);
        channelMngrSiteAuthenticationSkipped = vHostConfValueProvider.getBoolean(HstNodeTypes.VIRTUALHOSTS_PROPERTY_CHANNEL_MNGR_SITE_AUTHENTICATION_SKIPPED);

        if(vHostConfValueProvider.hasProperty(HstNodeTypes.GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER)) {
            versionInPreviewHeader = vHostConfValueProvider.getBoolean(HstNodeTypes.GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER);
        }

        if(vHostConfValueProvider.hasProperty(HstNodeTypes.GENERAL_PROPERTY_CACHEABLE)) {
            cacheable = vHostConfValueProvider.getBoolean(HstNodeTypes.GENERAL_PROPERTY_CACHEABLE);
            log.info("Page caching for HST is set to : {} ", cacheable);
        }

        defaultHostName  = vHostConfValueProvider.getString(HstNodeTypes.VIRTUALHOSTS_PROPERTY_DEFAULTHOSTNAME);
        if (defaultHostName != null) {
            defaultHostName = defaultHostName.toLowerCase();
        }
        if(scheme == null || "".equals(scheme)) {
            scheme = DEFAULT_SCHEME;
        }
        if (vHostConfValueProvider.hasProperty(HstNodeTypes.GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE)) {
            int statusCode = (int)vHostConfValueProvider.getLong(HstNodeTypes.GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE).longValue();
            if (ConfigurationUtils.isSupportedSchemeNotMatchingResponseCode(statusCode)) {
                schemeNotMatchingResponseCode = statusCode;
            } else {
                log.warn("Invalid '{}' configured on '{}'. Use inherited value. Supported values are '{}'", new String[]{HstNodeTypes.GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE,
                        vHostConfValueProvider.getPath(), ConfigurationUtils.suppertedSchemeNotMatchingResponseCodesAsString()});
            }
        }

        defaultResourceBundleIds = StringUtils.split(vHostConfValueProvider.getString(HstNodeTypes.GENERAL_PROPERTY_DEFAULT_RESOURCE_BUNDLE_ID), " ,\t\f\r\n");

        // now we loop through the hst:hostgroup nodes first:
        for(HstNode hostGroupNode : vhostsNode.getNodes()) {
            // assert node is of type virtualhostgroup
            if(!HstNodeTypes.NODETYPE_HST_VIRTUALHOSTGROUP.equals(hostGroupNode.getNodeTypeName())) {
                throw new ServiceException("Expected a hostgroup node of type '"+HstNodeTypes.NODETYPE_HST_VIRTUALHOSTGROUP+"' but found a node of type '"+hostGroupNode.getNodeTypeName()+"' at '"+hostGroupNode.getValueProvider().getPath()+"'");
            }
            Map<String, MutableVirtualHost> rootVirtualHosts =  virtualHostHashMap();
            try {
                rootVirtualHostsByGroup.put(hostGroupNode.getValueProvider().getName(), rootVirtualHosts);
            } catch (IllegalArgumentException e) {
                throw new ServiceException("It should not be possible to have two hostgroups with the same name. We found duplicate group with name '"+hostGroupNode.getValueProvider().getName()+"'");
            }

            String cmsLocation = hostGroupNode.getValueProvider().getString(HstNodeTypes.VIRTUALHOSTGROUP_PROPERTY_CMS_LOCATION);
            if(cmsLocation == null) {
                log.warn("VirtualHostGroup '{}' does not have a property hst:cmslocation configured.", hostGroupNode.getValueProvider().getName());
            } else {
                cmsLocation = cmsLocation.toLowerCase();
                try {
                    URI testLocation = new URI(cmsLocation);
                    log.info("Cms host location for hostGroup '{}' is '{}'", hostGroupNode.getValueProvider().getName(), testLocation.getHost());
                } catch (URISyntaxException e) {
                    log.warn("'{}' is an invalid cmsLocation. cms location can't be used and set to null for hostGroup '{}'", cmsLocation, hostGroupNode.getValueProvider().getName());
                }
            }

            int defaultPort = 0;
            Long longDefaultPort = hostGroupNode.getValueProvider().getLong(HstNodeTypes.VIRTUALHOSTGROUP_PROPERTY_DEFAULT_PORT);
            if (longDefaultPort == null) {
                log.info("VirtualHostGroup '{}' does not have a property hst:defaultport configured. It is necessary for the channel manager to generate correct URLs, " +
                        "when the site should be available on a specific port (not 80 or 443).", hostGroupNode.getValueProvider().getName());
            } else {
                defaultPort = longDefaultPort.intValue();
            }

            for(HstNode virtualHostNode : hostGroupNode.getNodes()) {

                try {
                    VirtualHostService virtualHost = new VirtualHostService(this, virtualHostNode, null, hostGroupNode.getValueProvider().getName(), cmsLocation , defaultPort, hstNodeLoadingCache);
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

        loadChannelsAndBluePrints(hstNodeLoadingCache);
        log.info("VirtualHostsService loading took '{}' ms.", String.valueOf(System.currentTimeMillis() - start));
    }

    public HstManager getHstManager() {
        return hstManager;
    }

    public boolean isExcluded(String pathInfo) {
        // test prefix
        if (prefixExclusions != null && prefixExclusions.length != 0) {
            for(String excludePrefix : prefixExclusions) {
                if(pathInfo.startsWith(excludePrefix)) {
                    return true;
                }
            }
        }

        // test suffix
        if (suffixExclusions != null && suffixExclusions.length != 0) {
            for(String excludeSuffix : suffixExclusions) {
                if(pathInfo.endsWith(excludeSuffix)) {
                    return true;
                }
            }
        }
        return hstManager.isExcludedByHstFilterInitParameter(pathInfo);
    }

    @Override
    public void addVirtualHost(MutableVirtualHost virtualHost) throws IllegalArgumentException {
       Map<String, MutableVirtualHost> rootVirtualHosts =  rootVirtualHostsByGroup.get(virtualHost.getHostGroupName());
       if(rootVirtualHosts == null) {
           rootVirtualHosts =  virtualHostHashMap();
           rootVirtualHostsByGroup.put(virtualHost.getHostGroupName(), rootVirtualHosts);
       }
       rootVirtualHosts.put(virtualHost.getName(), virtualHost);
    }

    @Override
    public Map<String, Map<String, MutableVirtualHost>> getRootVirtualHostsByGroup() {
        return rootVirtualHostsByGroup;
    }

    /**
     * Add this mount for lookup through {@link #getMountByGroupAliasAndType(String, String, String)}
     * @param mount
     */
    public void addMount(Mount mount) throws ServiceException {
        if(registeredMounts.contains(mount)) {
            log.debug(" Mount '{}' already added. Return", mount);
            return;
        }

        registeredMounts.add(mount);
        String hostGroup = mount.getVirtualHost().getHostGroupName();

        List<Mount> mountsForGroup = mountByHostGroup.get(hostGroup);
        if (mountsForGroup == null) {
            mountsForGroup = new ArrayList<Mount>();
            mountByHostGroup.put(hostGroup, mountsForGroup);
        }
        mountsForGroup.add(mount);

        Map<String, Mount> aliasTypeMap = mountByGroupAliasAndType.get(hostGroup);
        if (aliasTypeMap == null) {
            // when a duplicate key is tried to be put, an IllegalArgumentException must be thrown, hence the
            // DuplicateKeyNotAllowedHashMap
            aliasTypeMap = new DuplicateKeyNotAllowedHashMap<String, Mount>();
            mountByGroupAliasAndType.put(hostGroup, aliasTypeMap);
        }
        // add the mount for all alias-type combinations:
        for (String type : mount.getTypes()) {
            if(mount.getAlias() == null) {
                continue;
            }
            String aliasTypeKey = getAliasTypeKey(mount.getAlias(), type);
            try {
                aliasTypeMap.put(aliasTypeKey, mount);
            } catch (IllegalArgumentException e) {
                log.error("Incorrect hst:hosts configuration. Not allowed to have multiple mount's having the same " +
                        "'alias/type/types' combination within a single hst:hostgroup. Failed for mount '{}' in " +
                        "hostgroup '" + mount.getVirtualHost().getHostGroupName()+"' for host '" +
                        mount.getVirtualHost().getHostName() + "'. Make sure that you add a unique 'alias' in " +
                        "combination with the 'types' on the mount within a single hostgroup. The mount '{}' cannot " +
                        "be used for lookup. Change alias for it. Conflicting mounts are " + mount + " that " +
                        "conflicts with "+aliasTypeMap.get(aliasTypeKey) , mount.getName(), mount.getName());
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
        Task matchingTask = null;
        try {
            if (HDC.isStarted()) {
                matchingTask = HDC.getCurrentTask().startSubtask("Host and Mount Matching");
            }
            ResolvedVirtualHost resolvedVirtualHost = matchVirtualHost(hostName);
            ResolvedMount resolvedMount = null;
            if(resolvedVirtualHost != null) {
                resolvedMount  = resolvedVirtualHost.matchMount(contextPath, requestPath);
            }
            return resolvedMount;
        } finally {
            if (matchingTask != null) {
                matchingTask.stop();
            }
        }
    }

    public ResolvedVirtualHost matchVirtualHost(String hostName) throws MatchException {
        if(!virtualHostsConfigured) {
            throw new MatchException("No correct virtual hosts configured. Cannot continue request");
        }
        if (hostName == null) {
            throw new MatchException("HostName not allowed to be null");
        }

        //  hostname matching is always done lower-cased
        hostName =  hostName.toLowerCase();

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
        if(host == null && getDefaultHostName() != null && !getDefaultHostName().equals(portStrippedHostName)) {
            log.debug("Cannot find a mapping for servername '{}'. We try the default servername '{}'", portStrippedHostName, getDefaultHostName());
            if (portNumber != 0) {
                host = matchVirtualHost(getDefaultHostName()+":"+Integer.toString(portNumber));
            }
            else {
                host = matchVirtualHost(getDefaultHostName());
            }
        }
        if(host == null) {
           log.info("We cannot find a servername mapping for '{}'. Even the default servername '{}' cannot be found. Return null", portStrippedHostName , getDefaultHostName());

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
        for(Map<String, MutableVirtualHost> rootVirtualHosts : rootVirtualHostsByGroup.values()) {
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

    public String getDefaultContextPath() {
        return defaultContextPath;
    }

    public boolean isPortInUrl() {
        return showPort;
    }

    public String getScheme(){
        return scheme;
    }

    public int getSchemeNotMatchingResponseCode() {
        return schemeNotMatchingResponseCode;
    }

    public String getLocale() {
        return locale;
    }

    public String getDefaultHostName() {
        return defaultHostName;
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

    @Override
    public List<String> getHostGroupNames() {
        return new ArrayList<>(mountByHostGroup.keySet());
    }

    /**
     * @return a HashMap<String, VirtualHostService> that throws an exception when you put in the same key twice
     */
    public static HashMap<String, MutableVirtualHost> virtualHostHashMap(){
        return new DuplicateKeyNotAllowedHashMap<String, MutableVirtualHost>();
    }


    private String getAliasTypeKey(String alias, String type) {
        return alias.toLowerCase() + '\uFFFF' + type;
    }


    @Override
    public Mount getMountByIdentifier(String uuid) {
        return mountsByIdentifier.get(uuid);
    }

    @Override
    public String getCmsPreviewPrefix() {
        return cmsPreviewPrefix;
    }

    @Override
    public String getChannelManagerHostGroupName() {
        return channelMngrVirtualHostGroupNodeName;
    }

    @Override
    public String getChannelManagerSitesName() {
        return channelMngrSitesNodeName;
    }

    @Override
    public boolean isDiagnosticsEnabled(String ip) {
        return diagnosticsEnabled && (ip == null || diagnosticsForIps.isEmpty() || diagnosticsForIps.contains(ip));
    }

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

    public boolean isChannelMngrSiteAuthenticationSkipped() {
        return channelMngrSiteAuthenticationSkipped;
    }

    @Override
    public Map<String, Channel> getChannels() {
        return channels;
    }

    @Override
    public Channel getChannelByJcrPath(final String channelPath) {
        if (StringUtils.isBlank(channelPath) || !channelPath.startsWith(channelsRoot)) {
            throw new IllegalArgumentException("Expected a valid channel JCR path which should start with '" + channelsRoot + "', but got '" + channelPath + "' instead");
        }
        final String channelId = channelPath.substring(channelPath.length());
        return channels.get(channelId);
    }

    @Override
    public Channel getChannelById(final String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("Expected a channel id, but got '" + id + "' instead");
        }
        return channels.get(id);
    }

    @Override
    public List<Blueprint> getBlueprints() {
        if (!bluePrintsPrototypeChecked) {
            setBluePrintsPrototypes();
        }
        return new ArrayList<>(blueprints.values());
    }

    @Override
    public Blueprint getBlueprint(final String id) {
        if (!bluePrintsPrototypeChecked) {
            setBluePrintsPrototypes();
        }
        return blueprints.get(id);
    }

    @Override
    public Class<? extends ChannelInfo> getChannelInfoClass(final Channel channel) throws ChannelException {
        if (channel == null) {
            throw new ChannelException("Cannot get ChannelInfoClass for null");
        }
        String channelInfoClassName = channel.getChannelInfoClassName();
        if (channelInfoClassName == null) {
            log.debug("No channelInfoClassName defined. Return just the ChannelInfo interface class");
            return ChannelInfo.class;
        }
        try {
            return (Class<? extends ChannelInfo>) ChannelPropertyMapper.class.getClassLoader().loadClass(channelInfoClassName);
        } catch (ClassNotFoundException cnfe) {
            throw new ChannelException("Configured class " + channelInfoClassName + " was not found", cnfe);
        } catch (ClassCastException cce) {
            throw new ChannelException("Configured class " + channelInfoClassName + " does not extend ChannelInfo",
                    cce);
        }
    }

    @Override
    public Class<? extends ChannelInfo> getChannelInfoClass(final String id) throws ChannelException {
        try {
            return getChannelInfoClass(getChannelById(id));
        } catch (IllegalArgumentException e) {
            throw new ChannelException("ChannelException for getChannelInfoClass", e);
        }

    }

    @Override
    public <T extends ChannelInfo> T getChannelInfo(final Channel channel) throws ChannelException {
        Class<? extends ChannelInfo> channelInfoClass = getChannelInfoClass(channel);
        return (T) ChannelUtils.getChannelInfo(channel.getProperties(), channelInfoClass);
    }

    @Override
    public ResourceBundle getResourceBundle(final Channel channel, final Locale locale) {
        String channelInfoClassName = channel.getChannelInfoClassName();
        if (channelInfoClassName != null) {
            return ResourceBundle.getBundle(channelInfoClassName, locale);
        }
        return null;
    }

    @Override
    public List<HstPropertyDefinition> getPropertyDefinitions(final Channel channel) {
        try {
            if (channel.getChannelInfoClassName() != null) {
                Class<? extends ChannelInfo> channelInfoClass = getChannelInfoClass(channel);
                if (channelInfoClass != null) {
                    return ChannelInfoClassProcessor.getProperties(channelInfoClass);
                }
            }
        } catch (ChannelException ex) {
            log.warn("Could not load properties", ex);
        }
        return Collections.emptyList();
    }

    @Override
    public List<HstPropertyDefinition> getPropertyDefinitions(final String channelId) {
         return getPropertyDefinitions(getChannelById(channelId));
    }


    private void loadBlueprints(final HstNode rootConfigNode) {
        HstNode blueprintsNode = rootConfigNode.getNode(HstNodeTypes.NODENAME_HST_BLUEPRINTS);
        if (blueprintsNode != null) {
            for (HstNode blueprintNode : blueprintsNode.getNodes()) {
                blueprints.put(blueprintNode.getName(), BlueprintHandler.buildBlueprint(blueprintNode));
            }
        }
    }

    private void loadChannels(final HstNode rootConfigNode) {
        HstNode channelsNode = rootConfigNode.getNode(HstNodeTypes.NODENAME_HST_CHANNELS);
        if (channelsNode != null) {
            for (HstNode channelNode : channelsNode.getNodes()) {
                loadChannel(channelNode);
            }
        } else {
            log.warn("Cannot load channels because node '{}' does not exist",
                    rootConfigNode.getValueProvider().getPath() + "/" + HstNodeTypes.NODENAME_HST_CHANNELS);
        }
    }

    private void loadChannel(HstNode currNode) {
        Channel channel = ChannelPropertyMapper.readChannel(currNode);
        channels.put(channel.getId(), channel);
    }

    private void populateChannelForMount(ContextualizableMount mount, final HstNodeLoadingCache hstNodeLoadingCache) {
        // we are only interested in Mount's that have isMapped = true and that
        // are live mounts: We do not display 'preview' Mounts in cms: instead, a
        // live mount decorated as preview are shown
        if (!mount.isMapped() || mount.isPreview() || mount.getHstSite() == null) {
            log.debug("Skipping mount '{}' because it is either not mapped or is a preview mount", mount.getName());
            return;
        }
        String channelPath = mount.getChannelPath();
        if (channelPath == null) {
            // mount does not have an associated channel
            log.debug("Ignoring mount '" + mount.getName() + "' since it does not have a channel path");
            return;
        }
        if (!channelPath.startsWith(channelsRoot)) {
            log.warn(
                    "Channel path '{}' is not part of the HST configuration under {}, ignoring channel info for mount {}.  Use the full repository path for identification.",
                    new Object[] { channelPath, hstNodeLoadingCache.getRootPath(), mount.getName() });

            return;
        }
        Channel channel = channels.get(channelPath.substring(channelsRoot.length()));
        if (channel == null) {
            log.warn("Unknown channel {}, ignoring mount {}", channelPath, mount.getName());
            return;
        }
        if (channel.getUrl() != null) {
            // We already encountered this channel while walking over all the mounts. This mount
            // therefore points to the same channel as another mount, which is not allowed (each channel has only
            // one mount)
            log.warn("Channel {} contains multiple mounts - analysing mount {}, found url {} in channel", new Object[] {
                    channelPath, mount.getName(), channel.getUrl() });

            return;
        }

        String mountPoint = mount.getMountPoint();
        if (mountPoint != null) {
            channel.setHstMountPoint(mountPoint);
            channel.setContentRoot(mount.getCanonicalContentPath());
            String configurationPath = mount.getHstSite().getConfigurationPath();
            if (configurationPath != null) {
                channel.setHstConfigPath(configurationPath);
            }
        }

        final HstSite previewHstSite = mount.getPreviewHstSite();
        channel.setPreviewHstConfigExists(previewHstSite.hasPreviewConfiguration());


        HstNode channelRootConfigNode = hstNodeLoadingCache.getNode(previewHstSite.getConfigurationPath());
        if (channelRootConfigNode == null) {
            final Set<String> empty = Collections.emptySet();
            channel.setChangedBySet(empty);
        } else {
            channel.setChangedBySet(new ChannelLazyLoadingChangedBySet(channelRootConfigNode, previewHstSite));
        }

        String mountPath = mount.getMountPath();
        channel.setLocale(mount.getLocale());
        channel.setMountId(mount.getIdentifier());
        channel.setMountPath(mountPath);

        VirtualHost virtualHost = mount.getVirtualHost();
        channel.setCmsPreviewPrefix(virtualHost.getVirtualHosts().getCmsPreviewPrefix());
        channel.setContextPath(mount.onlyForContextPath());
        channel.setHostname(virtualHost.getHostName());

        StringBuilder url = new StringBuilder();
        url.append(mount.getScheme());
        url.append("://");
        url.append(virtualHost.getHostName());
        if (mount.isPortInUrl()) {
            int port = mount.getPort();
            if (port != 0 && port != 80 && port != 443) {
                url.append(':');
                url.append(mount.getPort());
            }
        }
        if (virtualHost.isContextPathInUrl() && mount.onlyForContextPath() != null) {
            url.append(mount.onlyForContextPath());
        }
        if (StringUtils.isNotEmpty(mountPath)) {
            if (!mountPath.startsWith("/")) {
                url.append('/');
            }
            url.append(mountPath);
        }
        channel.setUrl(url.toString());
        mount.setChannel(channel);
    }

    private void loadChannelsAndBluePrints(HstNodeLoadingCache hstNodeLoadingCache) {
        long start = System.currentTimeMillis();
        final HstNode rootConfigNode = hstNodeLoadingCache.getNode(hstNodeLoadingCache.getRootPath());
        bluePrintsPrototypeChecked = false;
        loadBlueprints(rootConfigNode);
        List<Mount> mountsForCurrentHostGroup = Collections.emptyList();
        if (channelMngrVirtualHostGroupNodeName == null) {
            log.warn("Cannot load the Channel Manager because no host group configured on hst:hosts node");
        } else if (!getHostGroupNames().contains(channelMngrVirtualHostGroupNodeName)) {
            log.warn("Configured channel manager host group name {} does not exist", channelMngrVirtualHostGroupNodeName);
        } else {
            // in channel manager only the mounts for at most ONE single hostGroup are shown
            mountsForCurrentHostGroup = getMountsByHostGroup(channelMngrVirtualHostGroupNodeName);
            if (mountsForCurrentHostGroup.isEmpty()) {
                log.warn("No mounts found in host group {}.", channelMngrVirtualHostGroupNodeName);
            }
        }
        // load all the channels, even if they are not used by the current hostGroup
        loadChannels(rootConfigNode);
        for (Mount mountForCurrentHostGroup : mountsForCurrentHostGroup) {
            if (mountForCurrentHostGroup instanceof ContextualizableMount) {
                populateChannelForMount((ContextualizableMount) mountForCurrentHostGroup, hstNodeLoadingCache);
            }
        }

        // for ALL the mounts in ALL the host groups, set the channel Info if available
        for (String hostGroupName : getHostGroupNames()) {
            for (Mount mount : getMountsByHostGroup(hostGroupName)) {
                if (mount instanceof MutableMount) {
                    try {
                        String channelPath = mount.getChannelPath();
                        if (StringUtils.isEmpty(channelPath)) {
                            log.debug(
                                    "Mount '{}' does not have a channelpath configured. Skipping setting channelInfo.",
                                    mount);
                            continue;
                        }
                        String channelNodeName = channelPath.substring(channelPath.lastIndexOf("/") + 1);
                        Channel channel = this.channels.get(channelNodeName);
                        if (channel == null) {
                            log.debug(
                                    "Mount '{}' has channelpath configured that does not point to a channel info. Skipping setting channelInfo.",
                                    mount);
                            continue;
                        }
                        log.debug("Setting channel info for mount '{}'.", mount);
                        ((MutableMount) mount).setChannelInfo(getChannelInfo(channel));
                    } catch (ChannelException e) {
                        log.error("Could not set channel info to mount", e);
                    }
                }
            }
        }
        discardChannelsWithoutMountForCurrentHostGroup();
        log.info("Channel manager load took '{}' ms.", (System.currentTimeMillis() - start));
    }

    private void discardChannelsWithoutMountForCurrentHostGroup() {
        List<String> channelsToDiscard = new ArrayList<String>();
        for (Map.Entry<String, Channel> entry : channels.entrySet()) {
            if (entry.getValue().getMountId() == null) {
                log.warn("Channel '{}' is not referred by any mount for hostgroup '{}'. Discarding this channel",
                        entry.getValue().getId(), channelMngrVirtualHostGroupNodeName);
                channelsToDiscard.add(entry.getKey());
            }
        }
        for (String key : channelsToDiscard) {
            channels.remove(key);
        }
    }

    private void setBluePrintsPrototypes() {
        if (bluePrintsPrototypeChecked) {
            return;
        }
        try (HstNodeLoadingCache.LazyCloseableSession session = hstNodeLoadingCache.createLazyCloseableSession()) {
            for (Blueprint blueprint : blueprints.values()) {
                String prototypePath = BlueprintHandler.SUBSITE_TEMPLATES_PATH + blueprint.getId();
                if (session.getSession().nodeExists(prototypePath)) {
                    blueprint.setHasContentPrototype(true);
                }
            }
        } catch (Exception e) {
            throw new ModelLoadingException("Could not check blueprint prototypes : ", e);
        }
        bluePrintsPrototypeChecked = true;
    }
}
