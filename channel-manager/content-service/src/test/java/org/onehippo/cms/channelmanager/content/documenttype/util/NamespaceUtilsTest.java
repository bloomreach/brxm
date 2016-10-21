/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.documenttype.util;

import java.util.NoSuchElementException;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.documenttype.field.sort.NodeOrderFieldSorter;
import org.onehippo.cms.channelmanager.content.documenttype.field.sort.TwoColumnFieldSorter;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NamespaceUtils.class, JcrUtils.class})
public class NamespaceUtilsTest {
    @Test
    public void getDocumentRootNode() throws Exception {
        final Session session = createMock(Session.class);
        final Node rootNode = createMock(Node.class);

        expect(session.getNode("/hippo:namespaces/ns/testdocument")).andReturn(rootNode);
        replay(session);

        assertThat(NamespaceUtils.getDocumentTypeRootNode("ns:testdocument", session).get(), equalTo(rootNode));
    }

    @Test(expected = NoSuchElementException.class)
    public void getRootNodeWithInvalidId() throws Exception {
        final Session session = createMock(Session.class);

        NamespaceUtils.getDocumentTypeRootNode("blabla", session).get();
    }

    @Test(expected = NoSuchElementException.class)
    public void getRootNodeWithRepositoryException() throws Exception {
        final Session session = createMock(Session.class);

        expect(session.getNode("/hippo:namespaces/ns/testdocument")).andThrow(new RepositoryException());
        replay(session);

        NamespaceUtils.getDocumentTypeRootNode("ns:testdocument", session).get();
    }

    @Test
    public void getPluginClass() throws Exception {
        final String pluginClass = "pluginClass";
        final Property property = createMock(Property.class);
        final Node editorFieldNode = createMock(Node.class);

        expect(editorFieldNode.hasProperty("plugin.class")).andReturn(true);
        expect(editorFieldNode.getProperty("plugin.class")).andReturn(property);
        expect(property.getString()).andReturn(pluginClass);
        replay(editorFieldNode, property);

        assertThat(NamespaceUtils.getPluginClassForField(editorFieldNode).get(), equalTo(pluginClass));
    }

    @Test(expected = NoSuchElementException.class)
    public void getPluginClassWithRepositoryException() throws Exception {
        final Node editorFieldNode = createMock(Node.class);

        PowerMock.mockStaticPartial(JcrUtils.class, "getNodePathQuietly");

        expect(editorFieldNode.hasProperty("plugin.class")).andThrow(new RepositoryException());
        expect(JcrUtils.getNodePathQuietly(editorFieldNode)).andReturn("/bla");
        replay(editorFieldNode);
        PowerMock.replayAll();

        NamespaceUtils.getPluginClassForField(editorFieldNode).get();
    }

    @Test
    public void retrieveFieldSorterTwoColumns() throws Exception {
        final Node root = createMock(Node.class);
        final Node editorNode = createMock(Node.class);
        final Node layout = createMock(Node.class);

        PowerMock.mockStaticPartial(NamespaceUtils.class, "getPluginClassForField");

        expect(root.hasNode(NamespaceUtils.EDITOR_CONFIG_PATH)).andReturn(true);
        expect(root.getNode(NamespaceUtils.EDITOR_CONFIG_PATH)).andReturn(editorNode);
        expect(editorNode.hasNode("root")).andReturn(true);
        expect(editorNode.getNode("root")).andReturn(layout);
        expect(NamespaceUtils.getPluginClassForField(layout)).andReturn(Optional.of("org.hippoecm.frontend.editor.layout.TwoColumn"));
        replay(root, editorNode);
        PowerMock.replayAll();

        assertThat("2-col sorter is retrieved", NamespaceUtils.retrieveFieldSorter(root).get() instanceof TwoColumnFieldSorter);
    }

    @Test
    public void retrieveFieldSorterUnknownLayout() throws Exception {
        final Node root = createMock(Node.class);
        final Node editorNode = createMock(Node.class);
        final Node layout = createMock(Node.class);

        PowerMock.mockStaticPartial(NamespaceUtils.class, "getPluginClassForField");

        expect(root.hasNode(NamespaceUtils.EDITOR_CONFIG_PATH)).andReturn(true);
        expect(root.getNode(NamespaceUtils.EDITOR_CONFIG_PATH)).andReturn(editorNode);
        expect(editorNode.hasNode("root")).andReturn(true);
        expect(editorNode.getNode("root")).andReturn(layout);
        expect(NamespaceUtils.getPluginClassForField(layout)).andReturn(Optional.of("unknown"));
        replay(root, editorNode);
        PowerMock.replayAll();

        assertThat("default sorter is retrieved", NamespaceUtils.retrieveFieldSorter(root).get() instanceof NodeOrderFieldSorter);
    }

    @Test
    public void retrieveFieldSorterNoPluginClass() throws Exception {
        final Node root = createMock(Node.class);
        final Node editorNode = createMock(Node.class);
        final Node layout = createMock(Node.class);

        PowerMock.mockStaticPartial(NamespaceUtils.class, "getPluginClassForField");

        expect(root.hasNode(NamespaceUtils.EDITOR_CONFIG_PATH)).andReturn(true);
        expect(root.getNode(NamespaceUtils.EDITOR_CONFIG_PATH)).andReturn(editorNode);
        expect(editorNode.hasNode("root")).andReturn(true);
        expect(editorNode.getNode("root")).andReturn(layout);
        expect(NamespaceUtils.getPluginClassForField(layout)).andReturn(Optional.empty());
        replay(root, editorNode);
        PowerMock.replayAll();

        assertThat("default sorter is retrieved", NamespaceUtils.retrieveFieldSorter(root).get() instanceof NodeOrderFieldSorter);
    }

    @Test
    public void retrieveFieldSorterNoLayoutNode() throws Exception {
        final Node root = createMock(Node.class);
        final Node editorNode = createMock(Node.class);

        expect(root.hasNode(NamespaceUtils.EDITOR_CONFIG_PATH)).andReturn(true);
        expect(root.getNode(NamespaceUtils.EDITOR_CONFIG_PATH)).andReturn(editorNode);
        expect(editorNode.hasNode("root")).andReturn(false);
        replay(root, editorNode);

        assertThat("default sorter is retrieved", NamespaceUtils.retrieveFieldSorter(root).get() instanceof NodeOrderFieldSorter);
    }

    @Test(expected = NoSuchElementException.class)
    public void retrieveFieldSorterNoEditorNode() throws Exception {
        final Node root = createMock(Node.class);

        expect(root.hasNode(NamespaceUtils.EDITOR_CONFIG_PATH)).andReturn(false);
        replay(root);

        NamespaceUtils.retrieveFieldSorter(root).get();
    }

    @Test(expected = NoSuchElementException.class)
    public void retrieveFieldSorterRepositoryException() throws Exception {
        final Node root = createMock(Node.class);

        PowerMock.mockStaticPartial(JcrUtils.class, "getNodePathQuietly");

        expect(root.hasNode(NamespaceUtils.EDITOR_CONFIG_PATH)).andThrow(new RepositoryException());
        expect(JcrUtils.getNodePathQuietly(root)).andReturn("/bla");
        replay(root);
        PowerMock.replayAll();

        NamespaceUtils.retrieveFieldSorter(root).get();
    }
}
