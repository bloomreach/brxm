package org.onehippo.repository.embeddedrmi;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.hippoecm.repository.api.HierarchyResolver;

public class RemoteHierarchyResolver implements HierarchyResolver {
    public Item getItem(Node ancestor, String path, boolean isProperty, Entry last) throws InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Item getItem(Node ancestor, String path) throws InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Property getProperty(Node node, String field) throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Property getProperty(Node node, String field, Entry last) throws RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Node getNode(Node node, String field) throws InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
