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
import org.onehippo.cms7.services.HippoServiceRegistration;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.events.HippoWorkflowEvent;
import org.onehippo.repository.events.PersistedWorkflowEventListener;
import org.onehippo.repository.events.PersistedWorkflowEventsService;
import org.onehippo.repository.modules.ConfigurableDaemonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BroadcastModule implements ConfigurableDaemonModule, BroadcastService, PersistedWorkflowEventsService {

    private static final Logger log = LoggerFactory.getLogger(BroadcastModule.class);

    private static final String JACKRABBIT_CLUSTER_ID_SYSTEM_PROPERTY = "org.apache.jackrabbit.core.cluster.node_id";
    private static final String JACKRABBIT_CLUSTER_ID_DESCRIPTOR_KEY = "jackrabbit.cluster.id";

    private Session session;
    private String clusterId;
    private BroadcastThread broadcastThread;
    private String moduleConfigPath;

    public BroadcastModule() {
    }

    @Override
    public void configure(final Node moduleConfig) throws RepositoryException {
        this.moduleConfigPath = moduleConfig.getPath();
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
            if (session.nodeExists(moduleConfigPath)) {
                final Node moduleConfigNode = session.getNode(moduleConfigPath);
                if (moduleConfigNode != null) {
                    if (moduleConfigNode.hasProperty("queryLimit")) {
                        broadcastThread.setQueryLimit(moduleConfigNode.getProperty("queryLimit").getLong());
                    }
                    if (moduleConfigNode.hasProperty("pollingTime")) {
                        broadcastThread.setPollingTime(moduleConfigNode.getProperty("pollingTime").getLong());
                    }
                    if (moduleConfigNode.hasProperty("maxEventAge")) {
                        broadcastThread.setMaxEventAge(moduleConfigNode.getProperty("maxEventAge").getLong());
                    }
                }
            }
        } catch (PathNotFoundException e) {
            session.refresh(false);
            log.warn("Exception while reading configuration", e);
        } catch (Exception e) {
            session.refresh(false);
            throw e;
        }
    }

    public synchronized long getLastProcessed(String channelName, boolean onlyNewEvents) throws RepositoryException {
        long lastItem = -1L;

        try {
            if (session.nodeExists(moduleConfigPath)) {
                Node moduleConfigNode = session.getNode(moduleConfigPath);

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

    public synchronized void writeLastProcessed(String channelName, final long timeStamp)
            throws RepositoryException {
        log.debug("processed item: {}", timeStamp);

        try {

            if (session.nodeExists(moduleConfigPath)) {
                Node moduleConfigNode = session.getNode(moduleConfigPath);

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
            }
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
        for (HippoServiceRegistration registration : HippoServiceRegistry.getRegistrations(PersistedWorkflowEventsService.class)) {
            if (registration.getService() instanceof PersistedWorkflowEventListener) {
                PersistedWorkflowEventListener listener = (PersistedWorkflowEventListener)registration.getService();
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
        PersistedWorkflowEventListener listener = null;
        ClassLoader listenerClassLoader = null;
        long oldestProcessingStamp = -1;
        for (HippoServiceRegistration registration : registrationMap.values()) {
            try {
                PersistedWorkflowEventListener aListener = (PersistedWorkflowEventListener)registration.getService();
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
        private final PersistedWorkflowEventListener listener;
        private final ClassLoader listenerClassLoader;

        BroadcastJobImpl(PersistedWorkflowEventListener listener, ClassLoader listenerClassLoader, final long lastProcessed) {
            this.lastProcessed = lastProcessed;
            this.listener = listener;
            this.listenerClassLoader = listenerClassLoader;
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
        public void publish(final HippoWorkflowEvent event) {
            ClassLoader ccl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(listenerClassLoader);
                listener.onWorkflowEvent(event);
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
