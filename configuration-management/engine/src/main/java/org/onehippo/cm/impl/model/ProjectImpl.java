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
package org.onehippo.cm.impl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.impl.model.builder.sorting.OrderableComparator;

public class ProjectImpl implements Project {

    private String name;
    private ConfigurationImpl configuration;

    private Set<String> modifiableAfter = new LinkedHashSet<>();
    private Set<String> after = Collections.unmodifiableSet(modifiableAfter);

    private final List<ModuleImpl> modifiableModules = new ArrayList<>();
    private final List<Module> modules = Collections.unmodifiableList(modifiableModules);
    private final Map<String, ModuleImpl> moduleMap = new HashMap<>();

    public ProjectImpl(final String name, final ConfigurationImpl configuration) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter 'name' cannot be null");
        }
        this.name = name;

        if (configuration == null) {
            throw new IllegalArgumentException("Parameter 'configuration' cannot be null");
        }
        this.configuration = configuration;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public Set<String> getAfter() {
        return after;
    }

    public ProjectImpl addAfter(final Set<String> after) {
        modifiableAfter.addAll(after);
        return this;
    }

    @Override
    public List<Module> getModules() {
        return modules;
    }

    public List<ModuleImpl> getModifiableModules() {
        return modifiableModules;
    }

    public ModuleImpl addModule(final String name) {
        final ModuleImpl module = new ModuleImpl(name, this);
        moduleMap.put(name, module);
        modifiableModules.add(module);
        return module;
    }

    public void sortModules() {
        modifiableModules.sort(new OrderableComparator<>());
    }

    void pushModule(final ModuleImpl module) {
        final String name = module.getName();
        final ModuleImpl consolidated = moduleMap.containsKey(name) ? moduleMap.get(name) : addModule(name);

        consolidated.addAfter(module.getAfter());
        consolidated.pushDefinitions(module);
    }
}
