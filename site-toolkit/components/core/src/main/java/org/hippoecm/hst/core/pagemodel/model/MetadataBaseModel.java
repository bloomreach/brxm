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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Metabase base model.
 */
public class MetadataBaseModel {

    private Map<String, Object> metadataMap;

    public MetadataBaseModel() {
    }

    @JsonProperty("_meta")
    @JsonInclude(Include.NON_NULL)
    public Map<String, Object> getMetadataMap() {
        if (metadataMap == null) {
            return null;
        }

        return Collections.unmodifiableMap(metadataMap);
    }

    public void putMetadata(String name, Object value) {
        if (metadataMap == null) {
            metadataMap = new LinkedHashMap<>();
        }

        metadataMap.put(name, value);
    }

    public Object getMetadata(String name) {
        if (metadataMap == null) {
            return null;
        }

        return metadataMap.get(name);
    }

    public Object removeMetadata(String name) {
        if (metadataMap != null) {
            return metadataMap.remove(name);
        }

        return null;
    }

    public void clearMetadataMap() {
        if (metadataMap != null) {
            metadataMap.clear();
        }
    }

}
