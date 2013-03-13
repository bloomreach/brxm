package org.hippoecm.frontend.plugins.standards.list.comparators;

import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;

public class NodeNameComparator extends NodeComparator {
    private static final long serialVersionUID = 1L;

    @Override
    public int compare(final JcrNodeModel node1, final JcrNodeModel node2) {
        try {
            return String.CASE_INSENSITIVE_ORDER.compare(node1.getNode().getName(), node2.getNode().getName());
        } catch (RepositoryException ignore) {
        }

        return 0;
    }
}
