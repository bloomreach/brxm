/*
 * Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.events;

import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyValueGetterImpl;
import org.hippoecm.repository.util.ValueGetter;
import org.onehippo.cms7.event.HippoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.util.JcrUtils.getProperties;


class Broadcaster implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(Broadcaster.class);
    private static final long DEFAULT_TIMESTAMP = -1L;

    private static final String HIPPOLOG_TIMESTAMP = "hippolog:timestamp";

    private final Session session;
    private final BroadcastService broadcastService;
    private final ValueGetter<Property,?> propertyValueGetter;

    private volatile boolean keepRunning = true;

    private long queryLimit;
    private long maxEventAgeHours;

    public Broadcaster(final Session session, final BroadcastService broadcastService) {
        this.session = session;
        this.broadcastService = broadcastService;
        this.propertyValueGetter = new PropertyValueGetterImpl();
    }

    public void setQueryLimit(long limit) {
        this.queryLimit = limit;
    }

    public void setMaxEventAge(final long maxEventAge) {
        this.maxEventAgeHours = maxEventAge;
    }

    public void run() {
        while (keepRunning) {
            log.debug("Polling");
            final BroadcastJob job = broadcastService.getNextJob();
            if (job != null) {
                try {
                    final long globalLastProcessedItem = job.getGlobalLastProcessed();

                    final List<Node> logItems = new LinkedList<>();
                    boolean possiblyEventsLeftToProcess = getNextLogNodes(globalLastProcessedItem, logItems);

                    if (logItems.isEmpty()) {
                        log.debug("No pending log items to process");
                    } else {
                        long timeStamp = DEFAULT_TIMESTAMP;
                        for (Node logItem : logItems) {
                            try {
                                log.debug("Publishing event {}", JcrUtils.getNodePathQuietly(logItem));
                                final HippoEvent event = createEvent(logItem);
                                job.publish(event);
                                timeStamp = event.timestamp();
                            } catch (RepositoryException | RuntimeException re) {
                                log.warn("Unable to process logItem at {}", JcrUtils.getNodePathQuietly(logItem), re);
                            }
                        }
                        if (timeStamp > DEFAULT_TIMESTAMP) {
                            job.setLastProcessed(timeStamp);
                        }
                    }
                    if (possiblyEventsLeftToProcess) {
                        // unless stopped (keepRunning==false), continue while loop
                        continue;
                    }
                } catch (Exception e) {
                    log.warn("Error during execution of Broadcast Job", e);
                }
            }
            break;
        }
    }

    void stop() {
        keepRunning = false;
    }

    private boolean getNextLogNodes(long lastItem, List<Node> nodes) throws RepositoryException {
        log.debug("lastItem processed item: {}", lastItem);

        try {

            if (maxEventAgeHours > -1) {
                final long maxEventAgeTimestamp = System.currentTimeMillis() - maxEventAgeHours * 60 * 60 * 1000;
                if (lastItem < maxEventAgeTimestamp) {
                    log.info("max event age timestamp is passed. Set lastItem to max event age timestamp");
                    lastItem = maxEventAgeTimestamp;
                }
            }

            final String statement = "SELECT * FROM hippolog:item WHERE hippolog:timestamp > " + lastItem + " ORDER BY hippolog:timestamp ASC";
            Query query = session.getWorkspace().getQueryManager().createQuery(statement, Query.SQL);

            if (queryLimit > -1) {
                log.debug("setting query limit to {}", queryLimit);
                query.setLimit(queryLimit);
            }

            long resultCount = 0;

            QueryResult queryResult = query.execute();
            NodeIterator nodeIterator = queryResult.getNodes();

            // iterate through results (which are in reverse chronological order)
            // until timestamp is older than what we are interested in
            for (Node logNode : new NodeIterable(nodeIterator)) {
                resultCount++;
                // add log node if valid and has timestamp higher than lastItem (which should always be the case since
                // we query on hippolog:timestamp > " + lastItem + "
                if (logNode.hasProperty(HIPPOLOG_TIMESTAMP)) {
                    long timeStamp = logNode.getProperty(HIPPOLOG_TIMESTAMP).getLong();
                    if (timeStamp > lastItem) {
                        nodes.add(logNode);
                    } else {
                        break;
                    }
                }
            }

            // return true if there are possibly Events Left To Process
            return queryLimit > -1 && resultCount == queryLimit;

        } catch (Exception e) {
            session.refresh(false);
            throw e;
        }
    }

    /**
     * This method is responsible creating hippo event from log node. It will parse all the properties of log item and
     * populate in hippoevent
     *
     * @param logNode the logged event node
     * @return HippoEvent with the workflow information
     */
    private HippoEvent createEvent(Node logNode) throws RepositoryException {
        final HippoEvent<?> event = new HippoEvent("repository");
        for (Property property : getProperties(logNode)) {
            final String key = getKeyFromPropertyName(property.getName());
            final Object value = propertyValueGetter.getValue(property);
            event.set(key, value);
        }
        return event;
    }

    private String getKeyFromPropertyName(String name) {
        int colonIndex = name.indexOf(':');
        return colonIndex != -1 ? name.substring(colonIndex + 1) : name;
    }
}
