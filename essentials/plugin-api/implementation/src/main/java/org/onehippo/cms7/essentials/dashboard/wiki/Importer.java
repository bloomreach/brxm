package org.onehippo.cms7.essentials.dashboard.wiki;

import java.util.Properties;

import javax.jcr.Session;

/**
 * @version "$Id$"
 */
public interface Importer {

    public void importAction(final Session session, final WikiStrategy strategy, Properties properties);

}
