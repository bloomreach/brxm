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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceBeanMapper;
import org.onehippo.cms7.crisp.api.resource.ResourceCollection;
import org.onehippo.cms7.crisp.core.resource.jackson.model.ExtendedData;
import org.onehippo.cms7.crisp.core.resource.jackson.model.Image;
import org.onehippo.cms7.crisp.core.resource.jackson.model.Product;
import org.onehippo.cms7.crisp.core.resource.jackson.model.Widget;
import org.onehippo.cms7.crisp.core.resource.jackson.model.Window;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;;

public class JacksonResourceMappingTest {

    private Resource widgetRootResource;
    private Resource productsRootResource;
    private ResourceBeanMapper resourceBeanMapper;

    @Before
    public void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        InputStream input = null;

        try {
            input = getClass().getResourceAsStream("widget.json");
            JsonNode rootNode = objectMapper.readTree(input);
            widgetRootResource = new JacksonResource(rootNode);
        } finally {
            IOUtils.closeQuietly(input);
        }

        try {
            input = getClass().getResourceAsStream("products.json");
            JsonNode rootNode = objectMapper.readTree(input);
            productsRootResource = new JacksonResource(rootNode);
        } finally {
            IOUtils.closeQuietly(input);
        }

        resourceBeanMapper = new JacksonResourceBeanMapper(objectMapper);
    }

    @Test
    public void testBeanMapping() throws Exception {
        Resource widgetRes = widgetRootResource.getValueMap().get("widget", Resource.class);
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

    @Test
    public void testBeanMappingCollection() throws Exception {
        final ResourceCollection resCol = productsRootResource.getChildren();
        final Collection<Product> productsCollection = resourceBeanMapper.mapCollection(resCol, Product.class);
        assertNotNull(productsCollection);
        assertEquals(resCol.size(), productsCollection.size());

        Product product = productsCollection.iterator().next();
        assertEquals("4150349", product.getSKU());
        assertEquals("MultiSync X431BT - 109.22 cm (43 \") , 1920 x 480, 16:4, 500 cd/m\u00b2, 3000:1, 8 ms",
                product.getDescription());
        assertEquals("NEC MultiSync X431BT", product.getName());

        ExtendedData extendedData = product.getExtendedData();
        assertEquals("NEC MultiSync X431BT", extendedData.getTitle());
        assertEquals("Link", extendedData.getType());
        assertEquals("Incentro-HIC-Site/-/products/4150349", extendedData.getUri());
        assertEquals("MultiSync X431BT - 109.22 cm (43 \") , 1920 x 480, 16:4, 500 cd/m\u00b2, 3000:1, 8 ms",
                extendedData.getDescription());
    }

    @Test
    public void testBeanMappingCollectionPage() throws Exception {
        final ResourceCollection resCol = productsRootResource.getChildren();
        final List<Product> productsList = new LinkedList<>();
        resourceBeanMapper.mapCollection(resCol, Product.class, productsList, 1, 2);
        assertNotNull(productsList);
        assertNotEquals(resCol.size(), productsList.size());
        assertEquals(2, productsList.size());
        assertEquals("4696003", productsList.get(0).getSKU());
        assertEquals("4017125", productsList.get(1).getSKU());
    }
}
