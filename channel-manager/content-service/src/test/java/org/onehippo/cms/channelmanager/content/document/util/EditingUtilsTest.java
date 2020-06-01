/*
 * Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms.channelmanager.content.document.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Session;

import org.easymock.EasyMockRunner;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hippoecm.repository.api.DocumentWorkflowAction.commitEditableInstance;
import static org.hippoecm.repository.api.DocumentWorkflowAction.delete;
import static org.hippoecm.repository.api.DocumentWorkflowAction.disposeEditableInstance;
import static org.hippoecm.repository.api.DocumentWorkflowAction.obtainEditableInstance;
import static org.hippoecm.repository.api.DocumentWorkflowAction.rename;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;

@RunWith(EasyMockRunner.class)
public class EditingUtilsTest {

    final Map<String, Serializable> hints = new HashMap<>();

    @Test
    public void canObtainEditableDocument() throws Exception {

        assertFalse(EditingUtils.canObtainEditableDocument(hints));

        hints.put(obtainEditableInstance().getAction(), null);
        assertFalse(EditingUtils.canObtainEditableDocument(hints));

        hints.put(obtainEditableInstance().getAction(), true);
        assertTrue(EditingUtils.canObtainEditableDocument(hints));
    }

    @Test
    public void canUpdateDocument() throws Exception {

        assertFalse(EditingUtils.canUpdateDocument(hints));

        hints.put(commitEditableInstance().getAction(), null);
        assertFalse(EditingUtils.canUpdateDocument(hints));

        hints.put(commitEditableInstance().getAction(), true);
        assertTrue(EditingUtils.canUpdateDocument(hints));
    }

    @Test
    public void canUpdateDocumentWithException() throws Exception {

        hints.put(commitEditableInstance().getAction(), "FOO");
        assertFalse(EditingUtils.canUpdateDocument(hints));
    }

    @Test
    public void canDisposeEditableDocument() throws Exception {

        assertFalse(EditingUtils.canDisposeEditableDocument(hints));

        hints.put(disposeEditableInstance().getAction(), false);
        assertFalse(EditingUtils.canDisposeEditableDocument(hints));

        hints.put(disposeEditableInstance().getAction(), true);
        assertTrue(EditingUtils.canDisposeEditableDocument(hints));

    }

    @Test
    public void canArchiveDocument() throws Exception {

        assertFalse(EditingUtils.canArchiveDocument(hints));

        hints.put(delete().getAction(), Boolean.FALSE);
        assertFalse(EditingUtils.canArchiveDocument(hints));

        hints.put(delete().getAction(), Boolean.TRUE);
        assertTrue(EditingUtils.canArchiveDocument(hints));
    }

    @Test
    public void canEraseDocument() throws Exception {

        final FolderWorkflow workflow = createMock(FolderWorkflow.class);
        final Map<String, Serializable> hints = new HashMap<>();

        expect(workflow.hints()).andReturn(hints).anyTimes();
        replay(workflow);

        assertFalse(EditingUtils.canEraseDocument(workflow));

        hints.put("delete", Boolean.FALSE);
        assertFalse(EditingUtils.canEraseDocument(workflow));

        hints.put("delete", Boolean.TRUE);
        assertTrue(EditingUtils.canEraseDocument(workflow));
    }

    @Test
    public void canRenameDocument() throws Exception {

        assertFalse(EditingUtils.canRenameDocument(hints));

        hints.put(rename().getAction(), Boolean.FALSE);
        assertFalse(EditingUtils.canRenameDocument(hints));

        hints.put(rename().getAction(), Boolean.TRUE);
        assertTrue(EditingUtils.canRenameDocument(hints));
    }

    @Test
    public void isActionAvailable() throws Exception {

        assertFalse(EditingUtils.isActionAvailable("action", hints));

        hints.put("action", Boolean.FALSE);
        assertFalse(EditingUtils.isActionAvailable("action", hints));
        hints.put("action", Boolean.TRUE);
        assertTrue(EditingUtils.isActionAvailable("action", hints));
        hints.put("action", Boolean.FALSE);
        assertFalse(EditingUtils.isActionAvailable("action", hints));
    }

    @Test
    public void isRequestActionAvailable() throws Exception {

        assertFalse(EditingUtils.isRequestActionAvailable("action", "uuid", hints));

        HashMap requests = new HashMap();
        hints.put("requests", requests);

        assertFalse(EditingUtils.isRequestActionAvailable("action", "uuid", hints));

        HashMap request = new HashMap();
        requests.put("uuid", request);
        assertFalse(EditingUtils.isRequestActionAvailable("action", "uuid", hints));

        request.put("action", Boolean.FALSE);
        assertFalse(EditingUtils.isRequestActionAvailable("action", "uuid", hints));

        request.put("action", Boolean.TRUE);
        assertTrue(EditingUtils.isRequestActionAvailable("action", "uuid", hints));

        request.put("action", "aap");
        assertFalse(EditingUtils.isRequestActionAvailable("action", "uuid", hints));
    }

    @Test
    public void isHintActionAvailable() {
        assertFalse(EditingUtils.isHintActionTrue(hints, "action"));

        hints.put("action", Boolean.FALSE);
        assertFalse(EditingUtils.isHintActionTrue(hints, "action"));

        hints.put("action", Boolean.TRUE);
        assertTrue(EditingUtils.isHintActionTrue(hints, "action"));

        hints.put("action", "no-boolean");
        assertFalse(EditingUtils.isHintActionTrue(hints, "action"));
    }

    @Test
    public void hasPreview() throws Exception {

        assertFalse(EditingUtils.hasPreview(hints));

        hints.put("previewAvailable", Boolean.FALSE);
        assertFalse(EditingUtils.hasPreview(hints));

        hints.put("previewAvailable", Boolean.TRUE);
        assertTrue(EditingUtils.hasPreview(hints));
    }

    @Test
    public void getEditableDocumentNode() throws Exception {
        final Node draftNode = createMock(Node.class);
        final Session session = createMock(Session.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Document document = createMock(Document.class);

        expect(workflow.obtainEditableInstance(MASTER_BRANCH_ID)).andReturn(document);
        expect(document.getNode(session)).andReturn(draftNode);
        replay(workflow, document);

        final Optional<Node> nodeOptional = EditingUtils.getEditableDocumentNode(workflow, MASTER_BRANCH_ID, session);
        assertThat("There should be a draft", nodeOptional.isPresent());
        if (nodeOptional.isPresent()) {
            assertThat(nodeOptional.get(), equalTo(draftNode));
        }

        verify(workflow, document);
    }

    @Test
    public void getEditableDocumentNodeWithWorkflowException() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Session session = createMock(Session.class);

        expect(workflow.obtainEditableInstance(MASTER_BRANCH_ID)).andThrow(new WorkflowException("bla"));
        expect(session.getUserID()).andReturn("bla");
        replay(workflow, session);

        assertFalse(EditingUtils.getEditableDocumentNode(workflow, MASTER_BRANCH_ID, session).isPresent());

        verify(workflow, session);
    }
}
