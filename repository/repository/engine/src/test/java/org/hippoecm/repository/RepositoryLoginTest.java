/*
 *  Copyright 2008 Hippo.
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

import java.io.IOException;
import java.util.StringTokenizer;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class RepositoryLoginTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private HippoRepository server;
    private Session serverSession;
    private Node users;

    private static final String ANONYMOUS_ID = "anonymous";
    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    private static final String USERS_PATH = "hippo:configuration/hippo:users";

    private static final String TESTUSER_PASS = "testpass";
    private static final String TESTUSER_ID_PLAIN = "testuser-plain";
    private static final String TESTUSER_ID_MD5 = "testuser-md5";
    private static final String TESTUSER_ID_SHA1 = "testuser-sha1";
    private static final String TESTUSER_ID_SHA256 = "testuser-sha256";
    private static final String TESTUSER_HASH_MD5 = "$MD5$LDiazWf2qBc=$JIW7oSBflwFdxzKDnFHKPw==";
    private static final String TESTUSER_HASH_SHA1 = "$SHA-1$LDiazWf2qBc=$VjcsDMKtiRKYushsjTNDuk5a//4=";
    private static final String TESTUSER_HASH_SHA256 = "$SHA-256$LDiazWf2qBc=$/bzV6rjHX+fgx4dVz6oaPcW3kX1ynSJ+vGv1mbbm+v4=";

    @Before
    public void setUp() throws RepositoryException, IOException {
        server = HippoRepositoryFactory.getHippoRepository();
        serverSession = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);

        // create user config path
        Node node = serverSession.getRootNode();
        StringTokenizer tokenizer = new StringTokenizer(USERS_PATH, "/");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            node = node.addNode(token);
        }

        // create test users
        Node testuser;
        users = serverSession.getRootNode().getNode(USERS_PATH);
        testuser = users.addNode(TESTUSER_ID_PLAIN, HippoNodeType.NT_USER);
        testuser.setProperty("hipposys:password", TESTUSER_PASS);
        testuser = users.addNode(TESTUSER_ID_MD5, HippoNodeType.NT_USER);
        testuser.setProperty("hipposys:password", TESTUSER_HASH_MD5);
        testuser = users.addNode(TESTUSER_ID_SHA1, HippoNodeType.NT_USER);
        testuser.setProperty("hipposys:password", TESTUSER_HASH_SHA1);
        testuser = users.addNode(TESTUSER_ID_SHA256, HippoNodeType.NT_USER);
        testuser.setProperty("hipposys:password", TESTUSER_HASH_SHA256);
        serverSession.save();
    }

    @After
    public void tearDown() throws RepositoryException {
        if (users != null) {
            if (users.hasNode(TESTUSER_ID_PLAIN)) {
                users.getNode(TESTUSER_ID_PLAIN).remove();
            }
            if (users.hasNode(TESTUSER_ID_MD5)) {
                users.getNode(TESTUSER_ID_MD5).remove();
            }
            if (users.hasNode(TESTUSER_ID_SHA1)) {
                users.getNode(TESTUSER_ID_SHA1).remove();
            }
            if (users.hasNode(TESTUSER_ID_SHA256)) {
                users.getNode(TESTUSER_ID_SHA256).remove();
            }
        }
        if (serverSession != null) {
            serverSession.save();
            serverSession.logout();
        }
        if (server != null) {
            server.close();
        }
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
    public void testLoginPlainFail() throws Exception {
        Session session = null;
        try {
            session = server.login(TESTUSER_ID_PLAIN, "wrongpassword".toCharArray());
            session.logout();
            fail("Plain login succeeded with invalid credentials");
        } catch (LoginException ex) {
            assertEquals(null, session);
        }
    }

    @Test
    public void testLoginHashFail() throws Exception {
        Session session = null;
        try {
            session = server.login(TESTUSER_ID_SHA1, "wrongpassword".toCharArray());
            session.logout();
            fail("Hashed login succeeded with invalid credentials");
        } catch (LoginException ex) {
            assertEquals(null, session);
        }
    }

    @Test
    public void testLoginNoEmptyPassword() throws Exception {
        Session session = null;
        try {
            session = server.login(TESTUSER_ID_SHA1, "".toCharArray());
            session.logout();
            fail("Login succeeded with empty password");
        } catch (LoginException ex) {
            assertEquals(null, session);
        }
    }

    @Test
    public void testLoginNoNullPassword() throws Exception {
        Session session = null;
        try {
            session = server.login(TESTUSER_ID_SHA1, null);
            session.logout();
            fail("Login succeeded with null password");
        } catch (LoginException ex) {
            assertEquals(null, session);
        }
    }

    @Test
    public void testLoginNullUsername() throws Exception {
        Session session = null;
        try {
            session = server.login(null, TESTUSER_PASS.toCharArray());
            assertEquals(ANONYMOUS_ID, session.getUserID());
            session.logout();
        } catch (LoginException ex) {
            fail("Anonymous login failed with username null");
        }
    }

    @Test
    public void testLoginNullUsernameNullPassword() throws Exception {
        Session session = null;
        try {
            session = server.login(null);
            assertEquals(ANONYMOUS_ID, session.getUserID());
            session.logout();
        } catch (LoginException ex) {
            fail("Anonymous login failed with username null");
        }
    }

    @Test
    public void testLoginAnonymous() throws Exception {
        try {
            Session session = server.login();
            assertEquals(ANONYMOUS_ID, session.getUserID());
            session.logout();
        } catch (LoginException e) {
            fail("Anonymous login failed");
        }
    }

    @Test
    public void testRootSessionOnCredentials() throws RepositoryException {
        SimpleCredentials creds = new SimpleCredentials("nono", "blabla".toCharArray());
        try {
            Session session = server.login(creds);
            fail("this should have failed");
            session.logout();
        } catch (LoginException ex) {
            Object object = creds.getAttribute("rootSession");
            assertNull(object);
        }
    }

    @Test
    public void testImpersonateAsWorkflowUser() throws RepositoryException {
        SimpleCredentials creds = new SimpleCredentials("nono", "blabla".toCharArray());
        Session anonymousSession = server.login();
        assertEquals("anonymous", anonymousSession.getUserID());
        Session workflowSession = serverSession.impersonate(new SimpleCredentials("workflowuser", "anything".toCharArray()));
        anonymousSession.logout();
        assertEquals("workflowuser", workflowSession.getUserID());
        workflowSession.logout();
    }

    @Test
    public void testLogins() throws RepositoryException {
        for (int count = 1; count <= 1024; count = count << 1) {
            long t1 = System.currentTimeMillis();
            Session[] sessions = new Session[count];
            for (int i = 0; i < count; i++) {
                sessions[i] = serverSession.impersonate(new SimpleCredentials("admin", "admin".toCharArray()));
                sessions[i].logout();
            }
            long t2 = System.currentTimeMillis();
            //System.err.println(count+"\t"+(t2-t1)+"\t"+((t2-t1)/count));
        }
    }

    @Test
    public void testConcurrentLogins() throws RepositoryException {
        for (int count = 1; count <= 1024; count = count << 1) {
            long t1 = System.currentTimeMillis();
            Session[] sessions = new Session[count];
            for (int i = 0; i < count; i++) {
                sessions[i] = serverSession.impersonate(new SimpleCredentials("admin", "admin".toCharArray()));
            }
            for (int i = 0; i < count; i++) {
                sessions[i].logout();
            }
            long t2 = System.currentTimeMillis();
            //System.err.println(count+"\t"+(t2-t1)+"\t"+((t2-t1)/count));
        }
    }
}
