package org.hippoecm.repository.ocm.fieldmanager;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jdo.spi.PersistenceCapable;
import org.hippoecm.repository.ocm.JcrOID;

public class SimpleColumnResolver implements ColumnResolver {
    public SimpleColumnResolver() {
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
