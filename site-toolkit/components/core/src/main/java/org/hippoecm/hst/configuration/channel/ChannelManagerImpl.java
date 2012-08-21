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
package org.hippoecm.hst.configuration.channel;

import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.channel.ChannelException.Type;
import org.hippoecm.hst.configuration.channel.ChannelManagerEventListenerException.Status;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.MutableMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.container.CmsJcrSessionThreadLocal;
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelManagerImpl implements MutableChannelManager {

    private static final String DEFAULT_HST_ROOT_PATH = "/hst:hst";
    private static final String DEFAULT_HST_SITES = "hst:sites";
    private static final String DEFAULT_CONTENT_ROOT = "/content/documents";

    static final Logger log = LoggerFactory.getLogger(ChannelManagerImpl.class.getName());

    private String rootPath = DEFAULT_HST_ROOT_PATH;
    private String hostGroup = null;
    private String sites = DEFAULT_HST_SITES;

    private Map<String, Blueprint> blueprints;
    private Map<String, Channel> channels;
    private Repository repository;
    private String channelsRoot = DEFAULT_HST_ROOT_PATH + "/" + HstNodeTypes.NODENAME_HST_CHANNELS + "/";
    private String contentRoot = DEFAULT_CONTENT_ROOT;

    /**
     * The codec which is used for the channel ID
     */
    private StringCodec channelIdCodec = new StringCodecFactory.UriEncoding();

    private List<ChannelManagerEventListener> channelManagerEventListeners = Collections.synchronizedList(
            new ArrayList<ChannelManagerEventListener>());

    public ChannelManagerImpl() {
    }

    public void setRepository(final Repository repository) {
        this.repository = repository;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath.trim();
        channelsRoot = rootPath + "/" + HstNodeTypes.NODENAME_HST_CHANNELS + "/";
    }

    public void setContentRoot(final String contentRoot) {
        this.contentRoot = contentRoot.trim();
    }

    public void addChannelManagerEventListeners(ChannelManagerEventListener... listeners) {
        if (listeners == null) {
            return;
        }

        for (ChannelManagerEventListener listener : listeners) {
            channelManagerEventListeners.add(listener);
        }
    }

    public void removeChannelManagerEventListeners(ChannelManagerEventListener... listeners) {
        if (listeners == null) {
            return;
        }

        for (ChannelManagerEventListener listener : listeners) {
            channelManagerEventListeners.remove(listener);
        }
    }

    private void loadBlueprints(final Node configNode) throws RepositoryException {
        if (configNode.hasNode(HstNodeTypes.NODENAME_HST_BLUEPRINTS)) {
            Node blueprintsNode = configNode.getNode(HstNodeTypes.NODENAME_HST_BLUEPRINTS);
            NodeIterator blueprintIterator = blueprintsNode.getNodes();
            while (blueprintIterator.hasNext()) {
                Node blueprintNode = blueprintIterator.nextNode();
                blueprints.put(blueprintNode.getName(), BlueprintHandler.buildBlueprint(blueprintNode));
            }
        }
    }

    private void loadChannels(final Node configNode) throws RepositoryException {
        if (configNode.hasNode(HstNodeTypes.NODENAME_HST_CHANNELS)) {
            Node channelsFolder = configNode.getNode(HstNodeTypes.NODENAME_HST_CHANNELS);
            NodeIterator rootChannelNodes = channelsFolder.getNodes();
            while (rootChannelNodes.hasNext()) {
                Node hgNode = rootChannelNodes.nextNode();
                loadChannel(hgNode);
            }
        } else {
            log.warn("Cannot load channels because node '{}' does not exist",
                     configNode.getPath() + "/" + HstNodeTypes.NODENAME_HST_CHANNELS);
        }
    }

    private void loadChannel(Node currNode) throws RepositoryException {
        Channel channel = ChannelPropertyMapper.readChannel(currNode);
        channels.put(channel.getId(), channel);
    }

    private void loadFromMount(MutableMount mount) {
        // we are only interested in Mount's that have isMapped = true and that 
        // are live mounts: We do not display 'preview' Mounts in cms: instead, a 
        // live mount decorated as preview are shown
        if (!mount.isMapped() || mount.isPreview()) {
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
                    new Object[] { channelPath, rootPath, mount.getName() });

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
            channel.setHstPreviewMountPoint(mountPoint + "-preview");
            channel.setContentRoot(mount.getCanonicalContentPath());
            String configurationPath = mount.getHstSite().getConfigurationPath();
            if (configurationPath != null) {
                channel.setHstConfigPath(configurationPath);
            }
        }

        channel.setLockedBy(mount.getLockedBy());
        final Calendar lockedOn = mount.getLockedOn();
        if (lockedOn != null) {
            channel.setLockedOn(lockedOn.getTimeInMillis());
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

    /**
     * Make sure that HST manager is initialized.
     *
     * @throws ChannelException when initializing the HST manager failed
     */
    void load() throws ChannelException {
        if (channels == null) {
            HstManager manager = HstServices.getComponentManager().getComponent(HstManager.class.getName());

            try {
                manager.getVirtualHosts();
            } catch (RepositoryNotAvailableException e) {
                throw new ChannelException("could not build channels");
            }
            if (channels == null) {
                throw new ChannelException("channels could not be loaded");
            }
        }
    }

    public synchronized void load(VirtualHosts virtualHosts, Session session) throws RepositoryException {
        Node configNode = session.getNode(rootPath);

        blueprints = new HashMap<String, Blueprint>();
        loadBlueprints(configNode);

        channels = new HashMap<String, Channel>();

        hostGroup = virtualHosts.getChannelManagerHostGroupName();
        sites = virtualHosts.getChannelManagerSitesName();

        List<Mount> mounts = Collections.emptyList();
        if (hostGroup == null) {
            log.warn("Cannot load the Channel Manager because no host group configured on hst:hosts node");
        } else if (!virtualHosts.getHostGroupNames().contains(hostGroup)) {
            log.warn("Configured channel manager host group name {} does not exist", hostGroup);
        } else {
            // in channel manager only the mounts for at most ONE single hostGroup are shown
            mounts = virtualHosts.getMountsByHostGroup(hostGroup);
            if (mounts.size() == 0) {
                log.warn("No mounts found in host group {}.", hostGroup);
            }
        }

        // load all the channels, even if they are not used by the current hostGroup
        loadChannels(configNode);

        for (Mount mount : mounts) {
            if (mount instanceof MutableMount) {
                loadFromMount((MutableMount) mount);
            }
        }

        // for ALL the mounts in ALL the host groups, set the channel Info if available
        for (String hostGroupName : virtualHosts.getHostGroupNames()) {
            for (Mount mount : virtualHosts.getMountsByHostGroup(hostGroupName)) {
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
    }

    protected Session getSession() throws RepositoryException {
        final Session session = CmsJcrSessionThreadLocal.getJcrSession();
        if (session == null) {
            log.debug("Could not find a JCR session object instance when expected to have one already instantiated");
            throw new IllegalStateException("Could not find a JCR session object instance when expected to have one already instantiated");
        }

        return session;
    }

    public synchronized Channel getChannelByJcrPath(String jcrPath) throws ChannelException {
        load();
        if (StringUtils.isBlank(jcrPath) || !jcrPath.startsWith(channelsRoot)) {
            throw new ChannelException("Expected a valid channel JCR path which should start with '" + channelsRoot + "', but got '" + jcrPath + "' instead");
        }

        final String channelId = jcrPath.substring(channelsRoot.length());
        return channels.get(channelId);
    }

    public synchronized Channel getChannelById(String id) throws ChannelException {
        load();
        if (StringUtils.isBlank(id)) {
            throw new ChannelException("Expected a channel id, but got '" + id + "' instead");
        }

        return channels.get(id);
    }

    // PUBLIC interface; all synchronised to guarantee consistent state

    @Override
    public synchronized Map<String, Channel> getChannels() throws ChannelException {
        load();

        Map<String, Channel> result = new HashMap<String, Channel>();
        for (Map.Entry<String, Channel> entry : channels.entrySet()) {
            result.put(entry.getKey(), new Channel(entry.getValue()));
        }
        return result;
    }

    @Override
    public synchronized String persist(final String blueprintId, Channel channel) throws ChannelException {
        if (!blueprints.containsKey(blueprintId)) {
            throw new ChannelException("Blueprint id " + blueprintId + " is not valid");
        }

        Blueprint blueprint = blueprints.get(blueprintId);

        try {
            final Session session = getSession();
            Node configNode = session.getNode(rootPath);
            String channelId = createUniqueChannelId(channel.getName(), session);
            createChannel(configNode, blueprint, session, channelId, channel);

            ChannelManagerEvent event = new ChannelManagerEventImpl(blueprint, channelId, channel, configNode);
            for (ChannelManagerEventListener listener : channelManagerEventListeners) {
                try {
                    listener.channelCreated(event);
                } catch (ChannelManagerEventListenerException e) {
                    if (e.getStatus() == Status.STOP_CHANNEL_PROCESSING) {
                        session.refresh(false);
                        throw new ChannelException(e.getMessage(), e, Type.STOPPED_BY_LISTENER,
                                                   "Channel creation stopped by listener '" + listener.getClass().getName() + "'");
                    } else {
                        log.warn(
                                "Channel created event listener, " + listener + ", failed to handle the event. Continue channel processing",
                                e);
                    }
                } catch (Exception listenerEx) {
                    log.warn("Channel created event listener, " + listener + ", failed to handle the event",
                             listenerEx);
                }
            }

            channels = null;

            session.save();

            return channelId;
        } catch (RepositoryException e) {
            throw new ChannelException("Unable to save channel to the repository", e);
        }

    }

    /**
     * Creates a unique ID for a channel. The ID can safely be used as a new JCR node name in the hst:channels,
     * hst:sites, and hst:configurations configuration.
     *
     * @param channelName the name of the channel
     * @param session     JCR session to use for sanity checks of node names
     * @return a unique channel ID based on the given channel name
     * @throws ChannelException
     */
    protected String createUniqueChannelId(String channelName, Session session) throws ChannelException {
        if (StringUtils.isBlank(channelName)) {
            throw new ChannelException("Cannot create channel ID: channel name is blank");
        }
        try {
            String channelId = channelIdCodec.encode(channelName);
            int retries = 0;
            Node channelsNode = session.getNode(channelsRoot);
            Node rootNode = session.getNode(rootPath);
            Node sitesNode = rootNode.getNode(sites);
            Node configurationsNode = rootNode.getNode(HstNodeTypes.NODENAME_HST_CONFIGURATIONS);

            while (channelsNode.hasNode(channelId) || sitesNode.hasNode(channelId) || configurationsNode.hasNode(
                    channelId)) {
                retries += 1;
                StringBuilder builder = new StringBuilder(channelName);
                builder.append('-');
                builder.append(retries);
                channelId = channelIdCodec.encode(builder.toString());
            }

            return channelId;
        } catch (RepositoryException e) {
            throw new ChannelException("Cannot create channel ID for channelName '" + channelName + "'", e);
        }
    }

    @Override
    public synchronized void save(final Channel channel) throws ChannelException {
        load();
        if (!channels.containsKey(channel.getId())) {
            throw new ChannelException("No channel with id " + channel.getId() + " was found");
        }

        try {
            final Session session = getSession();
            Node configNode = session.getNode(rootPath);
            updateChannel(configNode, channel);

            ChannelManagerEvent event = new ChannelManagerEventImpl(null, null, channel, configNode);
            for (ChannelManagerEventListener listener : channelManagerEventListeners) {
                try {
                    listener.channelUpdated(event);
                } catch (ChannelManagerEventListenerException e) {
                    if (e.getStatus() == Status.STOP_CHANNEL_PROCESSING) {
                        session.refresh(false);
                        throw new ChannelException(e.getMessage(), e, Type.STOPPED_BY_LISTENER,
                                                   "Channel '" + channel.getId() + "' update stopped by listener '" + listener.getClass().getName() + "'");
                    } else {
                        log.warn(
                                "Channel created event listener, " + listener + ", failed to handle the event. Continue channel processing",
                                e);
                    }
                } catch (Exception listenerEx) {
                    log.error("Channel updated event listener, " + listener + ", failed to handle the event",
                              listenerEx);
                }
            }

            channels = null;

            session.save();
        } catch (RepositoryException e) {
            throw new ChannelException("Unable to save channel to the repository", e);
        }

    }

    @Override
    public synchronized List<Blueprint> getBlueprints() throws ChannelException {
        load();
        return new ArrayList<Blueprint>(blueprints.values());
    }

    @Override
    public synchronized Blueprint getBlueprint(final String id) throws ChannelException {
        load();
        if (!blueprints.containsKey(id)) {
            throw new ChannelException("Blueprint " + id + " does not exist");
        }
        return blueprints.get(id);
    }

    @Override
    public Class<? extends ChannelInfo> getChannelInfoClass(Channel channel) throws ChannelException {
        String channelInfoClassName = channel.getChannelInfoClassName();
        if (channelInfoClassName == null) {
            log.debug("No channelInfoClassName defined. Return just the ChannelInfo interface class");
            return ChannelInfo.class;
        }
        try {
            return (Class<? extends ChannelInfo>) ChannelPropertyMapper.class.getClassLoader().loadClass(
                    channelInfoClassName);
        } catch (ClassNotFoundException cnfe) {
            throw new ChannelException("Configured class " + channelInfoClassName + " was not found", cnfe);
        } catch (ClassCastException cce) {
            throw new ChannelException("Configured class " + channelInfoClassName + " does not extend ChannelInfo",
                                       cce);
        }
    }

    @Override
    public <T extends ChannelInfo> T getChannelInfo(Channel channel) throws ChannelException {
        Class<? extends ChannelInfo> channelInfoClass = getChannelInfoClass(channel);
        return (T) ChannelUtils.getChannelInfo(channel.getProperties(), channelInfoClass);
    }

    @Override
    public List<HstPropertyDefinition> getPropertyDefinitions(Channel channel) {
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
    public List<HstPropertyDefinition> getPropertyDefinitions(String channelId) {
        try {
            return getPropertyDefinitions(getChannelById(channelId));
        } catch (ChannelException ex) {
            log.warn("Could not retieve channel of id '" + channelId + "'", ex);
        }

        return Collections.emptyList();
    }

    @Override
    public ResourceBundle getResourceBundle(Channel channel, Locale locale) {
        String channelInfoClassName = channel.getChannelInfoClassName();
        if (channelInfoClassName != null) {
            return ResourceBundle.getBundle(channelInfoClassName, locale);
        }
        return null;
    }

    @Override
    public synchronized boolean canUserModifyChannels() {
        try {
            final Session session = getSession();
            return session.hasPermission(rootPath + "/" + HstNodeTypes.NODENAME_HST_CHANNELS + "/accesstest", Session.ACTION_ADD_NODE);
        } catch (RepositoryException e) {
            log.error("Repository error when determining channel manager access", e);
        }

        return false;
    }

    /* (non-Javadoc)
    * @see org.hippoecm.hst.configuration.channel.ChannelManager.getChannelInfoClass(String id)
    */
    @Override
    public Class<? extends ChannelInfo> getChannelInfoClass(String id) throws ChannelException {
        return getChannelInfoClass(getChannelById(id));
    }

    public synchronized void invalidate() {
        channels = null;
        blueprints = null;
    }

    // private - internal - methods

    private void createChannel(Node configRoot, Blueprint blueprint, Session session, final String channelId, final Channel channel) throws ChannelException, RepositoryException {
        // Create virtual host
        final URI channelUri = getChannelUri(channel);
        final Node virtualHost = getOrCreateVirtualHost(configRoot, channelUri.getHost());

        // Create channel
        copyOrCreateChannelNode(configRoot, channelId, channel);

        // Create or reuse HST configuration
        final Node blueprintNode = BlueprintHandler.getNode(session, blueprint);
        final String hstConfigPath = reuseOrCopyConfiguration(session, configRoot, blueprintNode, channelId);
        channel.setHstConfigPath(hstConfigPath);

        // Determine the content path to use in the 'site' node. If the blueprint has a content prototype, we will use
        // '/' for now to ensure valid Hippo mirrors, and fill in the created path *after* creating all HST
        // configuration (due to the Session.save() in the workflow action, all HST configuration should be created
        // first to avoid a partial save). Otherwise, we can directly use the existing content path in the channel.
        Session jcrSession = configRoot.getSession();
        Node contentRoot = jcrSession.getRootNode();
        if (!blueprint.getHasContentPrototype()) {
            String channelContentRoot = channel.getContentRoot();
            if (StringUtils.isNotEmpty(channelContentRoot) && jcrSession.nodeExists(channelContentRoot)) {
                contentRoot = jcrSession.getNode(channelContentRoot);
            } else {
                throw new ChannelException(
                        "Blueprint '" + blueprint.getId() + "' does not have a content prototype, and channel '" + channelId + "' refers to a non-existing content root: '" + channelContentRoot + "'");
            }
        }

        // Create live and preview site nodes. We don't need to set the property 'hst:configurationpath', as the
        // HST derives it by convention. If the site node in the blueprint contains an explicit configuration path,
        // that property will be copied and used instead of the derived one.
        final Node sitesNode = configRoot.getNode(sites);
        final Node liveSiteNode = copyOrCreateSiteNode(blueprintNode, sitesNode, channelId, "live", contentRoot);
        final Node previewSiteNode = copyOrCreateSiteNode(blueprintNode, sitesNode, channelId + "-preview", "preview",
                                                          contentRoot);

        final String mountPointPath = liveSiteNode.getPath();
        channel.setHstMountPoint(mountPointPath);

        final String previewMountPointPath = previewSiteNode.getPath();
        channel.setHstPreviewMountPoint(previewMountPointPath);

        // Create mount
        Node mount = createMountNode(virtualHost, blueprintNode, channelUri.getPath());
        mount.setProperty(HstNodeTypes.MOUNT_PROPERTY_CHANNELPATH, channelsRoot + channelId);
        mount.setProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT, mountPointPath);
        final String locale = channel.getLocale();
        if (locale != null) {
            mount.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCALE, locale);
        }

        // Create content if the blueprint contains a content prototype. The path of the created content node has to
        // be set on the HST site nodes.
        if (blueprint.getHasContentPrototype()) {
            final Node contentRootNode = createContent(blueprint, session, channelId, channel);

            final Node liveContentMirror = liveSiteNode.getNode(HstNodeTypes.NODENAME_HST_CONTENTNODE);
            final Node previewContentMirror = previewSiteNode.getNode(HstNodeTypes.NODENAME_HST_CONTENTNODE);

            liveContentMirror.setProperty(HippoNodeType.HIPPO_DOCBASE, contentRootNode.getIdentifier());
            previewContentMirror.setProperty(HippoNodeType.HIPPO_DOCBASE, contentRootNode.getIdentifier());
        }
    }

    private void copyOrCreateChannelNode(final Node configRoot, final String channelId, final Channel channel) throws RepositoryException {
        if (!configRoot.hasNode(HstNodeTypes.NODENAME_HST_CHANNELS)) {
            configRoot.addNode(HstNodeTypes.NODENAME_HST_CHANNELS, HstNodeTypes.NODETYPE_HST_CHANNELS);
        }
        Node channelNode = configRoot.getNode(HstNodeTypes.NODENAME_HST_CHANNELS).addNode(channelId,
                                                                                          HstNodeTypes.NODETYPE_HST_CHANNEL);
        ChannelPropertyMapper.saveChannel(channelNode, channel);
    }

    private String reuseOrCopyConfiguration(final Session session, final Node configRoot, final Node blueprintNode, final String channelId) throws ChannelException, RepositoryException {
        // first try to copy existing blueprint configuration
        if (blueprintNode.hasNode(HstNodeTypes.NODENAME_HST_CONFIGURATION)) {
            Node blueprintConfiguration = blueprintNode.getNode(HstNodeTypes.NODENAME_HST_CONFIGURATION);
            Node hstConfigurations = configRoot.getNode(HstNodeTypes.NODENAME_HST_CONFIGURATIONS);
            Node configuration = copyNodes(blueprintConfiguration, hstConfigurations, channelId);
            return configuration.getPath();
        }

        // next, try to reuse the configuration path specified in the hst:site node of the blueprint
        if (blueprintNode.hasNode(HstNodeTypes.NODENAME_HST_SITE)) {
            Node siteNode = blueprintNode.getNode(HstNodeTypes.NODENAME_HST_SITE);
            if (siteNode.hasProperty(HstNodeTypes.SITE_CONFIGURATIONPATH)) {
                String configurationPath = siteNode.getProperty(HstNodeTypes.SITE_CONFIGURATIONPATH).getString();
                if (!session.nodeExists(configurationPath)) {
                    throw new ChannelException(
                            "Blueprint '" + blueprintNode.getPath() + "' does not have an hst:configuration node, and its hst:site node points to a non-existing node: '" + configurationPath + "'");
                }
                return configurationPath;
            }
        }

        // no clue which configuration to use
        throw new ChannelException(
                "Blueprint '" + blueprintNode.getPath() + "' does not specify any hst:configuration to use. " +
                        "Either include an hst:configuration node to copy, or include an hst:site node with an existing hst:configurationpath property.");
    }

    private Node createMountNode(Node virtualHost, final Node blueprintNode, final String mountPath) throws ChannelException, RepositoryException {
        ArrayList<String> mountPathElements = new ArrayList<String>();
        mountPathElements.add(HstNodeTypes.MOUNT_HST_ROOTNAME);
        mountPathElements.addAll(Arrays.asList(StringUtils.split(mountPath, '/')));

        Node mount = virtualHost;

        for (int i = 0; i < mountPathElements.size() - 1; i++) {
            String mountPathElement = mountPathElements.get(i);
            if (mount.hasNode(mountPathElement)) {
                mount = mount.getNode(mountPathElement);
            } else {
                throw mountNotFoundException(mount.getPath() + "/" + mountPathElement);
            }
        }

        String lastMountPathElementName = mountPathElements.get(mountPathElements.size() - 1);

        if (mount.hasNode(lastMountPathElementName)) {
            throw mountExistsException(mount.getPath() + '/' + lastMountPathElementName);
        }

        if (blueprintNode.hasNode(HstNodeTypes.NODENAME_HST_MOUNT)) {
            mount = copyNodes(blueprintNode.getNode(HstNodeTypes.NODENAME_HST_MOUNT), mount, lastMountPathElementName);
        } else {
            mount = mount.addNode(lastMountPathElementName, HstNodeTypes.NODETYPE_HST_MOUNT);
        }

        return mount;
    }

    private Node getOrCreateVirtualHost(final Node configRoot, final String hostName) throws RepositoryException {
        final String[] elements = hostName.split("[.]");

        Node mount = configRoot.getNode(HstNodeTypes.NODENAME_HST_HOSTS + "/" + hostGroup);

        for (int i = elements.length - 1; i >= 0; i--) {
            mount = getOrAddNode(mount, elements[i], HstNodeTypes.NODETYPE_HST_VIRTUALHOST);
        }

        return mount;
    }

    private Node copyOrCreateSiteNode(final Node blueprintNode, final Node sitesNode, final String siteNodeName, final String hippoAvailability, final Node contentRoot) throws RepositoryException {

        if (blueprintNode.hasNode(HstNodeTypes.NODENAME_HST_SITE)) {
            return copySiteNode(blueprintNode, sitesNode, siteNodeName, contentRoot, hippoAvailability);
        } else {
            return createSiteNode(sitesNode, siteNodeName, contentRoot, hippoAvailability);
        }
    }

    private Node copySiteNode(final Node blueprintNode, final Node sitesNode, final String siteNodeName, final Node contentRoot, final String hippoAvailability) throws RepositoryException {
        Node blueprintSiteNode = blueprintNode.getNode(HstNodeTypes.NODENAME_HST_SITE);

        log.debug("Copying site node '{}' to '{}/{}'", new Object[] {blueprintSiteNode.getPath(), sitesNode.getPath(), siteNodeName});

        Node siteNode = copyNodes(blueprintSiteNode, sitesNode, siteNodeName);

        Node contentNode = siteNode.getNode(HstNodeTypes.NODENAME_HST_CONTENTNODE);
        contentNode.setProperty(HippoNodeType.HIPPO_VALUES, new String[]{hippoAvailability});
        contentNode.setProperty(HippoNodeType.HIPPO_DOCBASE, contentRoot.getIdentifier());

        return siteNode;
    }

    private Node createSiteNode(final Node sitesNode, final String siteNodeName, final Node contentRoot, final String hippoAvailability) throws RepositoryException {
        log.debug("Creating site node '{}/{}'; content root='{}',hippo:availability='{}'", new Object[] {sitesNode.getPath(), siteNodeName, contentRoot.getPath(), hippoAvailability});

        final Node siteNode = sitesNode.addNode(siteNodeName, HstNodeTypes.NODETYPE_HST_SITE);

        final Node contentNode = siteNode.addNode(HstNodeTypes.NODENAME_HST_CONTENTNODE, HippoNodeType.NT_FACETSELECT);
        contentNode.setProperty(HippoNodeType.HIPPO_FACETS, new String[]{"hippo:availability"});
        contentNode.setProperty(HippoNodeType.HIPPO_VALUES, new String[]{hippoAvailability});
        contentNode.setProperty(HippoNodeType.HIPPO_MODES, new String[]{"single"});
        contentNode.setProperty(HippoNodeType.HIPPO_DOCBASE, contentRoot.getIdentifier());

        return siteNode;
    }

    private Node createContent(final Blueprint blueprint, final Session session, final String channelId, final Channel channel) throws RepositoryException, ChannelException {
        String blueprintContentPath = blueprint.getContentRoot();
        if (blueprintContentPath == null) {
            blueprintContentPath = contentRoot;
        }

        log.debug("Creating new subsite content from blueprint '{}' under '{}'", blueprint.getId(),
                  blueprintContentPath);

        FolderWorkflow fw = (FolderWorkflow) getWorkflow("subsite", session.getNode(blueprintContentPath));
        if (fw == null) {
            throw cannotCreateContent(blueprintContentPath, null);
        }
        try {
            String contentRootPath = fw.add("new-subsite", blueprint.getId(), channelId);
            session.refresh(true);


            final Node contentRootNode = session.getNode(contentRootPath);
            DefaultWorkflow defaultWorkflow = (DefaultWorkflow) getWorkflow("core", contentRootNode);
            defaultWorkflow.localizeName(channel.getName());

            session.refresh(true);

            return contentRootNode;
        } catch (WorkflowException e) {
            throw cannotCreateContent(blueprintContentPath, e);
        } catch (RemoteException e) {
            throw cannotCreateContent(blueprintContentPath, e);
        }
    }

    private static Node getOrAddNode(Node parent, String nodeName, String nodeType) throws RepositoryException {
        if (parent.hasNode(nodeName)) {
            return parent.getNode(nodeName);
        } else {
            return parent.addNode(nodeName, nodeType);
        }
    }

    static Node copyNodes(Node source, Node parent, String name) throws RepositoryException {
        Node clone = parent.addNode(name, source.getPrimaryNodeType().getName());
        for (NodeType mixin : source.getMixinNodeTypes()) {
            clone.addMixin(mixin.getName());
        }
        for (PropertyIterator pi = source.getProperties(); pi.hasNext(); ) {
            Property prop = pi.nextProperty();
            if (prop.getDefinition().isProtected()) {
                continue;
            }
            if (prop.isMultiple()) {
                clone.setProperty(prop.getName(), prop.getValues());
            } else {
                clone.setProperty(prop.getName(), prop.getValue());
            }
        }
        for (NodeIterator ni = source.getNodes(); ni.hasNext(); ) {
            Node node = ni.nextNode();
            if (isVirtual(node)) {
                continue;
            }

            copyNodes(node, clone, node.getName());
        }
        return clone;
    }

    public Workflow getWorkflow(String category, Node node) throws RepositoryException {
        Workspace workspace = node.getSession().getWorkspace();

        ClassLoader workspaceClassloader = workspace.getClass().getClassLoader();
        ClassLoader currentClassloader = Thread.currentThread().getContextClassLoader();

        try {
            if (workspaceClassloader != currentClassloader) {
                Thread.currentThread().setContextClassLoader(workspaceClassloader);
            }

            WorkflowManager wfm = ((HippoWorkspace) workspace).getWorkflowManager();
            return wfm.getWorkflow(category, node);
        } catch (RepositoryException e) {
            throw e;
        } catch (Exception e) {
            // Other exception which are not handled properly in the repository (we cannot do better here then just log them)
            if (log.isDebugEnabled()) {
                log.warn("Exception in workflow", e);
            } else {
                log.warn("Exception in workflow: {}", e.toString());
            }
        } finally {
            if (workspaceClassloader != currentClassloader) {
                Thread.currentThread().setContextClassLoader(currentClassloader);
            }
        }

        return null;
    }

    private static boolean isVirtual(final Node node) throws RepositoryException {
        // skip virtual nodes
        if (node instanceof HippoNode) {
            HippoNode hn = (HippoNode) node;
            try {
                Node canonicalNode = hn.getCanonicalNode();
                if (canonicalNode == null) {
                    return true;
                }
                if (!canonicalNode.isSame(hn)) {
                    return true;
                }
            } catch (ItemNotFoundException infe) {
                return true;
            }
        }
        return false;
    }

    private void updateChannel(Node configRoot, final Channel channel) throws ChannelException, RepositoryException {
        URI channelUri = getChannelUri(channel);
        Node virtualHost = getOrCreateVirtualHost(configRoot, channelUri.getHost());

        // resolve mount
        Node mount;
        if (virtualHost.hasNode(HstNodeTypes.MOUNT_HST_ROOTNAME)) {
            mount = virtualHost.getNode(HstNodeTypes.MOUNT_HST_ROOTNAME);
        } else {
            throw mountNotFoundException(virtualHost.getPath() + "/" + HstNodeTypes.MOUNT_HST_ROOTNAME);
        }
        final String mountPath = channel.getMountPath();
        if (mountPath != null) {
            for (String mountPathElement : StringUtils.split(mountPath, '/')) {
                if (mount.hasNode(mountPathElement)) {
                    mount = mount.getNode(mountPathElement);
                } else {
                    throw mountNotFoundException(mount.getPath() + "/" + mountPathElement);
                }
            }
        }

        ChannelPropertyMapper.saveChannel(
                configRoot.getNode(HstNodeTypes.NODENAME_HST_CHANNELS + "/" + channel.getId()), channel);
    }

    /**
     * Returns the channel's URL is a URI object. The returned URI has a supported scheme and a host name.
     *
     * @param channel the channel
     * @return the validated URI of the channel
     * @throws ChannelException if the channel URL is not a valid URI, does not have a supported scheme or does not
     *                          contain a host name.
     */
    private URI getChannelUri(final Channel channel) throws ChannelException {
        URI uri;

        try {
            uri = new URI(channel.getUrl());
        } catch (URISyntaxException e) {
            throw new ChannelException("Invalid channel URL: '" + channel.getUrl() + "'");
        }

        if (!"http".equals(uri.getScheme())) {
            throw new ChannelException(
                    "Illegal channel URL scheme: '" + uri.getScheme() + "'. Only 'http' is currently supported");
        }

        if (StringUtils.isBlank(uri.getHost())) {
            throw new ChannelException("Channel URL '" + uri + "' does not contain a host name");
        }

        return uri;
    }

    /**
     * Static factory method for a ChannelException of type {@link ChannelException.Type#MOUNT_NOT_FOUND}.
     *
     * @param missingMount the absolute JCR path of the missing mount
     * @return a ChannelException of type {@link ChannelException.Type#MOUNT_NOT_FOUND}.
     */
    static ChannelException mountNotFoundException(String missingMount) {
        return new ChannelException("Mount not found: " + missingMount, ChannelException.Type.MOUNT_NOT_FOUND,
                                    missingMount);
    }

    /**
     * Static factory method for a ChannelException of type {@link ChannelException.Type#MOUNT_EXISTS}.
     *
     * @param existingMount the absolute JCR path of the mount that already exists
     * @return a ChannelException of type {@link ChannelException.Type#MOUNT_EXISTS}.
     */
    static ChannelException mountExistsException(String existingMount) {
        return new ChannelException("Mount already exists: " + existingMount, ChannelException.Type.MOUNT_EXISTS,
                                    existingMount);
    }

    /**
     * Static factory method for a ChannelException of type {@link ChannelException.Type#CANNOT_CREATE_CONTENT}.
     *
     * @param contentRoot the absolute JCR path of the path at which the content could not be created
     * @return a ChannelException of type {@link ChannelException.Type#CANNOT_CREATE_CONTENT}.
     */
    static ChannelException cannotCreateContent(String contentRoot, Throwable cause) {
        return new ChannelException("Could not create content at '" + contentRoot + "'", cause,
                                    ChannelException.Type.CANNOT_CREATE_CONTENT, contentRoot);
    }

    private static class ChannelManagerEventImpl implements ChannelManagerEvent {

        private Blueprint blueprint;
        private String channelId;
        private Channel channel;
        private Node configRootNode;

        private ChannelManagerEventImpl(Blueprint blueprint, String channelId, Channel channel, Node configRootNode) {
            this.blueprint = blueprint;
            this.channelId = channelId;
            this.channel = channel;
            this.configRootNode = configRootNode;
        }

        public Blueprint getBlueprint() {
            return blueprint;
        }

        public String getChannelId() {
            if (channelId != null) {
                return channelId;
            } else if (channel != null) {
                return channel.getId();
            }

            return null;
        }

        public Channel getChannel() {
            return channel;
        }

        public Node getConfigRootNode() {
            return configRootNode;
        }
    }

}
