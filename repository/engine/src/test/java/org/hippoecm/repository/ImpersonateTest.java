/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.repository.impl.RepositoryDecorator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.InternalHippoRepository;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
    public void testLoginAsSystemIsDenied() throws RepositoryException {
        Node config = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH);
        Node users = config.getNode(HippoNodeType.USERS_PATH);

        // create system user
        Node testUser = users.addNode("system", HippoNodeType.NT_USER);
        testUser.setProperty(HippoNodeType.HIPPO_PASSWORD, "system");
        try {
            session.save();
            try {
                session.getRepository().login(new SimpleCredentials("system", "system".toCharArray()));
                fail("Login as system user should be denied");
            } catch (LoginException ignore) {
            }
        } finally {
            if (users.hasNode("system")) {
                users.getNode("system").remove();
                session.save();
            }
        }
    }

    @Test
    public void testImpersonatingSystemIsDenied() throws RepositoryException {
        try {
            session.getRepository().login(new SimpleCredentials("system", "system".toCharArray()));
            fail("Login as system user should be denied");
        } catch (LoginException ignore) {
        }
    }

    @Test
    public void testImpersonateAnyUserFromAnyUserIsAllowed() throws RepositoryException {
        // setup user session
        Session userSession = server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());
        Session impersonateSession = userSession.impersonate(new SimpleCredentials("admin", new char[] {}));
        assertEquals("admin", impersonateSession.getUserID());
        userSession.logout();
        impersonateSession.logout();
    }

    @Test
    public void testSystemImpersonateWithNoSystemImpersonation() throws RepositoryException {
        InternalHippoRepository internalRepository = (InternalHippoRepository) RepositoryDecorator.unwrap(server.getRepository());
        Session systemSession = internalRepository.createSystemSession();
        try {
            assertEquals("system", systemSession.getUserID());
            Session userSession = systemSession.impersonate(new SimpleCredentials(TEST_USER_ID, TEST_USER_PASS.toCharArray()));

            assertTrue(systemSession.hasPermission("/hippo:configuration/hippo:domains", Session.ACTION_READ));
            assertFalse("test user should not have read access to hippo:domains",
                    userSession.hasPermission("/hippo:configuration/hippo:domains", Session.ACTION_READ));

            String userId = userSession.getUserID();
            userSession.logout();
            assertEquals(TEST_USER_ID, userId);
        } finally {
            systemSession.logout();
        }
    }

    @Test
    public void testSystemImpersonatesSystemIgnoringPassword() throws RepositoryException {
        InternalHippoRepository internalRepository = (InternalHippoRepository) RepositoryDecorator.unwrap(server.getRepository());
        Session systemSession = internalRepository.createSystemSession();
        try {
            assertEquals("system", systemSession.getUserID());
            Session anotherSystemSession = systemSession.impersonate(new SimpleCredentials("system", "foobar".toCharArray()));
            String userId = anotherSystemSession.getUserID();
            anotherSystemSession.logout();
            assertEquals("system", userId);
        } finally {
            systemSession.logout();
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

}
