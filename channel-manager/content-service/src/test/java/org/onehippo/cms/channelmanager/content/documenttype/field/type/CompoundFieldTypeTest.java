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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms.channelmanager.content.documenttype.field.validation.ValidationErrorInfo;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
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
    private FieldType compoundField;
    private Node node;

    @Before
    public void setup() {
        stringField1 = new StringFieldType();
        stringField1.setId(STRING_PROPERTY_1);

        stringField2 = new StringFieldType();
        stringField2.setId(STRING_PROPERTY_2);

        compoundField = new CompoundFieldType();
        compoundField.setId("compound:field");
        compoundField.setMultiple(true);
        compoundField.setOptional(true);

        fieldType = new CompoundFieldType();
        fieldType.setId(NODE_NAME);
        fieldType.getFields().add(stringField1);
        fieldType.getFields().add(stringField2);
        fieldType.getFields().add(compoundField);

        node = MockNode.root();
    }

    @Test
    public void readFromSinglePresentCompound() throws Exception {
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value");

        final Object value = fieldType.readFrom(node).get();
        assertTrue(value instanceof Map);
        final Map map = (Map)value;
        assertThat(map.get(STRING_PROPERTY_1), equalTo("Value"));
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

        final Object value = fieldType.readFrom(node).get();
        assertTrue(value instanceof Map);
        final Map map = (Map)value;
        assertThat(map.get(STRING_PROPERTY_1), equalTo("Value 1"));
        assertFalse(map.containsKey("compound:field"));
    }

    @Test
    public void readFromOptionalPresentCompound() throws Exception {
        fieldType.setOptional(true);
        fieldType.setMultiple(true);
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value");

        final Object value = fieldType.readFrom(node).get();
        assertTrue(value instanceof List);
        final List list = (List)value;
        assertThat(list.size(), equalTo(1));
        final Map map = (Map)list.get(0);
        assertThat(map.get(STRING_PROPERTY_1), equalTo("Value"));
        assertFalse(map.containsKey("compound:field"));
    }

    @Test
    public void readFromOptionalAbsentCompound() throws Exception {
        fieldType.setOptional(true);
        fieldType.setMultiple(true);

        assertFalse(fieldType.readFrom(node).isPresent());
    }

    @Test
    public void readFromOptionalCompoundWhen2Present() throws Exception {
        fieldType.setOptional(true);
        fieldType.setMultiple(true);
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value 1");
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value 2");

        final Object value = fieldType.readFrom(node).get();
        assertTrue(value instanceof List);
        final List list = (List)value;
        assertThat(list.size(), equalTo(1));
        final Map map = (Map)list.get(0);
        assertThat(map.get(STRING_PROPERTY_1), equalTo("Value 1"));
    }

    @Test
    public void readFromMultiplePresentCompound() throws Exception {
        fieldType.setMultiple(true);
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value");

        final Object value = fieldType.readFrom(node).get();
        assertTrue(value instanceof List);
        final List list = (List)value;
        assertThat(list.size(), equalTo(1));
        final Map map = (Map)list.get(0);
        assertThat(map.get(STRING_PROPERTY_1), equalTo("Value"));
        assertFalse(map.containsKey("compound:field"));
    }

    @Test
    public void readFromMultipleAbsentCompound() throws Exception {
        fieldType.setMultiple(true);

        assertFalse(fieldType.readFrom(node).isPresent());
    }

    @Test
    public void readFromMultipleCompoundWhen2Present() throws Exception {
        fieldType.setMultiple(true);
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value 1");
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value 2");

        final Object value = fieldType.readFrom(node).get();
        assertTrue(value instanceof List);
        final List list = (List)value;
        assertThat(list.size(), equalTo(2));
        final Map map1 = (Map)list.get(0);
        assertThat(map1.get(STRING_PROPERTY_1), equalTo("Value 1"));
        final Map map2 = (Map)list.get(1);
        assertThat(map2.get(STRING_PROPERTY_1), equalTo("Value 2"));
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
            fail("Must be non-empty");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        try {
            fieldType.writeTo(node, Optional.of(Collections.emptyList()));
            fail("Must not be List");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList("bla")));
            fail("Must not be String");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        Map<String, Object> value = new HashMap<>();
        try {
            fieldType.writeTo(node, Optional.of(value));
            fail("Singular subfield values missing");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        value.put(STRING_PROPERTY_1, "New Value 1");
        value.put(STRING_PROPERTY_2, "New Value 2");
        fieldType.writeTo(node, Optional.of(value));
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("New Value 1"));
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_2).getString(), equalTo("New Value 2"));
    }

    @Test
    public void writeToSingleAbsentCompound() throws Exception {
        Map<String, Object> value = new HashMap<>();
        value.put(STRING_PROPERTY_1, "New Value 1");
        value.put(STRING_PROPERTY_2, "New Value 2");

        try {
            fieldType.writeTo(node, Optional.of(value));
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

        Map<String, Object> value = new HashMap<>();
        value.put(STRING_PROPERTY_1, "New Value 1");
        value.put(STRING_PROPERTY_2, "New Value 2");

        fieldType.writeTo(node, Optional.of(value));

        NodeIterator iterator = node.getNodes(NODE_NAME);
        assertThat(iterator.getSize(), equalTo(1L));
        Node compound = iterator.nextNode();
        assertThat(compound.getProperty(STRING_PROPERTY_1).getString(), equalTo("New Value 1"));
        assertThat(compound.getProperty(STRING_PROPERTY_2).getString(), equalTo("New Value 2"));
    }

    @Test
    public void writeToOptionalPresentCompound() throws Exception {
        fieldType.setMultiple(true);
        fieldType.setOptional(true);

        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value");

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasNode(NODE_NAME));
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value");

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertFalse(node.hasNode(NODE_NAME));
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value");

        try {
            fieldType.writeTo(node, Optional.of(Collections.EMPTY_MAP));
            fail("Must be List");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        Map<String, Object> value = new HashMap<>();
        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(value, value)));
            fail("Must not be more than 1 value");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(value)));
            fail("Map values must be accepted by sub-fields");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        value.put(STRING_PROPERTY_1, "New Value 1");
        value.put(STRING_PROPERTY_2, "New Value 2");
        fieldType.writeTo(node, Optional.of(Arrays.asList(value)));
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("New Value 1"));
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_2).getString(), equalTo("New Value 2"));
    }

    @Test
    public void writeToOptionalAbsentCompound() throws Exception {
        fieldType.setMultiple(true);
        fieldType.setOptional(true);

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasNode(NODE_NAME));

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertFalse(node.hasNode(NODE_NAME));

        Map<String, Object> value = new HashMap<>();
        value.put(STRING_PROPERTY_1, "New Value 1");
        value.put(STRING_PROPERTY_2, "New Value 2");
        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(value)));
            fail("Can't create new node");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.CARDINALITY_CHANGE));
        }
        assertFalse(node.hasNode(NODE_NAME));
    }

    @Test
    public void writeToOptionalCompoundWith2Present() throws Exception {
        fieldType.setMultiple(true);
        fieldType.setOptional(true);

        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value 1");
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value 2");

        Map<String, Object> value = new HashMap<>();
        value.put(STRING_PROPERTY_1, "New Value 1");
        value.put(STRING_PROPERTY_2, "New Value 2");

        fieldType.writeTo(node, Optional.of(Arrays.asList(value)));

        NodeIterator iterator = node.getNodes(NODE_NAME);
        assertThat(iterator.getSize(), equalTo(1L));
        Node compound = iterator.nextNode();
        assertThat(compound.getProperty(STRING_PROPERTY_1).getString(), equalTo("New Value 1"));
        assertThat(compound.getProperty(STRING_PROPERTY_2).getString(), equalTo("New Value 2"));
    }

    @Test
    public void writeToMultiplePresentCompound() throws Exception {
        fieldType.setMultiple(true);

        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value");

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasNode(NODE_NAME));
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value");

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertFalse(node.hasNode(NODE_NAME));
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value");

        try {
            fieldType.writeTo(node, Optional.of(Collections.EMPTY_MAP));
            fail("Must be List");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        Map<String, Object> value = new HashMap<>();
        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(value, value)));
            fail("Must not be more than 1 value");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.CARDINALITY_CHANGE));
        }
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(value)));
            fail("Map values must be accepted by sub-fields");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        value.put(STRING_PROPERTY_1, "New Value 1");
        value.put(STRING_PROPERTY_2, "New Value 2");
        fieldType.writeTo(node, Optional.of(Arrays.asList(value)));
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("New Value 1"));
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_2).getString(), equalTo("New Value 2"));
    }

    @Test
    public void writeToMultipleAbsentCompound() throws Exception {
        fieldType.setMultiple(true);

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasNode(NODE_NAME));

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertFalse(node.hasNode(NODE_NAME));

        Map<String, Object> value = new HashMap<>();
        value.put(STRING_PROPERTY_1, "New Value 1");
        value.put(STRING_PROPERTY_2, "New Value 2");

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(value)));
            fail("Cannot create node");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.CARDINALITY_CHANGE));
        }
        assertFalse(node.hasNode(NODE_NAME));
    }

    @Test
    public void writeToMultipleCompoundWith2Present() throws Exception {
        fieldType.setMultiple(true);

        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value 1");
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value 2");

        Map<String, Object> value = new HashMap<>();
        value.put(STRING_PROPERTY_1, "New Value 1");
        value.put(STRING_PROPERTY_2, "New Value 2");

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(value)));
            fail("Cardinality too low");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.CARDINALITY_CHANGE));
        }

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(value, value, value)));
            fail("Cardinality too high");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.CARDINALITY_CHANGE));
        }

        fieldType.writeTo(node, Optional.of(Arrays.asList(value, value)));

        NodeIterator iterator = node.getNodes(NODE_NAME);
        assertThat(iterator.getSize(), equalTo(2L));
        Node compound1 = iterator.nextNode();
        assertThat(compound1.getProperty(STRING_PROPERTY_1).getString(), equalTo("New Value 1"));
        assertThat(compound1.getProperty(STRING_PROPERTY_2).getString(), equalTo("New Value 2"));
        Node compound2 = iterator.nextNode();
        assertThat(compound2.getProperty(STRING_PROPERTY_1).getString(), equalTo("New Value 1"));
        assertThat(compound2.getProperty(STRING_PROPERTY_2).getString(), equalTo("New Value 2"));
    }

    @Test
    public void writeToException() throws Exception {
        final Node node = createMock(Node.class);

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
    public void validateRequired() {
        fieldType.addValidator(FieldType.Validator.REQUIRED);

        final ValidationErrorInfo error = (ValidationErrorInfo)fieldType.validate(Optional.empty()).get();
        assertThat(error.getCode(), equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_ABSENT));

        Map<String, Object> value = new HashMap<>();
        value.put(STRING_PROPERTY_1, "New Value 1");
        value.put(STRING_PROPERTY_2, "New Value 2");
        assertFalse(fieldType.validate(Optional.of(value)).isPresent());
    }

    @Test
    public void validateEmpty() {
        assertFalse(fieldType.validate(Optional.empty()).isPresent());
        assertFalse(fieldType.validate(Optional.of(Collections.emptyList())).isPresent());
    }

    @Test
    public void validateSingle() {
        stringField2.addValidator(FieldType.Validator.REQUIRED);

        Map<String, Object> value = new HashMap<>();
        value.put(STRING_PROPERTY_1, "New Value 1");
        value.put(STRING_PROPERTY_2, "New Value 2");
        assertFalse(fieldType.validate(Optional.of(value)).isPresent());

        // remove required
        value.remove(STRING_PROPERTY_2);
        final Object error = fieldType.validate(Optional.of(value)).get();
        assertTrue(error instanceof Map);
        final Map<String, Object> errorMap = (Map<String, Object>) error;
        assertFalse(errorMap.containsKey(STRING_PROPERTY_1));
        assertTrue(errorMap.containsKey(STRING_PROPERTY_2));
        final ValidationErrorInfo errorInfo = (ValidationErrorInfo) errorMap.get(STRING_PROPERTY_2);
        assertTrue(errorInfo.getCode() == ValidationErrorInfo.Code.REQUIRED_FIELD_ABSENT);
    }

    @Test
    public void validateMultiple() {
        stringField2.addValidator(FieldType.Validator.REQUIRED);

        Map<String, Object> goodValue = new HashMap<>();
        goodValue.put(STRING_PROPERTY_1, "New Value 1");
        goodValue.put(STRING_PROPERTY_2, "New Value 2");

        Map<String, Object> badValue = new HashMap<>();
        badValue.put(STRING_PROPERTY_1, "New Value 1");
        // string property 2 required but missing

        assertFalse(fieldType.validate(Optional.of(Arrays.asList(goodValue, goodValue))).isPresent());

        // error in first instance
        Object error = fieldType.validate(Optional.of(Arrays.asList(goodValue, badValue))).get();
        assertTrue(error instanceof List);
        List errorMapList = (List) error;
        assertThat(errorMapList.size(), equalTo(2));
        assertTrue(((Map)errorMapList.get(0)).isEmpty());
        ValidationErrorInfo errorInfo = (ValidationErrorInfo) ((Map)errorMapList.get(1)).get(STRING_PROPERTY_2);
        assertThat(errorInfo.getCode(), equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_ABSENT));

        // error in second instance
        error = fieldType.validate(Optional.of(Arrays.asList(badValue, goodValue))).get();
        assertTrue(error instanceof List);
        errorMapList = (List) error;
        assertThat(errorMapList.size(), equalTo(2));
        errorInfo = (ValidationErrorInfo) ((Map)errorMapList.get(0)).get(STRING_PROPERTY_2);
        assertThat(errorInfo.getCode(), equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_ABSENT));
        assertTrue(((Map)errorMapList.get(1)).isEmpty());

        // error in both instances
        error = fieldType.validate(Optional.of(Arrays.asList(badValue, badValue))).get();
        assertTrue(error instanceof List);
        errorMapList = (List) error;
        assertThat(errorMapList.size(), equalTo(2));
        errorInfo = (ValidationErrorInfo) ((Map)errorMapList.get(0)).get(STRING_PROPERTY_2);
        assertThat(errorInfo.getCode(), equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_ABSENT));
        errorInfo = (ValidationErrorInfo) ((Map)errorMapList.get(1)).get(STRING_PROPERTY_2);
        assertThat(errorInfo.getCode(), equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_ABSENT));
    }
}