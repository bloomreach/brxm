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

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.easymock.Mock;
import org.easymock.MockType;
import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecService;
import org.hippoecm.repository.api.StringCodecService.Encoding;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.util.DocumentUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.error.ForbiddenException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.mock.MockNode;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertNull;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest({ContentWorkflowUtils.class, DocumentUtils.class, EditingUtils.class, HippoServiceRegistry.class})
public class DocumentNameUtilsTest {

    @Mock(type = MockType.NICE)
    private Map<String, Serializable> hints;

    @Before
    public void setUp() {
        mockStatic(ContentWorkflowUtils.class);
        mockStatic(EditingUtils.class);
        mockStatic(HippoServiceRegistry.class);
    }

    @Test(expected = IllegalAccessException.class)
    public void cannotCreateInstance() throws Exception {
        DocumentNameUtils.class.newInstance();
    }

    @Test
    public void encodeUrlNameWithLocale() {
        final StringCodecService service = createMock(StringCodecService.class);
        final StringCodec codec = createMock(StringCodec.class);

        expect(HippoServiceRegistry.getService(eq(StringCodecService.class))).andReturn(service);
        expect(service.getStringCodec(eq(Encoding.NODE_NAME), eq("en"))).andReturn(codec);
        expect(codec.encode("Some Name")).andReturn("some-name");
        replayAll();

        final String encoded = DocumentNameUtils.encodeUrlName("Some Name", "en");

        assertThat(encoded, equalTo("some-name"));
        verifyAll();
    }

    @Test
    public void encodeUrlNameWithoutLocale() {
        final StringCodecService service = createMock(StringCodecService.class);
        final StringCodec codec = createMock(StringCodec.class);

        expect(HippoServiceRegistry.getService(eq(StringCodecService.class))).andReturn(service);
        expect(service.getStringCodec(eq(Encoding.NODE_NAME), eq(null))).andReturn(codec);
        expect(codec.encode("Some Name")).andReturn("some-name");
        replayAll();

        final String encoded = DocumentNameUtils.encodeUrlName("Some Name", null);

        assertThat(encoded, equalTo("some-name"));
        verifyAll();
    }

    @Test
    public void encodeUrlNameNull() {
        final String encoded = DocumentNameUtils.encodeUrlName(null, null);
        assertNull(encoded);
    }

    @Test
    public void encodeDisplayNameWithLocale() {
        final StringCodecService service = createMock(StringCodecService.class);
        final StringCodec codec = createMock(StringCodec.class);

        expect(HippoServiceRegistry.getService(eq(StringCodecService.class))).andReturn(service);
        expect(service.getStringCodec(eq(Encoding.DISPLAY_NAME), eq("en"))).andReturn(codec);
        expect(codec.encode("Some Name")).andReturn("Some Name (encoded)");
        replayAll();

        final String encoded = DocumentNameUtils.encodeDisplayName("Some Name", "en");

        assertThat(encoded, equalTo("Some Name (encoded)"));
        verifyAll();
    }

    @Test
    public void encodeDisplayNameWithoutLocale() {
        final StringCodecService service = createMock(StringCodecService.class);
        final StringCodec codec = createMock(StringCodec.class);

        expect(HippoServiceRegistry.getService(eq(StringCodecService.class))).andReturn(service);
        expect(service.getStringCodec(eq(Encoding.DISPLAY_NAME), eq(null))).andReturn(codec);
        expect(codec.encode("Some Name")).andReturn("Some Name (encoded)");
        replayAll();

        final String encoded = DocumentNameUtils.encodeDisplayName("Some Name", null);

        assertThat(encoded, equalTo("Some Name (encoded)"));
        verifyAll();
    }

    @Test
    public void getUrlName() throws Exception {
        final Node handle = createHandle("test", "Test");
        assertThat(DocumentNameUtils.getUrlName(handle), equalTo("test"));
    }

    @Test(expected = InternalServerErrorException.class)
    public void getUrlNameException() throws Exception {
        final Node handle = createMock(Node.class);
        expect(handle.getName()).andThrow(new RepositoryException());
        expect(handle.getPath()).andThrow(new RepositoryException());
        replayAll();
        DocumentNameUtils.getUrlName(handle);
    }

    @Test(expected = InternalServerErrorException.class)
    public void setUrlNameOfDocumentThatAllowsRenameException() throws Exception {
        final Node handle = createHandle("test", "Test");
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);
        expect(ContentWorkflowUtils.getDocumentWorkflow(eq(handle))).andReturn(workflow);
        expect(EditingUtils.canRenameDocument(eq(hints))).andReturn(true);

        workflow.rename("New name");
        expectLastCall().andThrow(new RepositoryException());

        replayAll();

        DocumentNameUtils.setUrlName(handle, "New name", hints);
    }

    @Test
    public void setUrlNameOfDocumentThatAllowsRenameSuccess() throws Exception {
        final Node handle = createHandle("test", "Test");
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);
        expect(ContentWorkflowUtils.getDocumentWorkflow(eq(handle))).andReturn(workflow);
        expect(EditingUtils.canRenameDocument(eq(hints))).andReturn(true);

        workflow.rename("New name");
        expectLastCall();

        replayAll();

        DocumentNameUtils.setUrlName(handle, "New name", hints);

        verifyAll();
    }

    @Test(expected = ForbiddenException.class)
    public void setUrlNameOfDocumentWithPreviewVariantThatForbidsRename() throws Exception {
        final Node handle = createHandle("test", "Test");
        final DocumentWorkflow workflow = createMock(DocumentWorkflow.class);
        expect(ContentWorkflowUtils.getDocumentWorkflow(eq(handle))).andReturn(workflow);
        expect(EditingUtils.canRenameDocument(eq(hints))).andReturn(false);
        expect(EditingUtils.hasPreview(eq(hints))).andReturn(true);

        replayAll();

        DocumentNameUtils.setUrlName(handle, "New name", hints);
    }

    @Test(expected = InternalServerErrorException.class)
    public void setUrlNameOfDraftException() throws Exception {
        final Node handle = createHandle("test", "Test");
        final DocumentWorkflow documentWorkflow = createMock(DocumentWorkflow.class);
        final DefaultWorkflow defaultWorkflow = createMock(DefaultWorkflow.class);
        expect(ContentWorkflowUtils.getDocumentWorkflow(eq(handle))).andReturn(documentWorkflow);
        expect(EditingUtils.canRenameDocument(eq(hints))).andReturn(false);
        expect(EditingUtils.hasPreview(eq(hints))).andReturn(false);
        expect(ContentWorkflowUtils.getDefaultWorkflow(eq(handle))).andReturn(defaultWorkflow);

        defaultWorkflow.rename(eq("New name"));
        expectLastCall().andThrow(new RepositoryException());

        replayAll();

        DocumentNameUtils.setUrlName(handle, "New name", hints);
    }

    @Test
    public void setUrlNameOfDraftSuccess() throws Exception {
        final Node handle = createHandle("test", "Test");
        final DocumentWorkflow documentWorkflow = createMock(DocumentWorkflow.class);
        final DefaultWorkflow defaultWorkflow = createMock(DefaultWorkflow.class);
        expect(ContentWorkflowUtils.getDocumentWorkflow(eq(handle))).andReturn(documentWorkflow);
        expect(EditingUtils.canRenameDocument(eq(hints))).andReturn(false);
        expect(EditingUtils.hasPreview(eq(hints))).andReturn(false);
        expect(ContentWorkflowUtils.getDefaultWorkflow(eq(handle))).andReturn(defaultWorkflow);

        defaultWorkflow.rename(eq("New name"));
        expectLastCall();
        replayAll();

        DocumentNameUtils.setUrlName(handle, "New name", hints);

        verifyAll();
    }

    @Test(expected = InternalServerErrorException.class)
    public void getDisplayNameMissing() throws Exception {
        mockStatic(DocumentUtils.class);
        final Node handle = createMock(Node.class);
        expect(DocumentUtils.getDisplayName(eq(handle))).andReturn(Optional.empty());
        replayAll();
        DocumentNameUtils.getDisplayName(handle);
    }

    @Test
    public void getDisplayNameSuccess() throws Exception {
        final Node handle = createHandle("test", "Test");
        assertThat(DocumentNameUtils.getDisplayName(handle), equalTo("Test"));
    }

    @Test(expected = InternalServerErrorException.class)
    public void setDisplayNameException() throws Exception {
        final Node handle = createHandle("test", "Test");
        final DefaultWorkflow workflow = createMock(DefaultWorkflow.class);

        expect(ContentWorkflowUtils.getDefaultWorkflow(eq(handle))).andReturn(workflow);

        workflow.setDisplayName(eq("New name"));
        expectLastCall().andThrow(new RepositoryException());

        replayAll();

        DocumentNameUtils.setDisplayName(handle, "New name");
    }

    @Test
    public void setDisplayNameSuccess() throws Exception {
        final Node handle = createHandle("test", "Test");
        final DefaultWorkflow workflow = createMock(DefaultWorkflow.class);

        expect(ContentWorkflowUtils.getDefaultWorkflow(eq(handle))).andReturn(workflow);

        workflow.setDisplayName(eq("New name"));
        expectLastCall();

        replayAll();

        DocumentNameUtils.setDisplayName(handle, "New name");

        verifyAll();
    }

    private static MockNode createHandle(final String nodeName, final String displayName) throws Exception {
        final MockNode node = MockNode.root().addNode(nodeName, "hippo:document");
        node.setProperty("hippo:name", displayName);
        return node;
    }
}