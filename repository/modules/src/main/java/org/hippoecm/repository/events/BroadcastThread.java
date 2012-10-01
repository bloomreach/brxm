/*
 * Copyright (C) 2012 Hippo B.V.
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.onehippo.repository.events.HippoWorkflowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BroadcastThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(BroadcastThread.class);
    private static final long DEFAULT_TIMESTAMP = -1L;

    private static final String QUERY = "//*[@jcr:primaryType='hippolog:item'] order by @hippolog:timestamp descending";

    private static final String HIPPOLOG_TIMESTAMP = "hippolog:timestamp";

    private static final long DEFAULT_POLLING_TIME = 5000L; // 5 seconds (5000 milliseconds)
    private static final long DEFAULT_QUERY_LIMIT = 500L;
    private static final long DEFAULT_MAX_EVENT_AGE = 120L; // 5 days (5*24 hours)

    private volatile boolean keepRunning = true;

    private final Session session;
    private final BroadcastService broadcastService;

    private long queryLimit;
    private long pollingTime;
    private long maxEventAge;

    public BroadcastThread(final Session session, final BroadcastService broadcastService) {
        this.session = session;
        this.broadcastService = broadcastService;

        this.queryLimit = DEFAULT_QUERY_LIMIT;
        this.pollingTime = DEFAULT_POLLING_TIME;
        this.maxEventAge = DEFAULT_MAX_EVENT_AGE;
    }

    public void setQueryLimit(long limit) {
        this.queryLimit = limit;
    }

    public void setPollingTime(final long pollingTime) {
        this.pollingTime = pollingTime;
    }

    public void setMaxEventAge(final long maxEventAge) {
        this.maxEventAge = maxEventAge;
    }

    public void run() {
        while (keepRunning) {
            BroadcastJob job = broadcastService.getNextJob();
            if (job != null) {
                runJob(job);
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

    private void runJob(final BroadcastJob job) {
        try {
            long lastProcessItem = job.getLastProcessed();
            logger.debug("Getting latest log items from {}", lastProcessItem);

            List<Node> logItems = getNextLogNodes(lastProcessItem);
            long timeStamp = processEvents(job, logItems);

            if (timeStamp > -1L) {
                job.setLastProcessed(timeStamp);
            }
        } catch (Exception e) {
            logger.warn("Error during running thread", e);
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

        } finally {
            session.refresh(false);
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
        if (logItems.size() == 0) {
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
            } catch (RepositoryException re) {
                logger.warn("Unable to process logItem at " + path, re);
            }
        }
        return timeStamp;
    }

    /**
     * This method is responsible creating hippo event from log node. It will parse all the properties of log item and
     * populate in hippoevent
     *
     * @param logNode the logged event node
     * @return HippoEvent with the workflow information
     */
    private HippoWorkflowEvent createEvent(Node logNode) throws RepositoryException {
        HippoWorkflowEvent event = new HippoWorkflowEvent();
        event.timestamp(logNode.getProperty("hippolog:timestamp").getLong());
        event.user(logNode.getProperty("hippolog:eventUser").getString());
        event.className(logNode.getProperty("hippolog:eventClass").getString());
        event.methodName(logNode.getProperty("hippolog:eventMethod").getString());
        // conditional properties
        if (logNode.hasProperty("hippolog:eventDocument")) {
            event.documentPath(logNode.getProperty("hippolog:eventDocument").getString());
        }
        if (logNode.hasProperty("hippolog:handleUuid")) {
            event.handleUuid(logNode.getProperty("hippolog:handleUuid").getString());
        }
        if (logNode.hasProperty("hippolog:eventReturnType")) {
            event.returnType(logNode.getProperty("hippolog:eventReturnType").getString());
        }
        if (logNode.hasProperty("hippolog:eventReturnValue")) {
            event.returnValue(logNode.getProperty("hippolog:eventReturnValue").getString());
        }
        if (logNode.hasProperty("hippolog:eventArguments")) {
            final Value[] values = logNode.getProperty("hippolog:eventArguments").getValues();
            ArrayList<String> arguments = new ArrayList<String>(values.length);
            for (Value value : values) {
                arguments.add(value.getString());
            }
            event.arguments(arguments);
        }

        return event;
    }

}
