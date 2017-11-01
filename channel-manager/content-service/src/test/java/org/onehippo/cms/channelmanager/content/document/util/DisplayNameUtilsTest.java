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
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({HippoServiceRegistry.class, WorkflowUtils.class})
public class DisplayNameUtilsTest {

    @Before
    public void setUp() {
        mockStatic(HippoServiceRegistry.class);
        mockStatic(WorkflowUtils.class);
    }

    @Test(expected = IllegalAccessException.class)
    public void cannotCreateInstance() throws Exception {
        DisplayNameUtils.class.newInstance();
    }

    @Test
    public void encodeDisplayNameWithLocale() {
        final StringCodecService service = createMock(StringCodecService.class);
        final StringCodec codec = createMock(StringCodec.class);

        expect(HippoServiceRegistry.getService(eq(StringCodecService.class))).andReturn(service);
        expect(service.getStringCodec(eq(StringCodecService.Encoding.DISPLAY_NAME), eq("en"))).andReturn(codec);
        expect(codec.encode("Some Name")).andReturn("Some Name (encoded)");
        replayAll();

        final String encoded = DisplayNameUtils.encodeDisplayName("Some Name", "en");

        assertThat(encoded, equalTo("Some Name (encoded)"));
        verifyAll();
    }

    @Test
    public void encodeDisplayNameWithoutLocale() {
        final StringCodecService service = createMock(StringCodecService.class);
        final StringCodec codec = createMock(StringCodec.class);

        expect(HippoServiceRegistry.getService(eq(StringCodecService.class))).andReturn(service);
        expect(service.getStringCodec(eq(StringCodecService.Encoding.DISPLAY_NAME), eq(null))).andReturn(codec);
        expect(codec.encode("Some Name")).andReturn("Some Name (encoded)");
        replayAll();

        final String encoded = DisplayNameUtils.encodeDisplayName("Some Name", null);

        assertThat(encoded, equalTo("Some Name (encoded)"));
        verifyAll();
    }

    @Test
    public void setDisplayNameWithoutWorkflow() {
        final Node node = createMock(Node.class);
        expect(WorkflowUtils.getWorkflow(eq(node), eq("core"), eq(DefaultWorkflow.class))).andReturn(Optional.empty());
        replayAll();

        DisplayNameUtils.setDisplayName(node, "Test");

        verifyAll();
    }

    @Test
    public void setDisplayName() throws Exception {
        final Node node = createMock(Node.class);
        final DefaultWorkflow workflow = createMock(DefaultWorkflow.class);
        expect(WorkflowUtils.getWorkflow(eq(node), eq("core"), eq(DefaultWorkflow.class))).andReturn(Optional.of(workflow));

        workflow.setDisplayName(eq("Test"));
        expectLastCall();

        replayAll();

        DisplayNameUtils.setDisplayName(node, "Test");

        verifyAll();
    }

    @Test
    public void setDisplayNameWorkflowThrowsException() throws Exception {
        final Node node = createMock(Node.class);
        final DefaultWorkflow workflow = createMock(DefaultWorkflow.class);
        expect(WorkflowUtils.getWorkflow(eq(node), eq("core"), eq(DefaultWorkflow.class))).andReturn(Optional.of(workflow));

        workflow.setDisplayName(eq("Test"));
        expectLastCall().andThrow(new RepositoryException());
        expect(node.getPath()).andReturn("/test");

        replayAll();

        DisplayNameUtils.setDisplayName(node, "Test");

        verifyAll();
    }
}