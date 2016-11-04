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

package org.onehippo.cms.channelmanager.content.documenttype.field.sort;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.sort.FieldSorter;
import org.onehippo.cms.channelmanager.content.documenttype.field.sort.TwoColumnFieldSorter;
import org.onehippo.cms.channelmanager.content.documenttype.util.NamespaceUtils;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class TwoColumnFieldSorterTest {
    private final FieldSorter sorter = new TwoColumnFieldSorter();

    @Test
    public void sortFields() throws Exception {
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final ContentType type = createMock(ContentType.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);

        final Node root = MockNode.root();
        final Node contentTypeRoot = root.addNode("bla", "bla");
        final Node nodeType = contentTypeRoot.addNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE, "bla")
                                             .addNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE, "bla");
        nodeType.addNode("has-node-type-field", "bla").setProperty(HippoNodeType.HIPPO_PATH, "path");

        final Node editorConfig = contentTypeRoot.addNode("editor:templates", "bla")
                                                 .addNode("_default_", "bla");
        editorConfig.addNode("no-field", "bla");
        final Node field1Right = editorConfig.addNode("field1-right", "bla");
        field1Right.setProperty("field", "has-node-type-field");
        field1Right.setProperty("wicket.id", "test.right.item");
        editorConfig.addNode("unplaced-field", "bla").setProperty("field", "has-node-type-field");
        editorConfig.addNode("also-no-field", "bla").setProperty("field", "no-node-type-field");
        final Node field2Right = editorConfig.addNode("field2-right", "bla");
        field2Right.setProperty("field", "has-node-type-field");
        field2Right.setProperty("wicket.id", "test.right.item");
        final Node misplacedField = editorConfig.addNode("misplaced-field", "bla");
        misplacedField.setProperty("field", "has-node-type-field");
        misplacedField.setProperty("wicket.id", "abra.cadabra");
        final Node field1Left = editorConfig.addNode("field1-left", "bla");
        field1Left.setProperty("field", "has-node-type-field");
        field1Left.setProperty("wicket.id", "test.left.item");
        final Node field2Left = editorConfig.addNode("field2-left", "bla");
        field2Left.setProperty("field", "has-node-type-field");
        field2Left.setProperty("wicket.id", "test.left.item");
        final Node field3Right = editorConfig.addNode("field3-right", "bla");
        field3Right.setProperty("field", "has-node-type-field");
        field3Right.setProperty("wicket.id", "test.right.item");

        expect(context.getContentTypeRoot()).andReturn(contentTypeRoot);
        expect(context.getContentType()).andReturn(type).anyTimes();
        expect(type.getItem(anyObject())).andReturn(item).anyTimes();
        replay(context, type);

        final List<FieldTypeContext> fields = sorter.sortFields(context);

        assertThat(fields.size(), equalTo(5));
        assertThat(fields.get(0).getEditorConfigNode().getName(), equalTo("field1-left"));
        assertThat(fields.get(1).getEditorConfigNode().getName(), equalTo("field2-left"));
        assertThat(fields.get(2).getEditorConfigNode().getName(), equalTo("field1-right"));
        assertThat(fields.get(3).getEditorConfigNode().getName(), equalTo("field2-right"));
        assertThat(fields.get(4).getEditorConfigNode().getName(), equalTo("field3-right"));
    }

    @Test
    public void sortFieldsWithGlobalException() throws Exception {
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final ContentType type = createMock(ContentType.class);
        final Node contentTypeRoot = createMock(Node.class);

        expect(context.getContentType()).andReturn(type);
        expect(type.getName()).andReturn("bla");
        expect(context.getContentTypeRoot()).andReturn(contentTypeRoot);
        expect(contentTypeRoot.getNode(NamespaceUtils.EDITOR_CONFIG_PATH)).andThrow(new RepositoryException());
        replay(context, type, contentTypeRoot);

        final List<FieldTypeContext> fields = sorter.sortFields(context);

        assertThat(fields.size(), equalTo(0));
    }

    /*
       public List<FieldTypeContext> sortFields(final ContentTypeContext context) {
        final List<FieldTypeContext> sortedFields = new ArrayList<>();
        final Node rootNode = context.getContentTypeRoot();

        try {
            final Node editorConfigNode = rootNode.getNode(NamespaceUtils.EDITOR_CONFIG_PATH);
            final Node nodeTypeNode = rootNode.getNode(NamespaceUtils.NODE_TYPE_PATH);

            for (final Node editorFieldNode : new NodeIterable(editorConfigNode.getNodes())) {
                createFieldContextForWicketIdSuffix(".left.item", editorFieldNode, nodeTypeNode, context)
                        .ifPresent(sortedFields::add);
            }

            for (final Node editorFieldNode : new NodeIterable(editorConfigNode.getNodes())) {
                createFieldContextForWicketIdSuffix(".right.item", editorFieldNode, nodeTypeNode, context)
                        .ifPresent(sortedFields::add);
            }
        } catch (RepositoryException e) {
            log.warn("Failed to sort fields of content type {}", context.getContentType().getName(), e);
        }

     */
}
