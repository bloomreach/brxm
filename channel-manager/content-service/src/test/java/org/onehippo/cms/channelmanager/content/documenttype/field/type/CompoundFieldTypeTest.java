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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.field.validation.ValidationErrorInfo;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.repository.mock.MockNode;

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

public class CompoundFieldTypeTest {

    private static final String NODE_NAME = "node:name";
    private static final String STRING_PROPERTY_1 = "string:field1";
    private static final String STRING_PROPERTY_2 = "string:field2";

    private CompoundFieldType fieldType;
    private FieldType stringField1;
    private FieldType stringField2;
    private CompoundFieldType compoundField;
    private Node node;

    @Before
    public void setup() {
        stringField1 = new StringFieldType();
        stringField1.setId(STRING_PROPERTY_1);

        stringField2 = new StringFieldType();
        stringField2.setId(STRING_PROPERTY_2);

        compoundField = new CompoundFieldType();
        compoundField.setId("compound:field");
        compoundField.setMinValues(0);

        fieldType = new CompoundFieldType();
        fieldType.setId(NODE_NAME);
        fieldType.getFields().add(stringField1);
        fieldType.getFields().add(stringField2);
        fieldType.getFields().add(compoundField);

        node = MockNode.root();
    }

    @Test
    public void isValid() {
        assertTrue(fieldType.isValid());

        CompoundFieldType empty = new CompoundFieldType();
        assertFalse(empty.isValid());

        fieldType.addValidator(FieldType.Validator.UNSUPPORTED);
        assertFalse(fieldType.isValid());
    }

    @Test
    public void readFromSinglePresentCompound() throws Exception {
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value");

        final List<FieldValue> list = fieldType.readFrom(node).get();
        assertThat(list.size(), equalTo(1));
        final Map<String, List<FieldValue>> map = list.get(0).getFields();
        assertThat(map.get(STRING_PROPERTY_1).get(0).getValue(), equalTo("Value"));
        assertFalse(map.containsKey("compound:field"));
    }

    @Test
    public void readFromSingleAbsentCompound() throws Exception {
        assertFalse(fieldType.readFrom(node).isPresent());
    }

    @Test
    public void readFromSingleCompoundWhen2Present() throws Exception {
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value 1");
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value 2");

        final List<FieldValue> values = fieldType.readFrom(node).get();
        assertThat(values.size(), equalTo(1));
        final Map<String, List<FieldValue>> map = values.get(0).getFields();
        assertThat(map.get(STRING_PROPERTY_1).get(0).getValue(), equalTo("Value 1"));
        assertFalse(map.containsKey("compound:field"));
    }

    @Test
    public void readFromOptionalPresentCompound() throws Exception {
        fieldType.setMinValues(0);
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value");

        final List<FieldValue> values = fieldType.readFrom(node).get();
        assertThat(values.size(), equalTo(1));
        final Map<String, List<FieldValue>> map = values.get(0).getFields();
        assertThat(map.get(STRING_PROPERTY_1).get(0).getValue(), equalTo("Value"));
        assertFalse(map.containsKey("compound:field"));
    }

    @Test
    public void readFromOptionalAbsentCompound() throws Exception {
        fieldType.setMinValues(0);

        assertFalse(fieldType.readFrom(node).isPresent());
    }

    @Test
    public void readFromOptionalCompoundWhen2Present() throws Exception {
        fieldType.setMinValues(0);
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value 1");
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value 2");

        final List<FieldValue> values = fieldType.readFrom(node).get();
        assertThat(values.size(), equalTo(1));
        final Map<String, List<FieldValue>> map = values.get(0).getFields();
        assertThat(map.get(STRING_PROPERTY_1).get(0).getValue(), equalTo("Value 1"));
    }

    @Test
    public void readFromMultiplePresentCompound() throws Exception {
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value");

        final List<FieldValue> values = fieldType.readFrom(node).get();
        assertThat(values.size(), equalTo(1));
        final Map<String, List<FieldValue>> map = values.get(0).getFields();
        assertThat(map.get(STRING_PROPERTY_1).get(0).getValue(), equalTo("Value"));
        assertFalse(map.containsKey("compound:field"));
    }

    @Test
    public void readFromMultipleAbsentCompound() throws Exception {
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);

        assertFalse(fieldType.readFrom(node).isPresent());
    }

    @Test
    public void readFromMultipleCompoundWhen2Present() throws Exception {
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value 1");
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value 2");

        final List<FieldValue> values = fieldType.readFrom(node).get();
        assertThat(values.size(), equalTo(2));
        final Map<String, List<FieldValue>> map1 = values.get(0).getFields();
        assertThat(map1.get(STRING_PROPERTY_1).get(0).getValue(), equalTo("Value 1"));
        final Map<String, List<FieldValue>> map2 = values.get(1).getFields();
        assertThat(map2.get(STRING_PROPERTY_1).get(0).getValue(), equalTo("Value 2"));
    }

    @Test
    public void readFromException() throws Exception {
        final Node node = createMock(Node.class);

        expect(node.getNodes(NODE_NAME)).andThrow(new RepositoryException());
        replay(node);

        assertFalse(fieldType.readFrom(node).isPresent());
        verify(node);
    }

    @Test
    public void writeToSinglePresentCompound() throws Exception {
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value");

        try {
            fieldType.writeTo(node, Optional.empty());
            fail("Must have value");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        try {
            fieldType.writeTo(node, Optional.of(Collections.emptyList()));
            fail("Must have value");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        fieldType.writeTo(node, Optional.of(listOf(valueOf(validCompound()))));
        assertTrue(isWrittenSuccessfully(node.getNode(NODE_NAME)));
    }

    @Test
    public void writeToSingleAbsentCompound() throws Exception {
        try {
            fieldType.writeTo(node, Optional.of(listOf(valueOf(validCompound()))));
            fail("Unable to create compound node");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.CARDINALITY_CHANGE));
        }
        assertFalse(node.hasNode(NODE_NAME));
    }

    @Test
    public void writeToSingleCompoundWith2Present() throws Exception {
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value 1");
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value 1");

        fieldType.writeTo(node, Optional.of(listOf(valueOf(validCompound()))));

        NodeIterator iterator = node.getNodes(NODE_NAME);
        assertThat(iterator.getSize(), equalTo(1L));
        assertTrue(isWrittenSuccessfully(iterator.nextNode()));
    }

    @Test
    public void writeToOptionalPresentCompound() throws Exception {
        fieldType.setMinValues(0);

        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value");

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasNode(NODE_NAME));
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value");

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertFalse(node.hasNode(NODE_NAME));
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value");

        Map<String, List<FieldValue>> map = new HashMap<>();
        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf(map), valueOf(map))));
            fail("Must not be more than 1 value");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        try {
            fieldType.writeTo(node, Optional.of(listOf(valueOf(map))));
            fail("Map values must be accepted by sub-fields");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        fieldType.writeTo(node, Optional.of(listOf(valueOf(validCompound()))));
        assertTrue(isWrittenSuccessfully(node.getNode(NODE_NAME)));
    }

    @Test
    public void writeToOptionalAbsentCompound() throws Exception {
        fieldType.setMinValues(0);

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasNode(NODE_NAME));

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertFalse(node.hasNode(NODE_NAME));

        try {
            fieldType.writeTo(node, Optional.of(listOf(valueOf(validCompound()))));
            fail("Can't create new node");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.CARDINALITY_CHANGE));
        }
        assertFalse(node.hasNode(NODE_NAME));
    }

    @Test
    public void writeToOptionalCompoundWith2Present() throws Exception {
        fieldType.setMinValues(0);

        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value 1");
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value 2");

        fieldType.writeTo(node, Optional.of(listOf(valueOf(validCompound()))));

        NodeIterator iterator = node.getNodes(NODE_NAME);
        assertThat(iterator.getSize(), equalTo(1L));
        assertTrue(isWrittenSuccessfully(iterator.nextNode()));
    }

    @Test
    public void writeToMultiplePresentCompound() throws Exception {
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);

        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value");

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasNode(NODE_NAME));
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value");

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertFalse(node.hasNode(NODE_NAME));
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value");

        Map<String, List<FieldValue>> map = new HashMap<>();
        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf(map), valueOf(map))));
            fail("Must not be more than 1 value");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.CARDINALITY_CHANGE));
        }
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        try {
            fieldType.writeTo(node, Optional.of(listOf(valueOf(map))));
            fail("Map values must be accepted by sub-fields");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        fieldType.writeTo(node, Optional.of(listOf(valueOf(validCompound()))));
        assertTrue(isWrittenSuccessfully(node.getNode(NODE_NAME)));
    }

    @Test
    public void writeToMultipleAbsentCompound() throws Exception {
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasNode(NODE_NAME));

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertFalse(node.hasNode(NODE_NAME));

        try {
            fieldType.writeTo(node, Optional.of(listOf(valueOf(validCompound()))));
            fail("Cannot create node");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.CARDINALITY_CHANGE));
        }
        assertFalse(node.hasNode(NODE_NAME));
    }

    @Test
    public void writeToMultipleCompoundWith2Present() throws Exception {
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);

        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value 1");
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value 2");

        Map<String, List<FieldValue>> map = validCompound();

        try {
            fieldType.writeTo(node, Optional.of(listOf(valueOf(map))));
            fail("Cardinality too low");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.CARDINALITY_CHANGE));
        }

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf(map), valueOf(map), valueOf(map))));
            fail("Cardinality too high");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.CARDINALITY_CHANGE));
        }

        fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf(map), valueOf(map))));

        NodeIterator iterator = node.getNodes(NODE_NAME);
        assertThat(iterator.getSize(), equalTo(2L));
        assertTrue(isWrittenSuccessfully(iterator.nextNode()));
        assertTrue(isWrittenSuccessfully(iterator.nextNode()));
    }

    @Test
    public void writeToException() throws Exception {
        final Node node = createMock(Node.class);
        fieldType.setMinValues(0);

        expect(node.getNodes(NODE_NAME)).andThrow(new RepositoryException());
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
    public void writeFieldOtherId() throws ErrorWithPayloadException {
        final Node node = createMock(Node.class);
        final FieldPath fieldPath = new FieldPath("other:id");
        final List<FieldValue> fieldValues = Collections.emptyList();
        replay(node);

        assertFalse(fieldType.writeField(node, fieldPath, fieldValues));
        verify(node);
    }

    @Test
    public void writeFieldUnknownChildNode() throws ErrorWithPayloadException {
        final FieldPath fieldPath = new FieldPath(NODE_NAME + "/unknown:child");
        final List<FieldValue> fieldValues = Collections.emptyList();

        try {
            fieldType.writeField(node, fieldPath, fieldValues);
            fail("Exception not thrown");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
    }

    @Test
    public void writeFieldGetChildFails() throws ErrorWithPayloadException, RepositoryException {
        final Node node = createMock(Node.class);
        final FieldPath fieldPath = new FieldPath(NODE_NAME + "/" + STRING_PROPERTY_1);
        final FieldValue fieldValue = new FieldValue("New value");

        expect(node.hasNode(NODE_NAME)).andReturn(true);
        expect(node.getNode(NODE_NAME)).andThrow(new RepositoryException());
        expect(node.getPath()).andReturn("/test");
        replay(node);

        try {
            fieldType.writeField(node, fieldPath, Collections.singletonList(fieldValue));
            fail("Exception not thrown");
        } catch (InternalServerErrorException e) {
            verify(node);
        }
    }

    @Test
    public void writeFieldToStringProperty() throws ErrorWithPayloadException, RepositoryException {
        final Node compound = node.addNode(NODE_NAME, "compound:type");
        compound.setProperty(STRING_PROPERTY_1, "Value");

        final FieldPath fieldPath = new FieldPath(NODE_NAME + "/" + STRING_PROPERTY_1);
        final FieldValue fieldValue = new FieldValue("New value");

        assertTrue(fieldType.writeField(node, fieldPath, Collections.singletonList(fieldValue)));
        assertThat(compound.getProperty(STRING_PROPERTY_1).getString(), equalTo("New value"));
    }

    @Test
    public void writeFieldToNestedCompound() throws ErrorWithPayloadException, RepositoryException {
        compoundField.getFields().add(stringField1);

        final Node compound = node.addNode(NODE_NAME, "compound:type");
        final Node nestedCompound = compound.addNode("compound:field", "nestedcompound:type");

        nestedCompound.setProperty(STRING_PROPERTY_1, "Value");

        final FieldPath fieldPath = new FieldPath(NODE_NAME + "/compound:field/" + STRING_PROPERTY_1);
        final FieldValue fieldValue = new FieldValue("New value");

        assertTrue(fieldType.writeField(node, fieldPath, Collections.singletonList(fieldValue)));
        assertThat(nestedCompound.getProperty(STRING_PROPERTY_1).getString(), equalTo("New value"));
    }

    @Test
    public void validateEmpty() {
        assertTrue(fieldType.validate(Collections.emptyList()));
    }

    @Test
    public void validateSingle() {
        stringField2.addValidator(FieldType.Validator.REQUIRED);

        Map<String, List<FieldValue>> valueMap = validCompound();
        assertTrue(fieldType.validate(listOf(valueOf(valueMap))));

        valueMap.put(STRING_PROPERTY_2, listOf(valueOf(""))); // remove required

        assertFalse(fieldType.validate(listOf(valueOf(valueMap))));
        assertFalse(valueMap.get(STRING_PROPERTY_1).get(0).hasErrorInfo());
        assertThat(valueMap.get(STRING_PROPERTY_2).get(0).getErrorInfo().getCode(),
                equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));
    }

    @Test
    public void validateMultiple() {
        Map<String, List<FieldValue>> valueA;
        Map<String, List<FieldValue>> valueB;

        stringField2.addValidator(FieldType.Validator.REQUIRED);

        valueA = validCompound();
        valueB = validCompound();

        assertTrue(fieldType.validate(Arrays.asList(valueOf(valueA), valueOf(valueB))));

        // error in first instance
        valueA = validCompound();
        valueA.put(STRING_PROPERTY_2, listOf(valueOf(""))); // invalid, because required
        valueB = validCompound();

        assertFalse(fieldType.validate(Arrays.asList(valueOf(valueA), valueOf(valueB))));
        assertFalse(valueA.get(STRING_PROPERTY_1).get(0).hasErrorInfo());
        assertThat(valueA.get(STRING_PROPERTY_2).get(0).getErrorInfo().getCode(),
                equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));
        assertFalse(valueB.get(STRING_PROPERTY_1).get(0).hasErrorInfo());
        assertFalse(valueB.get(STRING_PROPERTY_2).get(0).hasErrorInfo());

        // error in second instance
        valueA = validCompound();
        valueB = validCompound();
        valueB.put(STRING_PROPERTY_2, listOf(valueOf(""))); // invalid, because required

        assertFalse(fieldType.validate(Arrays.asList(valueOf(valueA), valueOf(valueB))));
        assertFalse(valueA.get(STRING_PROPERTY_1).get(0).hasErrorInfo());
        assertFalse(valueA.get(STRING_PROPERTY_2).get(0).hasErrorInfo());
        assertFalse(valueB.get(STRING_PROPERTY_1).get(0).hasErrorInfo());
        assertThat(valueB.get(STRING_PROPERTY_2).get(0).getErrorInfo().getCode(),
                equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));

        // error in both instances
        valueA = validCompound();
        valueA.put(STRING_PROPERTY_2, listOf(valueOf(""))); // invalid, because required
        valueB = validCompound();
        valueB.put(STRING_PROPERTY_2, listOf(valueOf(""))); // invalid, because required

        assertFalse(fieldType.validate(Arrays.asList(valueOf(valueA), valueOf(valueB))));
        assertFalse(valueA.get(STRING_PROPERTY_1).get(0).hasErrorInfo());
        assertThat(valueA.get(STRING_PROPERTY_2).get(0).getErrorInfo().getCode(),
                equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));
        assertFalse(valueB.get(STRING_PROPERTY_1).get(0).hasErrorInfo());
        assertThat(valueB.get(STRING_PROPERTY_2).get(0).getErrorInfo().getCode(),
                equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));
    }

    private Map<String, List<FieldValue>> validCompound() {
        Map<String, List<FieldValue>> map = new HashMap<>();
        map.put(STRING_PROPERTY_1, listOf(valueOf("New Value for String property 1")));
        map.put(STRING_PROPERTY_2, listOf(valueOf("New Value for String property 2")));
        return map;
    }

    private boolean isWrittenSuccessfully(final Node node) throws Exception {
        return node.getProperty(STRING_PROPERTY_1).getString().equals("New Value for String property 1")
            && node.getProperty(STRING_PROPERTY_2).getString().equals("New Value for String property 2");
    }

    private FieldValue valueOf(final Map<String, List<FieldValue>> value) {
        return new FieldValue(value);
    }

    private FieldValue valueOf(final String value) {
        return new FieldValue(value);
    }

    private List<FieldValue> listOf(final FieldValue value) {
        return Collections.singletonList(value);
    }
}