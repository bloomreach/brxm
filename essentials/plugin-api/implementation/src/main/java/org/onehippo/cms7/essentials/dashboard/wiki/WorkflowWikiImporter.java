package org.onehippo.cms7.essentials.dashboard.wiki;

import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class WorkflowWikiImporter implements Importer {

    private static Logger log = LoggerFactory.getLogger(WorkflowWikiImporter.class);

    @Override
    public void importAction(final Session session, final int amount, final int offset, final int maxSubFolder, final int maxDocsPerFolder, final int numberOfTranslations, final String filesystemLocation, final String siteContentBasePath, final boolean addImages, final WikiStrategy strategy) {

    }
}
