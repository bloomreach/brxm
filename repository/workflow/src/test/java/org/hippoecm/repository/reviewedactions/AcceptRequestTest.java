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
import static org.junit.Assert.assertFalse;
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
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

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
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.testutils.RepositoryTestCase;

public class AcceptRequestTest extends RepositoryTestCase {

    protected static final String LOREM = "Lorem ipsum dolor sit amet";

    protected WorkflowManager workflowMgr = null;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        removeNode("/test");

        Node test = session.getRootNode().addNode("test");
        Node handle = test.addNode("myarticle", "hippo:handle");
        handle.addMixin("mix:referenceable");
        Node variant = handle.addNode("myarticle", "hippostdpubwf:test");
        variant.addMixin("mix:versionable");
        variant.addMixin("hippostd:publishableSummary");
        variant.setProperty("hippostdpubwf:content", LOREM);
        variant.setProperty("hippostd:holder", "admin");
        variant.setProperty("hippostd:state", "published");
        variant.setProperty("hippo:availability", new String[] { "live", "preview" });
        variant.setProperty("hippostd:stateSummary", "live");
        variant.setProperty("hippostdpubwf:createdBy", "admin");
        variant.setProperty("hippostdpubwf:creationDate", "2010-02-04T16:32:28.068+02:00");
        variant.setProperty("hippostdpubwf:lastModifiedBy", "admin");
        variant.setProperty("hippostdpubwf:lastModificationDate", "2010-02-04T16:32:28.068+02:00");

        session.save();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        removeNode("/test");
        super.tearDown();
    }

    @Test
    public void testRequestWhenModified() throws Exception {
        
        // set up
        Node variant = getNode("test/myarticle/myarticle");
        variant.setProperty("hippostd:state", "published");
        session.save();

        // steps taken by an author
        final Node handle = getNode("test/myarticle");
        DocumentWorkflow workflow = (DocumentWorkflow) getWorkflow(handle, "default");
        assertNotNull("No document workflow on handle", workflow);

        Document document = workflow.obtainEditableInstance();
        Node draft = document.getNode(session);
        Property prop = draft.getProperty("hippostdpubwf:content");
        prop.setValue("edited");
        session.save();
        workflow.commitEditableInstance();

        workflow.requestPublication();

        poll(new Executable() {
            @Override
            public void execute() throws Exception {
                assertTrue("Publication request not found", handle.hasNode("hippo:request"));
            }
        }, 10);

        Node request = handle.getNode("hippo:request");
        workflow.acceptRequest(request.getIdentifier());

        poll(new Executable() {
            @Override
            public void execute() throws Exception {
                assertFalse("Scheduled publication not executed", handle.hasNode("hippo:request"));
            }
        }, 10);

        Node published = getNode("test/myarticle/myarticle[@hippostd:state='published']");
        if (published != null) {
            final Value[] values = published.getProperty("hippo:availability").getValues();
            assertEquals(1, values.length);
            assertEquals("edited", published.getProperty("hippostdpubwf:content").getString());
        } else {
            Assert.fail("Published document not found");
        }

    }

    @Test
    public void testScheduledPublication() throws Exception {

        // Set up
        Node variant = getNode("test/myarticle/myarticle");
        variant.setProperty("hippostd:state", "unpublished");
        session.save();

        // Request publication
        final Node handle = getNode("test/myarticle");
        DocumentWorkflow workflow = (DocumentWorkflow) getWorkflow(handle, "default");
        assertNotNull("No applicable workflow where there should be one", workflow);

        workflow.requestPublication(new Date(System.currentTimeMillis() + 1000));
        session.save();

        poll(new Executable() {
            @Override
            public void execute() throws Exception {
                assertTrue("Publication request not found", handle.hasNode("hippo:request"));
            }
        }, 10);

        // Accept request for publication
        Node request = handle.getNode("hippo:request");
        workflow.acceptRequest(request.getIdentifier());

        poll(new Executable() {
            @Override
            public void execute() throws Exception {
                assertFalse("Publication not executed", handle.hasNode("hippo:request"));
            }
        }, 10);

        Node published = getNode("test/myarticle/myarticle[@hippostd:state='published']");
        if (published != null) {
            assertEquals("published", published.getProperty("hippostd:state").getString());
            final Value[] values = published.getProperty("hippo:availability").getValues();
            assertEquals(1, values.length);
        } else {
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

    private void poll(Executable executable, int seconds) throws Exception {
        while (true) {
            try {
                executable.execute();
                return;
            } catch (AssertionError e) {
                if (seconds-- <= 0) {
                    throw e;
                }
                Thread.sleep(1000);
            }
        }
    }

    private static interface Executable {
        void execute() throws Exception;
    }
}
