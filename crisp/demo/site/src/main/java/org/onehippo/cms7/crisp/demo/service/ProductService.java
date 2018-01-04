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
package org.onehippo.cms7.crisp.demo.service;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceBeanMapper;
import org.onehippo.cms7.crisp.demo.model.Product;
import org.onehippo.cms7.crisp.hst.module.CrispHstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;

import static org.onehippo.cms7.crisp.demo.Constants.RESOURCE_SPACE_DEMO_PRODUCT_CATALOG;

/**
 * Example business service in Circuit Breaker pattern with {@link HystrixCommand} annotation for fallback
 * in case of exceptions or timeout (1000ms by default according to Hystrix documentation).
 *
 * @see <a href="https://github.com/Netflix/Hystrix/wiki/How-it-Works">https://github.com/Netflix/Hystrix/wiki/How-it-Works</a>
 * @see <a href="https://github.com/Netflix/Hystrix/wiki/Configuration#execution.isolation.thread.timeoutInMilliseconds">https://github.com/Netflix/Hystrix/wiki/Configuration#execution.isolation.thread.timeoutInMilliseconds</a>
 */
@Service
public class ProductService {

    private static Logger log = LoggerFactory.getLogger(ProductService.class);

    private static final URL DEMO_LOCAL_CACHED_PRODUCTS_JSON_URL = ProductService.class
            .getResource("cached-products.json");

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * The example Hystrix command operation with a fallback method and execution timeout (3 seconds in this demo).
     * <P>
     * <EM>Tip:</EM> You might want to put a debugger inside the method to make a <strong>timeout</strong> for a demo.
     * Or, you can set {@code productCatalogs} to null to throw an exception for demonstration purpose as well.
     * </P>
     * @return
     */
    @HystrixCommand(
            fallbackMethod = "getReliableProductCollection",
            commandProperties = {
                    @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "3000")
            }
        )
    public Collection<Product> getProductCollection() {
        Resource productCatalogs = null;

        ResourceServiceBroker resourceServiceBroker = CrispHstServices.getDefaultResourceServiceBroker();
        final Map<String, Object> pathVars = new HashMap<>();
        productCatalogs = resourceServiceBroker.findResources(RESOURCE_SPACE_DEMO_PRODUCT_CATALOG, "/products/",
                pathVars);

        ResourceBeanMapper resourceBeanMapper = resourceServiceBroker
                .getResourceBeanMapper(RESOURCE_SPACE_DEMO_PRODUCT_CATALOG);
        Collection<Product> productCollection = resourceBeanMapper.mapCollection(productCatalogs.getChildren(),
                Product.class);
        return productCollection;
    }

    /**
     * A reliable fallback operation example, reading cached data from a JSON data file in the classpath.
     * @return
     */
    public Collection<Product> getReliableProductCollection() {
        List<Product> productsList = new LinkedList<>();

        InputStream is = null;
        BufferedInputStream bis = null;

        try {
            is = DEMO_LOCAL_CACHED_PRODUCTS_JSON_URL.openStream();
            bis = new BufferedInputStream(is);
            JsonNode root = objectMapper.readTree(bis);

            for (Iterator<JsonNode> it = root.elements(); it.hasNext();) {
                JsonNode elem = it.next();
                Product product = objectMapper.convertValue(elem, Product.class);
                productsList.add(product);
            }
        } catch (Exception e) {
            log.error("Failed to read data from json resource file.", e);
        } finally {
            IOUtils.closeQuietly(bis);
            IOUtils.closeQuietly(is);
        }

        return productsList;
    }

}
