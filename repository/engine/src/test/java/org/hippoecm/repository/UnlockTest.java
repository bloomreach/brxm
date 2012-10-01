package org.hippoecm.repository;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static junit.framework.Assert.assertFalse;

public class UnlockTest extends RepositoryTestCase {

    @Test
    public void testAdminCanUnlockNodeWithoutLockOwnership() throws Exception {

        final Node test = session.getRootNode().addNode("test");
        test.addMixin("mix:lockable");
        session.save();
        session.getWorkspace().getLockManager().lock("/test", false, false, 60, null);
        final Session adminSession = session.getRepository().login(new SimpleCredentials("admin", "admin".toCharArray()));
        adminSession.getWorkspace().getLockManager().unlock("/test");
        assertFalse("Node /test is still locked", session.getWorkspace().getLockManager().isLocked("/test"));
    }
}
