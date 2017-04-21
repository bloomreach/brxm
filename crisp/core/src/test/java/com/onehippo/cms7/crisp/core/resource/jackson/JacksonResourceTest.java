/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.core.resource.jackson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onehippo.cms7.crisp.api.resource.Resource;

public class JacksonResourceTest {

    private JsonNode rootNode;
    private Resource rootResource;

    @Before
    public void setUp() throws Exception {
        InputStream input = null;

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            input = JacksonResourceTest.class.getResourceAsStream("widget.json");
            rootNode = objectMapper.readTree(input);
            rootResource = new JacksonResource(rootNode);
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    @Test
    public void testTraversal() throws Exception {
        assertEquals("OBJECT", rootResource.gerResourceType());
        assertTrue(rootResource.isResourceType("OBJECT"));
        assertNull(rootResource.getName());
        assertEquals("/", rootResource.getPath());
        assertTrue(rootResource.getMetadata().isEmpty());
        assertNull(rootResource.getParent());

        Resource widget = rootResource.getValueMap().get("widget", Resource.class);
        assertNotNull(widget);
        assertEquals("OBJECT", widget.gerResourceType());
        assertTrue(widget.isResourceType("OBJECT"));
        assertEquals("widget", widget.getName());
        assertEquals("/widget", widget.getPath());
        assertTrue(widget.isAnyChildContained());
        assertEquals("on", widget.getValueMap().get("debug"));
        assertTrue(widget.getMetadata().isEmpty());
        assertSame(rootResource, widget.getParent());

        Resource window = widget.getValueMap().get("window", Resource.class);
        assertNotNull(window);
        assertEquals("OBJECT", window.gerResourceType());
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
        assertEquals("ARRAY", images.gerResourceType());
        assertTrue(images.isResourceType("ARRAY"));
        assertEquals("images", images.getName());
        assertEquals("/widget/images", images.getPath());
        assertTrue(images.isAnyChildContained());
        assertTrue(images.getValueMap().isEmpty());
        assertTrue(images.getMetadata().isEmpty());
        assertSame(widget, images.getParent());

        List<Resource> imageResources = new ArrayList<>();
        for (Iterator<Resource> it = images.getChildIterator(); it.hasNext();) {
            imageResources.add(it.next());
        }
        assertEquals(2, imageResources.size());

        Resource image = imageResources.get(0);
        assertNotNull(image);
        assertEquals("OBJECT", image.gerResourceType());
        assertTrue(image.isResourceType("OBJECT"));
        assertEquals("[1]", image.getName());
        assertEquals("/widget/images/[1]", image.getPath());
        assertEquals("Images/Sun.png", image.getValueMap().get("src"));
        assertEquals("sun1", image.getValueMap().get("name"));
        assertEquals(250, image.getValueMap().get("hOffset"));
        assertEquals(250, image.getValueMap().get("vOffset"));
        assertEquals("center", image.getValueMap().get("alignment"));
        assertFalse(image.isAnyChildContained());
        assertTrue(image.getMetadata().isEmpty());
        assertSame(images, image.getParent());

        image = imageResources.get(1);
        assertNotNull(image);
        assertEquals("OBJECT", image.gerResourceType());
        assertTrue(image.isResourceType("OBJECT"));
        assertEquals("[2]", image.getName());
        assertEquals("/widget/images/[2]", image.getPath());
        assertEquals("Images/Moon.png", image.getValueMap().get("src"));
        assertEquals("moon1", image.getValueMap().get("name"));
        assertEquals(100, image.getValueMap().get("hOffset"));
        assertEquals(100, image.getValueMap().get("vOffset"));
        assertEquals("left", image.getValueMap().get("alignment"));
        assertFalse(image.isAnyChildContained());
        assertTrue(image.getMetadata().isEmpty());
        assertSame(images, image.getParent());

        imageResources.clear();
        for (Resource imageRes : images.getChildren()) {
            imageResources.add(imageRes);
        }
        assertEquals(2, imageResources.size());

        image = imageResources.get(0);
        assertNotNull(image);
        assertEquals("OBJECT", image.gerResourceType());
        assertTrue(image.isResourceType("OBJECT"));
        assertEquals("[1]", image.getName());
        assertEquals("/widget/images/[1]", image.getPath());
        assertEquals("Images/Sun.png", image.getValueMap().get("src"));
        assertEquals("sun1", image.getValueMap().get("name"));
        assertEquals(250, image.getValueMap().get("hOffset"));
        assertEquals(250, image.getValueMap().get("vOffset"));
        assertEquals("center", image.getValueMap().get("alignment"));
        assertFalse(image.isAnyChildContained());
        assertTrue(image.getMetadata().isEmpty());
        assertSame(images, image.getParent());

        image = imageResources.get(1);
        assertNotNull(image);
        assertEquals("OBJECT", image.gerResourceType());
        assertTrue(image.isResourceType("OBJECT"));
        assertEquals("[2]", image.getName());
        assertEquals("/widget/images/[2]", image.getPath());
        assertEquals("Images/Moon.png", image.getValueMap().get("src"));
        assertEquals("moon1", image.getValueMap().get("name"));
        assertEquals(100, image.getValueMap().get("hOffset"));
        assertEquals(100, image.getValueMap().get("vOffset"));
        assertEquals("left", image.getValueMap().get("alignment"));
        assertFalse(image.isAnyChildContained());
        assertTrue(image.getMetadata().isEmpty());
        assertSame(images, image.getParent());
    }

    @Test
    public void testPagination() throws Exception {
        Resource widget = rootResource.getValueMap().get("widget", Resource.class);
        Resource images = widget.getValueMap().get("images", Resource.class);
        assertEquals(2, images.getChildCount());

        List<Resource> children = (List<Resource>) images.getChildren();
        assertEquals(2, children.size());
        assertEquals("Images/Sun.png", children.get(0).getValueMap().get("src"));
        assertEquals("Images/Moon.png", children.get(1).getValueMap().get("src"));

        try {
            children = (List<Resource>) images.getChildren(-1, 0);
            fail("Negative offset should not be allowed.");
        } catch (IllegalArgumentException expectedException) {
        }

        children = (List<Resource>) images.getChildren(0, 0);
        assertEquals(0, children.size());

        children = (List<Resource>) images.getChildren(0, 1);
        assertEquals(1, children.size());
        assertEquals("Images/Sun.png", children.get(0).getValueMap().get("src"));

        children = (List<Resource>) images.getChildren(0, 2);
        assertEquals(2, children.size());
        assertEquals("Images/Sun.png", children.get(0).getValueMap().get("src"));
        assertEquals("Images/Moon.png", children.get(1).getValueMap().get("src"));

        children = (List<Resource>) images.getChildren(0, -1);
        assertEquals(2, children.size());
        assertEquals("Images/Sun.png", children.get(0).getValueMap().get("src"));
        assertEquals("Images/Moon.png", children.get(1).getValueMap().get("src"));

        children = (List<Resource>) images.getChildren(1, 1);
        assertEquals(1, children.size());
        assertEquals("Images/Moon.png", children.get(0).getValueMap().get("src"));

        children = (List<Resource>) images.getChildren(1, 2);
        assertEquals(1, children.size());
        assertEquals("Images/Moon.png", children.get(0).getValueMap().get("src"));

        children = (List<Resource>) images.getChildren(1, Long.MAX_VALUE);
        assertEquals(1, children.size());
        assertEquals("Images/Moon.png", children.get(0).getValueMap().get("src"));

        try {
            children = (List<Resource>) images.getChildren(2, Long.MAX_VALUE);
            fail("Out of index offset.");
        } catch (IllegalArgumentException expectedException) {
        }
    }
}
