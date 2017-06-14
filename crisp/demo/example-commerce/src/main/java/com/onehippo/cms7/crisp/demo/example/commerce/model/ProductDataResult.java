/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.demo.example.commerce.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "data")
public class ProductDataResult {

    private List<Product> products;

    @XmlElementWrapper(name = "products")
    @XmlElements(@XmlElement(name = "product"))
    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

}
