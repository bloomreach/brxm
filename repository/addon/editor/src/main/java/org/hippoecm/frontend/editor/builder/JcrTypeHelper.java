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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.frontend.plugins.standardworkflow.types.JcrTypeDescriptor;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrTypeHelper extends NodeModelWrapper {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(JcrTypeHelper.class);

    public JcrTypeHelper(JcrNodeModel nodeModel) {
        super(nodeModel);
    }

    public JcrTypeDescriptor getTypeDescriptor(String version) {
        try {
            Node baseNode = getNodeModel().getNode();
            Node ntHandle = baseNode.getNode(HippoNodeType.HIPPO_NODETYPE);
            NodeIterator ntIter = ntHandle.getNodes(HippoNodeType.HIPPO_NODETYPE);
            while (ntIter.hasNext()) {
                Node typeNode = ntIter.nextNode();
                if (typeNode.isNodeType(HippoNodeType.NT_REMODEL)) {
                    String ntVersion = typeNode.getProperty(HippoNodeType.HIPPO_REMODEL).getString();
                    if (version.equals(ntVersion)) {
                        String typeName;
                        if (typeNode.hasProperty(HippoNodeType.HIPPO_TYPE)) {
                            typeName = typeNode.getProperty(HippoNodeType.HIPPO_TYPE).getString();
                        } else {
                            typeName = baseNode.getName();
                        }
                        
                        return new JcrTypeDescriptor(new JcrNodeModel(typeNode), baseNode.getName(), typeName);
                    }
                }
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

    public JcrNodeModel getPrototype() {
        try {
            NodeIterator iter = getNodeModel().getNode().getNode(HippoNodeType.HIPPO_PROTOTYPE).getNodes(
                    HippoNodeType.HIPPO_PROTOTYPE);
            while (iter.hasNext()) {
                Node node = iter.nextNode();
                if (node.isNodeType(HippoNodeType.NT_REMODEL)) {
                    if (node.getProperty(HippoNodeType.HIPPO_REMODEL).getString().equals("draft")) {
                        return new JcrNodeModel(node);
                    }
                }
            }
            throw new ItemNotFoundException("draft version of prototype was not found");
        } catch(RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

}
