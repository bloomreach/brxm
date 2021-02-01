/*
 * Copyright 2016-2021 Hippo B.V. (http://www.onehippo.com)
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
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest({AbstractFieldType.class, JcrUtils.class, ContentTypeContext.class, ChoiceFieldUtils.class,
        FieldTypeUtils.class, LocalizationUtils.class})
public class ChoiceFieldUtilsTest {

    @Test
    public void isChoiceFieldNoEditorConfigNode() {
        final FieldTypeContext context = createMock(FieldTypeContext.class);

        expect(context.getEditorConfigNode()).andReturn(Optional.empty());
        replayAll();

        assertFalse(ChoiceFieldUtils.isChoiceField(context));

        verifyAll();
    }

    @Test
    public void isChoiceFieldNoProperties() {
        final FieldTypeContext context = createMock(FieldTypeContext.class);
        final Node node = MockNode.root();

        expect(context.getEditorConfigNode()).andReturn(Optional.of(node));
        replayAll();

        assertFalse(ChoiceFieldUtils.isChoiceField(context));

        verifyAll();
    }

    @Test
    public void isChoiceFieldWithProvider() throws Exception {
        final FieldTypeContext context = createMock(FieldTypeContext.class);
        final Node node = MockNode.root();
        node.setProperty("cpItemsPath", "bla");

        expect(context.getEditorConfigNode()).andReturn(Optional.of(node));
        replayAll();

        assertTrue(ChoiceFieldUtils.isChoiceField(context));

        verifyAll();
    }

    @Test
    public void isChoiceFieldWithList() throws Exception {
        final FieldTypeContext context = createMock(FieldTypeContext.class);
        final Node node = MockNode.root();
        node.setProperty("compoundList", "bla");

        expect(context.getEditorConfigNode()).andReturn(Optional.of(node));
        replayAll();

        assertTrue(ChoiceFieldUtils.isChoiceField(context));

        verifyAll();
    }

    @Test
    public void isChoiceFieldWithException() throws Exception {
        final FieldTypeContext context = createMock(FieldTypeContext.class);
        final Node node = createMock(Node.class);

        expect(context.getEditorConfigNode()).andReturn(Optional.of(node));
        expect(node.hasProperty("cpItemsPath")).andThrow(new RepositoryException());
        replayAll();

        assertFalse(ChoiceFieldUtils.isChoiceField(context));

        verifyAll();
    }

    @Test
    public void populateProviderBasedChoicesWithNoProviderId() {
        final Node node = MockNode.root();
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();
        final FieldsInformation fieldsInfo = FieldsInformation.allSupported();

        ChoiceFieldUtils.populateProviderBasedChoices(node, parentContext, choices, fieldsInfo);

        assertTrue(choices.isEmpty());
        assertThat(fieldsInfo, equalTo(FieldsInformation.allSupported()));
    }

    @Test
    public void populateProviderBasedChoicesWithRepositoryException() throws Exception {
        final Node node = createMock(Node.class);
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();
        final FieldsInformation fieldsInfo = FieldsInformation.allSupported();

        PowerMock.mockStaticPartial(JcrUtils.class, "getNodePathQuietly");

        expect(node.hasProperty("cpItemsPath")).andThrow(new RepositoryException());
        expect(JcrUtils.getNodePathQuietly(node)).andReturn("/bla");

        replayAll();

        ChoiceFieldUtils.populateProviderBasedChoices(node, parentContext, choices, fieldsInfo);

        verifyAll();

        assertTrue(choices.isEmpty());
        assertThat(fieldsInfo, equalTo(FieldsInformation.allSupported()));
    }

    @Test
    public void populateProviderBasedChoicesWithoutContentType() throws Exception {
        final Node node = MockNode.root();
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();
        final FieldsInformation fieldsInfo = FieldsInformation.allSupported();

        PowerMock.mockStaticPartial(ContentTypeContext.class, "getContentType");

        node.setProperty("cpItemsPath", "nonexistent:type");
        expect(ContentTypeContext.getContentType("nonexistent:type")).andReturn(Optional.empty());

        replayAll();

        ChoiceFieldUtils.populateProviderBasedChoices(node, parentContext, choices, fieldsInfo);

        verifyAll();

        assertTrue(choices.isEmpty());
        assertThat(fieldsInfo, equalTo(FieldsInformation.allSupported()));
    }

    @Test
    public void populateProviderBasedChoicesWithChoiceWithoutContentType() throws Exception {
        final Node node = MockNode.root();
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();
        final ContentType provider = createMock(ContentType.class);
        final Map<String, ContentTypeChild> choiceMap = new HashMap<>();
        final ContentTypeChild choice = createMock(ContentTypeChild.class);
        final FieldsInformation fieldsInfo = FieldsInformation.allSupported();

        PowerMock.mockStaticPartial(ContentTypeContext.class, "getContentType");

        node.setProperty("cpItemsPath", "choice:provider");
        expect(ContentTypeContext.getContentType("choice:provider")).andReturn(Optional.of(provider));
        expect(provider.getChildren()).andReturn(choiceMap);

        // Choice has no content type, and gets not added.
        choiceMap.put("choice", choice);
        expect(choice.getItemType()).andReturn("choiceType");
        expect(ContentTypeContext.getContentType("choiceType")).andReturn(Optional.empty());

        replayAll();

        ChoiceFieldUtils.populateProviderBasedChoices(node, parentContext, choices, fieldsInfo);

        verifyAll();

        assertTrue(choices.isEmpty());
        assertThat(fieldsInfo, equalTo(FieldsInformation.allSupported()));
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
        final FieldsInformation fieldsInfo = FieldsInformation.allSupported();

        PowerMock.mockStaticPartial(ContentTypeContext.class, "getContentType");

        node.setProperty("cpItemsPath", "choice:provider");
        expect(ContentTypeContext.getContentType("choice:provider")).andReturn(Optional.of(provider));
        expect(provider.getChildren()).andReturn(choiceMap);

        // Choice has a content type and gets instantiated, but turns out to be invalid and gets not added
        choiceMap.put("choice", choice);
        expect(choice.getName()).andReturn("choiceName");
        expect(choice.getItemType()).andReturn("choiceType").anyTimes();
        expect(choice.getEffectiveType()).andReturn("String").anyTimes();
        expect(choice.isProperty()).andReturn(false);
        expect(choice.isMultiple()).andReturn(false);
        expect(choice.isOrdered()).andReturn(false);
        expect(choice.getValidators()).andReturn(Collections.emptyList());
        expect(ContentTypeContext.getContentType("choiceType")).andReturn(Optional.of(compound));
        expect(ContentTypeContext.getContentType("String")).andReturn(Optional.empty());
        expect(compound.isCompoundType()).andReturn(false);
        expect(compound.isContentType("hippostd:html")).andReturn(false);
        expect(compound.isContentType("hippogallerypicker:imagelink")).andReturn(false);
        expect(compound.isContentType("hippo:mirror")).andReturn(false);

        replayAll();

        ChoiceFieldUtils.populateProviderBasedChoices(node, parentContext, choices, fieldsInfo);

        verifyAll();

        assertTrue(choices.isEmpty());
        assertFalse(fieldsInfo.isAllFieldsIncluded());
        assertTrue(fieldsInfo.getCanCreateAllRequiredFields());
        assertThat(fieldsInfo.getUnsupportedFieldTypes(), equalTo(Collections.singleton("Custom")));
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
        final FieldTypeContext context = PowerMock.createMockAndExpectNew(FieldTypeContext.class, choice, parentContext);
        final CompoundFieldType compoundField = PowerMock.createMockAndExpectNew(CompoundFieldType.class);
        final FieldsInformation fieldsInfo = FieldsInformation.allSupported();

        PowerMock.mockStaticPartial(ContentTypeContext.class, "getContentType");

        node.setProperty("cpItemsPath", "choice:provider");
        expect(ContentTypeContext.getContentType("choice:provider")).andReturn(Optional.of(provider));
        expect(provider.getChildren()).andReturn(choiceMap);

        choiceMap.put("choice", choice);
        expect(choice.getItemType()).andReturn("choiceType").anyTimes();
        expect(ContentTypeContext.getContentType("choiceType")).andReturn(Optional.of(compound));
        expect(compound.isCompoundType()).andReturn(true);
        compoundField.initProviderBasedChoice(context, "choiceType");
        expectLastCall();
        expect(context.createContextForCompound()).andReturn(Optional.empty());

        replayAll();

        ChoiceFieldUtils.populateProviderBasedChoices(node, parentContext, choices, fieldsInfo);

        verifyAll();

        assertThat(choices.size(), equalTo(1));
        assertThat(choices.get("choiceType"), equalTo(compoundField));
        assertThat(fieldsInfo, equalTo(FieldsInformation.allSupported()));
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
        final FieldTypeContext context = PowerMock.createMockAndExpectNew(FieldTypeContext.class, choice, parentContext);
        final RichTextFieldType richTextField = PowerMock.createMockAndExpectNew(RichTextFieldType.class);
        final FieldsInformation fieldsInfo = FieldsInformation.allSupported();

        PowerMock.mockStaticPartial(ContentTypeContext.class, "getContentType");

        node.setProperty("cpItemsPath", "choice:provider");
        expect(ContentTypeContext.getContentType("choice:provider")).andReturn(Optional.of(provider));
        expect(provider.getChildren()).andReturn(choiceMap);

        choiceMap.put("choice", choice);
        expect(choice.getItemType()).andReturn("choiceType").anyTimes();
        expect(ContentTypeContext.getContentType("choiceType")).andReturn(Optional.of(compound));
        expect(compound.isCompoundType()).andReturn(false);
        expect(compound.isContentType("hippostd:html")).andReturn(true);

        expect(richTextField.init(context)).andReturn(FieldsInformation.allSupported());
        expect(context.createContextForCompound()).andReturn(Optional.empty());

        replayAll();

        ChoiceFieldUtils.populateProviderBasedChoices(node, parentContext, choices, fieldsInfo);

        verifyAll();

        assertThat(choices.size(), equalTo(1));
        assertThat(choices.get("choiceType"), equalTo(richTextField));
        assertThat(fieldsInfo, equalTo(FieldsInformation.allSupported()));
    }

    @Test
    public void populateListBasedChoicesWithoutCompoundList() {
        final Node node = MockNode.root();
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();

        ChoiceFieldUtils.populateListBasedChoices(node, parentContext, choices, null);

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

        replayAll();

        ChoiceFieldUtils.populateListBasedChoices(node, parentContext, choices, null);

        verifyAll();

        assertTrue(choices.isEmpty());
    }

    @Test
    public void populateListBasedChoicesWithChoicesWithoutContentType() throws Exception {
        final Node node = MockNode.root();
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();
        final ContentType contentType = createMock(ContentType.class);
        final FieldsInformation fieldsInfo = FieldsInformation.allSupported();

        PowerMock.mockStaticPartial(ContentTypeContext.class, "createFromParent");

        node.setProperty("compoundList", "  one,  two  ,namespaced:three,four  ");
        expect(parentContext.getContentType()).andReturn(contentType).anyTimes();
        expect(contentType.getPrefix()).andReturn("prefix").anyTimes();
        expect(ContentTypeContext.createFromParent("prefix:one", parentContext)).andReturn(Optional.empty());
        expect(ContentTypeContext.createFromParent("prefix:two", parentContext)).andReturn(Optional.empty());
        expect(ContentTypeContext.createFromParent("namespaced:three", parentContext)).andReturn(Optional.empty());
        expect(ContentTypeContext.createFromParent("prefix:four", parentContext)).andReturn(Optional.empty());

        replayAll();

        ChoiceFieldUtils.populateListBasedChoices(node, parentContext, choices, fieldsInfo);

        verifyAll();

        assertTrue(choices.isEmpty());
        assertThat(fieldsInfo, equalTo(FieldsInformation.allSupported()));
    }

    @Test
    public void populateListBasedChoicesWithNonCompoundChoice() throws Exception {
        final Node node = MockNode.root();
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();
        final ContentTypeContext childContext = createMock(ContentTypeContext.class);
        final ContentType compound = createMock(ContentType.class);
        final FieldsInformation fieldsInfo = FieldsInformation.allSupported();

        PowerMock.mockStaticPartial(ContentTypeContext.class, "createFromParent");

        node.setProperty("compoundList", "prefixed:choice");
        expect(ContentTypeContext.createFromParent("prefixed:choice", parentContext)).andReturn(Optional.of(childContext));
        expect(childContext.getContentType()).andReturn(compound).anyTimes();
        expect(compound.getName()).andReturn("NonCompound");
        expect(compound.getValidators()).andReturn(Collections.emptyList());
        expect(compound.isCompoundType()).andReturn(false);
        expect(compound.isContentType("hippostd:html")).andReturn(false);
        expect(compound.isContentType("hippogallerypicker:imagelink")).andReturn(false);
        expect(compound.isContentType("hippo:mirror")).andReturn(false);

        replayAll();

        ChoiceFieldUtils.populateListBasedChoices(node, parentContext, choices, fieldsInfo);

        verifyAll();

        assertTrue(choices.isEmpty());
        assertFalse(fieldsInfo.isAllFieldsIncluded());
        assertTrue(fieldsInfo.getCanCreateAllRequiredFields());
        assertThat(fieldsInfo.getUnsupportedFieldTypes(), equalTo(Collections.singleton("Custom")));
    }

    @Test
    public void populateListBasedChoicesWithValidCompound() throws Exception {
        final Node node = MockNode.root();
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();
        final ContentTypeContext childContext = createMock(ContentTypeContext.class);
        final ContentType compound = createMock(ContentType.class);
        final CompoundFieldType compoundField = PowerMock.createMockAndExpectNew(CompoundFieldType.class);
        final FieldsInformation fieldsInfo = FieldsInformation.allSupported();

        PowerMock.mockStaticPartial(ContentTypeContext.class, "createFromParent");
        PowerMock.mockStaticPartial(FieldTypeUtils.class, "populateFields");

        node.setProperty("compoundList", "prefixed:choice");
        expect(ContentTypeContext.createFromParent("prefixed:choice", parentContext)).andReturn(Optional.of(childContext));
        expect(childContext.getContentType()).andReturn(compound).anyTimes();
        expect(compound.isCompoundType()).andReturn(true);
        expect(compound.getName()).andReturn("compound:id");
        expect(compound.getValidators()).andReturn(Collections.singletonList("compound-validator"));

        compoundField.initListBasedChoice(childContext, "compound:id");
        expectLastCall();
        expect(compoundField.getDisplayName()).andReturn("bla");
        compoundField.addValidatorName("compound-validator");
        expectLastCall();

        replayAll();

        ChoiceFieldUtils.populateListBasedChoices(node, parentContext, choices, fieldsInfo);

        verifyAll();

        assertThat(choices.size(), equalTo(1));
        assertThat(choices.get("compound:id"), equalTo(compoundField));
        assertThat(fieldsInfo, equalTo(FieldsInformation.allSupported()));
    }

    @Test
    public void populateListBasedChoicesWithValidCompoundAndPatchedDisplayName() throws Exception {
        final Node node = MockNode.root();
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();
        final ContentTypeContext childContext = createMock(ContentTypeContext.class);
        final ContentType compound = createMock(ContentType.class);
        final CompoundFieldType compoundField = PowerMock.createMockAndExpectNew(CompoundFieldType.class);
        final FieldsInformation fieldsInfo = FieldsInformation.allSupported();

        PowerMock.mockStaticPartial(ContentTypeContext.class, "createFromParent");
        PowerMock.mockStaticPartial(FieldTypeUtils.class, "populateFields");
        PowerMock.mockStaticPartial(LocalizationUtils.class, "determineDocumentDisplayName");

        node.setProperty("compoundList", "prefixed:choice");
        expect(ContentTypeContext.createFromParent("prefixed:choice", parentContext)).andReturn(Optional.of(childContext));
        expect(childContext.getContentType()).andReturn(compound).anyTimes();
        expect(compound.isCompoundType()).andReturn(true);
        expect(compound.getName()).andReturn("compound:id");
        compoundField.initListBasedChoice(childContext, "compound:id");
        expectLastCall();
        expect(compoundField.getId()).andReturn("compound:id");
        expect(compoundField.getDisplayName()).andReturn(null);
        expect(compound.getValidators()).andReturn(Collections.emptyList());
        expect(childContext.getResourceBundle()).andReturn(Optional.empty());
        expect(LocalizationUtils.determineDocumentDisplayName("compound:id", Optional.empty()))
                .andReturn(Optional.of("Patched Display Name"));
        compoundField.setDisplayName("Patched Display Name");
        expectLastCall();

        replayAll();

        ChoiceFieldUtils.populateListBasedChoices(node, parentContext, choices, fieldsInfo);

        verifyAll();

        assertThat(choices.size(), equalTo(1));
        assertThat(choices.get("compound:id"), equalTo(compoundField));
        assertThat(fieldsInfo, equalTo(FieldsInformation.allSupported()));
    }

    @Test
    public void populateListBasedChoicesWithValidRichText() throws Exception {
        final Node node = MockNode.root();
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        final Map<String, NodeFieldType> choices = new HashMap<>();
        final ContentTypeContext childContext = createMock(ContentTypeContext.class);
        final ContentType compound = createMock(ContentType.class);
        final RichTextFieldType richTextField = PowerMock.createMockAndExpectNew(RichTextFieldType.class);
        final FieldTypeContext richTextFieldContext = PowerMock.createMockAndExpectNew(FieldTypeContext.class,
                "hippostd:html", "hippostd:html", "hippostd:html", false, false, false, Collections.emptyList(), childContext, null);
        final FieldsInformation fieldsInfo = FieldsInformation.allSupported();

        PowerMock.mockStaticPartial(ContentTypeContext.class, "createFromParent");
        PowerMock.mockStaticPartial(FieldTypeUtils.class, "populateFields");

        node.setProperty("compoundList", "prefixed:choice");
        expect(ContentTypeContext.createFromParent("prefixed:choice", parentContext)).andReturn(Optional.of(childContext));
        expect(childContext.getContentType()).andReturn(compound).anyTimes();
        expect(compound.isCompoundType()).andReturn(false);
        expect(compound.isContentType("hippostd:html")).andReturn(true);
        expect(compound.getName()).andReturn("hippostd:html");
        expect(compound.getValidators()).andReturn(Collections.emptyList());

        expect(richTextField.init(richTextFieldContext)).andReturn(FieldsInformation.allSupported());
        expect(richTextField.getDisplayName()).andReturn("bla");

        replayAll();

        ChoiceFieldUtils.populateListBasedChoices(node, parentContext, choices, fieldsInfo);

        verifyAll();

        assertThat(choices.size(), equalTo(1));
        assertThat(choices.get("hippostd:html"), equalTo(richTextField));
        assertThat(fieldsInfo, equalTo(FieldsInformation.allSupported()));
    }
}
