/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugin.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.wicket.Session;
import org.hippoecm.frontend.UserSession;
import org.hippoecm.frontend.plugin.EventChannel;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.repository.api.HippoNodeType;

public class PluginRepositoryConfig implements PluginConfig {
    private static final long serialVersionUID = 1L;

    private static final String rootPluginId = "rootPlugin";

    private final static String PLUGIN_RENDERER = "hippo:renderer";
    private final static String INCOMING_CHANNELS = "hippo:incoming";
    private final static String OUTGOING_CHANNELS = "hippo:outgoing";

    public PluginRepositoryConfig() {
    }

    public PluginDescriptor getRoot() {
        try {
            Node rootPluginConfigNode = lookupConfigNode(rootPluginId);
            return nodeToDescriptor(rootPluginConfigNode);
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public List getChildren(PluginDescriptor pluginDescriptor) {
        List result = new ArrayList();
        try {
            Node pluginNode = lookupConfigNode(pluginDescriptor.getPluginId());
            NodeIterator it = pluginNode.getNodes();
            while (it.hasNext()) {
                Node child = it.nextNode();
                if (child != null) {
                    result.add(nodeToDescriptor(child));
                }
            }
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    public PluginDescriptor getPlugin(String pluginId) {
        PluginDescriptor result = null;
        try {
            Node pluginNode = lookupConfigNode(pluginId);
            result = nodeToDescriptor(pluginNode);
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    // Privates

    private Node lookupConfigNode(String pluginId) throws RepositoryException {
        UserSession session = (UserSession) Session.get();

        String xpath = HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.FRONTEND_PATH + "/"
                + session.getFrontendApp() + "//" + pluginId;

        QueryManager queryManager = session.getJcrSession().getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(xpath, Query.XPATH);
        QueryResult result = query.execute();
        NodeIterator iter = result.getNodes();
        if (iter.getSize() > 1) {
            throw new IllegalStateException("Plugin id's must be unique within a configuration, but " + pluginId
                    + " is configured more than once");
        }
        return iter.hasNext() ? iter.nextNode() : null;
    }

    private PluginDescriptor nodeToDescriptor(Node pluginNode) throws RepositoryException {
        String classname = pluginNode.getProperty(PLUGIN_RENDERER).getString();
        String pluginId = pluginNode.getName();
        Set<EventChannel> incoming = getChannels(pluginNode, INCOMING_CHANNELS);
        Set<EventChannel> outgoing = getChannels(pluginNode, OUTGOING_CHANNELS);
        return new PluginDescriptor(pluginId, classname, incoming, outgoing);
    }

    private Set<EventChannel> getChannels(Node pluginNode, String type) throws RepositoryException {
        Set<EventChannel> result = new HashSet<EventChannel>();
        if (pluginNode.hasProperty(type)) {
            Value[] channels = pluginNode.getProperty(type).getValues();
            for (int i = 0; i < channels.length; i++) {
                EventChannel channel = new EventChannel(channels[i].getString());
                result.add(channel);
            }
        }
        return result;
    }
}
