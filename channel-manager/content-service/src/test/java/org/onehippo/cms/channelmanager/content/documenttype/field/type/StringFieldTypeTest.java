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

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.repository.mock.MockNode;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(PowerMockRunner.class)
@PrepareForTest(JcrUtils.class)
public class StringFieldTypeTest {

    private static final String PROPERTY = "test:id";

    @Test
    public void readFromSingleString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        node.setProperty(PROPERTY, "Value");

        assertThat(fieldType.readFrom(node).get(), equalTo("Value"));
    }

    @Test
    public void readFromSingleIncorrectString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);

        node.setProperty(PROPERTY, new String[]{"Value", "Ignore"});
        assertThat(fieldType.readFrom(node).get(), equalTo("Value"));

        node.setProperty(PROPERTY, new String[0]);
        assertThat(fieldType.readFrom(node).get(), equalTo(""));

        node.getProperty(PROPERTY).remove();
        assertThat(fieldType.readFrom(node).get(), equalTo(""));
    }

    @Test
    public void readFromOptionalString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setOptional(true);
        fieldType.setMultiple(true);

        assertThat(fieldType.readFrom(node).isPresent(), equalTo(false));

        node.setProperty(PROPERTY, "Value");
        assertThat(fieldType.readFrom(node).get(), equalTo(Collections.singletonList("Value")));
    }

    @Test
    public void readFromOptionalIncorrectString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setOptional(true);
        fieldType.setMultiple(true);

        node.setProperty(PROPERTY, new String[0]);
        assertThat(fieldType.readFrom(node).isPresent(), equalTo(false));

        node.setProperty(PROPERTY, new String[]{"Value", "Ignore"});
        assertThat(fieldType.readFrom(node).get(), equalTo(Collections.singletonList("Value")));
    }

    @Test
    public void readFromMultipleString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMultiple(true);

        assertThat(fieldType.readFrom(node).isPresent(), equalTo(false));

        node.setProperty(PROPERTY, new String[0]);
        assertThat(fieldType.readFrom(node).isPresent(), equalTo(false));

        node.setProperty(PROPERTY, new String[]{"Value 1", "Value 2"});
        assertThat(fieldType.readFrom(node).get(), equalTo(Arrays.asList("Value 1", "Value 2")));
    }

    @Test
    public void readFromMultipleIncorrectString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMultiple(true);

        node.setProperty(PROPERTY, "Value");
        assertThat(fieldType.readFrom(node).get(), equalTo(Collections.singletonList("Value")));

        fieldType.addValidator(FieldType.Validator.REQUIRED);
        node.getProperty(PROPERTY).remove();
        assertThat(fieldType.readFrom(node).get(), equalTo(Collections.singletonList("")));

        node.setProperty(PROPERTY, new String[0]);
        assertThat(fieldType.readFrom(node).get(), equalTo(Collections.singletonList("")));
    }

    @Test
    public void readFromException() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = createMock(Node.class);

        PowerMock.mockStaticPartial(JcrUtils.class, "getNodePathQuietly");

        fieldType.setId(PROPERTY);
        expect(node.hasProperty(PROPERTY)).andThrow(new RepositoryException());
        expect(JcrUtils.getNodePathQuietly(node)).andReturn("bla");
        replay(node);
        PowerMock.replayAll();

        assertThat(fieldType.readFrom(node).get(), equalTo(""));
    }



    @Test
    public void writeToSingleString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        node.setProperty(PROPERTY, "Old Value");

        assertThat(fieldType.writeTo(node, Optional.empty()), equalTo(1)); // must not be missing
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        assertThat(fieldType.writeTo(node, Optional.of(Boolean.TRUE)), equalTo(1)); // must be of type String
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        assertThat(fieldType.writeTo(node, Optional.of("New Value")), equalTo(0));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("New Value"));
    }

    @Test
    public void writeToSingleRequiredString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.addValidator(FieldType.Validator.REQUIRED);
        node.setProperty(PROPERTY, "Old Value");

        assertThat(fieldType.writeTo(node, Optional.empty()), equalTo(1)); // must not be missing
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        assertThat(fieldType.writeTo(node, Optional.of(Boolean.TRUE)), equalTo(1)); // must be of type String
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        assertThat(fieldType.writeTo(node, Optional.of("")), equalTo(1)); // must not be empty
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        assertThat(fieldType.writeTo(node, Optional.of("New Value")), equalTo(0));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("New Value"));
    }

    @Test
    public void writeToOptionalPresentString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setOptional(true);
        fieldType.setMultiple(true);
        node.setProperty(PROPERTY, "Old Value");

        assertThat(fieldType.writeTo(node, Optional.empty()), equalTo(0));
        assertThat(node.hasProperty(PROPERTY), equalTo(false));
        node.setProperty(PROPERTY, "Old Value");

        assertThat(fieldType.writeTo(node, Optional.of(Collections.emptyList())), equalTo(0));
        assertThat(node.hasProperty(PROPERTY), equalTo(false));
        node.setProperty(PROPERTY, "Old Value");

        assertThat(fieldType.writeTo(node, Optional.of("New Value")), equalTo(1)); // must be of type List
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("one", "two"))), equalTo(1)); // must have length 1
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList(Boolean.TRUE))), equalTo(1)); // element must be of type String
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("New Value"))), equalTo(0));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("New Value"));
    }

    @Test
    public void writeToOptionalPresentRequiredString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setOptional(true);
        fieldType.setMultiple(true);
        fieldType.addValidator(FieldType.Validator.REQUIRED);
        node.setProperty(PROPERTY, "Old Value");

        assertThat(fieldType.writeTo(node, Optional.empty()), equalTo(1)); // must not be missing
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        assertThat(fieldType.writeTo(node, Optional.of(Collections.emptyList())), equalTo(1)); // must not be missing
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        assertThat(fieldType.writeTo(node, Optional.of("New Value")), equalTo(1)); // must be of type List
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("one", "two"))), equalTo(1)); // must have length 1
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList(Boolean.TRUE))), equalTo(1)); // element must be of type String
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList(""))), equalTo(1)); // must not be empty
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("New Value"))), equalTo(0));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("New Value"));
    }

    @Test
    public void writeToOptionalAbsentString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setOptional(true);
        fieldType.setMultiple(true);

        assertThat(fieldType.writeTo(node, Optional.empty()), equalTo(0));
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList())), equalTo(0));
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        assertThat(fieldType.writeTo(node, Optional.of("New Value")), equalTo(1)); // must be of type List
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("one", "two"))), equalTo(1)); // must have length 1
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList(Boolean.TRUE))), equalTo(1)); // element must be of type String
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("New Value"))), equalTo(0));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("New Value"));
    }

    @Test
    public void writeToOptionalAbsentRequiredString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setOptional(true);
        fieldType.setMultiple(true);
        fieldType.addValidator(FieldType.Validator.REQUIRED);

        assertThat(fieldType.writeTo(node, Optional.empty()), equalTo(1)); // must not be missing
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList())), equalTo(1)); // must not be missing
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        assertThat(fieldType.writeTo(node, Optional.of("New Value")), equalTo(1)); // must be of type List
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("one", "two"))), equalTo(1)); // must have length 1
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList(Boolean.TRUE))), equalTo(1)); // element must be of type String
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("New Value"))), equalTo(0));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("New Value"));
    }

    @Test
    public void writeToMultiplePresentString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMultiple(true);
        node.setProperty(PROPERTY, new String[]{"Old 1", "Old 2"});

        assertThat(fieldType.writeTo(node, Optional.empty()), equalTo(0));
        assertThat(node.hasProperty(PROPERTY), equalTo(false));
        node.setProperty(PROPERTY, new String[]{"Old 1", "Old 2"});

        assertThat(fieldType.writeTo(node, Optional.of(Collections.emptyList())), equalTo(0));
        assertThat(node.hasProperty(PROPERTY), equalTo(false));
        node.setProperty(PROPERTY, new String[]{"Old 1", "Old 2"});

        assertThat(fieldType.writeTo(node, Optional.of("New 1")), equalTo(1)); // must be of type List
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(2));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("Single Value"))), equalTo(0));
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(1));
        node.setProperty(PROPERTY, new String[]{"Old 1", "Old 2"});

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("One", "Two", "Three"))), equalTo(0));
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(3));
        node.setProperty(PROPERTY, new String[]{"Old 1", "Old 2"});

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList(Boolean.TRUE, Boolean.TRUE))), equalTo(2)); // element must be of type String
        assertThat(node.getProperty(PROPERTY).getValues()[0].getString(), equalTo("Old 1"));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("New 1", Boolean.TRUE))), equalTo(1)); // ...all of them!
        assertThat(node.getProperty(PROPERTY).getValues()[0].getString(), equalTo("Old 1"));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("New 1", ""))), equalTo(0));
        assertThat(node.getProperty(PROPERTY).getValues()[0].getString(), equalTo("New 1"));
        assertThat(node.getProperty(PROPERTY).getValues()[1].getString(), equalTo(""));
    }

    @Test
    public void writeToMultiplePresentRequiredString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMultiple(true);
        fieldType.addValidator(FieldType.Validator.REQUIRED);
        node.setProperty(PROPERTY, new String[]{"Old 1", "Old 2"});

        assertThat(fieldType.writeTo(node, Optional.empty()), equalTo(1)); // must not be missing
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(2));

        assertThat(fieldType.writeTo(node, Optional.of(Collections.emptyList())), equalTo(1)); // must not be missing
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(2));

        assertThat(fieldType.writeTo(node, Optional.of("New 1")), equalTo(1)); // must be of type List
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(2));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("Single Value"))), equalTo(0));
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(1));
        node.setProperty(PROPERTY, new String[]{"Old 1", "Old 2"});

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("One", "Two", "Three"))), equalTo(0));
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(3));
        node.setProperty(PROPERTY, new String[]{"Old 1", "Old 2"});

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList(Boolean.TRUE, Boolean.TRUE))), equalTo(2)); // element must be of type String
        assertThat(node.getProperty(PROPERTY).getValues()[0].getString(), equalTo("Old 1"));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("New 1", Boolean.TRUE))), equalTo(1)); // ...all of them!
        assertThat(node.getProperty(PROPERTY).getValues()[0].getString(), equalTo("Old 1"));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("New 1", ""))), equalTo(1)); // all elements must be non-empty
        assertThat(node.getProperty(PROPERTY).getValues()[0].getString(), equalTo("Old 1"));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("", ""))), equalTo(2)); // all elements must be non-empty
        assertThat(node.getProperty(PROPERTY).getValues()[0].getString(), equalTo("Old 1"));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("New 1", "New 2"))), equalTo(0));
        assertThat(node.getProperty(PROPERTY).getValues()[0].getString(), equalTo("New 1"));
        assertThat(node.getProperty(PROPERTY).getValues()[1].getString(), equalTo("New 2"));
    }

    @Test
    public void writeToMultipleAbsentString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMultiple(true);

        assertThat(fieldType.writeTo(node, Optional.empty()), equalTo(0));
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        assertThat(fieldType.writeTo(node, Optional.of(Collections.emptyList())), equalTo(0));
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        assertThat(fieldType.writeTo(node, Optional.of("New 1")), equalTo(1)); // must be List
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList(Boolean.TRUE, Boolean.TRUE))), equalTo(2)); // element must be of type String
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("New 1", Boolean.TRUE))), equalTo(1)); // ...all of them!
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList(""))), equalTo(0));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo(""));
        node.getProperty(PROPERTY).remove();

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("One", "Two", "Three"))), equalTo(0));
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(3));
    }

    @Test
    public void writeToMultipleAbsentRequiredString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMultiple(true);
        fieldType.addValidator(FieldType.Validator.REQUIRED);

        assertThat(fieldType.writeTo(node, Optional.empty()), equalTo(1)); // must not be missing
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        assertThat(fieldType.writeTo(node, Optional.of(Collections.emptyList())), equalTo(1)); // must not be missing
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        assertThat(fieldType.writeTo(node, Optional.of(Boolean.TRUE)), equalTo(1)); // must be List
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList(Boolean.TRUE, ""))), equalTo(2)); // must be string and not empty
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("One", "Two"))), equalTo(0));
        assertThat(node.getProperty(PROPERTY).getValues()[0].getString(), equalTo("One"));
        assertThat(node.getProperty(PROPERTY).getValues()[1].getString(), equalTo("Two"));
    }

    @Test
    public void writeToMultipleIncorrectString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMultiple(true);
        node.setProperty(PROPERTY, "Old Value"); // singular property in spite of multiple type

        assertThat(fieldType.writeTo(node, Optional.empty()), equalTo(0));
        assertThat(node.hasProperty(PROPERTY), equalTo(false));
        node.setProperty(PROPERTY, "Old Value"); // singular property in spite of multiple type

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("One", "Two"))), equalTo(0));
        assertThat(node.getProperty(PROPERTY).isMultiple(), equalTo(true));
        node.setProperty(PROPERTY, "Old Value"); // singular property in spite of multiple type

        assertThat(fieldType.writeTo(node, Optional.of("New Value")), equalTo(1));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));
    }

    @Test
    public void writeToSingleIncorrectString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        node.setProperty(PROPERTY, new String[]{"Old Value"}); // multiple property in spite of singular type

        assertThat(fieldType.writeTo(node, Optional.empty()), equalTo(1));
        assertThat(node.hasProperty(PROPERTY), equalTo(true));

        assertThat(fieldType.writeTo(node, Optional.of("New Value")), equalTo(0));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("New Value"));
        node.setProperty(PROPERTY, new String[]{"Old Value"}); // multiple property in spite of singular type

        assertThat(fieldType.writeTo(node, Optional.of(Arrays.asList("New Value"))), equalTo(1));
        assertThat(node.getProperty(PROPERTY).isMultiple(), equalTo(true));
    }

    @Test
    public void writeToMultipleEmptyString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMultiple(true);
        node.setProperty(PROPERTY, new String[0]); // multiple, empty property

        assertThat(fieldType.writeTo(node, Optional.empty()), equalTo(0));
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(0)); // mutliple property still there

        assertThat(fieldType.writeTo(node, Optional.of(Collections.emptyList())), equalTo(0));
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(0)); // mutliple property still there
    }

    @Test
    public void writeToSingleException() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = createMock(Node.class);

        fieldType.setId(PROPERTY);
        expect(node.setProperty(PROPERTY, "New Value")).andThrow(new RepositoryException());
        replay(node);

        assertThat(fieldType.writeTo(node, Optional.of("New Value")), equalTo(1));
        verify(node);
    }

    @Test
    public void writeToMultipleException() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = createMock(Node.class);

        fieldType.setId(PROPERTY);
        fieldType.setMultiple(true);
        expect(node.hasProperty(PROPERTY)).andThrow(new RepositoryException());
        replay(node);

        assertThat(fieldType.writeTo(node, Optional.empty()), equalTo(1));
        verify(node);
    }
}
