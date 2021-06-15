/*
 * Copyright 2018-2021 Hippo B.V. (http://www.onehippo.com)
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

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hippoecm.frontend.FrontendNodeType.FRONTEND_CONFIG;
import static org.hippoecm.frontend.FrontendNodeType.FRONTEND_DISPLAY_NAME;
import static org.hippoecm.frontend.FrontendNodeType.FRONTEND_EXTENSION_POINT;
import static org.hippoecm.frontend.FrontendNodeType.FRONTEND_URL;
import static org.hippoecm.frontend.FrontendNodeType.NT_UI_EXTENSION;
import static org.hippoecm.frontend.FrontendNodeType.NT_UI_EXTENSIONS;
import static org.hippoecm.frontend.FrontendNodeType.UI_EXTENSIONS_NODE_NAME;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.collections4.MapUtils;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.hippoecm.hst.mock.core.container.MockComponentManager;
import org.hippoecm.hst.mock.core.container.MockContainerConfiguration;
import org.hippoecm.hst.site.HstServices;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

public class JcrUiExtensionLoaderTest {

    private static final String CHANNEL_PAGE_TOOLS_CONFIG_VALUE = "channel.page.tools";

    private static final Map<String, String> PLATFORM_PROPS = Collections
            .unmodifiableMap(MapUtils.putAll(new LinkedHashMap<String, String>(),
                    new String[] {
                            "public.brx.smEndpoint", "https://core.dxpapi.com/api/v1/core/",
                            "public.brx.smAccountId", "1234"
                            }));

    private JcrUiExtensionLoader loader;
    private MockNode root;

    private ComponentManager oldComponentManager;

    @Before
    public void setUp() throws Exception {
        root = MockNode.root();
        loader = new JcrUiExtensionLoader(root.getSession());

        final MockContainerConfiguration containerConfiguration = new MockContainerConfiguration();
        PLATFORM_PROPS.forEach((key, value) -> {
            containerConfiguration.setProperty(String.class, key, value);
        });

        final MockComponentManager componentManager = new MockComponentManager() {
            @Override
            public ContainerConfiguration getContainerConfiguration() {
                return containerConfiguration;
            }
        };

        this.oldComponentManager = HstServices.getComponentManager();
        HstServices.setComponentManager(componentManager);
    }

    @After
    public void tearDown() throws Exception {
        HstServices.setComponentManager(oldComponentManager);
    }

    private MockNode createConfigNode() throws RepositoryException {
        return root
                .addNode("hippo:configuration", "hipposys:configuration")
                .addNode("hippo:frontend", "hipposys:applicationfolder")
                .addNode("cms", "nt:unstructured")
                .addNode(UI_EXTENSIONS_NODE_NAME, NT_UI_EXTENSIONS);
    }

    private void addNode(final MockNode configNode, final String name) throws RepositoryException {
        final MockNode mockNode = configNode.addNode(name, NT_UI_EXTENSION);
        // mockNode.setProperty("frontend:displayName", "display name");
        mockNode.setProperty("frontend:extensionPoint", CHANNEL_PAGE_TOOLS_CONFIG_VALUE);
        mockNode.setProperty("frontend:url", "http://url");
    }
    
    @Test
    public void noConfigNode() {
        final Set<UiExtension> extensions = loader.loadUiExtensions();
        assertTrue(extensions.isEmpty());
    }

    @Test
    public void noConfigNodeLoadOne() {
        final Optional<UiExtension> uiExtension = loader.loadUiExtension("extension1", UiExtensionPoint.DOCUMENT_FIELD);
        assertFalse(uiExtension.isPresent());
    }

    @Test
    public void zeroExtensions() throws RepositoryException {
        createConfigNode();
        final Set<UiExtension> extensions = loader.loadUiExtensions();
        assertTrue(extensions.isEmpty());
    }

    @Test
    public void zeroExtensionsLoadOne() throws RepositoryException {
        createConfigNode();
        final Optional<UiExtension> uiExtension = loader.loadUiExtension("extension1", UiExtensionPoint.DOCUMENT_FIELD);
        assertFalse(uiExtension.isPresent());
    }

    @Test
    public void singleExtension() throws RepositoryException {
        final MockNode configNode = createConfigNode();
        final MockNode extensionNode = configNode.addNode("extension1", NT_UI_EXTENSION);
        extensionNode.setProperty(FRONTEND_EXTENSION_POINT, CHANNEL_PAGE_TOOLS_CONFIG_VALUE);
        extensionNode.setProperty(FRONTEND_DISPLAY_NAME, "Extension One");
        extensionNode.setProperty(FRONTEND_URL, "/extensions/extension-one");
        extensionNode.setProperty(FRONTEND_CONFIG,
                "{ \"url\": \"${public.brx.smEndpoint}?account_id=${public.brx.smAccountId}\" }");

        final Set<UiExtension> extensions = loader.loadUiExtensions();
        assertThat(extensions.size(), equalTo(1));

        final UiExtension extension = extensions.iterator().next();
        assertThat(extension.getId(), equalTo("extension1"));
        assertThat(extension.getExtensionPoint(), equalTo(UiExtensionPoint.CHANNEL_PAGE_TOOL));
        assertThat(extension.getDisplayName(), equalTo("Extension One"));
        assertThat(extension.getUrl(), equalTo("/extensions/extension-one"));
        assertThat(extension.getConfig(),
                equalTo("{ \"url\": \"https://core.dxpapi.com/api/v1/core/?account_id=1234\" }"));
    }

    @Test
    public void singleExtensionLoadOne() throws RepositoryException {
        final MockNode configNode = createConfigNode();
        final MockNode extensionNode = configNode.addNode("extension1", NT_UI_EXTENSION);
        extensionNode.setProperty(FRONTEND_EXTENSION_POINT, CHANNEL_PAGE_TOOLS_CONFIG_VALUE);
        extensionNode.setProperty(FRONTEND_DISPLAY_NAME, "Extension One");
        extensionNode.setProperty(FRONTEND_URL, "/extensions/extension-one");

        final Optional<UiExtension> uiExtension = loader.loadUiExtension("extension1", UiExtensionPoint.CHANNEL_PAGE_TOOL);
        assertTrue(uiExtension.isPresent());
        final UiExtension extension = uiExtension.get();

        assertThat(extension.getId(), equalTo("extension1"));
        assertThat(extension.getExtensionPoint(), equalTo(UiExtensionPoint.CHANNEL_PAGE_TOOL));
        assertThat(extension.getDisplayName(), equalTo("Extension One"));
        assertThat(extension.getUrl(), equalTo("/extensions/extension-one"));
    }

    @Test
    public void singleExtensionLoadOneType() throws RepositoryException {
        final MockNode configNode = createConfigNode();
        final MockNode extensionNode = configNode.addNode("extension1", NT_UI_EXTENSION);
        extensionNode.setProperty(FRONTEND_EXTENSION_POINT, CHANNEL_PAGE_TOOLS_CONFIG_VALUE);
        extensionNode.setProperty(FRONTEND_DISPLAY_NAME, "Extension One");
        extensionNode.setProperty(FRONTEND_URL, "/extensions/extension-one");

        final Optional<UiExtension> uiExtension = loader.loadUiExtension("extension1", UiExtensionPoint.DOCUMENT_FIELD);
        assertFalse(uiExtension.isPresent());
    }

    @Test
    public void multipleExtensions() throws RepositoryException {
        final MockNode configNode = createConfigNode();

        final MockNode extensionNode1 = configNode.addNode("extension1", NT_UI_EXTENSION);
        extensionNode1.setProperty(FRONTEND_EXTENSION_POINT, CHANNEL_PAGE_TOOLS_CONFIG_VALUE);
        extensionNode1.setProperty(FRONTEND_DISPLAY_NAME, "Extension One");
        extensionNode1.setProperty(FRONTEND_URL, "/extensions/extension-one");

        final MockNode extensionNode2 = configNode.addNode("extension2", NT_UI_EXTENSION);
        extensionNode2.setProperty(FRONTEND_EXTENSION_POINT, CHANNEL_PAGE_TOOLS_CONFIG_VALUE);
        extensionNode2.setProperty(FRONTEND_DISPLAY_NAME, "Extension Two");
        extensionNode2.setProperty(FRONTEND_URL, "/extensions/extension-two");

        final Set<UiExtension> extensions = loader.loadUiExtensions();
        assertThat(extensions.size(), equalTo(2));

        final Iterator<UiExtension> iterator = extensions.iterator();

        final UiExtension extension1 = iterator.next();
        assertThat(extension1.getId(), equalTo("extension1"));
        assertThat(extension1.getExtensionPoint(), equalTo(UiExtensionPoint.CHANNEL_PAGE_TOOL));
        assertThat(extension1.getDisplayName(), equalTo("Extension One"));
        assertThat(extension1.getUrl(), equalTo("/extensions/extension-one"));

        final UiExtension extension2 = iterator.next();
        assertThat(extension2.getId(), equalTo("extension2"));
        assertThat(extension2.getExtensionPoint(), equalTo(UiExtensionPoint.CHANNEL_PAGE_TOOL));
        assertThat(extension2.getDisplayName(), equalTo("Extension Two"));
        assertThat(extension2.getUrl(), equalTo("/extensions/extension-two"));
    }

    @Test
    public void multipleExtensionsLoadOne() throws RepositoryException {
        final MockNode configNode = createConfigNode();

        final MockNode extensionNode1 = configNode.addNode("extension1", NT_UI_EXTENSION);
        extensionNode1.setProperty(FRONTEND_EXTENSION_POINT, CHANNEL_PAGE_TOOLS_CONFIG_VALUE);
        extensionNode1.setProperty(FRONTEND_DISPLAY_NAME, "Extension One");
        extensionNode1.setProperty(FRONTEND_URL, "/extensions/extension-one");

        final MockNode extensionNode2 = configNode.addNode("extension2", NT_UI_EXTENSION);
        extensionNode2.setProperty(FRONTEND_EXTENSION_POINT, CHANNEL_PAGE_TOOLS_CONFIG_VALUE);
        extensionNode2.setProperty(FRONTEND_DISPLAY_NAME, "Extension Two");
        extensionNode2.setProperty(FRONTEND_URL, "/extensions/extension-two");

        final Optional<UiExtension> uiExtension = loader.loadUiExtension("extension2", UiExtensionPoint.CHANNEL_PAGE_TOOL);
        assertTrue(uiExtension.isPresent());

        final UiExtension extension2 = uiExtension.get();
        assertThat(extension2.getId(), equalTo("extension2"));
        assertThat(extension2.getExtensionPoint(), equalTo(UiExtensionPoint.CHANNEL_PAGE_TOOL));
        assertThat(extension2.getDisplayName(), equalTo("Extension Two"));
        assertThat(extension2.getUrl(), equalTo("/extensions/extension-two"));
    }

    @Test
    public void extensionsMustHaveUniqueID() throws RepositoryException {
        final MockNode configNode = createConfigNode();

        addNode(configNode, "extension1");
        addNode(configNode, "extension1");

        final Set<UiExtension> extensions = loader.loadUiExtensions();
        assertThat(extensions.size(), equalTo(1));
    }

    @Test
    public void defaultValue() throws RepositoryException {
        final MockNode configNode = createConfigNode();
        addNode(configNode, "extension1");

        final UiExtension extension = loader.loadUiExtensions().iterator().next();
        assertThat(extension.getDisplayName(), equalTo("extension1"));
        assertThat(extension.getInitialHeightInPixels(), equalTo(150));
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
