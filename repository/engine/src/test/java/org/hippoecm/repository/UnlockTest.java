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
