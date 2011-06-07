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
    private static final String HST_MOUNTPOINT_PROP = "hst:mountpoint";
    private List<Channel> channels;
    private JcrNodeModel configNodeModel;
    static String vhostNodeName;

    private void populateChannels(Node n) throws RepositoryException {
        boolean restMount = false;
        NodeIterator nodes = n.getNodes();
        while (nodes.hasNext()) {
            Node currNode = nodes.nextNode();
            if (currNode.isNodeType("hst:mount")) {
                if (currNode.hasProperty("hst:types")) {
                    Value[] hstTypes = currNode.getProperty("hst:types").getValues();
                    for (Value hstType : hstTypes) {
                        restMount = hstType.getString().contains("rest");//Ignore REST mounts
                    }
                }

                if (!restMount) {
                    Channel channel = new Channel(currNode.getName());
                    if (currNode.hasProperty("hst:type")) {
                        channel.setType(currNode.getProperty("hst:type").getString());
                    } else {
                        channel.setType("live");
                    }
                    this.channels.add(channel);
                }
            }
            populateChannels(currNode);

        }
    }

    public ChannelDataProvider(JcrNodeModel hstConfigNodeModel) {
        this.configNodeModel = hstConfigNodeModel;
        this.channels = new ArrayList<Channel>();
        Channel c;
        try {


            Node configNode = this.configNodeModel.getNode();
            NodeIterator rootChannelNodes = configNode.getNodes();

            while (rootChannelNodes.hasNext()) {
                Node n = rootChannelNodes.nextNode();
                //Get all the virtualhosts.
                if (n.isNodeType(HST_VIRTUALHOST_NT)) {
                    vhostNodeName = n.getName();
                    populateChannels(n);
                }
            }
        } catch (RepositoryException e) {
            log.error("Unable to get the list of channel nodes from repository :  " + e.getMessage(), e);
        }
    }

    @Override
    public Iterator iterator(int first, int count) {
        return channels.subList(first, first + count).iterator();
    }

    @Override
    public int size() {
        return channels.size();
    }

    @Override
    public IModel model(final Object object) {
        return new AbstractReadOnlyModel<Channel>() {
            @Override
            public Channel getObject() {
                return (Channel) object;
            }
        };
    }

    @Override
    public void detach() {
        this.configNodeModel.detach();

    }
}
