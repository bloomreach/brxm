package org.onehippo.cms7.essentials.dashboard.contentblocks.matcher;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.onehippo.cms7.essentials.dashboard.utils.JcrMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class HasProviderMatcher implements JcrMatcher {

    private static Logger log = LoggerFactory.getLogger(HasProviderMatcher.class);

    @Override
    public boolean matches(final Node node) throws RepositoryException {
        return (node.hasProperty("cbitem") && node.getProperty("cbitem").getBoolean());
    }
}
