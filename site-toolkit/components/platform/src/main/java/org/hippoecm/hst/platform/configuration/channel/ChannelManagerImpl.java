/*
 *  Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.channel;

import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.channel.Blueprint;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.ChannelException.Type;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.configuration.channel.ChannelManagerEvent;
import org.hippoecm.hst.configuration.channel.ChannelManagerEventListener;
import org.hippoecm.hst.configuration.channel.ChannelManagerEventListenerException;
import org.hippoecm.hst.configuration.channel.ChannelManagerEventListenerException.Status;
import org.hippoecm.hst.configuration.channel.ChannelManagerEventListenerRegistry;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.platform.model.HstModel;
import org.hippoecm.hst.platform.model.HstModelImpl;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.hippoecm.hst.util.JcrSessionUtils;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.InetAddresses;

import static org.hippoecm.hst.configuration.HstNodeTypes.CHANNEL_PROPERTY_NAME;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_CHANNEL;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_HOSTS;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CHANNEL;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONFIGURATION;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_WORKSPACE;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITE_CONFIGURATIONPATH;

public class ChannelManagerImpl implements ChannelManager {

    private static final String DEFAULT_HST_SITES = "hst:sites";
    public static final String DEFAULT_CONTENT_ROOT = "/content/documents";

    static final Logger log = LoggerFactory.getLogger(ChannelManagerImpl.class.getName());

    private String sites = DEFAULT_HST_SITES;

    /**
     * The codec which is used for the channel ID
     */
    private final StringCodec CHANNEL_ID_CODEC = new StringCodecFactory.UriEncoding();

    private final HstModelImpl hstModel;
    private final String contentRoot;

    public ChannelManagerImpl(final HstModelImpl hstModel, final String contentRoot) {
        this.hstModel = hstModel;
        this.contentRoot = contentRoot;
    }

    @Override
    public String persist(final Session session, final String blueprintId, Channel channel) throws ChannelException {
        synchronized (hstModel) {
            final Blueprint blueprint = hstModel.getVirtualHosts().getBlueprint(blueprintId);
            if (blueprint == null) {
                throw new ChannelException("Blueprint id " + blueprintId + " is not valid");
            }
            try {
                final Node configNode = session.getNode(hstModel.getConfigurationRootPath());

                final String channelName = createUniqueHstConfigurationName(channel.getName(), session);
                channel.setId(channelName);

                final Node createdContentNode = createChannel(configNode, blueprint, session, channelName, channel);
                final ChannelManagerEvent event = new ChannelManagerEventImpl(blueprint, channel, configNode);
                getChannelManagerEventListeners().forEach(listener -> {
                    try {
                        listener.channelCreated(event);
                    } catch (ChannelManagerEventListenerException e) {
                        if (e.getStatus() == Status.STOP_CHANNEL_PROCESSING) {
                            try {
                                session.refresh(false);
                                if (createdContentNode != null) {
                                    log.info("Removing just created root content node '{}' due ChannelManagerEventListenerException '{}'", createdContentNode.getPath(), e.toString());
                                    createdContentNode.remove();
                                    session.save();
                                }
                            } catch (RepositoryException re) {
                                log.warn("Failed to clean up temporarily created content node.", e);
                            }
                            throw new ChannelException("Channel creation stopped by listener '" + listener.getClass().getName() + "'",
                                    e, Type.STOPPED_BY_LISTENER, e.getMessage());
                        } else {
                            log.warn(
                                    "Channel created event listener, " + listener + ", failed to handle the event. Continue channel processing",
                                    e);
                        }
                    } catch (Exception listenerEx) {
                        log.warn("Channel created event listener, " + listener + ", failed to handle the event",
                                listenerEx);
                    }
                });

                String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode(hstModel.getConfigurationRootPath()), false);
                session.save();
                hstModel.getEventPathsInvalidator().eventPaths(pathsToBeChanged);
                return channelName;
            } catch (RepositoryException e) {
                throw new ChannelException("Unable to save channel to the repository", e);
            }
        }
    }

    /**
     * Creates a unique configuration name for a new hst configuration. The name can safely be used as a new JCR node name
     * in the hst:configurations and hst:sites configuration.
     *
     * @param channelName the name of the channel
     * @param session     JCR session to use for sanity checks of node names
     * @return a unique configuration name based on the given channel name
     * @throws ChannelException
     */
    protected String createUniqueHstConfigurationName(String channelName, Session session) throws ChannelException {

        if (StringUtils.isBlank(channelName)) {
            throw new ChannelException("Cannot create channel ID: channel name is blank");
        }
        try {
            String encodedChannelName = CHANNEL_ID_CODEC.encode(channelName);

            int retries = 0;

            final Set<String> uniqueChannelNames = collectChannelAndSiteNames(session);
            while (uniqueChannelNames.contains(encodedChannelName)) {
                retries += 1;
                final String builder = channelName + '-' + retries;
                encodedChannelName = CHANNEL_ID_CODEC.encode(builder);
            }

            return encodedChannelName;
        } catch (RepositoryException e) {
            throw new ChannelException("Cannot create channel ID for channelName '" + channelName + "'", e);
        }
    }

    private Set<String> collectChannelAndSiteNames(final Session session) throws RepositoryException {
        final HstModelRegistry modelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);

        final Set<String> uniqueChannelNames = new HashSet<>();

        for (HstModel model : modelRegistry.getHstModels()) {
            final String hstRoot = ((HstModelImpl) model).getConfigurationRootPath();
            Node rootNode = session.getNode(hstRoot);

            Node sitesNode = rootNode.getNode(sites);
            Node configurationsNode = rootNode.getNode(HstNodeTypes.NODENAME_HST_CONFIGURATIONS);

            for (Node channelNode : new NodeIterable(configurationsNode.getNodes())) {
                uniqueChannelNames.add(channelNode.getName());
            }

            for (Node siteNode : new NodeIterable(sitesNode.getNodes())) {
                uniqueChannelNames.add(siteNode.getName());
            }
        }
        return uniqueChannelNames;
    }

    @Override
    public void save(final Session session, final String hostGroupName, final Channel channel) throws ChannelException {
        synchronized (hstModel) {
            try {
                // TODO is hostGroupName not available on the Channel object already??? IF so, remove from method
                Node configNode = session.getNode(hstModel.getConfigurationRootPath());
                updateChannel(configNode, hostGroupName, channel);

                ChannelManagerEvent event = new ChannelManagerEventImpl(null, channel, configNode);
                getChannelManagerEventListeners().forEach(listener -> {
                    try {
                        listener.channelUpdated(event);
                    } catch (ChannelManagerEventListenerException e) {
                        if (e.getStatus() == Status.STOP_CHANNEL_PROCESSING) {
                            try {
                                session.refresh(false);
                            } catch (RepositoryException re) {
                                log.warn("Failed to refresh session.", e);
                            }
                            throw new ChannelException("Channel '" + channel.getId() + "' update stopped by listener '" + listener.getClass().getName() + "'",
                                    e, Type.STOPPED_BY_LISTENER, e.getMessage());
                        } else {
                            log.warn(
                                    "Channel created event listener, " + listener + ", failed to handle the event. Continue channel processing",
                                    e);
                        }
                    } catch (Exception listenerEx) {
                        log.error("Channel updated event listener, " + listener + ", failed to handle the event",
                                listenerEx);
                    }
                });
                String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode(hstModel.getConfigurationRootPath()), false);
                session.save();
                hstModel.getEventPathsInvalidator().eventPaths(pathsToBeChanged);
            } catch (RepositoryException | IllegalArgumentException e) {
                throw new ChannelException("Unable to save channel to the repository", e);
            }
        }
    }

    /**
     * @return created contentRootNode or <code>null</code> when no content has been created as part of the channel creation. Note that if there has been
     * created content, this content also already has been persisted as it is created through workflow. In case of a later
     * {@link ChannelManagerEventListenerException} the created content has to be explicitly removed again.
     */
    private Node createChannel(Node configRoot, Blueprint blueprint, Session session, final String channelName, final Channel channel) throws ChannelException, RepositoryException {
        Node contentRootNode = null;
        try {
            // Create virtual host
            final URI channelUri = getChannelUri(channel);
            // NOTE channel.getHostGroup() is null for a blueprint channel since a blueprint can be used for any host group
            final Node virtualHost = getOrCreateVirtualHost(configRoot, channelUri.getHost());

            // Create or reuse HST configuration
            final Node blueprintNode =  session.getNode(blueprint.getPath());
            final String hstConfigPath = reuseOrCopyConfiguration(session, configRoot, blueprintNode, channelName, channel);
            channel.setHstConfigPath(hstConfigPath);

            // Create content if the blueprint contains a content prototype. The path of the created content node has to
            // be set on the HST site nodes.
            final String channelContentRootPath;
            if (blueprint.getHasContentPrototype()) {
                contentRootNode = createContent(blueprint, session, channelName, channel);
                channelContentRootPath = contentRootNode.getPath();
                channel.setContentRoot(channelContentRootPath);
            } else {
                channelContentRootPath = channel.getContentRoot();
            }

            final Node sitesNode = configRoot.getNode(sites);
            final Node liveSiteNode = createSiteNode(sitesNode, channelName, channelContentRootPath);

            final String mountPointPath = liveSiteNode.getPath();
            channel.setHstMountPoint(mountPointPath);

            // Create mount
            Node mount = createMountNode(virtualHost, blueprintNode, channelUri.getPath());
            mount.setProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT, mountPointPath);
            final String locale = channel.getLocale();
            if (locale != null) {
                mount.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCALE, locale);
            }
        } catch (ChannelException e) {
            if (contentRootNode != null) {
                session.refresh(false);     // remove the new configuration
                contentRootNode.remove();   // remove the new content which was persisted via workflow already
                session.save();
            } else {
                session.refresh(false);
            }
            throw e;
        } catch (Exception e) {
            session.refresh(false);
            throw e;
        }
        return contentRootNode;
    }


    private String reuseOrCopyConfiguration(final Session session, final Node configRoot, final Node blueprintNode, final String channelName, final Channel channel) throws ChannelException, RepositoryException {
        final Node hstConfigurations = configRoot.getNode(HstNodeTypes.NODENAME_HST_CONFIGURATIONS);
        // first try to copy existing blueprint configuration
        if (blueprintNode.hasNode(HstNodeTypes.NODENAME_HST_CONFIGURATION)) {
            final Node blueprintConfiguration = blueprintNode.getNode(HstNodeTypes.NODENAME_HST_CONFIGURATION);

            final Node configuration = copyNodes(blueprintConfiguration, hstConfigurations, channelName);
            if (!configuration.hasNode(NODENAME_HST_WORKSPACE)) {
                configuration.addNode(NODENAME_HST_WORKSPACE, NODETYPE_HST_WORKSPACE);
            }
            if (configuration.hasNode(NODENAME_HST_CHANNEL)) {
                // move the channel to workspace so it is editable
                session.move(configuration.getPath() + "/" + NODENAME_HST_CHANNEL, configuration.getPath() +"/" + NODENAME_HST_WORKSPACE + "/" + NODENAME_HST_CHANNEL);
            }
            final Node workspace = configuration.getNode(NODENAME_HST_WORKSPACE);
            if (!workspace.hasNode(NODENAME_HST_CHANNEL)) {
                workspace.addNode(NODENAME_HST_CHANNEL, NODETYPE_HST_CHANNEL);
            }
            // only reset the name
            workspace.getNode(NODENAME_HST_CHANNEL).setProperty(CHANNEL_PROPERTY_NAME, channel.getName());
            return configuration.getPath();
        }

        // next, try to reuse the configuration path specified in the hst:site node of the blueprint
        if (blueprintNode.hasNode(HstNodeTypes.NODENAME_HST_SITE)) {
            Node siteNode = blueprintNode.getNode(HstNodeTypes.NODENAME_HST_SITE);
            if (siteNode.hasProperty(SITE_CONFIGURATIONPATH)) {
                String configurationPath = siteNode.getProperty(SITE_CONFIGURATIONPATH).getString();
                if (!session.nodeExists(configurationPath)) {
                    throw new ChannelException(
                            "Blueprint '" + blueprintNode.getPath() + "' does not have an hst:configuration node, and its hst:site node points to a non-existing node: '" + configurationPath + "'");
                }
                // the blueprint site node has explicit pointer to configuration. We will create a configuration for
                // the new site that inherits everything. The new channel *will* however gets its own hst:workspace/hst:channel
                // node otherwise the channel won't be visible in the channel manager and we'll copy the 'inherited' channel
                // its channel node if it has one.
                Node configuration = hstConfigurations.addNode(channelName, NODETYPE_HST_CONFIGURATION);
                Node workspaceNode = configuration.addNode(NODENAME_HST_WORKSPACE, NODETYPE_HST_WORKSPACE);
                Node inherited = session.getNode(configurationPath);
                final Node inheritedChannelNode;
                if (inherited.hasNode(NODENAME_HST_CHANNEL)) {
                    inheritedChannelNode = inherited.getNode(NODENAME_HST_CHANNEL);
                } else if (inherited.hasNode(NODENAME_HST_WORKSPACE + "/" + NODENAME_HST_CHANNEL)) {
                    inheritedChannelNode = inherited.getNode(NODENAME_HST_WORKSPACE + "/" + NODENAME_HST_CHANNEL);
                } else {
                    inheritedChannelNode = null;
                }

                if (inheritedChannelNode != null) {
                    Node channelNode = JcrUtils.copy(session, inheritedChannelNode.getPath(), workspaceNode.getPath() + "/" + NODENAME_HST_CHANNEL);
                    channelNode.setProperty(CHANNEL_PROPERTY_NAME, channel.getName());
                } else {
                    Node channelNode = workspaceNode.addNode(NODENAME_HST_CHANNEL, NODETYPE_HST_CHANNEL);
                    channelNode.setProperty(CHANNEL_PROPERTY_NAME, channel.getName());
                }
                configuration.setProperty(HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM,
                        new String[]{"../"+inherited.getName(), "../" + inherited.getName() + "/" + NODENAME_HST_WORKSPACE});
                return configuration.getPath();
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

    /*
     * This returns the host group of the current request, which is a cms wicket channel manager request. However, we
     * REQUIRE that the cms hostgroup name is the same as the host group name for the separate hst website configurations,
     * hence we can get hold of the cms host group via the current HstRequestContext
     */
    private String getHostGroupNameFromContext() throws ChannelException {
        final String cmsHostGroupName = RequestContextProvider.get().getVirtualHost().getHostGroupName();
        if (StringUtils.isEmpty(cmsHostGroupName)) {
            throw new ChannelException("There is no hostgroup for cms host available. Cannot get or create virtual hosts");
        }
        return cmsHostGroupName;
    }

    private Node getOrCreateVirtualHost(final Node configRoot, final String hostName) throws RepositoryException, ChannelException {
        return getOrCreateVirtualHost(configRoot, hostName, getHostGroupNameFromContext());
    }

    private Node getOrCreateVirtualHost(final Node configRoot, final String hostName, final String hostGroupName) throws RepositoryException, ChannelException {
        String[] elements;

        if (InetAddresses.isInetAddress(hostName)) {
            elements = new String[]{ hostName };
        } else {
            elements = hostName.split("[.]");
        }

        if (!configRoot.hasNode(NODENAME_HST_HOSTS + "/" + hostGroupName)) {
            throw new ChannelException(String.format("Cannot persist new channel since host group '%s' does not exist below '%s'",
                    hostGroupName, configRoot.getPath() + "/" + NODENAME_HST_HOSTS));
        }

        Node host = configRoot.getNode(NODENAME_HST_HOSTS + "/" + hostGroupName);

        for (int i = elements.length - 1; i >= 0; i--) {
            host = getOrAddNode(host, elements[i], HstNodeTypes.NODETYPE_HST_VIRTUALHOST);
        }

        return host;
    }

    private Node createSiteNode(final Node sitesNode, final String siteNodeName, final String contentRootPath) throws RepositoryException {
        log.debug("Creating site node '{}/{}'; content root='{}'", new Object[] {sitesNode.getPath(), siteNodeName, contentRootPath});
        final Node siteNode = sitesNode.addNode(siteNodeName, HstNodeTypes.NODETYPE_HST_SITE);
        siteNode.setProperty(HstNodeTypes.SITE_CONTENT, contentRootPath);
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
            defaultWorkflow.setDisplayName(channel.getName());

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

    private void updateChannel(Node configRoot, final String hostGroupName, final Channel channel) throws ChannelException, RepositoryException {
        URI channelUri = getChannelUri(channel);
        Node virtualHost = getOrCreateVirtualHost(configRoot, channelUri.getHost(), hostGroupName);

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
                configRoot.getSession().getNode(channel.getChannelPath()), channel);
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

        if (!isSupportedScheme(uri.getScheme())) {
            throw new ChannelException(
                    "Illegal channel URL scheme: '" + uri.getScheme() + "'. Only 'http' and 'https' is currently supported");
        }

        if (StringUtils.isBlank(uri.getHost())) {
            throw new ChannelException("Channel URL '" + uri + "' does not contain a host name");
        }

        return uri;
    }

    private boolean isSupportedScheme(final String scheme) {
        if (scheme == null) {
            return false;
        }
        if (scheme.equals("http") || scheme.equals("https")) {
            return true;
        }
        return false;
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
        private Channel channel;
        private Node configRootNode;

        private ChannelManagerEventImpl(final Blueprint blueprint, final Channel channel, final Node configRootNode) {
            if (channel == null || configRootNode == null) {
                throw new IllegalArgumentException("Channel and configRootNode are not allowed to be null in a channel manager event");
            }
            this.blueprint = blueprint;
            this.channel = channel;
            this.configRootNode = configRootNode;
        }

        @Override
        public Blueprint getBlueprint() {
            return blueprint;
        }

        @Override
        public Channel getChannel() {
            return channel;
        }

        @Override
        public Node getConfigRootNode() {
            return configRootNode;
        }
    }

    protected Session getSession() throws RepositoryException {
        return RequestContextProvider.get().getSession();
    }

    private Stream<ChannelManagerEventListener> getChannelManagerEventListeners() {
        return ChannelManagerEventListenerRegistry.get().getEntries().map(e -> e.getServiceObject());
    }

}
