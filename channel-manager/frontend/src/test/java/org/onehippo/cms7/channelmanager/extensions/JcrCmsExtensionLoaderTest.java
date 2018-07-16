/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.extensions;

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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class JcrCmsExtensionLoaderTest {

    private JcrCmsExtensionLoader loader;
    private MockNode configNode;

    @Before
    public void setUp() throws RepositoryException {
        final MockNode root = MockNode.root();
        loader = new JcrCmsExtensionLoader(root.getSession());
        configNode = root
                .addNode("hippo:configuration", "hipposys:configuration")
                .addNode("hippo:frontend", "hipposys:applicationfolder")
                .addNode("cms", "nt:unstructured")
                .addNode("extensions", "nt:unstructured");
    }

    @Test
    public void zeroExtensions() {
        final Set<CmsExtension> extensions = loader.loadCmsExtensions();
        assertTrue(extensions.isEmpty());
    }

    @Test
    public void singleExtension() throws RepositoryException {
        final MockNode extensionNode = configNode.addNode("extension1", "nt:unstructured");
        extensionNode.setProperty("context", "page");
        extensionNode.setProperty("displayName", "Extension One");
        extensionNode.setProperty("urlPath", "/extensions/extension-one");

        final Set<CmsExtension> extensions = loader.loadCmsExtensions();
        assertThat(extensions.size(), equalTo(1));

        final CmsExtension extension = extensions.iterator().next();
        assertThat(extension.getId(), equalTo("extension1"));
        assertThat(extension.getContext(), equalTo(CmsExtensionContext.PAGE));
        assertThat(extension.getDisplayName(), equalTo("Extension One"));
        assertThat(extension.getUrlPath(), equalTo("/extensions/extension-one"));
    }

    @Test
    public void multipleExtensions() throws RepositoryException {
        final MockNode extensionNode1 = configNode.addNode("extension1", "nt:unstructured");
        extensionNode1.setProperty("context", "page");
        extensionNode1.setProperty("displayName", "Extension One");
        extensionNode1.setProperty("urlPath", "/extensions/extension-one");

        final MockNode extensionNode2 = configNode.addNode("extension2", "nt:unstructured");
        extensionNode2.setProperty("context", "page");
        extensionNode2.setProperty("displayName", "Extension Two");
        extensionNode2.setProperty("urlPath", "/extensions/extension-two");

        final Set<CmsExtension> extensions = loader.loadCmsExtensions();
        assertThat(extensions.size(), equalTo(2));

        final Iterator<CmsExtension> iterator = extensions.iterator();

        final CmsExtension extension1 = iterator.next();
        assertThat(extension1.getId(), equalTo("extension1"));
        assertThat(extension1.getContext(), equalTo(CmsExtensionContext.PAGE));
        assertThat(extension1.getDisplayName(), equalTo("Extension One"));
        assertThat(extension1.getUrlPath(), equalTo("/extensions/extension-one"));

        final CmsExtension extension2 = iterator.next();
        assertThat(extension2.getId(), equalTo("extension2"));
        assertThat(extension2.getContext(), equalTo(CmsExtensionContext.PAGE));
        assertThat(extension2.getDisplayName(), equalTo("Extension Two"));
        assertThat(extension2.getUrlPath(), equalTo("/extensions/extension-two"));
    }

    @Test
    public void extensionsMustHaveUniqueID() throws RepositoryException {
        configNode.addNode("extension1", "nt:unstructured");
        configNode.addNode("extension1", "nt:unstructured");

        final Set<CmsExtension> extensions = loader.loadCmsExtensions();
        assertThat(extensions.size(), equalTo(1));
    }

    @Test
    public void defaultValues() throws RepositoryException {
        configNode.addNode("extension1", "nt:unstructured");

        final CmsExtension extension = loader.loadCmsExtensions().iterator().next();
        assertThat(extension.getContext(), equalTo(null));
        assertThat(extension.getDisplayName(), equalTo("extension1"));
        assertThat(extension.getUrlPath(), equalTo(null));
    }

    @Test
    public void repositoryException() throws RepositoryException {
        final Session brokenSession = createMock(Session.class);
        expect(brokenSession.getNode(anyString())).andThrow(new RepositoryException());
        replay(brokenSession);

        loader = new JcrCmsExtensionLoader(brokenSession);
        final Set<CmsExtension> extensions = loader.loadCmsExtensions();
        assertTrue(extensions.isEmpty());

        verify(brokenSession);
    }
}
