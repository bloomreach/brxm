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

import org.hippoecm.hst.core.component.HstRequest;

/**
 * Component window model representation.
 */
public class ComponentWindowModel extends IdentifiableLinkableMetadataBaseModel {

    private final String name;
    private final String type;
    private String label;
    private Map<String, Object> models;

    public ComponentWindowModel(final String id, final String name, final String type) {
        super(id);
        this.name = name;
        this.type = type;
    }

    /**
     * Return container item component's name.
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Return component's type name. i.e component class' FQCN.
     * @return
     */
    public String getType() {
        return type;
    }

    /**
     * Return component's label. i.e. hst:label property value of an hst:containeritemcomponent.
     * @return
     */
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Return the contributed model map by <code>HstRequest{@link HstRequest#setModel(String,Object)} calls.
s     * @return
     */
    public Map<String, Object> getModels() {
        if (models == null) {
            return Collections.emptyMap();
        }

        return models;
    }

    public void setModels(Map<String, Object> models) {
        this.models = models;
    }

    public void putModel(String name, Object model) {
        if (models == null) {
            models = new LinkedHashMap<>();
        }

        models.put(name, model);
    }

}
