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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;

import org.hippoecm.repository.util.JcrUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.util.NamespaceUtils;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo.Reason;
import org.onehippo.repository.mock.MockNode;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({JcrUtils.class, NamespaceUtils.class})
public class StringFieldTypeTest {

    private static final String PROPERTY = "test:id";
    private StringFieldType fieldType;
    private FieldTypeContext context;
    private Node node;

    @Before
    public void setup() {
        PowerMock.mockStatic(JcrUtils.class);
        PowerMock.mockStatic(NamespaceUtils.class);

        fieldType = new StringFieldType();
        context = createMock(FieldTypeContext.class);
        node = MockNode.root();
    }

    @Test
    public void initializeMaxLengthNoMaxLength() throws Exception {
        expect(context.getStringConfig("maxlength")).andReturn(Optional.empty());

        replay(context);

        fieldType.initializeMaxLength(context);
        assertNull(fieldType.getMaxLength());

        verify(context);
    }

    @Test
    public void initializeMaxLengthBadFormat() throws Exception {
        expect(context.getStringConfig("maxlength")).andReturn(Optional.of("bad format"));

        PowerMock.replayAll(context);

        fieldType.initializeMaxLength(context);
        assertNull(fieldType.getMaxLength());

        PowerMock.verifyAll();
    }

    @Test
    public void initializeMaxLengthGoodFormat() throws Exception {
        expect(context.getStringConfig("maxlength")).andReturn(Optional.of("123"));

        PowerMock.replayAll(context);

        fieldType.initializeMaxLength(context);
        assertThat(fieldType.getMaxLength(), equalTo(123L));

        PowerMock.verifyAll();
    }

    @Test
    public void writeToSingleTooLong() throws Exception {
        fieldType.setId(PROPERTY);
        fieldType.setMaxLength("10");
        node.setProperty(PROPERTY, "Old Value");

        try {
            fieldType.writeTo(node, Optional.of(Collections.singletonList(valueOf("Too longggg"))));
            fail("Must not be too long");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        fieldType.writeTo(node, Optional.of(Collections.singletonList(valueOf("New Value!"))));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("New Value!"));
    }

    @Test
    public void writeToMultipleTooLong() throws Exception {
        fieldType.setId(PROPERTY);
        fieldType.setMaxValues(Integer.MAX_VALUE);
        fieldType.setMinValues(0);
        fieldType.setMaxLength("10");
        fieldType.setMultiple(true);

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("okay"), valueOf("Too longggg"))));
            fail("Must not be too long");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(Reason.INVALID_DATA));
        }
        assertFalse(node.hasProperty(PROPERTY));

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("Too longggg"), valueOf("okay"))));
            fail("Must not be too long");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(Reason.INVALID_DATA));
        }
        assertFalse(node.hasProperty(PROPERTY));

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("Too longggg"), valueOf("Too longggg"))));
            fail("Must not be too long");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(Reason.INVALID_DATA));
        }
        assertFalse(node.hasProperty(PROPERTY));

        fieldType.writeTo(node, Optional.of(Arrays.asList(valueOf("New Value!"), valueOf("New Value!"))));
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(2));
    }

    private List<FieldValue> listOf(final FieldValue value) {
        return Collections.singletonList(value);
    }

    private FieldValue valueOf(final String value) {
        return new FieldValue(value);
    }

}
