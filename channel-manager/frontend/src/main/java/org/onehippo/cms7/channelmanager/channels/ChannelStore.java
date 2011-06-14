package org.onehippo.cms7.channelmanager.channels;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.ExtField;
import org.wicketstuff.js.ext.data.ExtJsonStore;
import org.wicketstuff.js.ext.util.JSONIdentifier;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Channel JSON Store.
 */
public class ChannelStore extends ExtJsonStore<Object> {

    private static final Logger log = LoggerFactory.getLogger(ChannelStore.class);

    private static final String HST_VIRTUALHOST_NT = "hst:virtualhost";
    private static final String HST_MOUNT_NT = "hst:mount";
    private static final String HST_MOUNTPOINT_PROP = "hst:mountpoint";
    private static final String HST_TYPE_PROP = "hst:type";
    private static final String HST_TYPES_PROP = "hst:types";

    private List<Channel> channels;
    private JcrNodeModel configNodeModel;


    public ChannelStore(List<ExtField> fields, JcrNodeModel hstHostGroupNodeModel) {
        super(fields);
        this.configNodeModel = hstHostGroupNodeModel;
        this.channels = new ArrayList<Channel>();
        try {
            Node configNode = this.configNodeModel.getNode();
            Node hstHostsNode = configNode.getParent();

            if (!hstHostsNode.getName().equals("hst:hosts")) {
                //panic
                log.error("The config node is not a virtualhost group under hst:hosts, please check the configuration");
            } else {
                NodeIterator rootChannelNodes = configNode.getNodes();
                while (rootChannelNodes.hasNext()) {
                    Node n = rootChannelNodes.nextNode();
                    //Get only the virtualhost nodes.
                    if (n.isNodeType(HST_VIRTUALHOST_NT)) {
                        String vhostNodeName = n.getName();
                        populateChannels(n, null);
                    }
                }
                Collections.sort(channels, new ChannelComparator());
            }
        } catch (RepositoryException e) {
            log.error("Unable to get the list of channel nodes from repository :  " + e.getMessage(), e);
        }

    }

    @Override
    protected JSONObject getProperties() throws JSONException {
        final JSONObject properties = super.getProperties();
        properties.put("writer", new JSONIdentifier("new Ext.data.JsonWriter()"));
        return properties;
    }

    @Override
    protected long getTotal() {
        return this.channels.size();
    }

    @Override
    protected JSONArray getData() throws JSONException {
        JSONArray data = new JSONArray();
        for (Channel channel : channels) {
            JSONObject object = new JSONObject();
            object.put("title", channel.getTitle());
            object.put("contentRoot", channel.getContentRoot());
            data.put(object);
        }
        return data;
    }


    /**
     * Recursively gets the list of "channels" configured under a virtual host node.
     * <p/>
     * Ignores the mounts which are configured to be "rest" or "composer" either in hst:type or hst:types.
     *
     * @param node   - the inital node to start with, must be a virtual host node.
     * @param parent - the parent channel - can be null if this is the root channel.
     * @throws javax.jcr.RepositoryException - In case cannot read required node/property from the repository.
     */
    private void populateChannels(Node node, Channel parent) throws RepositoryException {
        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            boolean ignoreMount = false;
            Channel channel = null;
            Node currNode = nodes.nextNode();

            if (currNode.isNodeType(HST_MOUNT_NT)) {
                if (currNode.hasProperty(HST_TYPE_PROP)) {
                    String hstMountType = currNode.getProperty(HST_TYPE_PROP).getString();
                    if (hstMountType.contains("rest") || hstMountType.contains("composer")) {
                        ignoreMount = true;
                    }
                }
                if (currNode.hasProperty(HST_TYPES_PROP)) {
                    Value[] hstTypes = currNode.getProperty(HST_TYPES_PROP).getValues();
                    for (Value hstType : hstTypes) {
                        String mountType = hstType.getString();
                        if (mountType.contains("rest") || mountType.contains("composer")) {
                            ignoreMount = true;
                        }
                    }
                }


                if (!ignoreMount) {
                    channel = new Channel(currNode.getName());
                    if (currNode.hasProperty(HST_TYPE_PROP)) {
                        String mountType = currNode.getProperty(HST_TYPE_PROP).getString();
                        channel.setType(mountType);
                    }
                    channel.setParent(parent);

                    if (currNode.hasProperty("hst:mountpoint")) {
                        String mountPoint = currNode.getProperty("hst:mountpoint").getString();
                        Node siteNode = currNode.getSession().getNode(mountPoint);
                        if (siteNode.hasProperty("hst:configurationpath")) {
                            channel.setHstConfigPath(siteNode.getProperty("hst:configurationpath").getString());
                        }
                        Node contentNode = siteNode.getNode("hst:content");
                        if (contentNode.hasProperty("hippo:docbase")) {
                            String siteDocbase = contentNode.getProperty("hippo:docbase").getString();
                            String contentRoot = contentNode.getSession().getNodeByIdentifier(siteDocbase).getPath();
                            channel.setContentRoot(contentRoot);
                        }
                    }

                    this.channels.add(channel);
                }
            }
            //Get the channels from the child node.
            populateChannels(currNode, channel);
        }
    }

}
