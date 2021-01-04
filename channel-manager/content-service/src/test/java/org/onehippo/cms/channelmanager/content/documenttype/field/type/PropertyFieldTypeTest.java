/*
 * Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.JcrUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldValidators;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType.Type;
import org.onehippo.cms.channelmanager.content.documenttype.util.NamespaceUtils;
import org.onehippo.cms.channelmanager.content.documenttype.validation.CompoundContext;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.repository.mock.MockNode;
import org.powermock.api.easymock.PowerMock;
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
import static org.onehippo.cms.channelmanager.content.ValidateAndWrite.validateAndWriteTo;
import static org.onehippo.cms.channelmanager.content.documenttype.field.type.AbstractFieldTypeTest.assertViolation;
import static org.onehippo.cms.channelmanager.content.documenttype.field.type.AbstractFieldTypeTest.assertZeroViolations;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStaticPartial;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({
        "com.sun.org.apache.xalan.*",
        "com.sun.org.apache.xerces.*",
        "javax.activation.*",
        "javax.management.*",
        "javax.net.ssl.*",
        "javax.xml.*",
        "org.apache.logging.log4j.*",
        "org.w3c.dom.*",
        "org.xml.*",
})
@PrepareForTest({JcrUtils.class, FieldTypeUtils.class, NamespaceUtils.class})
public class PropertyFieldTypeTest {

    private static final String PROPERTY = "test:id";
    private PropertyFieldType fieldType;
    private Node node;
    private CompoundContext nodeContext;

    @Before
    public void setup() {
        PowerMock.mockStatic(JcrUtils.class);
        PowerMock.mockStatic(NamespaceUtils.class);
        fieldType = new PropertyFieldType() {

            @Override
            public Object getValidatedValue(final FieldValue value, final CompoundContext context) {
                return value.getValue();
            }

            @Override
            protected String getDefault() {
                return "";
            }
        };
        fieldType.setType(Type.STRING);
        node = MockNode.root();
        nodeContext = new CompoundContext(node, null, null, null);
    }

    @Test
    public void readFromSingleField() throws Exception {
        fieldType.setId(PROPERTY);
        node.setProperty(PROPERTY, "Value");

        final List<FieldValue> list = fieldType.readFrom(node).get();
        assertThat(list.size(), equalTo(1));
        assertThat(list.get(0).getValue(), equalTo("Value"));
    }

    @Test
    public void readFromSingleIncorrectField() throws Exception {
        fieldType.setId(PROPERTY);

        node.setProperty(PROPERTY, new String[]{"Value", "Ignore"});
        List<FieldValue> list = fieldType.readFrom(node).get();
        assertThat(list.size(), equalTo(1));
        assertThat(list.get(0).getValue(), equalTo("Value"));

        node.setProperty(PROPERTY, new String[0]);
        list = fieldType.readFrom(node).get();
        assertThat(list.size(), equalTo(1));
        assertThat(list.get(0).getValue(), equalTo(""));

        node.getProperty(PROPERTY).remove();
        list = fieldType.readFrom(node).get();
        assertThat(list.size(), equalTo(1));
        assertThat(list.get(0).getValue(), equalTo(""));
    }

    @Test
    public void readFromOptionalField() throws Exception {
        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);

        assertFalse(fieldType.readFrom(node).isPresent());

        node.setProperty(PROPERTY, "Value");
        assertThat(fieldType.readFrom(node).get().get(0).getValue(), equalTo("Value"));
    }

    @Test
    public void readFromOptionalIncorrectField() throws Exception {
        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);

        node.setProperty(PROPERTY, new String[0]);
        assertFalse(fieldType.readFrom(node).isPresent());

        node.setProperty(PROPERTY, new String[]{"Value", "Ignore"});
        assertThat(fieldType.readFrom(node).get().get(0).getValue(), equalTo("Value"));
    }

    @Test
    public void readFromMultipleField() throws Exception {
        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);

        assertFalse(fieldType.readFrom(node).isPresent());

        node.setProperty(PROPERTY, new String[0]);
        assertFalse(fieldType.readFrom(node).isPresent());

        node.setProperty(PROPERTY, new String[]{"Value 1", "Value 2"});
        assertThat(fieldType.readFrom(node).get().get(0).getValue(), equalTo("Value 1"));
        assertThat(fieldType.readFrom(node).get().get(1).getValue(), equalTo("Value 2"));
    }

    @Test
    public void readFromMultipleIncorrectField() throws Exception {
        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);

        node.setProperty(PROPERTY, "Value");
        assertThat(fieldType.readFrom(node).get().get(0).getValue(), equalTo("Value"));

        fieldType.addValidatorName(FieldValidators.REQUIRED);
        node.getProperty(PROPERTY).remove();
        assertThat(fieldType.readFrom(node).get().get(0).getValue(), equalTo(""));

        node.setProperty(PROPERTY, new String[0]);
        assertThat(fieldType.readFrom(node).get().get(0).getValue(), equalTo(""));
    }

    @Test
    public void writeToSingleField() throws Exception {
        fieldType.setId(PROPERTY);
        fieldType.setJcrType(PropertyType.TYPENAME_STRING);
        node.setProperty(PROPERTY, "Old Value");

        try {
            validateAndWriteTo(node, fieldType, Collections.singletonList(null));
            fail("Must not be missing");
        } catch (final BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        try {
            validateAndWriteTo(node, fieldType, Collections.emptyList());
            fail("Must have 1 entry");
        } catch (final BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(Reason.INVALID_DATA));
        }

        try {

            validateAndWriteTo(node, fieldType, Arrays.asList(valueOf("One"), valueOf("Two")));
            fail("Must have 1 entry");
        } catch (final BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(Reason.INVALID_DATA));
        }
        validateAndWriteTo(node, fieldType, listOf(valueOf("New Value")));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("New Value"));
    }

    @Test
    public void writeToOptionalPresentField() throws Exception {
        fieldType.setId(PROPERTY);
        fieldType.setJcrType(PropertyType.TYPENAME_STRING);
        fieldType.setMinValues(0);
        node.setProperty(PROPERTY, "Old Value");

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasProperty(PROPERTY));
        node.setProperty(PROPERTY, "Old Value");

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertFalse(node.hasProperty(PROPERTY));
        node.setProperty(PROPERTY, "Old Value");

        try {
            validateAndWriteTo(node, fieldType, Arrays.asList(valueOf("one"), valueOf("two")));
            fail("Must have length 1");
        } catch (final BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        fieldType.writeTo(node, Optional.of(listOf(valueOf("New Value"))));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("New Value"));
    }

    @Test
    public void writeToOptionalAbsentField() throws Exception {
        fieldType.setId(PROPERTY);
        fieldType.setJcrType(PropertyType.TYPENAME_STRING);
        fieldType.setMinValues(0);

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasProperty(PROPERTY));

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertFalse(node.hasProperty(PROPERTY));

        try {
            validateAndWriteTo(node, fieldType, Arrays.asList(valueOf("one"), valueOf("two")));
            fail("Must have length 1");
        } catch (final BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(Reason.INVALID_DATA));
        }
        assertFalse(node.hasProperty(PROPERTY));

        fieldType.writeTo(node, Optional.of(listOf(valueOf("New Value"))));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("New Value"));
    }

    @Test
    public void writeToMultiplePresentField() throws Exception {
        fieldType.setId(PROPERTY);
        fieldType.setJcrType(PropertyType.TYPENAME_STRING);
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);
        fieldType.setMultiple(true);
        node.setProperty(PROPERTY, new String[]{"Old 1", "Old 2"});

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasProperty(PROPERTY));
        node.setProperty(PROPERTY, new String[]{"Old 1", "Old 2"});

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertFalse(node.hasProperty(PROPERTY));
        node.setProperty(PROPERTY, new String[]{"Old 1", "Old 2"});

        fieldType.writeTo(node, Optional.of(listOf(valueOf("Single Value"))));
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(1));
        node.setProperty(PROPERTY, new String[]{"Old 1", "Old 2"});

        fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("One"), valueOf("Two"), valueOf("Three"))));
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(3));
        node.setProperty(PROPERTY, new String[]{"Old 1", "Old 2"});

        fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("New 1"), valueOf(""))));
        assertThat(node.getProperty(PROPERTY).getValues()[0].getString(), equalTo("New 1"));
        assertThat(node.getProperty(PROPERTY).getValues()[1].getString(), equalTo(""));
    }

    @Test
    public void writeToMultipleAbsentField() throws Exception {
        fieldType.setId(PROPERTY);
        fieldType.setJcrType(PropertyType.TYPENAME_STRING);
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);
        fieldType.setMultiple(true);

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasProperty(PROPERTY));

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertFalse(node.hasProperty(PROPERTY));

        fieldType.writeTo(node, Optional.of(listOf(valueOf(""))));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo(""));
        node.getProperty(PROPERTY).remove();

        fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("One"), valueOf("Two"), valueOf("Three"))));
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(3));
    }

    @Test
    public void writeToMultipleIncorrectField() throws Exception {
        fieldType.setId(PROPERTY);
        fieldType.setJcrType(PropertyType.TYPENAME_STRING);
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);
        fieldType.setMultiple(true);

        node.setProperty(PROPERTY, "Old Value"); // singular property in spite of multiple type

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasProperty(PROPERTY));

        node.setProperty(PROPERTY, "Old Value"); // singular property in spite of multiple type

        fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("One"), valueOf("Two"))));
        assertTrue(node.getProperty(PROPERTY).isMultiple());

        node.setProperty(PROPERTY, "Old Value"); // singular property in spite of multiple type
    }

    @Test
    public void writeToSingleIncorrectField() throws Exception {
        fieldType.setId(PROPERTY);
        fieldType.setJcrType(PropertyType.TYPENAME_STRING);
        node.setProperty(PROPERTY, new String[]{"Old Value"}); // multiple property in spite of singular type

        fieldType.writeTo(node, Optional.of(listOf(valueOf("New Value"))));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("New Value"));
    }

    @Test
    public void writeToMultipleEmptyField() throws Exception {
        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);
        node.setProperty(PROPERTY, new String[0]); // multiple, empty property

        fieldType.writeTo(node, Optional.empty());
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(0)); // multiple property still there

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(0)); // multiple property still there
    }

    @Test
    public void readFromException() throws Exception {
        final Node mockedNode = createMock(Node.class);

        fieldType.setId(PROPERTY);
        fieldType.setId(PROPERTY);
        expect(mockedNode.hasProperty(PROPERTY)).andThrow(new RepositoryException());
        expect(JcrUtils.getNodePathQuietly(mockedNode)).andReturn("bla");
        replayAll();

        assertThat(fieldType.readFrom(mockedNode).get().get(0).getValue(), equalTo(""));
    }

    @Test
    public void writeToSingleException() throws Exception {
        final Node mockedNode = createMock(Node.class);

        fieldType.setId(PROPERTY);
        fieldType.setJcrType(PropertyType.TYPENAME_STRING);
        expect(mockedNode.hasProperty(PROPERTY)).andReturn(false);
        expect(mockedNode.setProperty(PROPERTY, "New Value", PropertyType.STRING)).andThrow(new RepositoryException());
        replayAll();

        try {
            fieldType.writeTo(mockedNode, Optional.of(listOf(valueOf("New Value"))));
            fail("Exception not thrown");
        } catch (InternalServerErrorException e) {
            assertNull(e.getPayload());
        }
        verifyAll();
    }

    @Test
    public void writeToMultipleException() throws Exception {
        final Node mockedNode = createMock(Node.class);

        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);
        expect(mockedNode.hasProperty(PROPERTY)).andThrow(new RepositoryException());
        replayAll();

        try {
            fieldType.writeTo(mockedNode, Optional.empty());
            fail("Exception not thrown");
        } catch (final InternalServerErrorException e) {
            assertNull(e.getPayload());
        }
        verifyAll();
    }

    @Test
    public void writeSingleOnExistingMultipleProperty() throws Exception {
        final Property replacedProperty = node.setProperty(PROPERTY, new String[]{"Value1", "Value2"});

        fieldType.setId(PROPERTY);
        fieldType.setJcrType(PropertyType.TYPENAME_STRING);
        fieldType.setMultiple(false);
        fieldType.writeTo(node, Optional.of(listOf(valueOf("New Value"))));

        assertFalse(node.getProperty(PROPERTY).isSame(replacedProperty));
        assertFalse(node.getProperty(PROPERTY).isMultiple());
    }

    @Test
    public void writeMultipleOnExistingSingleProperty() throws Exception {
        final Property replacedProperty = node.setProperty(PROPERTY, "Value");

        fieldType.setId(PROPERTY);
        fieldType.setJcrType(PropertyType.TYPENAME_STRING);
        fieldType.setMaxValues(2);
        fieldType.setMultiple(true);
        fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("New Value1"), valueOf("New Value2"))));

        assertFalse(node.getProperty(PROPERTY).isSame(replacedProperty));
        assertTrue(node.getProperty(PROPERTY).isMultiple());
    }

    @Test
    public void writeFieldOtherId() throws ErrorWithPayloadException {
        fieldType.setId(PROPERTY);

        final FieldPath fieldPath = new FieldPath("other:id");
        final List<FieldValue> fieldValues = Collections.emptyList();
        replayAll();

        assertFalse(fieldType.writeField(fieldPath, fieldValues, null));
        verifyAll();
    }

    @Test
    public void writeFieldSuccess() throws ErrorWithPayloadException, RepositoryException {
        fieldType.setId(PROPERTY);
        fieldType.setJcrType(PropertyType.TYPENAME_STRING);

        final FieldPath fieldPath = new FieldPath(PROPERTY);
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("value"));

        assertTrue(fieldType.writeField(fieldPath, fieldValues, nodeContext));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("value"));
    }

    @Test
    public void writeFieldValidates() throws ErrorWithPayloadException, RepositoryException {
        fieldType.setId(PROPERTY);
        fieldType.setJcrType(PropertyType.TYPENAME_STRING);
        fieldType.addValidatorName(FieldValidators.REQUIRED);

        final FieldPath fieldPath = new FieldPath(PROPERTY);
        final FieldValue emptyValue = new FieldValue("");

        mockStaticPartial(FieldTypeUtils.class, "getValidator");
        expect(FieldTypeUtils.getValidator(FieldValidators.REQUIRED)).andReturn(new TestValidatorInstance(new AlwaysGoodTestValidator()));
        replayAll();

        final boolean valueWritten = fieldType.writeField(fieldPath, Collections.singletonList(emptyValue), nodeContext);

        assertTrue(valueWritten);
        assertThat(node.getProperty(PROPERTY).getString(), equalTo(""));
        verifyAll();
    }

    @Test
    public void validateEmpty() {
        try {
            validateAndWriteTo(node, fieldType, Collections.singletonList(null));
            fail("Must not be missing");
        } catch (final BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
    }

    @Test
    public void validateGood() {
        assertZeroViolations(fieldType.validate(listOf(valueOf("")), null));
        assertZeroViolations(fieldType.validate(listOf(valueOf("blabla")), null));
        fieldType.setMaxValues(2);
        assertZeroViolations(fieldType.validate(Arrays.asList(valueOf("one"), valueOf("two")), null));
    }

    @Test
    public void validateBad() {
        mockValidators();

        fieldType.addValidatorName("always-bad");
        final FieldValue test = new FieldValue("test");

        final CompoundContext context = new CompoundContext(MockNode.root(), null, null, null);
        assertViolation(fieldType.validateValue(test, context));
        assertThat(test.getErrorInfo().getValidation(), equalTo("always-bad"));
    }

    @Test
    public void validateSecondBad() {
        mockValidators();

        fieldType.addValidatorName("always-good");
        fieldType.addValidatorName("always-bad");
        final FieldValue test = new FieldValue("test");

        final CompoundContext context = new CompoundContext(MockNode.root(), null, null, null);
        assertViolation(fieldType.validateValue(test, context));
        assertThat(test.getErrorInfo().getValidation(), equalTo("always-bad"));
    }

    @Test
    public void firstViolationIsReported() {
        mockValidators();

        fieldType.addValidatorName("non-empty");
        fieldType.addValidatorName("always-bad");
        final FieldValue test = new FieldValue(""); // empty value should trigger non-empty validator

        final CompoundContext context = new CompoundContext(MockNode.root(), null, null, null);
        assertViolation(fieldType.validateValue(test, context));
        assertThat(test.getErrorInfo().getValidation(), equalTo("non-empty")); // and not "always-bad"
    }

    @Test
    public void ignoreUnknownValidators() {
        mockValidators();

        fieldType.addValidatorName("unknown");
        fieldType.addValidatorName("always-good");
        final FieldValue test = new FieldValue("test");

        final CompoundContext context = new CompoundContext(MockNode.root(), null, null, null);
        assertZeroViolations(fieldType.validateValue(test, context));
    }

    private static void mockValidators() {
        PowerMock.mockStaticPartial(FieldTypeUtils.class, "getValidator");
        expect(FieldTypeUtils.getValidator(eq("always-good")))
                .andReturn(new TestValidatorInstance(new AlwaysGoodTestValidator()))
                .anyTimes();
        expect(FieldTypeUtils.getValidator(eq("always-bad")))
                .andReturn(new TestValidatorInstance(new AlwaysBadTestValidator()))
                .anyTimes();
        expect(FieldTypeUtils.getValidator(eq("non-empty")))
                .andReturn(new TestValidatorInstance(new NonEmptyTestValidator()))
                .anyTimes();
        expect(FieldTypeUtils.getValidator(eq("unknown")))
                .andReturn(null)
                .anyTimes();
        replayAll();
    }

    private List<FieldValue> listOf(final FieldValue value) {
        return Collections.singletonList(value);
    }

    private FieldValue valueOf(final String value) {
        return new FieldValue(value);
    }
}
