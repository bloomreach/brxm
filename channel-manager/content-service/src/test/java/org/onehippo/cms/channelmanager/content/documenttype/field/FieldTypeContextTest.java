/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;
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
import org.onehippo.cms.services.validation.legacy.LegacyValidatorMapper;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertFalse;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStaticPartial;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest({ContentTypeContext.class, NamespaceUtils.class, LegacyValidatorMapper.class})
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

        replayAll();

        assertFalse(FieldTypeContext.create(editorFieldConfigNode, context).isPresent());

        verifyAll();
    }

    @Test
    public void createNoTypes() {
        final Node editorFieldConfigNode = createMock(Node.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);

        expect(NamespaceUtils.getFieldProperty(editorFieldConfigNode)).andReturn(Optional.of("fieldName"));

        expect(context.getFieldScanningContexts()).andReturn(Collections.emptyList());

        replayAll();

        assertFalse(FieldTypeContext.create(editorFieldConfigNode, context).isPresent());

        verifyAll();
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

        replayAll();

        assertFalse(FieldTypeContext.create(editorFieldConfigNode, context).isPresent());

        verifyAll();
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

        replayAll();

        assertFalse(FieldTypeContext.create(editorFieldConfigNode, context).isPresent());

        verifyAll();
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
        expect(NamespaceUtils.getPathForNodeTypeField(nodeTypeNode, "fieldName")).andReturn(
                Optional.of("myproject:date"));

        expect(context.getFieldScanningContexts()).andReturn(Collections.singletonList(type));
        expect(contentType.getItem("myproject:date")).andReturn(contentTypeItem);

        expect(contentTypeItem.getName()).andReturn("myproject:date");
        expect(contentTypeItem.getEffectiveType()).andReturn("Date").times(2);
        expect(contentTypeItem.getItemType()).andReturn("CalendarDate");
        expect(contentTypeItem.isProperty()).andReturn(true);
        expect(contentTypeItem.isMultiple()).andReturn(false);
        expect(contentTypeItem.getValidators()).andReturn(Collections.emptyList());

        expect(ContentTypeContext.getContentType("Date")).andReturn(Optional.empty());

        replayAll();

        final FieldTypeContext ftc = FieldTypeContext.create(editorFieldConfigNode, context).get();
        assertThat(ftc.getJcrName(), equalTo("myproject:date"));
        assertThat(ftc.getJcrType(), equalTo("Date"));
        assertThat(ftc.getType(), equalTo("CalendarDate"));
        assertThat(ftc.isProperty(), equalTo(true));
        assertThat(ftc.isMultiple(), equalTo(false));
        assertThat(ftc.getValidators(), equalTo(Collections.emptyList()));
        assertThat(ftc.getEditorConfigNode(), equalTo(Optional.of(editorFieldConfigNode)));
        assertThat(ftc.getParentContext(), equalTo(context));

        verifyAll();
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
        expect(NamespaceUtils.getPathForNodeTypeField(nodeTypeNode2, "fieldName")).andReturn(
                Optional.of("myproject:date"));

        expect(context.getFieldScanningContexts()).andReturn(Arrays.asList(type1, type2));
        expect(contentType2.getItem("myproject:date")).andReturn(contentTypeItem);

        expect(contentTypeItem.getName()).andReturn("myproject:date");
        expect(contentTypeItem.getEffectiveType()).andReturn("Date").times(2);
        expect(contentTypeItem.getItemType()).andReturn("CalendarDate");
        expect(contentTypeItem.isProperty()).andReturn(true);
        expect(contentTypeItem.isMultiple()).andReturn(false);
        expect(contentTypeItem.getValidators()).andReturn(Collections.emptyList());

        expect(ContentTypeContext.getContentType("Date")).andReturn(Optional.empty());

        replayAll();

        final FieldTypeContext ftc = FieldTypeContext.create(editorFieldConfigNode, context).get();
        assertThat(ftc.getJcrName(), equalTo("myproject:date"));
        assertThat(ftc.getJcrType(), equalTo("Date"));
        assertThat(ftc.getType(), equalTo("CalendarDate"));
        assertThat(ftc.isProperty(), equalTo(true));
        assertThat(ftc.isMultiple(), equalTo(false));
        assertThat(ftc.getValidators(), equalTo(Collections.emptyList()));
        assertThat(ftc.getEditorConfigNode(), equalTo(Optional.of(editorFieldConfigNode)));
        assertThat(ftc.getParentContext(), equalTo(context));

        verifyAll();
    }

    @Test
    public void createContextForLegacyValidators() {
        final List<String> legacyValidators = Arrays.asList("required", "non-empty");
        final List<String> newValidators = Collections.singletonList("required");

        mockStaticPartial(LegacyValidatorMapper.class, "legacyMapper", List.class, String.class);
        expect(LegacyValidatorMapper.legacyMapper(eq(legacyValidators), eq("Html"))).andReturn(newValidators);
        replayAll();

        final FieldTypeContext requiredString = new FieldTypeContext("myproject:description", "String", "Html",
                true, false, legacyValidators, null, null);
        assertThat(requiredString.getValidators(), equalTo(newValidators));
        verifyAll();
    }

    @Test
    public void createContextWithCompoundTypeValidators() {
        final ContentTypeItem contentTypeItem = createMock(ContentTypeItem.class);
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final ContentType contentType = createMock(ContentType.class);

        expect(contentTypeItem.getName()).andReturn("compound-field");
        expect(contentTypeItem.getEffectiveType()).andReturn("effective-type").times(2);
        expect(contentTypeItem.getItemType()).andReturn("item-type");
        expect(contentTypeItem.isProperty()).andReturn(true);
        expect(contentTypeItem.isMultiple()).andReturn(false);
        expect(contentTypeItem.getValidators()).andReturn(Collections.singletonList("content-type-item-validator"));

        expect(ContentTypeContext.getContentType("effective-type")).andReturn(Optional.of(contentType));

        expect(contentType.isCompoundType()).andReturn(true);
        expect(contentType.getValidators()).andReturn(Collections.singletonList("content-type-validator"));

        replayAll();

        final FieldTypeContext fieldTypeContext = new FieldTypeContext(contentTypeItem, parentContext);

        assertThat(fieldTypeContext.getValidators(), contains("content-type-validator", "content-type-item-validator"));
        verifyAll();
    }

    @Test
    public void createContextWithValidators() {
        final ContentTypeItem contentTypeItem = createMock(ContentTypeItem.class);
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final ContentType contentType = createMock(ContentType.class);

        expect(contentTypeItem.getName()).andReturn("compound-field");
        expect(contentTypeItem.getEffectiveType()).andReturn("effective-type").times(2);
        expect(contentTypeItem.getItemType()).andReturn("item-type");
        expect(contentTypeItem.isProperty()).andReturn(true);
        expect(contentTypeItem.isMultiple()).andReturn(false);
        expect(contentTypeItem.getValidators()).andReturn(Collections.singletonList("content-type-item-validator"));

        expect(ContentTypeContext.getContentType("effective-type")).andReturn(Optional.of(contentType));

        expect(contentType.isCompoundType()).andReturn(false);

        replayAll();

        final FieldTypeContext fieldTypeContext = new FieldTypeContext(contentTypeItem, parentContext);

        assertThat(fieldTypeContext.getValidators(), contains("content-type-item-validator"));
        verifyAll();
    }

    @Test
    public void instantiateWithoutNode() {
        final ContentTypeItem contentTypeItem = createMock(ContentTypeItem.class);
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);

        expect(contentTypeItem.getName()).andReturn("myproject:date");
        expect(contentTypeItem.getEffectiveType()).andReturn("Date").times(2);
        expect(contentTypeItem.getItemType()).andReturn("CalendarDate");
        expect(contentTypeItem.isProperty()).andReturn(true);
        expect(contentTypeItem.isMultiple()).andReturn(false);
        expect(contentTypeItem.getValidators()).andReturn(Collections.emptyList());

        expect(ContentTypeContext.getContentType("Date")).andReturn(Optional.empty());

        replayAll();

        final FieldTypeContext context = new FieldTypeContext(contentTypeItem, parentContext);

        assertThat(context.getJcrName(), equalTo("myproject:date"));
        assertThat(context.getJcrType(), equalTo("Date"));
        assertThat(context.getType(), equalTo("CalendarDate"));
        assertThat(context.isProperty(), equalTo(true));
        assertThat(context.isMultiple(), equalTo(false));
        assertThat(context.getValidators(), equalTo(Collections.emptyList()));
        assertFalse(context.getEditorConfigNode().isPresent());
        assertThat(context.getParentContext(), equalTo(parentContext));
    }

    @Test
    public void createContextForCompound() {
        final ContentTypeItem contentTypeItem = createMock(ContentTypeItem.class);
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final ContentTypeContext childContext = createMock(ContentTypeContext.class);

        expect(contentTypeItem.getName()).andReturn("itemName");
        expect(contentTypeItem.getEffectiveType()).andReturn("id").times(2);
        expect(contentTypeItem.getItemType()).andReturn("id");
        expect(contentTypeItem.isProperty()).andReturn(true);
        expect(contentTypeItem.isMultiple()).andReturn(false);
        expect(contentTypeItem.getValidators()).andReturn(Collections.emptyList());

        expect(ContentTypeContext.createFromParent("id", parentContext)).andReturn(Optional.of(childContext));
        expect(ContentTypeContext.getContentType("id")).andReturn(Optional.empty());

        replayAll();

        final FieldTypeContext context = new FieldTypeContext(contentTypeItem, parentContext);
        assertThat(context.createContextForCompound().get(), equalTo(childContext));

        verifyAll();
    }

    @Test
    public void getBooleanConfig() {
        final FieldTypeContext context = new FieldTypeContext(null, null, null, false, false, null, null, null);
        final Optional<Boolean> value = Optional.of(true);

        expect(NamespaceUtils.getConfigProperty(context, "test", JcrBooleanReader.get())).andReturn(value);

        replayAll();

        assertThat(context.getBooleanConfig("test"), equalTo(value));
    }

    @Test
    public void getStringConfig() {
        final FieldTypeContext context = new FieldTypeContext(null, null, null, false, false, null, null, null);
        final Optional<String> value = Optional.of("value");

        expect(NamespaceUtils.getConfigProperty(context, "test", JcrStringReader.get())).andReturn(value);

        replayAll();

        assertThat(context.getStringConfig("test"), equalTo(value));
    }

    @Test
    public void getMultipleStringConfig() {
        final FieldTypeContext context = new FieldTypeContext(null, null, null, false, false, null, null, null);
        final Optional<String[]> values = Optional.of(new String[]{"a", "b"});

        expect(NamespaceUtils.getConfigProperty(context, "test", JcrMultipleStringReader.get())).andReturn(values);

        replayAll();

        assertThat(context.getMultipleStringConfig("test"), equalTo(values));
    }
}
