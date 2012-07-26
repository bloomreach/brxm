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

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;
import org.hippoecm.repository.DerivedDataEngine;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.impl.SessionDecorator;
import org.hippoecm.repository.ocm.JcrOID;

public class ColumnResolverImpl implements ColumnResolver {
    
    private HierarchyResolver resolver;
    
    public ColumnResolverImpl(HierarchyResolver resolver) {
        this.resolver = resolver;
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
        if (node == null) {
            return null;
        }
        return resolver.getProperty(node, column);
    }

    @Override
    public Node resolveNode(Node node, String column) throws RepositoryException {
        if (node == null) {
            return null;
        }
        return resolver.getNode(node, column);
    }

    @Override
    public JcrOID resolveClone(Cloneable cloned) throws RepositoryException {
        Document original = ((Document)cloned).isCloned();
        if (original != null) {
            return new JcrOID(original.getIdentity(), cloned.getClass().getName());
        } else {
            return null;
        }
    }

    @Override
    public Node copyClone(Node source, Cloneable cloned, Node target, String name, Node current) throws RepositoryException {
        if (current != null) {
            DerivedDataEngine.removal(current);
            current.remove();
        }
        Node child = target.addNode(name, source.getPrimaryNodeType().getName());
        SessionDecorator.copy(source, child);
        Document document = (Document)cloned;
        document.setIdentity(child.getIdentifier());
        return child;
    }

    @Override
    public NodeLocation resolveNodeLocation(Node node, String column) throws RepositoryException {
        HierarchyResolver.Entry last = new HierarchyResolver.Entry();
        Node child = (Node) resolver.getItem(node, column, false, last);
        return new ColumnResolver.NodeLocation(last.node, child, last.relPath);
    }
}
