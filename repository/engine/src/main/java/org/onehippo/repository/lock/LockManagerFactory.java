/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.lock;

import javax.jcr.RepositoryException;
import javax.sql.DataSource;

import org.apache.jackrabbit.core.util.db.ConnectionHelper;
import org.apache.jackrabbit.core.util.db.ConnectionHelperDataSourceAccessor;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.onehippo.cms7.services.lock.LockManager;
import org.onehippo.repository.lock.db.DbLockManager;
import org.onehippo.repository.lock.memory.MemoryLockManager;

public class LockManagerFactory {

    private RepositoryImpl repositoryImpl;

    public LockManagerFactory(final RepositoryImpl repositoryImpl) {

        this.repositoryImpl = repositoryImpl;
    }

    /**
     * Creates the {@link LockManager} which can be used for general purpose locking *not* using JCR at all
     * @throws RuntimeException if the lock manager cannot be created, resulting the repository startup to short-circuit
     * @throws RepositoryException if a repository exception happened while creating the lock manager
     */
    public AbstractLockManager create() throws RuntimeException, RepositoryException {

        final ConnectionHelper journalConnectionHelper = repositoryImpl.getJournalConnectionHelperAccessor().getConnectionHelper();
        if (journalConnectionHelper != null) {
            final DataSource dataSource = ConnectionHelperDataSourceAccessor.getDataSource(journalConnectionHelper);
            String clusterNodeId = repositoryImpl.getDescriptor("jackrabbit.cluster.id");
            return new DbLockManager(dataSource, clusterNodeId == null ? "default" : clusterNodeId);
        } else {
            return new MemoryLockManager();
        }
    }
}
