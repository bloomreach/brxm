/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.demo.beans;

import org.hippoecm.hst.content.beans.index.IndexField;
import org.hippoecm.hst.content.beans.standard.IdentifiableContentBean;

public class GoGreenProductBean implements IdentifiableContentBean {

    private String identifier;
    private String title;
    private String summary;
    private String description;

    private Double price;
    private String[] categories;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(final String identifier) {
        this.identifier = identifier;
    }

    @IndexField
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @IndexField
    public String getSummary() {
        return summary ;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    @IndexField
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @IndexField
    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }


    @IndexField
    public String[] getCategories() {
        return categories;
    }
    public void setCategories(String[] categories) {
        this.categories = categories;
    }

}
