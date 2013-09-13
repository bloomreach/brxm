package org.onehippo.cms7.essentials.dashboard.relateddocs;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.onehippo.cms7.essentials.dashboard.utils.JcrMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class NotRelatedDocMatcher implements JcrMatcher {

    private static Logger log = LoggerFactory.getLogger(NotRelatedDocMatcher.class);


    @Override
    public boolean matches(final Node typeNode) throws RepositoryException {
        return !new HasRelatedDocMatcher().matches(typeNode);
    }
}
