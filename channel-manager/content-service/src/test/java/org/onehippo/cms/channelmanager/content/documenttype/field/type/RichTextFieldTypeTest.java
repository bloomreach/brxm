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

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.testutils.log4j.Log4jListener;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MockNode.class)
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
            Node value = document.addNode(FIELD_NAME, "hippostd:html");
            value.setProperty("hippostd:content", html);
            return value;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertNoWarningsLogged(Runnable test) throws Exception {
        try (Log4jListener listener = Log4jListener.onWarn()) {
            test.run();
            assertThat("no warnings logged", listener.messages().count(), equalTo(0L));
        }
    }

    @Test
    public void readSingleValue() throws Exception {
        addValue("<p>value</p>");
        assertNoWarningsLogged(() -> {
            List<FieldValue> fieldValues = type.readFrom(document).get();
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
            List<FieldValue> fieldValues = type.readFrom(document).get();
            assertThat(fieldValues.size(), equalTo(2));
            assertThat(fieldValues.get(0).getValue(), equalTo("<p>one</p>"));
            assertThat(fieldValues.get(1).getValue(), equalTo("<p>two</p>"));
        });
    }

    @Test
    public void readOptionalEmptyValue() throws Exception {
        assertNoWarningsLogged(() -> {
            assertFalse(type.readFrom(document).isPresent());
        });
    }

    @Test
    public void exceptionWhileReading() throws Exception {
        // make hippostd:content property multi-valued so reading it will throw an exception
        Node value = addValue("");
        value.getProperty("hippostd:content").setValue(new String[]{"one", "two"});

        try (Log4jListener listener = Log4jListener.onWarn()) {
            assertFalse(type.readFrom(document).isPresent());
            assertThat(listener.messages().count(), equalTo(1L));
        }
    }
}