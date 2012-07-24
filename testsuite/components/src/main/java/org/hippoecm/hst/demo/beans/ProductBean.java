/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.demo.beans;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.ContentNodeBinder;
import org.hippoecm.hst.content.beans.ContentNodeBindingException;
import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.index.IndexField;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean;

@Node(jcrType="demosite:productdocument")
public class ProductBean extends TextBean implements ContentNodeBinder {
    
    private String brand;
    private String product;
    private String color;
    private String type;
    private Double price;
    private String [] tags;
    
    private HippoGalleryImageSetBean imageBean;
    private boolean imagesLoaded = false;

    @IndexField
    public String getTitle() {
        // product does not have a title
        return this.getLocalizedName();
    }

    @IndexField
    public String getBrand() {
        if (brand == null) {
            brand = getProperty("demosite:brand");
        }
        return brand;
    }
    
    public void setBrand(String brand) {
        this.brand = brand;
    }

    @IndexField
    public String getProduct() {
        if (product == null) {
            product = getProperty("demosite:product");
        }
        return product;
    }
    
    public void setProduct(String product) {
        this.product = product;
    }

    @IndexField
    public String getColor() {
        if (color == null) {
            color = getProperty("demosite:color");
        }
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }

    @IndexField
    public String getType() {
        if (type == null) {
            type = getProperty("demosite:type");
        }
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public Double getPrice() {
        if (price == null) {
            price = getProperty("demosite:price");
        }
        return price;
    }
    
    public void setPrice(Double price) {
        this.price = price;
    }

    @IndexField
    public String[] getTags() {
        if (tags == null) {
            tags = getProperty("hippostd:tags");
        }
        return tags;
    }
    
    public void setTags(String [] tags) {
        this.tags = tags;
    }
    
    public HippoGalleryImageSetBean getImage() {
        if(imagesLoaded) {
            return this.imageBean;
        }
        imagesLoaded = true;
        this.imageBean = this.getLinkedBean("demosite:image", HippoGalleryImageSetBean.class);
        return imageBean;
    }
    
    public boolean bind(Object content, javax.jcr.Node node) throws ContentNodeBindingException {
        try {
            ProductBean product = (ProductBean) content;
            node.setProperty("demosite:brand", product.getBrand());
            node.setProperty("demosite:product", product.getProduct());
            node.setProperty("demosite:color", product.getColor());
            node.setProperty("demosite:type", product.getType());
            node.setProperty("demosite:price", product.getPrice());
            node.setProperty("hippostd:tags", product.getTags());
            return true;
        } catch (RepositoryException e) {
            throw new ContentNodeBindingException(e);
        }
    }
}
