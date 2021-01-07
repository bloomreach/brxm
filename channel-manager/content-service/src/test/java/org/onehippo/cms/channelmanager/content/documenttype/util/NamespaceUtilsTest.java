/*
 * Copyright 2016-2021 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.sort.NodeOrderFieldSorter;
import org.onehippo.cms.channelmanager.content.documenttype.field.sort.TwoColumnFieldSorter;
import org.onehippo.repository.mock.MockNode;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest({NamespaceUtils.class, JcrUtils.class})
public class NamespaceUtilsTest {

    @Before
    public void setup() {
        PowerMock.mockStatic(JcrUtils.class);
    }

    @Test
    public void getDocumentRootNode() throws Exception {
        final Session session = createMock(Session.class);
        final Node rootNode = createMock(Node.class);

        expect(session.getNode("/hippo:namespaces/ns/testdocument")).andReturn(rootNode);
        replayAll();

        assertThat(NamespaceUtils.getContentTypeRootNode("ns:testdocument", session).get(), equalTo(rootNode));
    }

    @Test
    public void getSystemTypeRootNode() throws Exception {
        final Session session = createMock(Session.class);
        final Node rootNode = createMock(Node.class);

        expect(session.getNode("/hippo:namespaces/system/String")).andReturn(rootNode);
        replayAll();

        assertThat(NamespaceUtils.getContentTypeRootNode("String", session).get(), equalTo(rootNode));
    }

    @Test
    public void getRootNodeWithInvalidId() {
        final Session session = createMock(Session.class);

        assertFalse(NamespaceUtils.getContentTypeRootNode("bla:bla:bla", session).isPresent());
    }

    @Test
    public void getRootNodeWithRepositoryException() throws Exception {
        final Session session = createMock(Session.class);

        expect(session.getNode("/hippo:namespaces/ns/testdocument")).andThrow(new RepositoryException());
        replayAll();

        assertFalse(NamespaceUtils.getContentTypeRootNode("ns:testdocument", session).isPresent());
    }

    @Test
    public void getNodeTypeNodeWithRepositoryException() throws Exception {
        final Node contentTypeRootNode = createMock(Node.class);

        expect(JcrUtils.getNodePathQuietly(contentTypeRootNode)).andReturn("/bla");
        expect(contentTypeRootNode.hasNode(NamespaceUtils.NODE_TYPE_PATH)).andThrow(new RepositoryException());
        replayAll();

        assertFalse(NamespaceUtils.getNodeTypeNode(contentTypeRootNode, false).isPresent());
        verifyAll();
    }

    @Test
    public void getNodeTypeNodeWithoutNode() throws Exception {
        final Node contentTypeRootNode = createMock(Node.class);

        expect(contentTypeRootNode.hasNode(NamespaceUtils.NODE_TYPE_PATH)).andReturn(false);
        replayAll();

        assertFalse(NamespaceUtils.getNodeTypeNode(contentTypeRootNode, false).isPresent());
        verifyAll();
    }

    @Test
    public void getNodeTypeNodeIgnoreChildren() throws Exception {
        final Node contentTypeRootNode = createMock(Node.class);
        final Node nodeTypeNode = createMock(Node.class);

        expect(contentTypeRootNode.hasNode(NamespaceUtils.NODE_TYPE_PATH)).andReturn(true);
        expect(contentTypeRootNode.getNode(NamespaceUtils.NODE_TYPE_PATH)).andReturn(nodeTypeNode);
        replayAll();

        assertThat(NamespaceUtils.getNodeTypeNode(contentTypeRootNode, true).get(), equalTo(nodeTypeNode));
        verifyAll();
    }

    @Test
    public void getNodeTypeNodeWithoutChildren() throws Exception {
        final Node contentTypeRootNode = createMock(Node.class);
        final Node nodeTypeNode = createMock(Node.class);

        expect(contentTypeRootNode.hasNode(NamespaceUtils.NODE_TYPE_PATH)).andReturn(true);
        expect(contentTypeRootNode.getNode(NamespaceUtils.NODE_TYPE_PATH)).andReturn(nodeTypeNode);
        expect(nodeTypeNode.hasNodes()).andReturn(false);
        replayAll();

        assertFalse(NamespaceUtils.getNodeTypeNode(contentTypeRootNode, false).isPresent());
        verifyAll();
    }

    @Test
    public void getNodeTypeNodeWithChildren() throws Exception {
        final Node contentTypeRootNode = createMock(Node.class);
        final Node nodeTypeNode = createMock(Node.class);

        expect(contentTypeRootNode.hasNode(NamespaceUtils.NODE_TYPE_PATH)).andReturn(true);
        expect(contentTypeRootNode.getNode(NamespaceUtils.NODE_TYPE_PATH)).andReturn(nodeTypeNode);
        expect(nodeTypeNode.hasNodes()).andReturn(true);
        replayAll();

        assertThat(NamespaceUtils.getNodeTypeNode(contentTypeRootNode, false).get(), equalTo(nodeTypeNode));
        verifyAll();
    }

    @Test
    public void getEditorFieldConfigNodesWithRepositoryException() throws Exception {
        final Node contentTypeRootNode = createMock(Node.class);

        expect(contentTypeRootNode.hasNode(NamespaceUtils.EDITOR_CONFIG_PATH)).andThrow(new RepositoryException());
        expect(JcrUtils.getNodePathQuietly(contentTypeRootNode)).andReturn("/bla");
        replayAll();

        assertThat(NamespaceUtils.getEditorFieldConfigNodes(contentTypeRootNode), empty());
        verifyAll();
    }

    @Test
    public void getEditorFieldConfigNodesWithoutEditorConfig() throws Exception {
        final Node contentTypeRootNode = createMock(Node.class);

        expect(contentTypeRootNode.hasNode(NamespaceUtils.EDITOR_CONFIG_PATH)).andReturn(false);
        replayAll();

        assertThat(NamespaceUtils.getEditorFieldConfigNodes(contentTypeRootNode), empty());
        verifyAll();
    }

    @Test
    public void getEditorFieldConfigNodesWithEditorConfig() throws Exception {
        final Node contentTypeRootNode = MockNode.root();
        final Node editorConfigNode = contentTypeRootNode.addNode("editor:templates", "bla").addNode("_default_", "bla");

        final Node fieldNode1 = editorConfigNode.addNode("field1", "bla");
        final Node fieldNode2 = editorConfigNode.addNode("field2", "bla");
        final Node fieldNode3 = editorConfigNode.addNode("field3", "bla");

        final List<Node> editorFieldConfigNodes = NamespaceUtils.getEditorFieldConfigNodes(contentTypeRootNode);

        assertThat(editorFieldConfigNodes.size(), equalTo(3));
        assertThat(editorFieldConfigNodes.get(0), equalTo(fieldNode1));
        assertThat(editorFieldConfigNodes.get(1), equalTo(fieldNode2));
        assertThat(editorFieldConfigNodes.get(2), equalTo(fieldNode3));
    }

    @Test
    public void getPathForNodeTypeFieldWithRepositoryException() throws Exception {
        final Node nodeTypeNode = createMock(Node.class);

        expect(nodeTypeNode.hasNode("fieldName")).andThrow(new RepositoryException());
        expect(JcrUtils.getNodePathQuietly(nodeTypeNode)).andReturn("/bla");
        replayAll();

        assertFalse(NamespaceUtils.getPathForNodeTypeField(nodeTypeNode, "fieldName").isPresent());
        verifyAll();
    }

    @Test
    public void getPathForNodeTypeFieldWithoutFieldNode() throws Exception {
        final Node nodeTypeNode = createMock(Node.class);

        expect(nodeTypeNode.hasNode("fieldName")).andReturn(false);
        replayAll();

        assertFalse(NamespaceUtils.getPathForNodeTypeField(nodeTypeNode, "fieldName").isPresent());
        verifyAll();
    }

    @Test
    public void getPathForNodeTypeFieldWithFieldNode() throws Exception {
        final Node nodeTypeNode = createMock(Node.class);
        final Node fieldNode = createMock(Node.class);
        final Property property = createMock(Property.class);

        expect(nodeTypeNode.hasNode("fieldName")).andReturn(true);
        expect(nodeTypeNode.getNode("fieldName")).andReturn(fieldNode);
        expect(fieldNode.hasProperty(HippoNodeType.HIPPO_PATH)).andReturn(true);
        expect(fieldNode.getProperty(HippoNodeType.HIPPO_PATH)).andReturn(property);
        expect(property.getString()).andReturn("/path");
        replayAll();

        assertThat(NamespaceUtils.getPathForNodeTypeField(nodeTypeNode, "fieldName").get(), equalTo("/path"));
        verifyAll();
    }

    @Test
    public void getConfigPropertyFromClusterOptions() throws Exception {
        final Node editorFieldConfigNode = MockNode.root();
        final Node clusterOptionsNode = editorFieldConfigNode.addNode(NamespaceUtils.CLUSTER_OPTIONS, null);
        clusterOptionsNode.setProperty("maxlength", "256");
        final FieldTypeContext fieldContext = new FieldTypeContext(null, null, null, false, false, null, null, editorFieldConfigNode);

        assertThat(NamespaceUtils.getConfigProperty(fieldContext, "maxlength", JcrStringReader.get()).get(), equalTo("256"));
    }

    @Test
    public void getConfigPropertyFromEditorConfig() throws Exception {
        final Node editorFieldConfigNode = MockNode.root();
        editorFieldConfigNode.setProperty("maxlength", "256");
        final FieldTypeContext fieldContext = new FieldTypeContext(null, null, null, false, false, null, null, editorFieldConfigNode);

        assertThat(NamespaceUtils.getConfigProperty(fieldContext, "maxlength", JcrStringReader.get()).get(), equalTo("256"));
    }

    @Test
    public void getConfigPropertyNotInClusterOptionsOrEditorConfigFromType() throws Exception {
        final String propertyName = "maxlength";
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Node editorFieldConfigNode = createMock(Node.class);
        final Node clusterOptionsNode = createMock(Node.class);
        final Node contentTypeRootNode = createMock(Node.class);
        final Node contentTypeEditorConfigNode = createMock(Node.class);
        final Property property = createMock(Property.class);
        final Session session = createMock(Session.class);
        final FieldTypeContext fieldContext = new FieldTypeContext("fieldName", "hippo:fieldtype", "hippo:fieldtype",
                true, false, Collections.emptyList(), parentContext, editorFieldConfigNode);

        expect(editorFieldConfigNode.hasNode(NamespaceUtils.CLUSTER_OPTIONS)).andReturn(true);
        expect(editorFieldConfigNode.getNode(NamespaceUtils.CLUSTER_OPTIONS)).andReturn(clusterOptionsNode);
        expect(editorFieldConfigNode.hasProperty(propertyName)).andReturn(false);
        expect(clusterOptionsNode.hasProperty(propertyName)).andReturn(false);

        expect(parentContext.getSession()).andReturn(session);
        expect(session.getNode("/hippo:namespaces/hippo/fieldtype")).andReturn(contentTypeRootNode);
        expect(contentTypeRootNode.hasNode("editor:templates/_default_")).andReturn(true);
        expect(contentTypeRootNode.getNode("editor:templates/_default_")).andReturn(contentTypeEditorConfigNode);
        expect(contentTypeEditorConfigNode.hasProperty(propertyName)).andReturn(true);
        expect(contentTypeEditorConfigNode.getProperty(propertyName)).andReturn(property);
        expect(property.getString()).andReturn("256");

        replayAll();

        assertThat(NamespaceUtils.getConfigProperty(fieldContext, propertyName, JcrStringReader.get()).get(),
                equalTo("256"));

        verifyAll();
    }

    @Test
    public void getConfigPropertyNoClusterOptionsAndNotInEditorConfigFromType() throws Exception {
        final String propertyName = "maxlength";
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Node editorFieldConfigNode = createMock(Node.class);
        final Node contentTypeRootNode = createMock(Node.class);
        final Node contentTypeEditorConfigNode = createMock(Node.class);
        final Property property = createMock(Property.class);
        final Session session = createMock(Session.class);
        final FieldTypeContext fieldContext = new FieldTypeContext("fieldName", "hippo:fieldtype", "hippo:fieldtype",
                true, false, Collections.emptyList(), parentContext, editorFieldConfigNode);

        expect(editorFieldConfigNode.hasNode(NamespaceUtils.CLUSTER_OPTIONS)).andReturn(false);
        expect(editorFieldConfigNode.hasProperty(propertyName)).andReturn(false);

        expect(parentContext.getSession()).andReturn(session);
        expect(session.getNode("/hippo:namespaces/hippo/fieldtype")).andReturn(contentTypeRootNode);
        expect(contentTypeRootNode.hasNode("editor:templates/_default_")).andReturn(true);
        expect(contentTypeRootNode.getNode("editor:templates/_default_")).andReturn(contentTypeEditorConfigNode);
        expect(contentTypeEditorConfigNode.hasProperty(propertyName)).andReturn(true);
        expect(contentTypeEditorConfigNode.getProperty(propertyName)).andReturn(property);
        expect(property.getString()).andReturn("256");

        replayAll();

        assertThat(NamespaceUtils.getConfigProperty(fieldContext, propertyName, JcrStringReader.get()).get(),
                equalTo("256"));

        verifyAll();
    }

    @Test
    public void getConfigPropertyNoClusterOptionsNotInType() throws Exception {
        final String propertyName = "maxlength";
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Node editorFieldConfigNode = createMock(Node.class);
        final Node contentTypeRootNode = createMock(Node.class);
        final Node contentTypeEditorConfigNode = createMock(Node.class);
        final Session session = createMock(Session.class);
        final FieldTypeContext fieldContext = new FieldTypeContext("fieldName", "hippo:fieldtype", "hippo:fieldtype",
                true, false, Collections.emptyList(), parentContext, editorFieldConfigNode);

        expect(editorFieldConfigNode.hasNode(NamespaceUtils.CLUSTER_OPTIONS)).andReturn(false);
        expect(editorFieldConfigNode.hasProperty(propertyName)).andReturn(false);

        expect(parentContext.getSession()).andReturn(session);
        expect(session.getNode("/hippo:namespaces/hippo/fieldtype")).andReturn(contentTypeRootNode);
        expect(contentTypeRootNode.hasNode("editor:templates/_default_")).andReturn(true);
        expect(contentTypeRootNode.getNode("editor:templates/_default_")).andReturn(contentTypeEditorConfigNode);
        expect(contentTypeEditorConfigNode.hasProperty(propertyName)).andReturn(false);

        replayAll();

        assertFalse(NamespaceUtils.getConfigProperty(fieldContext, propertyName, JcrStringReader.get()).isPresent());

        verifyAll();
    }

    @Test
    public void getPluginClass() throws Exception {
        final String pluginClass = "pluginClass";
        final Property property = createMock(Property.class);
        final Node editorFieldNode = createMock(Node.class);

        expect(editorFieldNode.hasProperty("plugin.class")).andReturn(true);
        expect(editorFieldNode.getProperty("plugin.class")).andReturn(property);
        expect(property.getString()).andReturn(pluginClass);
        replayAll();

        assertThat(NamespaceUtils.getPluginClassForField(editorFieldNode).get(), equalTo(pluginClass));
    }

    @Test
    public void getPluginClassForFieldWithoutProperty() throws Exception {
        final Node editorFieldNode = createMock(Node.class);

        expect(editorFieldNode.hasProperty("plugin.class")).andReturn(false);
        replayAll();

        assertFalse(NamespaceUtils.getPluginClassForField(editorFieldNode).isPresent());
    }

    @Test
    public void getPluginClassWithRepositoryException() throws Exception {
        final Node editorFieldNode = createMock(Node.class);

        expect(editorFieldNode.hasProperty("plugin.class")).andThrow(new RepositoryException());
        expect(JcrUtils.getNodePathQuietly(editorFieldNode)).andReturn("/bla");
        replayAll(editorFieldNode);

        assertFalse(NamespaceUtils.getPluginClassForField(editorFieldNode).isPresent());
    }

    @Test
    public void getWicketIdForField() throws Exception {
        final Property property = createMock(Property.class);
        final Node editorFieldNode = createMock(Node.class);

        expect(editorFieldNode.hasProperty("wicket.id")).andReturn(true);
        expect(editorFieldNode.getProperty("wicket.id")).andReturn(property);
        expect(property.getString()).andReturn("WicketID");
        replayAll();

        assertThat(NamespaceUtils.getWicketIdForField(editorFieldNode).get(), equalTo("WicketID"));

        verifyAll();
    }

    @Test
    public void getFieldProperty() throws Exception {
        final Property property = createMock(Property.class);
        final Node editorFieldNode = createMock(Node.class);

        expect(editorFieldNode.hasProperty("field")).andReturn(true);
        expect(editorFieldNode.getProperty("field")).andReturn(property);
        expect(property.getString()).andReturn("fieldName");
        replayAll();

        assertThat(NamespaceUtils.getFieldProperty(editorFieldNode).get(), equalTo("fieldName"));

        verifyAll();
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
        replayAll(root, editorNode);

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
        replayAll(root, editorNode);

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
        replayAll(root, editorNode);

        assertThat("default sorter is retrieved", NamespaceUtils.retrieveFieldSorter(root).get() instanceof NodeOrderFieldSorter);
    }

    @Test
    public void retrieveFieldSorterNoLayoutNode() throws Exception {
        final Node root = createMock(Node.class);
        final Node editorNode = createMock(Node.class);

        expect(root.hasNode(NamespaceUtils.EDITOR_CONFIG_PATH)).andReturn(true);
        expect(root.getNode(NamespaceUtils.EDITOR_CONFIG_PATH)).andReturn(editorNode);
        expect(editorNode.hasNode("root")).andReturn(false);
        replayAll();

        assertThat("default sorter is retrieved", NamespaceUtils.retrieveFieldSorter(root).get() instanceof NodeOrderFieldSorter);
    }

    @Test
    public void retrieveFieldSorterNoEditorNode() throws Exception {
        final Node root = createMock(Node.class);

        expect(root.hasNode(NamespaceUtils.EDITOR_CONFIG_PATH)).andReturn(false);
        replayAll();

        assertFalse(NamespaceUtils.retrieveFieldSorter(root).isPresent());
    }

    @Test
    public void retrieveFieldSorterRepositoryException() throws Exception {
        final Node root = createMock(Node.class);

        expect(root.hasNode(NamespaceUtils.EDITOR_CONFIG_PATH)).andThrow(new RepositoryException());
        expect(JcrUtils.getNodePathQuietly(root)).andReturn("/bla");
        replayAll(root);

        assertFalse(NamespaceUtils.retrieveFieldSorter(root).isPresent());
    }
}
