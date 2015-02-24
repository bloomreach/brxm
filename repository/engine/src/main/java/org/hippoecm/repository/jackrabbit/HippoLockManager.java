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

import java.util.concurrent.ScheduledExecutorService;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.lock.LockInfo;
import org.apache.jackrabbit.core.lock.LockManagerImpl;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.spi.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HippoLockManager extends LockManagerImpl {

    private static final Logger log = LoggerFactory.getLogger(HippoLockManager.class);

    /**
     * Create a new instance of this class.
     *
     * @param session  system session
     * @param fs       file system for persisting locks
     * @param executor scheduled executor service for handling lock timeouts
     * @throws javax.jcr.RepositoryException if an error occurs
     */
    public HippoLockManager(org.apache.jackrabbit.core.SessionImpl session, FileSystem fs, ScheduledExecutorService executor) throws RepositoryException {
        super(session, fs, executor);
    }

    @Override
    protected void checkUnlock(final LockInfo info, final Session session) throws LockException, RepositoryException {
        if (session instanceof org.apache.jackrabbit.core.SessionImpl) {
            final org.apache.jackrabbit.core.SessionImpl sessionImpl = ((org.apache.jackrabbit.core.SessionImpl) session);
            final Path path = sessionImpl.getHierarchyManager().getPath(info.getId());
            if (sessionImpl.getAccessManager().isGranted(path, Permission.LOCK_MNGMT)) {
                return;
            }
        }
        super.checkUnlock(info, session);
    }

    @Override
    public Lock lock(final NodeImpl node, final boolean isDeep, final boolean isSessionScoped) throws LockException, RepositoryException {
        if (isSessionScoped) {
            log.warn("Session-scoped locks are not propagated through the cluster, use open-scoped if you want to be able to run a cluster.");
        }
        return super.lock(node, isDeep, isSessionScoped);
    }

    @Override
    public Lock lock(final NodeImpl node, final boolean isDeep, final boolean isSessionScoped, final long timoutHint, final String ownerInfo) throws LockException, RepositoryException {
        if (isSessionScoped) {
            log.warn("Session-scoped locks are not propagated through the cluster, use open-scoped if you want to be able to run a cluster.");
        }
        return super.lock(node, isDeep, isSessionScoped, timoutHint, ownerInfo);
    }

    @Override
    public void externalLock(final NodeId nodeId, final boolean isDeep, final String lockOwner) throws RepositoryException {
        try {
            super.externalLock(nodeId, isDeep, lockOwner);
        } catch (ItemNotFoundException e) {
            log.debug("Node {} on which lock occurred could not be found", nodeId);
        }
    }

    @Override
    public void externalUnlock(final NodeId nodeId) throws RepositoryException {
        try {
            super.externalUnlock(nodeId);
        } catch (ItemNotFoundException e) {
            log.debug("Node {} on which lock occurred could not be found", nodeId);
        }
    }
}
