/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.editor.builder;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.Session;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.types.JcrTypeDescriptor;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrTypeHelper extends NodeModelWrapper {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrTypeHelper.class);

    public JcrTypeHelper(JcrNodeModel nodeModel) {
        super(nodeModel);
    }

    public String getName() {
        try {
            Node baseNode = getNodeModel().getNode();
            return baseNode.getParent().getName() + ":" + baseNode.getName();
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    public JcrTypeDescriptor getTypeDescriptor() {
        try {
            Node baseNode = getNodeModel().getNode();

            String prefix = baseNode.getParent().getName();
            String name;
            String uri;
            if ("system".equals(prefix)) {
                name = baseNode.getName();
                uri = "internal";
            } else {
                name = prefix + ":" + baseNode.getName();
                uri = getUri(prefix);
            }

            Node ntHandle = baseNode.getNode(HippoNodeType.HIPPO_NODETYPE);
            NodeIterator ntIter = ntHandle.getNodes(HippoNodeType.HIPPO_NODETYPE);
            Node typeNode = null;
            while (ntIter.hasNext()) {
                Node node = ntIter.nextNode();
                if (!node.isNodeType(HippoNodeType.NT_REMODEL)) {
                    typeNode = node;
                    break;
                } else {
                    if (node.getProperty(HippoNodeType.HIPPO_URI).getString().equals(uri)) {
                        typeNode = node;
                    }
                }
            }

            if (typeNode != null) {
                String typeName;
                if (typeNode.hasProperty(HippoNodeType.HIPPO_TYPE)) {
                    typeName = typeNode.getProperty(HippoNodeType.HIPPO_TYPE).getString();
                } else {
                    typeName = name;
                }

                return new JcrTypeDescriptor(new JcrNodeModel(typeNode), name, typeName);
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    public JcrNodeModel getTemplate() {
        try {
            Node typeNode = getNodeModel().getNode();
            if (typeNode.hasNode(HippoNodeType.HIPPO_TEMPLATE)) {
                Node node = typeNode.getNode(HippoNodeType.HIPPO_TEMPLATE);
                NodeIterator nodes = node.getNodes(HippoNodeType.HIPPO_TEMPLATE);
                while (nodes.hasNext()) {
                    Node template = nodes.nextNode();
                    if (template.isNodeType("frontend:plugincluster")) {
                        return new JcrNodeModel(template);
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    public JcrNodeModel storeTemplate(IClusterConfig cluster) {
        try {
            Node typeNode = getNodeModel().getNode();
            Node node;
            if (!typeNode.hasNode(HippoNodeType.HIPPO_TEMPLATE)) {
                node = typeNode.addNode(HippoNodeType.HIPPO_TEMPLATE, HippoNodeType.NT_HANDLE);
            } else {
                node = typeNode.getNode(HippoNodeType.HIPPO_TEMPLATE);
            }
            node = node.addNode(HippoNodeType.HIPPO_TEMPLATE, "frontend:plugincluster");
            JcrNodeModel templateModel = new JcrNodeModel(node);

            JcrClusterConfig jcrConfig = new JcrClusterConfig(templateModel);
            for (Map.Entry entry : (Set<Map.Entry>) ((Map) cluster).entrySet()) {
                jcrConfig.put(entry.getKey(), entry.getValue());
            }

            for (IPluginConfig plugin : cluster.getPlugins()) {
                String name = UUID.randomUUID().toString();
                Node child = node.addNode(name, "frontend:plugin");
                JcrPluginConfig pluginConfig = new JcrPluginConfig(new JcrNodeModel(child));
                for (Map.Entry entry : (Set<Map.Entry>) ((Map) plugin).entrySet()) {
                    pluginConfig.put(entry.getKey(), entry.getValue());
                }
            }

            node.setProperty("frontend:services", getValues(cluster.getServices()));
            node.setProperty("frontend:references", getValues(cluster.getReferences()));
            node.setProperty("frontend:properties", getValues(cluster.getProperties()));

            return templateModel;

        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    public JcrNodeModel getPrototype() {
        try {
            Node baseNode = getNodeModel().getNode();
            String prefix = baseNode.getParent().getName();

            Node prototype = null;
            NodeIterator iter = getNodeModel().getNode().getNode(HippoNodeType.HIPPO_PROTOTYPE).getNodes(
                    HippoNodeType.HIPPO_PROTOTYPE);
            while (iter.hasNext()) {
                Node node = iter.nextNode();
                if (node.isNodeType("nt:unstructured")) {
                    return new JcrNodeModel(node);
                }
                String nt = node.getPrimaryNodeType().getName();
                if (prefix.equals(nt.substring(0, nt.indexOf(':')))) {
                    prototype = node;
                }
            }
            if (prototype != null) {
                return new JcrNodeModel(prototype);
            }
            throw new ItemNotFoundException("no prototype found");
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    private Value[] getValues(List<String> list) throws RepositoryException {
        Value[] values = new Value[list.size()];
        int i = 0;
        for (String override : list) {
            values[i++] = ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue(override);
        }
        return values;
    }

    private String getUri(String prefix) throws NamespaceException, RepositoryException {
        NamespaceRegistry nsReg = ((UserSession) Session.get()).getJcrSession().getWorkspace().getNamespaceRegistry();
        String[] prefixes = nsReg.getPrefixes();
        for (String pref : prefixes) {
            if (pref.equals(prefix)) {
                return nsReg.getURI(prefix);
            }
        }
        return null;
    }

}
