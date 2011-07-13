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

import java.util.ArrayList;
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
        while (!ancestor.isNodeType("hst:virtualhostgroup")) {
            if ("hst:root".equals(ancestor.getName())) {
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
        while (!ancestor.isNodeType("hst:virtualhostgroup")) {
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
            throw new ChannelException("Blue print id " + blueprintId + " is not valid");
        }
        Channel channel = new Channel(blueprintId, nextChannelId());/*
        BlueprintService blueprint = blueprints.get(blueprintId);
        List<HstPropertyDefinition> propertyDefinitions = blueprint.getPropertyDefinitions();
        Map<HstPropertyDefinition, Object> defaultValues = blueprint.getDefaultValues();
        if (propertyDefinitions != null) {
            for (HstPropertyDefinition hpd : propertyDefinitions) {
                if (defaultValues.containsKey(hpd)) {
                    channel.loadProperties().put(hpd, defaultValues.get(hpd));
                } else {
                    channel.loadProperties().put(hpd, hpd.getDefaultValue());
                }
            }
        }*/
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

        String tmp = channel.getUrl();
        if (!tmp.startsWith("http://")) {
            throw new ChannelException("URL does not start with 'http://'.  No other protocol is currently supported");
        }
        tmp = tmp.substring("http://".length());

        String mountPath = "";
        if (tmp.indexOf('/') >= 0) {
            mountPath = tmp.substring(tmp.indexOf('/') + 1);
            while (mountPath.lastIndexOf('/') == mountPath.length() - 1) {
                mountPath = mountPath.substring(0, mountPath.lastIndexOf('/'));
            }
        }
        Node parent = createVirtualHost(configRoot, tmp);

        // create mount
        Node mount = createMount(parent, blueprintNode, mountPath);
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

            Node contentMirrorNode = siteNode.getNode(HstNodeTypes.NODENAME_HST_CONTENTNODE);
            if (channel.getContentRoot() != null) {
                String contentRootPath = channel.getContentRoot();
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

    private Node createMount(Node parent, final Node blueprintNode, final String mountPath) throws RepositoryException {
        Node mount;
        String[] mountPathEls = mountPath.split("/");
        String name = "hst:root";
        if (mountPathEls.length > 0) {
            if (parent.hasNode("hst:root")) {
                parent = parent.getNode("hst:root");
            } else {
                parent = parent.addNode("hst:root", "hst:mount");
            }
            for (int i = 0; i < mountPathEls.length - 1; i++) {
                if (parent.hasNode(mountPathEls[i])) {
                    parent = parent.getNode(mountPathEls[i]);
                } else {
                    parent = parent.addNode(mountPathEls[i], "hst:mount");
                }
            }
            name = mountPathEls[mountPathEls.length - 1];
        }
        if (blueprintNode.hasNode("hst:mount")) {
            mount = copyNodes(blueprintNode.getNode("hst:mount"), parent, name);
        } else {
            mount = parent.addNode(name, "hst:mount");
        }
        return mount;
    }

    private Node createVirtualHost(final Node configRoot, final String tmp) throws RepositoryException {
        String domainEls = tmp.substring(0, tmp.indexOf('/'));

        // create virtual host
        Node parent = configRoot.getNode("hst:hosts/" + hostGroup);
        String[] elements = domainEls.split("[.]");
        for (int i = elements.length - 1; i >= 0; i--) {
            if (parent.hasNode(elements[i])) {
                parent = parent.getNode(elements[i]);
            } else {
                parent = parent.addNode(elements[i], "hst:virtualhost");
            }
        }
        return parent;
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

        String tmp = channel.getUrl();
        if (!tmp.startsWith("http://")) {
            throw new ChannelException("URL does not start with 'http://'.  No other protocol is currently supported");
        }
        tmp = tmp.substring("http://".length());

        String mountPath = "";
        if (tmp.indexOf('/') >= 0) {
            mountPath = tmp.substring(tmp.indexOf('/') + 1);
        }

        // resolve virtual host
        Node mount = configRoot.getNode("hst:hosts/" + hostGroup);
        String[] elements = tmp.substring(0, tmp.indexOf('/')).split("[.]");
        for (int i = elements.length - 1; i >= 0; i--) {
            mount = mount.getNode(elements[i]);
        }

        // resolve mount
        mount = mount.getNode("hst:root");
        String[] mountPathEls = mountPath.split("/");
        for (String mountPathElement : mountPathEls) {
            mount = mount.getNode(mountPathElement);
        }

        Node channelPropsNode;
        if (!mount.hasNode(HstNodeTypes.NODENAME_HST_CHANNELINFO)) {
            channelPropsNode = mount.addNode(HstNodeTypes.NODENAME_HST_CHANNELINFO, HstNodeTypes.NODETYPE_HST_CHANNELINFO);
        } else {
            channelPropsNode = mount.getNode(HstNodeTypes.NODENAME_HST_CHANNELINFO);
        }

        ChannelPropertyMapper.saveProperties(channelPropsNode, bps.getPropertyDefinitions(), channel.getProperties());
    }

}
