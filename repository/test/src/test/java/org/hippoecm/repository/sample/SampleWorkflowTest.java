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
package org.hippoecm.repository.sample;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class SampleWorkflowTest extends RepositoryTestCase {

    @Test
    public void testWorkflow() throws Exception {
        SampleWorkflowSetup.commonStart(server);
        try {
            Session session = server.login("admin","admin".toCharArray());

            Node root = session.getRootNode();
            Node node = root.getNode("files/myarticle");
            assertEquals(node.getProperty("sample:authorId").getLong(), SampleWorkflowSetup.oldAuthorId);
            WorkflowManager manager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
            try {
                Workflow workflow = manager.getWorkflow("mycategory", node);
                assertNotNull(workflow);
                if (workflow instanceof SampleWorkflow) {
                    SampleWorkflow myworkflow = (SampleWorkflow) workflow;
                    myworkflow.renameAuthor("Jan Smit");
                } else {
                    fail("workflow not of proper type " + workflow.getClass().getName());
                }

            } catch (Exception ex) {
                System.err.println(ex.getMessage());
                ex.printStackTrace(System.err);
                throw ex;
            }

            session.save();
            session.refresh(false);
            assertEquals(node.getProperty("sample:authorId").getLong(), SampleWorkflowSetup.newAuthorId);

            session.logout();
        } finally {
            SampleWorkflowSetup.commonEnd(server);
        }
    }
}
