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

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.repository.api.HippoNodeType;

public class PluginRepositoryConfig implements PluginConfig {
    private static final long serialVersionUID = 1L;

    private static final String pluginConfigRoot = "configuration/frontend/default";
    private static final String rootPluginId = "rootPlugin";

    private JcrNodeModel pluginConfigNodeModel;

    public PluginRepositoryConfig() throws RepositoryException {
        JcrNodeModel rootModel = JcrNodeModel.getRootModel();
        pluginConfigNodeModel = new JcrNodeModel(null, rootModel.getNode().getNode(pluginConfigRoot));
    }

    public PluginDescriptor getRoot() {
        try {
            return getPluginDescriptor(pluginConfigNodeModel.getNode().getNode(rootPluginId));
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public List getChildren(PluginDescriptor pluginDescriptor) {
        List result = new ArrayList();
        try {
            Node pluginNode = getPluginNode(pluginDescriptor);
            NodeIterator it = pluginNode.getNodes();
            while (it.hasNext()) {
                Node child = it.nextNode();
                if (child != null) {
                    result.add(getPluginDescriptor(child));
                }
            }
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }
    
    public PluginDescriptor getPlugin(String wicketPath)  {
        wicketPath = wicketPath.substring(2);
        wicketPath = wicketPath.replaceAll(":", "/");
        PluginDescriptor result = null;
        try {
            Node pluginNode = pluginConfigNodeModel.getNode().getNode(wicketPath);
            result = getPluginDescriptor(pluginNode);
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    //conversion of wicket path to jcr path and vv.
    //jcr path   :  /some/path
    //wicket path:  0:some:path

    private Node getPluginNode(PluginDescriptor pluginDescriptor) throws RepositoryException {
        String path = pluginDescriptor.getPath().substring(2);
        path = path.replaceAll(":", "/");
        return pluginConfigNodeModel.getNode().getNode(path);
    }

    private PluginDescriptor getPluginDescriptor(Node pluginNode) throws RepositoryException {
        String classname = pluginNode.getProperty(HippoNodeType.HIPPO_RENDERER).getString();
        String path = pluginNode.getPath().substring(1);
        path = path.replaceFirst(pluginConfigRoot, "0");
        path = path.replaceAll("/", ":");
        return new PluginDescriptor(path, classname);
    }

}
