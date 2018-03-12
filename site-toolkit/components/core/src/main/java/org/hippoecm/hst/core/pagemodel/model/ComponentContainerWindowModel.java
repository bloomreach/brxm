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
import java.util.LinkedHashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Component container window model representation.
 */
public class ComponentContainerWindowModel extends IdentifiableLinkableMetadataBaseModel {

    private final String name;
    private Set<ComponentWindowModel> componentWindows;

    public ComponentContainerWindowModel(final String id, final String name) {
        super(id);
        this.name = name;
    }

    /**
     * Return container component's name.
     * @return
     */
    @JsonInclude(Include.NON_NULL)
    public String getName() {
        return name;
    }

    @JsonProperty("components")
    public Set<ComponentWindowModel> getComponentWindows() {
        if (componentWindows == null) {
            return Collections.emptySet();
        }

        return componentWindows;
    }

    public void addComponentWindow(ComponentWindowModel componentWindow) {
        if (componentWindows == null) {
            componentWindows = new LinkedHashSet<>();
        }

        componentWindows.add(componentWindow);
    }
}
