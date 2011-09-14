/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.jackrabbit;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.core.id.NodeId;

import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.observation.EventStateCollection;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ISMLocking;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.state.StaleItemStateException;
import org.hippoecm.repository.replication.ReplicationUpdateEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoSharedItemStateManager extends SharedItemStateManager {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(HippoSharedItemStateManager.class);

    public RepositoryImpl repository;

    private List<ReplicationUpdateEventListener> updateListeners = new ArrayList<ReplicationUpdateEventListener>();

    public HippoSharedItemStateManager(RepositoryImpl repository, PersistenceManager persistMgr, NodeId rootNodeId,
            NodeTypeRegistry ntReg, boolean usesReferences, ItemStateCacheFactory cacheFactory, ISMLocking locking)
            throws ItemStateException {
        super(persistMgr, rootNodeId, ntReg, usesReferences, cacheFactory, locking);
        this.repository = repository;
    }

    @Override
    public void update(ChangeLog local, EventStateCollectionFactory factory) throws ReferentialIntegrityException,
            StaleItemStateException, ItemStateException {
        super.update(local, factory);
        updateInternalListeners(local, factory);
    }

    @Override
    public void externalUpdate(ChangeLog external, EventStateCollection events) {
        super.externalUpdate(external, events);
        updateExternalListeners(external, events);
    }

    @SuppressWarnings("unchecked")
    public void updateInternalListeners(ChangeLog changes, EventStateCollectionFactory factory) {
        EventStateCollection events = null;
        try {
            events = factory.createEventStateCollection();
        } catch (RepositoryException e) {
            log.error("Unable to create events for for local changes", e);
        }

        synchronized (updateListeners) {
            for (ReplicationUpdateEventListener listener : updateListeners) {
                try {
                    listener.internalUpdate(changes, events.getEvents());
                } catch (RepositoryException e) {
                    log.error("Error while updating replication update event listener.", e);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void updateExternalListeners(ChangeLog changes, EventStateCollection events) {
        synchronized (updateListeners) {
            for (ReplicationUpdateEventListener listener : updateListeners) {
                try {
                    listener.externalUpdate(changes, events.getEvents());
                } catch (RepositoryException e) {
                    log.error("Error while updating replication update event listener.", e);
                }
            }
        }
    }

    /**
     * Register a {@link ReplicationUpdateEventListener}.
     * @param listener
     */
    public void registerUpdateListener(ReplicationUpdateEventListener listener) {
        synchronized (updateListeners) {
            updateListeners.add(listener);
        }
    }

    /**
     * Unregister a {@link ReplicationUpdateEventListener}.
     * @param listener
     */
    public void unRegisterUpdateListener(ReplicationUpdateEventListener listener) {
        synchronized (updateListeners) {
            updateListeners.remove(listener);
        }
    }
}
