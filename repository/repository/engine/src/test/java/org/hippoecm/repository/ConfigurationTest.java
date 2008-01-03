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
import javax.jcr.Session;
import javax.jcr.RepositoryException;

import junit.framework.TestCase;

public class ConfigurationTest extends TestCase {

    private final static String SVN_ID = "$Id$";
    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    private HippoRepository server;
    private Session session;

    /**
     * Handle atomikos setup and create transaction test node
     */
    public void setUp() throws Exception {
        server = HippoRepositoryFactory.getHippoRepository();
        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
    }
   
    public void tearDown() throws Exception {
        Node root = session.getRootNode();
        try {
            root.getNode("configtest").remove();
            root.getNode("hippo:configuration/hippo:initialize/testnode").remove();
        } catch (RepositoryException e) {
            // ignore
        }
        session.save();
        session.logout();
        server.close();
    }

    public synchronized void testConfiguration() throws Exception {
        Node root = session.getRootNode();
        Node node = root.addNode("hippo:configuration/hippo:initialize/testnode", "hippo:initializeitem");
        node.setProperty("hippo:content", "configtest.xml");
        node.setProperty("hippo:contentroot", "/configtest");
        session.save();

        // observation manager calls listeners asynchronously  
        wait(1000);

        node = root.getNode("configtest");
        assertNotNull(node.getNode("testnode"));
    }
}
