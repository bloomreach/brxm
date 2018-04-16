/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
import org.onehippo.cms.channelmanager.content.document.model.PublicationState;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;

public class PublicationStateUtilsTest {

    @Test
    public void getDocumentStateNew() throws Exception {
        final MockNode newNode = createDocumentVariant("new");
        assertThat(PublicationStateUtils.getPublicationState(newNode), equalTo(PublicationState.NEW));
    }

    @Test
    public void getDocumentStateLive() throws Exception {
        final MockNode liveNode = createDocumentVariant("live");
        assertThat(PublicationStateUtils.getPublicationState(liveNode), equalTo(PublicationState.LIVE));
    }

    @Test
    public void getDocumentStateChanged() throws Exception {
        final MockNode changedNode = createDocumentVariant("changed");
        assertThat(PublicationStateUtils.getPublicationState(changedNode), equalTo(PublicationState.CHANGED));
    }

    @Test
    public void getDocumentStateUnknown() throws Exception {
        final MockNode unknownNode = createDocumentVariant("unknown");
        assertThat(PublicationStateUtils.getPublicationState(unknownNode), equalTo(PublicationState.UNKNOWN));
    }

    @Test
    public void getCustomDocumentState() throws Exception {
        final MockNode customNode = createDocumentVariant("custom");
        assertThat(PublicationStateUtils.getPublicationState(customNode), equalTo(PublicationState.UNKNOWN));
    }

    @Test
    public void getDocumentStateOfImage() throws Exception {
        final MockNode imageSet = MockNode.root().addNode("hippo.png", "hippogallery:imageset");
        assertThat(PublicationStateUtils.getPublicationState(imageSet), equalTo(PublicationState.UNKNOWN));
    }

    @Test
    public void getDocumentStateException() throws Exception {
        final Node brokenNode = createMock(Node.class);
        expect(brokenNode.isNodeType("hippostd:publishableSummary")).andThrow(new RepositoryException());
        expect(brokenNode.getPath()).andThrow(new RepositoryException());
        replayAll();

        assertThat(PublicationStateUtils.getPublicationState(brokenNode), equalTo(PublicationState.UNKNOWN));
    }

    private static MockNode createDocumentVariant(final String stateSummary) throws RepositoryException {
        final MockNode variant = MockNode.root().addNode("variant", "hippo:document");
        variant.addMixin("hippostd:publishableSummary");
        variant.setProperty("hippostd:stateSummary", stateSummary);
        return variant;
    }
}