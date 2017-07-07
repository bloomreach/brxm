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

package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import javax.jcr.Node;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldValidators;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({LocalizationUtils.class, FieldTypeUtils.class})
public class AbstractFieldTypeTest {

    private AbstractFieldType fieldType;

    @Before
    public void setup() {
        fieldType = new AbstractFieldType() {
            @Override
            public Optional<List<FieldValue>> readFrom(Node node) {
                return null;
            }

            @Override
            protected void writeValues(Node node, Optional<List<FieldValue>> optionalValue, boolean validateValues)
                    throws ErrorWithPayloadException { }

            @Override
            public boolean writeField(Node node, FieldPath fieldPath, List<FieldValue> value) throws ErrorWithPayloadException {
                return false;
            }

            @Override
            public boolean validate(List<FieldValue> valueList) {
                return false;
            }
        };
    }

    @Test
    public void hasUnsupportedValidator() {
        assertFalse(fieldType.hasUnsupportedValidator());
        fieldType.addValidator(FieldType.Validator.UNSUPPORTED);
        assertTrue(fieldType.hasUnsupportedValidator());
        fieldType.getValidators().remove(FieldType.Validator.UNSUPPORTED);
        assertFalse(fieldType.hasUnsupportedValidator());
    }

    @Test
    public void isValid() {
        assertTrue(fieldType.isValid());

        fieldType.addValidator(FieldType.Validator.UNSUPPORTED);
        assertFalse(fieldType.isValid());
    }

    @Test
    public void trimToMaxValues() {
        final List<String> list = new ArrayList<>();
        list.add("one");
        list.add("two");
        list.add("three");
        assertThat(fieldType.getMaxValues(), equalTo(1));
        fieldType.setMaxValues(2);
        fieldType.trimToMaxValues(list);
        assertThat(list.size(), equalTo(2));
        assertThat(list.get(1), equalTo("two"));
        fieldType.setMaxValues(5);
        fieldType.trimToMaxValues(list);
        assertThat(list.size(), equalTo(2));
        assertThat(list.get(1), equalTo("two"));
        fieldType.setMaxValues(0);
        fieldType.trimToMaxValues(list);
        assertThat(list.size(), equalTo(0));
    }

    @Test
    public void isMultiple() {
        assertFalse(fieldType.isMultiple());
        fieldType.setMaxValues(2);
        assertTrue(fieldType.isMultiple());
    }


    @Test
    public void checkCardinalityMoreThanAllowed() throws Exception {
        final List<FieldValue> list = new ArrayList<>();
        list.add(new FieldValue("one"));
        list.add(new FieldValue("two"));
        list.add(new FieldValue("three"));

        fieldType.setMaxValues(2);
        checkCardinality(list);
    }

    @Test
    public void checkCardinalityLessThanRequired() throws Exception {
        final List<FieldValue> list = new ArrayList<>();
        list.add(new FieldValue("one"));
        list.add(new FieldValue("two"));
        list.add(new FieldValue("three"));

        assertThat(fieldType.getMinValues(), equalTo(1));
        fieldType.setMinValues(4);
        checkCardinality(list);
    }

    @Test
    public void checkCardinalityNoneButRequired() throws Exception {
        fieldType.setMinValues(0);
        fieldType.addValidator(FieldType.Validator.REQUIRED);
        checkCardinality(Collections.emptyList());
    }

    private void checkCardinality(final List<FieldValue> list) throws Exception {
        try {
            fieldType.checkCardinality(list);
            fail("No exception");
        } catch (BadRequestException e) {
            assertTrue(e.getPayload() instanceof ErrorInfo);
            ErrorInfo errorInfo = (ErrorInfo) e.getPayload();
            assertThat(errorInfo.getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
    }

    @Test
    public void checkCardinalitySuccess() throws Exception {
        fieldType.checkCardinality(Collections.singletonList(new FieldValue("one")));
    }

    @Test
    public void validateValuesEmpty() {
        assertTrue(fieldType.validateValues(Collections.emptyList(), null));
    }

    @Test
    public void validateValuesAllGood() {
        final Predicate<FieldValue> validator = (value) -> true;
        final List<FieldValue> list = new ArrayList<>();
        list.add(new FieldValue("one"));
        list.add(new FieldValue("two"));
        assertTrue(fieldType.validateValues(list, validator));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void validateValuesLastBad() {
        final Predicate<FieldValue> validator = createMock(Predicate.class);
        final List<FieldValue> list = new ArrayList<>();
        list.add(new FieldValue("one"));
        list.add(new FieldValue("two"));

        expect(validator.test(list.get(0))).andReturn(true);
        expect(validator.test(list.get(1))).andReturn(false);

        replay(validator);

        assertFalse(fieldType.validateValues(list, validator));

        verify(validator);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void validateValuesFirstBad() {
        final Predicate<FieldValue> validator = createMock(Predicate.class);
        final List<FieldValue> list = new ArrayList<>();
        list.add(new FieldValue("one"));
        list.add(new FieldValue("two"));

        expect(validator.test(list.get(0))).andReturn(false);
        expect(validator.test(list.get(1))).andReturn(true);

        replay(validator);

        assertFalse(fieldType.validateValues(list, validator));

        verify(validator);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void validateValuesBothBad() {
        final Predicate<FieldValue> validator = createMock(Predicate.class);
        final List<FieldValue> list = new ArrayList<>();
        list.add(new FieldValue("one"));
        list.add(new FieldValue("two"));

        expect(validator.test(list.get(0))).andReturn(false);
        expect(validator.test(list.get(1))).andReturn(false);

        replay(validator);

        assertFalse(fieldType.validateValues(list, validator));

        verify(validator);
    }

    @Test
    public void initOptionalNoLocalization() {
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);
        final DocumentType docType = new DocumentType();
        final List<String> validators = Collections.singletonList(FieldValidators.OPTIONAL);

        PowerMock.mockStaticPartial(LocalizationUtils.class, "determineFieldDisplayName", "determineFieldHint");
        PowerMock.mockStaticPartial(FieldTypeUtils.class, "determineValidators");

        expect(LocalizationUtils.determineFieldDisplayName("field:id", Optional.empty(), Optional.empty()))
                .andReturn(Optional.empty());
        expect(LocalizationUtils.determineFieldHint("field:id", Optional.empty(), Optional.empty()))
                .andReturn(Optional.empty());
        FieldTypeUtils.determineValidators(fieldType, docType, validators);
        expectLastCall();

        expect(fieldContext.getParentContext()).andReturn(parentContext);
        expect(fieldContext.getContentTypeItem()).andReturn(item);
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.empty());
        expect(parentContext.getResourceBundle()).andReturn(Optional.empty());
        expect(parentContext.getDocumentType()).andReturn(docType);
        expect(item.getName()).andReturn("field:id");
        expect(item.getValidators()).andReturn(validators).anyTimes();
        expect(item.isMultiple()).andReturn(false);

        PowerMock.replayAll();
        replay(fieldContext, parentContext, item);

        fieldType.init(fieldContext);

        assertThat(fieldType.getId(), equalTo("field:id"));
        assertNull(fieldType.getDisplayName());
        assertNull(fieldType.getHint());
        assertThat(fieldType.getMinValues(), equalTo(0));
        assertThat(fieldType.getMaxValues(), equalTo(1));

        verify(fieldContext, parentContext, item);
        PowerMock.verifyAll();
    }

    @Test
    public void initMultipleWithLocalization() {
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);
        final DocumentType docType = new DocumentType();
        final List<String> validators = Collections.emptyList();

        PowerMock.mockStaticPartial(LocalizationUtils.class, "determineFieldDisplayName", "determineFieldHint");
        PowerMock.mockStaticPartial(FieldTypeUtils.class, "determineValidators");

        expect(LocalizationUtils.determineFieldDisplayName("field:id", Optional.empty(), Optional.empty()))
                .andReturn(Optional.of("Field Display Name"));
        expect(LocalizationUtils.determineFieldHint("field:id", Optional.empty(), Optional.empty()))
                .andReturn(Optional.of("Hint"));
        FieldTypeUtils.determineValidators(fieldType, docType, validators);
        expectLastCall();

        expect(fieldContext.getParentContext()).andReturn(parentContext);
        expect(fieldContext.getContentTypeItem()).andReturn(item);
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.empty());
        expect(parentContext.getResourceBundle()).andReturn(Optional.empty());
        expect(parentContext.getDocumentType()).andReturn(docType);
        expect(item.getName()).andReturn("field:id");
        expect(item.getValidators()).andReturn(validators).anyTimes();
        expect(item.isMultiple()).andReturn(true);

        PowerMock.replayAll();
        replay(fieldContext, parentContext, item);

        fieldType.init(fieldContext);

        assertThat(fieldType.getId(), equalTo("field:id"));
        assertThat(fieldType.getDisplayName(), equalTo("Field Display Name"));
        assertThat(fieldType.getHint(), equalTo("Hint"));
        assertThat(fieldType.getMinValues(), equalTo(0));
        assertThat(fieldType.getMaxValues(), equalTo(Integer.MAX_VALUE));

        verify(fieldContext, parentContext, item);
        PowerMock.verifyAll();
    }

    @Test
    public void initSingularNoLocalization() {
        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);
        final DocumentType docType = new DocumentType();
        final List<String> validators = Collections.emptyList();

        PowerMock.mockStaticPartial(LocalizationUtils.class, "determineFieldDisplayName", "determineFieldHint");
        PowerMock.mockStaticPartial(FieldTypeUtils.class, "determineValidators");

        expect(LocalizationUtils.determineFieldDisplayName("field:id", Optional.empty(), Optional.empty()))
                .andReturn(Optional.empty());
        expect(LocalizationUtils.determineFieldHint("field:id", Optional.empty(), Optional.empty()))
                .andReturn(Optional.empty());
        FieldTypeUtils.determineValidators(fieldType, docType, validators);
        expectLastCall();

        expect(fieldContext.getParentContext()).andReturn(parentContext);
        expect(fieldContext.getContentTypeItem()).andReturn(item);
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.empty());
        expect(parentContext.getResourceBundle()).andReturn(Optional.empty());
        expect(parentContext.getDocumentType()).andReturn(docType);
        expect(item.getName()).andReturn("field:id");
        expect(item.getValidators()).andReturn(validators).anyTimes();
        expect(item.isMultiple()).andReturn(false);

        PowerMock.replayAll();
        replay(fieldContext, parentContext, item);

        fieldType.init(fieldContext);

        assertThat(fieldType.getId(), equalTo("field:id"));
        assertNull(fieldType.getDisplayName());
        assertNull(fieldType.getHint());
        assertThat(fieldType.getMinValues(), equalTo(1));
        assertThat(fieldType.getMaxValues(), equalTo(1));

        verify(fieldContext, parentContext, item);
        PowerMock.verifyAll();
    }

    @Test
    public void type() {
        assertNull(fieldType.getType());
        fieldType.setType(FieldType.Type.MULTILINE_STRING);
        assertThat(fieldType.getType(), equalTo(FieldType.Type.MULTILINE_STRING));
    }
}
