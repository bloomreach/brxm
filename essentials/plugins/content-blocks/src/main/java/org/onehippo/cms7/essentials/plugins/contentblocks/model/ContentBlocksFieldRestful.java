/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugins.contentblocks.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.onehippo.cms7.essentials.dashboard.model.Restful;

public class ContentBlocksFieldRestful implements Restful {
    private String name;
    private String originalName;
    private String pickerType;
    private long maxItems;
    List<String> compoundRefs;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(final String originalName) {
        this.originalName = originalName;
    }

    public String getPickerType() {
        return pickerType;
    }

    public void setPickerType(final String pickerType) {
        this.pickerType = pickerType;
    }

    public long getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(final long maxItems) {
        this.maxItems = maxItems;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonSubTypes({
            @JsonSubTypes.Type(String.class)
    })
    public List<String> getCompoundRefs() {
        return compoundRefs;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
    @JsonSubTypes({
            @JsonSubTypes.Type(String.class)
    })
    public void setCompoundRefs(final List<String> compoundRefs) {
        this.compoundRefs = compoundRefs;
    }

    public void addCompoundRef(String compoundRef) {
        if (compoundRefs == null) {
            compoundRefs = new ArrayList<>();
        }

        compoundRefs.add(compoundRef);
    }

}
