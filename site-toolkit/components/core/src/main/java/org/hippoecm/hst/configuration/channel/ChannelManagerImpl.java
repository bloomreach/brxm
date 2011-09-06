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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.security.auth.Subject;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.security.HstSubject;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelManagerImpl implements ChannelManager {

    private static final String DEFAULT_HOST_GROUP = "dev-localhost";
    private static final String DEFAULT_HST_ROOT_PATH = "/hst:hst";
    private static final String DEFAULT_HST_SITES = "hst:sites";

    static final Logger log = LoggerFactory.getLogger(ChannelManagerImpl.class.getName());

    private String rootPath = DEFAULT_HST_ROOT_PATH;
    private String hostGroup = DEFAULT_HOST_GROUP;
    private String sites = DEFAULT_HST_SITES;

    private Map<String, BlueprintService> blueprints;
    private Map<String, Channel> channels;
    private Credentials credentials;
    private Repository repository;
    private String channelsRoot = DEFAULT_HST_ROOT_PATH + "/" + HstNodeTypes.NODENAME_HST_CHANNELS + "/";

    /**
     * The codec which is used for the channel ID
     */
    private StringCodec channelIdCodec = new StringCodecFactory.UriEncoding();

    public ChannelManagerImpl() {
    }

    public void setCredentials(final Credentials credentials) {
        this.credentials = credentials;
    }

    public void setRepository(final Repository repository) {
        this.repository = repository;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
        channelsRoot = rootPath + "/" + HstNodeTypes.NODENAME_HST_CHANNELS + "/";
    }

    public void setHostGroup(String hostGroup) {
        this.hostGroup = hostGroup;
    }

    public void setSites(final String sites) {
        this.sites = sites;
    }

    private void loadBlueprints(final Node configNode) throws RepositoryException {
        if (configNode.hasNode(HstNodeTypes.NODENAME_HST_BLUEPRINTS)) {
            Node blueprintsNode = configNode.getNode(HstNodeTypes.NODENAME_HST_BLUEPRINTS);
            NodeIterator blueprintIterator = blueprintsNode.getNodes();
            while (blueprintIterator.hasNext()) {
                Node blueprint = blueprintIterator.nextNode();
                blueprints.put(blueprint.getName(), new BlueprintService(blueprint));
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
            log.warn("Cannot load channels because node '{}' does not exist", configNode.getPath() + "/" + HstNodeTypes.NODENAME_HST_CHANNELS);
        }
    }

    private void loadMounts(final Node configNode) throws RepositoryException {
        if (!configNode.hasNode(HstNodeTypes.NODENAME_HST_HOSTS)) {
            log.warn("Cannot load mounts because node '{}' does not exist", configNode.getPath() + "/" + HstNodeTypes.NODENAME_HST_HOSTS);
            return;
        }

        final Node hstHosts = configNode.getNode(HstNodeTypes.NODENAME_HST_HOSTS);
        final String locale = getStringPropertyOrDefault(hstHosts, HstNodeTypes.GENERAL_PROPERTY_LOCALE, null);

        if (!hstHosts.hasNode(hostGroup)) {
            log.warn("Cannot load mounts because node '{}' does not exist", hstHosts.getPath() + "/" + hostGroup);
            return;
        }

        final Node virtualHostGroup = hstHosts.getNode(this.hostGroup);

        NodeIterator rootVirtualHostNodes = virtualHostGroup.getNodes();
        while (rootVirtualHostNodes.hasNext()) {
            Node virtualHost = rootVirtualHostNodes.nextNode();
            populateChannels(virtualHost, locale);
        }
    }

    private void loadChannel(Node currNode) throws RepositoryException {
        Channel channel = ChannelPropertyMapper.readChannel(currNode);
        channels.put(channel.getId(), channel);
    }

    /**
     * Recursively populates the channels with URLs and other mount information.
     * Ignores the mounts which are configured to be "rest" or "composer" either in hst:type or hst:types.
     *
     * @param node the inital node to start with, must be a virtual host node.
     * @param defaultLocale the locale to use for this node if it does not specify a locale itself
     * @throws javax.jcr.RepositoryException In case cannot read required node/property from the repository.
     */
    private void populateChannels(Node node, String defaultLocale) throws RepositoryException {
        final String nodeLocale = getStringPropertyOrDefault(node, HstNodeTypes.GENERAL_PROPERTY_LOCALE, defaultLocale);

        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            Node currNode = nodes.nextNode();

            //Get the channels from the child node.
            populateChannels(currNode, nodeLocale);

            if (!currNode.isNodeType(HstNodeTypes.NODETYPE_HST_MOUNT) || !currNode.hasProperty(HstNodeTypes.MOUNT_PROPERTY_CHANNELPATH)) {
                continue;
            }

            String channelPath = currNode.getProperty(HstNodeTypes.MOUNT_PROPERTY_CHANNELPATH).getString();
            if (!channelPath.startsWith(channelsRoot)) {
                log.warn("Channel id " + channelPath + " is not part of the hst configuration under " + rootPath +
                        ", ignoring channel info for mount " + currNode.getPath() +
                        ".  Use the full repository path for identification.");
                continue;
            }
            Channel channel = channels.get(channelPath.substring(channelsRoot.length()));
            if (channel == null) {
                log.warn("Unknown channel " + channelPath + ", ignoring mount " + currNode.getPath());
                continue;
            }
            if (channel.getUrl() != null) {
                // We already encountered this channel while recursively walking over all the mounts. This mount
                // therefore points to the same channel as another mount, which is not allowed (each channel has only
                // one mount)
                log.warn("Channel " + channelPath + " contains multiple mounts - analysing node " + currNode.getPath() + ", found url " + channel.getUrl() + " in channel");
                continue;
            }

            if (currNode.hasProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT)) {
                String mountPoint = currNode.getProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT).getString();
                Node siteNode = currNode.getSession().getNode(mountPoint);
                channel.setHstMountPoint(siteNode.getPath());
                if (siteNode.hasProperty(HstNodeTypes.SITE_CONFIGURATIONPATH)) {
                    channel.setHstConfigPath(siteNode.getProperty(HstNodeTypes.SITE_CONFIGURATIONPATH).getString());
                }
                Node contentNode = siteNode.getNode(HstNodeTypes.NODENAME_HST_CONTENTNODE);
                if (contentNode.hasProperty(HippoNodeType.HIPPO_DOCBASE)) {
                    String siteDocbase = contentNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                    String contentRoot = contentNode.getSession().getNodeByIdentifier(siteDocbase).getPath();
                    channel.setContentRoot(contentRoot);
                }
            }

            channel.setMountId(currNode.getIdentifier());

            setUrlFor(currNode, channel);

            final String channelLocale = getStringPropertyOrDefault(currNode, HstNodeTypes.GENERAL_PROPERTY_LOCALE, nodeLocale);
            channel.setLocale(channelLocale);
        }
    }

    private void setUrlFor(final Node currNode, final Channel channel) throws RepositoryException {
        StringBuilder mountBuilder = new StringBuilder();
        Node ancestor = currNode;
        while (!ancestor.isNodeType(HstNodeTypes.NODETYPE_HST_VIRTUALHOSTGROUP)) {
            if (HstNodeTypes.MOUNT_HST_ROOTNAME.equals(ancestor.getName())) {
                ancestor = ancestor.getParent();
                break;
            }
            mountBuilder.insert(0, ancestor.getName());
            mountBuilder.insert(0, "/");
            ancestor = ancestor.getParent();
        }
        channel.setSubMountPath(mountBuilder.toString());
        boolean firstHost = true;
        StringBuilder hostBuilder = new StringBuilder();
        while (!ancestor.isNodeType(HstNodeTypes.NODETYPE_HST_VIRTUALHOSTGROUP)) {
            if (firstHost) {
                firstHost = false;
            } else {
                hostBuilder.append(".");
            }
            hostBuilder.append(ancestor.getName());
            ancestor = ancestor.getParent();
        }
        channel.setHostname(hostBuilder.toString());
        channel.setUrl("http://" + hostBuilder.toString() + mountBuilder.toString());
    }

    private void load() throws ChannelException {
        if (channels == null) {
            Session session = null;
            try {
                session = getSession(false);
                Node configNode = session.getNode(rootPath);

                blueprints = new HashMap<String, BlueprintService>();
                loadBlueprints(configNode);

                channels = new HashMap<String, Channel>();
                loadChannels(configNode);

                loadMounts(configNode);

            } catch (RepositoryException e) {
                throw new ChannelException("Could not load channels and/or blueprints", e);
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        }
    }

    protected Session getSession(boolean writable) throws RepositoryException {
        Credentials credentials = this.credentials;
        if (writable) {
            Subject subject = HstSubject.getSubject(null);
            if (subject != null) {
                Set<Credentials> repoCredsSet = subject.getPrivateCredentials(Credentials.class);
                if (!repoCredsSet.isEmpty()) {
                    credentials = repoCredsSet.iterator().next();
                } else {
                    throw new LoginException("Repository credentials for the subject is not found.");
                }
            } else {
                throw new LoginException("No subject available to obtain writable session");
            }
        }

        javax.jcr.Session session;

        if (credentials == null) {
            session = this.repository.login();
        } else {
            session = this.repository.login(credentials);
        }

        // session can come from a pooled event based pool so always refresh before building configuration:
        session.refresh(false);

        return session;
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
        String channelId = createUniqueChannelId(channel.getName());

        if (!blueprints.containsKey(blueprintId)) {
            throw new ChannelException("Blueprint id " + blueprintId + " is not valid");
        }
        BlueprintService bps = blueprints.get(blueprintId);

        Session session = null;
        try {
            session = getSession(true);
            Node configNode = session.getNode(rootPath);
            createChannel(configNode, bps, session, channelId, channel);

            channels = null;

            session.save();

            return channelId;
        } catch (RepositoryException e) {
            throw new ChannelException("Unable to save channel to the repository", e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

    String createUniqueChannelId(String channelName) throws ChannelException {
        if (StringUtils.isBlank(channelName)) {
            throw new ChannelException("Cannot create channel ID: channel name is blank");
        }

        load();

        String channelId = channelIdCodec.encode(channelName);
        int retries = 0;

        while (channels.containsKey(channelId)) {
            retries += 1;
            StringBuilder builder = new StringBuilder(channelName);
            builder.append('-');
            builder.append(retries);
            channelId = channelIdCodec.encode(builder.toString());
        }

        return channelId;
    }

    @Override
    public synchronized void save(final Channel channel) throws ChannelException {
        load();
        if (!channels.containsKey(channel.getId())) {
            throw new ChannelException("No channel with id " + channel.getId() + " was found");
        }
        Session session = null;
        try {
            session = getSession(true);
            Node configNode = session.getNode(rootPath);
            updateChannel(configNode, channel);

            channels = null;

            session.save();
        } catch (RepositoryException e) {
            throw new ChannelException("Unable to save channel to the repository", e);
        } finally {
            if (session != null) {
                session.logout();
            }
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
            return (Class<? extends ChannelInfo>) ChannelPropertyMapper.class.getClassLoader().loadClass(channelInfoClassName);
        } catch (ClassNotFoundException cnfe) {
            throw new ChannelException("Configured class " + channelInfoClassName + " was not found", cnfe);
        } catch (ClassCastException cce) {
            throw new ChannelException("Configured class " + channelInfoClassName + " does not extend ChannelInfo", cce);
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
    public synchronized Channel getChannelByJcrPath(final String channelPath) throws ChannelException {
        load();
        if (channelPath.startsWith(channelsRoot)) {
            return channels.get(channelPath.substring(channelsRoot.length()));
        } else {
           log.warn("Channel path " + channelPath + " is not part of the hst configuration under " + rootPath +
                ".  Use the full repository path for identification.");
        }
        return null;
    }

    @Override
    public ResourceBundle getResourceBundle(Channel channel, Locale locale) {
        String channelInfoClassName = channel.getChannelInfoClassName();
        if (channelInfoClassName != null) {
            return ResourceBundle.getBundle(channelInfoClassName, locale);
        }
        return null;
    }

    public synchronized void invalidate() {
        channels = null;
        blueprints = null;
    }

    // private - internal - methods

    private void createChannel(Node configRoot, BlueprintService bps, Session session, final String channelId, final Channel channel) throws ChannelException, RepositoryException {
        Node blueprintNode = bps.getNode(session);

        URI channelUri = getChannelUri(channel);
        Node virtualHost = getOrCreateVirtualHost(configRoot, channelUri.getHost());

        // create mount
        Node mount = createMountNode(virtualHost, blueprintNode, channelUri.getPath());
        mount.setProperty(HstNodeTypes.MOUNT_PROPERTY_CHANNELPATH, channelsRoot + channelId);
        if (mount.hasProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT)) {
            if (blueprintNode.hasNode(HstNodeTypes.NODENAME_HST_SITE)) {
                mount.setProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT, channelId);
            } else {
                mount.getProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT).remove();
            }
        }

        if (!configRoot.hasNode(HstNodeTypes.NODENAME_HST_CHANNELS)) {
            configRoot.addNode(HstNodeTypes.NODENAME_HST_CHANNELS, HstNodeTypes.NODETYPE_HST_CHANNELS);
        }
        Node channelNode = configRoot.getNode(HstNodeTypes.NODENAME_HST_CHANNELS).addNode(channelId, HstNodeTypes.NODETYPE_HST_CHANNEL);
        ChannelPropertyMapper.saveChannel(channelNode, channel);

        Session jcrSession = configRoot.getSession();
        if (blueprintNode.hasNode(HstNodeTypes.NODENAME_HST_SITE)) {
            Node siteNode = copyNodes(blueprintNode.getNode(HstNodeTypes.NODENAME_HST_SITE), configRoot.getNode(sites), channelId);
            mount.setProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT, siteNode.getPath());
            channel.setHstMountPoint(siteNode.getPath());

            if (siteNode.hasProperty(HstNodeTypes.SITE_CONFIGURATIONPATH)) {
                if (blueprintNode.hasNode(HstNodeTypes.NODENAME_HST_CONFIGURATION)) {
                    siteNode.setProperty(HstNodeTypes.SITE_CONFIGURATIONPATH, configRoot.getNode(HstNodeTypes.NODENAME_HST_CONFIGURATIONS).getPath() + "/" + channel.getId());
                } else {
                    // reuse the configuration path specified in the hst:site node, if it exists
                    String configurationPath = siteNode.getProperty(HstNodeTypes.SITE_CONFIGURATIONPATH).getString();
                    if (!jcrSession.nodeExists(configurationPath)) {
                        throw new ChannelException("The hst:site node in blueprint '" + blueprintNode.getPath()
                                + "' does not have a custom HST configuration in a child node 'hst:configuration' and property '" + HstNodeTypes.SITE_CONFIGURATIONPATH + "' points to a non-existing node");
                    }
                }
                channel.setHstConfigPath(siteNode.getProperty(HstNodeTypes.SITE_CONFIGURATIONPATH).getString());
            }

            final String contentRootPath = channel.getContentRoot();
            if (contentRootPath != null) {
                final Node contentMirrorNode = siteNode.getNode(HstNodeTypes.NODENAME_HST_CONTENTNODE);
                if (jcrSession.itemExists(contentRootPath)) {
                    contentMirrorNode.setProperty(HippoNodeType.HIPPO_DOCBASE, jcrSession.getNode(contentRootPath).getIdentifier());
                } else {
                    log.warn("Specified content root '" + contentRootPath + "' does not exist");
                    contentMirrorNode.setProperty(HippoNodeType.HIPPO_DOCBASE, jcrSession.getRootNode().getIdentifier());
                }
            }

            final String locale = channel.getLocale();
            if (locale != null) {
                mount.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCALE, locale);
            }
        } else if (mount.hasProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT)) {
            mount.getProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT).remove();
        }
        if (blueprintNode.hasNode(HstNodeTypes.NODENAME_HST_CONFIGURATION)) {
            copyNodes(blueprintNode.getNode(HstNodeTypes.NODENAME_HST_CONFIGURATION), configRoot.getNode(HstNodeTypes.NODENAME_HST_CONFIGURATIONS), channelId);
        }
    }

    private Node createMountNode(Node virtualHost, final Node blueprintNode, final String mountPath) throws MountNotFoundException, RepositoryException {
        ArrayList<String> mountPathElements = new ArrayList<String>();
        mountPathElements.add(HstNodeTypes.MOUNT_HST_ROOTNAME);
        mountPathElements.addAll(Arrays.asList(StringUtils.split(mountPath, '/')));

        Node mount = virtualHost;

        for (int i = 0; i < mountPathElements.size() - 1; i++) {
            String mountPathElement = mountPathElements.get(i);
            if (mount.hasNode(mountPathElement)) {
                mount = mount.getNode(mountPathElement);
            } else {
                throw new MountNotFoundException(mount.getPath() + "/" + mountPathElement);
            }
        }

        String lastMountPathElementName = mountPathElements.get(mountPathElements.size() - 1);

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

    private static Node getOrAddNode(Node parent, String nodeName, String nodeType) throws RepositoryException {
        if (parent.hasNode(nodeName)) {
            return parent.getNode(nodeName);
        } else {
            return parent.addNode(nodeName, nodeType);
        }
    }

    private static String getStringPropertyOrDefault(Node node, String propName, String defaultValue) throws RepositoryException {
        if (node.hasProperty(propName)) {
            return node.getProperty(propName).getString();
        }
        return defaultValue;
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
            throw new MountNotFoundException(virtualHost.getPath() + "/" + HstNodeTypes.MOUNT_HST_ROOTNAME);
        }
        final String path = channelUri.getPath();
        if (path != null) {
            for (String mountPathElement : StringUtils.split(path, '/')) {
                if (mount.hasNode(mountPathElement)) {
                    mount = mount.getNode(mountPathElement);
                } else {
                    throw new MountNotFoundException(mount.getPath() + "/" + mountPathElement);
                }
            }
        }

        ChannelPropertyMapper.saveChannel(configRoot.getNode(HstNodeTypes.NODENAME_HST_CHANNELS + "/" + channel.getId()), channel);
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
            throw new ChannelException("Illegal channel URL scheme: '" + uri.getScheme() + "'. Only 'http' is currently supported");
        }

        if (StringUtils.isBlank(uri.getHost())) {
            throw new ChannelException("Channel URL '" + uri + "' does not contain a host name");
        }

        return uri;
    }
    
    

}
