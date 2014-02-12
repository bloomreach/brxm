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
package org.hippoecm.repository.reviewedactions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

public class AcceptRequestTest extends RepositoryTestCase {

    protected static final String LOREM = "Lorem ipsum dolor sit amet";

    protected WorkflowManager workflowMgr = null;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        Node node, root = session.getRootNode();
        while(root.hasNode("test")) {
            root.getNode("test").remove();
            root.save();
            root.refresh(false);
        }
        node = root.addNode("test");
        node = node.addNode("myarticle", "hippo:handle");
        node.addMixin("mix:referenceable");
        node = node.addNode("myarticle", "hippostdpubwf:test");
        node.addMixin("mix:versionable");
        node.addMixin("hippostd:publishableSummary");
        node.setProperty("hippostdpubwf:content", LOREM);
        node.setProperty("hippostd:holder", "admin");
        node.setProperty("hippostd:state", "published");
        node.setProperty("hippo:availability", new String[] { "live", "preview" });
        node.setProperty("hippostd:stateSummary", "live");
        node.setProperty("hippostdpubwf:createdBy", "admin");
        node.setProperty("hippostdpubwf:creationDate", "2010-02-04T16:32:28.068+02:00");
        node.setProperty("hippostdpubwf:lastModifiedBy", "admin");
        node.setProperty("hippostdpubwf:lastModificationDate", "2010-02-04T16:32:28.068+02:00");

        session.save();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        Node root = session.getRootNode();
        while(root.hasNode("test")) {
            root.getNode("test").remove();
        }
        root.save();
        super.tearDown();
    }

    @Test
    public void testRequestWhenModified() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Node node = null;
        
        // set proper fixture
        {
            node = getNode("test/myarticle/myarticle");
            node.setProperty("hippostd:state", "published");
            session.save();
        }
        
        // steps taken by an author
        {
            node = getNode("test/myarticle/myarticle");
            BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            assertNotNull("No applicable workflow where there should be one", workflow);
            Document document = workflow.obtainEditableInstance();
            session.save();
            session.refresh(true);
            node = getNode("test/myarticle/myarticle[@hippostd:state='draft']");
            assertTrue(node.getIdentifier().equals(document.getIdentity()));
            Property prop = node.getProperty("hippostdpubwf:content");
            prop.setValue(prop.getString() + ",");
            session.save();
            session.refresh(true);

            node = getNode("test/myarticle/myarticle[@hippostd:state='draft']");
            BasicReviewedActionsWorkflow reviewedWorkflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            reviewedWorkflow.commitEditableInstance();
            session.save();
            session.refresh(true);

            reviewedWorkflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            assertNotNull("No applicable workflow where there should be one", reviewedWorkflow);
            reviewedWorkflow.requestPublication();
            session.save();
            session.refresh(true);
        }

        // These steps would be taken by editor:
        {
            node = getNode("test/myarticle/hippo:request[@hippostdpubwf:type='publish']");
            FullRequestWorkflow workflow = (FullRequestWorkflow) getWorkflow(node, "default");
            workflow.acceptRequest();
            session.save();
            session.refresh(true);
        }
    }

    @Test
    public void testScheduledPublication() throws WorkflowException, MappingException, RepositoryException, RemoteException, InterruptedException {
        Node node;
        int secondsToWait;
        boolean found;

        // set proper fixture
        {
            node = getNode("test/myarticle/myarticle");
            node.setProperty("hippostd:state", "unpublished");
            session.save();
            node = getNode("test/myarticle/myarticle[@hippostd:state='unpublished']");
            assertNotNull(node);
            assertEquals("unpublished", node.getProperty("hippostd:state").getString());
            node = getNode("test/myarticle/myarticle[@hippostd:state='published']");
            assertNull(node);
        }

        // steps taken by an author
        {
            node = getNode("test/myarticle/myarticle[@hippostd:state='unpublished']");
            BasicReviewedActionsWorkflow reviewedWorkflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            assertNotNull("No applicable workflow where there should be one", reviewedWorkflow);

            reviewedWorkflow.requestPublication(new Date(System.currentTimeMillis()+100));
            session.save();
        }

        // These steps would be taken by editor:
        {
            secondsToWait = 5;
            found = false;
            do {
                session.refresh(false);
                node = getNode("test/myarticle/hippo:request");
                if (node != null) {
                    FullRequestWorkflow workflow = (FullRequestWorkflow) getWorkflow(node, "default");
                    Map<String, Serializable> hints = workflow.hints();
                    if (hints.containsKey("acceptRequest") && ((Boolean)hints.get("acceptRequest")).booleanValue()) {
                        workflow.acceptRequest();
                        session.save();
                        found = true;
                    }
                    else {
                        Assert.fail("Publication request not acceptable");
                    }
                }
                else {
                    secondsToWait--;
                    if (secondsToWait > 0) {
                        Thread.sleep(1000);
                    }
                }

            } while (!found && secondsToWait > 0);

            if (!found) {
                Assert.fail("No acceptable publication request found");
            }
        }

        secondsToWait = 5;
        found = false;

        do {
            session.refresh(false);
            node = getNode("test/myarticle/myarticle[@hippostd:state='published']");
            if (node != null) {
                assertEquals("published", node.getProperty("hippostd:state").getString());
                final Value[] values = node.getProperty("hippo:availability").getValues();
                assertEquals(1, values.length);
                found = true;
            }
            else {
                secondsToWait--;
                if (secondsToWait > 0) {
                    Thread.sleep(1000);
                }
            }
        } while (!found && secondsToWait > 0);

        if (!found) {
            Assert.fail("Published document not found");
        }
    }
 
    protected Workflow getWorkflow(Node node, String category) throws RepositoryException {
        if (workflowMgr == null) {
            HippoWorkspace wsp = (HippoWorkspace) node.getSession().getWorkspace();
            workflowMgr = wsp.getWorkflowManager();
        }
        Node canonicalNode = ((HippoNode) node).getCanonicalNode();
        return workflowMgr.getWorkflow(category, canonicalNode);
    }

    protected Workflow getWorkflow(Document document, String category) throws RepositoryException {
        if (workflowMgr == null) {
            HippoWorkspace wsp = (HippoWorkspace) session.getWorkspace();
            workflowMgr = wsp.getWorkflowManager();
        }
        return workflowMgr.getWorkflow(category, document);
    }

    protected Node getNode(String path) throws RepositoryException {
        return ((HippoWorkspace)session.getWorkspace()).getHierarchyResolver().getNode(session.getRootNode(), path);
    }
}
