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

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.standardworkflow.DocumentVariant;
import org.hippoecm.repository.util.Utilities;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_ID;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DocumentWorkflowIsModifiedTest  extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void checkModified_hints() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        assertTrue(workflow.hints().containsKey("checkModified"));
        assertFalse("there is no draft hence check modified should be disabled",
                (Boolean)workflow.hints().get("checkModified"));

        workflow.obtainEditableInstance();

        workflow.commitEditableInstance();

        assertTrue((Boolean)workflow.hints().get("checkModified"));

        // non-existing branche
        assertTrue(workflow.hints("foo").containsKey("checkModified"));
        assertFalse((Boolean)workflow.hints("foo").get("checkModified"));

        workflow.branch("foo", "Foo");

        assertTrue(workflow.hints("foo").containsKey("checkModified"));
        assertTrue((Boolean)workflow.hints("foo").get("checkModified"));
    }

    @Test
    public void isModified_is_with_respect_to_correct_branch() throws Exception {

        // since these tests involve comparison on child nodes as well within version history, add some children to
        // the only variant below the handle

        final Node variant = handle.getNode(handle.getName());
        final Node content1 = variant.addNode("content", HippoStdNodeType.NT_HTML);
        content1.setProperty(HippoStdNodeType.HIPPOSTD_CONTENT, "content field 1");
        final Node content2 = variant.addNode("content", HippoStdNodeType.NT_HTML);
        content2.setProperty(HippoStdNodeType.HIPPOSTD_CONTENT, "content field 2");

        session.save();

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);

        workflow.obtainEditableInstance();
        final Node draft = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.DRAFT).get();

        draft.setProperty("title", "title master");
        session.save();

        assertTrue(workflow.isModified());

        workflow.commitEditableInstance();

        assertFalse(workflow.isModified());

        workflow.branch("foo", "Foo");
        workflow.obtainEditableInstance("foo");

        draft.setProperty("title", "title foo");

        session.save();

        assertTrue(workflow.isModified());

        workflow.commitEditableInstance();

        // obtain editable instance for master
        workflow.obtainEditableInstance();

        assertFalse(workflow.isModified());

        // if we now publish branch 'foo', the unpublished gets replaced by 'foo'. As a result, workflow.isModified()
        // should still return false, since the draft of master did not change compared to the unpublished version
        // of master
        workflow.publishBranch("foo");

        assertFalse("draft master should be compared with unpublished frozen node of master and be the same",
                workflow.isModified());

        workflow.commitEditableInstance();

        workflow.obtainEditableInstance("foo");

        // publish master with as a result, unpublished variant becomes for 'master'. Draft is still for 'foo'
        workflow.publish();

        assertFalse("draft 'foo' should be compared with unpublished frozen node of 'foo' and be the same",
                workflow.isModified());
    }


}
