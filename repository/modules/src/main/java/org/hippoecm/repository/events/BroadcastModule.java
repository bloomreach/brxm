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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.HippoAnnotationHandlerFinder;
import com.google.common.eventbus.HippoSynchronizedEventHandler;

import org.hippoecm.repository.ext.DaemonModule;
import org.onehippo.cms7.services.HippoServiceRegistration;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.repository.events.HippoWorkflowEvent;
import org.onehippo.repository.events.Persisted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BroadcastModule implements DaemonModule, BroadcastService {

    private static final Logger log = LoggerFactory.getLogger(BroadcastModule.class);

    private static final String JACKRABBIT_CLUSTER_ID_SYSTEM_PROPERTY = "org.apache.jackrabbit.core.cluster.node_id";
    private static final String JACKRABBIT_CLUSTER_ID_DESCRIPTOR_KEY = "jackrabbit.cluster.id";

    private Session session;
    private String clusterId;
    private BroadcastThread broadcastThread;

    public BroadcastModule() {
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
            if (session.nodeExists(BroadcastConstants.MODULE_CONFIG_PATH)) {
                Node moduleConfigNode = session.getNode(BroadcastConstants.MODULE_CONFIG_PATH);
                
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
            log.warn("Exception while reading configuration", e);
        } finally {
            session.refresh(false);
        }
    }

    public synchronized long getLastProcessed(String subscriberName) throws RepositoryException {
        long lastItem = -1L;

        try {
            String clusterId = getClusterId();
            if (session.nodeExists(BroadcastConstants.MODULE_CONFIG_PATH)) {
                Node moduleConfigNode = session.getNode(BroadcastConstants.MODULE_CONFIG_PATH);

                if (moduleConfigNode.hasNode(clusterId)) {
                    Node clusterNode = moduleConfigNode.getNode(clusterId);
                    if (clusterNode.hasNode(subscriberName)) {
                        Node subscriberNode = clusterNode.getNode(subscriberName);
                        return subscriberNode.getProperty(BroadcastConstants.LAST_PROCESSED).getLong();
                    }
                }
            }
        } catch (ItemNotFoundException infExp) {
            log.error("Error in getting last processed item ", infExp);
        } finally {

            if (log.isDebugEnabled()) {
                log.debug("last item is             " + lastItem);
            }

            session.refresh(false);
        }

        return lastItem;
    }

    public synchronized void writeLastProcessed(String subscriberName, final long timeStamp)
            throws RepositoryException {
        log.debug("processed item: {}", timeStamp);

        try {
            session.refresh(true);

            String clusterId = getClusterId();
            if (session.nodeExists(BroadcastConstants.MODULE_CONFIG_PATH)) {
                Node moduleConfigNode = session.getNode(BroadcastConstants.MODULE_CONFIG_PATH);

                Node clusterNode;
                if (moduleConfigNode.hasNode(clusterId)) {
                    clusterNode = moduleConfigNode.getNode(clusterId);
                } else {
                    clusterNode = moduleConfigNode.addNode(clusterId, BroadcastConstants.NT_CLUSTERNODE);
                }

                Node subscriberNode;
                if (!clusterNode.hasNode(subscriberName)) {
                    subscriberNode = clusterNode.addNode(subscriberName, BroadcastConstants.NT_SUBSCRIBER);
                } else {
                    subscriberNode = clusterNode.getNode(subscriberName);
                }

                subscriberNode.setProperty(BroadcastConstants.LAST_PROCESSED, timeStamp);
                session.save();
            }
        } catch (ItemNotFoundException infExp) {
            log.error("Error in saving last node value ", infExp);
        } finally {
            session.refresh(false);
        }
    }

    private String getClusterId() {
        return this.clusterId;
    }

    @Override
    public BroadcastJob getNextJob() {

        Multimap<String, HippoSynchronizedEventHandler> handlers = ArrayListMultimap.create();
        HippoAnnotationHandlerFinder hahf = new HippoAnnotationHandlerFinder();
        for (HippoServiceRegistration registration : HippoServiceRegistry.getRegistrations(HippoEventBus.class)) {
            final Multimap<Class<?>, ?> allHandlers = hahf.findAllHandlers(registration.getService());
            for (HippoSynchronizedEventHandler handler : (Collection<HippoSynchronizedEventHandler>) allHandlers.get(
                    HippoWorkflowEvent.class)) {
                for (Annotation annotation : handler.getAnnotations()) {
                    if (annotation.annotationType() == Persisted.class) {
                        Persisted persisted = (Persisted) annotation;
                        String name = persisted.name();
                        handlers.put(name, handler);
                    }
                }
            }
        }
        String oldestSubscriber = null;
        long oldestProcessingStamp = -1;
        for (String name : handlers.keys()) {
            try {
                long lastProcessed = getLastProcessed(name);
                if (lastProcessed <= oldestProcessingStamp) {
                    oldestProcessingStamp = lastProcessed;
                    oldestSubscriber = name;
                }
            } catch (RepositoryException e) {
                log.error("Error determining oldest handler", e);
            }
        }
        if (oldestSubscriber == null) {
            return null;
        }

        Collection<HippoSynchronizedEventHandler> subscriberHandlers = handlers.get(oldestSubscriber);

        return new BroadcastJobImpl(oldestSubscriber, oldestProcessingStamp, subscriberHandlers);
    }

    private class BroadcastJobImpl implements BroadcastJob {

        private final String name;
        private final long lastProcessed;
        private final Collection<HippoSynchronizedEventHandler> handlers;

        BroadcastJobImpl(String name, final long oldestProcessingStamp, Collection<HippoSynchronizedEventHandler> subscriberHandlers) {
            this.name = name;
            this.lastProcessed = oldestProcessingStamp;
            this.handlers = subscriberHandlers;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getLastProcessed() {
            return lastProcessed;
        }

        @Override
        public void setLastProcessed(final long time) {
            try {
                writeLastProcessed(name, time);
            } catch (RepositoryException e) {
                log.error("Unable to update last processing time for subscriber " + name, e);
            }
        }

        @Override
        public void publish(final HippoWorkflowEvent event) {
            for (HippoSynchronizedEventHandler handler : handlers) {
                try {
                    handler.handleEvent(event);
                } catch (InvocationTargetException e) {
                    log.error("Failed to dispatch workflow event", e);
                }
            }
        }
    }

}
