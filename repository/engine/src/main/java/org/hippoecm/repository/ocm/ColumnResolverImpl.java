package org.hippoecm.repository.ocm;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import org.hippoecm.repository.DerivedDataEngine;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.ocm.JcrOID;

public class ColumnResolverImpl implements ColumnResolver {
    public ColumnResolverImpl() {
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
        return ((HippoWorkspace)node.getSession().getWorkspace()).getHierarchyResolver().getProperty(node, column);
    }

    @Override
    public Node resolveNode(Node node, String column) throws RepositoryException {
        return ((HippoWorkspace)node.getSession().getWorkspace()).getHierarchyResolver().getNode(node, column);
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
        Node child = ((HippoSession)source.getSession()).copy(source, target.getPath() + "/" + name);
        Document document = (Document)cloned;
        document.setIdentity(child.getIdentifier());
        return child;
    }

    @Override
    public NodeLocation resolveNodeLocation(Node node, String column) throws RepositoryException {
        HierarchyResolver.Entry last = new HierarchyResolver.Entry();
        Node child = (Node)((HippoWorkspace)node.getSession().getWorkspace()).getHierarchyResolver().getItem(node, column, false, last);
        return new ColumnResolver.NodeLocation(last.node, child, last.relPath);
    }
}
