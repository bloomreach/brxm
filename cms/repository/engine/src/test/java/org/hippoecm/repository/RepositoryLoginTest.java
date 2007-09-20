/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository;

import java.io.IOException;
import java.util.StringTokenizer;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import junit.framework.TestCase;

public class RepositoryLoginTest extends TestCase {
    private final static String SVN_ID = "$Id:RepositoryLoginTest.java 8167 2007-09-14 13:37:17Z wgrevink $";

    private HippoRepository server;
    private Session systemSession;
    private Node users;

    private static final String TESTUSER_ID = "testuser";
    private static final String TESTUSER_PASS = "testpass";

    private static final String USERS_PATH = "configuration/users";
    private static final String ANONYMOUS_ID = "anonymous";
    private static final String SYSTEMUSER_ID = "systemuser";
    private static final char[] SYSTEMUSER_PASSWORD = "systempass".toCharArray();

    public void setUp() throws RepositoryException, IOException {
        server = HippoRepositoryFactory.getHippoRepository();
        systemSession = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        
        Node node = systemSession.getRootNode();
        StringTokenizer tokenizer = new StringTokenizer(USERS_PATH, "/");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            node = node.addNode(token);
        }
        users = systemSession.getRootNode().getNode(USERS_PATH);
        Node testuser = users.addNode(TESTUSER_ID);
        testuser.setProperty("password", TESTUSER_PASS);
        systemSession.save();
    }

    public void tearDown() throws RepositoryException {
        if (users.getNode(TESTUSER_ID) != null) {
            users.getNode(TESTUSER_ID).remove();
        }
        if (systemSession != null) {
            systemSession.save();
            systemSession.logout();
        }
        if (server != null) {
            server.close();
        }
    }

    public void testLoginSuccess() throws Exception {
        try {
            Session session = server.login(TESTUSER_ID, TESTUSER_PASS.toCharArray());
            assertEquals(TESTUSER_ID, session.getUserID());
            session.logout();
        } catch (LoginException e) {
            fail("Login failed with valid credentials");
        }
    }

    public void testLoginFail() throws Exception {
        Session session = null;
        try {
            session = server.login(TESTUSER_ID, "wrongpassword".toCharArray());
            session.logout();
            fail("Login succeeded with invalid credentials");
        } catch (LoginException ex) {
            assertEquals(null, session);
        }
    }

    public void testLoginNoEmptyPassword() throws Exception {
        Session session = null;
        try {
            session = server.login(TESTUSER_ID, "".toCharArray());
            session.logout();
            fail("Login succeeded with empty password");
        } catch (LoginException ex) {
            assertEquals(null, session);
        }
    }

    public void testLoginAnonymous() throws Exception {
        try {
            Session session = server.login();
            assertEquals(ANONYMOUS_ID, session.getUserID());
            session.logout();
        } catch (LoginException e) {
            fail("Anonymous login failed");
        }
    }
}
