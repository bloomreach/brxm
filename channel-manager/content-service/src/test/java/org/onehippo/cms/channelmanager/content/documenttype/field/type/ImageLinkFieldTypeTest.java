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
import org.onehippo.addon.frontend.gallerypicker.ImageItemFactory;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
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
public class ImageLinkFieldTypeTest {

    private String imageItemUrl;
    private ImageLinkFieldType imageLink;

    @Before
    public void setUp() {
        final ImageItemFactory mockImageItemFactory = createMock(ImageItemFactory.class);
        final ImageItem mockImageItem = createMock(ImageItem.class);
        expect(mockImageItemFactory.createImageItem(anyString())).andReturn(mockImageItem).anyTimes();
        expect(mockImageItem.getPrimaryUrl(anyObject())).andAnswer(() -> imageItemUrl).anyTimes();
        replayAll();

        imageLink = new ImageLinkFieldType(mockImageItemFactory);
        imageItemUrl =  "http://example.com";
    }

    @Test
    public void constructor() {
        assertThat(new ImageLinkFieldType().getType(), equalTo(FieldType.Type.IMAGE_LINK));
    }

    @Test
    public void init() throws Exception {
        // prevent re-testing the init method of the super class
        suppress(methodsDeclaredIn(AbstractFieldType.class));

        final MockNode editorConfigNode = MockNode.root();

        // put the default bootstrapped configuration in a fake 'cluster.options' node to simplify testing
        final MockNode clusterOptions = editorConfigNode.addNode("cluster.options", "frontend:plugincluster");
        clusterOptions.setProperty("base.uuid", "");
        clusterOptions.setProperty("cluster.name", "cms-pickers/images");
        clusterOptions.setProperty("enable.upload", "true");
        clusterOptions.setProperty("last.visited.enabled", "true");
        clusterOptions.setProperty("last.visited.key", "gallerypicker-imagelink");
        clusterOptions.setProperty("nodetypes", new String[0]);
        clusterOptions.setProperty("image.validator.id", "service.gallery.image.validation");

        final FieldTypeContext context = new FieldTypeContext(null, null, editorConfigNode);
        imageLink.init(context);

        final JsonNode imagePickerConfig = imageLink.getConfig().get("imagepicker");
        assertThat(imagePickerConfig.get("base.uuid").asText(), equalTo(""));
        assertThat(imagePickerConfig.get("cluster.name").asText(), equalTo("cms-pickers/images"));
        assertThat(imagePickerConfig.get("enable.upload").asText(), equalTo("true"));
        assertThat(imagePickerConfig.get("last.visited.enabled").asText(), equalTo("true"));
        assertThat(imagePickerConfig.get("last.visited.key").asText(), equalTo("gallerypicker-imagelink"));
        assertThat(imagePickerConfig.get("nodetypes").size(), equalTo(0));
        assertThat(imagePickerConfig.get("validator.id").asText(), equalTo("service.gallery.image.validation"));
    }

    @Test
    public void initListBasedChoice() {
        imageLink.initListBasedChoice("my:choice");
        assertThat(imageLink.getId(), equalTo("my:choice"));
    }

    @Test
    public void getDefault() {
        assertThat(imageLink.getDefault(), equalTo(""));
    }

    @Test
    public void getPropertyType() {
        assertThat(imageLink.getPropertyType(), equalTo(PropertyType.STRING));
    }

    @Test
    public void readMissingValues() {
        imageLink.setId("my:imagelink");
        final MockNode documentNode = MockNode.root();
        final List<FieldValue> fieldValues = imageLink.readValues(documentNode);
        assertTrue(fieldValues.isEmpty());
    }

    @Test
    public void readSingleValue() throws Exception {
        imageLink.setId("my:imagelink");

        final MockNode documentNode = MockNode.root();
        final MockNode imageLinkNode = documentNode.addNode("my:imagelink", "hippogallypicker:imagelink");
        imageLinkNode.setProperty("hippo:docbase", "1234");

        final List<FieldValue> fieldValues = imageLink.readValues(documentNode);
        assertThat(fieldValues.size(), equalTo(1));
        final FieldValue fieldValue = fieldValues.get(0);
        assertThat(fieldValue.getValue(), equalTo("1234"));
        assertThat(fieldValue.getUrl(), equalTo(imageItemUrl));
    }

    @Test
    public void readMultipleValues() throws Exception {
        imageLink.setId("my:imagelink");

        final MockNode documentNode = MockNode.root();
        final MockNode imageLinkNode1 = documentNode.addNode("my:imagelink", "hippogallypicker:imagelink");
        imageLinkNode1.setProperty("hippo:docbase", "1");
        final MockNode imageLinkNode2 = documentNode.addNode("my:imagelink", "hippogallypicker:imagelink");
        imageLinkNode2.setProperty("hippo:docbase", "2");

        final List<FieldValue> fieldValues = imageLink.readValues(documentNode);
        assertThat(fieldValues.size(), equalTo(2));

        final FieldValue fieldValue1 = fieldValues.get(0);
        assertThat(fieldValue1.getValue(), equalTo("1"));
        assertThat(fieldValue1.getUrl(), equalTo(imageItemUrl));

        final FieldValue fieldValue2 = fieldValues.get(1);
        assertThat(fieldValue2.getValue(), equalTo("2"));
        assertThat(fieldValue2.getUrl(), equalTo(imageItemUrl));
    }

    @Test
    public void readDefaultValue() throws Exception {
        imageLink.setId("my:imagelink");
        imageItemUrl =  "";

        final MockNode documentNode = MockNode.root();
        final MockNode imageLinkNode = documentNode.addNode("my:imagelink", "hippogallypicker:imagelink");
        imageLinkNode.setProperty("hippo:docbase", "cafebabe-cafe-babe-cafe-babecafebabe");

        final List<FieldValue> fieldValues = imageLink.readValues(documentNode);
        assertThat(fieldValues.size(), equalTo(1));
        final FieldValue fieldValue = fieldValues.get(0);
        assertThat(fieldValue.getValue(), equalTo(""));
        assertThat(fieldValue.getUrl(), equalTo(imageItemUrl));
    }

    @Test
    public void readValuesThrowsRepositoryException() throws Exception {
        imageLink.setId("my:imagelink");

        final Node node = createMock(Node.class);
        expect(node.getNodes(anyString())).andThrow(new RepositoryException());
        replayAll();

        final List<FieldValue> fieldValues = imageLink.readValues(node);
        assertTrue(fieldValues.isEmpty());
    }

    @Test
    public void readValue() throws Exception {
        imageLink.setId("my:imagelink");

        final MockNode imageLinkNode = MockNode.root();
        imageLinkNode.setProperty("hippo:docbase", "1234");

        final FieldValue fieldValue = imageLink.readValue(imageLinkNode);
        assertThat(fieldValue.getValue(), equalTo("1234"));
        assertThat(fieldValue.getUrl(), equalTo(imageItemUrl));
    }

    @Test
    public void readValueThrowsException() throws Exception {
        imageLink.setId("my:imagelink");

        final Node imageLinkNode = createMock(Node.class);
        expect(imageLinkNode.getProperty(anyString())).andThrow(new RepositoryException());
        expect(imageLinkNode.getPath()).andReturn("/my:imagelink");
        replayAll();

        final FieldValue fieldValue = imageLink.readValue(imageLinkNode);
        assertNull(fieldValue.getValue());
        assertNull(fieldValue.getUrl());
    }

    @Test(expected = BadRequestException.class)
    public void writeMissingValues() throws Exception {
        imageLink.setId("my:imagelink");
        final MockNode documentNode = MockNode.root();
        imageLink.writeValues(documentNode, Optional.of(Collections.singletonList(new FieldValue("1234"))), true);
    }

    @Test
    public void writeZeroOptionalValues() throws Exception {
        imageLink.setId("my:imagelink");
        imageLink.setMinValues(0);
        imageLink.setMaxValues(1);
        final MockNode documentNode = MockNode.root();
        imageLink.writeValues(documentNode, Optional.of(Collections.emptyList()), true);
    }

    @Test
    public void writeSingleValue() throws Exception {
        imageLink.setId("my:imagelink");
        final MockNode documentNode = MockNode.root();
        final MockNode imageLinkNode = documentNode.addNode("my:imagelink", "hippogallypicker:imagelink");

        imageLink.writeValues(documentNode, Optional.of(Collections.singletonList(new FieldValue("1234"))), true);

        assertThat(imageLinkNode.getProperty("hippo:docbase").getString(), equalTo("1234"));
    }

    @Test
    public void clearSingleValue() throws Exception {
        imageLink.setId("my:imagelink");
        final MockNode documentNode = MockNode.root();
        final MockNode imageLinkNode = documentNode.addNode("my:imagelink", "hippogallypicker:imagelink");

        imageLink.writeValues(documentNode, Optional.of(Collections.singletonList(new FieldValue(""))), true);

        assertThat(imageLinkNode.getProperty("hippo:docbase").getString(), equalTo("cafebabe-cafe-babe-cafe-babecafebabe"));
    }

    @Test
    public void clearMultipleValues() throws Exception {
        imageLink.setId("my:imagelink");
        imageLink.setMinValues(0);
        imageLink.setMaxValues(Integer.MAX_VALUE);

        final MockNode documentNode = MockNode.root();
        final MockNode imageLinkNode1 = documentNode.addNode("my:imagelink", "hippogallypicker:imagelink");
        final MockNode imageLinkNode2 = documentNode.addNode("my:imagelink", "hippogallypicker:imagelink");

        imageLink.writeValues(documentNode, Optional.of(Arrays.asList(new FieldValue(""), new FieldValue(""))), true);

        assertThat(imageLinkNode1.getProperty("hippo:docbase").getString(), equalTo("cafebabe-cafe-babe-cafe-babecafebabe"));
        assertThat(imageLinkNode2.getProperty("hippo:docbase").getString(), equalTo("cafebabe-cafe-babe-cafe-babecafebabe"));
    }

    @Test
    public void writeAndClearMultipleValues() throws Exception {
        imageLink.setId("my:imagelink");
        imageLink.setMinValues(0);
        imageLink.setMaxValues(Integer.MAX_VALUE);

        final MockNode documentNode = MockNode.root();
        final MockNode imageLinkNode1 = documentNode.addNode("my:imagelink", "hippogallypicker:imagelink");
        final MockNode imageLinkNode2 = documentNode.addNode("my:imagelink", "hippogallypicker:imagelink");

        imageLink.writeValues(documentNode, Optional.of(Arrays.asList(new FieldValue("1234"), new FieldValue(""))), true);

        assertThat(imageLinkNode1.getProperty("hippo:docbase").getString(), equalTo("1234"));
        assertThat(imageLinkNode2.getProperty("hippo:docbase").getString(), equalTo("cafebabe-cafe-babe-cafe-babecafebabe"));
    }

    @Test(expected = InternalServerErrorException.class)
    public void writeValuesThrowsRepositoryException() throws Exception {
        imageLink.setId("my:imagelink");

        final Node node = createMock(Node.class);
        expect(node.getNodes(anyString())).andThrow(new RepositoryException());
        replayAll();

        imageLink.writeValues(node, Optional.of(Collections.singletonList(new FieldValue("1234"))), true);
    }

    @Test
    public void validateValueNotRequired() {
        assertTrue(imageLink.validateValue(new FieldValue()));
    }

    @Test
    public void validateValueRequiredNotEmpty() {
        imageLink.addValidator(FieldType.Validator.REQUIRED);
        assertTrue(imageLink.validateValue(new FieldValue("1234")));
    }

    @Test
    public void validateValueRequiredAndEmpty() {
        imageLink.addValidator(FieldType.Validator.REQUIRED);
        assertFalse(imageLink.validateValue(new FieldValue("")));
    }
}