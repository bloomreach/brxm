/*
 * Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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

import org.hamcrest.CoreMatchers;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.standardworkflow.EditableWorkflow;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.util.DocumentUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason;
import org.onehippo.cms.channelmanager.content.error.MethodNotAllowed;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.mockStaticPartial;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest({DocumentUtils.class, WorkflowUtils.class})
public class ContentWorkflowUtilsTest {

    private Node handle;
    private Node folder;

    @Before
    public void setUp() {
        mockStatic(DocumentUtils.class);
        mockStaticPartial(ErrorInfo.class, "withDisplayName");
        mockStatic(WorkflowUtils.class);

        handle = createMock(Node.class);
        folder = createMock(Node.class);
    }

    @Test(expected = IllegalAccessException.class)
    public void cannotCreateInstance() throws Exception {
        ContentWorkflowUtils.class.newInstance();
    }

    @Test
    public void getDefaultWorkflowSuccess() throws Exception {
        final DefaultWorkflow workflow = createMock(DefaultWorkflow.class);

        expect(WorkflowUtils.getWorkflow(eq(handle), eq("core"), eq(DefaultWorkflow.class))).andReturn(Optional.of(workflow));
        replayAll();

        assertThat(ContentWorkflowUtils.getDefaultWorkflow(handle), equalTo(workflow));
        verifyAll();
    }

    @Test
    public void getDefaultWorkflowFailure() throws Exception {
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("core"), eq(DefaultWorkflow.class))).andReturn(Optional.empty());
        expect(DocumentUtils.getDisplayName(eq(handle))).andReturn(Optional.of("Document"));
        replayAll();

        try {
            ContentWorkflowUtils.getDefaultWorkflow(handle);
        } catch (final MethodNotAllowed e) {
            final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), CoreMatchers.equalTo(Reason.NOT_A_DOCUMENT));
        }

        verifyAll();
    }

    @Test
    public void getDocumentWorkflowSuccess() throws Exception {
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);

        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.of(workflow));
        replayAll();

        assertThat(ContentWorkflowUtils.getDocumentWorkflow(handle), equalTo(workflow));
        verifyAll();
    }

    @Test
    public void getDocumentWorkflowFailure() throws Exception {
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("default"), eq(DocumentWorkflow.class))).andReturn(Optional.empty());
        expect(DocumentUtils.getDisplayName(eq(handle))).andReturn(Optional.of("Document"));
        replayAll();

        try {
            ContentWorkflowUtils.getDocumentWorkflow(handle);
        } catch (final MethodNotAllowed e) {
            final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), CoreMatchers.equalTo(Reason.NOT_A_DOCUMENT));
        }

        verifyAll();
    }

    @Test
    public void getFolderWorkflowSuccess() throws Exception {
        final FolderWorkflow workflow = createMock(FolderWorkflow.class);

        expect(WorkflowUtils.getWorkflow(eq(folder), eq("internal"), eq(FolderWorkflow.class))).andReturn(Optional.of(workflow));
        replayAll();

        assertThat(ContentWorkflowUtils.getFolderWorkflow(folder), equalTo(workflow));
        verifyAll();
    }

    @Test
    public void getFolderWorkflowFailure() throws Exception {
        expect(WorkflowUtils.getWorkflow(eq(folder), eq("internal"), eq(FolderWorkflow.class))).andReturn(Optional.empty());
        expect(DocumentUtils.getDisplayName(eq(folder))).andReturn(Optional.of("Folder"));
        replayAll();

        try {
            ContentWorkflowUtils.getFolderWorkflow(folder);
        } catch (final MethodNotAllowed e) {
            final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), CoreMatchers.equalTo(Reason.NOT_A_FOLDER));
        }

        verifyAll();
    }

    @Test
    public void getEditableWorkflowSuccess() throws Exception {
        final EditableWorkflow workflow = createMock(EditableWorkflow.class);

        expect(WorkflowUtils.getWorkflow(eq(handle), eq("editing"), eq(EditableWorkflow.class))).andReturn(Optional.of(workflow));
        replayAll();

        assertThat(ContentWorkflowUtils.getEditableWorkflow(handle), equalTo(workflow));
        verifyAll();
    }

    @Test
    public void getEditableWorkflowFailure() throws Exception {
        expect(WorkflowUtils.getWorkflow(eq(handle), eq("editing"), eq(EditableWorkflow.class))).andReturn(Optional.empty());
        expect(DocumentUtils.getDisplayName(eq(handle))).andReturn(Optional.of("Document"));
        replayAll();

        try {
            ContentWorkflowUtils.getEditableWorkflow(handle);
        } catch (final MethodNotAllowed e) {
            final ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), CoreMatchers.equalTo(Reason.NOT_A_DOCUMENT));
        }

        verifyAll();
    }
}