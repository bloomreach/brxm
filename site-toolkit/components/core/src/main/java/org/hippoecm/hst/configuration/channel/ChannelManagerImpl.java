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
import java.util.Map;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelManagerImpl implements ChannelManager {

    static final Logger log = LoggerFactory.getLogger(ChannelManagerImpl.class.getName());

    private String rootPath = "/hst:hst";

    private int lastChannelId;
    private Map<String, BlueprintService> blueprints;
    private Map<String, Channel> channels;
    private Credentials credentials;
    private Repository repository;

    private String hostGroup = "dev-localhost";

    private String sites = "hst:sites";

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
        Node virtualHosts = configNode.getNode("hst:hosts/" + hostGroup);
        NodeIterator rootChannelNodes = virtualHosts.getNodes();
        while (rootChannelNodes.hasNext()) {
            Node hgNode = rootChannelNodes.nextNode();
            populateChannels(hgNode);
        }
    }

    /**
     * Recursively gets the list of "channels" configured under a virtual host node.
     * <p/>
     * Ignores the mounts which are configured to be "rest" or "composer" either in hst:type or hst:types.
     *
     * @param node - the inital node to start with, must be a virtual host node.
     * @throws javax.jcr.RepositoryException - In case cannot read required node/property from the repository.
     */
    private void populateChannels(Node node) throws RepositoryException {
        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            Node currNode = nodes.nextNode();

            //Get the channels from the child node.
            populateChannels(currNode);

            if (!currNode.isNodeType(HstNodeTypes.NODETYPE_HST_MOUNT)) {
                continue;
            }

            if (!currNode.hasProperty(HstNodeTypes.MOUNT_PROPERTY_CHANNELID)) {
                continue;
            }

            String id = currNode.getProperty(HstNodeTypes.MOUNT_PROPERTY_CHANNELID).getString();
            String bluePrintId = null;
            if (currNode.hasProperty(HstNodeTypes.MOUNT_PROPERTY_BLUEPRINTID)) {
                bluePrintId = currNode.getProperty(HstNodeTypes.MOUNT_PROPERTY_BLUEPRINTID).getString();
                if (!blueprints.containsKey(bluePrintId)) {
                    log.warn("Invalid blue print id '" + bluePrintId + "' found; ignoring channel");
                    continue;
                }
            }

            Channel channel;
            if (channels.containsKey(id)) {
                channel = channels.get(id);
                if (!channel.getBlueprintId().equals(bluePrintId)) {
                    log.warn("Channel found with id " + id + " that has a different blueprint id; " + "expected " + channel.getBlueprintId() + ", found " + bluePrintId + ".  Ignoring mount");
                }
            } else {
                channel = new Channel(bluePrintId, id);
                channels.put(id, channel);
            }

            setUrlFor(currNode, channel);

            if (currNode.hasNode(HstNodeTypes.NODENAME_HST_CHANNELINFO)) {
                Node propertiesNode = currNode.getNode(HstNodeTypes.NODENAME_HST_CHANNELINFO);

                if (propertiesNode.hasProperty(HstNodeTypes.CHANNELINFO_PROPERTY_NAME)) {
                    channel.setName(propertiesNode.getProperty(HstNodeTypes.CHANNELINFO_PROPERTY_NAME).getString());
                }

                BlueprintService blueprint = blueprints.get(bluePrintId);
                if (blueprint != null) {
                    Map<String, Object> channelProperties = channel.getProperties();
                    channelProperties.putAll(blueprint.loadChannelProperties(propertiesNode));
                } else {
                    log.warn("Unknown blueprint id '{}' found on node '{}'. Properties of this channel will not be loaded.", bluePrintId, currNode.getPath());
                }
            }

            if (currNode.hasProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT)) {
                String mountPoint = currNode.getProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT).getString();
                Node siteNode = currNode.getSession().getNode(mountPoint);
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

            } catch (RepositoryException e) {
                throw new ChannelException("Could not load channels and/or blueprints", e);
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        }
    }

    protected String nextChannelId() {
        while (channels.containsKey("channel-" + lastChannelId)) {
            lastChannelId++;
        }
        return "channel-" + lastChannelId;
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
        return Collections.unmodifiableMap(channels);
    }

    @Override
    public synchronized Channel createChannel(final String blueprintId) throws ChannelException {
        load();
        if (!blueprints.containsKey(blueprintId)) {
            throw new ChannelException("Blueprint id " + blueprintId + " is not valid");
        }
        Channel channel = new Channel(blueprintId, nextChannelId());
        Map<String, Object> properties = channel.getProperties();

        BlueprintService blueprint = blueprints.get(blueprintId);
        List<HstPropertyDefinition> propertyDefinitions = blueprint.getPropertyDefinitions();
        if (propertyDefinitions != null) {
            for (HstPropertyDefinition hpd : propertyDefinitions) {
                properties.put(hpd.getName(), hpd.getDefaultValue());
            }
        }
        return channel;
    }

    @Override
    public synchronized void save(final Channel channel) throws ChannelException {
        load();
        Session session = null;
        try {
            BlueprintService bps = blueprints.get(channel.getBlueprintId());
            if (bps == null) {
                throw new ChannelException("Invalid blueprint ID " + channel.getBlueprintId());
            }

            session = getSession(true);
            Node configNode = session.getNode(rootPath);
            if (channels.containsKey(channel.getId())) {

                // verify none of the essential properties has changed
                checkChannelUpdate(channels.get(channel.getId()), channel);

                updateChannel(configNode, bps, channel);
            } else {
                createChannel(configNode, bps, session, channel);
            }

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
    public synchronized <T> T getChannelInfo(String channelId) throws ChannelException {
        load();
        if (channelId != null && channels.containsKey(channelId)) {
            Channel channel = channels.get(channelId);
            String blueprintId = channel.getBlueprintId();
            Blueprint bp = blueprints.get(blueprintId);

            if (bp == null) {
                log.warn("No blueprint found with id '{}' for channel '{}'. The channel should have a blueprint with a channel info class in order to use the channel info object.", blueprintId, channelId);
            } else if (bp.getChannelInfoClass() == null) {
                log.warn("No channel info class specified for blueprint '{}' of channel '{}'. The channel should have a blueprint with a channel info class in order to use the channel info object.", blueprintId, channelId);
            } else {
                return (T) ChannelUtils.getChannelInfo(channel.getProperties(), bp.getChannelInfoClass());
            }
        }
        return null;
    }

    public synchronized void invalidate() {
        channels = null;
        blueprints = null;
    }

    // private - internal - methods

    private void createChannel(Node configRoot, BlueprintService bps, Session session, final Channel channel) throws ChannelException, RepositoryException {
        Node blueprintNode = bps.getNode(session);

        URI channelUri = getChannelUri(channel);
        Node virtualHost = getOrCreateVirtualHost(configRoot, channelUri.getHost());

        // create mount
        Node mount = createMountNode(virtualHost, blueprintNode, channelUri.getPath());
        mount.setProperty(HstNodeTypes.MOUNT_PROPERTY_CHANNELID, channel.getId());
        mount.setProperty(HstNodeTypes.MOUNT_PROPERTY_BLUEPRINTID, channel.getBlueprintId());
        if (mount.hasProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT)) {
            if (blueprintNode.hasNode(HstNodeTypes.NODENAME_HST_BLUEPRINT_SITE)) {
                mount.setProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT, channel.getId());
            } else {
                mount.getProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT).remove();
            }
        }

        Node channelPropsNode = mount.addNode(HstNodeTypes.NODENAME_HST_CHANNELINFO, HstNodeTypes.NODETYPE_HST_CHANNELINFO);
        bps.saveChannelProperties(channelPropsNode, channel.getProperties());
        channelPropsNode.setProperty(HstNodeTypes.CHANNELINFO_PROPERTY_NAME, channel.getName());

        Session jcrSession = configRoot.getSession();
        if (blueprintNode.hasNode(HstNodeTypes.NODENAME_HST_BLUEPRINT_SITE)) {
            Node siteNode = copyNodes(blueprintNode.getNode(HstNodeTypes.NODENAME_HST_BLUEPRINT_SITE), configRoot.getNode(sites), channel.getId());
            mount.setProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT, siteNode.getPath());
            if (siteNode.hasProperty(HstNodeTypes.SITE_CONFIGURATIONPATH)) {
                if (blueprintNode.hasNode("hst:configuration")) {
                    siteNode.setProperty(HstNodeTypes.SITE_CONFIGURATIONPATH, configRoot.getNode("hst:configurations").getPath() + "/" + channel.getId());
                } else {
                    siteNode.getProperty(HstNodeTypes.SITE_CONFIGURATIONPATH).remove();
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
        } else if (mount.hasProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT)) {
            mount.getProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT).remove();
        }
        if (blueprintNode.hasNode("hst:configuration")) {
            copyNodes(blueprintNode.getNode("hst:configuration"), configRoot.getNode("hst:configurations"), channel.getId());
        }
    }

    private Node createMountNode(Node virtualHost, final Node blueprintNode, final String mountPath) throws RepositoryException {
        ArrayList<String> mountPathElements = new ArrayList<String>();
        mountPathElements.add(HstNodeTypes.MOUNT_HST_ROOTNAME);
        mountPathElements.addAll(Arrays.asList(StringUtils.split(mountPath, '/')));

        Node mount = virtualHost;

        for (int i = 0; i < mountPathElements.size() - 1; i++) {
            mount = getOrAddNode(mount, mountPathElements.get(i), HstNodeTypes.NODETYPE_HST_MOUNT);
        }

        String lastMountPathElementName = mountPathElements.get(mountPathElements.size() - 1);

        if (blueprintNode.hasNode("hst:mount")) {
            mount = copyNodes(blueprintNode.getNode("hst:mount"), mount, lastMountPathElementName);
        } else {
            mount = mount.addNode(lastMountPathElementName, HstNodeTypes.NODETYPE_HST_MOUNT);
        }

        return mount;
    }

    private Node getOrCreateVirtualHost(final Node configRoot, final String hostName) throws RepositoryException {
        final String[] elements = hostName.split("[.]");

        Node mount = configRoot.getNode("hst:hosts/" + hostGroup);

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

    private void checkChannelUpdate(Channel reference, Channel update) throws ChannelException {
        if (reference.getBlueprintId().equals(update.getBlueprintId())) {
            return;
        }
        throw new ChannelException("Essential channel property has changed");
    }

    private void updateChannel(Node configRoot, BlueprintService bps, final Channel channel) throws ChannelException, RepositoryException {
        Channel previous = channels.get(channel.getId());
        if (!previous.getBlueprintId().equals(channel.getBlueprintId())) {
            throw new ChannelException("Cannot change channel to new blue print");
        }

        URI channelUri = getChannelUri(channel);
        Node virtualHost = getOrCreateVirtualHost(configRoot, channelUri.getHost());

        // resolve mount
        Node mount;
        if (virtualHost.hasNode(HstNodeTypes.MOUNT_HST_ROOTNAME)) {
            mount = virtualHost.getNode(HstNodeTypes.MOUNT_HST_ROOTNAME);
        } else {
            throw new ChannelException("Virtual host '" + virtualHost.getPath() + "' does not have a child node '"
                    + HstNodeTypes.MOUNT_HST_ROOTNAME + "'");
        }
        final String path = channelUri.getPath();
        if (path != null) {
            String[] mountPathEls = path.split("/");
            for (String mountPathElement : mountPathEls) {
                if (!mountPathElement.isEmpty()) {
                    mount = mount.getNode(mountPathElement);
                }
            }
        }

        Node channelPropsNode;
        if (!mount.hasNode(HstNodeTypes.NODENAME_HST_CHANNELINFO)) {
            channelPropsNode = mount.addNode(HstNodeTypes.NODENAME_HST_CHANNELINFO, HstNodeTypes.NODETYPE_HST_CHANNELINFO);
        } else {
            channelPropsNode = mount.getNode(HstNodeTypes.NODENAME_HST_CHANNELINFO);
        }

        channelPropsNode.setProperty(HstNodeTypes.CHANNELINFO_PROPERTY_NAME, channel.getName());
        ChannelPropertyMapper.saveProperties(channelPropsNode, bps.getPropertyDefinitions(), channel.getProperties());
    }

    /**
     * Returns the channel's URL is a URI object. The returned URI has a supported scheme and a host name.
     *
     * @param channel the channel
     * @return the validated URI of the channel
     * @throws ChannelException if the channel URL is not a valid URI, does not have a supported scheme or does not
     * contain a host name.
     */
    private URI getChannelUri(final Channel channel) throws ChannelException {
        URI uri;

        try {
            uri = new URI(channel.getUrl());
        } catch (URISyntaxException e) {
            throw new ChannelException("Invalid channel URL: '" + channel.getUrl() + "'");
        }

        if (!"http".equals(uri.getScheme())) {
            throw new ChannelException("Illegal channel URL scheme: '" + uri.getScheme()
                    + "'. Only 'http' is currently supported");
        }

        if (StringUtils.isBlank(uri.getHost())) {
            throw new ChannelException("Channel URL '" + uri + "' does not contain a host name");
        }

        return uri;
    }

}
