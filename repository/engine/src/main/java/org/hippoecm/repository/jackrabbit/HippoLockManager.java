/*
 *  Copyright 2012 Hippo.
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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.lock.LockInfo;
import org.apache.jackrabbit.core.lock.LockManagerImpl;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.spi.Path;


public class HippoLockManager extends LockManagerImpl {

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
}
