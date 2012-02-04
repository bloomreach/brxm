/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.hst.site.addon.module.model;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(namespace="http://www.onehippo.org/schema/hst/hst-addon-module_1_0.xsd", name="module")
@XmlType(namespace="http://www.onehippo.org/schema/hst/hst-addon-module_1_0.xsd", name="module",
        propOrder={ "name", "configLocations", "moduleDefinitions" })
public class ModuleDefinition {

    private String name;
    private List<String> configLocations;
    private List<ModuleDefinition> moduleDefinitions;

    @XmlElement(name="name", required=true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElementWrapper(name="config-locations")
    @XmlElement(name="config-location")
    public List<String> getConfigLocations() {
        return configLocations;
    }

    public void setConfigLocations(List<String> configLocations) {
        this.configLocations = configLocations;
    }

    @XmlElementWrapper(name="modules")
    @XmlElement(name="module")
    public List<ModuleDefinition> getModuleDefinitions() {
        return moduleDefinitions;
    }

    public void setModuleDefinitions(List<ModuleDefinition> moduleDefinitions) {
        this.moduleDefinitions = moduleDefinitions;
    }
}
