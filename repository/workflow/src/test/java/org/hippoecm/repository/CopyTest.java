/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

public class CopyTest extends RepositoryTestCase {

    private String[] content = {
            "/test/destination", "hippostd:folder",
            "jcr:mixinTypes", "mix:versionable",
            "/test/folder", "hippostd:folder",
            "jcr:mixinTypes", "mix:versionable",
            "/test/folder/document", "hippo:handle",
            "jcr:mixinTypes", "hippo:hardhandle"
    };

    protected WorkflowManager workflowManager;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        Node root = session.getRootNode();
        while(root.hasNode("test")) {
            root.getNode("test").remove();
            root.save();
            root.refresh(false);
        }
        root.addNode("test");
        build(content, session);
        session.save();

        workflowManager = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
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
    public void testDuplicatePublished() throws Exception {
        testPublishableDocument(true,  false, true,  false);
    }
    @Test
    public void testDuplicateUnpublished() throws Exception {
        testPublishableDocument(true,  true,  false, false);
    }
    @Test
    public void testDuplicateModifiedUnpublished() throws Exception {
        testPublishableDocument(true,  true,  true,  false);
    }
    @Test
    public void testDuplicateModifiedPublished() throws Exception {
        testPublishableDocument(true,  true,  true,  true);
    }
    @Test
    public void testCopyPublished() throws Exception {
        testPublishableDocument(false, false, true,  false);
    }
    @Test
    public void testCopyUnpublished() throws Exception {
        testPublishableDocument(false, true,  false, false);
    }
    @Test
    public void testCopyModifiedUnpublished() throws Exception {
        testPublishableDocument(false, true,  true,  false);
    }
    @Test
    public void testCopyModifiedPublished() throws Exception {
        testPublishableDocument(false, true,  true,  true);
    }

    private void testPublishableDocument(boolean duplicate, boolean unpublished, boolean published, boolean secondDocument) throws WorkflowException, RepositoryException, RemoteException {
        Node document, handle = session.getRootNode().getNode("test/folder/document");
        if (published) {
            document = handle.addNode("document", "hippo:document");
            document.addMixin("hippostdpubwf:document");
            document.addMixin("mix:versionable");
            document.setProperty("hippostd:holder", "admin");
            document.setProperty("hippostd:state", "published");
            document.setProperty("hippo:availability", (unpublished ? new String[] {"live", "preview"} : new String[] {"live"}));
            document.setProperty("hippostdpubwf:createdBy", "admin");
            document.setProperty("hippostdpubwf:creationDate", "2010-02-04T16:32:28.068+02:00");
            document.setProperty("hippostdpubwf:lastModifiedBy", "admin");
            document.setProperty("hippostdpubwf:lastModificationDate", "2010-02-04T16:32:28.068+02:00");
        }
        if (unpublished) {
            document = handle.addNode("document", "hippo:document");
            document.addMixin("hippostdpubwf:document");
            document.addMixin("mix:versionable");
            document.setProperty("hippostd:holder", "admin");
            document.setProperty("hippostd:state", "unpublished");
            document.setProperty("hippo:availability", new String[] {"preview"});
            document.setProperty("hippostdpubwf:createdBy", "admin");
            document.setProperty("hippostdpubwf:creationDate", "2010-02-04T16:32:28.068+02:00");
            document.setProperty("hippostdpubwf:lastModifiedBy", "admin");
            document.setProperty("hippostdpubwf:lastModificationDate", "2010-02-04T16:32:28.068+02:00");
        }
        session.save();

        Node destination = (duplicate ? session.getRootNode().getNode("test/folder") : session.getRootNode().getNode("test/destination"));
        Node folder = session.getRootNode().getNode("test/folder");
        document = (secondDocument ? folder.getNode("document/document[2]") : folder.getNode("document/document"));
        FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) workflowManager.getWorkflow("default", document);
        workflow.copy(new Document(destination), "copy");
    }

    @Test
    public void testDuplicateAsset() throws Exception {
        Node document = session.getRootNode().getNode("test/folder/document").addNode("document", "hippo:document");
        document.addMixin("mix:versionable");
        document.setProperty("hippo:availability", new String[] {"preview", "live"});
        session.save();

        Node destination = session.getRootNode().getNode("test/folder");
        DefaultWorkflow workflow = (DefaultWorkflow) workflowManager.getWorkflow("default", document);
        workflow.copy(new Document(destination), "copy");
    }

    @Test
    public void testCopyAsset() throws Exception {
        Node document = session.getRootNode().getNode("test/folder/document").addNode("document", "hippo:document");
        document.addMixin("mix:versionable");
        document.setProperty("hippo:availability", new String[] {"preview", "live"});
        session.save();

        Node destination = session.getRootNode().getNode("test/destination");
        DefaultWorkflow workflow = (DefaultWorkflow) workflowManager.getWorkflow("default", document);
        workflow.copy(new Document(destination), "copy");
    }
}
