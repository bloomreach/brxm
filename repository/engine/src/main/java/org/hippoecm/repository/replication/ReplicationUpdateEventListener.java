/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.repository.replication;

import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.cluster.UpdateEventListener;
import org.apache.jackrabbit.core.observation.EventState;
import org.apache.jackrabbit.core.state.ChangeLog;

/**
 * Interface used to receive information about incoming, external update events and 
 * local update events. The {@link UpdateEventListener} only can receive external 
 * update events.
 */
public interface ReplicationUpdateEventListener {

    /**
     * Handle an external update.
     *
     * @param changes external changes containing only node and property ids.
     * @param events A {@link List} of {@link EventState}s to deliver
     * @throws RepositoryException if the update cannot be processed
     */
    void externalUpdate(ChangeLog changes, List<EventState> events) throws RepositoryException;

    /**
     * Handle an internal update.
     *
     * @param changes internal changes containing only node and property ids.
     * @param events A {@link List} of {@link EventState}s to deliver
     * @throws RepositoryException if the update cannot be processed
     */
    void internalUpdate(ChangeLog changes, List<EventState> events) throws RepositoryException;

}
