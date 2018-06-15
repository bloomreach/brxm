/*
 * Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;

import org.hippoecm.repository.impl.LockManagerDecorator;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.repository.util.JcrConstants;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class LockTest extends RepositoryTestCase {

    private LockManager lockManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        final Node test = session.getRootNode().addNode("test");
        test.addMixin(JcrConstants.MIX_LOCKABLE);
        session.save();
        lockManager = session.getWorkspace().getLockManager();
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
    public void testLockSucceedsAfterTimeout() throws Exception {
        final LockManager lockManager = session.getWorkspace().getLockManager();
        lockManager.lock("/test", false, false, 2l, null);
        Thread.sleep(1000l);
        // refresh on the underlying lock so that hippo:timeout is not updated
        // but the lock is not released by jackrabbit internally
        LockManagerDecorator.unwrap(lockManager).getLock("/test").refresh();
        Thread.sleep(1001l);
        final Session anonSession = session.getRepository().login(CREDENTIALS);
        final LockManager anonLockManager = anonSession.getWorkspace().getLockManager();
        anonLockManager.lock("/test", false, false, Long.MAX_VALUE, null);
        assertTrue(anonLockManager.isLocked("/test"));
    }

    @Test
    public void timedOutLockIsNotLocked() throws Exception {
        lockManager.lock("/test", false, false, 1l, null);
        Thread.sleep(1001l);
        assertFalse("Timed out lock is still locked", lockManager.isLocked("/test"));
        lockManager.lock("/test", false, false, 1l, null);
        Thread.sleep(1001l);
        try {
            lockManager.getLock("/test");
            fail("Expected lock exception on getting timed out lock");
        } catch (LockException ignore) {}
        lockManager.lock("/test", false, false, 1l, null);
        Thread.sleep(1001l);
        try {
            lockManager.unlock("/test");
            fail("Expected lock exception on unlocking timed out lock");
        } catch (LockException ignore) {}
        lockManager.lock("/test", false, false, 1l, null);
        Thread.sleep(1001l);
        try {
            lockManager.lock("/test", false, false, 1l, null);
        } catch (LockException e) {
            fail("Can't lock node on which previous lock has timed out: " + e);
        }
        Thread.sleep(1001l);
        assertFalse("Timed out lock is still held", lockManager.holdsLock("/test"));
        lockManager.lock("/test", false, false, 1l, null);
        Thread.sleep(1001l);
        final Session testSession = session.getRepository().login(new SimpleCredentials("admin", "admin".toCharArray()));
        try {
            testSession.getNode("/test").setProperty("key", "value");
            testSession.save();
        } catch (LockException e) {
            fail("Can't set property on node on which lock has timed out");
        }
    }

}
