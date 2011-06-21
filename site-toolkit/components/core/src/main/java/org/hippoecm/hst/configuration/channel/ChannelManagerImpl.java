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

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelManagerImpl implements ChannelManager {

    static final Logger log = LoggerFactory.getLogger(ChannelManagerImpl.class.getName());

    private String rootPath = "/hst:hst";

    private int lastChannelId;
    private Map<String, Blueprint> blueprints;
    private Map<String, Channel> channels;
    private Credentials credentials;
    private Repository repository;

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
        if (configNode.hasNode("hst:hosts")) {
            Node virtualHosts = configNode.getNode("hst:hosts");
            NodeIterator rootChannelNodes = virtualHosts.getNodes();
            while (rootChannelNodes.hasNext()) {
                Node hgNode = rootChannelNodes.nextNode();
                populateChannels(hgNode);
            }
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
            Channel channel = null;
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
            }
            if (channels.containsKey(id)) {
                channel = channels.get(id);
                if (!channel.getBlueprintId().equals(bluePrintId)) {
                    log.warn("Channel found with id " + id + " that has a different blueprint id; " + "expected " + channel.getBlueprintId() + ", found " + bluePrintId + ".  Ignoring mount");
                }
            } else {
                channel = new Channel(bluePrintId, id);
                channels.put(id, channel);
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

    private void load() throws ChannelException {
        if (channels == null) {
            Session session = null;
            try {
                session = getSession();
                Node configNode = null;
                configNode = session.getNode(rootPath);

                channels = new HashMap<String, Channel>();
                blueprints = new HashMap<String, Blueprint>();
                loadChannels(configNode);
                loadBlueprints(configNode);
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

    protected Session getSession() throws RepositoryException {
        javax.jcr.Session session = null;

        if (this.credentials == null) {
            session = this.repository.login();
        } else {
            session = this.repository.login(this.credentials);
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
        return new Channel(blueprintId, nextChannelId());
    }

    @Override
    public synchronized void save(final Channel channel) throws ChannelException {
        Session session = null;
        try {
            session = getSession();
            Node configNode = session.getNode(rootPath);
            if (channels.containsKey(channel.getId())) {
                channels.clear();
                loadChannels(configNode);

                if (channels.containsKey(channel.getId())) {
                    Channel previous = channels.get(channel.getId());
                    if (!previous.getBlueprintId().equals(channel.getBlueprintId())) {
                        throw new ChannelException("Cannot change channel to new blue print");
                    }

                    // TODO: validate that mandatory properties (URL and such) have not changed
                } else {
                    throw new ChannelException("Channel was removed since it's retrieval");
                }
            } else {

            }
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

    public synchronized void invalidate() {
        channels = null;
        blueprints = null;
    }
}
