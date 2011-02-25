package org.hippoecm.repository.ocm.fieldmanager;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.hippoecm.repository.ocm.JcrOID;

public interface ColumnResolver {
    public Property resolveProperty(Node node, String column) throws RepositoryException;

    public Node resolveNode(Node node, String column) throws RepositoryException;

    public ColumnResolver.NodeLocation resolveNodeLocation(Node node, String column) throws RepositoryException;

    public JcrOID resolveClone(Cloneable cloned) throws RepositoryException;

    public Node copyClone(Node source, Cloneable cloned, Node target, String name, Node current) throws RepositoryException;

    public class NodeLocation {
        Node parent;
        Node child;
        String name;

        public NodeLocation(Node parent, Node child, String name) {
            this.parent = parent;
            this.child = child;
            this.name = name;
        }
    }
}
