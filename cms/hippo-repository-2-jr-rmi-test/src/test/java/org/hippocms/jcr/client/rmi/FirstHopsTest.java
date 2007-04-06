/*
 * Copyright 2006 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" 
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package org.hippocms.jcr.client.rmi;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

/**
 * @version $Id$
 *
 */
public class FirstHopsTest extends TestCase {
    
    private static final Logger log = LoggerFactory.getLogger(FirstHopsTest.class);
    
    public void testFirstHop() throws Exception {
        ClientRepositoryFactory factory = new ClientRepositoryFactory();
        Repository repository = factory.getRepository("rmi://localhost:1099/jr-standalone");
        assertNotNull( repository );
        Session session = repository.login();
        assertNotNull( session );
        try {
            String user = session.getUserID();
            String name = repository.getDescriptor(Repository.REP_NAME_DESC);
            assertEquals("anonymous",user);
            assertEquals("Jackrabbit",name);
            log.info("FirstHop: Logged in as " + user + " to a " + name + " repository.");
        } finally {
            session.logout();
        }
    }
    
    public void testSecondHop() throws Exception {
        ClientRepositoryFactory factory = new ClientRepositoryFactory();
        Repository repository = factory.getRepository("rmi://localhost:1099/jr-standalone");
        assertNotNull( repository );
        Session session = repository.login(new SimpleCredentials("username", "password".toCharArray()));
        assertNotNull( session );
        try {
            Node root = session.getRootNode();

            // Store content
            Node hello = root.addNode("hello");
            Node world = hello.addNode("world");
            world.setProperty("message", "Hello, World!");
            session.save();

            // Retrieve content
            Node node = root.getNode("hello/world");
            assertNotNull(node);
            assertEquals("/hello/world",node.getPath());
            assertEquals("Hello, World!",node.getProperty("message").getString());
            log.info("SecondHop: "+node.getProperty("message").getString());

            // Remove content
            root.getNode("hello").remove();
            session.save();
        } finally {
            session.logout();
        }
    }
}
