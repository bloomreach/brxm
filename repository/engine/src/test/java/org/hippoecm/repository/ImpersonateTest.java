/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class ImpersonateTest extends RepositoryTestCase {

    private static final String TEST_USER_ID = "testuser";
    private static final String TEST_USER_PASS = "password";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        Node config = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH);
        Node users = config.getNode(HippoNodeType.USERS_PATH);

        // create test user
        Node testUser = users.addNode(TEST_USER_ID, HippoNodeType.NT_USER);
        testUser.setProperty(HippoNodeType.HIPPO_PASSWORD, TEST_USER_PASS);
        session.save();

    }

    @After
    @Override
    public void tearDown() throws Exception {
        Node config = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH);
        Node users = config.getNode(HippoNodeType.USERS_PATH);

        if (users.hasNode(TEST_USER_ID)) {
            users.getNode(TEST_USER_ID).remove();
        }

        session.save();

        super.tearDown();
    }

    @Test
    public void testImpersonate() throws RepositoryException {
        // setup user session
        Session userSession = server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());
        Session impersonateSession = userSession.impersonate(new SimpleCredentials("admin", new char[] {}));
        assertEquals("admin", impersonateSession.getUserID());
    }
    @Test
    public void testAnonymous() throws RepositoryException {
        // setup user session
        Session userSession = server.login();
        try {
            userSession.impersonate(new SimpleCredentials("admin", new char[] {}));
            fail("User anonymous should not be allowed to impersonate.");
        } catch (LoginException e) {
            // correct
        }
    }

    @Test
    public void testIssueRootSessionOnCredentials() throws RepositoryException {
        SimpleCredentials creds = new SimpleCredentials("nono", "blabla".toCharArray());
        try {
            Session session = server.login(creds);
            fail("this should have failed");
            session.logout();
        } catch (LoginException ex) {
            Object object = creds.getAttribute("rootSession");
            assertFalse(object instanceof Session);
        }
    }

    @Test
    public void testIssueAnyoneCanImpersonateAsWorkflowUser() throws RepositoryException {
        SimpleCredentials creds = new SimpleCredentials("nono", "blabla".toCharArray());
        Session anonymousSession = server.login();
        assertEquals("anonymous", anonymousSession.getUserID());
        Session workflowSession = session.impersonate(new SimpleCredentials("workflowuser", "anything".toCharArray()));
        anonymousSession.logout();
        assertEquals("workflowuser", workflowSession.getUserID());
        workflowSession.logout();
    }
}
