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
import org.onehippo.cms.channelmanager.content.document.model.DocumentState;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;

public class DocumentStateUtilsTest {

    @Test
    public void getDocumentStateNew() throws Exception {
        final MockNode newNode = createDocumentVariant("new");
        assertThat(DocumentStateUtils.getDocumentState(newNode), equalTo(DocumentState.NEW));
    }

    @Test
    public void getDocumentStateLive() throws Exception {
        final MockNode liveNode = createDocumentVariant("live");
        assertThat(DocumentStateUtils.getDocumentState(liveNode), equalTo(DocumentState.LIVE));
    }

    @Test
    public void getDocumentStateChanged() throws Exception {
        final MockNode changedNode = createDocumentVariant("changed");
        assertThat(DocumentStateUtils.getDocumentState(changedNode), equalTo(DocumentState.CHANGED));
    }

    @Test
    public void getDocumentStateUnknown() throws Exception {
        final MockNode unknownNode = createDocumentVariant("unknown");
        assertThat(DocumentStateUtils.getDocumentState(unknownNode), equalTo(DocumentState.UNKNOWN));
    }

    @Test
    public void getCustomDocumentState() throws Exception {
        final MockNode customNode = createDocumentVariant("custom");
        assertThat(DocumentStateUtils.getDocumentState(customNode), equalTo(DocumentState.UNKNOWN));
    }

    @Test
    public void getDocumentStateOfImage() throws Exception {
        final MockNode imageSet = MockNode.root().addNode("hippo.png", "hippogallery:imageset");
        assertThat(DocumentStateUtils.getDocumentState(imageSet), equalTo(DocumentState.UNKNOWN));
    }

    @Test
    public void getDocumentStateException() throws Exception {
        final Node brokenNode = createMock(Node.class);
        expect(brokenNode.isNodeType("hippostd:publishableSummary")).andThrow(new RepositoryException());
        expect(brokenNode.getPath()).andThrow(new RepositoryException());
        replayAll();

        assertThat(DocumentStateUtils.getDocumentState(brokenNode), equalTo(DocumentState.UNKNOWN));
    }

    private static MockNode createDocumentVariant(final String stateSummary) throws RepositoryException {
        final MockNode variant = MockNode.root().addNode("variant", "hippo:document");
        variant.addMixin("hippostd:publishableSummary");
        variant.setProperty("hippostd:stateSummary", stateSummary);
        return variant;
    }
}