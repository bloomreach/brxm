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
package org.hippoecm.hst.pagemodelapi.v09.core.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.hippoecm.hst.pagemodelapi.v09.content.beans.jackson.jackson.LinkModel;

/**
 * Identifiable and linkable metadata base model.
 */
public class IdentifiableLinkableMetadataBaseModel extends IdentifiableMetadataBaseModel {

    private Map<String, LinkModel> linksMap;

    public IdentifiableLinkableMetadataBaseModel(final String id) {
        super(id);
    }

    @JsonProperty("_links")
    @JsonInclude(Include.NON_NULL)
    public Map<String, LinkModel> getLinksMap() {
        if (linksMap == null) {
            return null;
        }

        return Collections.unmodifiableMap(linksMap);
    }

    public void putLink(String name, LinkModel value) {
        if (linksMap == null) {
            linksMap = new LinkedHashMap<>();
        }

        linksMap.put(name, value);
    }

    public LinkModel getLink(String name) {
        if (linksMap == null) {
            return null;
        }

        return linksMap.get(name);
    }

    public LinkModel removeLink(String name) {
        if (linksMap != null) {
            return linksMap.remove(name);
        }

        return null;
    }

    public void clearLinks() {
        if (linksMap != null) {
            linksMap.clear();
        }
    }
}
