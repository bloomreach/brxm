package org.onehippo.cms7.essentials.dashboard.wiki;

import javax.jcr.Session;

/**
 * @version "$Id$"
 */
public interface Importer {

    public void importAction(final Session session, int amount, int offset, int maxSubFolder, int maxDocsPerFolder, int numberOfTranslations, String filesystemLocation, String siteContentBasePath, boolean addImages, final WikiStrategy strategy);

}
