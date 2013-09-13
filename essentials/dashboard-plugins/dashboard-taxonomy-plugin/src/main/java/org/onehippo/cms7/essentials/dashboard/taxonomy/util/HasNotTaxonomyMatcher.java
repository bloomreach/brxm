package org.onehippo.cms7.essentials.dashboard.taxonomy.util;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.onehippo.cms7.essentials.dashboard.utils.JcrMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class HasNotTaxonomyMatcher implements JcrMatcher {

    private static Logger log = LoggerFactory.getLogger(HasNotTaxonomyMatcher.class);

    @Override
    public boolean matches(final Node typeNode) throws RepositoryException {
       return !new HasTaxonomyMatcher().matches(typeNode);
    }

}
