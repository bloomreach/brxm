/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.logging;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.repository.api.HippoNodeIterator;
import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventLogCleanupJob implements RepositoryJob {

    private static final Logger log = LoggerFactory.getLogger(EventLogCleanupJob.class);

    private static final String ITEMS_QUERY = "SELECT * FROM hippolog:item ORDER BY hippolog:timestamp ASC";

    private static final String CONFIG_MINUTESTOLIVE = "minutestolive";
    private static final String CONFIG_MAXITEMS = "maxitems";

    private static final long DEFAULT_MAXITEMS = -1;
    private static final long DEFAULT_MINUTESTOLIVE = -1;

    @Override
    public void execute(final RepositoryJobExecutionContext context) throws RepositoryException {
        final Session session = context.createSystemSession();
        try {
            log.info("Running event log cleanup job");
            final long maxItems = parseLongAttribute(CONFIG_MAXITEMS, DEFAULT_MAXITEMS, context);
            removeTooManyItems(maxItems, session);
            final long minutesToLive = parseLongAttribute(CONFIG_MINUTESTOLIVE, DEFAULT_MINUTESTOLIVE, context);
            removeTimedOutItems(minutesToLive, session);
        } finally {
            session.logout();
        }
    }

    private long parseLongAttribute(final String attrName, final long defaultValue, final RepositoryJobExecutionContext context) {
        final String attrValue = context.getAttribute(attrName);
        long value = defaultValue;
        if (!StringUtils.isBlank(attrValue)) {
            try {
                value = Long.valueOf(attrValue);
            } catch (NumberFormatException e) {
                log.warn("'{}' configuration attribute cannot be parsed. Expected a long but was '{}'", attrName, attrValue);
            }
        }
        return value;
    }

    private void removeTooManyItems(long maxitems, Session session) throws RepositoryException {
        if (maxitems == -1) {
            log.info("No maxitems configured");
        } else {
            log.info("Truncating event log to {} most recent items", maxitems);
            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final Query query = queryManager.createQuery(ITEMS_QUERY, Query.SQL);
            final NodeIterator nodes = query.execute().getNodes();
            final long totalSize = ((HippoNodeIterator) nodes).getTotalSize();
            final long cleanupSize = totalSize - maxitems;
            for (int i = 0; i < cleanupSize; i++) {
                try {
                    Node node = nodes.nextNode();
                    log.debug("Removing event log item at {}", node.getPath());
                    remove(node);
                    if (i % 10 == 0) {
                        session.save();
                        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                    }
                } catch (RepositoryException e) {
                    log.error("Error while cleaning up event log", e);
                }
            }
            if (session.hasPendingChanges()) {
                session.save();
            }
            if (cleanupSize > 0) {
                log.info("Done cleaning {} items", cleanupSize);
            } else {
                log.info("No excessive amount of items");
            }
        }
    }

    private void removeTimedOutItems(long minutestolive, Session session) throws RepositoryException {
        if (minutestolive == -1) {
            log.info("No minutestolive configured");
        } else {
            final long timeoutTimestamp = System.currentTimeMillis() - minutestolive*1000*60;
            log.info("Removing items from event log from before {}", SimpleDateFormat.getDateTimeInstance().format(new Date(timeoutTimestamp)));
            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final Query query = queryManager.createQuery(ITEMS_QUERY, Query.SQL);
            final NodeIterator nodes = query.execute().getNodes();
            int count = 0;
            while (nodes.hasNext()) {
                try {
                    final Node node = nodes.nextNode();
                    if (node.getProperty("hippolog:timestamp").getLong() > timeoutTimestamp) {
                        break;
                    }
                    log.debug("Removing event log item at {}", node.getPath());
                    remove(node);
                    if (count++ % 10 == 0) {
                        session.save();
                        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                    }
                } catch (RepositoryException e) {
                    log.error("Error while cleaning up event log", e);
                }
            }
            if (session.hasPendingChanges()) {
                session.save();
            }
            if (count > 0) {
                log.info("Done cleaning {} items", count);
            } else {
                log.info("No timed out items");
            }
        }
    }

    private void remove(final Node node) throws RepositoryException {
        final Node parent = node.getParent();
        node.remove();
        if (parent != null && parent.getName().length() == 1 && parent.isNodeType("hippolog:folder") && parent.getNodes().getSize() == 0) {
            remove(parent);
        }
    }

}
