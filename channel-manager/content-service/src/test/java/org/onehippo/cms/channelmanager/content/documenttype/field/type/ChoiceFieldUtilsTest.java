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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.documenttype.util.LocalizationUtils;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeChild;
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
@PrepareForTest({JcrUtils.class, ContentTypeContext.class, ChoiceFieldUtils.class, FieldTypeUtils.class,
        LocalizationUtils.class})
public class ChoiceFieldUtilsTest {

    @Test
    public void isChoiceFieldNoEditorConfigNode() {
        final FieldTypeContext context = createMock(FieldTypeContext.class);

        expect(context.getEditorConfigNode()).andReturn(Optional.empty());
        replay(context);

        assertFalse(ChoiceFieldUtils.isChoiceField(context));

        verify(context);
    }

    @Test
    public void isChoiceFieldNoProperties() {
        final FieldTypeContext context = createMock(FieldTypeContext.class);
        final Node node = MockNode.root();

        expect(context.getEditorConfigNode()).andReturn(Optional.of(node));
        replay(context);

        assertFalse(ChoiceFieldUtils.isChoiceField(context));

        verify(context);
    }

    @Test
    public void isChoiceFieldWithProvider() throws Exception {
        final FieldTypeContext context = createMock(FieldTypeContext.class);
        final Node node = MockNode.root();
        node.setProperty("cpItemsPath", "bla");

        expect(context.getEditorConfigNode()).andReturn(Optional.of(node));
        replay(context);

        assertTrue(ChoiceFieldUtils.isChoiceField(context));

        verify(context);
    }

    @Test
    public void isChoiceFieldWithList() throws Exception {
        final FieldTypeContext context = createMock(FieldTypeContext.class);
        final Node node = MockNode.root();
        node.setProperty("compoundList", "bla");

        expect(context.getEditorConfigNode()).andReturn(Optional.of(node));
        replay(context);

        assertTrue(ChoiceFieldUtils.isChoiceField(context));

        verify(context);
    }

    @Test
    public void isChoiceFieldWithException() throws Exception {
        final FieldTypeContext context = createMock(FieldTypeContext.class);
        final Node node = createMock(Node.class);

        expect(context.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(node.hasProperty("cpItemsPath")).andThrow(new RepositoryException());
        replay(context, node);

        assertFalse(ChoiceFieldUtils.isChoiceField(context));

        verify(context, node);
    }

    @Test
    public void populateProviderBasedChoicesWithNoProviderId() {
        final Node node = MockNode.root();
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();

        ChoiceFieldUtils.populateProviderBasedChoices(node, parentContext, choices);

        assertTrue(choices.isEmpty());
    }

    @Test
    public void populateProviderBasedChoicesWithRepositoryException() throws Exception {
        final Node node = createMock(Node.class);
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();

        PowerMock.mockStaticPartial(JcrUtils.class, "getNodePathQuietly");

        expect(node.hasProperty("cpItemsPath")).andThrow(new RepositoryException());
        expect(JcrUtils.getNodePathQuietly(node)).andReturn("/bla");

        replay(node);
        PowerMock.replayAll();

        ChoiceFieldUtils.populateProviderBasedChoices(node, parentContext, choices);

        verify(node);
        PowerMock.verifyAll();

        assertTrue(choices.isEmpty());
    }

    @Test
    public void populateProviderBasedChoicesWithoutContentType() throws Exception {
        final Node node = MockNode.root();
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();

        PowerMock.mockStaticPartial(ContentTypeContext.class, "getContentType");

        node.setProperty("cpItemsPath", "nonexistent:type");
        expect(ContentTypeContext.getContentType("nonexistent:type")).andReturn(Optional.empty());

        PowerMock.replayAll();

        ChoiceFieldUtils.populateProviderBasedChoices(node, parentContext, choices);

        PowerMock.verifyAll();

        assertTrue(choices.isEmpty());
    }

    @Test
    public void populateProviderBasedChoicesWithChoiceWithoutContentType() throws Exception {
        final Node node = MockNode.root();
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();
        final ContentType provider = createMock(ContentType.class);
        final Map<String, ContentTypeChild> choiceMap = new HashMap<>();
        final ContentTypeChild choice = createMock(ContentTypeChild.class);

        PowerMock.mockStaticPartial(ContentTypeContext.class, "getContentType");

        node.setProperty("cpItemsPath", "choice:provider");
        expect(ContentTypeContext.getContentType("choice:provider")).andReturn(Optional.of(provider));
        expect(provider.getChildren()).andReturn(choiceMap);

        // Choice has no content type, and gets not added.
        choiceMap.put("choice", choice);
        expect(choice.getItemType()).andReturn("choiceType");
        expect(ContentTypeContext.getContentType("choiceType")).andReturn(Optional.empty());

        replay(provider, choice);
        PowerMock.replayAll();

        ChoiceFieldUtils.populateProviderBasedChoices(node, parentContext, choices);

        verify(provider, choice);
        PowerMock.verifyAll();

        assertTrue(choices.isEmpty());
    }

    @Test
    public void populateProviderBasedChoicesWithNonCompoundChoice() throws Exception {
        final Node node = MockNode.root();
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();
        final ContentType provider = createMock(ContentType.class);
        final Map<String, ContentTypeChild> choiceMap = new HashMap<>();
        final ContentTypeChild choice = createMock(ContentTypeChild.class);
        final ContentType compound = createMock(ContentType.class);

        PowerMock.mockStaticPartial(ContentTypeContext.class, "getContentType");

        node.setProperty("cpItemsPath", "choice:provider");
        expect(ContentTypeContext.getContentType("choice:provider")).andReturn(Optional.of(provider));
        expect(provider.getChildren()).andReturn(choiceMap);

        // Choice has a content type and gets instantiated, but turns out to be invalid and gets not added
        choiceMap.put("choice", choice);
        expect(choice.getItemType()).andReturn("choiceType");
        expect(ContentTypeContext.getContentType("choiceType")).andReturn(Optional.of(compound));
        expect(compound.isCompoundType()).andReturn(false);
        expect(compound.isContentType("hippostd:html")).andReturn(false);

        replay(provider, choice, compound);
        PowerMock.replayAll();

        ChoiceFieldUtils.populateProviderBasedChoices(node, parentContext, choices);

        verify(provider, choice, compound);
        PowerMock.verifyAll();

        assertTrue(choices.isEmpty());
    }

    @Test
    public void initProviderIdWithValidCompoundChoice() throws Exception {
        final Node node = MockNode.root();
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();
        final ContentType provider = createMock(ContentType.class);
        final Map<String, ContentTypeChild> choiceMap = new HashMap<>();
        final ContentTypeChild choice = createMock(ContentTypeChild.class);
        final ContentType compound = createMock(ContentType.class);
        final FieldTypeContext compoundContext = PowerMock.createMockAndExpectNew(FieldTypeContext.class, choice, parentContext);
        final CompoundFieldType compoundField = PowerMock.createMockAndExpectNew(CompoundFieldType.class);

        PowerMock.mockStaticPartial(ContentTypeContext.class, "getContentType");

        node.setProperty("cpItemsPath", "choice:provider");
        expect(ContentTypeContext.getContentType("choice:provider")).andReturn(Optional.of(provider));
        expect(provider.getChildren()).andReturn(choiceMap);

        choiceMap.put("choice", choice);
        expect(choice.getItemType()).andReturn("choiceType").anyTimes();
        expect(ContentTypeContext.getContentType("choiceType")).andReturn(Optional.of(compound));
        expect(compound.isCompoundType()).andReturn(true).times(2);
        compoundField.initProviderBasedChoice(compoundContext, "choiceType");
        expectLastCall();
        expect(compoundContext.createContextForCompound()).andReturn(Optional.empty());

        replay(provider, choice, compound);
        PowerMock.replayAll();

        ChoiceFieldUtils.populateProviderBasedChoices(node, parentContext, choices);

        verify(provider, choice, compound);
        PowerMock.verifyAll();

        assertThat(choices.size(), equalTo(1));
        assertThat(choices.get("choiceType"), equalTo(compoundField));
    }

    @Test
    public void initProviderIdWithValidRichTextChoice() throws Exception {
        final Node node = MockNode.root();
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();
        final ContentType provider = createMock(ContentType.class);
        final Map<String, ContentTypeChild> choiceMap = new HashMap<>();
        final ContentTypeChild choice = createMock(ContentTypeChild.class);
        final ContentType compound = createMock(ContentType.class);
        final FieldTypeContext compoundContext = PowerMock.createMockAndExpectNew(FieldTypeContext.class, choice, parentContext);
        final RichTextFieldType richTextField = PowerMock.createMockAndExpectNew(RichTextFieldType.class);

        PowerMock.mockStaticPartial(ContentTypeContext.class, "getContentType");

        node.setProperty("cpItemsPath", "choice:provider");
        expect(ContentTypeContext.getContentType("choice:provider")).andReturn(Optional.of(provider));
        expect(provider.getChildren()).andReturn(choiceMap);

        choiceMap.put("choice", choice);
        expect(choice.getItemType()).andReturn("choiceType").anyTimes();
        expect(ContentTypeContext.getContentType("choiceType")).andReturn(Optional.of(compound));
        expect(compound.isCompoundType()).andReturn(false).times(2);
        expect(compound.isContentType("hippostd:html")).andReturn(true).times(2);

        richTextField.init(compoundContext);
        expectLastCall();

        expect(compoundContext.createContextForCompound()).andReturn(Optional.empty());

        replay(provider, choice, compound);
        PowerMock.replayAll();

        ChoiceFieldUtils.populateProviderBasedChoices(node, parentContext, choices);

        verify(provider, choice, compound);
        PowerMock.verifyAll();

        assertThat(choices.size(), equalTo(1));
        assertThat(choices.get("choiceType"), equalTo(richTextField));
    }

    @Test
    public void populateListBasedChoicesWithoutCompoundList() {
        final Node node = MockNode.root();
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();

        ChoiceFieldUtils.populateListBasedChoices(node, parentContext, choices);

        assertTrue(choices.isEmpty());
    }

    @Test
    public void populateListBasedChoicesWithException() throws Exception {
        final Node node = createMock(Node.class);
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();

        PowerMock.mockStaticPartial(JcrUtils.class, "getNodePathQuietly");

        expect(node.hasProperty("compoundList")).andThrow(new RepositoryException());
        expect(JcrUtils.getNodePathQuietly(node)).andReturn("/bla");

        replay(node);
        PowerMock.replayAll();

        ChoiceFieldUtils.populateListBasedChoices(node, parentContext, choices);

        verify(node);
        PowerMock.verifyAll();

        assertTrue(choices.isEmpty());
    }

    @Test
    public void populateListBasedChoicesWithChoicesWithoutContentType() throws Exception {
        final Node node = MockNode.root();
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();
        final ContentType contentType = createMock(ContentType.class);

        PowerMock.mockStaticPartial(ContentTypeContext.class, "createFromParent");

        node.setProperty("compoundList", "  one,  two  ,namespaced:three,four  ");
        expect(parentContext.getContentType()).andReturn(contentType).anyTimes();
        expect(contentType.getPrefix()).andReturn("prefix").anyTimes();
        expect(ContentTypeContext.createFromParent("prefix:one", parentContext)).andReturn(Optional.empty());
        expect(ContentTypeContext.createFromParent("prefix:two", parentContext)).andReturn(Optional.empty());
        expect(ContentTypeContext.createFromParent("namespaced:three", parentContext)).andReturn(Optional.empty());
        expect(ContentTypeContext.createFromParent("prefix:four", parentContext)).andReturn(Optional.empty());

        replay(parentContext, contentType);
        PowerMock.replayAll();

        ChoiceFieldUtils.populateListBasedChoices(node, parentContext, choices);

        verify(parentContext, contentType);
        PowerMock.verifyAll();

        assertTrue(choices.isEmpty());
    }

    @Test
    public void populateListBasedChoicesWithNonCompoundChoice() throws Exception {
        final Node node = MockNode.root();
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();
        final ContentTypeContext childContext = createMock(ContentTypeContext.class);
        final ContentType compound = createMock(ContentType.class);

        PowerMock.mockStaticPartial(ContentTypeContext.class, "createFromParent");

        node.setProperty("compoundList", "prefixed:choice");
        expect(ContentTypeContext.createFromParent("prefixed:choice", parentContext)).andReturn(Optional.of(childContext));
        expect(childContext.getContentType()).andReturn(compound);
        expect(compound.isCompoundType()).andReturn(false);
        expect(compound.isContentType("hippostd:html")).andReturn(false);

        replay(parentContext, childContext, compound);
        PowerMock.replayAll();

        ChoiceFieldUtils.populateListBasedChoices(node, parentContext, choices);

        verify(parentContext, childContext, compound);
        PowerMock.verifyAll();

        assertTrue(choices.isEmpty());
    }

    @Test
    public void populateListBasedChoicesWithValidCompound() throws Exception {
        final Node node = MockNode.root();
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();
        final ContentTypeContext childContext = createMock(ContentTypeContext.class);
        final ContentType compound = createMock(ContentType.class);
        final CompoundFieldType compoundField = PowerMock.createMockAndExpectNew(CompoundFieldType.class);

        PowerMock.mockStaticPartial(ContentTypeContext.class, "createFromParent");
        PowerMock.mockStaticPartial(FieldTypeUtils.class, "populateFields");

        node.setProperty("compoundList", "prefixed:choice");
        expect(ContentTypeContext.createFromParent("prefixed:choice", parentContext)).andReturn(Optional.of(childContext));
        expect(childContext.getContentType()).andReturn(compound).anyTimes();
        expect(compound.isCompoundType()).andReturn(true).times(2);
        expect(compound.getName()).andReturn("compound:id");

        compoundField.initListBasedChoice(childContext, "compound:id");
        expectLastCall();
        expect(compoundField.getDisplayName()).andReturn("bla");

        replay(parentContext, childContext, compound);
        PowerMock.replayAll();

        ChoiceFieldUtils.populateListBasedChoices(node, parentContext, choices);

        verify(parentContext, childContext, compound);
        PowerMock.verifyAll();

        assertThat(choices.size(), equalTo(1));
        assertThat(choices.get("compound:id"), equalTo(compoundField));
    }

    @Test
    public void populateListBasedChoicesWithValidCompoundAndPatchedDisplayName() throws Exception {
        final Node node = MockNode.root();
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();
        final ContentTypeContext childContext = createMock(ContentTypeContext.class);
        final ContentType compound = createMock(ContentType.class);
        final CompoundFieldType compoundField = PowerMock.createMockAndExpectNew(CompoundFieldType.class);

        PowerMock.mockStaticPartial(ContentTypeContext.class, "createFromParent");
        PowerMock.mockStaticPartial(FieldTypeUtils.class, "populateFields");
        PowerMock.mockStaticPartial(LocalizationUtils.class, "determineDocumentDisplayName");

        node.setProperty("compoundList", "prefixed:choice");
        expect(ContentTypeContext.createFromParent("prefixed:choice", parentContext)).andReturn(Optional.of(childContext));
        expect(childContext.getContentType()).andReturn(compound).anyTimes();
        expect(compound.isCompoundType()).andReturn(true).times(2);
        expect(compound.getName()).andReturn("compound:id");
        compoundField.initListBasedChoice(childContext, "compound:id");
        expectLastCall();
        expect(compoundField.getId()).andReturn("compound:id");
        expect(compoundField.getDisplayName()).andReturn(null);
        expect(childContext.getResourceBundle()).andReturn(Optional.empty());
        expect(LocalizationUtils.determineDocumentDisplayName("compound:id", Optional.empty()))
                .andReturn(Optional.of("Patched Display Name"));
        compoundField.setDisplayName("Patched Display Name");
        expectLastCall();

        replay(parentContext, childContext, compound);
        PowerMock.replayAll();

        ChoiceFieldUtils.populateListBasedChoices(node, parentContext, choices);

        verify(parentContext, childContext, compound);
        PowerMock.verifyAll();

        assertThat(choices.size(), equalTo(1));
        assertThat(choices.get("compound:id"), equalTo(compoundField));
    }

    @Test
    public void populateListBasedChoicesWithValidRichText() throws Exception {
        final Node node = MockNode.root();
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();
        final ContentTypeContext childContext = createMock(ContentTypeContext.class);
        final ContentType compound = createMock(ContentType.class);
        final RichTextFieldType richTextField = PowerMock.createMockAndExpectNew(RichTextFieldType.class);

        PowerMock.mockStaticPartial(ContentTypeContext.class, "createFromParent");
        PowerMock.mockStaticPartial(FieldTypeUtils.class, "populateFields");

        node.setProperty("compoundList", "prefixed:choice");
        expect(ContentTypeContext.createFromParent("prefixed:choice", parentContext)).andReturn(Optional.of(childContext));
        expect(childContext.getContentType()).andReturn(compound).anyTimes();
        expect(compound.isCompoundType()).andReturn(false).times(2);
        expect(compound.isContentType("hippostd:html")).andReturn(true).times(2);
        expect(compound.getName()).andReturn("hippostd:html");

        richTextField.initListBasedChoice("hippostd:html");
        expectLastCall();

        expect(richTextField.getDisplayName()).andReturn("bla");

        replay(parentContext, childContext, compound);
        PowerMock.replayAll();

        ChoiceFieldUtils.populateListBasedChoices(node, parentContext, choices);

        verify(parentContext, childContext, compound);
        PowerMock.verifyAll();

        assertThat(choices.size(), equalTo(1));
        assertThat(choices.get("hippostd:html"), equalTo(richTextField));
    }
}
