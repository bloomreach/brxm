/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.document.model.PublicationState;
import org.onehippo.repository.mock.MockNode;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
public class PublicationStateUtilsTest {

    @Test
    public void newHandle() throws Exception {
        final MockNode newHandle = createDocumentVariant("new").getParent();
        assertThat(PublicationStateUtils.getPublicationStateFromHandle(newHandle), equalTo(PublicationState.NEW));
    }

    @Test
    public void liveHandle() throws Exception {
        final MockNode liveHandle = createDocumentVariant("live").getParent();
        assertThat(PublicationStateUtils.getPublicationStateFromHandle(liveHandle), equalTo(PublicationState.LIVE));
    }

    @Test
    public void changedHandle() throws Exception {
        final MockNode changedHandle = createDocumentVariant("changed").getParent();
        assertThat(PublicationStateUtils.getPublicationStateFromHandle(changedHandle), equalTo(PublicationState.CHANGED));
    }

    @Test
    public void unknownHandle() throws Exception {
        final MockNode unknownHandle = createDocumentVariant("unknown").getParent();
        assertThat(PublicationStateUtils.getPublicationStateFromHandle(unknownHandle), equalTo(PublicationState.UNKNOWN));
    }

    @Test
    public void customHandle() throws Exception {
        final MockNode customHandle = createDocumentVariant("custom").getParent();
        assertThat(PublicationStateUtils.getPublicationStateFromHandle(customHandle), equalTo(PublicationState.UNKNOWN));
    }

    @Test
    public void imageHandle() throws Exception {
        final MockNode imageSet = MockNode.root().addNode("hippo.png", "hippo:handle");
        imageSet.addNode("hippo.png", "hippo:document");

        assertThat(PublicationStateUtils.getPublicationStateFromHandle(imageSet), equalTo(PublicationState.UNKNOWN));
    }

    @Test
    @PrepareForTest({DocumentHandleUtils.class})
    public void brokenHandle() throws Exception {
        final Node brokenHandle = createMock(Node.class);

        mockStatic(DocumentHandleUtils.class);
        expect(DocumentHandleUtils.isValidHandle(brokenHandle)).andReturn(true);
        expect(brokenHandle.getName()).andThrow(new RepositoryException());
        expect(brokenHandle.getPath()).andThrow(new RepositoryException());
        replayAll();

        assertThat(PublicationStateUtils.getPublicationStateFromHandle(brokenHandle), equalTo(PublicationState.UNKNOWN));
    }

    @Test
    public void newVariant() throws Exception {
        final MockNode newVariant = createDocumentVariant("new");
        assertThat(PublicationStateUtils.getPublicationStateFromVariant(newVariant), equalTo(PublicationState.NEW));
    }

    @Test
    public void liveVariant() throws Exception {
        final MockNode liveVariant = createDocumentVariant("live");
        assertThat(PublicationStateUtils.getPublicationStateFromVariant(liveVariant), equalTo(PublicationState.LIVE));
    }

    @Test
    public void changedVariant() throws Exception {
        final MockNode changedVariant = createDocumentVariant("changed");
        assertThat(PublicationStateUtils.getPublicationStateFromVariant(changedVariant), equalTo(PublicationState.CHANGED));
    }

    @Test
    public void unknownVariant() throws Exception {
        final MockNode unknownVariant = createDocumentVariant("unknown");
        assertThat(PublicationStateUtils.getPublicationStateFromVariant(unknownVariant), equalTo(PublicationState.UNKNOWN));
    }

    @Test
    public void customVariant() throws Exception {
        final MockNode customVariant = createDocumentVariant("custom");
        assertThat(PublicationStateUtils.getPublicationStateFromVariant(customVariant), equalTo(PublicationState.UNKNOWN));
    }

    @Test
    public void imageVariant() throws Exception {
        final MockNode imageSet = MockNode.root().addNode("hippo.png", "hippogallery:imageset");
        assertThat(PublicationStateUtils.getPublicationStateFromVariant(imageSet), equalTo(PublicationState.UNKNOWN));
    }

    @Test
    public void brokenVariant() throws Exception {
        final Node brokenNode = createMock(Node.class);
        expect(brokenNode.isNodeType("hippostd:publishableSummary")).andThrow(new RepositoryException());
        expect(brokenNode.getPath()).andThrow(new RepositoryException());
        replayAll();

        assertThat(PublicationStateUtils.getPublicationStateFromVariant(brokenNode), equalTo(PublicationState.UNKNOWN));
    }

    private static MockNode createDocumentVariant(final String stateSummary) throws RepositoryException {
        final MockNode handle = MockNode.root().addNode("some-document", "hippo:handle");
        final MockNode variant = handle.addNode("some-document", "hippo:document");
        variant.addMixin("hippostd:publishableSummary");
        variant.setProperty("hippostd:stateSummary", stateSummary);
        return variant;
    }
}