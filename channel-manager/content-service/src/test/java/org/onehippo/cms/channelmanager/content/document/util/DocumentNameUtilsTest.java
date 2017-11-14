/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.StringCodec;
import org.hippoecm.repository.api.StringCodecService;
import org.hippoecm.repository.api.StringCodecService.Encoding;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms7.services.HippoServiceRegistry;
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
import static org.powermock.api.easymock.PowerMock.mockStaticPartial;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({HippoServiceRegistry.class, JcrUtils.class, WorkflowUtils.class})
public class DocumentNameUtilsTest {

    @Before
    public void setUp() {
        mockStatic(HippoServiceRegistry.class);
        mockStatic(WorkflowUtils.class);
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
    public void setIdenticalNames() throws Exception {
        final Node node = createHandle("some-name", "Some Name (encoded)");
        final StringCodecService service = createMock(StringCodecService.class);
        final StringCodec displayNameCodec = createMock(StringCodec.class);
        final StringCodec urlNameCodec = createMock(StringCodec.class);

        expect(HippoServiceRegistry.getService(eq(StringCodecService.class))).andReturn(service).anyTimes();
        expect(service.getStringCodec(eq(Encoding.NODE_NAME), eq("en"))).andReturn(urlNameCodec);
        expect(service.getStringCodec(eq(Encoding.DISPLAY_NAME), eq("en"))).andReturn(displayNameCodec);
        expect(urlNameCodec.encode(eq("some name"))).andReturn("some-name");
        expect(displayNameCodec.encode(eq("Some Name"))).andReturn("Some Name (encoded)");

        replayAll();

        DocumentNameUtils.setNames(node, "some name", "Some Name", "en");

        verifyAll();
    }

    @Test
    public void setNamesNoWorkflow() throws Exception {
        final Node node = createHandle("old-name", "Old Name (encoded)");
        final StringCodecService service = createMock(StringCodecService.class);
        final StringCodec displayNameCodec = createMock(StringCodec.class);
        final StringCodec urlNameCodec = createMock(StringCodec.class);

        expect(HippoServiceRegistry.getService(eq(StringCodecService.class))).andReturn(service).anyTimes();
        expect(service.getStringCodec(eq(Encoding.NODE_NAME), eq("en"))).andReturn(urlNameCodec);
        expect(service.getStringCodec(eq(Encoding.DISPLAY_NAME), eq("en"))).andReturn(displayNameCodec);
        expect(urlNameCodec.encode(eq("new name"))).andReturn("new-name");
        expect(displayNameCodec.encode(eq("New Name"))).andReturn("New Name (encoded)");

        expect(WorkflowUtils.getWorkflow(eq(node), eq("core"), eq(DefaultWorkflow.class))).andReturn(Optional.empty());

        replayAll();

        DocumentNameUtils.setNames(node, "new name", "New Name", "en");

        verifyAll();
    }

    @Test
    public void setNamesWorkflowRenameFails() throws Exception {
        final Node node = createHandle("old-name", "Old Name (encoded)");
        final StringCodecService service = createMock(StringCodecService.class);
        final StringCodec displayNameCodec = createMock(StringCodec.class);
        final StringCodec urlNameCodec = createMock(StringCodec.class);
        final DefaultWorkflow workflow = createMock(DefaultWorkflow.class);

        expect(HippoServiceRegistry.getService(eq(StringCodecService.class))).andReturn(service).anyTimes();
        expect(service.getStringCodec(eq(Encoding.NODE_NAME), eq("en"))).andReturn(urlNameCodec);
        expect(service.getStringCodec(eq(Encoding.DISPLAY_NAME), eq("en"))).andReturn(displayNameCodec);
        expect(urlNameCodec.encode(eq("new name"))).andReturn("new-name");
        expect(displayNameCodec.encode(eq("New Name"))).andReturn("New Name (encoded)");

        expect(WorkflowUtils.getWorkflow(eq(node), eq("core"), eq(DefaultWorkflow.class))).andReturn(Optional.of(workflow));

        workflow.rename("new-name");
        expectLastCall().andThrow(new WorkflowException("meh"));

        workflow.setDisplayName("New Name (encoded)");
        expectLastCall();

        replayAll();

        DocumentNameUtils.setNames(node, "new name", "New Name", "en");

        verifyAll();
    }

    @Test
    public void setNamesBoth() throws Exception {
        final Node node = createHandle("old-name", "Old Name (encoded)");
        final StringCodecService service = createMock(StringCodecService.class);
        final StringCodec displayNameCodec = createMock(StringCodec.class);
        final StringCodec urlNameCodec = createMock(StringCodec.class);
        final DefaultWorkflow workflow = createMock(DefaultWorkflow.class);

        expect(HippoServiceRegistry.getService(eq(StringCodecService.class))).andReturn(service).anyTimes();
        expect(service.getStringCodec(eq(Encoding.NODE_NAME), eq("en"))).andReturn(urlNameCodec);
        expect(service.getStringCodec(eq(Encoding.DISPLAY_NAME), eq("en"))).andReturn(displayNameCodec);
        expect(urlNameCodec.encode(eq("new name"))).andReturn("new-name");
        expect(displayNameCodec.encode(eq("New Name"))).andReturn("New Name (encoded)");

        expect(WorkflowUtils.getWorkflow(eq(node), eq("core"), eq(DefaultWorkflow.class))).andReturn(Optional.of(workflow));

        workflow.rename("new-name");
        expectLastCall();

        workflow.setDisplayName("New Name (encoded)");
        expectLastCall();

        replayAll();

        DocumentNameUtils.setNames(node, "new name", "New Name", "en");

        verifyAll();
    }

    @Test
    public void setNamesBothAndUrlNameReadIsNull() throws Exception {
        final Node node = createHandle("old-name", "Old Name (encoded)");
        final StringCodecService service = createMock(StringCodecService.class);
        final StringCodec displayNameCodec = createMock(StringCodec.class);
        final StringCodec urlNameCodec = createMock(StringCodec.class);
        final DefaultWorkflow workflow = createMock(DefaultWorkflow.class);
        mockStaticPartial(JcrUtils.class, "getNodeNameQuietly");

        expect(JcrUtils.getNodeNameQuietly(node)).andReturn(null);

        expect(HippoServiceRegistry.getService(eq(StringCodecService.class))).andReturn(service).anyTimes();
        expect(service.getStringCodec(eq(Encoding.NODE_NAME), eq("en"))).andReturn(urlNameCodec);
        expect(service.getStringCodec(eq(Encoding.DISPLAY_NAME), eq("en"))).andReturn(displayNameCodec);
        expect(urlNameCodec.encode(eq("new name"))).andReturn("new-name");
        expect(displayNameCodec.encode(eq("New Name"))).andReturn("New Name (encoded)");

        expect(WorkflowUtils.getWorkflow(eq(node), eq("core"), eq(DefaultWorkflow.class))).andReturn(Optional.of(workflow));

        workflow.rename("new-name");
        expectLastCall();

        workflow.setDisplayName("New Name (encoded)");
        expectLastCall();

        replayAll();

        DocumentNameUtils.setNames(node, "new name", "New Name", "en");

        verifyAll();
    }

    @Test
    public void setNamesOnlyUrlName() throws Exception {
        final Node node = createHandle("old-name", "Old Name (encoded)");
        final StringCodecService service = createMock(StringCodecService.class);
        final StringCodec displayNameCodec = createMock(StringCodec.class);
        final StringCodec urlNameCodec = createMock(StringCodec.class);
        final DefaultWorkflow workflow = createMock(DefaultWorkflow.class);

        expect(HippoServiceRegistry.getService(eq(StringCodecService.class))).andReturn(service).anyTimes();
        expect(service.getStringCodec(eq(Encoding.NODE_NAME), eq("en"))).andReturn(urlNameCodec);
        expect(service.getStringCodec(eq(Encoding.DISPLAY_NAME), eq("en"))).andReturn(displayNameCodec);
        expect(urlNameCodec.encode(eq("new name"))).andReturn("new-name");
        expect(displayNameCodec.encode(eq("Old Name"))).andReturn("Old Name (encoded)");

        expect(WorkflowUtils.getWorkflow(eq(node), eq("core"), eq(DefaultWorkflow.class))).andReturn(Optional.of(workflow));

        workflow.rename("new-name");
        expectLastCall();

        replayAll();

        DocumentNameUtils.setNames(node, "new name", "Old Name", "en");

        verifyAll();
    }

    @Test
    public void setNamesOnlyDisplayName() throws Exception {
        final Node node = createHandle("old-name", "Old Name (encoded)");
        final StringCodecService service = createMock(StringCodecService.class);
        final StringCodec displayNameCodec = createMock(StringCodec.class);
        final StringCodec urlNameCodec = createMock(StringCodec.class);
        final DefaultWorkflow workflow = createMock(DefaultWorkflow.class);

        expect(HippoServiceRegistry.getService(eq(StringCodecService.class))).andReturn(service).anyTimes();
        expect(service.getStringCodec(eq(Encoding.NODE_NAME), eq("en"))).andReturn(urlNameCodec);
        expect(service.getStringCodec(eq(Encoding.DISPLAY_NAME), eq("en"))).andReturn(displayNameCodec);
        expect(urlNameCodec.encode(eq("old name"))).andReturn("old-name");
        expect(displayNameCodec.encode(eq("New Name"))).andReturn("New Name (encoded)");

        expect(WorkflowUtils.getWorkflow(eq(node), eq("core"), eq(DefaultWorkflow.class))).andReturn(Optional.of(workflow));

        workflow.setDisplayName("New Name (encoded)");
        expectLastCall();

        replayAll();

        DocumentNameUtils.setNames(node, "old name", "New Name", "en");

        verifyAll();
    }

    @Test
    public void setDisplayNameWithoutWorkflow() {
        final Node node = createMock(Node.class);
        expect(WorkflowUtils.getWorkflow(eq(node), eq("core"), eq(DefaultWorkflow.class))).andReturn(Optional.empty());
        replayAll();

        DocumentNameUtils.setDisplayName(node, "Test", "de");

        verifyAll();
    }

    @Test
    public void setDisplayName() throws Exception {
        final Node node = createMock(Node.class);
        final DefaultWorkflow workflow = createMock(DefaultWorkflow.class);
        final StringCodecService service = createMock(StringCodecService.class);
        final StringCodec codec = createMock(StringCodec.class);

        expect(WorkflowUtils.getWorkflow(eq(node), eq("core"), eq(DefaultWorkflow.class))).andReturn(Optional.of(workflow));
        expect(HippoServiceRegistry.getService(eq(StringCodecService.class))).andReturn(service);
        expect(service.getStringCodec(eq(Encoding.DISPLAY_NAME), eq("de"))).andReturn(codec);
        expect(codec.encode(eq("Test"))).andReturn("Test (encoded)");

        workflow.setDisplayName(eq("Test (encoded)"));
        expectLastCall();

        replayAll();

        DocumentNameUtils.setDisplayName(node, "Test", "de");

        verifyAll();
    }

    @Test
    public void setDisplayNameWorkflowThrowsException() throws Exception {
        final Node node = createHandle("test", "Test");
        final DefaultWorkflow workflow = createMock(DefaultWorkflow.class);
        final StringCodecService service = createMock(StringCodecService.class);
        final StringCodec codec = createMock(StringCodec.class);

        expect(WorkflowUtils.getWorkflow(eq(node), eq("core"), eq(DefaultWorkflow.class))).andReturn(Optional.of(workflow));
        expect(HippoServiceRegistry.getService(eq(StringCodecService.class))).andReturn(service);
        expect(service.getStringCodec(eq(Encoding.DISPLAY_NAME), eq("de"))).andReturn(codec);
        expect(codec.encode(eq("Test"))).andReturn("Test (encoded)");

        workflow.setDisplayName(eq("Test (encoded)"));
        expectLastCall().andThrow(new RepositoryException());

        replayAll();

        DocumentNameUtils.setDisplayName(node, "Test", "de");

        verifyAll();
    }

    private static MockNode createHandle(final String nodeName, final String displayName) throws Exception {
        final MockNode node = MockNode.root().addNode(nodeName, "hippo:document");
        node.setProperty("hippo:name", displayName);
        return node;
    }
}