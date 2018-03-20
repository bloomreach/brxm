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
package org.onehippo.cms7.crisp.demo.example.commerce.repository;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.crisp.demo.example.commerce.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class ProductRepository {

    private static Logger log = LoggerFactory.getLogger(ProductRepository.class);

    private List<Product> productList;

    public ProductRepository() {
    }

    public List<Product> findProducts(final String query) {
        if (StringUtils.isBlank(query)) {
            return Collections.unmodifiableList(productList);
        }

        // NOTE: Only for demonstration purpose, simply iterate each product and collect item
        //       if product's toString() contains the query string.

        List<Product> list = new LinkedList<>();

        int size = productList.size();
        Product product;
        String name;

        for (int i = 0; i < size; i++) {
            product = productList.get(i);
            if (StringUtils.containsIgnoreCase(product.toString(), query)) {
                list.add(product);
            }
        }

        return Collections.unmodifiableList(list);
    }

    public Product findProductBySku(final String sku) {
        if (StringUtils.isBlank(sku)) {
            return null;
        }

        // NOTE: Only for demonstration purpose, simply iterate each product to find one by sku.

        int size = productList.size();
        Product product;
        String name;

        for (int i = 0; i < size; i++) {
            product = productList.get(i);
            if (StringUtils.equals(product.getSku(), sku)) {
                return product;
            }
        }

        return null;
    }

    @PostConstruct
    public void init() {
        final URL dataUrl = getClass().getResource("/META-INF/example/commerce/data/products.json");
        loadProductList(dataUrl);
    }

    private void loadProductList(final URL dataUrl) {
        if (productList == null) {
            List<Product> list = new LinkedList<>();

            ObjectMapper objectMapper = new ObjectMapper();
            InputStream is = null;
            BufferedInputStream bis = null;

            try {
                is = dataUrl.openStream();
                bis = new BufferedInputStream(is);
                JsonNode root = objectMapper.readTree(bis);
                JsonNode elem;
                Product product;

                for (Iterator<JsonNode> it = root.elements(); it.hasNext(); ) {
                    elem = it.next();
                    product = objectMapper.convertValue(elem, Product.class);
                    list.add(product);
                }
            } catch (Exception e) {
                log.error("Failed to read data from json resource file.", e);
            } finally {
                IOUtils.closeQuietly(bis);
                IOUtils.closeQuietly(is);
            }

            productList = list;
        }
    }
}
