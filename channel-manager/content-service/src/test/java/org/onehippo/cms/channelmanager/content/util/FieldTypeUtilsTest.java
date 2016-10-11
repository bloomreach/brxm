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

package org.onehippo.cms.channelmanager.content.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.jcr.Node;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.model.documenttype.DocumentType;
import org.onehippo.cms.channelmanager.content.model.documenttype.FieldType;
import org.onehippo.cms.channelmanager.content.model.documenttype.MultilineStringFieldType;
import org.onehippo.cms.channelmanager.content.model.documenttype.StringFieldType;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(PowerMockRunner.class)
@PrepareForTest(NamespaceUtils.class)
public class FieldTypeUtilsTest {
    private static final String PROPERTY_FIELD_PLUGIN = "org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin";

    @Test
    public void validateSupportedFieldTypes() {
        final FieldTypeContext context = createMock(FieldTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);

        expect(context.getContentTypeItem()).andReturn(item).anyTimes();
        expect(item.getItemType()).andReturn("String");
        replay(item, context);
        assertThat(FieldTypeUtils.isSupportedFieldType(context), equalTo(true));
        reset(item);

        expect(item.getItemType()).andReturn("Text");
        replay(item);
        assertThat(FieldTypeUtils.isSupportedFieldType(context), equalTo(true));
        reset(item);

        expect(item.getItemType()).andReturn("Html");
        replay(item);
        assertThat(FieldTypeUtils.isSupportedFieldType(context), equalTo(false));
        reset(item);

        expect(item.getItemType()).andReturn(null);
        replay(item);
        assertThat(FieldTypeUtils.isSupportedFieldType(context), equalTo(false));
    }

    @Test
    public void validatePluginClassValidation() {
        final FieldTypeContext context = createMock(FieldTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);
        final Node editorFieldNode = createMock(Node.class);
        PowerMock.mockStatic(NamespaceUtils.class);

        expect(context.getContentTypeItem()).andReturn(item).anyTimes();
        expect(context.getEditorConfigNode()).andReturn(editorFieldNode).anyTimes();
        expect(item.getName()).andReturn("dummy").anyTimes();
        expect(item.getItemType()).andReturn("String").anyTimes();
        replay(context, item);

        // deals with empty value
        expect(NamespaceUtils.getPluginClassForField(editorFieldNode)).andReturn(Optional.empty());
        PowerMock.replayAll();
        assertThat(FieldTypeUtils.usesDefaultFieldPlugin(context), equalTo(false));
        PowerMock.verifyAll();

        // rejects dummy text
        PowerMock.resetAll();
        expect(NamespaceUtils.getPluginClassForField(editorFieldNode)).andReturn(Optional.of("sometext"));
        PowerMock.replayAll();
        assertThat(FieldTypeUtils.usesDefaultFieldPlugin(context), equalTo(false));

        // accept correct value for both String and Text
        reset(item);
        expect(item.getName()).andReturn("dummy").anyTimes();
        expect(item.getItemType()).andReturn("String");
        expect(item.getItemType()).andReturn("Text");
        replay(item);

        PowerMock.resetAll();
        expect(NamespaceUtils.getPluginClassForField(editorFieldNode)).andReturn(Optional.of(PROPERTY_FIELD_PLUGIN)).anyTimes();
        PowerMock.replayAll();

        assertThat(FieldTypeUtils.usesDefaultFieldPlugin(context), equalTo(true));
        assertThat(FieldTypeUtils.usesDefaultFieldPlugin(context), equalTo(true));
    }

    @Test
    public void validateDeriveFieldType() {
        final ContentTypeProperty property = createMock(ContentTypeProperty.class);

        expect(property.getItemType()).andReturn("String");
        expect(property.getItemType()).andReturn("Text");
        replay(property);

        assertThat(FieldTypeUtils.createFieldType(property).get().getClass(), equalTo(StringFieldType.class));
        assertThat(FieldTypeUtils.createFieldType(property).get().getClass(), equalTo(MultilineStringFieldType.class));
    }

    @Test(expected = NoSuchElementException.class)
    public void validateDeriveFieldTypeOfUnknownType() {
        final ContentTypeProperty property = createMock(ContentTypeProperty.class);

        expect(property.getItemType()).andReturn("Html");
        replay(property);

        FieldTypeUtils.createFieldType(property).get();
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
