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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.RepoUtils;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.ProxiedServiceHolder;
import org.onehippo.repository.events.PersistedHippoEventListener;
import org.onehippo.repository.events.PersistedHippoEventListenerRegistry;
import org.onehippo.repository.modules.ConfigurableDaemonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BroadcastModule implements ConfigurableDaemonModule, BroadcastService {

    private static final Logger log = LoggerFactory.getLogger(BroadcastModule.class);

    public static final String QUERY_LIMIT = "queryLimit";
    public static final String POLLING_TIME = "pollingTime";
    public static final String MAX_EVENT_AGE = "maxEventAge";

    private static final long DEFAULT_POLLING_TIME = 5000L;
    private static final long DEFAULT_QUERY_LIMIT = 500L;
    private static final long DEFAULT_MAX_EVENT_AGE = 24L;

    private Session session;
    private String clusterId;
    private long pollingTime;
    private String moduleConfigIdentifier;
    private Broadcaster broadcaster;
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public BroadcastModule() {
    }

    @Override
    public void configure(final Node moduleConfig) throws RepositoryException {
        this.moduleConfigIdentifier = moduleConfig.getIdentifier();
        pollingTime = JcrUtils.getLongProperty(moduleConfig, POLLING_TIME, DEFAULT_POLLING_TIME);
    }

    public void initialize(Session session) throws RepositoryException {
        this.session = session;

        clusterId = RepoUtils.getClusterNodeId(session);
        log.debug("Cluster Node Id: {}", clusterId);

        broadcaster = new Broadcaster(session, this);
        configure(broadcaster);
        executor.scheduleAtFixedRate(broadcaster, 0l, pollingTime, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        broadcaster.stop();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Failed to shut down broadcaster cleanly: timed out waiting");
            }
        } catch (InterruptedException ignored) {
        }
    }

    protected void configure(final Broadcaster broadcaster) throws RepositoryException {
        final Node moduleConfigNode = session.getNodeByIdentifier(moduleConfigIdentifier);
        broadcaster.setQueryLimit(JcrUtils.getLongProperty(moduleConfigNode, QUERY_LIMIT, DEFAULT_QUERY_LIMIT));
        broadcaster.setMaxEventAge(JcrUtils.getLongProperty(moduleConfigNode, MAX_EVENT_AGE, DEFAULT_MAX_EVENT_AGE));
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
                } else if (onlyNewEvents) {
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
        final List<Pair<PersistedHippoEventListener, Long>> listenerInfos = new ArrayList<>();
        long globalLastProcessed = Long.MAX_VALUE;
        for (ProxiedServiceHolder<PersistedHippoEventListener> serviceHolder : getPersistedHippoEventsServiceRegistrations()) {
            try {
                PersistedHippoEventListener listener = serviceHolder.getServiceProxy();
                long lastProcessed = getLastProcessed(listener.getChannelName(), listener.onlyNewEvents());

                final Pair<PersistedHippoEventListener, Long> listenerInfo =
                        new ImmutablePair(listener, lastProcessed);
                listenerInfos.add(listenerInfo);
                if (lastProcessed < globalLastProcessed) {
                    globalLastProcessed = lastProcessed;
                }
            } catch (RepositoryException e) {
                log.error("Error getting last processed, skipping listener '{}'", serviceHolder.getServiceObject(), e);
            }
        }
        if (listenerInfos.size() == 0) {
            return null;
        }

        return new BroadcastJobImpl(listenerInfos, globalLastProcessed);
    }

    protected Collection<ProxiedServiceHolder<PersistedHippoEventListener>> getPersistedHippoEventsServiceRegistrations() {
        Map<String, ProxiedServiceHolder<PersistedHippoEventListener>> registrationMap = new HashMap<>();
        PersistedHippoEventListenerRegistry.get().getEntries().forEach(serviceHolder -> {
            PersistedHippoEventListener listener = serviceHolder.getServiceProxy();
            ProxiedServiceHolder<PersistedHippoEventListener> earlierRegistration = registrationMap.get(listener.getChannelName());
            if (earlierRegistration != null) {
                log.error("Invalid PersistedWorkflowEventListener registration: listener " +
                        serviceHolder.getServiceObject().getClass().getName() + " subscribes to channel [" + listener.getChannelName() + "] which already" +
                        "has been subscribed to by: " + earlierRegistration.getServiceObject().getClass().getName() + ". Only the first listener will " +
                        "receive events.");
            } else {
                registrationMap.put(listener.getChannelName(), serviceHolder);
            }
        });
        return registrationMap.values();
    }

    private class BroadcastJobImpl implements BroadcastJob {

        private List<Pair<PersistedHippoEventListener, Long>> listenerInfos;
        private long globalLastProcessed;

        BroadcastJobImpl(final List<Pair<PersistedHippoEventListener, Long>> listenerInfos,
                         final long globalLastProcessed) {
            this.listenerInfos = listenerInfos;
            this.globalLastProcessed = globalLastProcessed;
        }

        @Override
        public long getGlobalLastProcessed() {
            return globalLastProcessed;
        }

        @Override
        public void setLastProcessed(final long time) {
            // since PersistedHippoEventListener.onlyNewEvents exists we need to set the last processed PER
            // listener unfortunately instead of PER cluster node.
            for (Pair<PersistedHippoEventListener, Long> listener : listenerInfos) {
                final String channelName = listener.getLeft().getChannelName();
                try {
                    writeLastProcessed(channelName, time);
                } catch (RepositoryException e) {
                    log.error("Unable to update last processing time for channel " + channelName, e);
                }
            }

        }

        @Override
        public void publish(final HippoEvent event) {
            for (Pair<PersistedHippoEventListener, Long> listenerInfo : listenerInfos) {
                final PersistedHippoEventListener listener = listenerInfo.getLeft();
                if (shouldListenerProcessEvent(listenerInfo, event)) {
                    try {
                        listener.onHippoEvent(event);
                    } catch (Exception e) {
                        log.error("Failed to dispatch workflow event", e);
                    }
                }
            }
        }

        private boolean shouldListenerProcessEvent(final Pair<PersistedHippoEventListener, Long> listenerInfo, final HippoEvent event) {
            if (event.timestamp() < listenerInfo.getRight().longValue()) {
                // skip too old event
                return false;
            }
            final PersistedHippoEventListener listener = listenerInfo.getLeft();
            if (listener.getEventCategory() == null) {
                return true;
            }
            if (listener.getEventCategory().equals(event.category())) {
                return true;
            }
            return false;
        }
    }
}
