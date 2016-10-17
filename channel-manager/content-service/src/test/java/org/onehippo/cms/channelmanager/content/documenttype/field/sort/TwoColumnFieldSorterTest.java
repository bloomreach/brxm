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

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.sort.FieldSorter;
import org.onehippo.cms.channelmanager.content.documenttype.field.sort.TwoColumnFieldSorter;
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
        nodeType.addNode("field3-right", "bla").setProperty(HippoNodeType.HIPPO_PATH, "path");
        nodeType.addNode("field3-left", "bla").setProperty(HippoNodeType.HIPPO_PATH, "path");
        nodeType.addNode("field2-right", "bla").setProperty(HippoNodeType.HIPPO_PATH, "path");
        nodeType.addNode("field2-left", "bla").setProperty(HippoNodeType.HIPPO_PATH, "path");
        nodeType.addNode("field1-right", "bla").setProperty(HippoNodeType.HIPPO_PATH, "path");
        nodeType.addNode("field1-left", "bla").setProperty(HippoNodeType.HIPPO_PATH, "path");
        nodeType.addNode("unplaced-field", "bla").setProperty(HippoNodeType.HIPPO_PATH, "path");
        nodeType.addNode("misplaced-field", "bla").setProperty(HippoNodeType.HIPPO_PATH, "path");

        final Node editorConfig = contentTypeRoot.addNode("editor:templates", "bla")
                                                 .addNode("_default_", "bla");
        editorConfig.addNode("no-field", "bla");
        editorConfig.addNode("field1-right", "bla").setProperty("wicket.id", "test.right.item");
        editorConfig.addNode("unplaced-field", "bla");
        editorConfig.addNode("also-no-field", "bla");
        editorConfig.addNode("field2-right", "bla").setProperty("wicket.id", "test.right.item");
        editorConfig.addNode("misplaced-field", "bla").setProperty("wicket.id", "abra.cadabra");
        editorConfig.addNode("field1-left", "bla").setProperty("wicket.id", "test.left.item");
        editorConfig.addNode("field2-left", "bla").setProperty("wicket.id", "test.left.item");
        editorConfig.addNode("field3-right", "bla").setProperty("wicket.id", "test.right.item");

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
}
