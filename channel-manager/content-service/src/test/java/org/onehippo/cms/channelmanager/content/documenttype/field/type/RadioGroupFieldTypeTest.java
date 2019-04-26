/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

import javax.jcr.Node;

import org.junit.Test;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.forge.selection.frontend.plugin.Config;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;

public class RadioGroupFieldTypeTest {

    private static final String PROPERTY = "test:id";

    private FieldTypeContext getFieldTypeContext() {
        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        expect(parentContext.getLocale()).andReturn(new Locale("en"));
        expect(parentContext.getTimeZone()).andReturn(TimeZone.getTimeZone("Europe/Amsterdam"));
        expect(parentContext.getDocumentType()).andReturn(new DocumentType());
        expect(parentContext.getResourceBundle()).andReturn(Optional.empty());

        final FieldTypeContext fieldContext = createMock(FieldTypeContext.class);
        expect(fieldContext.getJcrName()).andReturn("myproject:radiogroup").anyTimes();
        expect(fieldContext.getValidators()).andReturn(Collections.emptyList());
        expect(fieldContext.isMultiple()).andReturn(false).anyTimes();
        expect(fieldContext.getEditorConfigNode()).andReturn(Optional.empty()).anyTimes();
        expect(fieldContext.getStringConfig("maxlength")).andReturn(Optional.empty());
        expect(fieldContext.getParentContext()).andReturn(parentContext).anyTimes();

        return fieldContext;
    }

    @Test
    public void testFieldConfig() {
        FieldTypeContext fieldTypeContext = getFieldTypeContext();
        expect(fieldTypeContext.getStringConfig(Config.SOURCE)).andReturn(Optional.of("/source/path"));
        expect(fieldTypeContext.getStringConfig(Config.SORT_COMPARATOR)).andReturn(Optional.of("my.Comparator"));
        expect(fieldTypeContext.getStringConfig(Config.SORT_BY)).andReturn(Optional.of("key"));
        expect(fieldTypeContext.getStringConfig(Config.SORT_ORDER)).andReturn(Optional.of("descending"));
        expect(fieldTypeContext.getStringConfig(Config.ORIENTATION)).andReturn(Optional.of("vertical"));

        replayAll();

        RadioGroupFieldType radioGroupFieldType = new RadioGroupFieldType();
        radioGroupFieldType.init(fieldTypeContext);

        assertThat(radioGroupFieldType.getSource(), equalTo("/source/path"));
        assertThat(radioGroupFieldType.getSortComparator(), equalTo("my.Comparator"));
        assertThat(radioGroupFieldType.getSortBy(), equalTo("key"));
        assertThat(radioGroupFieldType.getSortOrder(), equalTo("descending"));
        assertThat(radioGroupFieldType.getOrientation(), equalTo("vertical"));
    }

    @Test
    public void writeToSingleDouble() throws Exception {
        final Node node = MockNode.root();
        final PrimitiveFieldType fieldType = new RadioGroupFieldType();
        final String oldValue = "one";
        final String newValue = "two";

        fieldType.setId(PROPERTY);
        node.setProperty(PROPERTY, oldValue);

        try {
            fieldType.writeTo(node, Optional.empty());
            fail("Must not be missing");
        } catch (final BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getString(), equalTo(oldValue));

        try {
            fieldType.writeTo(node, Optional.of(Collections.emptyList()));
            fail("Must have 1 entry");
        } catch (final BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("11"), valueOf("12"))));
            fail("Must have 1 entry");
        } catch (final BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }

        fieldType.writeTo(node, Optional.of(listOf(valueOf(newValue))));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo(newValue));
    }

    private static List<FieldValue> listOf(final FieldValue value) {
        return Collections.singletonList(value);
    }

    private static FieldValue valueOf(final String value) {
        return new FieldValue(value);
    }
}
