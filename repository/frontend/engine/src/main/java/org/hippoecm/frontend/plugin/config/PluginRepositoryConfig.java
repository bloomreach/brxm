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
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.wicket.Session;
import org.hippoecm.frontend.UserSession;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.repository.api.HippoNodeType;

public class PluginRepositoryConfig implements PluginConfig {
    private static final long serialVersionUID = 1L;

    private static final String pluginConfigRoot = "hippo:configuration/hippo:frontend/default";
    private static final String rootPluginId = "rootPlugin";

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
        String xpath = pluginConfigRoot + "//" + pluginId;
        UserSession session = (UserSession) Session.get();
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
        String classname = pluginNode.getProperty(HippoNodeType.HIPPO_RENDERER).getString();
        String pluginId = pluginNode.getName();
        return new PluginDescriptor(pluginId, classname);
    }

}
