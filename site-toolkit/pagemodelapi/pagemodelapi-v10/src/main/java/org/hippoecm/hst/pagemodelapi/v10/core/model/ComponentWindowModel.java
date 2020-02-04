/*
 *  Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagemodelapi.v10.core.model;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.container.HstComponentWindow;

/**
 * Component window model representation.
 */
@JsonPropertyOrder({"id", "links", "meta","name", "label", "type", "xtype", "componentClass", "params", "paramsInfo", "definitionId", "children", "models"})
public class ComponentWindowModel extends IdentifiableLinkableMetadataBaseModel {

    private final String name;
    private final String componentClass;
    private final String type;
    private final String label;
    private Map<String, Object> models;
    private Set<ComponentWindowModel> components;

    @JsonInclude(Include.NON_NULL)
    private final String xtype;

    public ComponentWindowModel(final HstComponentWindow window) {
        super(window.getReferenceNamespace());
        name = window.getName();
        componentClass = window.getComponentName();

        final HstComponentConfiguration.Type componentType = window.getComponentInfo().getComponentType();
        type = componentType.toString();
        xtype = componentType.equals(HstComponentConfiguration.Type.CONTAINER_COMPONENT)
                ? StringUtils.lowerCase(window.getComponent().getComponentConfiguration().getXType())
                : null;

        label = window.getComponentInfo().getLabel();

        final Map<String, HstComponentWindow> childComponentWindows = window.getChildWindowMap();

        if (!childComponentWindows.isEmpty()) {
            components = new LinkedHashSet<>();

            for (HstComponentWindow child : childComponentWindows.values()) {
                components.add(new ComponentWindowModel(child));
            }
        }
    }

    /**
     * @return the container item component's name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the component's type name. i.e component class' FQCN.
     */
    public String getComponentClass() {
        return componentClass;
    }

    /**
     * @return the component type, see {@link HstComponentConfiguration.Type}
     */
    public String getType() {
        return type;
    }

    public String getXtype() {
        return xtype;
    }

    /**
     * Return component's label. i.e. hst:label property value of an hst:containeritemcomponent.
     * @return
     */
    @JsonInclude(Include.NON_NULL)
    public String getLabel() {
        return label;
    }

    @JsonInclude(Include.NON_NULL)
    public Set<ComponentWindowModel> getChildren() {
        return components;
    }

    /**
     * Return the contributed model map by <code>HstRequest{@link HstRequest#setModel(String,Object)} calls.
     * @return
     */
    @JsonInclude(Include.NON_NULL)
    public Map<String, Object> getModels() {
        return models;
    }

    public void putModel(String name, Object model) {
        if (models == null) {
            models = new LinkedHashMap<>();
        }

        models.put(name, model);
    }


}
