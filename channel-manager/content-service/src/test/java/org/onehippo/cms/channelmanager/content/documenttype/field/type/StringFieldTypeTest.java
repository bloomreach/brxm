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

package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.field.validation.ValidationErrorInfo;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.repository.mock.MockNode;
import org.powermock.api.easymock.PowerMock;
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
@PrepareForTest(JcrUtils.class)
public class StringFieldTypeTest {

    private static final String PROPERTY = "test:id";

    @Test
    public void readFromSingleString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        node.setProperty(PROPERTY, "Value");

        final List<FieldValue> list = fieldType.readFrom(node).get();
        assertThat(list.size(), equalTo(1));
        assertThat(list.get(0).getValue(), equalTo("Value"));
    }

    @Test
    public void readFromSingleIncorrectString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

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
    public void readFromOptionalString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);

        assertFalse(fieldType.readFrom(node).isPresent());

        node.setProperty(PROPERTY, "Value");
        assertThat(fieldType.readFrom(node).get().get(0).getValue(), equalTo("Value"));
    }

    @Test
    public void readFromOptionalIncorrectString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);

        node.setProperty(PROPERTY, new String[0]);
        assertFalse(fieldType.readFrom(node).isPresent());

        node.setProperty(PROPERTY, new String[]{"Value", "Ignore"});
        assertThat(fieldType.readFrom(node).get().get(0).getValue(), equalTo("Value"));
    }

    @Test
    public void readFromMultipleString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

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
    public void readFromMultipleIncorrectString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);

        node.setProperty(PROPERTY, "Value");
        assertThat(fieldType.readFrom(node).get().get(0).getValue(), equalTo("Value"));

        fieldType.addValidator(FieldType.Validator.REQUIRED);
        node.getProperty(PROPERTY).remove();
        assertThat(fieldType.readFrom(node).get().get(0).getValue(), equalTo(""));

        node.setProperty(PROPERTY, new String[0]);
        assertThat(fieldType.readFrom(node).get().get(0).getValue(), equalTo(""));
    }

    @Test
    public void readFromException() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = createMock(Node.class);

        PowerMock.mockStaticPartial(JcrUtils.class, "getNodePathQuietly");

        fieldType.setId(PROPERTY);
        expect(node.hasProperty(PROPERTY)).andThrow(new RepositoryException());
        expect(JcrUtils.getNodePathQuietly(node)).andReturn("bla");
        replay(node);
        PowerMock.replayAll();

        assertThat(fieldType.readFrom(node).get().get(0).getValue(), equalTo(""));
    }


    @Test
    public void writeToSingleString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        node.setProperty(PROPERTY, "Old Value");

        try {
            fieldType.writeTo(node, Optional.empty());
            fail("Must not be missing");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        try {
            fieldType.writeTo(node, Optional.of(Boolean.TRUE));
            fail("Must be List");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        try {
            fieldType.writeTo(node, Optional.of(Collections.emptyList()));
            fail("Must have 1 entry");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("One"), valueOf("Two"))));
            fail("Must have 1 entry");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }

        try {
            fieldType.writeTo(node, Optional.of(Collections.singletonList(Boolean.TRUE)));
            fail("Entries must be FieldValue");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }

        fieldType.writeTo(node, Optional.of(listOf(valueOf("New Value"))));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("New Value"));
    }

    @Test
    public void writeToOptionalPresentString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);
        node.setProperty(PROPERTY, "Old Value");

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasProperty(PROPERTY));
        node.setProperty(PROPERTY, "Old Value");

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertFalse(node.hasProperty(PROPERTY));
        node.setProperty(PROPERTY, "Old Value");

        try {
            fieldType.writeTo(node, Optional.of("New Value"));
            fail("Must be of type List");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("one"), valueOf("two"))));
            fail("Must have length 1");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        try {
            fieldType.writeTo(node, Optional.of(Collections.singletonList(Boolean.TRUE)));
            fail("Element must be of type FieldValue");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        fieldType.writeTo(node, Optional.of(listOf(valueOf("New Value"))));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("New Value"));
    }

    @Test
    public void writeToOptionalAbsentString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasProperty(PROPERTY));

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertFalse(node.hasProperty(PROPERTY));

        try {
            fieldType.writeTo(node, Optional.of("New Value"));
            fail("Must be of type List");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertFalse(node.hasProperty(PROPERTY));

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("one"), valueOf("two"))));
            fail("Must have length 1");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertFalse(node.hasProperty(PROPERTY));

        try {
            fieldType.writeTo(node, Optional.of(Collections.singletonList(Boolean.TRUE)));
            fail("Element must be of type String");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertFalse(node.hasProperty(PROPERTY));

        fieldType.writeTo(node, Optional.of(listOf(valueOf("New Value"))));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("New Value"));
    }

    @Test
    public void writeToMultiplePresentString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);
        node.setProperty(PROPERTY, new String[]{"Old 1", "Old 2"});

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasProperty(PROPERTY));
        node.setProperty(PROPERTY, new String[]{"Old 1", "Old 2"});

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertFalse(node.hasProperty(PROPERTY));
        node.setProperty(PROPERTY, new String[]{"Old 1", "Old 2"});

        try {
            fieldType.writeTo(node, Optional.of("New 1"));
            fail("Must be of type List");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(2));

        fieldType.writeTo(node, Optional.of(listOf(valueOf("Single Value"))));
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(1));
        node.setProperty(PROPERTY, new String[]{"Old 1", "Old 2"});

        fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("One"), valueOf("Two"), valueOf("Three"))));
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(3));
        node.setProperty(PROPERTY, new String[]{"Old 1", "Old 2"});

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(Boolean.TRUE, Boolean.TRUE)));
            fail("Element must be of type FieldValue");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getValues()[0].getString(), equalTo("Old 1"));

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("New 1"), Boolean.TRUE)));
            fail("...all of them!");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getValues()[0].getString(), equalTo("Old 1"));

        fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("New 1"), valueOf(""))));
        assertThat(node.getProperty(PROPERTY).getValues()[0].getString(), equalTo("New 1"));
        assertThat(node.getProperty(PROPERTY).getValues()[1].getString(), equalTo(""));
    }

    @Test
    public void writeToMultipleAbsentString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasProperty(PROPERTY));

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertFalse(node.hasProperty(PROPERTY));

        try {
            fieldType.writeTo(node, Optional.of("New 1"));
            fail("Must be List");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertFalse(node.hasProperty(PROPERTY));

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(Boolean.TRUE, Boolean.TRUE)));
            fail("Element must be of type FieldValue");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertFalse(node.hasProperty(PROPERTY));

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("New 1"), Boolean.TRUE)));
            fail("...all of them!");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertFalse(node.hasProperty(PROPERTY));

        fieldType.writeTo(node, Optional.of(listOf(valueOf(""))));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo(""));
        node.getProperty(PROPERTY).remove();

        fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("One"), valueOf("Two"), valueOf("Three"))));
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(3));
    }

    @Test
    public void writeToMultipleIncorrectString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);
        node.setProperty(PROPERTY, "Old Value"); // singular property in spite of multiple type

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasProperty(PROPERTY));
        node.setProperty(PROPERTY, "Old Value"); // singular property in spite of multiple type

        fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("One"), valueOf("Two"))));
        assertTrue(node.getProperty(PROPERTY).isMultiple());
        node.setProperty(PROPERTY, "Old Value"); // singular property in spite of multiple type

        try {
            fieldType.writeTo(node, Optional.of("New Value"));
            fail("Must be List");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));
    }

    @Test
    public void writeToSingleIncorrectString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        node.setProperty(PROPERTY, new String[]{"Old Value"}); // multiple property in spite of singular type

        fieldType.writeTo(node, Optional.of(listOf(valueOf("New Value"))));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("New Value"));
    }

    @Test
    public void writeToMultipleEmptyString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

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
    public void writeToSingleException() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = createMock(Node.class);

        fieldType.setId(PROPERTY);
        expect(node.setProperty(PROPERTY, "New Value")).andThrow(new RepositoryException());
        replay(node);

        try {
            fieldType.writeTo(node, Optional.of(listOf(valueOf("New Value"))));
            fail("Exception not thrown");
        } catch (InternalServerErrorException e) {
            assertNull(e.getPayload());
        }
        verify(node);
    }

    @Test
    public void writeToMultipleException() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
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
    public void validateNonRequired() {
        final StringFieldType fieldType = new StringFieldType();

        assertTrue(fieldType.validate(Collections.emptyList()));
        assertTrue(fieldType.validate(listOf(valueOf(""))));
        assertTrue(fieldType.validate(listOf(valueOf("blabla"))));
        assertTrue(fieldType.validate(Arrays.asList(valueOf("one"), valueOf("two"))));
    }

    @Test
    public void validateRequired() {
        final StringFieldType fieldType = new StringFieldType();

        fieldType.addValidator(FieldType.Validator.REQUIRED);

        // valid values
        assertTrue(fieldType.validate(listOf(valueOf("one"))));
        assertTrue(fieldType.validate(Arrays.asList(valueOf("one"), valueOf("two"))));

        // invalid values
        FieldValue v = valueOf("");
        assertFalse(fieldType.validate(listOf(v)));
        assertThat(v.getErrorInfo().getCode(), equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));

        List<FieldValue> l = Arrays.asList(valueOf("bla"), valueOf(""));
        assertFalse(fieldType.validate(l));
        assertFalse(l.get(0).hasErrorInfo());
        assertThat(l.get(1).getErrorInfo().getCode(), equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));

        l = Arrays.asList(valueOf(""), valueOf("bla"));
        assertFalse(fieldType.validate(l));
        assertThat(l.get(0).getErrorInfo().getCode(), equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));
        assertFalse(l.get(1).hasErrorInfo());

        l = Arrays.asList(valueOf(""), valueOf(""));
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
