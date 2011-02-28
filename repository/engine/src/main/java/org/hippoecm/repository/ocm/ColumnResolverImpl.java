package org.hippoecm.repository.ocm;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.hippoecm.repository.DerivedDataEngine;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.ocm.JcrOID;

public class ColumnResolverImpl implements ColumnResolver {
    public ColumnResolverImpl() {
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
