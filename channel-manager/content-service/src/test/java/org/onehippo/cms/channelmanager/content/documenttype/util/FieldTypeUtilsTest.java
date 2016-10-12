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

import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.jcr.Node;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeFactory;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.CompoundFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.MultilineStringFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.StringFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.util.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.util.NamespaceUtils;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NamespaceUtils.class, FieldTypeFactory.class})
public class FieldTypeUtilsTest {
    private static final String PROPERTY_FIELD_PLUGIN = "org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin";

    @Test
    public void isSupportedFieldTypeString() {
        final FieldTypeContext context = createMock(FieldTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);

        expect(context.getContentTypeItem()).andReturn(item);
        expect(item.isProperty()).andReturn(true);
        expect(item.getItemType()).andReturn("String");
        replay(item, context);

        assertThat(FieldTypeUtils.isSupportedFieldType(context), equalTo(true));
    }

    @Test
    public void isSupportedFieldTypeText() {
        final FieldTypeContext context = createMock(FieldTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);

        expect(context.getContentTypeItem()).andReturn(item);
        expect(item.isProperty()).andReturn(true);
        expect(item.getItemType()).andReturn("Text");
        replay(item, context);

        assertThat(FieldTypeUtils.isSupportedFieldType(context), equalTo(true));
    }

    @Test
    public void isSupportedFieldTypeHtml() {
        final FieldTypeContext context = createMock(FieldTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);

        expect(context.getContentTypeItem()).andReturn(item);
        expect(item.isProperty()).andReturn(true);
        expect(item.getItemType()).andReturn("Html");
        replay(item, context);

        assertThat(FieldTypeUtils.isSupportedFieldType(context), equalTo(false));
    }

    @Test
    public void isSupportedFieldTypeCompound() {
        final FieldTypeContext context = createMock(FieldTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);

        expect(context.getContentTypeItem()).andReturn(item);
        expect(item.isProperty()).andReturn(false);
        replay(item, context);

        assertThat(FieldTypeUtils.isSupportedFieldType(context), equalTo(true));
    }

    @Test
    public void usesDefaultFieldPluginNoDescriptor() {
        final FieldTypeContext context = createMock(FieldTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);
        final Node node = createMock(Node.class);

        PowerMock.mockStaticPartial(NamespaceUtils.class, "getPluginClassForField");

        expect(context.getContentTypeItem()).andReturn(item);
        expect(context.getEditorConfigNode()).andReturn(node);
        expect(item.isProperty()).andReturn(true);
        expect(item.getItemType()).andReturn("Unknown");
        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.empty());
        replay(item, context);
        PowerMock.replayAll();

        assertThat(FieldTypeUtils.usesDefaultFieldPlugin(context), equalTo(false));
    }

    @Test
    public void usesDefaultFieldPluginNoPluginClass() {
        final FieldTypeContext context = createMock(FieldTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);
        final Node node = createMock(Node.class);

        PowerMock.mockStaticPartial(NamespaceUtils.class, "getPluginClassForField");

        expect(context.getContentTypeItem()).andReturn(item);
        expect(context.getEditorConfigNode()).andReturn(node);
        expect(item.isProperty()).andReturn(true);
        expect(item.getItemType()).andReturn("String");
        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.empty());
        replay(item, context);
        PowerMock.replayAll();

        assertThat(FieldTypeUtils.usesDefaultFieldPlugin(context), equalTo(false));
    }

    @Test
    public void usesDefaultFieldPlugin() {
        final FieldTypeContext context = createMock(FieldTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);
        final Node node = createMock(Node.class);

        PowerMock.mockStaticPartial(NamespaceUtils.class, "getPluginClassForField");

        expect(context.getContentTypeItem()).andReturn(item);
        expect(context.getEditorConfigNode()).andReturn(node);
        expect(item.isProperty()).andReturn(true);
        expect(item.getItemType()).andReturn("String");
        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.of(PROPERTY_FIELD_PLUGIN));
        replay(item, context);
        PowerMock.replayAll();

        assertThat(FieldTypeUtils.usesDefaultFieldPlugin(context), equalTo(true));
    }

    @Test
    public void createAndInitFieldTypeString() {
        final FieldTypeContext context = createMock(FieldTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);
        final StringFieldType fieldType = createMock(StringFieldType.class);
        final ContentTypeContext contentTypeContext = createMock(ContentTypeContext.class);
        final DocumentType docType = createMock(DocumentType.class);

        PowerMock.mockStaticPartial(FieldTypeFactory.class, "createFieldType");

        expect(context.getContentTypeItem()).andReturn(item);
        expect(item.isProperty()).andReturn(true);
        expect(item.getItemType()).andReturn("String");
        expect(fieldType.init(context, contentTypeContext, docType)).andReturn(Optional.of(fieldType));
        expect(FieldTypeFactory.createFieldType(StringFieldType.class)).andReturn(Optional.of(fieldType));
        replay(context, item, fieldType);
        PowerMock.replayAll();

        assertThat(FieldTypeUtils.createAndInitFieldType(context, contentTypeContext, docType).get(), equalTo(fieldType));
    }

    @Test
    public void createAndInitFieldTypeMultilineString() {
        final FieldTypeContext context = createMock(FieldTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);
        final MultilineStringFieldType fieldType = createMock(MultilineStringFieldType.class);

        PowerMock.mockStaticPartial(FieldTypeFactory.class, "createFieldType");

        expect(context.getContentTypeItem()).andReturn(item);
        expect(item.isProperty()).andReturn(true);
        expect(item.getItemType()).andReturn("Text");
        expect(fieldType.init(context, null, null)).andReturn(Optional.of(fieldType));
        expect(FieldTypeFactory.createFieldType(MultilineStringFieldType.class)).andReturn(Optional.of(fieldType));
        replay(context, item, fieldType);
        PowerMock.replayAll();

        assertThat(FieldTypeUtils.createAndInitFieldType(context, null, null).get(), equalTo(fieldType));
    }

    @Test(expected = NoSuchElementException.class)
    public void createAndInitFieldTypeCompoundUnsupportedPropertyType() {
        final FieldTypeContext context = createMock(FieldTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);

        expect(context.getContentTypeItem()).andReturn(item);
        expect(item.isProperty()).andReturn(true);
        expect(item.getItemType()).andReturn("Unknown");
        replay(context, item);

        FieldTypeUtils.createAndInitFieldType(context, null, null).get();
    }

    @Test
    public void createAndInitFieldTypeCompound() {
        final FieldTypeContext context = createMock(FieldTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);
        final CompoundFieldType fieldType = createMock(CompoundFieldType.class);

        PowerMock.mockStaticPartial(FieldTypeFactory.class, "createFieldType");

        expect(context.getContentTypeItem()).andReturn(item);
        expect(item.isProperty()).andReturn(false);
        expect(item.getName()).andReturn("my:compound");
        expect(FieldTypeFactory.createFieldType(CompoundFieldType.class)).andReturn(Optional.of(fieldType));
        expect(fieldType.init(context, null, null)).andReturn(Optional.of(fieldType));
        replay(context, item, fieldType);
        PowerMock.replayAll();

        assertThat(FieldTypeUtils.createAndInitFieldType(context, null, null).get(), equalTo(fieldType));
    }

    @Test
    public void validateIgnoredValidator() {
        final FieldType fieldType = createMock(FieldType.class);
        final DocumentType docType = createMock(DocumentType.class);
        replay(fieldType);

        FieldTypeUtils.determineValidators(fieldType, docType, Collections.singletonList("optional"));
    }

    @Test
    public void validateMappedValidators() {
        final FieldType fieldType = createMock(FieldType.class);
        final DocumentType docType = createMock(DocumentType.class);

        fieldType.addValidator(FieldType.Validator.REQUIRED);
        expectLastCall();
        fieldType.addValidator(FieldType.Validator.REQUIRED);
        expectLastCall();
        replay(fieldType);

        FieldTypeUtils.determineValidators(fieldType, docType, Arrays.asList("required", "non-empty"));
    }

    @Test
    public void validateFieldValidators() {
        final FieldType fieldType = createMock(FieldType.class);
        final DocumentType docType = createMock(DocumentType.class);

        fieldType.addValidator(FieldType.Validator.UNSUPPORTED);
        expectLastCall();
        fieldType.addValidator(FieldType.Validator.UNSUPPORTED);
        expectLastCall();
        replay(fieldType);

        FieldTypeUtils.determineValidators(fieldType, docType, Arrays.asList("email", "references"));
    }

    @Test
    public void validateUnknownValidators() {
        final FieldType fieldType = createMock(FieldType.class);
        final DocumentType docType = createMock(DocumentType.class);

        docType.setReadOnlyDueToUnknownValidator(true);
        expectLastCall();
        replay(fieldType, docType);

        FieldTypeUtils.determineValidators(fieldType, docType, Collections.singletonList("unknown-validator"));
    }
}
