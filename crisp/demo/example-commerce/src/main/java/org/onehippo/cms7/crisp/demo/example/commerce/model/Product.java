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
package org.onehippo.cms7.crisp.demo.example.commerce.model;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;

@XmlRootElement(name = "product")
public class Product {

    private String sku;
    private String description;
    private String name;
    private Map<String, Object> extendedData = new LinkedHashMap<>();

    @JsonProperty("SKU")
    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getExtendedData() {
        return extendedData;
    }

    public void setExtendedData(Map<String, Object> extendedData) {
        this.extendedData = extendedData;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Product)) {
            return false;
        }

        Product that = (Product) o;

        return StringUtils.equals(this.sku, that.sku) && StringUtils.equals(this.description, that.description)
                && StringUtils.equals(this.name, that.name) && (this.extendedData.equals(that.extendedData));
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(name).append(sku).append(description).append(extendedData).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("name", name).append("sku", sku).append("description", description)
                .append(extendedData).toString();
    }
}
