package org.onehippo.cms7.essentials.dashboard.utils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Interface for iterating through a set of nodes to match. Used currently in org.onehippo.cms7.essentials.dashboard.utils.HippoNodeUtils#prototypes
 * @version "$Id$"
 */
public interface JcrMatcher {

    /**
     * Does the node match according to the rules which it implements
     * @param node
     * @return
     * @throws RepositoryException
     */
    public boolean matches(final Node node) throws RepositoryException;
}
