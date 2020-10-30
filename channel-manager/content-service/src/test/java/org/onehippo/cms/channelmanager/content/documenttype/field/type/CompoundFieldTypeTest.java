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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.easymock.IExpectationSetters;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.validation.CompoundContext;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.internal.ValidatorInstance;
import org.onehippo.repository.mock.MockNode;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.cms.channelmanager.content.ValidateAndWrite.validateAndWriteTo;
import static org.onehippo.cms.channelmanager.content.documenttype.field.type.AbstractFieldTypeTest.assertViolation;
import static org.onehippo.cms.channelmanager.content.documenttype.field.type.AbstractFieldTypeTest.assertViolations;
import static org.onehippo.cms.channelmanager.content.documenttype.field.type.AbstractFieldTypeTest.assertZeroViolations;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStaticPartial;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest({FieldTypeUtils.class})
public class CompoundFieldTypeTest {

    private static final String NODE_NAME = "node:name";
    private static final String STRING_PROPERTY_1 = "string:field1";
    private static final String STRING_PROPERTY_2 = "string:field2";

    private CompoundFieldType fieldType;
    private StringFieldType stringField1;
    private StringFieldType stringField2;
    private CompoundFieldType compoundField;
    private Node node;

    @Before
    public void setup() {
        mockStaticPartial(FieldTypeUtils.class, "getValidator");

        stringField1 = new StringFieldType();
        stringField1.setId(STRING_PROPERTY_1);
        stringField1.setJcrType(PropertyType.TYPENAME_STRING);

        stringField2 = new StringFieldType();
        stringField2.setId(STRING_PROPERTY_2);
        stringField2.setJcrType(PropertyType.TYPENAME_STRING);

        compoundField = new CompoundFieldType();
        compoundField.setId("compound:field");
        compoundField.setMinValues(0);

        fieldType = new CompoundFieldType();
        fieldType.setId(NODE_NAME);
        fieldType.setJcrType(PropertyType.TYPENAME_STRING);
        fieldType.getFields().add(stringField1);
        fieldType.getFields().add(stringField2);
        fieldType.getFields().add(compoundField);

        node = MockNode.root();
    }

    @Test
    public void emptyCompoundIsNotSupported() {
        CompoundFieldType empty = new CompoundFieldType();
        assertFalse(empty.isSupported());
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
    public void readFromSingleAbsentCompound() {
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
    public void readFromOptionalAbsentCompound() {
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
    public void readFromMultipleAbsentCompound() {
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
        expect(node.getPath()).andReturn("node/location");
        replay(node);

        assertFalse(fieldType.readFrom(node).isPresent());
        verify(node);
    }

    @Test
    @Ignore
    public void writeToSinglePresentCompound() throws Exception {
        final Node compound = node.addNode(NODE_NAME, "compound:type");
        compound.setProperty(STRING_PROPERTY_1, "Old Value");

        try {
            validateAndWriteTo(compound, node, fieldType,Collections.singletonList(null) );
            fail("Must have value");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        try {
            validateAndWriteTo(compound, node, fieldType, Collections.emptyList());
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
            validateAndWriteTo(node, fieldType, listOf(valueOf(validCompound())));
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
    @Ignore
    public void writeToOptionalPresentCompound() throws Exception {
        fieldType.setMinValues(0);
        fieldType.setMaxValues(Integer.MAX_VALUE);

        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value");

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasNode(NODE_NAME));
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value");

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertFalse(node.hasNode(NODE_NAME));
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value");

        final Map<String, List<FieldValue>> map = new HashMap<>();
        try {
            validateAndWriteTo(node.getNode(NODE_NAME), node, fieldType, Arrays.asList(valueOf(map), valueOf(map)));
            fail("Must not be more than 1 value");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.CARDINALITY_CHANGE));
        }
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        validateAndWriteTo(node.getNode(NODE_NAME), node, fieldType,listOf(valueOf(map)));
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        validateAndWriteTo(node.getNode(NODE_NAME), node, fieldType, listOf(valueOf(validCompound())));
        try {
            validateAndWriteTo(node.getNode(NODE_NAME), node, fieldType, Arrays.asList(valueOf(map), valueOf(map)));
            fail("Must not be more than 1 value");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.CARDINALITY_CHANGE));
        }
    }

    @Test
    public void writeToOptionalAbsentCompound() throws Exception {
        fieldType.setMinValues(0);

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasNode(NODE_NAME));

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertFalse(node.hasNode(NODE_NAME));

        try {
            validateAndWriteTo(node, fieldType, listOf(valueOf(validCompound())));
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

        final Node compound = node.addNode(NODE_NAME, "compound:type");
        compound.setProperty(STRING_PROPERTY_1, "Old Value");

        fieldType.writeTo(node, Optional.empty());
        assertFalse(node.hasNode(NODE_NAME));
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value");

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertFalse(node.hasNode(NODE_NAME));
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value");

        Map<String, List<FieldValue>> map = new HashMap<>();
        try {
            validateAndWriteTo(compound, node, fieldType, Arrays.asList(valueOf(map), valueOf(map)));
            fail("Must not be more than 1 value");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.CARDINALITY_CHANGE));
        }
        assertThat(node.getNode(NODE_NAME).getProperty(STRING_PROPERTY_1).getString(), equalTo("Old Value"));

        try {
            validateAndWriteTo(compound, node, fieldType, Arrays.asList(valueOf(map), valueOf(map)));
            fail("Map values must be accepted by sub-fields");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.CARDINALITY_CHANGE));
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
            validateAndWriteTo(node, fieldType, listOf(valueOf(validCompound())));
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

        final Node compound = node.addNode(NODE_NAME, "compound:type");
        compound.setProperty(STRING_PROPERTY_1, "Old Value 1");
        node.addNode(NODE_NAME, "compound:type").setProperty(STRING_PROPERTY_1, "Old Value 2");

        Map<String, List<FieldValue>> map = validCompound();

        try {
            validateAndWriteTo(compound, node, fieldType, listOf(valueOf(map)));
            fail("Cardinality too low");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.CARDINALITY_CHANGE));
        }

        try {
            validateAndWriteTo(compound, node, fieldType, Arrays.asList(valueOf(map), valueOf(map), valueOf(map)));
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
        final CompoundContext nodeContext = new CompoundContext(node, null, null, null);
        final FieldPath fieldPath = new FieldPath("other:id");
        final List<FieldValue> fieldValues = Collections.emptyList();
        replay(node);

        assertFalse(fieldType.writeField(fieldPath, fieldValues, nodeContext));
        verify(node);
    }

    @Test
    public void writeFieldUnknownChildNode() throws ErrorWithPayloadException {
        final FieldPath fieldPath = new FieldPath(NODE_NAME + "/unknown:child");
        final List<FieldValue> fieldValues = Collections.emptyList();
        final CompoundContext nodeContext = new CompoundContext(node, null, null, null);

        try {
            fieldType.writeField(fieldPath, fieldValues, nodeContext);
            fail("Exception not thrown");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
    }

    @Test
    public void writeFieldGetChildFails() throws ErrorWithPayloadException, RepositoryException {
        final Node node = createMock(Node.class);
        final CompoundContext nodeContext = new CompoundContext(node, null, null, null);
        final FieldPath fieldPath = new FieldPath(NODE_NAME + "/" + STRING_PROPERTY_1);
        final FieldValue fieldValue = new FieldValue("New value");

        expect(node.hasNode(NODE_NAME)).andReturn(true);
        expect(node.getNode(NODE_NAME)).andThrow(new RepositoryException());
        expect(node.getPath()).andReturn("/test");
        replay(node);

        try {
            fieldType.writeField(fieldPath, Collections.singletonList(fieldValue), nodeContext);
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
        final CompoundContext nodeContext = new CompoundContext(node, null, null, null);

        assertTrue(fieldType.writeField(fieldPath, Collections.singletonList(fieldValue), nodeContext));
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
        final CompoundContext nodeContext = new CompoundContext(node, null, null, null);

        assertTrue(fieldType.writeField(fieldPath, Collections.singletonList(fieldValue), nodeContext));
        assertThat(nestedCompound.getProperty(STRING_PROPERTY_1).getString(), equalTo("New value"));
    }

    @Test
    public void writeFieldToSecondCompound() throws ErrorWithPayloadException, RepositoryException {
        final Node compound1 = node.addNode(NODE_NAME, "compound:type");
        final Node compound2 = node.addNode(NODE_NAME, "compound:type");

        compound1.setProperty(STRING_PROPERTY_1, "Value in compound 1");
        compound2.setProperty(STRING_PROPERTY_1, "Value in compound 2");

        final FieldPath fieldPath = new FieldPath(NODE_NAME + "[2]/" + STRING_PROPERTY_1);
        final FieldValue fieldValue = new FieldValue("New value for compound 2");
        final CompoundContext nodeContext = new CompoundContext(node, null, null, null);

        assertTrue(fieldType.writeField(fieldPath, Collections.singletonList(fieldValue), nodeContext));
        assertThat(compound1.getProperty(STRING_PROPERTY_1).getString(), equalTo("Value in compound 1"));
        assertThat(compound2.getProperty(STRING_PROPERTY_1).getString(), equalTo("New value for compound 2"));
    }

    @Test
    public void validateCompoundGood() throws Exception {
        node.addNode(NODE_NAME, "compound:type");
        final CompoundContext nodeContext = new CompoundContext(node, null, null, null);
        fieldType.addValidatorName("compound-validator");

        expectValidator("compound-validator", new AlwaysGoodTestValidator());
        replayAll();

        final Map<String, List<FieldValue>> valueMap = validCompound();
        assertZeroViolations(fieldType.validate(listOf(valueOf(valueMap)), nodeContext));

        verifyAll();
    }

    @Test
    public void validateCompoundBad() throws Exception {
        node.addNode(NODE_NAME, "compound:type");
        final CompoundContext nodeContext = new CompoundContext(node, null, null, null);
        fieldType.addValidatorName("compound-validator");

        expectValidator("compound-validator", new AlwaysBadTestValidator());
        replayAll();

        final Map<String, List<FieldValue>> valueMap = validCompound();
        final List<FieldValue> compoundValue = listOf(valueOf(valueMap));

        assertViolation(fieldType.validate(compoundValue, nodeContext));
        assertErrorFromValidator(compoundValue.get(0), "compound-validator");
        assertFalse(valueMap.get(STRING_PROPERTY_1).get(0).hasErrorInfo());
        assertFalse(valueMap.get(STRING_PROPERTY_2).get(0).hasErrorInfo());

        verifyAll();
    }

    @Test
    public void validateCompoundBadAndFieldBad() throws Exception {
        node.addNode(NODE_NAME, "compound:type");
        final CompoundContext nodeContext = new CompoundContext(node, null, null, null);
        fieldType.addValidatorName("compound-validator");
        stringField2.addValidatorName("field-validator");

        expectValidator("compound-validator", new AlwaysBadTestValidator());
        expectValidator("field-validator", new AlwaysBadTestValidator());
        replayAll();

        final Map<String, List<FieldValue>> valueMap = validCompound();
        final List<FieldValue> compoundValue = listOf(valueOf(valueMap));

        assertViolations(fieldType.validate(compoundValue, nodeContext), 2);
        assertErrorFromValidator(compoundValue.get(0), "compound-validator");

        assertFalse(valueMap.get(STRING_PROPERTY_1).get(0).hasErrorInfo());
        assertErrorFromValidator(valueMap.get(STRING_PROPERTY_2).get(0), "field-validator");

        verifyAll();
    }

    @Test
    public void validateEmpty() {
        final CompoundContext nodeContext = new CompoundContext(node, null, null, null);
        fieldType.setMinValues(0);
        assertZeroViolations(fieldType.validate(Collections.emptyList(), nodeContext));
    }

    @Test
    public void validateSingleGood() throws Exception {
        node.addNode(NODE_NAME, "compound:type");
        final CompoundContext nodeContext = new CompoundContext(node, null, null, null);
        stringField2.addValidatorName("non-empty");

        expectValidator("non-empty", new NonEmptyTestValidator());
        replayAll();

        final Map<String, List<FieldValue>> valueMap = validCompound();
        assertZeroViolations(fieldType.validate(listOf(valueOf(valueMap)), nodeContext));
        verifyAll();
    }

    @Test
    public void validateSingleBad() throws Exception {
        node.addNode(NODE_NAME, "compound:type");
        final CompoundContext nodeContext = new CompoundContext(node, null, null, null);
        stringField2.addValidatorName("non-empty");

        expectValidator("non-empty", new NonEmptyTestValidator());
        replayAll();

        final Map<String, List<FieldValue>> valueMap = validCompound();
        valueMap.put(STRING_PROPERTY_2, listOf(valueOf(""))); // make non-empty field empty

        assertViolation(fieldType.validate(listOf(valueOf(valueMap)), nodeContext));
        assertFalse(valueMap.get(STRING_PROPERTY_1).get(0).hasErrorInfo());
        assertErrorFromValidator(valueMap.get(STRING_PROPERTY_2).get(0), "non-empty");

        verifyAll();
    }

    @Test
    public void validateMultipleAllGood() throws Exception {
        node.addNode(NODE_NAME, "compound:type");
        node.addNode(NODE_NAME, "compound:type");
        final CompoundContext nodeContext = new CompoundContext(node, null, null, null);
        stringField2.addValidatorName("non-empty");

        expectValidator("non-empty", new NonEmptyTestValidator()).times(2);
        replayAll();

        final Map<String, List<FieldValue>> valueA = validCompound();
        final Map<String, List<FieldValue>> valueB = validCompound();

        fieldType.setMaxValues(2);
        assertZeroViolations(fieldType.validate(Arrays.asList(valueOf(valueA), valueOf(valueB)), nodeContext));

        verifyAll();
    }

    @Test
    public void validateMultipleFirstBad() throws Exception {
        node.addNode(NODE_NAME, "compound:type");
        node.addNode(NODE_NAME, "compound:type");
        final CompoundContext nodeContext = new CompoundContext(node, null, null, null);
        stringField2.addValidatorName("non-empty");

        expectValidator("non-empty", new NonEmptyTestValidator()).times(2);
        replayAll();

        final Map<String, List<FieldValue>> valueA = validCompound();
        final Map<String, List<FieldValue>> valueB = validCompound();

        valueA.put(STRING_PROPERTY_2, listOf(valueOf(""))); // invalid, because required

        fieldType.setMinValues(2);
        fieldType.setMaxValues(2);
        assertViolation(fieldType.validate(Arrays.asList(valueOf(valueA), valueOf(valueB)), nodeContext));
        assertFalse(valueA.get(STRING_PROPERTY_1).get(0).hasErrorInfo());
        assertErrorFromValidator(valueA.get(STRING_PROPERTY_2).get(0), "non-empty");
        assertFalse(valueB.get(STRING_PROPERTY_1).get(0).hasErrorInfo());
        assertFalse(valueB.get(STRING_PROPERTY_2).get(0).hasErrorInfo());

        verifyAll();
    }

    @Test
    public void validateMultipleSecondBad() throws Exception {
        node.addNode(NODE_NAME, "compound:type");
        node.addNode(NODE_NAME, "compound:type");
        final CompoundContext nodeContext = new CompoundContext(node, null, null, null);
        stringField2.addValidatorName("non-empty");

        expectValidator("non-empty", new NonEmptyTestValidator()).times(2);
        replayAll();

        final Map<String, List<FieldValue>> valueA = validCompound();
        final Map<String, List<FieldValue>> valueB = validCompound();

        valueB.put(STRING_PROPERTY_2, listOf(valueOf(""))); // invalid, because required

        fieldType.setMinValues(0);
        fieldType.setMaxValues(2);
        assertViolation(fieldType.validate(Arrays.asList(valueOf(valueA), valueOf(valueB)), nodeContext));
        assertFalse(valueA.get(STRING_PROPERTY_1).get(0).hasErrorInfo());
        assertFalse(valueA.get(STRING_PROPERTY_2).get(0).hasErrorInfo());
        assertFalse(valueB.get(STRING_PROPERTY_1).get(0).hasErrorInfo());
        assertErrorFromValidator(valueB.get(STRING_PROPERTY_2).get(0), "non-empty");

        verifyAll();
    }

    @Test
    public void validateMultipleAllBad() throws Exception {
        node.addNode(NODE_NAME, "compound:type");
        node.addNode(NODE_NAME, "compound:type");
        final CompoundContext nodeContext = new CompoundContext(node, null, null, null);
        stringField2.addValidatorName("non-empty");

        expectValidator("non-empty", new NonEmptyTestValidator()).times(2);
        replayAll();

        final Map<String, List<FieldValue>> valueA = validCompound();
        final Map<String, List<FieldValue>> valueB = validCompound();

        valueA.put(STRING_PROPERTY_2, listOf(valueOf(""))); // invalid, because required
        valueB.put(STRING_PROPERTY_2, listOf(valueOf(""))); // invalid, because required

        fieldType.setMinValues(0);
        fieldType.setMaxValues(2);
        assertViolations(fieldType.validate(Arrays.asList(valueOf(valueA), valueOf(valueB)), nodeContext), 2);
        assertFalse(valueA.get(STRING_PROPERTY_1).get(0).hasErrorInfo());
        assertErrorFromValidator(valueA.get(STRING_PROPERTY_2).get(0), "non-empty");
        assertFalse(valueB.get(STRING_PROPERTY_1).get(0).hasErrorInfo());
        assertErrorFromValidator(valueB.get(STRING_PROPERTY_2).get(0), "non-empty");

        verifyAll();
    }

    @Test(expected = BadRequestException.class)
    public void validateNodeValueCountMismatch() {
        final CompoundContext nodeContext = new CompoundContext(node, null, null, null);
        stringField2.addValidatorName("non-empty");

        expectValidator("non-empty", new NonEmptyTestValidator());
        replayAll();
        final Map<String, List<FieldValue>> valueMap = validCompound();
        fieldType.validate(listOf(valueOf(valueMap)), nodeContext);
    }

    private static Map<String, List<FieldValue>> validCompound() {
        final Map<String, List<FieldValue>> map = new HashMap<>();
        map.put(STRING_PROPERTY_1, listOf(valueOf("New Value for String property 1")));
        map.put(STRING_PROPERTY_2, listOf(valueOf("New Value for String property 2")));
        return map;
    }

    private static boolean isWrittenSuccessfully(final Node node) throws Exception {
        return node.getProperty(STRING_PROPERTY_1).getString().equals("New Value for String property 1")
            && node.getProperty(STRING_PROPERTY_2).getString().equals("New Value for String property 2");
    }

    private static FieldValue valueOf(final Map<String, List<FieldValue>> value) {
        return new FieldValue(value);
    }

    private static FieldValue valueOf(final String value) {
        return new FieldValue(value);
    }

    private static List<FieldValue> listOf(final FieldValue value) {
        return Collections.singletonList(value);
    }

    private static IExpectationSetters<ValidatorInstance> expectValidator(final String validatorName, final Validator<Object> validator) {
        return expect(FieldTypeUtils.getValidator(eq(validatorName)))
                .andReturn(new TestValidatorInstance(validator));
    }

    private static void assertErrorFromValidator(final FieldValue fieldValue, final String validatorName) {
        assertTrue(fieldValue.hasErrorInfo());
        assertThat(fieldValue.getErrorInfo().getValidation(), equalTo(validatorName));
    }

}
