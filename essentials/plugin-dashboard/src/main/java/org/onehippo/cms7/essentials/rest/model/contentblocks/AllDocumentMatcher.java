package org.onehippo.cms7.essentials.rest.model.contentblocks;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.onehippo.cms7.essentials.dashboard.utils.JcrMatcher;

/**
 * @version "$Id$"
 */
public class AllDocumentMatcher implements JcrMatcher {


    @Override
    public boolean matches(final Node node) throws RepositoryException {
        return true;
    }
}
