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
        compoundField.setMinValues(0);

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

        final List list = fieldType.readFrom(node).get();
        assertThat(list.size(), equalTo(1));
        assertTrue(list.get(0) instanceof Map);
        final Map map = (Map) list.get(0);
        assertThat(map.get(STRING_PROPERTY_1), equalTo(Collections.singletonList("Value")));
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

        final List values = fieldType.readFrom(node).get();
        assertThat(values.size(), equalTo(1));
        assertTrue(values.get(0) instanceof Map);
        final Map map = (Map)values.get(0);
        assertThat(map.get(STRING_PROPERTY_1), equalTo(Collections.singletonList("Value 1")));
        assertFalse(map.containsKey("compound:field"));
    }

    @Test
    public void readFromOptionalPresentCompound() throws Exception {
        fieldType.setMinValues(0);
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value");

        final List values = fieldType.readFrom(node).get();
        assertThat(values.size(), equalTo(1));
        assertTrue(values.get(0) instanceof Map);
        final Map map = (Map)values.get(0);
        assertThat(map.get(STRING_PROPERTY_1), equalTo(Collections.singletonList("Value")));
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

        final List values = fieldType.readFrom(node).get();
        assertThat(values.size(), equalTo(1));
        assertTrue(values.get(0) instanceof Map);
        final Map map = (Map)values.get(0);
        assertThat(map.get(STRING_PROPERTY_1), equalTo(Collections.singletonList("Value 1")));
    }

    @Test
    public void readFromMultiplePresentCompound() throws Exception {
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value");

        final List values = fieldType.readFrom(node).get();
        assertThat(values.size(), equalTo(1));
        assertTrue(values.get(0) instanceof Map);
        final Map map = (Map)values.get(0);
        assertThat(map.get(STRING_PROPERTY_1), equalTo(Collections.singletonList("Value")));
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

        final List values = fieldType.readFrom(node).get();
        assertThat(values.size(), equalTo(2));
        final Map map1 = (Map)values.get(0);
        assertThat(map1.get(STRING_PROPERTY_1), equalTo(Collections.singletonList("Value 1")));
        final Map map2 = (Map)values.get(1);
        assertThat(map2.get(STRING_PROPERTY_1), equalTo(Collections.singletonList("Value 2")));
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

        Map<String, Object> value = new HashMap<>();
        try {
            fieldType.writeTo(node, Optional.of(value));
            fail("Must be List of Map");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        try {
            fieldType.writeTo(node, Optional.of(Collections.singletonList("No Map")));
            fail("Must be List of Map");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        fieldType.writeTo(node, Optional.of(listOf(validCompound())));
        assertTrue(isWrittenSuccessfully(node.getNode(NODE_NAME)));
    }

    @Test
    public void writeToSingleAbsentCompound() throws Exception {
        try {
            fieldType.writeTo(node, Optional.of(listOf(validCompound())));
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

        fieldType.writeTo(node, Optional.of(listOf(validCompound())));

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
            fieldType.writeTo(node, Optional.of(Collections.singletonList(value)));
            fail("Map values must be accepted by sub-fields");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        fieldType.writeTo(node, Optional.of(listOf(validCompound())));
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
            fieldType.writeTo(node, Optional.of(listOf(validCompound())));
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

        fieldType.writeTo(node, Optional.of(listOf(validCompound())));

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
            fieldType.writeTo(node, Optional.of(Collections.singletonList(value)));
            fail("Map values must be accepted by sub-fields");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        fieldType.writeTo(node, Optional.of(listOf(validCompound())));
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
            fieldType.writeTo(node, Optional.of(listOf(validCompound())));
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

        Map<String, Object> value = validCompound();

        try {
            fieldType.writeTo(node, Optional.of(listOf(value)));
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
    public void validateEmpty() {
        assertFalse(fieldType.validate(Optional.empty()).isPresent());
        assertFalse(fieldType.validate(Optional.of(Collections.emptyList())).isPresent());
    }

    @Test
    public void validateSingle() {
        stringField2.addValidator(FieldType.Validator.REQUIRED);

        Map<String, Object> valueMap = validCompound();
        assertFalse(fieldType.validate(Optional.of(listOf(valueMap))).isPresent());

        // remove required
        valueMap.put(STRING_PROPERTY_2, Collections.singletonList(""));
        final List errors = fieldType.validate(Optional.of(listOf(valueMap))).get();
        assertThat(errors.size(), equalTo(1));
        assertTrue(errors.get(0) instanceof Map);
        final Map<String, List> errorMap = (Map<String, List>) errors.get(0);
        assertFalse(errorMap.containsKey(STRING_PROPERTY_1));
        assertTrue(errorMap.containsKey(STRING_PROPERTY_2));
        final ValidationErrorInfo errorInfo = (ValidationErrorInfo) errorMap.get(STRING_PROPERTY_2).get(0);
        assertTrue(errorInfo.getCode() == ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY);
    }

    @Test
    public void validateMultiple() {
        stringField2.addValidator(FieldType.Validator.REQUIRED);

        Map<String, Object> goodValue = validCompound();
        Map<String, Object> badValue = validCompound();
        badValue.put(STRING_PROPERTY_2, Collections.singletonList("")); // invalid, because required

        assertFalse(fieldType.validate(Optional.of(Arrays.asList(goodValue, goodValue))).isPresent());

        List errorMapList;
        Map<String, List> errorMap;

        // error in first instance
        errorMapList = fieldType.validate(Optional.of(Arrays.asList(goodValue, badValue))).get();
        assertThat(errorMapList.size(), equalTo(2));
        assertTrue(((Map)errorMapList.get(0)).isEmpty());
        errorMap = (Map<String, List>)errorMapList.get(1);
        ValidationErrorInfo errorInfo = (ValidationErrorInfo) errorMap.get(STRING_PROPERTY_2).get(0);
        assertThat(errorInfo.getCode(), equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));

        // error in second instance
        errorMapList = fieldType.validate(Optional.of(Arrays.asList(badValue, goodValue))).get();
        assertThat(errorMapList.size(), equalTo(2));
        errorMap = (Map<String, List>)errorMapList.get(0);
        errorInfo = (ValidationErrorInfo) errorMap.get(STRING_PROPERTY_2).get(0);
        assertThat(errorInfo.getCode(), equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));
        assertTrue(((Map)errorMapList.get(1)).isEmpty());

        // error in both instances
        errorMapList = fieldType.validate(Optional.of(Arrays.asList(badValue, badValue))).get();
        assertThat(errorMapList.size(), equalTo(2));
        errorMap = (Map<String, List>)errorMapList.get(0);
        errorInfo = (ValidationErrorInfo) errorMap.get(STRING_PROPERTY_2).get(0);
        assertThat(errorInfo.getCode(), equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));
        errorMap = (Map<String, List>)errorMapList.get(1);
        errorInfo = (ValidationErrorInfo) errorMap.get(STRING_PROPERTY_2).get(0);
        assertThat(errorInfo.getCode(), equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));
    }

    private Map<String, Object> validCompound() {
        Map<String, Object> map = new HashMap<>();
        map.put(STRING_PROPERTY_1, Collections.singletonList("New Value for String property 1"));
        map.put(STRING_PROPERTY_2, Collections.singletonList("New Value for String property 2"));
        return map;
    }

    private boolean isWrittenSuccessfully(final Node node) throws Exception {
        return node.getProperty(STRING_PROPERTY_1).getString().equals("New Value for String property 1")
            && node.getProperty(STRING_PROPERTY_2).getString().equals("New Value for String property 2");
    }

    private List<Map> listOf(final Map map) {
        return Collections.singletonList(map);
    }
}