/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.locking;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.lock.LockManager;

import org.hippoecm.repository.impl.LockManagerDecorator;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.repository.util.JcrConstants;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class LockTest extends RepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        final Node test = session.getRootNode().addNode("test");
        test.addMixin(JcrConstants.MIX_LOCKABLE);
        session.save();
    }

    @Test
    public void testAdminCanUnlockNodeWithoutLockOwnership() throws Exception {
        session.getWorkspace().getLockManager().lock("/test", false, false, Long.MAX_VALUE, null);
        final Session adminSession = session.getRepository().login(new SimpleCredentials("admin", "admin".toCharArray()));
        adminSession.getWorkspace().getLockManager().unlock("/test");
        assertFalse("Node /test is still locked", session.getWorkspace().getLockManager().isLocked("/test"));
    }

    @Test
    public void testAnonymousUserCanUnlockNodeWithoutLockOwnership() throws Exception {
        session.getWorkspace().getLockManager().lock("/test", false, false, Long.MAX_VALUE, null);
        final Session anonSession = session.getRepository().login();
        anonSession.getWorkspace().getLockManager().unlock("/test");
        assertFalse("Node /test is still locked", session.getWorkspace().getLockManager().isLocked("/test"));
    }

    @Test
    public void testLockContainsHippoExpirationTimeout() throws Exception {
        session.getWorkspace().getLockManager().lock("/test", false, false, 2l, null);
        assertTrue(session.propertyExists("/test/hippo:lockExpirationTime"));
    }

    @Test
    public void testLockDoesNotContainHippoTimeout() throws Exception {
        session.getWorkspace().getLockManager().lock("/test", false, false, Long.MAX_VALUE, null);
        assertFalse(session.propertyExists("/test/hippo:timeout"));
    }

    @Test
    public void testExpireLockFailsBeforeTimeout() throws Exception {
        final HippoLockManager lockManager = (HippoLockManager) session.getWorkspace().getLockManager();
        lockManager.lock("/test", false, false, Long.MAX_VALUE, null);
        assertFalse(lockManager.expireLock("/test"));
    }

    @Test
    public void testExpireLockSucceedsAfterTimeout() throws Exception {
        final HippoLockManager lockManager = (HippoLockManager) session.getWorkspace().getLockManager();
        lockManager.lock("/test", false, false, 2l, null);
        Thread.sleep(1000l);
        // refresh on the underlying lock so that hippo:timeout is not updated
        // but the lock is not released by jackrabbit internally
        LockManagerDecorator.unwrap(lockManager).getLock("/test").refresh();
        Thread.sleep(1001l);
        assertTrue(lockManager.expireLock("/test"));
    }

    @Test
    public void testLockSucceedsAfterTimeout() throws Exception {
        final LockManager lockManager = session.getWorkspace().getLockManager();
        lockManager.lock("/test", false, false, 2l, null);
        Thread.sleep(1000l);
        // refresh on the underlying lock so that hippo:timeout is not updated
        // but the lock is not released by jackrabbit internally
        LockManagerDecorator.unwrap(lockManager).getLock("/test").refresh();
        Thread.sleep(1001l);
        final Session anonSession = session.getRepository().login();
        final LockManager anonLockManager = anonSession.getWorkspace().getLockManager();
        anonLockManager.lock("/test", false, false, Long.MAX_VALUE, null);
        anonLockManager.isLocked("/test");
    }

    @Test
    public void testNodeLockUsesHippoLockManager() throws Exception {
        assertTrue(session.getNode("/test").lock(false, false) instanceof HippoLock);
    }

    @Test
    public void testLockKeepAliveKeepsLockAlive() throws Exception {
        final HippoLockManager lockManager = (HippoLockManager) session.getWorkspace().getLockManager();
        final HippoLock lock = lockManager.lock("/test", false, false, 10l, null);
        lock.startKeepAlive();
        Thread.sleep(10001l);
        assertTrue(lock.isLive());
        assertFalse(lockManager.expireLock("/test"));
        lock.stopKeepAlive();
        Thread.sleep(10001l);
        assertTrue(lockManager.expireLock("/test"));
        assertFalse(lock.isLive());
    }

    @Test
    public void testLockKeepAliveReLocksDroppedLock() throws Exception {
        final HippoLockManager lockManager = (HippoLockManager) session.getWorkspace().getLockManager();
        final HippoLock lock = lockManager.lock("/test", false, false, 10l, null);
        lock.startKeepAlive();
        lockManager.unlock("/test");
        Thread.sleep(3001l);
        assertTrue(lockManager.isLocked("/test"));
        lock.stopKeepAlive();
    }
}
