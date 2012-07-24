/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.demo.jaxrs.model;

import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.hst.demo.beans.ProductBean;

/**
 * @version $Id$
 */
@XmlRootElement(name = "product")
public class ProductRepresentation extends BaseDocumentRepresentation {
    
    private String brand;
    private String product;
    private String color;
    private String type;
    private double price;
    private String [] tags;
    
    public ProductRepresentation represent(ProductBean bean) throws RepositoryException {
        super.represent(bean);
        this.brand = bean.getBrand();
        this.product = bean.getProduct();
        this.color = bean.getColor();
        this.type = bean.getType();
        this.price = bean.getPrice();
        this.tags = (String []) ArrayUtils.clone(bean.getTags());
        return this;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @XmlElementWrapper(name="tags")
    @XmlElements(@XmlElement(name="tag"))
    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }
    
}
