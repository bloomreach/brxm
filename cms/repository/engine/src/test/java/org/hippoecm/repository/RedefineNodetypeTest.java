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

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.spi.Name;

import org.hippoecm.repository.api.HippoNodeType;

import junit.framework.TestCase;

public class RedefineNodetypeTest extends TestCase {

    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    private HippoRepository server;
    private Session session;

    public void setUp() throws RepositoryException {
        server = HippoRepositoryFactory.getHippoRepository();
        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        if(session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
            session.save();
        }
    }

    public void tearDown() throws Exception {
        if(session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
            session.save();
        }
        session.logout();
        server.close();
    }

    private static void waitForRefresh(Session session) throws PathNotFoundException, RepositoryException {
        session.refresh(true);
        Node base = session.getRootNode().getNode("hippo:configuration").getNode("hippo:initialize");
        while(base.getNode("hippotest").hasProperty(HippoNodeType.HIPPO_NODETYPES)) {
            try {
                Thread.sleep(300);
            } catch(InterruptedException ex) {
            }
            session.refresh(true);
        }
    }

    public void testRedefine() throws RepositoryException {
        Node node, root = session.getRootNode().addNode("test");
        try {
            root.addNode("node", "hippotest:test");
            fail("node type should not yet exist");
        } catch(NoSuchNodeTypeException ex) {
            // expected
        }

        node = session.getRootNode().getNode("hippo:configuration").getNode("hippo:initialize");
        node = node.addNode("hippotest");
        node.setProperty(HippoNodeType.HIPPO_NAMESPACE, "http://www.hippoecm.org/test/1.0");
        node.setProperty(HippoNodeType.HIPPO_NODETYPES, "RedefineNodetypeTest-1.cnd");
        session.save();

        // this update is asynchronously
        waitForRefresh(session);

        node = root.addNode("node1", "hippotest:test");
        node.setProperty("hippotest:first", "aap");
        session.save();

        node = session.getRootNode().getNode("hippo:configuration").getNode("hippo:initialize");
        node.getNode("hippotest").remove();
        session.save();
        node = node.addNode("hippotest");
        node.setProperty(HippoNodeType.HIPPO_NAMESPACE, "http://www.hippoecm.org/test/1.1");
        node.setProperty(HippoNodeType.HIPPO_NODETYPES, "RedefineNodetypeTest-2.cnd");
        session.save();

        // this update is asynchronously
        waitForRefresh(session);

        node = root.addNode("node2", "hippotest:test");
        node.setProperty("hippotest:second", "mies");
        session.save();

        NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) session.getWorkspace().getNodeTypeManager();
        NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();

        Name[] nodetypes = ntreg.getRegisteredNodeTypes();
        for(int i=0; i<nodetypes.length; i++) {
            if(nodetypes[i].getNamespaceURI().equals("http://www.hippoecm.org/test/1.0")) {
                // find nodes of this type
                // copy the node, with with the new namespace
                // make a map of old uuid to new uuid
                // remove the old node
                // put new node in a list
            }
        }
        // query for all properties except jcr:uuid with the old uuids as value
        // put new uuid into property according to earlier map
        // 'return nodes list'
    }
}
