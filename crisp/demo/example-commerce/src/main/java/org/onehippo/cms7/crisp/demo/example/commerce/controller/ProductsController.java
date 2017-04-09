package org.onehippo.cms7.crisp.demo.example.commerce.controller;

import java.util.List;

import org.onehippo.cms7.crisp.demo.example.commerce.model.Product;
import org.onehippo.cms7.crisp.demo.example.commerce.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/products")
public class ProductsController {

    private static Logger log = LoggerFactory.getLogger(ProductsController.class);

    @Autowired
    private ProductRepository productRepository;

    @RequestMapping(value="/", method = RequestMethod.GET)
    public List<Product> findProducts(@RequestParam(value="q", required=false) String query) {
        return productRepository.findProducts(query);
    }

}
