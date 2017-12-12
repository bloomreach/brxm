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
package org.onehippo.cms7.crisp.core.resource.jdom;

import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceBeanMapper;
import org.onehippo.cms7.crisp.api.resource.ResourceCollection;
import org.onehippo.cms7.crisp.core.resource.jdom.model.Image;
import org.onehippo.cms7.crisp.core.resource.jdom.model.Widget;
import org.onehippo.cms7.crisp.core.resource.jdom.model.Window;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JdomResourceTest {

    private Element rootElem;
    private Resource rootResource;
    private ResourceBeanMapper resourceBeanMapper;

    @Before
    public void setUp() throws Exception {
        InputStream input = null;

        try {
            input = JdomResourceTest.class.getResourceAsStream("widget.xml");
            SAXBuilder jdomBuilder = new SAXBuilder();
            Document jdomDocument = jdomBuilder.build(input);
            rootElem = jdomDocument.getRootElement();
            rootResource = new JdomResource(rootElem);
        } finally {
            IOUtils.closeQuietly(input);
        }

        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan(Widget.class.getPackage().getName());
        resourceBeanMapper = new JdomResourceBeanMapper(marshaller);
    }

    @Test
    public void testTraversal() throws Exception {
        assertEquals("Element", rootResource.getResourceType());
        assertTrue(rootResource.isResourceType("Element"));
        assertNull(rootResource.getName());
        assertEquals("/ui", rootResource.getPath());
        assertTrue(rootResource.getMetadata().isEmpty());
        assertNull(rootResource.getParent());

        Resource widget = (Resource) rootResource.getValue("widget");
        assertNotNull(widget);
        assertEquals("Element", widget.getResourceType());
        assertTrue(widget.isResourceType("Element"));
        assertEquals("widget", widget.getName());
        assertEquals("/ui/widget", widget.getPath());
        assertTrue(widget.isAnyChildContained());
        assertEquals("on", ((Resource) widget.getValueMap().get("debug")).getValueMap().get(""));
        assertTrue(widget.getMetadata().isEmpty());
        assertSame(rootResource, widget.getParent());

        Resource window = widget.getValueMap().get("window", Resource.class);
        assertNotNull(window);
        assertEquals("Element", window.getResourceType());
        assertTrue(window.isResourceType("Element"));
        assertEquals("window", window.getName());
        assertEquals("/ui/widget/window", window.getPath());
        assertEquals("Sample Konfabulator Widget", window.getValueMap().get("title"));
        assertEquals("main_window", ((Resource) window.getValueMap().get("name")).getValueMap().get(""));
        assertEquals(Integer.valueOf(500), ((Resource) window.getValueMap().get("width")).getValueMap().get("", Integer.class));
        assertEquals(Integer.valueOf(500), ((Resource) window.getValueMap().get("height")).getValueMap().get("", Integer.class));
        assertTrue(window.isAnyChildContained());
        assertTrue(window.getMetadata().isEmpty());
        assertSame(widget, window.getParent());

        Resource images = widget.getValueMap().get("images", Resource.class);
        assertNotNull(images);
        assertEquals("Element", images.getResourceType());
        assertTrue(images.isResourceType("Element"));
        assertEquals("images", images.getName());
        assertEquals("/ui/widget/images", images.getPath());
        assertTrue(images.isAnyChildContained());
        assertTrue(images.getMetadata().isEmpty());
        assertSame(widget, images.getParent());

        ResourceCollection imageResources = images.getChildren();
        assertEquals(2, imageResources.size());

        Resource image = imageResources.get(0);
        assertNotNull(image);
        assertEquals("Element", image.getResourceType());
        assertTrue(image.isResourceType("Element"));
        assertEquals("image", image.getName());
        assertEquals("/ui/widget/images/image[1]", image.getPath());
        assertEquals("Images/Sun.png", image.getValueMap().get("src"));
        assertEquals("sun1", image.getValueMap().get("name"));
        assertEquals(Integer.valueOf(250), ((Resource) image.getValueMap().get("hOffset")).getValueMap().get("", Integer.class));
        assertEquals(Integer.valueOf(250), ((Resource) image.getValueMap().get("vOffset")).getValueMap().get("", Integer.class));
        assertEquals("center", ((Resource) image.getValueMap().get("alignment")).getValueMap().get(""));
        assertTrue(image.isAnyChildContained());
        assertTrue(image.getMetadata().isEmpty());
        assertSame(images, image.getParent());

        image = imageResources.get(1);
        assertNotNull(image);
        assertEquals("Element", image.getResourceType());
        assertTrue(image.isResourceType("Element"));
        assertEquals("image", image.getName());
        assertEquals("/ui/widget/images/image[2]", image.getPath());
        assertEquals("Images/Moon.png", image.getValueMap().get("src"));
        assertEquals("moon1", image.getValueMap().get("name"));
        assertEquals(Integer.valueOf(100), ((Resource) image.getValueMap().get("hOffset")).getValueMap().get("", Integer.class));
        assertEquals(Integer.valueOf(100), ((Resource) image.getValueMap().get("vOffset")).getValueMap().get("", Integer.class));
        assertEquals("left", ((Resource) image.getValueMap().get("alignment")).getValueMap().get(""));
        assertTrue(image.isAnyChildContained());
        assertTrue(image.getMetadata().isEmpty());
        assertSame(images, image.getParent());

        imageResources = images.getChildren();
        assertEquals(2, imageResources.size());

        image = imageResources.get(0);
        assertNotNull(image);
        assertEquals("Element", image.getResourceType());
        assertTrue(image.isResourceType("Element"));
        assertEquals("image", image.getName());
        assertEquals("/ui/widget/images/image[1]", image.getPath());
        assertEquals("Images/Sun.png", image.getValueMap().get("src"));
        assertEquals("sun1", image.getValueMap().get("name"));
        assertEquals(Integer.valueOf(250), ((Resource) image.getValueMap().get("hOffset")).getValueMap().get("", Integer.class));
        assertEquals(Integer.valueOf(250), ((Resource) image.getValueMap().get("vOffset")).getValueMap().get("", Integer.class));
        assertEquals("center", ((Resource) image.getValueMap().get("alignment")).getValueMap().get(""));
        assertTrue(image.isAnyChildContained());
        assertTrue(image.getMetadata().isEmpty());
        assertSame(images, image.getParent());

        image = imageResources.get(1);
        assertNotNull(image);
        assertEquals("Element", image.getResourceType());
        assertTrue(image.isResourceType("Element"));
        assertEquals("image", image.getName());
        assertEquals("/ui/widget/images/image[2]", image.getPath());
        assertEquals("Images/Moon.png", image.getValueMap().get("src"));
        assertEquals("moon1", image.getValueMap().get("name"));
        assertEquals(Integer.valueOf(100), ((Resource) image.getValueMap().get("hOffset")).getValueMap().get("", Integer.class));
        assertEquals(Integer.valueOf(100), ((Resource) image.getValueMap().get("vOffset")).getValueMap().get("", Integer.class));
        assertEquals("left", ((Resource) image.getValueMap().get("alignment")).getValueMap().get(""));
        assertTrue(image.isAnyChildContained());
        assertTrue(image.getMetadata().isEmpty());
        assertSame(images, image.getParent());
    }

    @Test
    public void testValueByRelPaths() throws Exception {
        assertEquals("on", ((Resource) rootResource.getValue("widget/debug")).getValueMap().get(""));

        assertEquals("Sample Konfabulator Widget", rootResource.getValue("widget/window/@title"));
        assertEquals("main_window", ((Resource) rootResource.getValue("widget/window/name")).getValueMap().get(""));
        assertEquals(Integer.valueOf(500), rootResource.getValue("widget/window/width", Integer.class));
        assertEquals(Integer.valueOf(500), rootResource.getValue("widget/window/height", Integer.class));

        assertEquals("Click Here", rootResource.getValue("widget/text/@data"));
        assertEquals(Integer.valueOf(36), rootResource.getValue("widget/text/@size", Integer.class));
        assertEquals("bold", rootResource.getValue("widget/text/@style"));
        assertEquals("text1", rootResource.getValue("widget/text/name", String.class));
        assertEquals(Integer.valueOf(250), rootResource.getValue("widget/text/hOffset", Integer.class));
        assertEquals("center", rootResource.getValue("widget/text/alignment", String.class));

        assertEquals("Images/Sun.png", rootResource.getValue("widget/images/image/@src"));
        assertEquals("sun1", rootResource.getValue("widget/images/image[1]/@name", String.class));
        assertEquals(Integer.valueOf(250), rootResource.getValue("widget/images/image/hOffset", Integer.class));
        assertEquals(Integer.valueOf(250), rootResource.getValue("widget/images/image[1]/vOffset", Integer.class));
        assertEquals("center", rootResource.getValue("widget/images/image[1]/alignment", String.class));

        assertEquals("Images/Moon.png", rootResource.getValue("widget/images/image[2]/@src"));
        assertEquals("moon1", rootResource.getValue("widget/images/image[2]/@name"));
        assertEquals(Integer.valueOf(100), rootResource.getValue("widget/images/image[2]/hOffset", Integer.class));
        assertEquals(Integer.valueOf(100), rootResource.getValue("widget/images/image[2]/vOffset", Integer.class));
        assertEquals("left", rootResource.getValue("widget/images/image[2]/alignment", String.class));
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
    public void testBeanMapping() throws Exception {
        Resource widgetRes = rootResource.getValueMap().get("widget", Resource.class);
        Widget widget = resourceBeanMapper.map(widgetRes, Widget.class);
        assertEquals("on", widget.getDebug());

        Window window = widget.getWindow();
        assertNotNull(window);
        assertEquals("Sample Konfabulator Widget", window.getTitle());
        assertEquals("main_window", window.getName());
        assertEquals(500, window.getWidth());
        assertEquals(500, window.getHeight());

        List<Image> images = widget.getImages();
        assertNotNull(images);
        assertEquals(2, images.size());

        Image image = images.get(0);
        assertEquals("Images/Sun.png", image.getSource());
        assertEquals("sun1", image.getName());
        assertEquals(250, image.gethOffset());
        assertEquals(250, image.getvOffset());
        assertEquals("center", image.getAlignment());

        image = images.get(1);
        assertEquals("Images/Moon.png", image.getSource());
        assertEquals("moon1", image.getName());
        assertEquals(100, image.gethOffset());
        assertEquals(100, image.getvOffset());
        assertEquals("left", image.getAlignment());
    }
}
