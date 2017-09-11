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
import org.onehippo.cms.channelmanager.content.documenttype.field.validation.ValidationErrorInfo;
import org.onehippo.cms.channelmanager.content.documenttype.util.NamespaceUtils;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.repository.mock.MockNode;
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({JcrUtils.class, NamespaceUtils.class})
public class LongFieldTypeTest {

    private static final String PROPERTY = "test:id";

    @Before
    public void setup() {
        PowerMock.mockStatic(JcrUtils.class);
        PowerMock.mockStatic(NamespaceUtils.class);
    }

    @Test
    public void readFromSingleLong() throws Exception {
        final LongFieldType fieldType = new LongFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        node.setProperty(PROPERTY, 10);

        final List<FieldValue> list = fieldType.readFrom(node).get();
        assertThat(list.size(), equalTo(1));
        assertThat(list.get(0).getValue(), equalTo("10"));
    }

    @Test
    public void readFromSingleIncorrectLong() throws Exception {
        final LongFieldType fieldType = new LongFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);

        node.setProperty(PROPERTY, new String[]{"10", "20"}, PropertyType.LONG);
        List<FieldValue> list = fieldType.readFrom(node).get();
        assertThat(list.size(), equalTo(1));
        assertThat(list.get(0).getValue(), equalTo("10"));

        node.setProperty(PROPERTY, new String[0], PropertyType.LONG);
        list = fieldType.readFrom(node).get();
        assertThat(list.size(), equalTo(1));
        assertThat(list.get(0).getValue(), equalTo("0"));

        node.getProperty(PROPERTY).remove();
        list = fieldType.readFrom(node).get();
        assertThat(list.size(), equalTo(1));
        assertThat(list.get(0).getValue(), equalTo("0"));
    }

    @Test
    public void readFromOptionalLong() throws Exception {
        final LongFieldType fieldType = new LongFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);

        assertFalse(fieldType.readFrom(node).isPresent());

        node.setProperty(PROPERTY, 10);
        assertThat(fieldType.readFrom(node).get().get(0).getValue(), equalTo("10"));
    }

    @Test
    public void readFromOptionalIncorrectLong() throws Exception {
        final LongFieldType fieldType = new LongFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);

        node.setProperty(PROPERTY, new String[0], PropertyType.LONG);
        assertFalse(fieldType.readFrom(node).isPresent());

        node.setProperty(PROPERTY, new String[]{"10", "20"}, PropertyType.LONG);
        assertThat(fieldType.readFrom(node).get().get(0).getValue(), equalTo("10"));
    }

    @Test
    public void readFromMultipleLong() throws Exception {
        final LongFieldType fieldType = new LongFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);

        assertFalse(fieldType.readFrom(node).isPresent());

        node.setProperty(PROPERTY, new String[0], PropertyType.LONG);
        assertFalse(fieldType.readFrom(node).isPresent());

        node.setProperty(PROPERTY, new String[]{"10", "20"}, PropertyType.LONG);
        assertThat(fieldType.readFrom(node).get().get(0).getValue(), equalTo("10"));
        assertThat(fieldType.readFrom(node).get().get(1).getValue(), equalTo("20"));
    }

    @Test
    public void readFromMultipleIncorrectLong() throws Exception {
        final LongFieldType fieldType = new LongFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);

        node.setProperty(PROPERTY, "10",PropertyType.LONG);
        assertThat(fieldType.readFrom(node).get().get(0).getValue(), equalTo("10"));

        fieldType.addValidator(FieldType.Validator.REQUIRED);
        node.getProperty(PROPERTY).remove();
        assertThat(fieldType.readFrom(node).get().get(0).getValue(), equalTo("0"));

        node.setProperty(PROPERTY, new String[0], PropertyType.LONG);
        assertThat(fieldType.readFrom(node).get().get(0).getValue(), equalTo("0"));
    }

    @Test
    public void readFromException() throws Exception {
        final LongFieldType fieldType = new LongFieldType();
        final Node node = createMock(Node.class);

        fieldType.setId(PROPERTY);
        fieldType.setId(PROPERTY);
        expect(node.hasProperty(PROPERTY)).andThrow(new RepositoryException());
        expect(JcrUtils.getNodePathQuietly(node)).andReturn("bla");
        PowerMock.replayAll(node);

        assertThat(fieldType.readFrom(node).get().get(0).getValue(), equalTo("0"));
    }


    @Test
    public void writeToSingleLong() throws Exception {
        final LongFieldType fieldType = new LongFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        node.setProperty(PROPERTY, "10", PropertyType.LONG);

        try {
            fieldType.writeTo(node, Optional.empty());
            fail("Must not be missing");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("10"));

        try {
            fieldType.writeTo(node, Optional.of(Collections.emptyList()));
            fail("Must have 1 entry");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("10"), valueOf("20"))));
            fail("Must have 1 entry");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }

        fieldType.writeTo(node, Optional.of(listOf(valueOf("20"))));
        assertThat(node.getProperty(PROPERTY).getLong(), equalTo(20L));
    }

    @Test
    public void writeToOptionalPresentLong() throws Exception {
        final LongFieldType fieldType = new LongFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);
        node.setProperty(PROPERTY, 10);

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasProperty(PROPERTY));
        node.setProperty(PROPERTY, 10);

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertFalse(node.hasProperty(PROPERTY));
        node.setProperty(PROPERTY, 10);

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("5"), valueOf("20"))));
            fail("Must have length 1");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("10"));

        fieldType.writeTo(node, Optional.of(listOf(valueOf("20"))));
        assertThat(node.getProperty(PROPERTY).getLong(), equalTo(20L));
    }

    @Test
    public void writeToOptionalAbsentLong() throws Exception {
        final LongFieldType fieldType = new LongFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasProperty(PROPERTY));

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertFalse(node.hasProperty(PROPERTY));

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("5"), valueOf("10"))));
            fail("Must have length 1");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertFalse(node.hasProperty(PROPERTY));

        fieldType.writeTo(node, Optional.of(listOf(valueOf("10"))));
        assertThat(node.getProperty(PROPERTY).getLong(), equalTo(10L));
    }

    @Test
    public void writeToMultiplePresentLong() throws Exception {
        final LongFieldType fieldType = new LongFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);
        fieldType.setMultiple(true);
        node.setProperty(PROPERTY, new String[]{"10", "20"}, PropertyType.LONG);

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasProperty(PROPERTY));
        node.setProperty(PROPERTY, new String[]{"10", "20"}, PropertyType.LONG);

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertFalse(node.hasProperty(PROPERTY));
        node.setProperty(PROPERTY, new String[]{"10", "20"}, PropertyType.LONG);

        fieldType.writeTo(node, Optional.of(listOf(valueOf("5"))));
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(1));
        node.setProperty(PROPERTY, new String[]{"10", "20"}, PropertyType.LONG);

        fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("5"), valueOf("10"), valueOf("20"))));
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(3));
    }

    @Test
    public void writeToMultipleAbsentLong() throws Exception {
        final LongFieldType fieldType = new LongFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);
        fieldType.setMultiple(true);

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasProperty(PROPERTY));

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertFalse(node.hasProperty(PROPERTY));

        // TODO: the MockValue impl does not throw a ValueFormatExceptoin when writing an empty string into a
        // long property but the actual Value impl does. In production we get the expected behavior (value is not set)
        // but in the test code the value is set to en empty string, which in turn throws when calling MockValue#getLong
        fieldType.writeTo(node, Optional.of(listOf(valueOf(""))));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo(""));
        node.getProperty(PROPERTY).remove();

        fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("5"), valueOf("10"), valueOf("20"))));
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(3));
    }

    @Test
    public void writeToMultipleIncorrectLong() throws Exception {
        final LongFieldType fieldType = new LongFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);
        fieldType.setMultiple(true);

        node.setProperty(PROPERTY, 10); // singular property in spite of multiple type

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasProperty(PROPERTY));
        node.setProperty(PROPERTY, 10); // singular property in spite of multiple type

        fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("5"), valueOf("20"))));
        assertTrue(node.getProperty(PROPERTY).isMultiple());
        node.setProperty(PROPERTY, 10); // singular property in spite of multiple type
    }

    @Test
    public void writeToSingleIncorrectLong() throws Exception {
        final LongFieldType fieldType = new LongFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        node.setProperty(PROPERTY, new String[]{"10"}, PropertyType.LONG); // multiple property in spite of singular type

        fieldType.writeTo(node, Optional.of(listOf(valueOf("20"))));
        assertThat(node.getProperty(PROPERTY).getLong(), equalTo(20L));
    }

    @Test
    public void writeToMultipleEmptyLong() throws Exception {
        final LongFieldType fieldType = new LongFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);
        node.setProperty(PROPERTY, new String[0], PropertyType.LONG); // multiple, empty property

        fieldType.writeTo(node, Optional.empty());
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(0)); // multiple property still there

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(0)); // multiple property still there
    }

    @Test
    public void writeToSingleException() throws Exception {
        final LongFieldType fieldType = new LongFieldType();
        final Node node = createMock(Node.class);

        fieldType.setId(PROPERTY);
        expect(node.hasProperty(PROPERTY)).andReturn(false);
        expect(node.setProperty(PROPERTY, "10", PropertyType.LONG)).andThrow(new RepositoryException());
        replay(node);

        try {
            fieldType.writeTo(node, Optional.of(listOf(valueOf("10"))));
            fail("Exception not thrown");
        } catch (InternalServerErrorException e) {
            assertNull(e.getPayload());
        }
        verify(node);
    }

    @Test
    public void writeToMultipleException() throws Exception {
        final LongFieldType fieldType = new LongFieldType();
        final Node node = createMock(Node.class);

        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);
        expect(node.hasProperty(PROPERTY)).andThrow(new RepositoryException());
        replay(node);

        try {
            fieldType.writeTo(node, Optional.empty());
            fail("Exception not thrown");
        } catch (InternalServerErrorException e) {
            assertNull(e.getPayload());
        }
        verify(node);
    }

    @Test
    public void writeSingleOnExistingMultipleProperty() throws Exception {
        final LongFieldType fieldType = new LongFieldType();
        final Node node = MockNode.root();
        final Property propertyThatWillBeReplaced = node.setProperty(PROPERTY, new String[]{"10", "20"}, PropertyType.LONG);

        fieldType.setMultiple(false);
        fieldType.setId(PROPERTY);
        fieldType.writeTo(node, Optional.of(listOf(valueOf("5"))));

        assertFalse(node.getProperty(PROPERTY).isSame(propertyThatWillBeReplaced));
        assertFalse(node.getProperty(PROPERTY).isMultiple());
    }

    @Test
    public void writeMultipleOnExistingSingleProperty() throws Exception {
        final LongFieldType fieldType = new LongFieldType();
        final Node node = MockNode.root();
        final Property propertyThatWillBeReplaced = node.setProperty(PROPERTY, 10);

        fieldType.setMaxValues(2);
        fieldType.setMultiple(true);
        fieldType.setId(PROPERTY);
        fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("5"), valueOf("20"))));

        assertFalse(node.getProperty(PROPERTY).isSame(propertyThatWillBeReplaced));
        assertTrue(node.getProperty(PROPERTY).isMultiple());
    }

    @Test
    public void writeFieldOtherId() throws ErrorWithPayloadException {
        final LongFieldType fieldType = new LongFieldType();
        fieldType.setId(PROPERTY);

        final Node node = createMock(Node.class);
        final FieldPath fieldPath = new FieldPath("other:id");
        final List<FieldValue> fieldValues = Collections.emptyList();
        replay(node);

        assertFalse(fieldType.writeField(node, fieldPath, fieldValues));
        verify(node);
    }

    @Test
    public void writeFieldSuccess() throws ErrorWithPayloadException, RepositoryException {
        final LongFieldType fieldType = new LongFieldType();
        fieldType.setId(PROPERTY);

        final Node node = MockNode.root();
        final FieldPath fieldPath = new FieldPath(PROPERTY);
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("10"));

        assertTrue(fieldType.writeField(node, fieldPath, fieldValues));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("10"));
    }

    @Test
    public void writeFieldDoesNotValidate() throws ErrorWithPayloadException, RepositoryException {
        final LongFieldType fieldType = new LongFieldType();
        fieldType.setId(PROPERTY);
        fieldType.addValidator(FieldType.Validator.REQUIRED);

        final Node node = MockNode.root();
        final FieldPath fieldPath = new FieldPath(PROPERTY);
        final FieldValue emptyValue = new FieldValue("");

        assertTrue(fieldType.writeField(node, fieldPath, Collections.singletonList(emptyValue)));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo(""));
    }

    // @Test
    // TODO: this code works in production, but not in the test; the MockValueFactory never throws a ValueFormatException
    // when calling MockValueFactory#createValue(String value, int type) as it uses the 'stringified' constructor of
    // MockValue, which then throws a ValueFormatException when calling getLong()...
    public void writeNumbersOnly() throws ErrorWithPayloadException, RepositoryException {
        final LongFieldType fieldType = new LongFieldType();
        fieldType.setId(PROPERTY);

        final Node node = MockNode.root();
        node.setProperty(PROPERTY, 10);
        fieldType.writeTo(node, Optional.of(listOf(valueOf("abc"))));
        assertThat(node.getProperty(PROPERTY).getLong(), equalTo(10L));
    }


    @Test
    public void validateNonRequired() {
        final LongFieldType fieldType = new LongFieldType();

        assertTrue(fieldType.validate(Collections.emptyList()));
        assertTrue(fieldType.validate(listOf(valueOf(""))));
        assertTrue(fieldType.validate(listOf(valueOf("5"))));
        assertTrue(fieldType.validate(Arrays.asList(valueOf("10"), valueOf("20"))));
    }

    @Test
    public void validateRequired() {
        final LongFieldType fieldType = new LongFieldType();

        fieldType.addValidator(FieldType.Validator.REQUIRED);

        // valid values
        assertTrue(fieldType.validate(listOf(valueOf("5"))));
        assertTrue(fieldType.validate(Arrays.asList(valueOf("10"), valueOf("20"))));

        // invalid values
        FieldValue v = valueOf("");
        assertFalse(fieldType.validate(listOf(v)));
        assertThat(v.getErrorInfo().getCode(), equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));

        v = valueOf(null);
        assertFalse(fieldType.validate(listOf(v)));
        assertThat(v.getErrorInfo().getCode(), equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));

        List<FieldValue> l = Arrays.asList(valueOf("10"), valueOf(""));
        assertFalse(fieldType.validate(l));
        assertFalse(l.get(0).hasErrorInfo());
        assertThat(l.get(1).getErrorInfo().getCode(), equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));

        l = Arrays.asList(valueOf("10"), valueOf(null));
        assertFalse(fieldType.validate(l));
        assertFalse(l.get(0).hasErrorInfo());
        assertThat(l.get(1).getErrorInfo().getCode(), equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));

        l = Arrays.asList(valueOf(""), valueOf("10"));
        assertFalse(fieldType.validate(l));
        assertThat(l.get(0).getErrorInfo().getCode(), equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));
        assertFalse(l.get(1).hasErrorInfo());

        l = Arrays.asList(valueOf(null), valueOf("10"));
        assertFalse(fieldType.validate(l));
        assertThat(l.get(0).getErrorInfo().getCode(), equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));
        assertFalse(l.get(1).hasErrorInfo());

        l = Arrays.asList(valueOf(""), valueOf(""));
        assertFalse(fieldType.validate(l));
        assertThat(l.get(0).getErrorInfo().getCode(), equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));
        assertThat(l.get(1).getErrorInfo().getCode(), equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));

        l = Arrays.asList(valueOf(null), valueOf(null));
        assertFalse(fieldType.validate(l));
        assertThat(l.get(0).getErrorInfo().getCode(), equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));
        assertThat(l.get(1).getErrorInfo().getCode(), equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));
    }

    private List<FieldValue> listOf(final FieldValue value) {
        return Collections.singletonList(value);
    }

    private FieldValue valueOf(final String value) {
        return new FieldValue(value);
    }
}
