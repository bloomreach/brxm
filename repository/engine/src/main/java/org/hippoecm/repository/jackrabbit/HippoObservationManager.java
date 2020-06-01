/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collections;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.cluster.ClusterNode;
import org.apache.jackrabbit.core.observation.EventFilter;
import org.apache.jackrabbit.core.observation.ObservationDispatcher;
import org.apache.jackrabbit.core.observation.ObservationManagerImpl;
import org.apache.jackrabbit.core.observation.RevisionEventJournalImpl;
import org.hippoecm.repository.api.RevisionEventJournal;

public class HippoObservationManager extends ObservationManagerImpl {

    private final SessionImpl session;
    private final ClusterNode clusterNode;

    public HippoObservationManager(final ObservationDispatcher dispatcher, final SessionImpl session, final ClusterNode clusterNode) {
        super(dispatcher, session, clusterNode);
        this.clusterNode = clusterNode;
        this.session = session;
    }

    public RevisionEventJournal getEventJournal(
            int eventTypes, String absPath, boolean isDeep,
            String[] uuid, String[] nodeTypeName)
            throws RepositoryException {

        if (clusterNode == null) {
            throw new UnsupportedRepositoryOperationException(
                    "Event journal is only available in cluster deployments");
        }

        EventFilter filter = createEventFilter(
                eventTypes, Collections.singletonList(absPath), isDeep, uuid, nodeTypeName, false, false, false);
        return new RevisionEventJournalImpl(
                filter, clusterNode.getJournal(), clusterNode.getId(), session);
    }

}
