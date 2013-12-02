/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.update;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdaterRegistry implements EventListener {

    private static final Logger log = LoggerFactory.getLogger(UpdaterRegistry.class);

    private static final String UPDATE_REGISTRY_PATH = "/" + HippoNodeType.CONFIGURATION_PATH + "/hippo:update/hippo:registry";

    private final Session session;
    private volatile Map<String, List<UpdaterInfo>> updaters = new HashMap<>();

    UpdaterRegistry(final Session session) throws RepositoryException {
        this.session = session;
    }

    void start() throws RepositoryException {
        session.getWorkspace().getObservationManager().addEventListener(
                this,
                Event.NODE_ADDED | Event.NODE_MOVED | Event.NODE_REMOVED |
                Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED,
                UPDATE_REGISTRY_PATH, true, null, null, true);
        buildRegistry();
    }

    void stop() {
        try {
            session.getWorkspace().getObservationManager().removeEventListener(this);
        } catch (RepositoryException e) {
            log.error(e.getClass().getName() + ": " + e.getMessage());
        }
        session.logout();
        updaters.clear();
    }

    private void buildRegistry() {
        Map<String, List<UpdaterInfo>> updatedUpdaters = new HashMap<>();
        try {
            final Node registry = JcrUtils.getNodeIfExists(UPDATE_REGISTRY_PATH, session);
            if (registry != null) {
                for (final Node node : new NodeIterable(registry.getNodes())) {
                    final String updaterName = node.getName();
                    try {
                        final UpdaterInfo updaterInfo = new UpdaterInfo(node);
                        if (updaterInfo.getNodeType() != null) {
                            List<UpdaterInfo> list = updatedUpdaters.get(updaterInfo.getNodeType());
                            if (list == null) {
                                list = new ArrayList<>();
                                updatedUpdaters.put(updaterInfo.getNodeType(), list);
                            }
                            list.add(updaterInfo);
                        }
                    } catch (Exception e) {
                        log.error("Failed to register updater '{}': {}", updaterName, e.toString());
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("Failed to build updater registry", e);
        }
        updaters = updatedUpdaters;
    }

    /**
     * Get the list of updaters that are registered for this node. After using the updater, the client must call
     * destroy on the updater.
     *
     * @param node  the node to get the updaters for
     * @return  the list the updaters that should be applied to this node, empty list if no updaters for this node.
     * @throws RepositoryException
     */
    public List<NodeUpdateVisitor> getUpdaters(final Node node) throws RepositoryException {
        if (updaters.isEmpty()) {
            return Collections.emptyList();
        }
        List<NodeUpdateVisitor> result = new ArrayList<>();
        for (Map.Entry<String, List<UpdaterInfo>> entry : updaters.entrySet()) {
            if (node.isNodeType(entry.getKey())) {
                for (UpdaterInfo updaterInfo : entry.getValue()) {
                    try {
                        final NodeUpdateVisitor updater = updaterInfo.getUpdaterClass().newInstance();
                        final Session system = session.impersonate(new SimpleCredentials("system", new char[]{}));
                        updater.initialize(system);
                        result.add(new NodeUpdateVisitor() {
                            @Override
                            public void initialize(final Session session) throws RepositoryException {
                            }

                            @Override
                            public boolean doUpdate(final Node node) throws RepositoryException {
                                return updater.doUpdate(node);
                            }

                            @Override
                            public boolean undoUpdate(final Node node) throws RepositoryException, UnsupportedOperationException {
                                return false;
                            }

                            @Override
                            public void destroy() {
                                updater.destroy();
                                system.logout();
                            }
                        });
                    } catch (InstantiationException | IllegalAccessException e) {
                        log.error("Failed to create updater: {}", e.toString());
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void onEvent(final EventIterator events) {
        buildRegistry();
    }
}
