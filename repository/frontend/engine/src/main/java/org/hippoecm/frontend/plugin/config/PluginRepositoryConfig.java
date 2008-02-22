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
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginRepositoryConfig implements PluginConfig {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(PluginRepositoryConfig.class);

    private JcrSessionModel jcrSession;
    private String basePath;

    public PluginRepositoryConfig(JcrSessionModel session, String base) {
        jcrSession = session;
        basePath = base;
    }

    public PluginDescriptor getPlugin(String pluginId) {
        PluginDescriptor result = null;
        try {
            Node pluginNode = getJcrSession().getRootNode().getNode(basePath + "/" + pluginId);
            if (pluginNode != null) {
                result = nodeToDescriptor(pluginNode);
            } else {
                log.error("No plugin node found for " + pluginId);
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return result;
    }

    protected Session getJcrSession() {
        return jcrSession.getSession();
    }

    protected PluginDescriptor nodeToDescriptor(Node pluginNode) throws RepositoryException {
        String classname = pluginNode.getProperty(HippoNodeType.HIPPO_RENDERER).getString();
        String pluginId = pluginNode.getName();
        PluginDescriptor descriptor = createDescriptor(pluginNode, pluginId, classname);

        // parse (optional) parameters
        if (pluginNode.hasNode(HippoNodeType.HIPPO_PARAMETERS)) {
            PropertyIterator iter = pluginNode.getNode(HippoNodeType.HIPPO_PARAMETERS).getProperties();
            while (iter.hasNext()) {
                Property property = iter.nextProperty();
                if (property.getName().equals("jcr:primaryType"))
                    continue;

                List<String> list = new LinkedList<String>();
                if (property.getDefinition().isMultiple()) {
                    for (Value value : property.getValues()) {
                        list.add(value.getString());
                    }
                } else {
                    list.add(property.getValue().getString());
                }
                descriptor.addParameter(property.getName(), list);
            }
        }
        return descriptor;
    }

    protected PluginDescriptor createDescriptor(Node node, String pluginId, String className) {
        return new Descriptor(node, pluginId, className);
    }

    protected class Descriptor extends PluginDescriptor {
        private static final long serialVersionUID = 1L;

        private String jcrPath;

        protected Descriptor(Node node, String pluginId, String className) {
            super(pluginId, className);
            try {
                this.jcrPath = node.getPath();
            } catch (RepositoryException ex) {
                log.error("Could not obtain plugin configuration node path: " + ex.getMessage());
            }
        }

        @Override
        public List<PluginDescriptor> getChildren() {
            List<PluginDescriptor> result = new ArrayList<PluginDescriptor>();
            try {
                Node pluginNode = getNode();
                if (pluginNode != null) {
                    NodeIterator it = pluginNode.getNodes();
                    while (it.hasNext()) {
                        Node child = it.nextNode();
                        if (child != null && child.isNodeType(HippoNodeType.NT_PLUGIN)) {
                            result.add(nodeToDescriptor(child));
                        }
                    }
                } else {
                    log.error("No plugin node found under " + jcrPath);
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
            return result;
        }

        protected Node getNode() throws RepositoryException {
            return (Node) getJcrSession().getItem(jcrPath);
        }
    }
}
