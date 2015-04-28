/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.security.JvmCredentials;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PASSKEY;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PASSWORD;
import static org.hippoecm.repository.api.HippoNodeType.NT_USER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class RepositoryLoginTest extends RepositoryTestCase {

    private static final Logger log = LoggerFactory.getLogger(RepositoryLoginTest.class);

    private static final String ANONYMOUS_ID = "anonymous";

    private static final String USERS_PATH = "/hippo:configuration/hippo:users";

    private static final String TESTUSER_PASS = "testpass";
    private static final String TESTUSER_ID_PLAIN = "testuser-plain";
    private static final String TESTUSER_ID_MD5 = "testuser-md5";
    private static final String TESTUSER_ID_SHA1 = "testuser-sha1";
    private static final String TESTUSER_ID_SHA256 = "testuser-sha256";
    private static final String TESTUSER_JVM = "testuser-jvm";
    private static final String TESTUSER_HASH_MD5 = "$MD5$LDiazWf2qBc=$JIW7oSBflwFdxzKDnFHKPw==";
    private static final String TESTUSER_HASH_SHA1 = "$SHA-1$LDiazWf2qBc=$VjcsDMKtiRKYushsjTNDuk5a//4=";
    private static final String TESTUSER_HASH_SHA256 = "$SHA-256$LDiazWf2qBc=$/bzV6rjHX+fgx4dVz6oaPcW3kX1ynSJ+vGv1mbbm+v4=";

    private static final int NUM_CONCURRENT_LOGINS = 25;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        final Node users = session.getNode(USERS_PATH);
        users.addNode(TESTUSER_ID_PLAIN, NT_USER).setProperty(HIPPO_PASSWORD, TESTUSER_PASS);
        users.addNode(TESTUSER_ID_MD5, NT_USER).setProperty(HIPPO_PASSWORD, TESTUSER_HASH_MD5);
        users.addNode(TESTUSER_ID_SHA1, NT_USER).setProperty(HIPPO_PASSWORD, TESTUSER_HASH_SHA1);
        users.addNode(TESTUSER_ID_SHA256, NT_USER).setProperty(HIPPO_PASSWORD, TESTUSER_HASH_SHA256);
        users.addNode(TESTUSER_JVM, NT_USER).setProperty(HIPPO_PASSKEY, JvmCredentials.PASSKEY);
        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        final Node users = session.getNode(USERS_PATH);
        users.getNode(TESTUSER_ID_PLAIN).remove();
        users.getNode(TESTUSER_ID_MD5).remove();
        users.getNode(TESTUSER_ID_SHA1).remove();
        users.getNode(TESTUSER_ID_SHA256).remove();
        users.getNode(TESTUSER_JVM).remove();
        session.save();
        super.tearDown();
    }

    @Test
    public void testLoginPlainSuccess() throws Exception {
        try {
            Session session = server.login(TESTUSER_ID_PLAIN, TESTUSER_PASS.toCharArray());
            assertEquals(TESTUSER_ID_PLAIN, session.getUserID());
            session.logout();
        } catch (LoginException e) {
            fail("Plain login failed with valid credentials");
        }
    }

    @Test
    public void testLoginMD5Success() throws Exception {
        try {
            Session session = server.login(TESTUSER_ID_MD5, TESTUSER_PASS.toCharArray());
            assertEquals(TESTUSER_ID_MD5, session.getUserID());
            session.logout();
        } catch (LoginException e) {
            fail("MD5 login failed with valid credentials");
        }
    }

    @Test
    public void testLoginSHA1Success() throws Exception {
        try {
            Session session = server.login(TESTUSER_ID_SHA1, TESTUSER_PASS.toCharArray());
            assertEquals(TESTUSER_ID_SHA1, session.getUserID());
            session.logout();
        } catch (LoginException e) {
            fail("SHA-1 login failed with valid credentials");
        }
    }

    @Test
    public void testLoginSHA256Success() throws Exception {
        try {
            Session session = server.login(TESTUSER_ID_SHA256, TESTUSER_PASS.toCharArray());
            assertEquals(TESTUSER_ID_SHA256, session.getUserID());
            session.logout();
        } catch (LoginException e) {
            fail("SHA-256 login failed with valid credentials");
        }
    }

    @Test
    public void testLoginJvmUser() throws Exception {
        try {
            final Session session = server.login(JvmCredentials.getCredentials(TESTUSER_JVM));
            assertEquals(TESTUSER_JVM, session.getUserID());
            session.logout();
        } catch (LoginException e) {
            fail("Jvm user login failed");
        }
    }

    @Test
    public void testLoginJvmUserRepositoryObtainedViaSession() throws Exception {
        try {
            final Session session1 = server.login(JvmCredentials.getCredentials(TESTUSER_JVM));
            assertEquals(TESTUSER_JVM, session1.getUserID());

            final Session session2 = session1.getRepository().login(JvmCredentials.getCredentials(TESTUSER_JVM));
            assertEquals(TESTUSER_JVM, session2.getUserID());

            session1.logout();
            session2.logout();
        } catch (LoginException e) {
            fail("Jvm user login failed");
        }
    }

    @Test(expected = LoginException.class)
    public void testLoginPlainFail() throws Exception {
        Session session = server.login(TESTUSER_ID_PLAIN, "wrongpassword".toCharArray());
        session.logout();
    }

    @Test(expected = LoginException.class)
    public void testLoginHashFail() throws Exception {
        Session session =  server.login(TESTUSER_ID_SHA1, "wrongpassword".toCharArray());
        session.logout();
    }

    @Test(expected = LoginException.class)
    public void testLoginNoEmptyPassword() throws Exception {
        Session session = server.login(TESTUSER_ID_SHA1, "".toCharArray());
        session.logout();
    }

    @Test(expected = LoginException.class)
    public void testLoginNoNullPassword() throws Exception {
        Session session = server.login(TESTUSER_ID_SHA1, null);
        session.logout();
    }

    @Test
    public void testLoginNullUsername() throws Exception {
        try {
            final Session session = server.login(null, TESTUSER_PASS.toCharArray());
            assertEquals(ANONYMOUS_ID, session.getUserID());
            session.logout();
        } catch (LoginException ex) {
            fail("Anonymous login failed with username null");
        }
    }

    @Test
    public void testLoginNullUsernameNullPassword() throws Exception {
        try {
            final Session session = server.login(null);
            assertEquals(ANONYMOUS_ID, session.getUserID());
            session.logout();
        } catch (LoginException ex) {
            fail("Anonymous login failed with username null");
        }
    }

    @Test
    public void testLoginAnonymous() throws Exception {
        try {
            final Session session = server.login();
            assertEquals(ANONYMOUS_ID, session.getUserID());
            session.logout();
        } catch (LoginException e) {
            fail("Anonymous login failed");
        }
    }

    @Test
    public void testImpersonateAsWorkflowUser() throws RepositoryException {
        Session anonymousSession = server.login();
        assertEquals("anonymous", anonymousSession.getUserID());
        Session workflowSession = session.impersonate(new SimpleCredentials("workflowuser", "anything".toCharArray()));
        anonymousSession.logout();
        assertEquals("workflowuser", workflowSession.getUserID());
        workflowSession.logout();
    }

    @Test
    @Ignore
    public void testLogins() throws RepositoryException {
        for (int count = 1; count <= NUM_CONCURRENT_LOGINS; count = count << 1) {
            long t1 = System.currentTimeMillis();
            Session[] sessions = new Session[count];
            for (int i = 0; i < count; i++) {
                sessions[i] = session.impersonate(new SimpleCredentials("admin", "admin".toCharArray()));
                sessions[i].logout();
            }
            long t2 = System.currentTimeMillis();
            log.info(count + "\t" + (t2 - t1) + "\t" + ((t2 - t1) / count));
        }
    }

    @Test
    @Ignore
    public void testConcurrentLogins() throws RepositoryException {
        for (int count = 1; count <= NUM_CONCURRENT_LOGINS; count = count << 1) {
            long t1 = System.currentTimeMillis();
            Session[] sessions = new Session[count];
            for (int i = 0; i < count; i++) {
                sessions[i] = session.impersonate(new SimpleCredentials("admin", "admin".toCharArray()));
            }
            for (int i = 0; i < count; i++) {
                sessions[i].logout();
            }
            long t2 = System.currentTimeMillis();
            log.info(count + "\t" + (t2 - t1) + "\t" + ((t2 - t1) / count));
        }
    }
}
