/*
 *  Copyright 2013-2014 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms7.hst.toolkit.addon.formdata;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormDataCleanupJob implements RepositoryJob {

    private static final Logger log = LoggerFactory.getLogger(FormDataCleanupJob.class);

    private static final String CONFIG_MINUTES_TO_LIVE = "minutestolive";
    private static final String CONFIG_BATCH_SIZE = "batchsize";
    private static final String CONFIG_EXCLUDE_PATHS = "excludepaths";

    private static String FORMDATA_QUERY = "SELECT * FROM hst:formdata ORDER BY hst:creationtime ASC";

    @Override
    public void execute(final RepositoryJobExecutionContext context) throws RepositoryException {
        log.info("Running form data cleanup job");
        final Session session = context.createSystemSession();
        try {
            long minutesToLive = Long.parseLong(context.getAttribute(CONFIG_MINUTES_TO_LIVE));
            long batchSize;
            try {
                batchSize = Long.parseLong(context.getAttribute(CONFIG_BATCH_SIZE));
            } catch (NumberFormatException e) {
                log.warn("Incorrect batch size '"+context.getAttribute(CONFIG_BATCH_SIZE)+"'. Setting default to 100");
                batchSize = 100;
            }
            String[] excludePaths = context.getAttribute(CONFIG_EXCLUDE_PATHS).split("\\|");
            removeOldFormData(minutesToLive, batchSize, excludePaths, session);
        } finally {
            session.logout();
        }
    }

    private void removeOldFormData(long minutesToLive,
                                   final long batchSize,
                                   final String[] excludePaths,
                                   final Session session) throws RepositoryException {
        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        final Query query = queryManager.createQuery(FORMDATA_QUERY, Query.SQL);
        final NodeIterator nodes = query.execute().getNodes();
        final long tooOldTimeStamp = System.currentTimeMillis() - minutesToLive * 60 * 1000L;
        int count = 0;
        outer:
        while (nodes.hasNext()) {
            try {
                final Node node = nodes.nextNode();
                if (node.getProperty("hst:creationtime").getDate().getTimeInMillis() > tooOldTimeStamp) {
                    break outer;
                }
                for (String path : excludePaths) {
                    if (!"".equals(path) && node.getPath().startsWith(path)) {
                        continue outer;
                    }
                }
                log.debug("Removing form data item at {}", node.getPath());
                remove(node, 2);
                if (count++ % batchSize == 0) {
                    session.save();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                }
            } catch (RepositoryException e) {
                log.error("Error while cleaning up form data", e);
            }
        }
        if (session.hasPendingChanges()) {
            session.save();
        }
        if (count > 0) {
            log.info("Done cleaning " + count + " items");
        } else {
            log.info("No timed out items");
        }
    }

    private void remove(final Node node, int ancestorsToRemove) throws RepositoryException {
        final Node parent = node.getParent();
        node.remove();
        if (ancestorsToRemove > 0 && parent != null && parent.getName().length() == 1 && parent.isNodeType("hst:formdatacontainer") && parent.getNodes().getSize() == 0) {
            remove(parent, ancestorsToRemove - 1);
        }
    }

}
