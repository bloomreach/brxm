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
package org.onehippo.cms7.crisp.demo.example.commerce.controller;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms7.crisp.demo.example.commerce.model.Product;
import org.onehippo.cms7.crisp.demo.example.commerce.model.ProductDataResult;
import org.onehippo.cms7.crisp.demo.example.commerce.model.ProductErrorsBody;
import org.onehippo.cms7.crisp.demo.example.commerce.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class ProductsController {

    private static final String UNKNOWN_QUERY_VALUE = "unknown";

    @Autowired
    private ProductRepository productRepository;

    @RequestMapping(value="/products",
            method = { RequestMethod.GET, RequestMethod.POST },
            consumes = { "*/*" },
            produces = { "application/json" }
    )
    public ResponseEntity<Object> findProductList(@RequestParam(value="q", required=false) String query) {
        // Example 404 handling for demo purpose.
        if (UNKNOWN_QUERY_VALUE.equals(query)) {
            return new ResponseEntity<>(new ProductErrorsBody(HttpStatus.NOT_FOUND.value(), "Product(s) not found."),
                    HttpStatus.NOT_FOUND);
        }

        final List<Product> products = productRepository.findProducts(query);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @RequestMapping(value="/products.xml",
            method = { RequestMethod.GET, RequestMethod.POST },
            consumes = { "*/*" },
            produces = { "application/xml" }
    )
    public ResponseEntity<Object> findProductDataResult(@RequestParam(value="q", required=false) String query) {
        if (UNKNOWN_QUERY_VALUE.equals(query)) {
            return new ResponseEntity<>(new ProductErrorsBody(HttpStatus.NOT_FOUND.value(), "Product(s) not found."),
                    HttpStatus.NOT_FOUND);
        }

        ProductDataResult result = new ProductDataResult();
        result.setProducts(productRepository.findProducts(query));
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @RequestMapping(value="/products/{sku}/image/download",
            method = { RequestMethod.GET, RequestMethod.POST },
            consumes = { "*/*" },
            produces = { "image/jpeg" }
    )
    public Resource downloadProductImage(@PathVariable(value="sku") String sku) {
        // Simply return a classpath resource to write a binary (only for demonstration purpose).
        return new ClassPathResource("META-INF/example/commerce/data/windpower.jpg");
    }

    @RequestMapping(value="/products/{sku}",
            method = { RequestMethod.GET, RequestMethod.PUT },
            consumes = { "application/json" },
            produces = { "application/json" }
    )
    public Product updateProduct(@PathVariable("sku") String sku, @RequestBody Product product) {
        return product;
    }

    @RequestMapping(value="/products/{sku}",
            method = { RequestMethod.DELETE, RequestMethod.PUT },
            consumes = { "*/*" },
            produces = { "application/json" }
    )
    public Product deleteProduct(@PathVariable("sku") String sku) {
        if (StringUtils.isBlank(sku)) {
            return null;
        }

        return productRepository.findProductBySku(sku);
    }

}
