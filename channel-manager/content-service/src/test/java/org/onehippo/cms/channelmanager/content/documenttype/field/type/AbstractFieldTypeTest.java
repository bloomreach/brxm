/*
 * Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.jcr.Node;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldValidators;
import org.onehippo.cms.channelmanager.content.documenttype.field.validation.CompoundContext;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

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
                return Optional.empty();
            }

            @Override
            public int validateValue(final FieldValue value, final CompoundContext context) {
                return 0;
            }

            @Override
            public Object getValidatedValue(final FieldValue value, final CompoundContext context) {
                return null;
            }

            @Override
            protected void writeValues(Node node, Optional<List<FieldValue>> optionalValue, boolean validateValues) {
            }

            @Override
            public boolean writeField(FieldPath fieldPath, List<FieldValue> value, final CompoundContext context) {
                return false;
            }

            @Override
            public int validate(final List<FieldValue> valueList, final CompoundContext context) throws ErrorWithPayloadException {
                return 0;
            }
        };
    }

    @Test
    public void hasUnsupportedValidator() {
        assertFalse(fieldType.hasUnsupportedValidator());
        fieldType.setUnsupportedValidator(true);
        assertTrue(fieldType.hasUnsupportedValidator());
        fieldType.setUnsupportedValidator(false);
        assertFalse(fieldType.hasUnsupportedValidator());
    }

    @Test
    public void isRequired() {
        assertFalse(fieldType.isRequired());

        fieldType.setRequired(true);
        assertTrue(fieldType.isRequired());

        fieldType.setRequired(false);
        assertFalse(fieldType.isRequired());
    }

    @Test
    public void isSupported() {
        assertTrue(fieldType.isSupported());

        fieldType.setUnsupportedValidator(true);
        assertFalse(fieldType.isSupported());
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
        fieldType.setMultiple(true);
        assertTrue(fieldType.isMultiple());
    }

    @Test
    public void zeroValidatorNames() {
        assertTrue(fieldType.getValidatorNames().isEmpty());
    }

    @Test
    public void oneValidatorName() {
        fieldType.addValidatorName("non-empty");

        final Set<String> validatorNames = fieldType.getValidatorNames();
        assertThat(validatorNames.size(), equalTo(1));
        assertThat(validatorNames.iterator().next(), equalTo("non-empty"));
    }

    @Test
    public void twoValidatorNames() {
        fieldType.addValidatorName("non-empty");
        fieldType.addValidatorName("email");

        final Set<String> validatorNames = fieldType.getValidatorNames();
        assertThat(validatorNames.size(), equalTo(2));

        final Iterator<String> names = validatorNames.iterator();
        assertThat(names.next(), equalTo("non-empty"));
        assertThat(names.next(), equalTo("email"));
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
        fieldType.setRequired(true);
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
    public void validateEmpty() {
        assertZeroViolations(fieldType.validate(Collections.emptyList(), null));
    }

//    TODO: move to three main types (primite, compound, choice)?
//    @Test
    public void validateBothGood() {
        fieldType = PowerMock.createMock(AbstractFieldType.class);

        final FieldValue one = new FieldValue("one");
        final FieldValue two = new FieldValue("two");

        expect(fieldType.validateValue(one, null)).andReturn(0);
        expect(fieldType.validateValue(two, null)).andReturn(0);
        replayAll();

        assertZeroViolations(fieldType.validate(Arrays.asList(one, two), null));
        verifyAll();
    }

//    TODO: move to three main types (primite, compound, choice)?
//    @Test
    public void validateFirstBad() {
        fieldType = PowerMock.createMock(AbstractFieldType.class);

        final FieldValue one = new FieldValue("one");
        final FieldValue two = new FieldValue("two");

        expect(fieldType.validateValue(one, null)).andReturn(1);
        expect(fieldType.validateValue(two, null)).andReturn(0);
        replayAll();

        assertViolation(fieldType.validate(Arrays.asList(one, two), null));
        verifyAll();
    }

//    TODO: move to three main types (primite, compound, choice)?
//    @Test
    public void validateSecondBad() {
        fieldType = PowerMock.createMock(AbstractFieldType.class);

        final FieldValue one = new FieldValue("one");
        final FieldValue two = new FieldValue("two");

        expect(fieldType.validateValue(one, null)).andReturn(0);
        expect(fieldType.validateValue(two, null)).andReturn(1);
        replayAll();

        assertViolation(fieldType.validate(Arrays.asList(one, two), null));
        verifyAll();
    }

//    TODO: move to three main types (primite, compound, choice)?
//    @Test
    public void validateBothBad() {
        fieldType = PowerMock.createMock(AbstractFieldType.class);

        final FieldValue one = new FieldValue("one");
        final FieldValue two = new FieldValue("two");

        expect(fieldType.validateValue(one, null)).andReturn(1);
        expect(fieldType.validateValue(two, null)).andReturn(1);
        replayAll();

        assertViolations(fieldType.validate(Arrays.asList(one, two), null), 2);
        verifyAll();
    }

    @Test
    public void initOptionalNoLocalization() {
        final FieldTypeContext fieldContext = new MockFieldTypeContext.Builder(fieldType)
                .validators(Collections.singletonList(FieldValidators.OPTIONAL))
                .build();

        replayAll();

        final FieldsInformation fieldsInfo = fieldType.init(fieldContext);

        assertThat(fieldsInfo, equalTo(FieldsInformation.allSupported()));
        assertFieldType(fieldType,
                MockFieldTypeContext.DEFAULT_JCR_NAME,
                MockFieldTypeContext.DEFAULT_JCR_TYPE,
                null, null,
                0, 1);

        verifyAll();
    }

    @Test
    public void initMultipleWithLocalization() {
        final FieldTypeContext fieldContext = new MockFieldTypeContext.Builder(fieldType)
                .displayName("Field Display Name")
                .hint("Hint")
                .multiple(true)
                .build();

        replayAll();

        final FieldsInformation fieldsInfo = fieldType.init(fieldContext);

        assertThat(fieldsInfo, equalTo(FieldsInformation.allSupported()));
        assertFieldType(fieldType,
                MockFieldTypeContext.DEFAULT_JCR_NAME,
                MockFieldTypeContext.DEFAULT_JCR_TYPE,
                "Field Display Name",
                "Hint",
                0,
                Integer.MAX_VALUE);

        verifyAll();
    }

    @Test
    public void initSingularNoLocalization() {
        final FieldTypeContext fieldContext = new MockFieldTypeContext.Builder(fieldType).build();

        replayAll();

        final FieldsInformation fieldsInfo = fieldType.init(fieldContext);

        assertFieldType(fieldType, "field:id", "String", null, null, 1, 1);
        assertThat(fieldsInfo, equalTo(FieldsInformation.allSupported()));

        verifyAll();
    }

    @Test
    public void type() {
        assertNull(fieldType.getType());
        fieldType.setType(FieldType.Type.MULTILINE_STRING);
        assertThat(fieldType.getType(), equalTo(FieldType.Type.MULTILINE_STRING));
    }

    static void assertZeroViolations(int violationCount) {
        assertViolations(violationCount, 0);
    }

    static void assertViolation(int violationCount) {
        assertViolations(violationCount, 1);
    }

    static void assertViolations(int actualViolationCount, int expectedViolationCount) {
        assertThat("Number of violations", actualViolationCount, equalTo(expectedViolationCount));
    }

    private static void assertFieldType(final AbstractFieldType fieldType,
                                        final String id,
                                        final String jcrType,
                                        final String displayName,
                                        final String hint,
                                        final int min,
                                        final int max) {

        assertThat(fieldType.getId(), equalTo(id));
        // TODO make it return effective-type
//        assertThat(fieldType.getType(), equalTo("Text"));
        assertThat(fieldType.getJcrType(), equalTo(jcrType));
        assertThat(fieldType.getDisplayName(), equalTo(displayName));
        assertThat(fieldType.getHint(), equalTo(hint));
        assertThat(fieldType.getMinValues(), equalTo(min));
        assertThat(fieldType.getMaxValues(), equalTo(max));
    }
}
