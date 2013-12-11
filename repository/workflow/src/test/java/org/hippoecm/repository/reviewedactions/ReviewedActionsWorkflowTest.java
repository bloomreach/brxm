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

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ReviewedActionsWorkflowTest extends RepositoryTestCase {

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
        node.setProperty("hippostdpubwf:content", LOREM);
        node.setProperty("hippostd:holder", "admin");
        node.setProperty("hippostd:state", "unpublished");
        node.setProperty("hippo:availability", new String[] { "preview" });
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
        session.refresh(false);
        while(root.hasNode("test")) {
            root.getNode("test").remove();
        }
        root.save();
        super.tearDown();
    }

    @Test
    public void testBasic() throws WorkflowException, RepositoryException, RemoteException {
        Node node = getNode("test/myarticle/myarticle");
        FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
        workflow.publish();
    }

    @Test
    public void testObtainEditableInstanceReturnsDraft() throws RepositoryException, WorkflowException, RemoteException {
        Node node = getNode("test/myarticle/myarticle");
        FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
        Document document = workflow.obtainEditableInstance();

        node = getNode("test/myarticle/myarticle[@hippostd:state='draft']");

        assertNotNull(node);
        assertNotNull(document);
        assertEquals(node.getIdentifier(), document.getIdentity());
    }

    @Test
    public void testDraftNotSearchable() throws RepositoryException, WorkflowException, RemoteException {
        Node node = getNode("test/myarticle/myarticle[@hippostd:state='unpublished']");
        FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
        workflow.obtainEditableInstance();

        String draftPath = node.getPath();
        String xpath = "//*[@hippostd:state='draft']";
        Query query = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath");
        QueryResult queryResult = query.execute();

        final NodeIterator nodes = queryResult.getNodes();
        while (nodes.hasNext()) {
            // we should not find the added draft as drafts do not get indexed!
            if (nodes.nextNode().getPath().equals(draftPath)) {
                fail("Newly created draft document should not be part of search results");
            }
        }
    }

    @Test
    public void editedContentEndsUpInUnpublished() throws RepositoryException, WorkflowException, RemoteException {
        // steps taken by an author
        Node node = getNode("test/myarticle/myarticle");
        BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
        Document document = workflow.obtainEditableInstance();

        node = getNode("test/myarticle/myarticle[@hippostd:state='draft']");
        assertEquals(node.getIdentifier(), document.getIdentity());

        Property prop = node.getProperty("hippostdpubwf:content");
        prop.setValue("edited content");
        session.save();

        BasicReviewedActionsWorkflow reviewedWorkflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
        reviewedWorkflow.commitEditableInstance();

        node = getNode("test/myarticle/myarticle[@hippostd:state='unpublished']");
        assertEquals("edited content", node.getProperty("hippostdpubwf:content").getString());
    }

    @Test
    public void publishPublishesDocument() throws RepositoryException, RemoteException, WorkflowException {
        Node node;

        // steps taken by an author
        node = getNode("test/myarticle/myarticle[@hippostd:state='unpublished']");
        FullReviewedActionsWorkflow reviewedWorkflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
        assertNotNull("No applicable workflow where there should be one", reviewedWorkflow);
        reviewedWorkflow.publish();

        node = getNode("test/myarticle/myarticle[@hippostd:state='published']");
        PublishableDocument document = new PublishableDocument(node);
        assertTrue("Published variant is not available live after publication", document.isAvailable("live"));
        assertFalse("Published variant is available in preview after publication", document.isAvailable("preview"));
    }

    @Test
    public void publishActionIsDisabledAfterPublishingDocument() throws RepositoryException, RemoteException, WorkflowException {
        Node node;

        // steps taken by an author
        node = getNode("test/myarticle/myarticle[@hippostd:state='unpublished']");
        FullReviewedActionsWorkflow reviewedWorkflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
        assertNotNull("No applicable workflow where there should be one", reviewedWorkflow);
        reviewedWorkflow.publish();

        final Map<String, Serializable> hints = reviewedWorkflow.hints();
        assertFalse("document is still publishable, immediately after publication", (Boolean) hints.get("publish"));
    }

    @Test
    public void cannotEditWithPendingRequest() throws RepositoryException, RemoteException, WorkflowException {
        Node node;

        node = getNode("test/myarticle/myarticle[@hippostd:state='unpublished']");
        FullReviewedActionsWorkflow reviewedWorkflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
        assertNotNull("No applicable workflow where there should be one", reviewedWorkflow);
        reviewedWorkflow.publish(); // make sure 'published' variant exists
        reviewedWorkflow.requestPublication();

        for (Node docNode : new NodeIterable(getNode("test/myarticle").getNodes("myarticle"))) {
            FullReviewedActionsWorkflow frw = (FullReviewedActionsWorkflow) getWorkflow(docNode, "default");
            Map<String,Serializable> hints = frw.hints();
            Serializable editable = hints.get("obtainEditableInstance");
            assertEquals(false, editable);
        }
    }

    @Test
    public void cannotPublishOrDepublishWhenInUse() throws RepositoryException, RemoteException, WorkflowException {
        Node node;

        node = getNode("test/myarticle/myarticle[@hippostd:state='unpublished']");
        FullReviewedActionsWorkflow reviewedWorkflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
        assertNotNull("No applicable workflow where there should be one", reviewedWorkflow);
        Document document = reviewedWorkflow.obtainEditableInstance();
        assertNotNull(document);
        for (Node docNode : new NodeIterable(getNode("test/myarticle").getNodes("myarticle"))) {
            FullReviewedActionsWorkflow frw = (FullReviewedActionsWorkflow) getWorkflow(docNode, "default");
            Map<String,Serializable> hints = frw.hints();
            Serializable publishable = hints.get("publish");
            assertEquals(false, publishable);
            Serializable depublishable = hints.get("depublish");
            assertEquals(false, depublishable);
        }
    }

    @Test
    public void canPublishAfterDepublish() throws RepositoryException, RemoteException, WorkflowException {
        Node node;

        node = getNode("test/myarticle/myarticle[@hippostd:state='unpublished']");
        FullReviewedActionsWorkflow reviewedWorkflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
        reviewedWorkflow.publish();

        node = getNode("test/myarticle/myarticle[@hippostd:state='published']");
        reviewedWorkflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
        reviewedWorkflow.depublish();

        node = getNode("test/myarticle/myarticle[@hippostd:state='unpublished']");
        reviewedWorkflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
        Serializable value = reviewedWorkflow.hints().get("publish");
        assertNotNull("No publish hint provided where there should be one", value);
        assertTrue("Publish hint should be true", value instanceof Boolean && ((Boolean)value).booleanValue());
    }

    @Test
    public void acceptedPublicationRequestPublishesDocument() throws RepositoryException, RemoteException, WorkflowException {
        Node node;

        // steps taken by an author
        node = getNode("test/myarticle/myarticle[@hippostd:state='unpublished']");
        BasicReviewedActionsWorkflow reviewedWorkflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
        assertNotNull("No applicable workflow where there should be one", reviewedWorkflow);
        reviewedWorkflow.requestPublication();

        // These steps would be taken by editor:
        node = getNode("test/myarticle/hippo:request[@hippostdpubwf:type='publish']");
        FullRequestWorkflow frw = (FullRequestWorkflow) getWorkflow(node, "default");
        frw.acceptRequest();

        node = getNode("test/myarticle/myarticle[@hippostd:state='published']");
        PublishableDocument document = new PublishableDocument(node);
        assertTrue("Published variant is not available live after publication", document.isAvailable("live"));
        assertFalse("Published variant is available in preview after publication", document.isAvailable("preview"));
    }

    @Test
    public void rejectedRequestDoesNotPublishDocument() throws RepositoryException, RemoteException, WorkflowException {
        Node node;

        // steps taken by an author
        node = getNode("test/myarticle/myarticle[@hippostd:state='unpublished']");
        BasicReviewedActionsWorkflow reviewedWorkflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
        assertNotNull("No applicable workflow where there should be one", reviewedWorkflow);
        reviewedWorkflow.requestPublication();

        // These steps would be taken by editor:
        node = getNode("test/myarticle/hippo:request[@hippostdpubwf:type='publish']");
        FullRequestWorkflow frw = (FullRequestWorkflow) getWorkflow(node, "default");
        frw.rejectRequest("rejected");

        node = getNode("test/myarticle/myarticle[@hippostd:state='published']");
        if (node != null) {
            PublishableDocument document = new PublishableDocument(node);
            assertFalse("Published variant is available live after publication", document.isAvailable("live"));
            assertFalse("Published variant is available in preview after publication", document.isAvailable("preview"));
        }
    }

    @Test
    public void cancelledRequestIsRemoved() throws RepositoryException, RemoteException, WorkflowException {
        Node node;

        // steps taken by an author
        node = getNode("test/myarticle/myarticle[@hippostd:state='unpublished']");
        BasicReviewedActionsWorkflow reviewedWorkflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
        assertNotNull("No applicable workflow where there should be one", reviewedWorkflow);
        reviewedWorkflow.requestPublication();

        // These steps would be taken by editor:
        node = getNode("test/myarticle/hippo:request[@hippostdpubwf:type='publish']");
        BasicRequestWorkflow requestWorkflow = (BasicRequestWorkflow) getWorkflow(node, "default");
        requestWorkflow.cancelRequest();

        node = getNode("test/myarticle/hippo:request[@hippostdpubwf:type='publish']");
        assertNull("Request was not removed after cancellation", node);
    }

    /**
     * https://issues.onehippo.com/browse/CMS7-688
     * When the request for publication is removed, it should be possible the request a deletion for a published
     * document. Currently, this sometimes works, and sometimes not
     */
    @Test
    public void canRequestDeletionAfterEarlierRequestIsCancelled() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        // These steps would be taken by author
        {
            Node node = getNode("test/myarticle/myarticle[@hippostd:state='unpublished']");
            BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            workflow.requestDeletion();
        }

        {
            Node node = getNode("test/myarticle/hippo:request[@hippostdpubwf:type='delete']");
            FullRequestWorkflow requestWorkflow = (FullRequestWorkflow) getWorkflow(node, "default");
            requestWorkflow.cancelRequest();

            // now it should be possible
            node = getNode("test/myarticle/myarticle[@hippostd:state='unpublished']");
            BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            try {
                workflow.requestDeletion();
            } catch (WorkflowException e) {
                fail("Unable to request deletion after an earlier request was cancelled");
            }
        }
    }

    /**
     * https://issues.onehippo.com/browse/CMS7-2318
     * Reviewed actions workflow object is invalid after disposing the editable instance
     */
    @Test
    public void workflowObjectIsValidAfterEditableInstanceIsDisposed() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        {
            // preparation: create draft
            Node node = getNode("test/myarticle/myarticle[@hippostd:state='unpublished']");
            BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            assertNotNull("No applicable workflow where there should be one", workflow);
            workflow.obtainEditableInstance();

            // "save" as commit/edit combo
            node = getNode("test/myarticle/myarticle[@hippostd:state='draft']");
            workflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            workflow.commitEditableInstance();
            node = getNode("test/myarticle/myarticle");
            workflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            workflow.obtainEditableInstance();

            // "revert" as dispose/edit combo
            node = getNode("test/myarticle/myarticle[@hippostd:state='draft']");
            workflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            workflow.disposeEditableInstance();

            node = getNode("test/myarticle/myarticle");
            workflow = (BasicReviewedActionsWorkflow) getWorkflow(node, "default");
            workflow.obtainEditableInstance();

            // cleanup
            node = getNode("test/myarticle/myarticle");
            workflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
            workflow.disposeEditableInstance();
        }
    }

    @Test
    public void testEditing() throws WorkflowException, MappingException, RepositoryException, RemoteException {
        Document document;
        Node node, root = session.getRootNode();
        {
            assertTrue(session.getRootNode().hasNode("test"));
            assertTrue(session.getRootNode().getNode("test").hasNode("myarticle"));
            assertTrue(session.getRootNode().getNode("test").getNode("myarticle").hasNode("myarticle"));
            assertTrue(session.getRootNode().getNode("test/myarticle").hasNode("myarticle"));
            assertTrue(session.getRootNode().getNode("test/myarticle").hasNode("myarticle"));
            node = getNode("test/myarticle/myarticle");
            node.setProperty("hippostd:state", "unpublished");
            node.save();

            FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
            workflow.publish();

            node = getNode("test/myarticle/myarticle");
            workflow = (FullReviewedActionsWorkflow) getWorkflow(node, "default");
            document = workflow.obtainEditableInstance();
            assertNotNull(document);
            node = session.getNodeByUUID(document.getIdentity());
            assertNotNull(node);
            assertEquals("draft", node.getProperty("hippostd:state").getString());

            workflow = (FullReviewedActionsWorkflow) getWorkflow(document, "default");
            document = workflow.disposeEditableInstance();
            assertNotNull(document);
            node = session.getNodeByUUID(document.getIdentity());
            assertNotNull(node);
            assertEquals("unpublished", node.getProperty("hippostd:state").getString());

            workflow = (FullReviewedActionsWorkflow) getWorkflow(document, "default");
            document = workflow.obtainEditableInstance();
            assertNotNull(document);
            node = session.getNodeByUUID(document.getIdentity());
            assertNotNull(node);
            assertEquals("draft", node.getProperty("hippostd:state").getString());

            workflow = (FullReviewedActionsWorkflow) getWorkflow(document, "default");
            document = workflow.commitEditableInstance();
            assertNotNull(document);
            node = session.getNodeByUUID(document.getIdentity());
            assertNotNull(node);
            assertEquals("unpublished", node.getProperty("hippostd:state").getString());

            session.refresh(true);
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
