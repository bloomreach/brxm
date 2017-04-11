/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeUtils;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.testutils.log4j.Log4jListener;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.powermock.api.easymock.PowerMock.replayAll;

@RunWith(PowerMockRunner.class)
@PrepareForTest(FieldTypeUtils.class)
@Ignore
public class RichTextFieldTypeTest {

    private static final String FIELD_NAME = "test:richtextfield";

    private Node document;
    private RichTextFieldType type;

    @Before
    public void setUp() throws RepositoryException {
        document = MockNode.root();
        type = new RichTextFieldType();
        type.setId(FIELD_NAME);
    }

    private Node addValue(final String html) {
        try {
            final Node value = document.addNode(FIELD_NAME, "hippostd:html");
            value.setProperty("hippostd:content", html);
            return value;
        } catch (final RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    private interface Code {
        void run() throws Exception;
    }

    private void assertWarningsLogged(final long count, final Code code, final Code... verifications) throws Exception {
        try (Log4jListener listener = Log4jListener.onWarn()) {
            try {
                code.run();
            } finally {
                assertThat(count + " warnings logged", listener.messages().count(), equalTo(count));
                for (final Code verification : verifications) {
                    verification.run();
                }
            }
        }
    }

    private void assertNoWarningsLogged(final Code code, final Code... verifications) throws Exception {
        assertWarningsLogged(0, code, verifications);
    }

    @Test
    public void readSingleValue() throws Exception {
        addValue("<p>value</p>");
        assertNoWarningsLogged(() -> {
            final List<FieldValue> fieldValues = type.readFrom(document).get();
            assertThat(fieldValues.size(), equalTo(1));
            assertThat(fieldValues.get(0).getValue(), equalTo("<p>value</p>"));
        });
    }

    @Test
    public void readMultipleValue() throws Exception {
        addValue("<p>one</p>");
        addValue("<p>two</p>");
        type.setMaxValues(2);
        assertNoWarningsLogged(() -> {
            final List<FieldValue> fieldValues = type.readFrom(document).get();
            assertThat(fieldValues.size(), equalTo(2));
            assertThat(fieldValues.get(0).getValue(), equalTo("<p>one</p>"));
            assertThat(fieldValues.get(1).getValue(), equalTo("<p>two</p>"));
        });
    }

    @Test
    public void readOptionalEmptyValue() throws Exception {
        assertNoWarningsLogged(() -> assertFalse(type.readFrom(document).isPresent()));
    }

    @Test
    public void exceptionWhileReading() throws Exception {
        // make hippostd:content property multi-valued so reading it will throw an exception
        final Node value = addValue("");
        value.getProperty("hippostd:content").setValue(new String[]{"one", "two"});

        assertWarningsLogged(1, () -> assertFalse(type.readFrom(document).isPresent()));
    }

    @Test
    public void writeSingleValue() throws Exception {
        addValue("<p>value</p>");
        assertNoWarningsLogged(() -> {
            final FieldValue newValue = new FieldValue("<p>changed</p>");

            type.writeTo(document, Optional.of(Collections.singletonList(newValue)));

            final List<FieldValue> fieldValues = type.readFrom(document).get();
            assertThat(fieldValues.size(), equalTo(1));
            assertThat(fieldValues.get(0).getValue(), equalTo("<p>changed</p>"));
        });
    }

    @Test
    public void writeMultipleValues() throws Exception {
        addValue("<p>one</p>");
        addValue("<p>two</p>");
        type.setMaxValues(2);
        assertNoWarningsLogged(() -> {
            final FieldValue newValue1 = new FieldValue("<p>one changed</p>");
            final FieldValue newValue2 = new FieldValue("<p>two changed</p>");

            type.writeTo(document, Optional.of(Arrays.asList(newValue1, newValue2)));

            final List<FieldValue> fieldValues = type.readFrom(document).get();
            assertThat(fieldValues.size(), equalTo(2));
            assertThat(fieldValues.get(0).getValue(), equalTo("<p>one changed</p>"));
            assertThat(fieldValues.get(1).getValue(), equalTo("<p>two changed</p>"));
        });
    }

    @Test
    public void writeOptionalEmptyValue() throws Exception {
        type.setMinValues(0);
        assertNoWarningsLogged(() -> {
            type.writeTo(document, Optional.of(Collections.emptyList()));
            assertFalse(type.readFrom(document).isPresent());
        });
    }

    @Test(expected = BadRequestException.class)
    public void writeLessValuesThanMinimum() throws Exception {
        // default minimum is 1
        type.writeTo(document, Optional.of(Collections.emptyList()));
    }

    @Test(expected = BadRequestException.class)
    public void writeMoreValuesThanMaximum() throws Exception {
        // default maximum is 1
        final FieldValue newValue1 = new FieldValue("<p>one</p>");
        final FieldValue newValue2 = new FieldValue("<p>two</p>");
        type.writeTo(document, Optional.of(Arrays.asList(newValue1, newValue2)));
    }

    @Test
    public void writeLessValuesThanStored() throws Exception {
        addValue("<p>one</p>");
        addValue("<p>two</p>");
        addValue("<p>three</p>");
        assertThat(document.getNodes().getSize(), equalTo(3L));

        assertNoWarningsLogged(
                () -> {
                    final FieldValue newValue = new FieldValue("<p>changed</p>");
                    type.writeTo(document, Optional.of(Collections.singletonList(newValue)));
                }, () -> {
                    final List<FieldValue> fieldValues = type.readFrom(document).get();
                    assertThat(fieldValues.size(), equalTo(1));
                    assertThat(fieldValues.get(0).getValue(), equalTo("<p>changed</p>"));
                    assertThat(document.getNodes().getSize(), equalTo(1L));
                });
    }

    @Test(expected = InternalServerErrorException.class)
    public void exceptionWhileWriting() throws Exception {
        PowerMock.mockStaticPartial(FieldTypeUtils.class, "writeCompoundValues");
        FieldTypeUtils.writeCompoundValues(anyObject(), anyObject(), anyInt(), anyObject());
        expectLastCall().andThrow(new RepositoryException());
        replayAll();

        addValue("<p>value</p>");
        final FieldValue newValue = new FieldValue("<p>changed</p>");

        assertWarningsLogged(1,
                () -> type.writeTo(document, Optional.of(Collections.singletonList(newValue))),
                () -> {
                    final List<FieldValue> fieldValues = type.readFrom(document).get();
                    assertThat(fieldValues.size(), equalTo(1));
                    assertThat(fieldValues.get(0).getValue(), equalTo("<p>value</p>"));
                });
    }
}