/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.demo.example.commerce.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.onehippo.cms7.crisp.demo.example.commerce.model.Product;
import com.onehippo.cms7.crisp.demo.example.commerce.model.ProductDataResult;
import com.onehippo.cms7.crisp.demo.example.commerce.repository.ProductRepository;

@RestController
@RequestMapping("/v1")
public class ProductsController {

    @Autowired
    private ProductRepository productRepository;

    @RequestMapping(value="/products",
            method = RequestMethod.GET,
            produces = { "application/json" }
    )
    public List<Product> findProductList(@RequestParam(value="q", required=false) String query) {
        return productRepository.findProducts(query);
    }

    @RequestMapping(value="/products.xml",
            method = RequestMethod.GET,
            produces = { "application/xml" }
    )
    public ProductDataResult findProductDataResult(@RequestParam(value="q", required=false) String query) {
        ProductDataResult result = new ProductDataResult();
        result.setProducts(productRepository.findProducts(query));
        return result;
    }

}
