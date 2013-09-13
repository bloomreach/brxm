package org.onehippo.cms7.essentials.dashboard.utils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public interface JcrMatcher {

    public boolean matches(final Node typeNode) throws RepositoryException;
}
