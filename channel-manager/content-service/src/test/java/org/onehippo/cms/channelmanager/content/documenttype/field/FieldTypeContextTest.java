/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.documenttype.field;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import javax.jcr.Node;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.FieldScanningContext;
import org.onehippo.cms.channelmanager.content.documenttype.util.JcrBooleanReader;
import org.onehippo.cms.channelmanager.content.documenttype.util.JcrMultipleStringReader;
import org.onehippo.cms.channelmanager.content.documenttype.util.JcrStringReader;
import org.onehippo.cms.channelmanager.content.documenttype.util.NamespaceUtils;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({ContentTypeContext.class, NamespaceUtils.class})
public class FieldTypeContextTest {

    @Before
    public void setup() {
        PowerMock.mockStatic(ContentTypeContext.class);
        PowerMock.mockStatic(NamespaceUtils.class);
    }

    @Test
    public void createNoFieldProperty() {
        final Node editorFieldConfigNode = createMock(Node.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);

        expect(NamespaceUtils.getFieldProperty(editorFieldConfigNode)).andReturn(Optional.empty());

        PowerMock.replayAll();

        assertFalse(FieldTypeContext.create(editorFieldConfigNode, context).isPresent());

        PowerMock.verifyAll();
    }

    @Test
    public void createNoTypes() {
        final Node editorFieldConfigNode = createMock(Node.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);

        expect(NamespaceUtils.getFieldProperty(editorFieldConfigNode)).andReturn(Optional.of("fieldName"));

        expect(context.getFieldScanningContexts()).andReturn(Collections.emptyList());

        PowerMock.replayAll();
        replay(context);

        assertFalse(FieldTypeContext.create(editorFieldConfigNode, context).isPresent());

        verify(context);
        PowerMock.verifyAll();
    }

    @Test
    public void createNoPath() {
        final Node editorFieldConfigNode = createMock(Node.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final ContentType contentType = createMock(ContentType.class);
        final Node nodeTypeNode = createMock(Node.class);
        final FieldScanningContext type = new FieldScanningContext(contentType, nodeTypeNode);

        expect(NamespaceUtils.getFieldProperty(editorFieldConfigNode)).andReturn(Optional.of("fieldName"));
        expect(NamespaceUtils.getPathForNodeTypeField(nodeTypeNode, "fieldName")).andReturn(Optional.empty());

        expect(context.getFieldScanningContexts()).andReturn(Collections.singletonList(type));

        PowerMock.replayAll();
        replay(context);

        assertFalse(FieldTypeContext.create(editorFieldConfigNode, context).isPresent());

        verify(context);
        PowerMock.verifyAll();
    }

    @Test
    public void createNoItem() {
        final Node editorFieldConfigNode = createMock(Node.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final ContentType contentType = createMock(ContentType.class);
        final Node nodeTypeNode = createMock(Node.class);
        final FieldScanningContext type = new FieldScanningContext(contentType, nodeTypeNode);

        expect(NamespaceUtils.getFieldProperty(editorFieldConfigNode)).andReturn(Optional.of("fieldName"));
        expect(NamespaceUtils.getPathForNodeTypeField(nodeTypeNode, "fieldName")).andReturn(Optional.of("itemName"));

        expect(context.getFieldScanningContexts()).andReturn(Collections.singletonList(type));
        expect(contentType.getItem("itemName")).andReturn(null);

        PowerMock.replayAll();
        replay(context, contentType);

        assertFalse(FieldTypeContext.create(editorFieldConfigNode, context).isPresent());

        verify(context, contentType);
        PowerMock.verifyAll();
    }

    @Test
    public void createContextFromPrimaryType() {
        final Node editorFieldConfigNode = createMock(Node.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final ContentType contentType = createMock(ContentType.class);
        final Node nodeTypeNode = createMock(Node.class);
        final FieldScanningContext type = new FieldScanningContext(contentType, nodeTypeNode);
        final ContentTypeItem contentTypeItem = createMock(ContentTypeItem.class);

        expect(NamespaceUtils.getFieldProperty(editorFieldConfigNode)).andReturn(Optional.of("fieldName"));
        expect(NamespaceUtils.getPathForNodeTypeField(nodeTypeNode, "fieldName")).andReturn(Optional.of("itemName"));

        expect(context.getFieldScanningContexts()).andReturn(Collections.singletonList(type));
        expect(contentType.getItem("itemName")).andReturn(contentTypeItem);

        PowerMock.replayAll();
        replay(context, contentType);

        final FieldTypeContext ftc = FieldTypeContext.create(editorFieldConfigNode, context).get();
        assertThat(ftc.getEditorConfigNode(), equalTo(Optional.of(editorFieldConfigNode)));
        assertThat(ftc.getParentContext(), equalTo(context));
        assertThat(ftc.getContentTypeItem(), equalTo(contentTypeItem));

        verify(context, contentType);
        PowerMock.verifyAll();
    }

    @Test
    public void createContextFromInheritedType() {
        final Node editorFieldConfigNode = createMock(Node.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final ContentType contentType1 = createMock(ContentType.class);
        final ContentType contentType2 = createMock(ContentType.class);
        final Node nodeTypeNode1 = createMock(Node.class);
        final Node nodeTypeNode2 = createMock(Node.class);
        final FieldScanningContext type1 = new FieldScanningContext(contentType1, nodeTypeNode1);
        final FieldScanningContext type2 = new FieldScanningContext(contentType2, nodeTypeNode2);
        final ContentTypeItem contentTypeItem = createMock(ContentTypeItem.class);

        expect(NamespaceUtils.getFieldProperty(editorFieldConfigNode)).andReturn(Optional.of("fieldName"));
        expect(NamespaceUtils.getPathForNodeTypeField(nodeTypeNode1, "fieldName")).andReturn(Optional.empty());
        expect(NamespaceUtils.getPathForNodeTypeField(nodeTypeNode2, "fieldName")).andReturn(Optional.of("itemName"));

        expect(context.getFieldScanningContexts()).andReturn(Arrays.asList(type1, type2));
        expect(contentType2.getItem("itemName")).andReturn(contentTypeItem);

        PowerMock.replayAll();
        replay(context, contentType2);

        final FieldTypeContext ftc = FieldTypeContext.create(editorFieldConfigNode, context).get();
        assertThat(ftc.getEditorConfigNode(), equalTo(Optional.of(editorFieldConfigNode)));
        assertThat(ftc.getParentContext(), equalTo(context));
        assertThat(ftc.getContentTypeItem(), equalTo(contentTypeItem));

        verify(context, contentType2);
        PowerMock.verifyAll();
    }

    @Test
    public void instantiateWithoutNode() {
        final ContentTypeItem contentTypeItem = createMock(ContentTypeItem.class);
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);

        final FieldTypeContext context = new FieldTypeContext(contentTypeItem, parentContext);

        assertFalse(context.getEditorConfigNode().isPresent());
        assertThat(context.getContentTypeItem(), equalTo(contentTypeItem));
        assertThat(context.getParentContext(), equalTo(parentContext));
    }

    @Test
    public void createContextForCompound() {
        final ContentTypeItem contentTypeItem = createMock(ContentTypeItem.class);
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final ContentTypeContext childContext = createMock(ContentTypeContext.class);
        final FieldTypeContext context = new FieldTypeContext(contentTypeItem, parentContext);

        expect(contentTypeItem.getItemType()).andReturn("id");
        expect(ContentTypeContext.createFromParent("id", parentContext)).andReturn(Optional.of(childContext));

        PowerMock.replayAll();
        replay(contentTypeItem);

        assertThat(context.createContextForCompound().get(), equalTo(childContext));

        verify(contentTypeItem);
        PowerMock.verifyAll();
    }

    @Test
    public void getBooleanConfig() {
        final FieldTypeContext context = new FieldTypeContext(null, null);
        final Optional<Boolean> value = Optional.of(true);

        expect(NamespaceUtils.getConfigProperty(context, "test", JcrBooleanReader.get())).andReturn(value);

        PowerMock.replayAll();

        assertThat(context.getBooleanConfig("test"), equalTo(value));
    }

    @Test
    public void getStringConfig() {
        final FieldTypeContext context = new FieldTypeContext(null, null);
        final Optional<String> value = Optional.of("value");

        expect(NamespaceUtils.getConfigProperty(context, "test", JcrStringReader.get())).andReturn(value);

        PowerMock.replayAll();

        assertThat(context.getStringConfig("test"), equalTo(value));
    }

    @Test
    public void getMultipleStringConfig() {
        final FieldTypeContext context = new FieldTypeContext(null, null);
        final Optional<String[]> values = Optional.of(new String[]{"a", "b"});

        expect(NamespaceUtils.getConfigProperty(context, "test", JcrMultipleStringReader.get())).andReturn(values);

        PowerMock.replayAll();

        assertThat(context.getMultipleStringConfig("test"), equalTo(values));
    }
}
