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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.channel.Blueprint;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.configuration.channel.HstPropertyDefinition;
import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.MutableVirtualHosts;
import org.hippoecm.hst.configuration.hosting.PortMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.container.site.CompositeHstSite;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.core.container.HstComponentRegistry;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.hippoecm.hst.platform.configuration.cache.HstConfigurationLoadingCache;
import org.hippoecm.hst.platform.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.platform.configuration.channel.BlueprintHandler;
import org.hippoecm.hst.platform.configuration.channel.ChannelInfoClassProcessor;
import org.hippoecm.hst.platform.configuration.channel.ChannelUtils;
import org.hippoecm.hst.platform.configuration.model.ModelLoadingException;
import org.hippoecm.hst.provider.ValueProvider;
import org.hippoecm.hst.resourcebundle.CompositeResourceBundle;
import org.hippoecm.hst.site.request.ResolvedVirtualHostImpl;
import org.hippoecm.hst.util.DuplicateKeyNotAllowedHashMap;
import org.hippoecm.hst.util.PathUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.context.HippoWebappContext;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;
import org.onehippo.cms7.services.hst.Channel;
import org.onehippo.repository.l10n.LocalizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.ConfigurationUtils.isSupportedSchemeNotMatchingResponseCode;
import static org.hippoecm.hst.configuration.ConfigurationUtils.supportedSchemeNotMatchingResponseCodesAsString;
import static org.hippoecm.hst.configuration.HstNodeTypes.MOUNT_PROPERTY_NOCHANNELINFO;

public class VirtualHostsService implements MutableVirtualHosts {

    private static final Logger log = LoggerFactory.getLogger(VirtualHostsService.class);

    private final static String WILDCARD = "_default_";
    private final static String CHANNEL_PARAMETERS_TRANSLATION_LOCATION = "hippo:hst.channelparameters";

    private final String contextPath;
    private HstNodeLoadingCache hstNodeLoadingCache;
    private Map<String, Map<String, VirtualHost>> rootVirtualHostsByGroup = new DuplicateKeyNotAllowedHashMap<>();

    private Map<String, List<Mount>> mountByHostGroup = new HashMap<>();
    private Map<String, Mount> mountsByIdentifier = new HashMap<>();
    private Map<String, Map<String, Mount>> mountByGroupAliasAndType = new HashMap<>();

    private List<Mount> registeredMounts = new ArrayList<>();

    private String defaultHostName;

    public static final String DEFAULT_HOMEPAGE_SITEMAP_ITEM = "root";
    /**
     * The homepage for this VirtualHosts.
     */
    private String homepage = DEFAULT_HOMEPAGE_SITEMAP_ITEM;

    public static final String DEFAULT_PAGE_NOT_FOUND_PATH_INFO = "error";

    /**
     * The pageNotFound for this VirtualHosts.
     */
    private String pageNotFound = DEFAULT_PAGE_NOT_FOUND_PATH_INFO;


    public static final String DEFAULT_LOCALE_STRING = "en_US";
    /**
     * The general locale configured on VirtualHosts. When the backing configuration does not contain a locale, the
     * value is <code>null</code>
     */
    private String locale = DEFAULT_LOCALE_STRING;

    /**
     * Whether the {@link Mount}'s below this VirtualHostsService should show the hst version as a response header
     * when they are a preview {@link Mount}
     */
    private boolean versionInPreviewHeader = true;

    private boolean virtualHostsConfigured;

    public static final String DEFAULT_SCHEME = "http";

    private String scheme = DEFAULT_SCHEME;
    /**
     * if the request is for example http but https is required, the default response code is SC_MOVED_PERMANENTLY. If nothing
     * needs to be done, set a 200. Not found 404 and not authorized Forbidden
     */
    private int schemeNotMatchingResponseCode = HttpServletResponse.SC_MOVED_PERMANENTLY;
    private boolean contextPathInUrl = true;

    private boolean showPort = true;

    /**
     * the cms preview prefix : The prefix all URLs when accessed through the CMS
     */
    private String cmsPreviewPrefix;

    // default depth of -1 meaning no maximum
    private int diagnosticsDepth = -1;

    // default threshold of -1 meaning no threshold
    private long diagnosticsThresholdMillis = -1;

    // default subtask threshold of -1 meaning no threshold
    private long diagnosticsUnitThresholdMillis = -1;

    private boolean diagnosticsEnabled;

    private boolean cacheable = false;

    private String [] defaultResourceBundleIds;

    private boolean channelMngrSiteAuthenticationSkipped;

    private Set<String> diagnosticsForIps = new HashSet<>(0);

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

    private final Map<String, Map<String, Channel>> channelsByHostGroup = new HashMap<>();

    private final Map<String, Blueprint> blueprints = new HashMap<>();
    private boolean bluePrintsPrototypeChecked;
    private String[] hstFilterPrefixExclusions;
    private String[] hstFilterSuffixExclusions;

    private HstComponentRegistry componentRegistry;

    public VirtualHostsService(final String contextPath, final ContainerConfiguration websiteContainerConfiguration, final HstNodeLoadingCache hstNodeLoadingCache,
                               final HstConfigurationLoadingCache hstConfigurationLoadingCache) {
        long start = System.currentTimeMillis();
        this.hstNodeLoadingCache = hstNodeLoadingCache;
        this.contextPath = contextPath;
        virtualHostsConfigured = true;

        // quick check if the basic mandatory hst nodes are available. If not, we throw a runtime model loading exception
        quickModelCheck();
        HstNode vhostsNode = hstNodeLoadingCache.getNode(hstNodeLoadingCache.getRootPath()+"/hst:hosts");
        if (vhostsNode == null) {
            throw new ModelLoadingException("No hst node found for '"+hstNodeLoadingCache.getRootPath()+"/hst:hosts'. Cannot load model.'");
        }
        ValueProvider vHostConfValueProvider = vhostsNode.getValueProvider();

        if (vHostConfValueProvider.hasProperty(HstNodeTypes.VIRTUALHOSTS_PROPERTY_SHOWCONTEXTPATH)) {
            contextPathInUrl = vHostConfValueProvider.getBoolean(HstNodeTypes.VIRTUALHOSTS_PROPERTY_SHOWCONTEXTPATH);
        }

        cmsPreviewPrefix = vHostConfValueProvider.getString(HstNodeTypes.VIRTUALHOSTS_PROPERTY_CMSPREVIEWPREFIX);
        diagnosticsEnabled = vHostConfValueProvider.getBoolean(HstNodeTypes.VIRTUALHOSTS_PROPERTY_DIAGNOSTISC_ENABLED);
        if (vHostConfValueProvider.hasProperty(HstNodeTypes.VIRTUALHOSTS_PROPERTY_DIAGNOSTICS_DEPTH)) {
            diagnosticsDepth = vHostConfValueProvider.getLong(HstNodeTypes.VIRTUALHOSTS_PROPERTY_DIAGNOSTICS_DEPTH).intValue();
        }
        if (vHostConfValueProvider.hasProperty(HstNodeTypes.VIRTUALHOSTS_PROPERTY_DIAGNOSTICS_THRESHOLD_MILLIS)) {
            diagnosticsThresholdMillis = vHostConfValueProvider.getLong(HstNodeTypes.VIRTUALHOSTS_PROPERTY_DIAGNOSTICS_THRESHOLD_MILLIS);
        }
        if (vHostConfValueProvider.hasProperty(HstNodeTypes.VIRTUALHOSTS_PROPERTY_DIAGNOSTICS_UNIT_THRESHOLD_MILLIS)) {
            diagnosticsUnitThresholdMillis = vHostConfValueProvider.getLong(HstNodeTypes.VIRTUALHOSTS_PROPERTY_DIAGNOSTICS_UNIT_THRESHOLD_MILLIS);
        }

        String[] ips = vHostConfValueProvider.getStrings(HstNodeTypes.VIRTUALHOSTS_PROPERTY_DIAGNOSTICS_FOR_IPS);
        Collections.addAll(diagnosticsForIps, ips);
        if (cmsPreviewPrefix == null) {
            // there is no explicit cms preview prefix configured. Take the default one from the website container configuration
            cmsPreviewPrefix = websiteContainerConfiguration.getString("cms.default.cmspreviewprefix", StringUtils.EMPTY);

        }
        if(StringUtils.isEmpty(cmsPreviewPrefix)) {
            log.info("cmsPreviewPrefix property '{}' on hst:hosts is configured to be empty. This means that when " +
                    "the cms and site run on the same host that a client who visited the preview in cms also" +
            		"sees the preview without the cms where he expected to see the live. ",
                    HstNodeTypes.VIRTUALHOSTS_PROPERTY_CMSPREVIEWPREFIX);
        } else {
            cmsPreviewPrefix =  PathUtils.normalizePath(cmsPreviewPrefix);
        }

        String sites = vHostConfValueProvider.getString(HstNodeTypes.VIRTUALHOSTS_PROPERTY_CHANNEL_MNGR_SITES);
        if(!StringUtils.isEmpty(sites)) {
            log.info("Channel manager will load work with hst:sites node '{}' instead of the default '{}'.",
                    sites, DEFAULT_CHANNEL_MNGR_SITES_NODE_NAME);
            channelMngrSitesNodeName = sites;
        }

        if (vHostConfValueProvider.hasProperty(HstNodeTypes.VIRTUALHOSTS_PROPERTY_SHOWPORT)) {
            showPort = vHostConfValueProvider.getBoolean(HstNodeTypes.VIRTUALHOSTS_PROPERTY_SHOWPORT);
        }

        if (vHostConfValueProvider.hasProperty(HstNodeTypes.VIRTUALHOSTS_PROPERTY_SCHEME)) {
            scheme = vHostConfValueProvider.getString(HstNodeTypes.VIRTUALHOSTS_PROPERTY_SCHEME);
        }

        if (vHostConfValueProvider.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCALE)) {
            locale = vHostConfValueProvider.getString(HstNodeTypes.GENERAL_PROPERTY_LOCALE);
        }
        if (vHostConfValueProvider.hasProperty(HstNodeTypes.GENERAL_PROPERTY_HOMEPAGE)) {
            homepage = vHostConfValueProvider.getString(HstNodeTypes.GENERAL_PROPERTY_HOMEPAGE);
        }
        if (vHostConfValueProvider.hasProperty(HstNodeTypes.GENERAL_PROPERTY_PAGE_NOT_FOUND)) {
            pageNotFound = vHostConfValueProvider.getString(HstNodeTypes.GENERAL_PROPERTY_PAGE_NOT_FOUND);
        }
        if (vHostConfValueProvider.hasProperty(HstNodeTypes.VIRTUALHOSTS_PROPERTY_CHANNEL_MNGR_SITE_AUTHENTICATION_SKIPPED)) {
            channelMngrSiteAuthenticationSkipped = vHostConfValueProvider.getBoolean(HstNodeTypes.VIRTUALHOSTS_PROPERTY_CHANNEL_MNGR_SITE_AUTHENTICATION_SKIPPED);
        } else {
            log.info("No '{}' configured on hst:hosts node. Setting it by default to true", HstNodeTypes.VIRTUALHOSTS_PROPERTY_CHANNEL_MNGR_SITE_AUTHENTICATION_SKIPPED);
            channelMngrSiteAuthenticationSkipped = true;
        }


        if(vHostConfValueProvider.hasProperty(HstNodeTypes.GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER)) {
            versionInPreviewHeader = vHostConfValueProvider.getBoolean(HstNodeTypes.GENERAL_PROPERTY_VERSION_IN_PREVIEW_HEADER);
        }

        if(vHostConfValueProvider.hasProperty(HstNodeTypes.GENERAL_PROPERTY_CACHEABLE)) {
            cacheable = vHostConfValueProvider.getBoolean(HstNodeTypes.GENERAL_PROPERTY_CACHEABLE);
            log.info("Page caching for HST is set to : {} ", cacheable);
        }

        defaultHostName  = vHostConfValueProvider.getString(HstNodeTypes.VIRTUALHOSTS_PROPERTY_DEFAULTHOSTNAME);
        if (defaultHostName != null) {
            log.warn("The property '{}' has been deprecated in 13.2.0 and its usage will be dropped in 14.0.0. There is " +
                    "no replacement, instead use explicit configured hosts. Using the property '{}' can result in errors " +
                            "when using hst branches", HstNodeTypes.VIRTUALHOSTS_PROPERTY_DEFAULTHOSTNAME);
            defaultHostName = defaultHostName.toLowerCase();
        }

        if(scheme == null || "".equals(scheme)) {
            scheme = DEFAULT_SCHEME;
        }
        if (vHostConfValueProvider.hasProperty(HstNodeTypes.GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE)) {
            int statusCode = (int)vHostConfValueProvider.getLong(HstNodeTypes.GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE).longValue();
            if (isSupportedSchemeNotMatchingResponseCode(statusCode)) {
                schemeNotMatchingResponseCode = statusCode;
            } else {
                log.warn("Invalid '{}' configured on '{}'. Use inherited value. Supported values are '{}'", new Object[]{HstNodeTypes.GENERAL_PROPERTY_SCHEME_NOT_MATCH_RESPONSE_CODE,
                        vHostConfValueProvider.getPath(), supportedSchemeNotMatchingResponseCodesAsString()});
            }
        }

        defaultResourceBundleIds = StringUtils.split(vHostConfValueProvider.getString(HstNodeTypes.GENERAL_PROPERTY_DEFAULT_RESOURCE_BUNDLE_ID), " ,\t\f\r\n");

        // now we loop through the hst:hostgroup nodes first:
        for(HstNode hostGroupNode : vhostsNode.getNodes()) {
            // assert node is of type virtualhostgroup
            if(!HstNodeTypes.NODETYPE_HST_VIRTUALHOSTGROUP.equals(hostGroupNode.getNodeTypeName())) {
                throw new ModelLoadingException("Expected a hostgroup node of type '"+HstNodeTypes.NODETYPE_HST_VIRTUALHOSTGROUP+"' but found a node of type '"+hostGroupNode.getNodeTypeName()+"' at '"+hostGroupNode.getValueProvider().getPath()+"'");
            }
            Map<String, VirtualHost> rootVirtualHosts =  virtualHostHashMap();
            try {
                rootVirtualHostsByGroup.put(hostGroupNode.getValueProvider().getName(), rootVirtualHosts);
            } catch (IllegalArgumentException e) {
                throw new ModelLoadingException("It should not be possible to have two hostgroups with the same name. We found duplicate group with name '"+hostGroupNode.getValueProvider().getName()+"'");
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
                    VirtualHostService virtualHost = new VirtualHostService(this, virtualHostNode, null,
                            hostGroupNode.getValueProvider().getName() , defaultPort, hstNodeLoadingCache, hstConfigurationLoadingCache);
                    rootVirtualHosts.put(virtualHost.getName(), virtualHost);
                } catch (ModelLoadingException e) {
                    if (e.isMissingEnvironmentVariable()) {
                        log.info("Skip virtualhost with name '{}'because missing environment variable.",
                                virtualHostNode.getValueProvider().getName(), e);
                    } else {
                        log.error("Unable to add virtualhost with name '{}'. Fix the configuration. This virtualhost will be skipped.",
                                virtualHostNode.getValueProvider().getName(), e);
                    }
                    // continue to next virtualHost
                } catch (DuplicateKeyNotAllowedHashMap.DuplicateKeyException e) {
                    log.error("VirtualHostMap is not allowed to have duplicate hostnames. This problem might also result from having two hosts configured"
                            + "something like 'preview.mycompany.org' and 'www.mycompany.org'. This results in 'mycompany.org' being a duplicate in a hierarchical presentation which the model makes from hosts splitted by dots. "
                            + "In this case, make sure to configure them hierarchically as org -> mycompany -> (preview , www)", e);
                } catch (IllegalArgumentException e) {
                    // do not use virtualHostNode.getSubstitutedName() since that one can throw an illegal state exc
                    log.error("Exception while trying to load virtual host '{}'", virtualHostNode.getName(), e);
               }
            }
        }

        long startChannelLoading = System.currentTimeMillis();
        loadChannelsMap();
        log.info("Loading channels took '{}' ms", (System.currentTimeMillis() - startChannelLoading));

        loadBluePrints(hstNodeLoadingCache);

        log.info("VirtualHostsService loading took '{}' ms.", String.valueOf(System.currentTimeMillis() - start));
    }

    public void setHstFilterPrefixExclusions(final String[] hstFilterPrefixExclusions) {
        this.hstFilterPrefixExclusions = hstFilterPrefixExclusions;
    }

    public void setHstFilterSuffixExclusions(final String[] hstFilterSuffixExclusions) {
        this.hstFilterSuffixExclusions = hstFilterSuffixExclusions;
    }

    public void loadChannelsMap() {

        channelsByHostGroup.clear();

        for (String hostGroupName : getHostGroupNames()) {
            if (!channelsByHostGroup.containsKey(hostGroupName)) {
                channelsByHostGroup.put(hostGroupName, new HashMap<>());
            }
            Map<String, Channel> hostGroupChannels = channelsByHostGroup.get(hostGroupName);
            for (Mount mount : getMountsByHostGroup(hostGroupName)) {
                if (mount instanceof ContextualizableMount) {
                    if (mount.isPreview()) {
                        log.debug("Explicit preview mounts are not added as channel (only their live equivalent");
                        continue;
                    }
                    HstSite hstSite = mount.getHstSite();
                    if (!mount.isMapped() || hstSite == null || hstSite.getChannel() == null) {
                        log.debug("Mount '{}' does not have channel associated with it", mount.getName());
                        continue;
                    }

                    // below bit dirty check. Perhaps cleaner in the future to introduce something like
                    // 'Channel#isCanonical()' to indicate whether the Channel object is to be used in channel mgr or
                    // channels map or not
                    if (mount instanceof PageModelApiMount) {
                        log.debug("Channel of PageModelApiMount is already represented by the parent mount");
                        continue;
                    }

                    Channel channel = hstSite.getChannel();

                    if (hostGroupChannels.containsKey(channel.getId())) {
                        warnDuplicateChannel(hostGroupName, mount, channel);
                        continue;
                    }
                    HstSite previewHstSite = ((ContextualizableMount)mount).getPreviewHstSite();
                    Channel previewChannel = previewHstSite.getChannel();

                    if (previewChannel != null && hostGroupChannels.containsKey(previewChannel.getId())) {
                        warnDuplicateChannel(hostGroupName, mount, previewChannel);
                        continue;
                    }

                    hostGroupChannels.put(channel.getId(), channel);

                    if (previewChannel != null) {
                        if (previewChannel.getId().equals(channel.getId())) {
                            log.debug("For channel '{}' there is no explicit preview configuration (yet).", channel);
                        } else {
                            hostGroupChannels.put(previewChannel.getId(), previewChannel);
                        }
                    }

                    for (HstSite s : new HstSite[]{hstSite, previewHstSite}) {
                        if (s instanceof CompositeHstSite) {
                            CompositeHstSite compositeHstSite = (CompositeHstSite)s;
                            for (HstSite site : compositeHstSite.getBranches().values()) {
                                Channel branch = site.getChannel();
                                if (hostGroupChannels.containsKey(branch.getId())) {
                                    warnDuplicateChannel(hostGroupName, mount, branch);
                                    continue;
                                }
                                hostGroupChannels.put(branch.getId(), branch);
                            }
                        }
                    }

                }
            }
        }

    }

    private void warnDuplicateChannel(final String hostGroupName, final Mount mount, final Channel channel) {
        log.warn("Skip channel with id '{}' because already present for host group '{}'. Most likely there is a " +
                "parent channel that already is a channel mngr channel. Set '{} = true' on mount " +
                "'{}' to avoid this problem.", channel.getId(), hostGroupName, MOUNT_PROPERTY_NOCHANNELINFO, mount);
    }

    private void quickModelCheck() {
        final String rootPath = hstNodeLoadingCache.getRootPath();
        String[] mandatoryNodes = new String[]{rootPath + "/hst:hosts", rootPath +"/hst:sites", rootPath +"/hst:configurations"};
        for (String mandatoryNode : mandatoryNodes) {
            if (hstNodeLoadingCache.getNode(mandatoryNode) == null) {
                throw new ModelLoadingException("Hst Model cannot be loaded because missing node '"+mandatoryNode+"'");
            }
        }
    }

    public boolean isHstFilterExcludedPath(String pathInfo) {
        if (hstFilterPrefixExclusions != null) {
            for(String excludePrefix : hstFilterPrefixExclusions) {
                if(pathInfo.startsWith(excludePrefix)) {
                    log.debug("pathInfo '{}' is excluded by init parameter containing excludePrefix '{}'", pathInfo, excludePrefix);
                    return true;
                }
            }
        }
        if (hstFilterSuffixExclusions != null) {
            for(String excludeSuffix : hstFilterSuffixExclusions) {
                if(pathInfo.endsWith(excludeSuffix)) {
                    log.debug("pathInfo '{}' is excluded by init parameter containing excludeSuffix '{}'", pathInfo, excludeSuffix);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Map<String, Map<String, VirtualHost>> getRootVirtualHostsByGroup() {
        return rootVirtualHostsByGroup;
    }

    /**
     * Add this mount for lookup through {@link #getMountByGroupAliasAndType(String, String, String)}
     * @param mount
     */
    public void addMount(Mount mount) {
        if(registeredMounts.contains(mount)) {
            log.debug(" Mount '{}' already added. Return", mount);
            return;
        }

        registeredMounts.add(mount);
        String hostGroup = mount.getVirtualHost().getHostGroupName();

        List<Mount> mountsForGroup = mountByHostGroup.get(hostGroup);
        if (mountsForGroup == null) {
            mountsForGroup = new ArrayList<>();
            mountByHostGroup.put(hostGroup, mountsForGroup);
        }
        mountsForGroup.add(mount);

        Map<String, Mount> aliasTypeMap = mountByGroupAliasAndType.get(hostGroup);
        if (aliasTypeMap == null) {
            // when a duplicate key is tried to be put, an IllegalArgumentException must be thrown, hence the
            // DuplicateKeyNotAllowedHashMap
            aliasTypeMap = new DuplicateKeyNotAllowedHashMap<>();
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

    @Deprecated
    public ResolvedMount matchMount(String hostName, String contextPath, String requestPath) throws MatchException {
        return matchMount(hostName, requestPath);
    }


    public ResolvedMount matchMount(String hostName, String requestPath) throws MatchException {
        Task matchingTask = null;
        boolean hdcStarted = HDC.isStarted();
        try {
            if (hdcStarted) {
                matchingTask = HDC.getCurrentTask().startSubtask("Host and Mount Matching");
            }
            ResolvedVirtualHost resolvedVirtualHost = matchVirtualHost(hostName);
            ResolvedMount resolvedMount = null;
            if(resolvedVirtualHost != null) {
                resolvedMount  = resolvedVirtualHost.matchMount(requestPath);
            }

            if (hdcStarted) {
                if (resolvedVirtualHost != null && resolvedVirtualHost.getVirtualHost() != null) {
                    matchingTask.setAttribute("virtualhost", resolvedVirtualHost.getVirtualHost().toString());
                }
                if (resolvedMount != null && resolvedMount.getMount() != null) {
                    matchingTask.setAttribute("mount", resolvedMount.getMount().toString());
                }
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
        int offset = portStrippedHostName.lastIndexOf(':');
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
           log.info("We cannot find a servername mapping for '{}'.  Return null", portStrippedHostName);

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
        for(Map<String, VirtualHost> rootVirtualHosts : rootVirtualHostsByGroup.values()) {
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

    public String getContextPath() {
        return contextPath;
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

    @Deprecated
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
    public static HashMap<String, VirtualHost> virtualHostHashMap(){
        return new DuplicateKeyNotAllowedHashMap<>();
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
    public String getChannelManagerSitesName() {
        return channelMngrSitesNodeName;
    }

    @Override
    public boolean isDiagnosticsEnabled(String ip) {
        return diagnosticsEnabled && (ip == null || diagnosticsForIps.isEmpty() || diagnosticsForIps.contains(ip));
    }

    public int getDiagnosticsDepth() {
        return diagnosticsDepth;
    }

    public long getDiagnosticsThresholdMillis() {
        return diagnosticsThresholdMillis;
    }

    public long getDiagnosticsUnitThresholdMillis() {
        return diagnosticsUnitThresholdMillis;
    }

    public boolean isCacheable() {
        return cacheable;
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
    public Map<String, Channel> getChannels(final String hostGroup) {
        final Map<String, Channel> channels = channelsByHostGroup.get(hostGroup);
        if (channels == null) {
            return Collections.emptyMap();
        }
        return channels;
    }

    @Override
    public Map<String, Map<String, Channel>> getChannels()  {
        return channelsByHostGroup;
    }

    @Override
    public Channel getChannelByJcrPath(final String hostGroup, final String channelPath) {
        final Map<String, Channel> channels = channelsByHostGroup.get(hostGroup);
        if (channels == null) {
            return null;
        }
        if (StringUtils.isBlank(channelPath)) {
            throw new IllegalArgumentException(String.format("Expected a valid channel JCR path for channelPath but got '%s' instead", channelPath));
        }
        return channels.values().stream().filter(channel -> channelPath.equals(channel.getChannelPath())).findFirst().orElse(null);
    }

    @Override
    public Channel getChannelById(final String hostGroup, final String id) {
        final Map<String, Channel> channels = channelsByHostGroup.get(hostGroup);
        if (channels == null) {
            return null;
        }
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
        return ChannelUtils.getChannelInfoClass(channel);
    }

    @Override
    public Class<? extends ChannelInfo> getChannelInfoClass(final String hostGroup, final String id) throws ChannelException {
        try {
            return getChannelInfoClass(getChannelById(hostGroup, id));
        } catch (IllegalArgumentException e) {
            throw new ChannelException("ChannelException for getChannelInfoClass", e);
        }
    }

    @Override
    public List<Class<? extends ChannelInfo>> getChannelInfoMixins(final Channel channel) throws ChannelException {
        if (channel == null) {
            throw new ChannelException("Cannot get ChannelMixinClasses for null");
        }

        return ChannelUtils.getChannelInfoMixins(channel);
    }

    @Override
    public List<Class<? extends ChannelInfo>> getChannelInfoMixins(final String hostGroup, final String id) throws ChannelException {
        try {
            return getChannelInfoMixins(getChannelById(hostGroup, id));
        } catch (IllegalArgumentException e) {
            throw new ChannelException("ChannelException for getChannelInfoMixins", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ChannelInfo> T getChannelInfo(final Channel channel) throws ChannelException {
        final Class<? extends ChannelInfo> channelInfoClass = getChannelInfoClass(channel);
        final List<Class<? extends ChannelInfo>> channelInfoMixins = getChannelInfoMixins(channel);
        return (T) ChannelUtils.getChannelInfo(channel.getContextPath(), channel.getProperties(), channelInfoClass,
                channelInfoMixins.toArray(new Class[channelInfoMixins.size()]));
    }

    @Override
    public ResourceBundle getResourceBundle(final Channel channel, final Locale locale) {

        final HippoWebappContext webappContext = HippoWebappContextRegistry.get().getContext(channel.getContextPath());
        if (webappContext == null) {
            log.warn("Cannot find webapp context for '{}'. Return null.", channel.getContextPath());
            return null;
        }
        final String channelInfoClassName = channel.getChannelInfoClassName();

        final ClassLoader classLoader = webappContext.getServletContext().getClassLoader();

        final List<ResourceBundle> bundles = channelInfoClassName != null ?
                Stream.of(channelInfoClassName)
                    .map(className -> getClass(classLoader, className))
                    .filter(Objects::nonNull)
                    .flatMap(this::appendInterfaces)
                    .distinct()
                    .map(Class::getName)
                    .map(className -> loadResourceBundle(className, classLoader, locale))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList())
                : new ArrayList<>();

        final List<String> channelInfoMixinNames = channel.getChannelInfoMixinNames();

        if (channelInfoMixinNames != null) {
            ResourceBundle bundle;
            for (String channelInfoMixinName : channelInfoMixinNames) {
                bundle = loadResourceBundle(channelInfoMixinName, classLoader, locale);

                if (bundle != null) {
                    bundles.add(bundle);
                }
            }
        }

        if (bundles.isEmpty()) {
            return null;
        } else if (bundles.size() == 1) {
            return bundles.get(0);
        }

        ResourceBundle[] bundlesArray = bundles.toArray(new ResourceBundle[bundles.size()]);
        // mixins can override preceding ones.
        ArrayUtils.reverse(bundlesArray);

        return new CompositeResourceBundle(bundlesArray);
    }

    private Class<?> getClass(final ClassLoader classLoader, final String className) {
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException cnfe) {
            log.error(String.format("The class %s couldn't be loaded. The channel info class hierarchy could be " +
                    "incomplete", className),cnfe);
            return null;
        }
    }

    private Stream<Class<?>> appendInterfaces(final Class<?> clazz) {
        final List<Class<?>> classes = new ArrayList<>();
        final Stack<Class<?>> pendingClassNames = new Stack<>();
        pendingClassNames.push(clazz);

        while (!pendingClassNames.isEmpty()) {
            final Class<?> current = pendingClassNames.pop();
            classes.add(current);
            for (final Class<?> anInterface : current.getInterfaces()) {
                pendingClassNames.push(anInterface);
            }
        }
        return classes.stream();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<HstPropertyDefinition> getPropertyDefinitions(final Channel channel) {
        try {
            if (channel.getChannelInfoClassName() != null) {
                Class<? extends ChannelInfo> channelInfoClass = getChannelInfoClass(channel);

                if (channelInfoClass != null) {
                    final List<Class<? extends ChannelInfo>> channelInfoMixins = getChannelInfoMixins(channel);
                    return ChannelInfoClassProcessor.getProperties(channelInfoClass,
                            channelInfoMixins.toArray(new Class[channelInfoMixins.size()]));
                }
            }
        } catch (ChannelException ex) {
            log.warn("Could not load properties", ex);
        }

        return Collections.emptyList();
    }

    @Override
    public List<HstPropertyDefinition> getPropertyDefinitions(final String hostGroup, final String channelId) {
         return getPropertyDefinitions(getChannelById(hostGroup, channelId));
    }

    @Override
    public HstComponentRegistry getComponentRegistry() {
        return componentRegistry;
    }

    public void setComponentRegistry(final HstComponentRegistry hstComponentRegistry) {
        this.componentRegistry = hstComponentRegistry;
    }

    private void loadBluePrints(final HstNode rootConfigNode) {
        HstNode blueprintsNode = rootConfigNode.getNode(HstNodeTypes.NODENAME_HST_BLUEPRINTS);
        if (blueprintsNode != null) {
            for (HstNode blueprintNode : blueprintsNode.getNodes()) {
                try {
                    blueprints.put(blueprintNode.getName(), BlueprintHandler.buildBlueprint(blueprintNode, contextPath));
                } catch (ModelLoadingException e) {
                    log.error("Cannot load blueprint '{}' :", blueprintNode.getValueProvider().getPath(), e);
                }
            }
        }
    }

    private void loadBluePrints(HstNodeLoadingCache hstNodeLoadingCache) {
        final HstNode rootConfigNode = hstNodeLoadingCache.getNode(hstNodeLoadingCache.getRootPath());
        bluePrintsPrototypeChecked = false;
        loadBluePrints(rootConfigNode);
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

    private ResourceBundle loadResourceBundle(final String channelInfoClassName,
                                              final ClassLoader classLoader, final Locale locale) {
        if (channelInfoClassName != null) {
            final LocalizationService localizationService = HippoServiceRegistry.getService(LocalizationService.class);

            if (localizationService != null) {
                final String bundleName = CHANNEL_PARAMETERS_TRANSLATION_LOCATION + "." + channelInfoClassName;
                final org.onehippo.repository.l10n.ResourceBundle repositoryResourceBundle =
                        localizationService.getResourceBundle(bundleName, locale);

                if (repositoryResourceBundle != null) {
                    return repositoryResourceBundle.toJavaResourceBundle();
                }
            }

            try {
                return ResourceBundle.getBundle(channelInfoClassName, locale, classLoader);
            } catch (MissingResourceException e) {
                log.info("Could not load repository or Java resource bundle for class '{}' and locale '{}', using " +
                        "untranslated labels for channel properties.", channelInfoClassName, locale);
            }
        }

        return null;
    }

}
