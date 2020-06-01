/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.testutils.log4j.Log4jInterceptor;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Lists;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({FieldTypeUtils.class, LocalizationUtils.class, ChoiceFieldUtils.class})
public class ChoiceFieldTypeTest {

    private final ChoiceFieldType choice = new ChoiceFieldType();
    private final CompoundFieldType compound1 = createMock(CompoundFieldType.class);
    private final CompoundFieldType compound2 = createMock(CompoundFieldType.class);

    @Before
    public void setup() {
        choice.setId("choice");

        // choice field has 2 choices.
        choice.getChoices().put("compound1", compound1);
        choice.getChoices().put("compound2", compound2);

        expect(compound1.getId()).andReturn("compound1").anyTimes();
        expect(compound2.getId()).andReturn("compound2").anyTimes();
    }

    @Test
    public void choiceFieldType() {
        final ChoiceFieldType choice = new ChoiceFieldType();

        assertThat(choice.getType(), equalTo(FieldType.Type.CHOICE));
        assertTrue(choice.getChoices().isEmpty());
    }

    @Test
    public void initWithoutNode() {
        final ChoiceFieldType choice = new ChoiceFieldType();
        final FieldTypeContext context = prepareFieldTypeContextForInit(choice, null, null);

        FieldsInformation fieldsInfo = choice.init(context);

        verifyAll();

        assertThat(choice.getId(), equalTo("choiceId"));
        assertTrue(choice.getChoices().isEmpty());
        assertThat(fieldsInfo, equalTo(FieldsInformation.allSupported()));
    }

    @Test
    public void initWithNode() {
        final ChoiceFieldType choice = new ChoiceFieldType();
        final Node node = MockNode.root();
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final FieldTypeContext context = prepareFieldTypeContextForInit(choice, node, parentContext);

        PowerMock.mockStaticPartial(ChoiceFieldUtils.class, "populateProviderBasedChoices", "populateListBasedChoices");

        ChoiceFieldUtils.populateProviderBasedChoices(eq(node), eq(parentContext), eq(choice.getChoices()), anyObject(FieldsInformation.class));
        expectLastCall();
        ChoiceFieldUtils.populateListBasedChoices(eq(node), eq(parentContext), eq(choice.getChoices()), anyObject(FieldsInformation.class));
        expectLastCall();

        replayAll();

        FieldsInformation fieldsInfo = choice.init(context);

        verify(context);
        verifyAll();

        assertThat(choice.getId(), equalTo("choiceId"));
        assertTrue(choice.getChoices().isEmpty());
        assertThat(fieldsInfo, equalTo(FieldsInformation.allSupported()));
    }

    @Test
    public void readFromAbsent() {
        assertThat(choice.readFrom(MockNode.root()), equalTo(Optional.empty()));
    }

    @Test
    public void readFromOnePresent() throws Exception {
        final Node node = MockNode.root();
        final Node choiceNode = node.addNode("choice", "compound2");
        final FieldValue value = new FieldValue("bla");

        expect(compound2.readValue(choiceNode)).andReturn(value);
        replayAll();

        List<FieldValue> valueList = choice.readFrom(node).get();

        verifyAll();

        assertThat(valueList.size(), equalTo(1));
        assertThat(valueList.get(0).getChosenId(), equalTo("compound2"));
        assertThat(valueList.get(0).getChosenValue(), equalTo(value));
    }

    @Test
    public void readFromTwoPresentNotMultiple() throws Exception {
        final Node node = MockNode.root();
        final Node choiceNode1 = node.addNode("choice", "compound2");
        final Node choiceNode2 = node.addNode("choice", "compound1");
        final FieldValue value1 = new FieldValue("bla");
        final FieldValue value2 = new FieldValue("bla");

        expect(compound2.readValue(choiceNode1)).andReturn(value1);
        expect(compound1.readValue(choiceNode2)).andReturn(value2);
        replayAll();

        List<FieldValue> valueList = choice.readFrom(node).get();

        verifyAll();

        assertThat(valueList.size(), equalTo(1));
        assertThat(valueList.get(0).getChosenId(), equalTo("compound2"));
        assertThat(valueList.get(0).getChosenValue(), equalTo(value1));
        // The second value is trimmed
    }

    @Test
    public void readFromTwoPresentMultiple() throws Exception {
        final Node node = MockNode.root();
        final Node choiceNode1 = node.addNode("choice", "compound2");
        final Node choiceNode2 = node.addNode("choice", "compound1");
        final FieldValue value1 = new FieldValue("bla");
        final FieldValue value2 = new FieldValue("bla");

        choice.setMaxValues(Integer.MAX_VALUE);
        expect(compound2.readValue(choiceNode1)).andReturn(value1);
        expect(compound1.readValue(choiceNode2)).andReturn(value2);
        replayAll();

        List<FieldValue> valueList = choice.readFrom(node).get();

        verifyAll();

        assertThat(valueList.size(), equalTo(2));
        assertThat(valueList.get(0).getChosenId(), equalTo("compound2"));
        assertThat(valueList.get(0).getChosenValue(), equalTo(value1));
        assertThat(valueList.get(1).getChosenId(), equalTo("compound1"));
        assertThat(valueList.get(1).getChosenValue(), equalTo(value2));
    }

    @Test
    public void readFromWithException() throws Exception {
        final Node node = createMock(Node.class);

        expect(node.getNodes("choice")).andThrow(new RepositoryException());
        expect(node.getPath()).andReturn("path/location");

        replayAll();

        assertFalse(choice.readFrom(node).isPresent());

        verify(node);
    }

    @Test
    public void readFromWithError() throws Exception {
        final Node node = MockNode.root();
        final Node choiceNode1 = node.addNode("choice", "compound1");

        replayAll();

        try (Log4jInterceptor listener = Log4jInterceptor.onError().trap(ChoiceFieldType.class).build()) {
            try {
                choice.readFrom(choiceNode1);
            } finally {
                assertEquals(listener.messages().count(), 1L);
            }
        }

        verifyAll();
    }

    @Test
    public void writeToSingleValue() throws Exception {
        final Node node = MockNode.root();
        node.addNode("untouched", "bla");       // ignore
        node.addNode("choice", "compound2");    // update
        node.addNode("choice", "compound1");    // excess, remove
        node.addNode("choice", "unsupported");  // unsupported, remove

        final Node compound2Node = node.getNode("choice[1]");
        final FieldValue compoundValue = new FieldValue("bla");
        final FieldValue choiceValue = new FieldValue("compound2", compoundValue);

        compound2.writeValue(compound2Node, compoundValue);
        expectLastCall();
        replayAll();

        choice.writeTo(node, Optional.of(Collections.singletonList(choiceValue)));

        verifyAll();
        assertTrue(node.hasNode("untouched"));
        assertThat(node.getNodes("choice").getSize(), equalTo(1L));
    }

    @Test
    public void handlesUnsupportedValues() throws Exception {
        choice.setMaxValues(3);

        final Node node = MockNode.root();
        final Node compound1Node = node.addNode("choice", "compound1");     // update
        final Node unsupportedNode = node.addNode("choice", "unsupported"); // unsupported
        final Node compound2Node = node.addNode("choice", "compound2");     // update

        final FieldValue compound1Value = new FieldValue("bla1");
        final FieldValue compound2Value = new FieldValue("bla2");

        compound1.writeValue(compound1Node, compound1Value);
        expectLastCall();

        choice.writeValue(unsupportedNode, ChoiceFieldType.UNSUPPORTED_FIELD_VALUE);
        expectLastCall();

        compound2.writeValue(compound2Node, compound2Value);
        expectLastCall();
        replayAll();

        choice.writeTo(node, Optional.of(Lists.newArrayList(
                new FieldValue("compound1", compound1Value),
                new FieldValue("compound2", compound2Value)
        )));

        verifyAll();
        assertThat(node.getNodes("choice").getSize(), equalTo(3L));
        assertThat(node.getNode("choice[1]"), equalTo(compound1Node));
        assertThat(node.getNode("choice[2]"), equalTo(unsupportedNode));
        assertThat(node.getNode("choice[3]"), equalTo(compound2Node));
    }

    @Test
    public void writeToWithException() throws Exception {
        final Node node = createMock(Node.class);
        final FieldValue compoundValue = new FieldValue("bla");
        final FieldValue choiceValue = new FieldValue("compound2", compoundValue);

        expect(node.getNodes("choice")).andThrow(new RepositoryException());
        replayAll();

        try {
            choice.writeTo(node, Optional.of(Collections.singletonList(choiceValue)));
            fail("No exception");
        } catch (InternalServerErrorException e) {
            assertNull(e.getPayload());
        }

        verify(node);
    }

    @Test
    public void writeToWrongChoice() throws Exception {
        final Node node = MockNode.root();
        final FieldValue compoundValue = new FieldValue("bla");
        final FieldValue choiceValue = new FieldValue("compound1", compoundValue);

        final Node choiceNode = node.addNode("choice", "compound2");

        replayAll();

        try {
            choice.writeTo(node, Optional.of(Collections.singletonList(choiceValue)));
            fail("No exception");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }

        verifyAll();
        assertThat(node.getNode("choice"), equalTo(choiceNode));
    }

    @Test
    public void writeToInvalidChoice() throws Exception {
        final Node node = MockNode.root();
        final FieldValue compoundValue = new FieldValue("bla");
        final FieldValue choiceValue = new FieldValue("invalid", compoundValue);

        final Node choiceNode = node.addNode("choice", "compound2");

        replayAll();

        try {
            choice.writeTo(node, Optional.of(Collections.singletonList(choiceValue)));
            fail("No exception");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }

        verifyAll();
        assertThat(node.getNode("choice"), equalTo(choiceNode));
    }

    @Test
    public void writeToMissingChoiceId() throws Exception {
        final Node node = MockNode.root();
        final FieldValue compoundValue = new FieldValue("bla");
        final FieldValue choiceValue = new FieldValue("compound2", compoundValue);

        final Node choiceNode = node.addNode("choice", "compound2");
        choiceValue.setChosenId(null);

        replayAll();

        try {
            choice.writeTo(node, Optional.of(Collections.singletonList(choiceValue)));
            fail("No exception");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }

        verifyAll();
        assertThat(node.getNode("choice"), equalTo(choiceNode));
    }

    @Test
    public void writeToMissingChoiceValue() throws Exception {
        final Node node = MockNode.root();
        final FieldValue compoundValue = new FieldValue("bla");
        final FieldValue choiceValue = new FieldValue("compound2", compoundValue);

        final Node choiceNode = node.addNode("choice", "compound2");
        choiceValue.setChosenValue(null);

        replayAll();

        try {
            choice.writeTo(node, Optional.of(Collections.singletonList(choiceValue)));
            fail("No exception");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }

        verifyAll();
        assertThat(node.getNode("choice"), equalTo(choiceNode));
    }

    @Test
    public void writeToMultipleChoiceValues() throws Exception {
        final Node node = MockNode.root();
        final Node choiceNode1 = node.addNode("choice", "compound2");
        final Node choiceNode2 = node.addNode("choice", "compound1");
        final FieldValue compoundValue1 = new FieldValue("value for compound 2");
        final FieldValue choiceValue1 = new FieldValue("compound2", compoundValue1);
        final FieldValue compoundValue2 = new FieldValue("value for compound 1");
        final FieldValue choiceValue2 = new FieldValue("compound1", compoundValue2);

        choice.setMaxValues(Integer.MAX_VALUE);

        compound1.writeValue(choiceNode2, compoundValue2);
        expectLastCall();
        compound2.writeValue(choiceNode1, compoundValue1);
        expectLastCall();
        replayAll();

        choice.writeTo(node, Optional.of(Arrays.asList(choiceValue1, choiceValue2)));

        verifyAll();
    }

    @Test
    public void writeToMultipleCardinalityChange() throws Exception {
        final Node node = MockNode.root();
        final FieldValue compoundValue = new FieldValue("value for compound 2");
        final FieldValue choiceValue = new FieldValue("compound2", compoundValue);

        choice.setMaxValues(Integer.MAX_VALUE);

        node.addNode("choice", "compound2");
        node.addNode("choice", "compound1");
        replayAll();

        try {
            choice.writeTo(node, Optional.of(Collections.singletonList(choiceValue)));
            fail("No Exception");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.CARDINALITY_CHANGE));
        }

        verifyAll();
    }

    @Test
    public void writeFieldOtherId() throws ErrorWithPayloadException {
        final Node node = createMock(Node.class);
        final FieldPath fieldPath = new FieldPath("other:id");
        replayAll();

        assertFalse(choice.writeField(node, fieldPath, Collections.emptyList()));
        verify(node);
    }

    @Test
    public void writeFieldUnknownChildNode() throws ErrorWithPayloadException {
        final Node node = MockNode.root();
        final FieldPath fieldPath = new FieldPath("choice/unknown:child");
        final List<FieldValue> fieldValues = Collections.emptyList();

        try {
            choice.writeField(node, fieldPath, fieldValues);
            fail("Exception not thrown");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
    }

    @Test
    public void writeFieldGetChildFails() throws ErrorWithPayloadException, RepositoryException {
        final Node node = createMock(Node.class);
        final FieldPath fieldPath = new FieldPath("choice/child");

        expect(node.hasNode("choice")).andReturn(true);
        expect(node.getNode("choice")).andThrow(new RepositoryException());
        expect(node.getPath()).andReturn("/test");
        replayAll();

        try {
            choice.writeField(node, fieldPath, Collections.emptyList());
            fail("Exception not thrown");
        } catch (InternalServerErrorException e) {
            verify(node);
        }
    }

    @Test
    public void writeFieldOnlyChoice() throws ErrorWithPayloadException, RepositoryException {
        final Node node = MockNode.root();
        final Node choiceNode = node.addNode("choice", "compound1");

        final FieldPath fieldPath = new FieldPath("choice/somefield");
        final List<FieldValue> fieldValues = Collections.emptyList();

        expect(compound1.writeFieldValue(choiceNode, new FieldPath("somefield"), fieldValues)).andReturn(true);
        replayAll();

        assertTrue(choice.writeField(node, fieldPath, fieldValues));

        verify(compound1);
    }

    @Test
    public void writeFieldFirstChoice() throws ErrorWithPayloadException, RepositoryException {
        final Node node = MockNode.root();
        final Node choiceNode1 = node.addNode("choice", "compound1");
        node.addNode("choice", "compound2");

        final FieldPath fieldPath = new FieldPath("choice/somefield");
        final List<FieldValue> fieldValues = Collections.emptyList();

        expect(compound1.writeFieldValue(choiceNode1, new FieldPath("somefield"), fieldValues)).andReturn(true);
        replayAll();

        assertTrue(choice.writeField(node, fieldPath, fieldValues));

        verify(compound1);
    }

    @Test
    public void writeFieldSecondChoice() throws ErrorWithPayloadException, RepositoryException {
        final Node node = MockNode.root();
        node.addNode("choice", "compound1");
        final Node choiceNode2 = node.addNode("choice", "compound2");

        final FieldPath fieldPath = new FieldPath("choice[2]/somefield");
        final List<FieldValue> fieldValues = Collections.emptyList();

        expect(compound2.writeFieldValue(choiceNode2, new FieldPath("somefield"), fieldValues)).andReturn(true);
        replayAll();

        assertTrue(choice.writeField(node, fieldPath, fieldValues));

        verify(compound2);
    }

    @Test
    public void writeFieldUnknownChoice() throws ErrorWithPayloadException, RepositoryException {
        final Node node = MockNode.root();
        node.addNode("choice", "unknown:compound");

        final FieldPath fieldPath = new FieldPath("choice/somefield");
        final List<FieldValue> fieldValues = Collections.emptyList();

        try {
            choice.writeField(node, fieldPath, fieldValues);
            fail("Exception not thrown");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
    }

    @Test
    public void validateNone() {
        assertTrue(choice.validate(Collections.emptyList()));
    }

    @Test
    public void validateAllGood() {
        final FieldValue compoundValue1 = new FieldValue("value of compound 2");
        final FieldValue choiceValue1 = new FieldValue("compound2", compoundValue1);
        final FieldValue compoundValue2 = new FieldValue("value of compound 1");
        final FieldValue choiceValue2 = new FieldValue("compound1", compoundValue2);

        expect(compound2.validateValue(compoundValue1)).andReturn(true);
        expect(compound1.validateValue(compoundValue2)).andReturn(true);
        replayAll();

        assertTrue(choice.validate(Arrays.asList(choiceValue1, choiceValue2)));
        verifyAll();
    }

    @Test
    public void validateFirstBad() {
        final FieldValue compoundValue1 = new FieldValue("value of compound 2");
        final FieldValue choiceValue1 = new FieldValue("compound2", compoundValue1);
        final FieldValue compoundValue2 = new FieldValue("value of compound 1");
        final FieldValue choiceValue2 = new FieldValue("compound1", compoundValue2);

        expect(compound2.validateValue(compoundValue1)).andReturn(false);
        expect(compound1.validateValue(compoundValue2)).andReturn(true);
        replayAll();

        assertFalse(choice.validate(Arrays.asList(choiceValue1, choiceValue2)));
        verifyAll();
    }

    @Test
    public void validateLastBad() {
        final FieldValue compoundValue1 = new FieldValue("value of compound 2");
        final FieldValue choiceValue1 = new FieldValue("compound2", compoundValue1);
        final FieldValue compoundValue2 = new FieldValue("value of compound 1");
        final FieldValue choiceValue2 = new FieldValue("compound1", compoundValue2);

        expect(compound2.validateValue(compoundValue1)).andReturn(true);
        expect(compound1.validateValue(compoundValue2)).andReturn(false);
        replayAll();

        assertFalse(choice.validate(Arrays.asList(choiceValue1, choiceValue2)));
        verifyAll();
    }

    @Test
    public void validateAllBad() {
        final FieldValue compoundValue1 = new FieldValue("value of compound 2");
        final FieldValue choiceValue1 = new FieldValue("compound2", compoundValue1);
        final FieldValue compoundValue2 = new FieldValue("value of compound 1");
        final FieldValue choiceValue2 = new FieldValue("compound1", compoundValue2);

        expect(compound2.validateValue(compoundValue1)).andReturn(false);
        expect(compound1.validateValue(compoundValue2)).andReturn(false);
        replayAll();

        assertFalse(choice.validate(Arrays.asList(choiceValue1, choiceValue2)));
        verifyAll();
    }

    private FieldTypeContext prepareFieldTypeContextForInit(final ChoiceFieldType choice, final Node node,
                                                            final ContentTypeContext parentContext) {
        final FieldTypeContext context = createMock(FieldTypeContext.class);
        final ContentTypeContext pc = parentContext != null ? parentContext : createMock(ContentTypeContext.class);

        PowerMock.mockStaticPartial(FieldTypeUtils.class, "determineValidators");
        PowerMock.mockStaticPartial(LocalizationUtils.class, "determineFieldDisplayName", "determineFieldHint");

        expect(context.getParentContext()).andReturn(pc).anyTimes();
        expect(context.getEditorConfigNode()).andReturn(Optional.ofNullable(node)).anyTimes();
        expect(pc.getResourceBundle()).andReturn(Optional.empty());
        expect(pc.getDocumentType()).andReturn(null);
        expect(context.getName()).andReturn("choiceId");
        expect(context.getValidators()).andReturn(Collections.emptyList()).anyTimes();
        expect(context.isMultiple()).andReturn(true).anyTimes();
        expect(LocalizationUtils.determineFieldDisplayName("choiceId", Optional.empty(), Optional.ofNullable(node)))
                .andReturn(Optional.empty());
        expect(LocalizationUtils.determineFieldHint("choiceId", Optional.empty(), Optional.ofNullable(node)))
                .andReturn(Optional.empty());
        FieldTypeUtils.determineValidators(choice, null, Collections.emptyList());
        expectLastCall();

        replayAll();

        return context;
    }
}
