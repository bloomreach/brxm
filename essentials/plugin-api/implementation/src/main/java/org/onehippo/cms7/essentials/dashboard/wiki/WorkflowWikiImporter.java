package org.onehippo.cms7.essentials.dashboard.wiki;

import java.util.Properties;

import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * We might eventually need a workflow importer
 * @version "$Id$"
 */
public class WorkflowWikiImporter implements Importer {

    private static Logger log = LoggerFactory.getLogger(WorkflowWikiImporter.class);


    @Override
    public void importAction(final Session session, final WikiStrategy strategy, final Properties properties) {

    }
}
