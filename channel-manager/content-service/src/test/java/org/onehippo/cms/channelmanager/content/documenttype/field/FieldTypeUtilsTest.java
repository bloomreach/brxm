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

package org.onehippo.cms.channelmanager.content.documenttype.field;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.sort.FieldSorter;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.ChoiceFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.ChoiceFieldUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.CompoundFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.MultilineStringFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.StringFieldType;
import org.onehippo.cms.channelmanager.content.documenttype.util.NamespaceUtils;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NamespaceUtils.class, FieldTypeFactory.class, ChoiceFieldUtils.class})
public class FieldTypeUtilsTest {
    private static final String PROPERTY_FIELD_PLUGIN = "org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin";
    private static final String COMPOUND_FIELD_PLUGIN = "org.hippoecm.frontend.editor.plugins.field.NodeFieldPlugin";
    private static final String CHOICE_FIELD_PLUGIN = "org.onehippo.forge.contentblocks.ContentBlocksFieldPlugin";

    @Before
    public void setup() {
        PowerMock.mockStatic(ChoiceFieldUtils.class);
        PowerMock.mockStatic(FieldTypeFactory.class);
        PowerMock.mockStatic(NamespaceUtils.class);
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

    @Test
    public void populateFieldsNoSorter() {
        final List<FieldType> fields = new ArrayList<>();
        final ContentTypeContext context = createMock(ContentTypeContext.class);

        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.empty());
        expect(context.getContentTypeRoot()).andReturn(null);
        replay(context);
        PowerMock.replayAll();

        FieldTypeUtils.populateFields(fields, context);

        assertThat(fields.size(), equalTo(0));
        verify(context);
        PowerMock.verifyAll();
    }

    @Test
    public void populateFieldsSorterNoFields() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);

        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.emptyList());
        expect(context.getContentTypeRoot()).andReturn(null);
        replay(sorter, context);
        PowerMock.replayAll();

        FieldTypeUtils.populateFields(fields, context);

        assertThat(fields.size(), equalTo(0));
        verify(sorter, context);
        PowerMock.verifyAll();
    }

    @Test
    public void populateFieldsUnsupportedFieldTypeUnknownProperty() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.singletonList(fieldContext));
        expect(fieldContext.getContentTypeItem()).andReturn(item);
        expect(item.isProperty()).andReturn(true);
        expect(item.getItemType()).andReturn("unknown");
        replay(sorter, context, fieldContext, item);
        PowerMock.replayAll();

        FieldTypeUtils.populateFields(fields, context);

        assertThat(fields.size(), equalTo(0));
        verify(sorter, context, fieldContext, item);
        PowerMock.verifyAll();
    }

    @Test
    public void populateFieldsUnsupportedFieldTypePropertyNoEditorConfigNode() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.singletonList(fieldContext));
        expect(fieldContext.getContentTypeItem()).andReturn(item);
        expect(item.isProperty()).andReturn(true);
        expect(item.getItemType()).andReturn("String");
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.empty());
        replay(sorter, context, fieldContext, item);
        PowerMock.replayAll();

        FieldTypeUtils.populateFields(fields, context);

        assertThat(fields.size(), equalTo(0));
        verify(sorter, context, fieldContext, item);
        PowerMock.verifyAll();
    }

    @Test
    public void populateFieldsUnsupportedFieldTypePropertyWithoutPlugin() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);
        final Node node = createMock(Node.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.singletonList(fieldContext));
        expect(fieldContext.getContentTypeItem()).andReturn(item);
        expect(item.isProperty()).andReturn(true);
        expect(item.getItemType()).andReturn("String");
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.empty());
        replay(sorter, context, fieldContext, item);
        PowerMock.replayAll();

        FieldTypeUtils.populateFields(fields, context);

        assertThat(fields.size(), equalTo(0));
        verify(sorter, context, fieldContext, item);
        PowerMock.verifyAll();
    }

    @Test
    public void populateFieldsUnsupportedFieldTypePropertyWithCustomPlugin() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);
        final Node node = createMock(Node.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.singletonList(fieldContext));
        expect(fieldContext.getContentTypeItem()).andReturn(item);
        expect(item.isProperty()).andReturn(true);
        expect(item.getItemType()).andReturn("String");
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.of("Custom plugin"));
        replay(sorter, context, fieldContext, item);
        PowerMock.replayAll();

        FieldTypeUtils.populateFields(fields, context);

        assertThat(fields.size(), equalTo(0));
        verify(sorter, context, fieldContext, item);
        PowerMock.verifyAll();
    }

    @Test
    public void populateFieldsStringFieldInstantiationFailure() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);
        final Node node = createMock(Node.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.singletonList(fieldContext));
        expect(fieldContext.getContentTypeItem()).andReturn(item);
        expect(item.isProperty()).andReturn(true);
        expect(item.getItemType()).andReturn("String");
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.of(PROPERTY_FIELD_PLUGIN));
        expect(FieldTypeFactory.createFieldType(StringFieldType.class)).andReturn(Optional.empty());
        replay(sorter, context, fieldContext, item);
        PowerMock.replayAll();

        FieldTypeUtils.populateFields(fields, context);

        assertThat(fields.size(), equalTo(0));
        verify(sorter, context, fieldContext, item);
        PowerMock.verifyAll();
    }

    @Test
    public void populateFieldsStringFieldInitFailure() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);
        final Node node = createMock(Node.class);
        final StringFieldType fieldType = createMock(StringFieldType.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.singletonList(fieldContext));
        expect(fieldContext.getContentTypeItem()).andReturn(item);
        expect(item.isProperty()).andReturn(true);
        expect(item.getItemType()).andReturn("String");
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.of(PROPERTY_FIELD_PLUGIN));
        expect(FieldTypeFactory.createFieldType(StringFieldType.class)).andReturn(Optional.of(fieldType));
        fieldType.init(fieldContext);
        expectLastCall();
        expect(fieldType.isValid()).andReturn(false);
        replay(sorter, context, fieldContext, item, fieldType);
        PowerMock.replayAll();

        FieldTypeUtils.populateFields(fields, context);

        assertThat(fields.size(), equalTo(0));
        verify(sorter, context, fieldContext, item, fieldType);
        PowerMock.verifyAll();
    }

    @Test
    public void populateFieldsStringField() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);
        final Node node = createMock(Node.class);
        final StringFieldType fieldType = createMock(StringFieldType.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.singletonList(fieldContext));
        expect(fieldContext.getContentTypeItem()).andReturn(item);
        expect(item.isProperty()).andReturn(true);
        expect(item.getItemType()).andReturn("String");
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.of(PROPERTY_FIELD_PLUGIN));
        expect(FieldTypeFactory.createFieldType(StringFieldType.class)).andReturn(Optional.of(fieldType));
        fieldType.init(fieldContext);
        expectLastCall();
        expect(fieldType.isValid()).andReturn(true);
        replay(sorter, context, fieldContext, item, fieldType);
        PowerMock.replayAll();

        FieldTypeUtils.populateFields(fields, context);

        assertThat(fields.size(), equalTo(1));
        assertThat(fields.get(0), equalTo(fieldType));
        verify(sorter, context, fieldContext, item, fieldType);
        PowerMock.verifyAll();
    }

    @Test
    public void populateFieldsStringAndMultilineString() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final FieldTypeContext fieldContext1 = createMock(FieldTypeContext.class);
        final FieldTypeContext fieldContext2 = createMock(FieldTypeContext.class);
        final FieldTypeContext fieldContext3 = createMock(FieldTypeContext.class);
        final ContentTypeItem item1 = createMock(ContentTypeItem.class);
        final ContentTypeItem item2 = createMock(ContentTypeItem.class);
        final ContentTypeItem item3 = createMock(ContentTypeItem.class);
        final Node node = createMock(Node.class);
        final StringFieldType stringField1 = createMock(StringFieldType.class);
        final StringFieldType stringField2 = createMock(StringFieldType.class);
        final MultilineStringFieldType multilineStringField = createMock(MultilineStringFieldType.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Arrays.asList(fieldContext1, fieldContext2, fieldContext3));
        expect(fieldContext1.getContentTypeItem()).andReturn(item1);
        expect(item1.isProperty()).andReturn(true);
        expect(item1.getItemType()).andReturn("String");
        expect(fieldContext2.getContentTypeItem()).andReturn(item2);
        expect(item2.isProperty()).andReturn(true);
        expect(item2.getItemType()).andReturn("Text");
        expect(fieldContext3.getContentTypeItem()).andReturn(item3);
        expect(item3.isProperty()).andReturn(true);
        expect(item3.getItemType()).andReturn("Label");
        expect(fieldContext1.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(fieldContext2.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(fieldContext3.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.of(PROPERTY_FIELD_PLUGIN)).anyTimes();
        expect(FieldTypeFactory.createFieldType(StringFieldType.class)).andReturn(Optional.of(stringField1));
        expect(FieldTypeFactory.createFieldType(MultilineStringFieldType.class)).andReturn(Optional.of(multilineStringField));
        expect(FieldTypeFactory.createFieldType(StringFieldType.class)).andReturn(Optional.of(stringField2));
        stringField1.init(fieldContext1);
        expectLastCall();
        expect(stringField1.isValid()).andReturn(true);
        multilineStringField.init(fieldContext2);
        expectLastCall();
        expect(multilineStringField.isValid()).andReturn(true);
        stringField2.init(fieldContext3);
        expectLastCall();
        expect(stringField2.isValid()).andReturn(true);

        PowerMock.replayAll();
        replay(sorter, context, fieldContext1, fieldContext2, fieldContext3, item1, item2, item3,
                stringField1, multilineStringField, stringField2);

        FieldTypeUtils.populateFields(fields, context);

        assertThat(fields.size(), equalTo(3));
        assertThat(fields.get(0), equalTo(stringField1));
        assertThat(fields.get(1), equalTo(multilineStringField));
        assertThat(fields.get(2), equalTo(stringField2));

        verify(sorter, context, fieldContext1, fieldContext2, fieldContext3, item1, item2, item3,
                stringField1, multilineStringField, stringField2);
        PowerMock.verifyAll();
    }

    @Test
    public void populateFieldsCompoundField() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);
        final Node node = createMock(Node.class);
        final CompoundFieldType fieldType = createMock(CompoundFieldType.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.singletonList(fieldContext));
        expect(fieldContext.getContentTypeItem()).andReturn(item);
        expect(item.isProperty()).andReturn(false);
        expect(ChoiceFieldUtils.isChoiceField(fieldContext)).andReturn(false);
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.of(COMPOUND_FIELD_PLUGIN));
        expect(FieldTypeFactory.createFieldType(CompoundFieldType.class)).andReturn(Optional.of(fieldType));
        fieldType.init(fieldContext);
        expectLastCall();
        expect(fieldType.isValid()).andReturn(true);
        replay(sorter, context, fieldContext, item, fieldType);
        PowerMock.replayAll();

        FieldTypeUtils.populateFields(fields, context);

        assertThat(fields.size(), equalTo(1));
        assertThat(fields.get(0), equalTo(fieldType));
        verify(sorter, context, fieldContext, item, fieldType);
        PowerMock.verifyAll();
    }

    @Test
    public void populateFieldsChoiceField() {
        final List<FieldType> fields = new ArrayList<>();
        final FieldSorter sorter = createMock(FieldSorter.class);
        final ContentTypeContext context = createMock(ContentTypeContext.class);
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);
        final Node node = createMock(Node.class);
        final ChoiceFieldType fieldType = createMock(ChoiceFieldType.class);

        expect(context.getContentTypeRoot()).andReturn(null);
        expect(NamespaceUtils.retrieveFieldSorter(null)).andReturn(Optional.of(sorter));
        expect(sorter.sortFields(context)).andReturn(Collections.singletonList(fieldContext));
        expect(fieldContext.getContentTypeItem()).andReturn(item);
        expect(item.isProperty()).andReturn(false);
        expect(ChoiceFieldUtils.isChoiceField(fieldContext)).andReturn(true);
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(NamespaceUtils.getPluginClassForField(node)).andReturn(Optional.of(CHOICE_FIELD_PLUGIN));
        expect(FieldTypeFactory.createFieldType(ChoiceFieldType.class)).andReturn(Optional.of(fieldType));
        fieldType.init(fieldContext);
        expectLastCall();
        expect(fieldType.isValid()).andReturn(true);
        replay(sorter, context, fieldContext, item, fieldType);
        PowerMock.replayAll();

        FieldTypeUtils.populateFields(fields, context);

        assertThat(fields.size(), equalTo(1));
        assertThat(fields.get(0), equalTo(fieldType));
        verify(sorter, context, fieldContext, item, fieldType);
        PowerMock.verifyAll();
    }

    @Test
    public void readFieldValues() {
        final StringFieldType field1 = createMock(StringFieldType.class);
        final StringFieldType field2 = createMock(StringFieldType.class);
        final Node node = createMock(Node.class);
        final Map<String, List<FieldValue>> valueMap = new HashMap<>();
        final FieldValue value1 = new FieldValue("one");
        final FieldValue value2 = new FieldValue("two");

        expect(field1.readFrom(node)).andReturn(Optional.empty());
        expect(field2.getId()).andReturn("field2");
        expect(field2.readFrom(node)).andReturn(Optional.of(Arrays.asList(value1, value2)));
        replay(field1, field2);

        FieldTypeUtils.readFieldValues(node, Arrays.asList(field1, field2), valueMap);

        assertFalse(valueMap.containsKey("field1"));
        assertThat(valueMap.get("field2").size(), equalTo(2));
        assertThat(valueMap.get("field2").get(0).getValue(), equalTo("one"));
        assertThat(valueMap.get("field2").get(1).getValue(), equalTo("two"));
        verify(field1, field2);
    }

    @Test
    public void writeFieldValues() throws Exception {
        final StringFieldType field1 = createMock(StringFieldType.class);
        final StringFieldType field2 = createMock(StringFieldType.class);
        final Node node = createMock(Node.class);
        final Map<String, List<FieldValue>> valueMap = new HashMap<>();
        final FieldValue value1 = new FieldValue("one");
        final FieldValue value2 = new FieldValue("two");
        valueMap.put("field2", Arrays.asList(value1, value2));

        expect(field1.hasUnsupportedValidator()).andReturn(false);
        expect(field1.getId()).andReturn("field1");
        field1.writeTo(node, Optional.empty());
        expectLastCall();
        expect(field2.hasUnsupportedValidator()).andReturn(false);
        expect(field2.getId()).andReturn("field2");
        field2.writeTo(node, Optional.of(Arrays.asList(value1, value2)));
        expectLastCall();
        replay(field1, field2);

        FieldTypeUtils.writeFieldValues(valueMap, Arrays.asList(field1, field2), node);

        verify(field1, field2);
    }

    @Test
    public void writeFieldValuesWithUnknownValidator() throws Exception {
        final StringFieldType field1 = createMock(StringFieldType.class);
        final StringFieldType field2 = createMock(StringFieldType.class);
        final Node node = createMock(Node.class);
        final Map<String, List<FieldValue>> valueMap = new HashMap<>();
        final FieldValue value1 = new FieldValue("one");
        final FieldValue value2 = new FieldValue("two");
        valueMap.put("field2", Arrays.asList(value1, value2));

        expect(field1.hasUnsupportedValidator()).andReturn(true);
        expect(field2.hasUnsupportedValidator()).andReturn(false);
        expect(field2.getId()).andReturn("field2");
        field2.writeTo(node, Optional.of(Arrays.asList(value1, value2)));
        expectLastCall();
        replay(field1, field2);

        FieldTypeUtils.writeFieldValues(valueMap, Arrays.asList(field1, field2), node);

        verify(field1, field2);
    }

    @Test
    public void validateFieldValuesNoFields() {
        final Map<String, List<FieldValue>> valueMap = new HashMap<>();

        assertTrue(FieldTypeUtils.validateFieldValues(valueMap, Collections.emptyList()));
    }

    @Test
    public void validateFieldValuesTwoValid() {
        final StringFieldType field1 = createMock(StringFieldType.class);
        final StringFieldType field2 = createMock(StringFieldType.class);
        final Map<String, List<FieldValue>> valueMap = new HashMap<>();
        final List<FieldValue> validValueList = Collections.singletonList(new FieldValue("valid"));
        valueMap.put("field2", validValueList);

        expect(field1.getId()).andReturn("field1");
        expect(field2.getId()).andReturn("field2");
        expect(field2.validate(validValueList)).andReturn(true);
        replay(field1, field2);

        assertTrue(FieldTypeUtils.validateFieldValues(valueMap, Arrays.asList(field1, field2)));
        verify(field1, field2);
    }

    @Test
    public void validateFieldValuesFirstInvalid() {
        final StringFieldType field1 = createMock(StringFieldType.class);
        final StringFieldType field2 = createMock(StringFieldType.class);
        final Map<String, List<FieldValue>> valueMap = new HashMap<>();
        final List<FieldValue> invalidValueList = Collections.singletonList(new FieldValue("invalid"));
        final List<FieldValue> validValueList = Collections.singletonList(new FieldValue("valid"));
        valueMap.put("field1", invalidValueList);
        valueMap.put("field2", validValueList);

        expect(field1.getId()).andReturn("field1");
        expect(field1.validate(invalidValueList)).andReturn(false);
        expect(field2.getId()).andReturn("field2");
        expect(field2.validate(validValueList)).andReturn(true);
        replay(field1, field2);

        assertFalse(FieldTypeUtils.validateFieldValues(valueMap, Arrays.asList(field1, field2)));
        verify(field1, field2);
    }

    @Test
    public void validateFieldValuesSecondInvalid() {
        final StringFieldType field1 = createMock(StringFieldType.class);
        final StringFieldType field2 = createMock(StringFieldType.class);
        final Map<String, List<FieldValue>> valueMap = new HashMap<>();
        final List<FieldValue> invalidValueList = Collections.singletonList(new FieldValue("invalid"));
        final List<FieldValue> validValueList = Collections.singletonList(new FieldValue("valid"));
        valueMap.put("field1", validValueList);
        valueMap.put("field2", invalidValueList);

        expect(field1.getId()).andReturn("field1");
        expect(field1.validate(validValueList)).andReturn(true);
        expect(field2.getId()).andReturn("field2");
        expect(field2.validate(invalidValueList)).andReturn(false);
        replay(field1, field2);

        assertFalse(FieldTypeUtils.validateFieldValues(valueMap, Arrays.asList(field1, field2)));
        verify(field1, field2);
    }

    @Test
    public void validateFieldValuesBothInvalid() {
        final StringFieldType field1 = createMock(StringFieldType.class);
        final StringFieldType field2 = createMock(StringFieldType.class);
        final Map<String, List<FieldValue>> valueMap = new HashMap<>();
        final List<FieldValue> invalidValueList = Collections.singletonList(new FieldValue("invalid"));
        valueMap.put("field1", invalidValueList);
        valueMap.put("field2", invalidValueList);

        expect(field1.getId()).andReturn("field1");
        expect(field1.validate(invalidValueList)).andReturn(false);
        expect(field2.getId()).andReturn("field2");
        expect(field2.validate(invalidValueList)).andReturn(false);
        replay(field1, field2);

        assertFalse(FieldTypeUtils.validateFieldValues(valueMap, Arrays.asList(field1, field2)));
        verify(field1, field2);
    }
}
