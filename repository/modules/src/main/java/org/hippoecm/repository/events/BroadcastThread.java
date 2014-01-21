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

import org.hippoecm.repository.util.PropertyValueGetterImpl;
import org.hippoecm.repository.util.ValueGetter;
import org.onehippo.repository.events.HippoWorkflowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import java.util.*;

import static org.hippoecm.repository.util.JcrUtils.getProperties;


class BroadcastThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(BroadcastThread.class);
    private static final long DEFAULT_TIMESTAMP = -1L;

    private static final String QUERY = "//*[@jcr:primaryType='hippolog:item'] order by @hippolog:timestamp descending";

    private static final String HIPPOLOG_TIMESTAMP = "hippolog:timestamp";

    private static final long DEFAULT_POLLING_TIME = 5000L; // 5 seconds (5000 milliseconds)
    private static final long DEFAULT_QUERY_LIMIT = 500L;
    private static final long DEFAULT_MAX_EVENT_AGE = 120L; // 5 days (5*24 hours)

    private volatile boolean keepRunning = true;

    class JobRunner {

        private final BroadcastJob job;
        private boolean processedEvents = false;

        JobRunner(BroadcastJob job) {
            this.job = job;
        }

        void run() {
            try {
                long lastProcessItem = job.getLastProcessed();
                logger.debug("Getting latest log items from {}", lastProcessItem);

                List<Node> logItems = getNextLogNodes(lastProcessItem);
                long timeStamp = processEvents(job, logItems);

                if (timeStamp > -1L) {
                    job.setLastProcessed(timeStamp);
                }
                processedEvents = (logItems.size() > 0);
            } catch (Exception e) {
                logger.warn("Error during running thread", e);
            }
        }

        /**
         * Process the logNodes for events
         *
         *
         * @param job ordered list of Node instances corresponding to logged events
         * @param logItems ordered list of Node instances corresponding to logged events
         * @return timestamp
         */
        private Long processEvents(final BroadcastJob job, final List<Node> logItems) {
            Long timeStamp = DEFAULT_TIMESTAMP;
            if (logItems.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No pending log items to process");
                }
                return DEFAULT_TIMESTAMP;
            }
            for (Node logItem : logItems) {
                String path = "<unknown>";
                try {
                    path = logItem.getPath();
                    final HippoWorkflowEvent event = createEvent(logItem);
                    job.publish(event);
                    timeStamp = event.timestamp();
                } catch (RepositoryException | RuntimeException re) {
                    logger.warn("Unable to process logItem at " + path, re);
                }
            }
            return timeStamp;
        }

        public boolean wereEventsProcessed() {
            return processedEvents;
        }
    }

    private final Session session;
    private final BroadcastService broadcastService;
    private final ValueGetter<Property,?> propertyValueGetter;

    private long queryLimit;
    private long pollingTime;
    private long maxEventAge;

    public BroadcastThread(final Session session, final BroadcastService broadcastService) {
        this.session = session;
        this.broadcastService = broadcastService;
        this.propertyValueGetter = new PropertyValueGetterImpl();

        this.queryLimit = DEFAULT_QUERY_LIMIT;
        this.pollingTime = DEFAULT_POLLING_TIME;
        this.maxEventAge = DEFAULT_MAX_EVENT_AGE;
    }

    public void setQueryLimit(long limit) {
        this.queryLimit = limit;
    }

    public synchronized void setPollingTime(final long pollingTime) {
        this.pollingTime = pollingTime;
    }

    public void setMaxEventAge(final long maxEventAge) {
        this.maxEventAge = maxEventAge;
    }

    public void run() {
        while (keepRunning) {
            BroadcastJob job = broadcastService.getNextJob();
            if (job != null) {
                JobRunner runner = new JobRunner(job);
                runner.run();

                if (keepRunning && runner.wereEventsProcessed()) {
                    continue;
                }
            }

            if (keepRunning) {
                try {
                    synchronized (this) {
                        wait(pollingTime);
                    }
                } catch (InterruptedException e) {
                    keepRunning = false;
                    logger.error("Error during running thread", e);
                }
            }
        }
    }

    /**
     * Invoking this method will stop this thread.
     */
    public void stopThread() {
        this.keepRunning = false;
        int numPollsToWait = 5;
        while (isAlive() && numPollsToWait-- > 0) {
            try {
                synchronized (this) {
                    wait(pollingTime);
                }
            } catch (InterruptedException e) {
                logger.error("Error during running thread", e);
            }
        }
        if (isAlive()) {
            logger.warn("Unable to shut down the broadcast thread");
        }
    }

    public List<Node> getNextLogNodes(long lastItem) throws RepositoryException {
        logger.debug("lastItem processed item: {}", lastItem);

        try {
            LinkedList<Node> nodes = new LinkedList<Node>();
            Query query = session.getWorkspace().getQueryManager().createQuery(QUERY, Query.XPATH);

            if (queryLimit > -1) {
                logger.debug("setting query limit to {}", queryLimit);
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
                    logger.debug("skip events older than {}", minTimestamp.getTime());
                    lastItem = minTimestamp.getTimeInMillis();
                }
            }

            // iterate through results (which are in reverse chronological order)
            // until timestamp is older than what we are interested in
            while (nodeIterator.hasNext()) {
                Node logNode = nodeIterator.nextNode();

                // add log node if valid and has timestamp higher than lastItem
                if (logNode != null && logNode.hasProperty(HIPPOLOG_TIMESTAMP)) {
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
    private HippoWorkflowEvent createEvent(Node logNode) throws RepositoryException {
        final HippoWorkflowEvent<?> event = new HippoWorkflowEvent();
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
