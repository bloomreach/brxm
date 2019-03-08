/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.openui.extensions;

import java.util.Iterator;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hippoecm.frontend.FrontendNodeType.FRONTEND_DISPLAY_NAME;
import static org.hippoecm.frontend.FrontendNodeType.FRONTEND_EXTENSION_POINT;
import static org.hippoecm.frontend.FrontendNodeType.FRONTEND_URL;
import static org.hippoecm.frontend.FrontendNodeType.NT_UI_EXTENSION;
import static org.hippoecm.frontend.FrontendNodeType.NT_UI_EXTENSIONS;
import static org.hippoecm.frontend.FrontendNodeType.UI_EXTENSIONS_NODE_NAME;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class JcrUiExtensionLoaderTest {

    private static final String CHANNEL_PAGE_TOOLS_EXTENSION_POINT = "channel.page.tools";

    private JcrUiExtensionLoader loader;
    private MockNode root;

    @Before
    public void setUp() throws RepositoryException {
        root = MockNode.root();
        loader = new JcrUiExtensionLoader(root.getSession());
    }

    private MockNode createConfigNode() throws RepositoryException {
        return root
                .addNode("hippo:configuration", "hipposys:configuration")
                .addNode("hippo:frontend", "hipposys:applicationfolder")
                .addNode("cms", "nt:unstructured")
                .addNode(UI_EXTENSIONS_NODE_NAME, NT_UI_EXTENSIONS);
    }

    @Test
    public void noConfigNode() {
        final Set<UiExtension> extensions = loader.loadUiExtensions();
        assertTrue(extensions.isEmpty());
    }

    @Test
    public void zeroExtensions() throws RepositoryException {
        createConfigNode();
        final Set<UiExtension> extensions = loader.loadUiExtensions();
        assertTrue(extensions.isEmpty());
    }

    @Test
    public void singleExtension() throws RepositoryException {
        final MockNode configNode = createConfigNode();
        final MockNode extensionNode = configNode.addNode("extension1", NT_UI_EXTENSION);
        extensionNode.setProperty(FRONTEND_EXTENSION_POINT, CHANNEL_PAGE_TOOLS_EXTENSION_POINT);
        extensionNode.setProperty(FRONTEND_DISPLAY_NAME, "Extension One");
        extensionNode.setProperty(FRONTEND_URL, "/extensions/extension-one");

        final Set<UiExtension> extensions = loader.loadUiExtensions();
        assertThat(extensions.size(), equalTo(1));

        final UiExtension extension = extensions.iterator().next();
        assertThat(extension.getId(), equalTo("extension1"));
        assertThat(extension.getExtensionPoint(), equalTo(CHANNEL_PAGE_TOOLS_EXTENSION_POINT));
        assertThat(extension.getDisplayName(), equalTo("Extension One"));
        assertThat(extension.getUrl(), equalTo("/extensions/extension-one"));
    }

    @Test
    public void multipleExtensions() throws RepositoryException {
        final MockNode configNode = createConfigNode();

        final MockNode extensionNode1 = configNode.addNode("extension1", NT_UI_EXTENSION);
        extensionNode1.setProperty(FRONTEND_EXTENSION_POINT, CHANNEL_PAGE_TOOLS_EXTENSION_POINT);
        extensionNode1.setProperty(FRONTEND_DISPLAY_NAME, "Extension One");
        extensionNode1.setProperty(FRONTEND_URL, "/extensions/extension-one");

        final MockNode extensionNode2 = configNode.addNode("extension2", NT_UI_EXTENSION);
        extensionNode2.setProperty(FRONTEND_EXTENSION_POINT, CHANNEL_PAGE_TOOLS_EXTENSION_POINT);
        extensionNode2.setProperty(FRONTEND_DISPLAY_NAME, "Extension Two");
        extensionNode2.setProperty(FRONTEND_URL, "/extensions/extension-two");

        final Set<UiExtension> extensions = loader.loadUiExtensions();
        assertThat(extensions.size(), equalTo(2));

        final Iterator<UiExtension> iterator = extensions.iterator();

        final UiExtension extension1 = iterator.next();
        assertThat(extension1.getId(), equalTo("extension1"));
        assertThat(extension1.getExtensionPoint(), equalTo(CHANNEL_PAGE_TOOLS_EXTENSION_POINT));
        assertThat(extension1.getDisplayName(), equalTo("Extension One"));
        assertThat(extension1.getUrl(), equalTo("/extensions/extension-one"));

        final UiExtension extension2 = iterator.next();
        assertThat(extension2.getId(), equalTo("extension2"));
        assertThat(extension2.getExtensionPoint(), equalTo(CHANNEL_PAGE_TOOLS_EXTENSION_POINT));
        assertThat(extension2.getDisplayName(), equalTo("Extension Two"));
        assertThat(extension2.getUrl(), equalTo("/extensions/extension-two"));
    }

    @Test
    public void extensionsMustHaveUniqueID() throws RepositoryException {
        final MockNode configNode = createConfigNode();

        configNode.addNode("extension1", NT_UI_EXTENSION);
        configNode.addNode("extension1", NT_UI_EXTENSION);

        final Set<UiExtension> extensions = loader.loadUiExtensions();
        assertThat(extensions.size(), equalTo(1));
    }

    @Test
    public void defaultValues() throws RepositoryException {
        final MockNode configNode = createConfigNode();
        configNode.addNode("extension1", NT_UI_EXTENSION);

        final UiExtension extension = loader.loadUiExtensions().iterator().next();
        assertThat(extension.getExtensionPoint(), equalTo(null));
        assertThat(extension.getDisplayName(), equalTo("extension1"));
        assertThat(extension.getUrl(), equalTo(null));
    }

    @Test
    public void repositoryException() throws RepositoryException {
        final Session brokenSession = createMock(Session.class);
        expect(brokenSession.nodeExists(anyString())).andThrow(new RepositoryException());
        replay(brokenSession);

        loader = new JcrUiExtensionLoader(brokenSession);
        final Set<UiExtension> extensions = loader.loadUiExtensions();
        assertTrue(extensions.isEmpty());

        verify(brokenSession);
    }
}
