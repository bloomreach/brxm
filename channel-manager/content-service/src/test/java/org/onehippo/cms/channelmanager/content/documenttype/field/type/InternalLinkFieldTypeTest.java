/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.addon.frontend.gallerypicker.ImageItem;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.type.FieldType.Type;
import org.onehippo.cms.channelmanager.content.error.BadRequestException;
import org.onehippo.cms.channelmanager.content.error.InternalServerErrorException;
import org.onehippo.repository.mock.MockNode;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.JsonNode;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.support.membermodification.MemberMatcher.methodsDeclaredIn;
import static org.powermock.api.support.membermodification.MemberModifier.suppress;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest({AbstractFieldType.class})
public class InternalLinkFieldTypeTest {

    private String documentItemUrl;
    private InternalLinkFieldType documentLink;

    @Before
    public void setUp() {
        final ImageItem mockImageItem = createMock(ImageItem.class);
        expect(mockImageItem.getPrimaryUrl(anyObject())).andAnswer(() -> documentItemUrl).anyTimes();
        replayAll();

        documentLink = new InternalLinkFieldType();
        documentItemUrl =  "http://example.com";
    }

    @Test
    public void constructor() {
        assertThat(new InternalLinkFieldType().getType(), equalTo(Type.INTERNAL_LINK));
    }

    @Test
    public void init() throws Exception {
        // prevent re-testing the init method of the super class
        suppress(methodsDeclaredIn(AbstractFieldType.class));

        final MockNode editorConfigNode = MockNode.root();

        // put the default bootstrapped configuration in a fake 'cluster.options' node to simplify testing
        final MockNode clusterOptions = editorConfigNode.addNode("cluster.options", "frontend:plugincluster");
        clusterOptions.setProperty("base.path", "");
        clusterOptions.setProperty("base.uuid", "");
        clusterOptions.setProperty("cluster.name", "cms-pickers/documents-only");
        clusterOptions.setProperty("last.visited.enabled", "true");
        clusterOptions.setProperty("last.visited.key", "");
        clusterOptions.setProperty("last.visited.last.visited.nodetypes", "");
        clusterOptions.setProperty("nodetypes", new String[0]);

        final FieldTypeContext context = new FieldTypeContext(null, null, false, false, null, null, editorConfigNode);
        documentLink.init(context);

        final JsonNode imagePickerConfig = documentLink.getConfig().get("imagepicker");
        assertThat(imagePickerConfig.get("base.path").asText(), equalTo(""));
        assertThat(imagePickerConfig.get("base.uuid").asText(), equalTo(""));
        assertThat(imagePickerConfig.get("cluster.name").asText(), equalTo("cms-pickers/documents-only"));
        assertThat(imagePickerConfig.get("last.visited.enabled").asText(), equalTo("true"));
        assertThat(imagePickerConfig.get("last.visited.key").asText(), equalTo(""));
        assertThat(imagePickerConfig.get("last.visited.nodetypes").asText(), equalTo(""));
        assertThat(imagePickerConfig.get("nodetypes").size(), equalTo(0));
    }

    @Test
    public void getDefault() {
        assertThat(documentLink.getDefault(), equalTo(""));
    }

    @Test
    public void getPropertyType() {
        assertThat(documentLink.getPropertyType(), equalTo(PropertyType.STRING));
    }

    @Test
    public void readMissingValues() {
        documentLink.setId("my:documentlink");
        final MockNode documentNode = MockNode.root();
        final List<FieldValue> fieldValues = documentLink.readValues(documentNode);
        assertTrue(fieldValues.isEmpty());
    }

    @Test
    public void readSingleValue() throws Exception {
        documentLink.setId("my:documentlink");

        final MockNode documentNode = MockNode.root();
        final MockNode documentLinkNode = documentNode.addNode("my:documentlink", "hippogallypicker:imagelink");
        documentLinkNode.setProperty("hippo:docbase", "1234");

        final List<FieldValue> fieldValues = documentLink.readValues(documentNode);
        assertThat(fieldValues.size(), equalTo(1));
        final FieldValue fieldValue = fieldValues.get(0);
        assertThat(fieldValue.getValue(), equalTo("1234"));
        assertThat(fieldValue.getUrl(), equalTo(documentItemUrl));
    }

    @Test
    public void readMultipleValues() throws Exception {
        documentLink.setId("my:documentlink");

        final MockNode documentNode = MockNode.root();
        final MockNode documentLinkNode1 = documentNode.addNode("my:documentlink", "hippogallypicker:imagelink");
        documentLinkNode1.setProperty("hippo:docbase", "1");
        final MockNode documentLinkNode2 = documentNode.addNode("my:documentlink", "hippogallypicker:imagelink");
        documentLinkNode2.setProperty("hippo:docbase", "2");

        final List<FieldValue> fieldValues = documentLink.readValues(documentNode);
        assertThat(fieldValues.size(), equalTo(2));

        final FieldValue fieldValue1 = fieldValues.get(0);
        assertThat(fieldValue1.getValue(), equalTo("1"));
        assertThat(fieldValue1.getUrl(), equalTo(documentItemUrl));

        final FieldValue fieldValue2 = fieldValues.get(1);
        assertThat(fieldValue2.getValue(), equalTo("2"));
        assertThat(fieldValue2.getUrl(), equalTo(documentItemUrl));
    }

    @Test
    public void readDefaultValue() throws Exception {
        documentLink.setId("my:documentlink");
        documentItemUrl =  "";

        final MockNode documentNode = MockNode.root();
        final MockNode documentLinkNode = documentNode.addNode("my:documentlink", "hippogallypicker:imagelink");
        documentLinkNode.setProperty("hippo:docbase", "cafebabe-cafe-babe-cafe-babecafebabe");

        final List<FieldValue> fieldValues = documentLink.readValues(documentNode);
        assertThat(fieldValues.size(), equalTo(1));
        final FieldValue fieldValue = fieldValues.get(0);
        assertThat(fieldValue.getValue(), equalTo(""));
        assertThat(fieldValue.getUrl(), equalTo(documentItemUrl));
    }

    @Test
    public void readValuesThrowsRepositoryException() throws Exception {
        documentLink.setId("my:documentlink");

        final Node node = createMock(Node.class);
        expect(node.getNodes(anyString())).andThrow(new RepositoryException());
        replayAll();

        final List<FieldValue> fieldValues = documentLink.readValues(node);
        assertTrue(fieldValues.isEmpty());
    }

    @Test
    public void readValue() throws Exception {
        documentLink.setId("my:documentlink");

        final MockNode documentLinkNode = MockNode.root();
        documentLinkNode.setProperty("hippo:docbase", "1234");

        final FieldValue fieldValue = documentLink.readValue(documentLinkNode);
        assertThat(fieldValue.getValue(), equalTo("1234"));
        assertThat(fieldValue.getUrl(), equalTo(documentItemUrl));
    }

    @Test
    public void readValueThrowsException() throws Exception {
        documentLink.setId("my:documentlink");

        final Node documentLinkNode = createMock(Node.class);
        expect(documentLinkNode.getProperty(anyString())).andThrow(new RepositoryException());
        expect(documentLinkNode.getPath()).andReturn("/my:documentlink");
        replayAll();

        final FieldValue fieldValue = documentLink.readValue(documentLinkNode);
        assertNull(fieldValue.getValue());
        assertNull(fieldValue.getUrl());
    }

    @Test(expected = BadRequestException.class)
    public void writeMissingValues() throws Exception {
        documentLink.setId("my:documentlink");
        final MockNode documentNode = MockNode.root();
        documentLink.writeValues(documentNode, Optional.of(Collections.singletonList(new FieldValue("1234"))), true);
    }

    @Test
    public void writeZeroOptionalValues() throws Exception {
        documentLink.setId("my:documentlink");
        documentLink.setMinValues(0);
        documentLink.setMaxValues(1);
        final MockNode documentNode = MockNode.root();
        documentLink.writeValues(documentNode, Optional.of(Collections.emptyList()), true);
    }

    @Test
    public void writeSingleValue() throws Exception {
        documentLink.setId("my:documentlink");
        final MockNode documentNode = MockNode.root();
        final MockNode documentLinkNode = documentNode.addNode("my:documentlink", "hippogallypicker:imagelink");

        documentLink.writeValues(documentNode, Optional.of(Collections.singletonList(new FieldValue("1234"))), true);

        assertThat(documentLinkNode.getProperty("hippo:docbase").getString(), equalTo("1234"));
    }

    @Test
    public void clearSingleValue() throws Exception {
        documentLink.setId("my:documentlink");
        final MockNode documentNode = MockNode.root();
        final MockNode documentLinkNode = documentNode.addNode("my:documentlink", "hippogallypicker:imagelink");

        documentLink.writeValues(documentNode, Optional.of(Collections.singletonList(new FieldValue(""))), true);

        assertThat(documentLinkNode.getProperty("hippo:docbase").getString(), equalTo("cafebabe-cafe-babe-cafe-babecafebabe"));
    }

    @Test
    public void clearMultipleValues() throws Exception {
        documentLink.setId("my:documentlink");
        documentLink.setMinValues(0);
        documentLink.setMaxValues(Integer.MAX_VALUE);

        final MockNode documentNode = MockNode.root();
        final MockNode documentLinkNode1 = documentNode.addNode("my:documentlink", "hippogallypicker:imagelink");
        final MockNode documentLinkNode2 = documentNode.addNode("my:documentlink", "hippogallypicker:imagelink");

        documentLink.writeValues(documentNode, Optional.of(Arrays.asList(new FieldValue(""), new FieldValue(""))), true);

        assertThat(documentLinkNode1.getProperty("hippo:docbase").getString(), equalTo("cafebabe-cafe-babe-cafe-babecafebabe"));
        assertThat(documentLinkNode2.getProperty("hippo:docbase").getString(), equalTo("cafebabe-cafe-babe-cafe-babecafebabe"));
    }

    @Test
    public void writeAndClearMultipleValues() throws Exception {
        documentLink.setId("my:documentlink");
        documentLink.setMinValues(0);
        documentLink.setMaxValues(Integer.MAX_VALUE);

        final MockNode documentNode = MockNode.root();
        final MockNode documentLinkNode1 = documentNode.addNode("my:documentlink", "hippogallypicker:imagelink");
        final MockNode documentLinkNode2 = documentNode.addNode("my:documentlink", "hippogallypicker:imagelink");

        documentLink.writeValues(documentNode, Optional.of(Arrays.asList(new FieldValue("1234"), new FieldValue(""))), true);

        assertThat(documentLinkNode1.getProperty("hippo:docbase").getString(), equalTo("1234"));
        assertThat(documentLinkNode2.getProperty("hippo:docbase").getString(), equalTo("cafebabe-cafe-babe-cafe-babecafebabe"));
    }

    @Test(expected = InternalServerErrorException.class)
    public void writeValuesThrowsRepositoryException() throws Exception {
        documentLink.setId("my:documentlink");

        final Node node = createMock(Node.class);
        expect(node.getNodes(anyString())).andThrow(new RepositoryException());
        replayAll();

        documentLink.writeValues(node, Optional.of(Collections.singletonList(new FieldValue("1234"))), true);
    }

    @Test
    public void validateValueNotRequired() {
        assertTrue(documentLink.validateValue(new FieldValue()));
    }

    @Test
    public void validateValueRequiredNotEmpty() {
        documentLink.addValidator(FieldType.Validator.REQUIRED);
        assertTrue(documentLink.validateValue(new FieldValue("1234")));
    }

    @Test
    public void validateValueRequiredAndEmpty() {
        documentLink.addValidator(FieldType.Validator.REQUIRED);
        assertFalse(documentLink.validateValue(new FieldValue("")));
    }
}
