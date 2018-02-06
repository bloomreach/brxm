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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.hippoecm.hst.content.beans.standard.HippoBean;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AggregatedPageModel {

    private PageDefinitionModel pageDefinition;
    private Set<ComponentContainerWindowModel> containerWindowSet;
    private Map<String, HippoBean> contentMap;

    public PageDefinitionModel getPageDefinition() {
        return pageDefinition;
    }

    public void setPageDefinition(PageDefinitionModel pageDefinition) {
        this.pageDefinition = pageDefinition;
    }

    @JsonProperty("containers")
    public Set<ComponentContainerWindowModel> getContainerWindowSet() {
        if (containerWindowSet == null) {
            return Collections.emptySet();
        }

        return containerWindowSet;
    }

    public void setContainerWindowSet(Set<ComponentContainerWindowModel> containerWindowSet) {
        this.containerWindowSet = containerWindowSet;
    }

    public void addContainerWindow(ComponentContainerWindowModel containerWindow) {
        if (containerWindowSet == null) {
            containerWindowSet = new LinkedHashSet<>();
        }

        containerWindowSet.add(containerWindow);
    }

    @JsonProperty("content")
    public Map<String, HippoBean> getContentMap() {
        if (contentMap == null) {
            return Collections.emptyMap();
        }

        return contentMap;
    }

    public void setContentMap(Map<String, HippoBean> contentMap) {
        this.contentMap = contentMap;
    }

    public void putContent(String id, HippoBean content) {
        if (contentMap == null) {
            contentMap = new LinkedHashMap<>();
        }

        contentMap.put(id, content);
    }
}
