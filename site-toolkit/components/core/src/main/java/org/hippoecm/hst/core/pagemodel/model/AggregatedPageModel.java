/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.pagemodel.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Aggregated page model which represents the whole output in the page model pipeline request processing.
 */
public class AggregatedPageModel extends IdentifiableLinkableMetadataBaseModel {

    private ComponentWindowModel page;
    private Map<String, Object> contentMap;
    private Map<String, ComponentWindowModel> flattened = new HashMap<>();

    public AggregatedPageModel(final String id) {
        super(id);
    }

    @JsonProperty("page")
    public ComponentWindowModel getPage() {
        return page;
    }

    public void setPage(final ComponentWindowModel page) {
        this.page = page;
        populateFlattened(page);
    }

    private void populateFlattened(final ComponentWindowModel model) {
        flattened.put(model.getId(), model);

        final Set<ComponentWindowModel> components = model.getComponents();
        if (components != null) {
            for (ComponentWindowModel child : components) {
                populateFlattened(child);
            }
        }
    }

    @JsonProperty("content")
    @JsonInclude(Include.NON_NULL)
    public Map<String, Object> getContentMap() {
        return contentMap;
    }

    public void putContent(String id, Object content) {
        if (contentMap == null) {
            contentMap = new LinkedHashMap<>();
        }
        contentMap.put(id, content);
    }

    @JsonIgnore
    public Optional<ComponentWindowModel> getModel(final String referenceNamespace) {
        return Optional.ofNullable(flattened.get(referenceNamespace));
    }

}
