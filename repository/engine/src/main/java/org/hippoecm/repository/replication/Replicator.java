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

import java.util.List;

import org.apache.jackrabbit.core.cluster.ChangeLogRecord;
import org.apache.jackrabbit.core.config.ConfigurationException;

/**
 * A {@link Replicator} takes care of replicating a set of changes in the form of a 
 * {@link ChangeLogRecord} to a remote repository. 
 */
public interface Replicator {

    /**
     * Initialize the replicator
     * @param context the {@link ReplicatorContext}
     * @throws ConfigurationException When the initialization fails.
     */
    void init(ReplicatorContext context, List<Filter> filters) throws ConfigurationException;

    /**
     * Do the actual replication.
     * @param record the {@link ChangeLogRecord} containing the changes to replicate.
     * @throws RecoverableReplicationException When the replication of the record can be tried again at a later time.
     * @throws FatalReplicationException When there is no need to try to replicate the record again.
     */
    void replicate(ChangeLogRecord record) throws RecoverableReplicationException, FatalReplicationException;

    /**
     * Stop the {@link Replicator}.
     */
    void destroy();
}
