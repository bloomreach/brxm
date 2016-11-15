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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms7.services.contenttype.ContentTypeItem;
import org.onehippo.repository.mock.MockNode;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FieldTypeUtils.class, LocalizationUtils.class, ChoiceFieldUtils.class})
public class ChoiceFieldTypeTest {

    private final ChoiceFieldType choice = new ChoiceFieldType();
    private final CompoundFieldType compound1 = createMock(CompoundFieldType.class);
    private final CompoundFieldType compound2 = createMock(CompoundFieldType.class);

    @Before
    public void setup() {
        choice.setId("choice");

        // choice field has 2 choices.
        choice.getChoices().add(compound1);
        choice.getChoices().add(compound2);

        expect(compound1.getId()).andReturn("compound1").anyTimes();
        expect(compound2.getId()).andReturn("compound2").anyTimes();
    }

    @Test
    public void choiceFieldType() {
        final ChoiceFieldType choice = new ChoiceFieldType();

        assertThat(choice.getType(), equalTo(FieldType.Type.CHOICE));
        assertTrue(choice.getChoices().isEmpty());
        assertFalse(choice.isValid());

        choice.getChoices().add(new CompoundFieldType());
        assertTrue(choice.isValid());
    }

    @Test
    public void initWithoutNode() {
        final ChoiceFieldType choice = new ChoiceFieldType();
        final FieldTypeContext context = prepareFieldTypeContextForInit(choice, null, null);

        replay(context);

        choice.init(context);

        verify(context);
        PowerMock.verifyAll();

        assertThat(choice.getId(), equalTo("choiceId"));
        assertTrue(choice.getChoices().isEmpty());
    }

    @Test
    public void initWithNode() {
        final ChoiceFieldType choice = new ChoiceFieldType();
        final Node node = MockNode.root();
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final FieldTypeContext context = prepareFieldTypeContextForInit(choice, node, parentContext);

        PowerMock.mockStaticPartial(ChoiceFieldUtils.class, "populateProviderBasedChoices", "populateListBasedChoices");

        ChoiceFieldUtils.populateProviderBasedChoices(node, parentContext, choice.getChoices());
        expectLastCall();
        ChoiceFieldUtils.populateListBasedChoices(node, parentContext, choice.getChoices());
        expectLastCall();

        replay(context, parentContext);
        PowerMock.replayAll();

        choice.init(context);

        verify(context);
        PowerMock.verifyAll();

        assertThat(choice.getId(), equalTo("choiceId"));
        assertTrue(choice.getChoices().isEmpty());
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

        expect(compound2.readSingleFrom(choiceNode)).andReturn(value);
        replayAll();

        List<FieldValue> valueList = choice.readFrom(node).get();

        verifyAll();

        assertThat(valueList.size(), equalTo(1));
        assertThat(valueList.get(0).getChoiceId(), equalTo("compound2"));
        assertThat(valueList.get(0).getChoiceValue(), equalTo(value));
    }

    @Test
    public void readFromTwoPresentNotMultiple() throws Exception {
        final Node node = MockNode.root();
        final Node choiceNode1 = node.addNode("choice", "compound2");
        final Node choiceNode2 = node.addNode("choice", "compound1");
        final FieldValue value1 = new FieldValue("bla");
        final FieldValue value2 = new FieldValue("bla");

        expect(compound2.readSingleFrom(choiceNode1)).andReturn(value1);
        expect(compound1.readSingleFrom(choiceNode2)).andReturn(value2);
        replayAll();

        List<FieldValue> valueList = choice.readFrom(node).get();

        verifyAll();

        assertThat(valueList.size(), equalTo(1));
        assertThat(valueList.get(0).getChoiceId(), equalTo("compound2"));
        assertThat(valueList.get(0).getChoiceValue(), equalTo(value1));
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
        expect(compound2.readSingleFrom(choiceNode1)).andReturn(value1);
        expect(compound1.readSingleFrom(choiceNode2)).andReturn(value2);
        replayAll();

        List<FieldValue> valueList = choice.readFrom(node).get();

        verifyAll();

        assertThat(valueList.size(), equalTo(2));
        assertThat(valueList.get(0).getChoiceId(), equalTo("compound2"));
        assertThat(valueList.get(0).getChoiceValue(), equalTo(value1));
        assertThat(valueList.get(1).getChoiceId(), equalTo("compound1"));
        assertThat(valueList.get(1).getChoiceValue(), equalTo(value2));
    }

    @Test
    public void readFromWithException() throws Exception {
        final Node node = createMock(Node.class);

        expect(node.getNodes("choice")).andThrow(new RepositoryException());

        replay(node);

        assertFalse(choice.readFrom(node).isPresent());

        verify(node);
    }



    // TODO Add tests for writing and validating values


    /*
            */

    private void replayAll() {
        replay(compound1, compound2);
    }

    private void verifyAll() {
        verify(compound1, compound2);
    }

    private FieldTypeContext prepareFieldTypeContextForInit(final ChoiceFieldType choice, final Node node,
                                                            final ContentTypeContext parentContext) {
        final FieldTypeContext context = createMock(FieldTypeContext.class);
        final ContentTypeContext pc = parentContext != null ? parentContext : createMock(ContentTypeContext.class);
        final ContentTypeItem item = createMock(ContentTypeItem.class);

        PowerMock.mockStaticPartial(FieldTypeUtils.class, "determineValidators");
        PowerMock.mockStaticPartial(LocalizationUtils.class, "determineFieldDisplayName", "determineFieldHint");

        expect(context.getParentContext()).andReturn(pc).anyTimes();
        expect(context.getContentTypeItem()).andReturn(item);
        expect(context.getEditorConfigNode()).andReturn(Optional.ofNullable(node)).anyTimes();
        expect(pc.getResourceBundle()).andReturn(Optional.empty());
        expect(pc.getDocumentType()).andReturn(null);
        expect(item.getName()).andReturn("choiceId");
        expect(item.getValidators()).andReturn(Collections.emptyList()).anyTimes();
        expect(item.isMultiple()).andReturn(true);
        expect(LocalizationUtils.determineFieldDisplayName("choiceId", Optional.empty(), Optional.ofNullable(node)))
                .andReturn(Optional.empty());
        expect(LocalizationUtils.determineFieldHint("choiceId", Optional.empty(), Optional.ofNullable(node)))
                .andReturn(Optional.empty());
        FieldTypeUtils.determineValidators(choice, null, Collections.emptyList());
        expectLastCall();

        if (parentContext == null) {
            replay(pc);
        }
        replay(item);
        PowerMock.replayAll();

        return context;
    }
}
