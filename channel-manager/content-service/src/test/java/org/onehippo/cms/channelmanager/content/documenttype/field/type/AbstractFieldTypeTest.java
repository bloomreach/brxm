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

package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
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
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms.channelmanager.content.documenttype.validation.CompoundContext;
import org.onehippo.cms.channelmanager.content.documenttype.validation.ValidationUtils;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.services.validation.api.ValueContext;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStaticPartial;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest({LocalizationUtils.class, FieldTypeUtils.class, ValidationUtils.class})
public class AbstractFieldTypeTest {

    private AbstractFieldType fieldType;

    @Before
    public void setup() {
        fieldType = new TestAbstractFieldType();
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

        fieldType.addValidatorName(FieldValidators.REQUIRED);
        assertTrue(fieldType.isRequired());
    }

    @Test
    public void isSupported() {
        assertTrue(fieldType.isSupported());

        fieldType.setUnsupportedValidator(true);
        assertFalse(fieldType.isSupported());
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
    public void alwaysValidWhenValidatorNamesIsEmpty() {
        fieldType = new TestAbstractFieldType() {
            @Override
            public Object getValidatedValue(final FieldValue value, final CompoundContext context) {
                fail("Should not be called");
                return null;
            }
        };

        assertZeroViolations(fieldType.validateValue(null, null));
    }

    @Test
    public void validateEmpty() {
        assertZeroViolations(fieldType.validate(Collections.emptyList(), null));
    }

    @Test
    public void validateValueGood() {
        fieldType = createFieldType("field:id", "field:jcr-type", "field:effective-type");
        fieldType.addValidatorName("validator1");
        fieldType.addValidatorName("validator2");

        final CompoundContext context = createMock(CompoundContext.class);
        final ValueContext valueContext = createMock(ValueContext.class);
        expect(context.getValueContext(eq("field:id"), eq("field:jcr-type"), eq("field:effective-type")))
                .andReturn(valueContext);

        final FieldValue one = new FieldValue("one");
        mockStaticPartial(ValidationUtils.class, "validateValue", FieldValue.class, ValueContext.class, Set.class,
                Object.class);
        expect(ValidationUtils.validateValue(one, valueContext, set("validator1", "validator2"), one.getValue())).andReturn(0);

        replayAll();

        assertZeroViolations(fieldType.validateValue(one, context));

        verifyAll();
    }

    @Test
    public void validatorValueOneBad() {
        fieldType = createFieldType("field:id", "field:jcr-type", "field:effective-type");
        fieldType.addValidatorName("validator1");
        fieldType.addValidatorName("validator2");

        final CompoundContext context = createMock(CompoundContext.class);
        final ValueContext valueContext = createMock(ValueContext.class);
        expect(context.getValueContext(eq("field:id"), eq("field:jcr-type"), eq("field:effective-type")))
                .andReturn(valueContext);

        final FieldValue one = new FieldValue("one");
        mockStaticPartial(ValidationUtils.class, "validateValue", FieldValue.class, ValueContext.class, Set.class,
                Object.class);
        expect(ValidationUtils.validateValue(one, valueContext, set("validator1", "validator2"), one.getValue())).andReturn(1);

        replayAll();

        assertViolation(fieldType.validateValue(one, context));

        verifyAll();
    }

    @Test
    public void validatorValueTwoBad() {
        fieldType = createFieldType("field:id", "field:jcr-type", "field:effective-type");
        fieldType.addValidatorName("validator1");
        fieldType.addValidatorName("validator2");

        final CompoundContext context = createMock(CompoundContext.class);
        final ValueContext valueContext = createMock(ValueContext.class);
        expect(context.getValueContext(eq("field:id"), eq("field:jcr-type"), eq("field:effective-type")))
                .andReturn(valueContext);

        final FieldValue one = new FieldValue("one");
        mockStaticPartial(ValidationUtils.class, "validateValue", FieldValue.class, ValueContext.class, Set.class,
                Object.class);
        expect(ValidationUtils.validateValue(one, valueContext, set("validator1", "validator2"), one.getValue())).andReturn(2);

        replayAll();

        assertViolations(fieldType.validateValue(one, context), 2);

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

    private static Set<String> set(String... strings) {
        return new LinkedHashSet<>(Arrays.asList(strings));
    }

    private static class TestAbstractFieldType extends AbstractFieldType {

        @Override
        public Optional<List<FieldValue>> readFrom(Node node) {
            return Optional.empty();
        }

        @Override
        public List<FieldValue> readValues(final Node node) {
            return null;
        }

        @Override
        public Object getValidatedValue(final FieldValue value, final CompoundContext context) {
            return null;
        }

        @Override
        public void writeValues(final Node node, final Optional<List<FieldValue>> optionalValues) {
        }

        @Override
        public boolean writeField(FieldPath fieldPath, List<FieldValue> value, final CompoundContext context) {
            return false;
        }

        @Override
        public int validate(final List<FieldValue> valueList, final CompoundContext context) throws ErrorWithPayloadException {
            return 0;
        }
    }

    private static AbstractFieldType createFieldType(final String id, final String jcrType, final String effectiveType) {
        final AbstractFieldType fieldType = new TestAbstractFieldType() {
            @Override
            public Object getValidatedValue(final FieldValue value, final CompoundContext context) {
                return value.getValue();
            }
        };
        fieldType.setId(id);
        fieldType.setJcrType(jcrType);
        fieldType.setEffectiveType(effectiveType);
        return fieldType;
    }

}
