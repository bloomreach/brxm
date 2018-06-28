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

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

    private InternalLinkFieldType linkFieldType;
    private MockNode documentNode;
    private MockNode root;

    @Before
    public void setUp() throws RepositoryException {
        linkFieldType = new InternalLinkFieldType();
        documentNode = new MockNode("documentNode");

        root = MockNode.root();
        root.addNode(documentNode);
    }

    private String addLink(final String linkNodeName, final String linkedNodeDisplayName) throws RepositoryException {
        final MockNode linkedNode = new MockNode("linkedNodeName", "hippo:handle");
        linkedNode.setProperty("hippo:name", linkedNodeDisplayName);
        root.addNode(linkedNode);

        final MockNode documentLinkNode = documentNode.addNode(linkNodeName, HippoNodeType.NT_MIRROR);
        final String identifier = linkedNode.getIdentifier();
        documentLinkNode.setProperty(HippoNodeType.HIPPO_DOCBASE, identifier);
        return identifier;
    }

    private static void assertFieldValue(final FieldValue fieldValue, final String linkId, final String displayName) {
        assertThat(fieldValue.getValue(), equalTo(linkId));
        assertThat(fieldValue.getMetadata().get("displayName"), equalTo(displayName));
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
        clusterOptions.setProperty("last.visited.nodetypes", "");
        clusterOptions.setProperty("language.context.aware", true);
        clusterOptions.setProperty("nodetypes", new String[0]);

        final FieldTypeContext context = new FieldTypeContext(null, null, false, false, null, null, editorConfigNode);
        linkFieldType.init(context);

        final JsonNode linkPickerConfig = linkFieldType.getConfig().get("linkpicker");
        assertThat(linkPickerConfig.get("base.path").asText(), equalTo(""));
        assertThat(linkPickerConfig.get("base.uuid").asText(), equalTo(""));
        assertThat(linkPickerConfig.get("cluster.name").asText(), equalTo("cms-pickers/documents-only"));
        assertThat(linkPickerConfig.get("last.visited.enabled").asText(), equalTo("true"));
        assertThat(linkPickerConfig.get("last.visited.key").asText(), equalTo(""));
        assertThat(linkPickerConfig.get("last.visited.nodetypes").asText(), equalTo(""));
        assertThat(linkPickerConfig.get("language.context.aware").asBoolean(), equalTo(true));
        assertThat(linkPickerConfig.get("nodetypes").size(), equalTo(0));
    }

    @Test
    public void getDefault() {
        assertThat(linkFieldType.getDefault(), equalTo(""));
    }

    @Test
    public void getPropertyType() {
        assertThat(linkFieldType.getPropertyType(), equalTo(PropertyType.STRING));
    }

    @Test
    public void readMissingValues() {
        linkFieldType.setId("my:documentlink");

        final List<FieldValue> fieldValues = linkFieldType.readValues(documentNode);

        assertTrue(fieldValues.isEmpty());
    }

    @Test
    public void readSingleValue() throws Exception {
        linkFieldType.setId("my:documentlink");
        final String linkId = addLink("my:documentlink", "My linked node");

        final List<FieldValue> fieldValues = linkFieldType.readValues(documentNode);

        assertThat(fieldValues.size(), equalTo(1));
        assertFieldValue(fieldValues.get(0), linkId, "My linked node");
    }

    @Test
    public void readMultipleValues() throws Exception {
        linkFieldType.setId("my:documentlink");
        final String linkId1 = addLink("my:documentlink", "Link1");
        final String linkId2 = addLink("my:documentlink", "Link2");

        final List<FieldValue> fieldValues = linkFieldType.readValues(documentNode);

        assertThat(fieldValues.size(), equalTo(2));
        assertFieldValue(fieldValues.get(0), linkId1, "Link1");
        assertFieldValue(fieldValues.get(1), linkId2, "Link2");
    }

    @Test
    public void readDefaultValue() throws Exception {
        linkFieldType.setId("my:documentlink");

        final MockNode documentLinkNode = documentNode.addNode("my:documentlink", "hippo:mirror");
        documentLinkNode.setProperty("hippo:docbase", "cafebabe-cafe-babe-cafe-babecafebabe");
        final List<FieldValue> fieldValues = linkFieldType.readValues(documentNode);

        assertThat(fieldValues.size(), equalTo(1));
        assertFieldValue(fieldValues.get(0), "", "");
    }

    @Test
    public void readValuesThrowsRepositoryException() throws Exception {
        linkFieldType.setId("my:documentlink");

        final Node node = createMock(Node.class);
        expect(node.getNodes(anyString())).andThrow(new RepositoryException());
        replayAll();

        final List<FieldValue> fieldValues = linkFieldType.readValues(node);
        assertTrue(fieldValues.isEmpty());
    }

    @Test
    public void readValue() throws Exception {
        linkFieldType.setId("my:documentlink");
        final String linkId1 = addLink("my:documentlink", "Link1");

        final FieldValue fieldValue = linkFieldType.readValue(documentNode.getNode("my:documentlink"));

        assertFieldValue(fieldValue, linkId1, "Link1");
    }

    @Test
    public void readValueThrowsException() throws Exception {
        linkFieldType.setId("my:documentlink");

        final Node documentLinkNode = createMock(Node.class);
        expect(documentLinkNode.getProperty(anyString())).andThrow(new RepositoryException());
        expect(documentLinkNode.getPath()).andReturn("/my:documentlink");
        replayAll();

        final FieldValue fieldValue = linkFieldType.readValue(documentLinkNode);
        assertNull(fieldValue.getValue());
        assertNull(fieldValue.getMetadata());
    }

    @Test(expected = BadRequestException.class)
    public void writeMissingValues() throws Exception {
        linkFieldType.setId("my:documentlink");
        linkFieldType.writeValues(documentNode, Optional.of(Collections.singletonList(new FieldValue("1234"))), true);
    }

    @Test
    public void writeZeroOptionalValues() throws Exception {
        linkFieldType.setId("my:documentlink");
        linkFieldType.setMinValues(0);
        linkFieldType.setMaxValues(1);
        linkFieldType.writeValues(documentNode, Optional.of(Collections.emptyList()), true);
    }

    @Test
    public void writeSingleValue() throws Exception {
        linkFieldType.setId("my:documentlink");
        final MockNode documentLinkNode = documentNode.addNode("my:documentlink", "hippo:mirror");

        linkFieldType.writeValues(documentNode, Optional.of(Collections.singletonList(new FieldValue("1234"))), true);

        assertThat(documentLinkNode.getProperty("hippo:docbase").getString(), equalTo("1234"));
    }

    @Test
    public void clearSingleValue() throws Exception {
        linkFieldType.setId("my:documentlink");
        final MockNode documentLinkNode = documentNode.addNode("my:documentlink", "hippo:mirror");

        linkFieldType.writeValues(documentNode, Optional.of(Collections.singletonList(new FieldValue(""))), true);

        assertThat(documentLinkNode.getProperty("hippo:docbase").getString(),
                equalTo("cafebabe-cafe-babe-cafe-babecafebabe"));
    }

    @Test
    public void clearMultipleValues() throws Exception {
        linkFieldType.setId("my:documentlink");
        linkFieldType.setMinValues(0);
        linkFieldType.setMaxValues(Integer.MAX_VALUE);

        final MockNode documentLinkNode1 = documentNode.addNode("my:documentlink", "hippo:mirror");
        final MockNode documentLinkNode2 = documentNode.addNode("my:documentlink", "hippo:mirror");

        linkFieldType.writeValues(documentNode, Optional.of(Arrays.asList(new FieldValue(""), new FieldValue(""))),
                true);

        assertThat(documentLinkNode1.getProperty("hippo:docbase").getString(),
                equalTo("cafebabe-cafe-babe-cafe-babecafebabe"));
        assertThat(documentLinkNode2.getProperty("hippo:docbase").getString(),
                equalTo("cafebabe-cafe-babe-cafe-babecafebabe"));
    }

    @Test
    public void writeAndClearMultipleValues() throws Exception {
        linkFieldType.setId("my:documentlink");
        linkFieldType.setMinValues(0);
        linkFieldType.setMaxValues(Integer.MAX_VALUE);

        final MockNode documentLinkNode1 = documentNode.addNode("my:documentlink", "hippo:mirror");
        final MockNode documentLinkNode2 = documentNode.addNode("my:documentlink", "hippo:mirror");

        linkFieldType.writeValues(documentNode, Optional.of(Arrays.asList(new FieldValue("1234"), new FieldValue(""))),
                true);

        assertThat(documentLinkNode1.getProperty("hippo:docbase").getString(), equalTo("1234"));
        assertThat(documentLinkNode2.getProperty("hippo:docbase").getString(),
                equalTo("cafebabe-cafe-babe-cafe-babecafebabe"));
    }

    @Test(expected = InternalServerErrorException.class)
    public void writeValuesThrowsRepositoryException() throws Exception {
        linkFieldType.setId("my:documentlink");

        final Node node = createMock(Node.class);
        expect(node.getNodes(anyString())).andThrow(new RepositoryException());
        replayAll();

        linkFieldType.writeValues(node, Optional.of(Collections.singletonList(new FieldValue("1234"))), true);
    }

    @Test
    public void validateValueNotRequired() {
        assertTrue(linkFieldType.validateValue(new FieldValue()));
    }

    @Test
    public void validateValueRequiredNotEmpty() {
        linkFieldType.addValidator(FieldType.Validator.REQUIRED);
        assertTrue(linkFieldType.validateValue(new FieldValue("1234")));
    }

    @Test
    public void validateValueRequiredAndEmpty() {
        linkFieldType.addValidator(FieldType.Validator.REQUIRED);
        assertFalse(linkFieldType.validateValue(new FieldValue("")));
    }
}
