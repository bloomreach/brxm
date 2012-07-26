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
package org.hippoecm.repository.ocm;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jdo.spi.PersistenceCapable;

public class SimpleColumnResolver implements ColumnResolver {
    
    public SimpleColumnResolver() {
    }

    public PropertyDefinition resolvePropertyDefinition(Node node, String column, int propertyType) throws RepositoryException {
        if(node == null)
            return null;
        NodeType[] nodeTypes, mixinNodeTypes = node.getMixinNodeTypes();
        if (mixinNodeTypes != null) {
            nodeTypes = new NodeType[mixinNodeTypes.length + 1];
            System.arraycopy(mixinNodeTypes, 0, nodeTypes, 1, mixinNodeTypes.length);
        } else {
            nodeTypes = new NodeType[1];
        }
        nodeTypes[0] = node.getPrimaryNodeType();
        PropertyDefinition wildcardDefinition = null;
        for (NodeType nodeType : nodeTypes) {
            PropertyDefinition[] propertyDefinitions = nodeType.getPropertyDefinitions();
            if (propertyDefinitions != null) {
                for (PropertyDefinition definition : propertyDefinitions) {
                    if (definition.getName().equals(column)) {
                        if (propertyType != PropertyType.UNDEFINED) {
                            if (definition.getRequiredType() == PropertyType.UNDEFINED) {
                                if (wildcardDefinition == null || wildcardDefinition.getName().equals("*")) {
                                    wildcardDefinition = definition;
                                }
                            } else if (definition.getRequiredType() == propertyType) {
                                return definition;
                            }
                        } else {
                            return definition;
                        }
                    }
                }
            }
        }
        return wildcardDefinition;
    }

    @Override
    public Property resolveProperty(Node node, String column) throws RepositoryException {
        if (node.hasProperty(column)) {
            return node.getProperty(column);
        } else {
            return null;
        }
    }

    @Override
    public Node resolveNode(Node node, String column) throws RepositoryException {
        if (node.hasNode(column)) {
            return node.getNode(column);
        } else {
            return null;
        }
    }

    @Override
    public NodeLocation resolveNodeLocation(Node node, String column) throws RepositoryException {
        if (column.startsWith("/")) {
            node = node.getSession().getRootNode();
            column = column.substring(1);
        }
        int pos = column.lastIndexOf("/");
        if (pos >= 0) {
            String relPath = column.substring(0, pos);
            column = column.substring(pos + 1);
            node = (node.hasNode(relPath) ? node.getNode(relPath) : null);
        }
        return new NodeLocation(node, (node != null && node.hasNode(column) ? node.getNode(column) : null), column);
    }

    @Override
    public JcrOID resolveClone(Cloneable cloned) throws RepositoryException {
        if (cloned instanceof PersistenceCapable) {
            PersistenceCapable pc = (PersistenceCapable)cloned;
            Object objectId = pc.jdoGetObjectId();
            if (objectId instanceof JcrOID) {
                Object source = pc.jdoGetPersistenceManager().getObjectById(objectId);
                if (source != cloned) {
                    return (JcrOID)objectId;
                }
            }
        }
        return null;
    }

    @Override
    public Node copyClone(Node source, Cloneable cloned, Node target, String name, Node current) throws RepositoryException {
        Node child = null;
        if (current != null) {
            if (!current.getDefinition().allowsSameNameSiblings()) {
                current.remove();
            }
        }
        source.getSession().getWorkspace().copy(source.getPath(), target.getPath() + "/" + name);
        for (NodeIterator nodeIterator = target.getNodes(name); nodeIterator.hasNext();) {
            child = nodeIterator.nextNode();
        }
        return child;
    }
}
