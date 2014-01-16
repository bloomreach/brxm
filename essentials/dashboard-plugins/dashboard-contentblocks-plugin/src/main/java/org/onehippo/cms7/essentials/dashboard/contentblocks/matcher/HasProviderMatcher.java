package org.onehippo.cms7.essentials.dashboard.contentblocks.matcher;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.onehippo.cms7.essentials.dashboard.utils.JcrMatcher;

/**
 * @version "$Id$"
 */
public class HasProviderMatcher implements JcrMatcher {

    @Override
    public boolean matches(final Node node) throws RepositoryException {
        return (node.hasProperty("cbitem") && node.getProperty("cbitem").getBoolean());
    }
}
