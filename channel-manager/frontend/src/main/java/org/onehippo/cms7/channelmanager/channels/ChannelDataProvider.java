package org.onehippo.cms7.channelmanager.channels;

import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ChannelDataProvider implements IDataProvider {

    private final static Logger log = LoggerFactory.getLogger(ChannelDataProvider.class);

    private static final String HST_VIRTUALHOST_NT = "hst:virtualhost";
    private static final String HST_MOUNT_NT = "hst:mount";
    private static final String HST_MOUNTPOINT_PROP = "hst:mountpoint";
    private static final String HST_TYPE_PROP = "hst:type";
    private static final String HST_TYPES_PROP = "hst:types";

    private List<Channel> channels;
    private JcrNodeModel configNodeModel;
    static String vhostNodeName;

    /**
     * Recursively gets the list of "channels" configured under a virtual host node.
     * <p/>
     * Ignores the mounts which are configured to be "rest" or "composer" either in hst:type or hst:types.
     *
     * @param node - the inital node to start with, must be a virtual host node.
     * @throws RepositoryException - In case cannot read required node/property from the repository.
     */
    private void populateChannels(Node node) throws RepositoryException {

        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            boolean ignoreMount = false;
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
                    Channel channel = new Channel(currNode.getName());
                    if (currNode.hasProperty(HST_TYPE_PROP)) {
                        String mountType = currNode.getProperty(HST_TYPE_PROP).getString();
                        if (mountType.contains("preview")) {
                            channel.setType(mountType);
                        }
                    } else {
                        channel.setType("live");
                    }

                    this.channels.add(channel);
                }
            }
            //Get the channels from the child node.
            populateChannels(currNode);
        }
    }

    /**
     * Create ChannelDataProvider with the {@link Channel}s.
     *
     * @param hstHostGroupNodeModel - the JcrNodeModel for hst host-group.
     */
    public ChannelDataProvider(JcrNodeModel hstHostGroupNodeModel) {
        this.configNodeModel = hstHostGroupNodeModel;
        this.channels = new ArrayList<Channel>();
        Channel c;
        try {
            Node configNode = this.configNodeModel.getNode();
            NodeIterator rootChannelNodes = configNode.getNodes();

            while (rootChannelNodes.hasNext()) {
                Node n = rootChannelNodes.nextNode();
                //Get only the virtualhost nodess.
                if (n.isNodeType(HST_VIRTUALHOST_NT)) {
                    vhostNodeName = n.getName();
                    populateChannels(n);
                }
            }
        } catch (RepositoryException e) {
            log.error("Unable to get the list of channel nodes from repository :  " + e.getMessage(), e);
        }
    }

    /**
     * @inheritDoc
     */
    @Override
    public Iterator iterator(int first, int count) {
        return channels.subList(first, first + count).iterator();
    }

    /**
     * @inheritDoc
     */
    @Override
    public int size() {
        return channels.size();
    }

    /**
     * Returns readonly {@link Channel} model.
     *
     * @param object
     * @return
     */
    @Override
    public IModel model(final Object object) {
        return new AbstractReadOnlyModel<Channel>() {
            @Override
            public Channel getObject() {
                return (Channel) object;
            }
        };
    }

    /**
     * Detaches the configuration node model.
     */
    @Override
    public void detach() {
        this.configNodeModel.detach();

    }
}
