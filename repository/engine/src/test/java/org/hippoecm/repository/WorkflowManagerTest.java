/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.hippoecm.repository.impl.WorkflowManagerImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WorkflowManagerTest extends RepositoryTestCase {

    public interface TestWorkflow extends Workflow {

        Node getNode();
    }

    public static class TestHandleWorkflow extends WorkflowImpl implements TestWorkflow {

        public TestHandleWorkflow() throws RemoteException {
        }

        public Node getNode() {
            return super.getNode();
        }
    }

    public static class TestDocumentWorkflow extends WorkflowImpl implements TestWorkflow {

        public TestDocumentWorkflow() throws RemoteException {
        }

        public Node getNode() {
            return super.getNode();
        }
    }

    @Before
    public void createWorkflowConfig() throws RepositoryException {
        build(mount("/hippo:configuration/hippo:workflows", new String[] {
                "/testworkflow", "hipposys:workflowcategory",
                    "/testworkflow/handle", "hipposys:workflow",
                        "hipposys:nodetype", "hippo:handle",
                        "hipposys:subtype", "hippo:testdocument",
                        "hipposys:classname", TestHandleWorkflow.class.getName(),
                    "/testworkflow/doc", "hipposys:workflow",
                        "hipposys:nodetype", "hippo:testdocument",
                        "hipposys:classname", TestDocumentWorkflow.class.getName()
        }), session);
        session.getRootNode().addNode("test");
        build(mount("/test", new String[]{
                "/doc", "hippo:handle",
                    "jcr:mixinTypes", "mix:referenceable",
                    "/doc/doc", "hippo:testdocument",
        }), session);
        session.save();
    }

    @After
    public void cleanupWorkflowConfig() throws RepositoryException {
        session.getNode("/hippo:configuration/hippo:workflows/testworkflow").remove();
        session.save();
    }

    @Test
    public void subtypeIsResolvedOnHandle() throws RepositoryException {
        WorkflowManagerImpl workflowManager = new WorkflowManagerImpl(session);
        final Node handle = session.getNode("/test/doc");
        final TestWorkflow workflow = (TestWorkflow) workflowManager.getWorkflow("testworkflow", handle);
        assertNotNull(workflow);
        assertTrue(handle.isSame(workflow.getNode()));
    }

    @Test
    public void invalidSubtypeIsNotResolvedOnHandle() throws RepositoryException {
        WorkflowManagerImpl workflowManager = new WorkflowManagerImpl(session);
        final Node handle = session.getNode("/test/doc");
        session.getNode("/test/doc/doc").setPrimaryType("hippo:document");
        final TestWorkflow workflow = (TestWorkflow) workflowManager.getWorkflow("testworkflow", handle);
        assertNull(workflow);
    }

    @Test
    public void typeIsResolvedOnDocument() throws RepositoryException {
        WorkflowManagerImpl workflowManager = new WorkflowManagerImpl(session);
        final Node doc = session.getNode("/test/doc/doc");
        final TestWorkflow workflow = (TestWorkflow) workflowManager.getWorkflow("testworkflow", doc);
        assertNotNull(workflow);
        assertTrue(doc.isSame(workflow.getNode()));
    }

}
