/*
 * Copyright 2018-2021 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.jackrabbit.commons.JcrUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.addon.frontend.gallerypicker.ImageItem;
import org.onehippo.addon.frontend.gallerypicker.ImageItemFactory;
import org.onehippo.cms.channelmanager.content.TestUserContext;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.validation.CompoundContext;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_DOCBASE;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onehippo.repository.util.JcrConstants.GALLERY_PATH;
import static org.onehippo.repository.util.JcrConstants.NT_UNSTRUCTURED;
import static org.onehippo.repository.util.JcrConstants.ROOT_NODE_ID;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.api.support.membermodification.MemberMatcher.methodsDeclaredIn;
import static org.powermock.api.support.membermodification.MemberModifier.suppress;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
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
        clusterOptions.setProperty("base.path", "");
        clusterOptions.setProperty("base.uuid", "");
        clusterOptions.setProperty("cluster.name", "cms-pickers/images");
        clusterOptions.setProperty("enable.upload", "true");
        clusterOptions.setProperty("last.visited.enabled", "true");
        clusterOptions.setProperty("last.visited.key", "gallerypicker-imagelink");
        clusterOptions.setProperty("nodetypes", new String[0]);
        clusterOptions.setProperty("last.visited.nodetypes", new String[0]);
        clusterOptions.setProperty("image.validator.id", "service.gallery.image.validation");

        final ContentTypeContext parentContext = createMock(ContentTypeContext.class);
        expect(parentContext.getLocale()).andReturn(TestUserContext.TEST_LOCALE);
        replayAll();
        final FieldTypeContext context = new FieldTypeContext(null, null, null, false, false, false, null, parentContext, editorConfigNode);
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
        imageLinkNode.setProperty(HIPPO_DOCBASE, "1234");

        final List<FieldValue> fieldValues = imageLink.readValues(documentNode);
        assertThat(fieldValues.size(), equalTo(1));
        final FieldValue fieldValue = fieldValues.get(0);
        assertThat(fieldValue.getValue(), equalTo("1234"));
        assertThat(fieldValue.getMetadata().get("url"), equalTo(imageItemUrl));
    }

    @Test
    public void readMultipleValues() throws Exception {
        imageLink.setId("my:imagelink");

        final MockNode documentNode = MockNode.root();
        final MockNode imageLinkNode1 = documentNode.addNode("my:imagelink", "hippogallypicker:imagelink");
        imageLinkNode1.setProperty(HIPPO_DOCBASE, "1");
        final MockNode imageLinkNode2 = documentNode.addNode("my:imagelink", "hippogallypicker:imagelink");
        imageLinkNode2.setProperty(HIPPO_DOCBASE, "2");

        final List<FieldValue> fieldValues = imageLink.readValues(documentNode);
        assertThat(fieldValues.size(), equalTo(2));

        final FieldValue fieldValue1 = fieldValues.get(0);
        assertThat(fieldValue1.getValue(), equalTo("1"));
        assertThat(fieldValue1.getMetadata().get("url"), equalTo(imageItemUrl));

        final FieldValue fieldValue2 = fieldValues.get(1);
        assertThat(fieldValue2.getValue(), equalTo("2"));
        assertThat(fieldValue2.getMetadata().get("url"), equalTo(imageItemUrl));
    }

    @Test
    public void readRootNodeReturnsEmptyValue() throws Exception {
        imageLink.setId("my:imagelink");
        imageItemUrl =  "";

        final MockNode imageLinkNode = MockNode.root();
        imageLinkNode.setProperty(HIPPO_DOCBASE, ROOT_NODE_ID);

        final FieldValue fieldValue = imageLink.readValue(imageLinkNode);
        assertThat(fieldValue.getValue(), equalTo(""));
        assertThat(fieldValue.getMetadata().get("url"), equalTo(imageItemUrl));
    }

    @Test
    public void readGalleryRootNodeReturnsEmptyValue() throws Exception {
        imageLink.setId("my:imagelink");
        imageItemUrl =  "";

        final MockNode root = MockNode.root();
        final Node galleryRoot = JcrUtils.getOrCreateByPath(GALLERY_PATH, NT_UNSTRUCTURED, root.getSession());
        final Node imageLinkNode = root.addNode("my:imagelink", "hippogallypicker:imagelink");
        imageLinkNode.setProperty(HIPPO_DOCBASE, galleryRoot.getIdentifier());

        final FieldValue fieldValue = imageLink.readValue(imageLinkNode);
        assertThat(fieldValue.getValue(), equalTo(""));
        assertThat(fieldValue.getMetadata().get("url"), equalTo(imageItemUrl));
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
        imageLinkNode.setProperty(HIPPO_DOCBASE, "1234");

        final FieldValue fieldValue = imageLink.readValue(imageLinkNode);
        assertThat(fieldValue.getValue(), equalTo("1234"));
        assertThat(fieldValue.getMetadata().get("url"), equalTo(imageItemUrl));
    }

    @Test
    public void readValueThrowsException() throws Exception {
        imageLink.setId("my:imagelink");

        final Node imageLinkNode = createMock(Node.class);
        expect(imageLinkNode.getSession()).andThrow(new RepositoryException());
        expect(imageLinkNode.getPath()).andReturn("/my:imagelink");
        replayAll();

        final FieldValue fieldValue = imageLink.readValue(imageLinkNode);
        assertNull(fieldValue.getValue());
        assertNull(fieldValue.getMetadata());

        verifyAll();
    }

    @Test(expected = BadRequestException.class)
    public void writeMissingValues() throws Exception {
        imageLink.setId("my:imagelink");
        final MockNode documentNode = MockNode.root();
        final List<FieldValue> fieldValues = Collections.singletonList(new FieldValue("1234"));
        CompoundContext compoundContext = new CompoundContext(documentNode, documentNode, null, null);
        imageLink.validate(fieldValues, compoundContext);
        imageLink.writeValues(documentNode, Optional.of(fieldValues));
    }

    @Test
    public void writeZeroOptionalValues() throws Exception {
        imageLink.setId("my:imagelink");
        imageLink.setMinValues(0);
        imageLink.setMaxValues(1);
        final MockNode documentNode = MockNode.root();
        imageLink.writeValues(documentNode, Optional.of(Collections.emptyList()));
    }

    @Test
    public void writeSingleValue() throws Exception {
        imageLink.setId("my:imagelink");
        final MockNode documentNode = MockNode.root();
        final MockNode imageLinkNode = documentNode.addNode("my:imagelink", "hippogallypicker:imagelink");

        imageLink.writeValues(documentNode, Optional.of(Collections.singletonList(new FieldValue("1234"))));

        assertThat(imageLinkNode.getProperty(HIPPO_DOCBASE).getString(), equalTo("1234"));
    }

    @Test
    public void clearSingleValue() throws Exception {
        imageLink.setId("my:imagelink");
        final MockNode documentNode = MockNode.root();
        final MockNode imageLinkNode = documentNode.addNode("my:imagelink", "hippogallypicker:imagelink");

        imageLink.writeValues(documentNode, Optional.of(Collections.singletonList(new FieldValue(""))));

        assertThat(imageLinkNode.getProperty(HIPPO_DOCBASE).getString(), equalTo("cafebabe-cafe-babe-cafe-babecafebabe"));
    }

    @Test
    public void clearMultipleValues() throws Exception {
        imageLink.setId("my:imagelink");
        imageLink.setMinValues(0);
        imageLink.setMaxValues(Integer.MAX_VALUE);

        final MockNode documentNode = MockNode.root();
        final MockNode imageLinkNode1 = documentNode.addNode("my:imagelink", "hippogallypicker:imagelink");
        final MockNode imageLinkNode2 = documentNode.addNode("my:imagelink", "hippogallypicker:imagelink");

        imageLink.writeValues(documentNode, Optional.of(Arrays.asList(new FieldValue(""), new FieldValue(""))));

        assertThat(imageLinkNode1.getProperty(HIPPO_DOCBASE).getString(), equalTo("cafebabe-cafe-babe-cafe-babecafebabe"));
        assertThat(imageLinkNode2.getProperty(HIPPO_DOCBASE).getString(), equalTo("cafebabe-cafe-babe-cafe-babecafebabe"));
    }

    @Test
    public void writeAndClearMultipleValues() throws Exception {
        imageLink.setId("my:imagelink");
        imageLink.setMinValues(0);
        imageLink.setMaxValues(Integer.MAX_VALUE);

        final MockNode documentNode = MockNode.root();
        final MockNode imageLinkNode1 = documentNode.addNode("my:imagelink", "hippogallypicker:imagelink");
        final MockNode imageLinkNode2 = documentNode.addNode("my:imagelink", "hippogallypicker:imagelink");

        imageLink.writeValues(documentNode, Optional.of(Arrays.asList(new FieldValue("1234"), new FieldValue(""))));

        assertThat(imageLinkNode1.getProperty(HIPPO_DOCBASE).getString(), equalTo("1234"));
        assertThat(imageLinkNode2.getProperty(HIPPO_DOCBASE).getString(), equalTo("cafebabe-cafe-babe-cafe-babecafebabe"));
    }

    @Test(expected = InternalServerErrorException.class)
    public void writeValuesThrowsRepositoryException() throws Exception {
        imageLink.setId("my:imagelink");

        final Node node = createMock(Node.class);
        expect(node.getNodes(anyString())).andThrow(new RepositoryException());
        replayAll();

        imageLink.writeValues(node, Optional.of(Collections.singletonList(new FieldValue("1234"))));
    }
}
