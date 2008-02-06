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

import java.rmi.RemoteException;

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
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.RemodelWorkflow;

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

    private static void waitForRefresh(Session session, String prefix) throws PathNotFoundException, RepositoryException {
        session.refresh(true);
        Node base = session.getRootNode().getNode("hippo:configuration").getNode("hippo:initialize");
        while(base.getNode(prefix).hasProperty(HippoNodeType.HIPPO_NODETYPES) ||
              base.getNode(prefix).hasProperty(HippoNodeType.HIPPO_NODETYPESRESOURCE)) {
            try {
                Thread.sleep(300);
            } catch(InterruptedException ex) {
            }
            session.refresh(true);
        }
    }

    /*
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
        node.setProperty(HippoNodeType.HIPPO_NODETYPESRESOURCE, "RedefineNodetypeTest-1.cnd");
        session.save();

        // this update is asynchronously
        waitForRefresh(session, "hippotest");

        node = root.addNode("node1", "hippotest:test");
        node.setProperty("hippotest:first", "aap");
        session.save();

        node = session.getRootNode().getNode("hippo:configuration").getNode("hippo:initialize");
        node.getNode("hippotest").remove();
        session.save();
        node = node.addNode("hippotest");
        node.setProperty(HippoNodeType.HIPPO_NAMESPACE, "http://www.hippoecm.org/test/1.1");
        node.setProperty(HippoNodeType.HIPPO_NODETYPESRESOURCE, "RedefineNodetypeTest-2.cnd");
        session.save();

        // this update is asynchronously
        waitForRefresh(session, "hippotest");

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
    */

    public void testWorkflow() throws RepositoryException, WorkflowException, RemoteException {

        String cnd1 =
            "<rep='internal'>\n" +
            "<jcr='http://www.jcp.org/jcr/1.0'>\n" +
            "<nt='http://www.jcp.org/jcr/nt/1.0'>\n" +
            "<mix='http://www.jcp.org/jcr/mix/1.0'>\n" +
            "<hippo='http://www.hippoecm.org/nt/1.0'>\n" +
            "<hippotest2='http://www.hippoecm.org/test2/1.0'>\n" +
            "\n" +
            "[hippotest2:test] > nt:base\n" +
            "- hippotest2:first (string) mandatory\n";
        String cnd2 =
            "<rep='internal'>\n" +
            "<jcr='http://www.jcp.org/jcr/1.0'>\n" +
            "<nt='http://www.jcp.org/jcr/nt/1.0'>\n" +
            "<mix='http://www.jcp.org/jcr/mix/1.0'>\n" +
            "<hippo='http://www.hippoecm.org/nt/1.0'>\n" +
            "<hippotest2='http://www.hippoecm.org/test2/1.1'>\n" +
            "\n" +
            "[hippotest2:test] > nt:base\n" +
            "- hippotest2:second (string) mandatory\n";

        session.getRootNode().addNode("test");

        Node node, base = session.getRootNode().getNode("hippo:configuration").getNode("hippo:initialize");
        node = base.addNode("hippotest2");
        node.setProperty(HippoNodeType.HIPPO_NAMESPACE, "http://www.hippoecm.org/test2/1.0");
        node.setProperty(HippoNodeType.HIPPO_NODETYPES, cnd1);
        session.save();
        waitForRefresh(session, "hippotest2");

        node = session.getRootNode().getNode("test").addNode("testing", "hippotest2:test");
        node.setProperty("hippotest2:first","aap");
        session.save();

        WorkflowManager wfmgr = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
        Workflow wf = wfmgr.getWorkflow("internal", base.getNode("hippotest2"));
        assertNotNull(wf);
        assertTrue(wf instanceof RemodelWorkflow);
        String[] nodes = ((RemodelWorkflow)wf).remodel(cnd2);
        assertNotNull(nodes);

        org.hippoecm.repository.Utilities.dump(session.getRootNode());
        node = session.getRootNode().getNode("test").getNode("testing");
        assertTrue(node.getPrimaryNodeType().getName().equals("hippotest2_1.0:test"));
    }
}
