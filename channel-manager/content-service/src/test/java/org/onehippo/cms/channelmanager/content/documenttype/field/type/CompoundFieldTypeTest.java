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
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

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
        assertThat(value instanceof Map, equalTo(true));
        final Map map = (Map)value;
        assertThat(map.get(STRING_PROPERTY_1), equalTo("Value"));
        assertThat(map.containsKey("compound:field"), equalTo(false));
    }

    @Test
    public void readFromSingleAbsentCompound() throws Exception {
        assertThat(fieldType.readFrom(node).isPresent(), equalTo(false));
    }

    @Test
    public void readFromSingleCompoundWhen2Present() throws Exception {
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value 1");
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value 2");

        final Object value = fieldType.readFrom(node).get();
        assertThat(value instanceof Map, equalTo(true));
        final Map map = (Map)value;
        assertThat(map.get(STRING_PROPERTY_1), equalTo("Value 1"));
        assertThat(map.containsKey("compound:field"), equalTo(false));
    }

    @Test
    public void readFromOptionalPresentCompound() throws Exception {
        fieldType.setOptional(true);
        fieldType.setMultiple(true);
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value");

        final Object value = fieldType.readFrom(node).get();
        assertThat(value instanceof List, equalTo(true));
        final List list = (List)value;
        assertThat(list.size(), equalTo(1));
        final Map map = (Map)list.get(0);
        assertThat(map.get(STRING_PROPERTY_1), equalTo("Value"));
        assertThat(map.containsKey("compound:field"), equalTo(false));
    }

    @Test
    public void readFromOptionalAbsentCompound() throws Exception {
        fieldType.setOptional(true);
        fieldType.setMultiple(true);

        assertThat(fieldType.readFrom(node).isPresent(), equalTo(false));
    }

    @Test
    public void readFromOptionalCompoundWhen2Present() throws Exception {
        fieldType.setOptional(true);
        fieldType.setMultiple(true);
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value 1");
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value 2");

        final Object value = fieldType.readFrom(node).get();
        assertThat(value instanceof List, equalTo(true));
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
        assertThat(value instanceof List, equalTo(true));
        final List list = (List)value;
        assertThat(list.size(), equalTo(1));
        final Map map = (Map)list.get(0);
        assertThat(map.get(STRING_PROPERTY_1), equalTo("Value"));
        assertThat(map.containsKey("compound:field"), equalTo(false));
    }

    @Test
    public void readFromMultipleAbsentCompound() throws Exception {
        fieldType.setMultiple(true);

        assertThat(fieldType.readFrom(node).isPresent(), equalTo(false));
    }

    @Test
    public void readFromMultipleCompoundWhen2Present() throws Exception {
        fieldType.setMultiple(true);
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value 1");
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Value 2");

        final Object value = fieldType.readFrom(node).get();
        assertThat(value instanceof List, equalTo(true));
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

        assertThat(fieldType.readFrom(node).isPresent(), equalTo(false));
        verify(node);
    }


    @Test
    public void writeToSinglePresentCompound() throws Exception {
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value");

        assertThat(fieldType.writeTo(node, Optional.empty()), equalTo(1));
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        assertThat(fieldType.writeTo(node, Optional.of(Collections.emptyList())), equalTo(1));
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("bla"))), equalTo(1)); // must be Map
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        Map<String, Object> value = new HashMap<>();
        assertThat(fieldType.writeTo(node, Optional.of(value)), equalTo(2)); // map values must be accepted by sub-fields
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        value.put(STRING_PROPERTY_1, "New Value 1");
        value.put(STRING_PROPERTY_2, "New Value 2");
        assertThat(fieldType.writeTo(node, Optional.of(value)), equalTo(0));
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("New Value 1"));
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_2).getString(), equalTo("New Value 2"));
    }

    @Test
    public void writeToSingleAbsentCompound() throws Exception {
        Map<String, Object> value = new HashMap<>();
        value.put(STRING_PROPERTY_1, "New Value 1");
        value.put(STRING_PROPERTY_2, "New Value 2");
        assertThat(fieldType.writeTo(node, Optional.of(value)), equalTo(1));
        assertThat(node.hasNode(NODE_NAME), equalTo(false));
    }

    @Test
    public void writeToSingleCompoundWith2Present() throws Exception {
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value 1");
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value 2");

        Map<String, Object> value = new HashMap<>();
        value.put(STRING_PROPERTY_1, "New Value 1");
        value.put(STRING_PROPERTY_2, "New Value 2");
        assertThat(fieldType.writeTo(node, Optional.of(value)), equalTo(0));
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

        assertThat(fieldType.writeTo(node, Optional.empty()), equalTo(0));
        assertThat(node.hasNode(NODE_NAME), equalTo(false));
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value");

        assertThat(fieldType.writeTo(node, Optional.of(Collections.EMPTY_MAP)), equalTo(1)); // must be List
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        assertThat(fieldType.writeTo(node, Optional.of(Collections.emptyList())), equalTo(0));
        assertThat(node.hasNode(NODE_NAME), equalTo(false));
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value");

        Map<String, Object> value = new HashMap<>();
        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList(value, value))), equalTo(1)); // must not be more than 1 value
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList(value))), equalTo(2)); // map values must be accepted by sub-fields
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        value.put(STRING_PROPERTY_1, "New Value 1");
        value.put(STRING_PROPERTY_2, "New Value 2");
        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList(value))), equalTo(0));
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("New Value 1"));
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_2).getString(), equalTo("New Value 2"));
    }

    @Test
    public void writeToOptionalAbsentCompound() throws Exception {
        fieldType.setMultiple(true);
        fieldType.setOptional(true);

        assertThat(fieldType.writeTo(node, Optional.empty()), equalTo(0));
        assertThat(node.hasNode(NODE_NAME), equalTo(false));

        assertThat(fieldType.writeTo(node, Optional.of(Collections.emptyList())), equalTo(0));
        assertThat(node.hasNode(NODE_NAME), equalTo(false));

        Map<String, Object> value = new HashMap<>();
        value.put(STRING_PROPERTY_1, "New Value 1");
        value.put(STRING_PROPERTY_2, "New Value 2");
        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList(value))), equalTo(1));
        assertThat(node.hasNode(NODE_NAME), equalTo(false));
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
        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList(value))), equalTo(0));
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

        assertThat(fieldType.writeTo(node, Optional.empty()), equalTo(1));
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        assertThat(fieldType.writeTo(node, Optional.of(Collections.EMPTY_MAP)), equalTo(1)); // must be List
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        assertThat(fieldType.writeTo(node, Optional.of(Collections.emptyList())), equalTo(1)); // must not be empty
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        Map<String, Object> value = new HashMap<>();
        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList(value, value))), equalTo(1)); // must not be more than 1 value
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList(value))), equalTo(2)); // map values must be accepted by sub-fields
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        value.put(STRING_PROPERTY_1, "New Value 1");
        value.put(STRING_PROPERTY_2, "New Value 2");
        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList(value))), equalTo(0));
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("New Value 1"));
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_2).getString(), equalTo("New Value 2"));
    }

    @Test
    public void writeToMultipleAbsentCompound() throws Exception {
        fieldType.setMultiple(true);

        assertThat(fieldType.writeTo(node, Optional.empty()), equalTo(0));
        assertThat(node.hasNode(NODE_NAME), equalTo(false));

        assertThat(fieldType.writeTo(node, Optional.of(Collections.emptyList())), equalTo(0));
        assertThat(node.hasNode(NODE_NAME), equalTo(false));

        Map<String, Object> value = new HashMap<>();
        value.put(STRING_PROPERTY_1, "New Value 1");
        value.put(STRING_PROPERTY_2, "New Value 2");
        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList(value))), equalTo(1));
        assertThat(node.hasNode(NODE_NAME), equalTo(false));
    }

    @Test
    public void writeToMultipleCompoundWith2Present() throws Exception {
        fieldType.setMultiple(true);

        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value 1");
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value 2");

        Map<String, Object> value = new HashMap<>();
        value.put(STRING_PROPERTY_1, "New Value 1");
        value.put(STRING_PROPERTY_2, "New Value 2");
        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList(value))), equalTo(1)); // cardinality too low
        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList(value, value, value))), equalTo(1)); // cardinality too high
        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList(value, value))), equalTo(0));

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

        assertThat(fieldType.writeTo(node, Optional.empty()), equalTo(1));
        verify(node);
    }
}