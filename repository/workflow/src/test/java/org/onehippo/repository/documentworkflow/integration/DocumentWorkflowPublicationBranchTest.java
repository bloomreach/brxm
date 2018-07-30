/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.documentworkflow.integration;

import javax.jcr.Node;
import javax.jcr.version.VersionHistory;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DocumentVariant;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_ID;

public class DocumentWorkflowPublicationBranchTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void publication_master_when_there_are_branches() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        assertTrue((Boolean)workflow.hints().get("publish"));
        assertTrue((Boolean)workflow.hints().get("requestPublication"));
        assertFalse((Boolean)workflow.hints().get("requestDepublication"));
        assertFalse((Boolean)workflow.hints().get("depublish"));

        workflow.branch("foo", "Foo");
        workflow.obtainEditableInstance();

        final Node draft = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).get();
        draft.setProperty("title", "title foo");
        session.save();
        workflow.commitEditableInstance();

        final Node unpublished = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();

        assertTrue(unpublished.isNodeType(HIPPO_MIXIN_BRANCH_INFO));

        // although unpublished is now for branch, (de)publish should still be disabled
        assertTrue((Boolean)workflow.hints().get("publish"));
        // TODO REPO-2029 since there is a version, requestPublication and requestDepublication should be disabled
        // since there are branches, request(de)publication should be false, even if the unpublished is for master
        assertFalse((Boolean)workflow.hints().get("requestPublication"));
        assertFalse((Boolean)workflow.hints().get("depublish"));
        assertFalse((Boolean)workflow.hints().get("requestDepublication"));

        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            // below should publish master!
            workflow.publish();
        } catch (WorkflowException e) {
            fail(e.toString());
        }

        // result of the publish above should be that MASTER is published
        assertFalse(unpublished.isNodeType(HIPPO_MIXIN_BRANCH_INFO));

        // the branch foo should as a result be checked in
        final VersionHistory versionHistory = session.getWorkspace().getVersionManager().getVersionHistory(unpublished.getPath());
        assertTrue(versionHistory.hasVersionLabel("foo-unpublished"));
        assertEquals("title foo", JcrUtils.getStringProperty(versionHistory.getVersionByLabel("foo-unpublished").getFrozenNode(),"title", null));

        assertFalse((Boolean)workflow.hints().get("publish"));
        assertFalse((Boolean)workflow.hints().get("requestPublication"));
        assertTrue((Boolean)workflow.hints().get("requestDepublication"));
        assertTrue((Boolean)workflow.hints().get("depublish"));

        workflow.checkoutBranch("foo");

        assertFalse((Boolean)workflow.hints().get("publish"));
        assertFalse((Boolean)workflow.hints().get("requestPublication"));
        assertFalse((Boolean)workflow.hints().get("depublish"));
        assertFalse((Boolean)workflow.hints().get("requestDepublication"));

        workflow.checkoutBranch(MASTER_BRANCH_ID);

        // delete the unpublished manually
        unpublished.remove();
        session.save();

        assertFalse((Boolean)workflow.hints().get("publish"));
        assertFalse((Boolean)workflow.hints().get("requestPublication"));
        assertTrue((Boolean)workflow.hints().get("depublish"));
        assertTrue((Boolean)workflow.hints().get("requestDepublication"));

    }

}
