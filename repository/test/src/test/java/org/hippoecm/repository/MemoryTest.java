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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MemoryTest extends FacetedNavigationAbstractTest {


    private static final String TEST_USER_ID = "testuser";
    private static final String TEST_USER_PASS = "password";

    private static final int NUMBER_OF_LOGINS = 2;
    private static final int NUMBER_OF_TRAVERSE_LOGINS = 2;
    private static final int NUMBER_OF_TRAVERSE_NODES = 10;
    private static final int NUMBER_OF_GCS = 2;
    private static final long GC_DELAY_MS = 2;
    private static final long FINITSH_DELAY_MS = 1;

    public void cleanup() throws RepositoryException  {
        Node config = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH);
        Node users = config.getNode(HippoNodeType.USERS_PATH);
        if (users.hasNode(TEST_USER_ID)) {
            users.getNode(TEST_USER_ID).remove();
        }
        session.save();
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        cleanup();
        Node config = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH);
        Node users = config.getNode(HippoNodeType.USERS_PATH);

        // create test user
        Node testUser = users.addNode(TEST_USER_ID, HippoNodeType.NT_USER);
        testUser.setProperty(HippoNodeType.HIPPO_PASSWORD, TEST_USER_PASS);
        session.save();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        Node config = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH);
        Node users = config.getNode(HippoNodeType.USERS_PATH);

        if (users.hasNode(TEST_USER_ID)) {
            users.getNode(TEST_USER_ID).remove();
        }
        session.save();

        super.tearDown();
    }
    
    /**
     * Increase NUMBER_OF_TRAVERSE_LOGINS and/or NUMBER_OF_TRAVERSE_NODES and run with:
     *  mvn -o test -Dtest=MemoryTest -Dmaven.surefire.debug="-agentlib:yjpagent -Xmx128m"
     *  and make a memorydump during FINISH_DELAY_MS
     * @throws RepositoryException
     */
    @Test
    public void testManyLoginsWithTraverse() throws RepositoryException {
        commonStart(NUMBER_OF_TRAVERSE_NODES);
        // setup user session
        Session userSession = null;
        for (int i = 0; i < NUMBER_OF_TRAVERSE_LOGINS; i++) {
            userSession = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
            Node node = userSession.getRootNode().getNode("test/navigation/xyz");
            for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                Node child = iter.nextNode();
                if (!"jcr:system".equals(child.getName())) {
                    traverse(child);
               }
           }

            userSession.logout();
        }
        for (int i = 0; i < NUMBER_OF_GCS; i++) {
            System.gc();
            try {
                Thread.sleep(GC_DELAY_MS);
            } catch(InterruptedException ex) {
            }
        }
        try {
            Thread.sleep(FINITSH_DELAY_MS);
        } catch(InterruptedException ex) {
        }
        commonEnd();
    }

    /**
     * Increase NUMBER_OF_LOGINS and run with:
     *  mvn -o test -Dtest=MemoryTest -Dmaven.surefire.debug="-agentlib:yjpagent -Xmx128m"
     *  and make a memorydump during FINISH_DELAY_MS
     * @throws RepositoryException
     */
    @Test
    public void testManyLogins() throws RepositoryException {
        // setup user session
        Session userSession = null;

        for (int i = 0; i < NUMBER_OF_LOGINS; i++) {
            userSession = server.login(TEST_USER_ID, TEST_USER_PASS.toCharArray());
            userSession.logout();
        }
        for (int i = 0; i < NUMBER_OF_GCS; i++) {
            System.gc();
            try {
                Thread.sleep(GC_DELAY_MS);
            } catch(InterruptedException ex) {
            }
        }
        try {
            Thread.sleep(FINITSH_DELAY_MS);
        } catch(InterruptedException ex) {
        }
    }
}

