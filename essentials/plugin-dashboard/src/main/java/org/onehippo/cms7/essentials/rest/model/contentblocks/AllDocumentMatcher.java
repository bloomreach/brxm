package org.onehippo.cms7.essentials.rest.model.contentblocks;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.onehippo.cms7.essentials.dashboard.utils.JcrMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class AllDocumentMatcher implements JcrMatcher {

    private static Logger log = LoggerFactory.getLogger(AllDocumentMatcher.class);

    @Override
    public boolean matches(final Node node) throws RepositoryException {
        return true;
    }
}
