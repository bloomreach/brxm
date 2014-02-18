package org.onehippo.cms7.essentials.dashboard.relateddocs;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.onehippo.cms7.essentials.dashboard.utils.JcrMatcher;

/**
 * @version "$Id$"
 */
public class NotRelatedDocMatcher implements JcrMatcher {


    @Override
    public boolean matches(final Node node) throws RepositoryException {
        return !new HasRelatedDocMatcher().matches(node);
    }
}
