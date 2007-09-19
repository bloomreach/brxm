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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.LoginException;
import javax.jcr.AccessDeniedException;

import junit.framework.TestCase;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;

public class LoginTest extends TestCase {
    private HippoRepository server;

    public void setUp() throws Exception {
        server = HippoRepositoryFactory.getHippoRepository();
    }

    public void tearDown() throws Exception {
        server.close();
    }

    public void testLoginAnonymous() throws RepositoryException {
        Session session = null;
        try {
            session = server.login();
        } catch(LoginException ex) {
        }
        assertNotNull("Anonymous user should be able to log in", session);
        try {
            Node node = session.getRootNode().addNode("test");
            session.save();
            fail("anonymous user is not suppost to have writable access");
            node.remove();
            session.save();
        } catch(AccessDeniedException ex) {
        }
        session.logout();
    }

    public void testLoginFail() throws RepositoryException {
        Session session = null;
        try {
            session = server.login("nobody","invalid".toCharArray());
        } catch(LoginException ex) {
        }
        assertNull("Non existent user should not be able to log in", session);
    }

    public void testLoginDummy() throws RepositoryException {
        Session session = null;
        try {
            session = server.login("dummy","dummy".toCharArray());
        } catch(LoginException ex) {
        }
        assertNull("Dummy user should not be able to log in", session);
    }

    /*
    public void testJohndoe() throws RepositoryException {
        Session session = null;
        try {
            session = server.login("johndoe","secret".toCharArray());
        } catch(LoginException ex) {
        }
        assertNotNull("Valid user should be able to log in", session);
        try {
            Node node = session.getRootNode().addNode("test");
            session.save();
            node.remove();
            session.save();
        } catch(AccessDeniedException ex) {
            fail("authenticated user is suppost to have writable access");
        }
        session.logout();
    }

    public void testJohndoeFail() throws RepositoryException {
        Session session = null;
        try {
            session = server.login("johndoe","geheim".toCharArray());
        } catch(LoginException ex) {
        }
        assertNull("Authentication with wrong password should fail", session);
    }
    */

    public void testLoginSystemUser() throws RepositoryException {
        Session session = null;
        try {
            session = server.login("systemuser","systempass".toCharArray());
        } catch(LoginException ex) {
        }
        assertNotNull("Authentication as systemuser should succeed", session);
        try {
            Node node = session.getRootNode().addNode("test");
            session.save();
            node.remove();
            session.save();
        } catch(AccessDeniedException ex) {
            fail("authenticated user is suppost to have writable access");
        }
        session.logout();
    }
}
