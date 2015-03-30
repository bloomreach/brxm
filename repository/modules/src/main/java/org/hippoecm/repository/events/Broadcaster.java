/*
 * Copyright 2012-2014 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
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

    private class JobRunner {

        private final BroadcastJob job;
        private boolean processedEvents = false;

        private JobRunner(BroadcastJob job) {
            this.job = job;
        }

        private void run() {
            try {
                long lastProcessItem = job.getLastProcessed();
                String eventCategory = job.getEventCategory();
                log.debug("Getting latest log items in category {} starting from {}", eventCategory, lastProcessItem);

                List<Node> logItems = getNextLogNodes(lastProcessItem, eventCategory);
                long timeStamp = processEvents(job, logItems);

                if (timeStamp > -1L) {
                    job.setLastProcessed(timeStamp);
                }
                processedEvents = (logItems.size() > 0);
            } catch (Exception e) {
                log.warn("Error during running thread", e);
            }
        }

        private Long processEvents(final BroadcastJob job, final List<Node> logItems) {
            Long timeStamp = DEFAULT_TIMESTAMP;
            if (logItems.isEmpty()) {
                log.debug("No pending log items to process");
                return DEFAULT_TIMESTAMP;
            }
            for (Node logItem : logItems) {
                try {
                    log.debug("Publishing event {} to channel {}", JcrUtils.getNodePathQuietly(logItem), job.getChannelName());
                    final HippoEvent event = createEvent(logItem);
                    job.publish(event);
                    timeStamp = event.timestamp();
                } catch (RepositoryException | RuntimeException re) {
                    log.warn("Unable to process logItem at {}", JcrUtils.getNodePathQuietly(logItem), re);
                }
            }
            return timeStamp;
        }

        private boolean wereEventsProcessed() {
            return processedEvents;
        }
    }

    private final Session session;
    private final BroadcastService broadcastService;
    private final ValueGetter<Property,?> propertyValueGetter;

    private volatile boolean keepRunning = true;

    private long queryLimit;
    private long maxEventAge;

    public Broadcaster(final Session session, final BroadcastService broadcastService) {
        this.session = session;
        this.broadcastService = broadcastService;
        this.propertyValueGetter = new PropertyValueGetterImpl();
    }

    public void setQueryLimit(long limit) {
        this.queryLimit = limit;
    }

    public void setMaxEventAge(final long maxEventAge) {
        this.maxEventAge = maxEventAge;
    }

    public void run() {
        while (keepRunning) {
            log.debug("Polling");
            final BroadcastJob job = broadcastService.getNextJob();
            if (job != null) {
                log.debug("Found job: {}", job.getChannelName());
                JobRunner runner = new JobRunner(job);
                runner.run();
                if (!runner.wereEventsProcessed()) {
                    break;
                }
            } else {
                break;
            }
        }
    }

    void stop() {
        keepRunning = false;
    }

    private List<Node> getNextLogNodes(long lastItem, final String eventCategory) throws RepositoryException {
        log.debug("lastItem processed item: {}", lastItem);

        try {
            LinkedList<Node> nodes = new LinkedList<Node>();
            final String statement = eventCategory == null ? "SELECT * FROM hippolog:item ORDER BY hippolog:timestamp DESC"
                    : "SELECT * FROM hippolog:item WHERE hippolog:category = '" + eventCategory + "' ORDER BY hippolog:timestamp DESC";
            Query query = session.getWorkspace().getQueryManager().createQuery(statement, Query.SQL);

            if (queryLimit > -1) {
                log.debug("setting query limit to {}", queryLimit);
                query.setLimit(queryLimit);
            }

            QueryResult queryResult = query.execute();
            NodeIterator nodeIterator = queryResult.getNodes();

            // if a maximum event age has been configured, check if last processed item
            // is older than maximum age, and update lastItem if so
            if (maxEventAge > -1) {
                Calendar minTimestamp = new GregorianCalendar();
                minTimestamp.add(Calendar.HOUR_OF_DAY, -((int) maxEventAge));
                if (lastItem < minTimestamp.getTimeInMillis()) {
                    log.debug("skip events older than {}", minTimestamp.getTime());
                    lastItem = minTimestamp.getTimeInMillis();
                }
            }

            // iterate through results (which are in reverse chronological order)
            // until timestamp is older than what we are interested in
            for (Node logNode : new NodeIterable(nodeIterator)) {
                // add log node if valid and has timestamp higher than lastItem
                if (logNode.hasProperty(HIPPOLOG_TIMESTAMP)) {
                    long timeStamp = logNode.getProperty(HIPPOLOG_TIMESTAMP).getLong();
                    if (timeStamp > lastItem) {
                        nodes.add(logNode);
                    } else {
                        break;
                    }
                }
            }

            // reverse list because query result was sorted by timestamp descending
            List<Node> nodesSorted = new LinkedList<Node>();
            Iterator<Node> it = nodes.descendingIterator();
            while (it.hasNext()) {
                nodesSorted.add(it.next());
            }

            return nodesSorted;

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
