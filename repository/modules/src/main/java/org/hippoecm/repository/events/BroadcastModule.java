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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistration;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.events.PersistedHippoEventListener;
import org.onehippo.repository.events.PersistedHippoEventsService;
import org.onehippo.repository.modules.ConfigurableDaemonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BroadcastModule implements ConfigurableDaemonModule, BroadcastService, PersistedHippoEventsService {

    private static final Logger log = LoggerFactory.getLogger(BroadcastModule.class);

    private static final String JACKRABBIT_CLUSTER_ID_SYSTEM_PROPERTY = "org.apache.jackrabbit.core.cluster.node_id";
    private static final String JACKRABBIT_CLUSTER_ID_DESCRIPTOR_KEY = "jackrabbit.cluster.id";

    public static final String QUERY_LIMIT = "queryLimit";
    public static final String POLLING_TIME = "pollingTime";
    public static final String MAX_EVENT_AGE = "maxEventAge";

    private Session session;
    private String clusterId;
    private BroadcastThread broadcastThread;
    private String moduleConfigIdentifier;

    public BroadcastModule() {
    }

    @Override
    public void configure(final Node moduleConfig) throws RepositoryException {
        this.moduleConfigIdentifier = moduleConfig.getIdentifier();
    }

    public void initialize(Session session) throws RepositoryException {
        this.session = session;

        clusterId = System.getProperty(JACKRABBIT_CLUSTER_ID_SYSTEM_PROPERTY);
        if (clusterId == null) {
            clusterId = session.getRepository().getDescriptor(JACKRABBIT_CLUSTER_ID_DESCRIPTOR_KEY);
        }
        if (clusterId == null) {
            clusterId = "default";
        } else {
            log.debug("Cluster Node Id: {}", clusterId);
        }

        broadcastThread = new BroadcastThread(session, this);
        configure(broadcastThread);
        broadcastThread.start();
    }

    @Override
    public void shutdown() {
        broadcastThread.stopThread();
        broadcastThread = null;
    }

    protected void configure(final BroadcastThread broadcastThread) throws RepositoryException {
        try {
            final Node moduleConfigNode = session.getNodeByIdentifier(moduleConfigIdentifier);
            if (moduleConfigNode.hasProperty(QUERY_LIMIT)) {
                broadcastThread.setQueryLimit(moduleConfigNode.getProperty(QUERY_LIMIT).getLong());
            }
            if (moduleConfigNode.hasProperty(POLLING_TIME)) {
                broadcastThread.setPollingTime(moduleConfigNode.getProperty(POLLING_TIME).getLong());
            }
            if (moduleConfigNode.hasProperty(MAX_EVENT_AGE)) {
                broadcastThread.setMaxEventAge(moduleConfigNode.getProperty(MAX_EVENT_AGE).getLong());
            }
        } catch (PathNotFoundException | ItemNotFoundException e) {
            session.refresh(false);
            log.warn("Exception while reading configuration", e);
        } catch (Exception e) {
            session.refresh(false);
            throw e;
        }
    }

    private synchronized long getLastProcessed(String channelName, boolean onlyNewEvents) throws RepositoryException {
        long lastItem = -1L;

        try {
            final Node moduleConfigNode = session.getNodeByIdentifier(moduleConfigIdentifier);
            if (moduleConfigNode.hasNode(clusterId)) {
                Node clusterNode = moduleConfigNode.getNode(clusterId);
                if (clusterNode.hasNode(channelName)) {
                    Node channelNode = clusterNode.getNode(channelName);
                    lastItem = channelNode.getProperty(BroadcastConstants.LAST_PROCESSED).getLong();
                }
                else if (onlyNewEvents) {
                    lastItem = System.currentTimeMillis();
                    writeLastProcessed(channelName, lastItem);
                }
            }
        } catch (ItemNotFoundException infExp) {
            log.error("Error in getting last processed item ", infExp);
        } catch (Exception e) {
            session.refresh(false);
            throw e;
        }
        log.debug("last item is {}", lastItem);
        return lastItem;
    }

    public synchronized void writeLastProcessed(String channelName, final long timeStamp) throws RepositoryException {
        log.debug("processed item: {}", timeStamp);

        try {

            Node moduleConfigNode = session.getNodeByIdentifier(moduleConfigIdentifier);

            Node clusterNode;
            if (moduleConfigNode.hasNode(clusterId)) {
                clusterNode = moduleConfigNode.getNode(clusterId);
            } else {
                clusterNode = moduleConfigNode.addNode(clusterId, BroadcastConstants.NT_CLUSTERNODE);
            }
            if (!clusterNode.isNodeType(HippoNodeType.NT_SKIPINDEX)) {
                // make sure the frequently updated clusterNode is not indexed as we do not need to search
                // for it but it would otherwise pollute the search index
                clusterNode.addMixin(HippoNodeType.NT_SKIPINDEX);
            }

            Node channelNode;
            if (!clusterNode.hasNode(channelName)) {
                channelNode = clusterNode.addNode(channelName, BroadcastConstants.NT_SUBSCRIBER);
            } else {
                channelNode = clusterNode.getNode(channelName);
            }

            channelNode.setProperty(BroadcastConstants.LAST_PROCESSED, timeStamp);
            session.save();
        } catch (ItemNotFoundException infExp) {
            log.error("Error in saving last node value ", infExp);
        } catch (Exception e) {
            session.refresh(false);
            throw e;
        }
    }

    @Override
    public BroadcastJob getNextJob() {
        Map<String, HippoServiceRegistration> registrationMap = new HashMap<>();
        for (HippoServiceRegistration registration : HippoServiceRegistry.getRegistrations(PersistedHippoEventsService.class)) {
            if (registration.getService() instanceof PersistedHippoEventListener) {
                PersistedHippoEventListener listener = (PersistedHippoEventListener)registration.getService();
                HippoServiceRegistration earlierRegistration = registrationMap.get(listener.getChannelName());
                if (earlierRegistration != null) {
                    log.error("Invalid PersistedWorkflowEventsService registration: listener "+
                            listener.getClass().getName() +" subscribes to channel ["+listener.getChannelName()+"] which already" +
                            "has been subscribed to by: "+earlierRegistration.getService().getClass().getName()+". Only the first listener will "+
                            "receive events.");
                }
                else {
                    registrationMap.put(listener.getChannelName(), registration);
                }
            }
            else {
                log.error("Invalid PersistedWorflowEventsService registration: " +
                        registration.getService().getClass().getName() +
                        " does not implement the PersistedWorkflowEventListener interface");
            }
        }
        PersistedHippoEventListener listener = null;
        ClassLoader listenerClassLoader = null;
        long oldestProcessingStamp = -1;
        for (HippoServiceRegistration registration : registrationMap.values()) {
            try {
                PersistedHippoEventListener aListener = (PersistedHippoEventListener)registration.getService();
                long lastProcessed = getLastProcessed(aListener.getChannelName(), aListener.onlyNewEvents());
                if (listener == null || lastProcessed <= oldestProcessingStamp) {
                    listener = aListener;
                    oldestProcessingStamp = lastProcessed;
                    listenerClassLoader = registration.getClassLoader();
                }
            } catch (RepositoryException e) {
                log.error("Error determining oldest listener", e);
            }
        }
        if (listener == null) {
            return null;
        }

        return new BroadcastJobImpl(listener, listenerClassLoader, oldestProcessingStamp);
    }

    private class BroadcastJobImpl implements BroadcastJob {

        private final long lastProcessed;
        private final PersistedHippoEventListener listener;
        private final ClassLoader listenerClassLoader;

        BroadcastJobImpl(PersistedHippoEventListener listener, ClassLoader listenerClassLoader, final long lastProcessed) {
            this.lastProcessed = lastProcessed;
            this.listener = listener;
            this.listenerClassLoader = listenerClassLoader;
        }

        @Override
        public String getEventCategory() {
            return listener.getEventCategory();
        }

        @Override
        public String getChannelName() {
            return listener.getChannelName();
        }

        @Override
        public long getLastProcessed() {
            return lastProcessed;
        }

        @Override
        public void setLastProcessed(final long time) {
            try {
                writeLastProcessed(listener.getChannelName(), time);
            } catch (RepositoryException e) {
                log.error("Unable to update last processing time for channel " + listener.getChannelName(), e);
            }
        }

        @Override
        public void publish(final HippoEvent event) {
            ClassLoader ccl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(listenerClassLoader);
                listener.onHippoEvent(event);
            }
            catch (Exception e) {
                log.error("Failed to dispatch workflow event", e);
            }
            finally {
                Thread.currentThread().setContextClassLoader(ccl);
            }
        }
    }
}
