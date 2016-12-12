/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.onehippo.cm.api.model.ConfigurationGroup;
import org.onehippo.cm.api.model.ConfigurationModule;
import org.onehippo.cm.api.model.ConfigurationSource;

public class ConfigurationModuleImpl implements ConfigurationModule {
    private String name;
    private List<ConfigurationModule> dependsOn;
    private ConfigurationGroup group;

    public ConfigurationModuleImpl(final ConfigurationGroup group, final String name) {
        this.name = name;
        this.dependsOn = new ArrayList<>();
        this.group = group;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ConfigurationGroup getGroup() {
        return group;
    }

    @Override
    public List<ConfigurationModule> getDependsOn() {
        return dependsOn;
    }

    void addDependency(final ConfigurationModule module) {
        dependsOn.add(module);
    }

    @Override
    public List<ConfigurationSource> getSources() {
        return Collections.emptyList();
    }

}
