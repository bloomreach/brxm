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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Value;

import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.ExecuteOnLogLevel;
import org.onehippo.repository.testutils.RepositoryTestCase;

public class FailingWorkflowTest extends RepositoryTestCase {

    private String[] content = {
        "/test", "nt:unstructured",
            "/test/folder", "hippostd:folder",
                "jcr:mixinTypes", "mix:versionable",
                "/test/folder/document", "hippo:handle",
                    "jcr:mixinTypes", "hippo:hardhandle",
                    "/test/folder/document/document", "hippostdpubwf:test",
                        "jcr:mixinTypes", "mix:versionable",
                        "hippostdpubwf:createdBy", "admin",
                        "hippostdpubwf:creationDate", "2010-02-04T16:32:28.068+02:00",
                        "hippostdpubwf:lastModifiedBy", "admin",
                        "hippostdpubwf:lastModificationDate", "2010-02-04T16:32:28.068+02:00",
                        "hippostd:holder", "admin",
                        "hippostd:state", "published"
    };

    Map<String, Value[]> privileges = new HashMap<>();

    @Before
    public void setUp() throws Exception {
        super.setUp();

        build(content, session);
        session.save();

        privileges.clear();

        for (NodeIterator iter = session.getRootNode().getNode("hippo:configuration/hippo:workflows").getNodes(); iter.hasNext(); ) {
            for (NodeIterator i = iter.nextNode().getNodes(); i.hasNext(); ) {
                Node workflowNode = i.nextNode();
                if (workflowNode.hasProperty("hipposys:privileges")) {
                    privileges.put(workflowNode.getPath(), workflowNode.getProperty("hipposys:privileges").getValues());
                    workflowNode.getProperty("hipposys:privileges").remove();
                }
            }
        }

        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        for (Map.Entry<String, Value[]> toRestore : privileges.entrySet()) {
            session.getNode(toRestore.getKey()).setProperty("hipposys:privileges", toRestore.getValue());
        }
        session.save();
        super.tearDown();
    }

    @Test
    public void testFailAfterRename() throws Exception {
        WorkflowManager manager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        Node handle = session.getRootNode().getNode("test/folder/document");
        Node document = handle.getNode(handle.getName());
        // WorkflowDescriptor workflowDescriptor = manager.getWorkflowDescriptor("default", document);
        // Workflow workflowInterface = manager.getWorkflow(workflowDescriptor);
        Workflow workflowInterface = manager.getWorkflow("default", document);
        assertTrue(workflowInterface instanceof FullReviewedActionsWorkflow);
        FullReviewedActionsWorkflow workflow = (FullReviewedActionsWorkflow) workflowInterface;
        final FullReviewedActionsWorkflow runnableWorkflow = workflow;
        try {
            ExecuteOnLogLevel.fatal(new ExecuteOnLogLevel.Executable() {
                @Override
                public void execute() throws Exception {
                    runnableWorkflow.rename("fail");
                }
            }, "org.onehippo.repository.scxml.SCXMLWorkflowExecutor");
            fail("rename should have failed");
        } catch(ExecuteOnLogLevel.ExecutableException ex) {
            if (ex.getCause() instanceof WorkflowException) {
                // expected
            }
            else {
                throw ex.getCause();
            }
        }

        document = handle.getNode(handle.getName());
        // workflowDescriptor = manager.getWorkflowDescriptor("default", document);
        // workflowInterface = manager.getWorkflow(workflowDescriptor);
        workflowInterface = manager.getWorkflow("default", document);
        assertTrue(workflowInterface instanceof FullReviewedActionsWorkflow);
        workflow = (FullReviewedActionsWorkflow) workflowInterface;

        workflow.depublish();

        document = handle.getNode(handle.getName());
        // workflowDescriptor = manager.getWorkflowDescriptor("default", document);
        // workflowInterface = manager.getWorkflow(workflowDescriptor);
        workflowInterface = manager.getWorkflow("default", document);
        assertTrue(workflowInterface instanceof FullReviewedActionsWorkflow);
        workflow = (FullReviewedActionsWorkflow) workflowInterface;
        workflow.rename("succeed");
    }
}
