/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms7.crisp.core.resource.jackson;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceCollection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JacksonResourceTest {

    private static final int[] EXIF_VERSION = { 48, 50, 50, 49 };
    private static final String[] EXIF_GEO_LOCATION = { "N42", "W71" };


    private JsonNode rootNode;
    private Resource rootResource;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        InputStream input = null;

        try {
            objectMapper = new ObjectMapper();
            input = JacksonResourceTest.class.getResourceAsStream("widget.json");
            rootNode = objectMapper.readTree(input);
            rootResource = new JacksonResource(rootNode);
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    @Test
    public void testTraversal() throws Exception {
        assertEquals("OBJECT", rootResource.getResourceType());
        assertTrue(rootResource.isResourceType("OBJECT"));
        assertNull(rootResource.getName());
        assertEquals("/", rootResource.getPath());
        assertTrue(rootResource.getMetadata().isEmpty());
        assertNull(rootResource.getParent());

        Resource widget = rootResource.getValueMap().get("widget", Resource.class);
        assertNotNull(widget);
        assertEquals("OBJECT", widget.getResourceType());
        assertTrue(widget.isResourceType("OBJECT"));
        assertEquals("widget", widget.getName());
        assertEquals("/widget", widget.getPath());
        assertTrue(widget.isAnyChildContained());
        assertEquals("on", widget.getValueMap().get("debug"));
        assertTrue(widget.getMetadata().isEmpty());
        assertSame(rootResource, widget.getParent());

        Resource window = widget.getValueMap().get("window", Resource.class);
        assertNotNull(window);
        assertEquals("OBJECT", window.getResourceType());
        assertTrue(window.isResourceType("OBJECT"));
        assertEquals("window", window.getName());
        assertEquals("/widget/window", window.getPath());
        assertEquals("Sample Konfabulator Widget", window.getValueMap().get("title"));
        assertEquals("main_window", window.getValueMap().get("name"));
        assertEquals(500, window.getValueMap().get("width"));
        assertEquals(500, window.getValueMap().get("height"));
        assertFalse(window.isAnyChildContained());
        assertTrue(window.getMetadata().isEmpty());
        assertSame(widget, window.getParent());

        Resource images = widget.getValueMap().get("images", Resource.class);
        assertNotNull(images);
        assertEquals("ARRAY", images.getResourceType());
        assertTrue(images.isResourceType("ARRAY"));
        assertEquals("images", images.getName());
        assertEquals("/widget/images", images.getPath());
        assertTrue(images.isAnyChildContained());
        assertTrue(images.getValueMap().isEmpty());
        assertTrue(images.getMetadata().isEmpty());
        assertSame(widget, images.getParent());

        ResourceCollection imageResources = images.getChildren();
        assertEquals(2, imageResources.size());

        Resource image = imageResources.get(0);
        assertNotNull(image);
        assertEquals("OBJECT", image.getResourceType());
        assertTrue(image.isResourceType("OBJECT"));
        assertEquals("[1]", image.getName());
        assertEquals("/widget/images/[1]", image.getPath());
        assertEquals("Images/Sun.png", image.getValueMap().get("src"));
        assertEquals("sun1", image.getValueMap().get("name"));
        assertEquals(250, image.getValueMap().get("hOffset"));
        assertEquals(250, image.getValueMap().get("vOffset"));
        assertEquals("center", image.getValueMap().get("alignment"));
        assertTrue(image.getMetadata().isEmpty());
        assertSame(images, image.getParent());

        image = imageResources.get(1);
        assertNotNull(image);
        assertEquals("OBJECT", image.getResourceType());
        assertTrue(image.isResourceType("OBJECT"));
        assertEquals("[2]", image.getName());
        assertEquals("/widget/images/[2]", image.getPath());
        assertEquals("Images/Moon.png", image.getValueMap().get("src"));
        assertEquals("moon1", image.getValueMap().get("name"));
        assertEquals(100, image.getValueMap().get("hOffset"));
        assertEquals(100, image.getValueMap().get("vOffset"));
        assertEquals("left", image.getValueMap().get("alignment"));
        assertTrue(image.getMetadata().isEmpty());
        assertSame(images, image.getParent());

        imageResources = images.getChildren();
        assertEquals(2, imageResources.size());

        image = imageResources.get(0);
        assertNotNull(image);
        assertEquals("OBJECT", image.getResourceType());
        assertTrue(image.isResourceType("OBJECT"));
        assertEquals("[1]", image.getName());
        assertEquals("/widget/images/[1]", image.getPath());
        assertEquals("Images/Sun.png", image.getValueMap().get("src"));
        assertEquals("sun1", image.getValueMap().get("name"));
        assertEquals(250, image.getValueMap().get("hOffset"));
        assertEquals(250, image.getValueMap().get("vOffset"));
        assertEquals("center", image.getValueMap().get("alignment"));
        assertTrue(image.getMetadata().isEmpty());
        assertSame(images, image.getParent());

        image = imageResources.get(1);
        assertNotNull(image);
        assertEquals("OBJECT", image.getResourceType());
        assertTrue(image.isResourceType("OBJECT"));
        assertEquals("[2]", image.getName());
        assertEquals("/widget/images/[2]", image.getPath());
        assertEquals("Images/Moon.png", image.getValueMap().get("src"));
        assertEquals("moon1", image.getValueMap().get("name"));
        assertEquals(100, image.getValueMap().get("hOffset"));
        assertEquals(100, image.getValueMap().get("vOffset"));
        assertEquals("left", image.getValueMap().get("alignment"));
        assertTrue(image.getMetadata().isEmpty());
        assertSame(images, image.getParent());
    }

    @Test
    public void testValueByRelPaths() throws Exception {
        assertEquals("on", rootResource.getValue("widget/debug"));

        assertEquals("Sample Konfabulator Widget", rootResource.getValue("widget/window/title"));
        assertEquals("main_window", rootResource.getValue("widget/window/name"));
        assertEquals(500, rootResource.getValue("widget/window/width"));
        assertEquals(500, rootResource.getValue("widget/window/height"));

        assertEquals("Click Here", rootResource.getValue("widget/text/data"));
        assertEquals(36, rootResource.getValue("widget/text/size"));
        assertEquals("bold", rootResource.getValue("widget/text/style"));
        assertEquals("text1", rootResource.getValue("widget/text/name"));
        assertEquals(250, rootResource.getValue("widget/text/hOffset"));
        assertEquals("center", rootResource.getValue("widget/text/alignment"));

        assertEquals("Images/Sun.png", rootResource.getValue("widget/images[1]/src"));
        assertEquals("sun1", rootResource.getValue("widget/images[1]/name"));
        assertEquals(250, rootResource.getValue("widget/images[1]/hOffset"));
        assertEquals(250, rootResource.getValue("widget/images[1]/vOffset"));
        assertEquals("center", rootResource.getValue("widget/images[1]/alignment"));

        assertEquals("Images/Moon.png", rootResource.getValue("widget/images[2]/src"));
        assertEquals("moon1", rootResource.getValue("widget/images[2]/name"));
        assertEquals(100, rootResource.getValue("widget/images[2]/hOffset"));
        assertEquals(100, rootResource.getValue("widget/images[2]/vOffset"));
        assertEquals("left", rootResource.getValue("widget/images[2]/alignment"));
    }

    @Test
    public void testPagination() throws Exception {
        Resource widget = rootResource.getValueMap().get("widget", Resource.class);
        Resource images = widget.getValueMap().get("images", Resource.class);
        assertEquals(2, images.getChildCount());

        ResourceCollection children = images.getChildren();
        assertEquals(2, children.size());
        assertEquals("Images/Sun.png", children.get(0).getValueMap().get("src"));
        assertEquals("Images/Moon.png", children.get(1).getValueMap().get("src"));

        try {
            children = images.getChildren(-1, 0);
            fail("Negative offset should not be allowed.");
        } catch (IllegalArgumentException expectedException) {
        }

        children = images.getChildren(0, 0);
        assertEquals(0, children.size());

        children = images.getChildren(0, 1);
        assertEquals(1, children.size());
        assertEquals("Images/Sun.png", children.get(0).getValueMap().get("src"));

        children = images.getChildren(0, 2);
        assertEquals(2, children.size());
        assertEquals("Images/Sun.png", children.get(0).getValueMap().get("src"));
        assertEquals("Images/Moon.png", children.get(1).getValueMap().get("src"));

        children = images.getChildren(0, -1);
        assertEquals(2, children.size());
        assertEquals("Images/Sun.png", children.get(0).getValueMap().get("src"));
        assertEquals("Images/Moon.png", children.get(1).getValueMap().get("src"));

        children = images.getChildren(1, 1);
        assertEquals(1, children.size());
        assertEquals("Images/Moon.png", children.get(0).getValueMap().get("src"));

        children = images.getChildren(1, 2);
        assertEquals(1, children.size());
        assertEquals("Images/Moon.png", children.get(0).getValueMap().get("src"));

        children = images.getChildren(1, Long.MAX_VALUE);
        assertEquals(1, children.size());
        assertEquals("Images/Moon.png", children.get(0).getValueMap().get("src"));

        try {
            children = images.getChildren(2, Long.MAX_VALUE);
            fail("Out of index offset.");
        } catch (IllegalArgumentException expectedException) {
        }
    }

    @Test
    public void testArrays() throws Exception {
        Resource widget = rootResource.getValueMap().get("widget", Resource.class);
        Resource images = widget.getValueMap().get("images", Resource.class);
        ResourceCollection imageResources = images.getChildren();

        Resource image = imageResources.get(0);

        Resource exifVersion = image.getValueMap().get("exifVersion", Resource.class);
        ResourceCollection children = exifVersion.getChildren();
        assertEquals(4, children.size());

        for (int i = 0; i < children.size(); i++) {
            Resource child = children.get(i);
            assertEquals(EXIF_VERSION[i], child.getDefaultValue());
        }

        Resource exifGeoLocation = image.getValueMap().get("exifGeoLocation", Resource.class);
        children = exifGeoLocation.getChildren();
        assertEquals(2, children.size());

        for (int i = 0; i < children.size(); i++) {
            Resource child = children.get(i);
            assertEquals(EXIF_GEO_LOCATION[i], child.getDefaultValue());
        }
    }
}
