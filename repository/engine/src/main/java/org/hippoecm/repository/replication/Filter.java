/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Session;

import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.ChangeLog;

/**
 * A {@link Filter} takes care of filtering the changes to replicate. 
 * The filter provides two hooks. The first hook {@link #filterChangeLog(ChangeLog)} filters the {@link ChangeLog} before any
 * replication is done. The second hook {@link #filterSession(Session)} is called just before the save on the {@link Session}
 * is called. Note that this call can also happen on the remote repository depending on the replicating mechanism.
 */
public interface Filter {

    /**
     * Initialize the replicator filter.
     * @param context the {@link ReplicatorContext}
     * @throws ConfigurationException
     */
    void init(ReplicatorContext context) throws ConfigurationException;

    /**
     * Check is the node is excluded from replication by the current filter.
     * @param nodeId the id of the node to check
     * @return true if the node is excluded from replication.
     */
    boolean isNodeExcluded(NodeId nodeId);
    
    /**
     * Check if exclude nodes from replication should be actively removed.
     * @return true if the node should be removed in the remote repository
     */
    boolean removeExistingExcludedNodes();
    
    /**
     * Destroy the filter.
     */
    void destroy();
}
