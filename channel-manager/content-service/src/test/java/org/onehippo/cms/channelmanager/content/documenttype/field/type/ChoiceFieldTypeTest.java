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
import java.util.Optional;

import javax.jcr.Node;

import org.junit.Test;
import org.junit.runner.RunWith;
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

    // TODO Add tests for reading, writing and validating values


    /*
            */

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
