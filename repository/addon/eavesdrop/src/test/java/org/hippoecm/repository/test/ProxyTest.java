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
package org.hippoecm.repository.test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.HippoRepositoryServer;
import org.hippoecm.repository.proxyrepository.ProxyHippoRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProxyTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    protected HippoRepositoryServer backgroundServer;

    @Before
    public void setUp() throws Exception {
        backgroundServer = new HippoRepositoryServer();
        backgroundServer.run(true);
        Thread.sleep(3000);

        HippoRepository server = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
        Session session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        session.getRootNode().addNode("test","nt:unstructured");
        session.save();
        session.logout();
        server.close();
    }

    @After
    public void tearDown() throws Exception {
        HippoRepository server = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
        Session session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        if(session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
            session.save();
        }
        session.logout();
        server.close();

        backgroundServer.close();
        Thread.sleep(3000);
    }

    @Test
    public void testProxy() throws Exception {
        HippoRepository server = HippoRepositoryFactory.getHippoRepository("proxy:rmi://localhost:1099/hipporepository");
        assertTrue(server instanceof ProxyHippoRepository);

        OutputStream output = new FileOutputStream("dump");
        Session session = ((ProxyHippoRepository)server).login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD, output);
        Node node = session.getRootNode();
        assertNotNull(node);
        assertTrue(node.hasProperty("jcr:primaryType"));
        node = node.getNode("test");
        assertNotNull(node);
        node.addNode("dupe");
        session.save();
        ((ProxyHippoRepository)server).logout(session);

        session = ((ProxyHippoRepository)server).login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD, new FileInputStream("dump"));
        session.logout();
        server.close();

        server = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        assertTrue(session.getRootNode().getNode("test").hasNode("dupe"));
        assertTrue(session.getRootNode().getNode("test").hasNode("dupe[2]"));
        assertFalse(session.getRootNode().getNode("test").hasNode("dupe[3]"));
        session.logout();
        server.close();
    }
}
