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

import javax.jcr.LoginException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.RepositoryException;
import org.junit.Ignore;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * An example class to show how to write unit tests for the repository.
 */
public class BoilerPlateTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Ignore
    public void testRootSessionOnCredentials() throws RepositoryException {
        SimpleCredentials creds = new SimpleCredentials("nono", "blabla".toCharArray());
        try {
            Session session = server.login(creds);
            fail("this should have failed");
            session.logout();
        } catch (LoginException ex) {
            Object object = creds.getAttribute("rootSession");
            assertNotNull(object);
            assertTrue(object instanceof Session);
        }
    }

    @Ignore
    public void testImpersonateAsWorkflowUser() throws RepositoryException {
        SimpleCredentials creds = new SimpleCredentials("nono", "blabla".toCharArray());
        Session anonymousSession = server.login();
        assertEquals("anonymous", anonymousSession.getUserID());
        Session workflowSession = session.impersonate(new SimpleCredentials("workflowuser", "anything".toCharArray()));
        anonymousSession.logout();
        assertEquals("workflowuser", workflowSession.getUserID());
        workflowSession.logout();
    }

    @Ignore
    public void testLogins() throws RepositoryException {
        for (int count = 1; count <= 1024; count = count << 1) {
            long t1 = System.currentTimeMillis();
            Session[] sessions = new Session[count];
            for (int i = 0; i < count; i++) {
                sessions[i] = session.impersonate(new SimpleCredentials("admin", "admin".toCharArray()));
                sessions[i].logout();
            }
            long t2 = System.currentTimeMillis();
            System.err.println(count+"\t"+(t2-t1)+"\t"+((t2-t1)/count));
        }
    }
    @Test
    public void testConcurrentLogins() throws RepositoryException {
        for (int count = 1; count <= 1024; count = count << 1) {
            long t1 = System.currentTimeMillis();
            Session[] sessions = new Session[count];
            for (int i = 0; i < count; i++) {
                sessions[i] = session.impersonate(new SimpleCredentials("admin", "admin".toCharArray()));
            }
            for (int i = 0; i < count; i++) {
                sessions[i].logout();
            }
            long t2 = System.currentTimeMillis();
            System.err.println(count+"\t"+(t2-t1)+"\t"+((t2-t1)/count));
        }
    }
}

