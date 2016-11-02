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
import java.util.List;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms.channelmanager.content.documenttype.field.validation.ValidationErrorInfo;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
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
import static org.junit.Assert.fail;

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

        try {
            fieldType.writeTo(node, Optional.empty());
            fail("Must not be missing");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        try {
            fieldType.writeTo(node, Optional.of(Boolean.TRUE));
            fail("Must be of type String");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        fieldType.writeTo(node, Optional.of("New Value"));
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

        fieldType.writeTo(node, Optional.empty());
        assertThat(node.hasProperty(PROPERTY), equalTo(false));
        node.setProperty(PROPERTY, "Old Value");

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertThat(node.hasProperty(PROPERTY), equalTo(false));
        node.setProperty(PROPERTY, "Old Value");

        try {
            fieldType.writeTo(node, Optional.of("New Value"));
            fail("Must be of type List");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList("one", "two")));
            fail("Must have length 1");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(Boolean.TRUE)));
            fail("Element must be of type String");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));

        fieldType.writeTo(node, Optional.of(Arrays.asList("New Value")));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("New Value"));
    }

    @Test
    public void writeToOptionalAbsentString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setOptional(true);
        fieldType.setMultiple(true);

        fieldType.writeTo(node, Optional.empty());
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        fieldType.writeTo(node, Optional.of(Arrays.asList()));
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        try {
            fieldType.writeTo(node, Optional.of("New Value"));
            fail("Must be of type List");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList("one", "two")));
            fail("Must have length 1");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(Boolean.TRUE)));
            fail("Element must be of type String");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        fieldType.writeTo(node, Optional.of(Arrays.asList("New Value")));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("New Value"));
    }

    @Test
    public void writeToMultiplePresentString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMultiple(true);
        node.setProperty(PROPERTY, new String[]{"Old 1", "Old 2"});

        fieldType.writeTo(node, Optional.empty());
        assertThat(node.hasProperty(PROPERTY), equalTo(false));
        node.setProperty(PROPERTY, new String[]{"Old 1", "Old 2"});

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertThat(node.hasProperty(PROPERTY), equalTo(false));
        node.setProperty(PROPERTY, new String[]{"Old 1", "Old 2"});

        try {
            fieldType.writeTo(node, Optional.of("New 1"));
            fail("Must be of type List");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(2));

        fieldType.writeTo(node, Optional.of(Arrays.asList("Single Value")));
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(1));
        node.setProperty(PROPERTY, new String[]{"Old 1", "Old 2"});

        fieldType.writeTo(node, Optional.of(Arrays.asList("One", "Two", "Three")));
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(3));
        node.setProperty(PROPERTY, new String[]{"Old 1", "Old 2"});

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(Boolean.TRUE, Boolean.TRUE)));
            fail("Element must be of type String");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getValues()[0].getString(), equalTo("Old 1"));

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList("New 1", Boolean.TRUE)));
            fail("...all of them!");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getValues()[0].getString(), equalTo("Old 1"));

        fieldType.writeTo(node, Optional.of(Arrays.asList("New 1", "")));
        assertThat(node.getProperty(PROPERTY).getValues()[0].getString(), equalTo("New 1"));
        assertThat(node.getProperty(PROPERTY).getValues()[1].getString(), equalTo(""));
    }

    @Test
    public void writeToMultipleAbsentString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMultiple(true);

        fieldType.writeTo(node, Optional.empty());
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        try {
            fieldType.writeTo(node, Optional.of("New 1"));
            fail("Must be List");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList(Boolean.TRUE, Boolean.TRUE)));
            fail("Element must be of type String");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList("New 1", Boolean.TRUE)));
            fail("...all of them!");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.hasProperty(PROPERTY), equalTo(false));

        fieldType.writeTo(node, Optional.of(Arrays.asList("")));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo(""));
        node.getProperty(PROPERTY).remove();

        fieldType.writeTo(node, Optional.of(Arrays.asList("One", "Two", "Three")));
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(3));
    }

    @Test
    public void writeToMultipleIncorrectString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMultiple(true);
        node.setProperty(PROPERTY, "Old Value"); // singular property in spite of multiple type

        fieldType.writeTo(node, Optional.empty());
        assertThat(node.hasProperty(PROPERTY), equalTo(false));
        node.setProperty(PROPERTY, "Old Value"); // singular property in spite of multiple type

        fieldType.writeTo(node, Optional.of(Arrays.asList("One", "Two")));
        assertThat(node.getProperty(PROPERTY).isMultiple(), equalTo(true));
        node.setProperty(PROPERTY, "Old Value"); // singular property in spite of multiple type

        try {
            fieldType.writeTo(node, Optional.of("New Value"));
            fail("Must be List");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("Old Value"));
    }

    @Test
    public void writeToSingleIncorrectString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        node.setProperty(PROPERTY, new String[]{"Old Value"}); // multiple property in spite of singular type

        try {
            fieldType.writeTo(node, Optional.empty());
            fail("Must be String");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.hasProperty(PROPERTY), equalTo(true));

        fieldType.writeTo(node, Optional.of("New Value"));
        assertThat(node.getProperty(PROPERTY).getString(), equalTo("New Value"));
        node.setProperty(PROPERTY, new String[]{"Old Value"}); // multiple property in spite of singular type

        try {
            fieldType.writeTo(node, Optional.of(Arrays.asList("New Value")));
            fail("Must be String");
        } catch (BadRequestException e) {
            assertThat(((ErrorInfo) e.getPayload()).getReason(), equalTo(ErrorInfo.Reason.INVALID_DATA));
        }
        assertThat(node.getProperty(PROPERTY).isMultiple(), equalTo(true));
    }

    @Test
    public void writeToMultipleEmptyString() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = MockNode.root();

        fieldType.setId(PROPERTY);
        fieldType.setMultiple(true);
        node.setProperty(PROPERTY, new String[0]); // multiple, empty property

        fieldType.writeTo(node, Optional.empty());
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(0)); // mutliple property still there

        fieldType.writeTo(node, Optional.of(Collections.emptyList()));
        assertThat(node.getProperty(PROPERTY).getValues().length, equalTo(0)); // mutliple property still there
    }

    @Test
    public void writeToSingleException() throws Exception {
        final StringFieldType fieldType = new StringFieldType();
        final Node node = createMock(Node.class);

        fieldType.setId(PROPERTY);
        expect(node.setProperty(PROPERTY, "New Value")).andThrow(new RepositoryException());
        replay(node);

        try {
            fieldType.writeTo(node, Optional.of("New Value"));
            fail("Exception not thrown");
        } catch (InternalServerErrorException e) {
            assertThat(e.getPayload(), equalTo(null));
        }
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

        try {
            fieldType.writeTo(node, Optional.empty());
            fail("Exception not thrown");
        } catch (InternalServerErrorException e) {
            assertThat(e.getPayload(), equalTo(null));
        }
        verify(node);
    }

    @Test
    public void validateNonRequired() {
        final StringFieldType fieldType = new StringFieldType();

        assertThat(fieldType.validate(Optional.empty()).isPresent(), equalTo(false));
        assertThat(fieldType.validate(Optional.of("")).isPresent(), equalTo(false));
        assertThat(fieldType.validate(Optional.of("blabla")).isPresent(), equalTo(false));
        assertThat(fieldType.validate(Optional.of(Collections.emptyList())).isPresent(), equalTo(false));
        assertThat(fieldType.validate(Optional.of(Arrays.asList("one", "two"))).isPresent(), equalTo(false));
    }

    @Test
    public void validateSingleRequired() {
        final StringFieldType fieldType = new StringFieldType();

        fieldType.addValidator(FieldType.Validator.REQUIRED);

        assertThat(((ValidationErrorInfo) fieldType.validate(Optional.of("")).get()).getCode(),
                equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));
        assertThat(fieldType.validate(Optional.of("blabla")).isPresent(), equalTo(false));
    }

    @Test
    public void validateMultipleRequired() {
        final StringFieldType fieldType = new StringFieldType();

        fieldType.addValidator(FieldType.Validator.REQUIRED);

        assertThat(((ValidationErrorInfo) fieldType.validate(Optional.empty()).get()).getCode(),
                equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_ABSENT));

        Object error = fieldType.validate(Optional.of(Arrays.asList("bla", ""))).get();
        assertThat((error instanceof List), equalTo(true));
        List<ValidationErrorInfo> errorList = (List<ValidationErrorInfo>) error;
        assertThat(errorList.size(), equalTo(2));
        assertThat(errorList.get(0).getCode(), equalTo(null));
        assertThat(errorList.get(1).getCode(), equalTo(ValidationErrorInfo.Code.REQUIRED_FIELD_EMPTY));

        assertThat(fieldType.validate(Optional.of(Arrays.asList("bla", "blo"))).isPresent(), equalTo(false));
    }
}
