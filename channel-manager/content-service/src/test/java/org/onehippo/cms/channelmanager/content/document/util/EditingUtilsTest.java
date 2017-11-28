/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;

public class EditingUtilsTest {

    @Test
    public void createDraft() throws Exception {
        final Node draft = createMock(Node.class);
        final Session session = createMock(Session.class);
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Document document = createMock(Document.class);

        expect(workflow.obtainEditableInstance()).andReturn(document);
        expect(document.getNode(session)).andReturn(draft);
        replay(workflow, document);

        final Optional<Node> draftOptional = EditingUtils.createDraft(workflow, session);
        assertThat("There should be a draft", draftOptional.isPresent());
        if (draftOptional.isPresent()) {
            assertThat(draftOptional.get(), equalTo(draft));
        }

        verify(workflow, document);
    }

    @Test
    public void createDraftWithWorkflowException() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Session session = createMock(Session.class);

        expect(workflow.obtainEditableInstance()).andThrow(new WorkflowException("bla"));
        expect(session.getUserID()).andReturn("bla");
        replay(workflow, session);

        assertFalse(EditingUtils.createDraft(workflow, session).isPresent());

        verify(workflow, session);
    }

    @Test
    public void copyToPreviewAndKeepEditingWithException() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Session session = createMock(Session.class);

        expect(workflow.commitEditableInstance()).andThrow(new WorkflowException("bla"));
        expect(session.getUserID()).andReturn("bla");
        replay(workflow, session);

        assertFalse(EditingUtils.copyToPreviewAndKeepEditing(workflow, session).isPresent());

        verify(workflow, session);
    }

    @Test
    public void copyToPreviewAndKeepEditing() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);
        final Session session = createMock(Session.class);
        final Document document = createMock(Document.class);
        final Node draft = createMock(Node.class);

        expect(workflow.commitEditableInstance()).andReturn(null);
        expect(workflow.obtainEditableInstance()).andReturn(document);
        expect(document.getNode(session)).andReturn(draft);
        replay(workflow, document);

        final Optional<Node> draftOptional = EditingUtils.copyToPreviewAndKeepEditing(workflow, session);
        assertThat("Draft should be present", draftOptional.isPresent());
        if (draftOptional.isPresent()) {
            assertThat(draftOptional.get(), equalTo(draft));
        }

        verify(workflow, document);
    }
}
