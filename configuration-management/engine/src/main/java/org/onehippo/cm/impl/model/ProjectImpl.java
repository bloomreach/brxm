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
import java.util.Objects;
import java.util.Set;

import org.onehippo.cm.api.model.Group;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.impl.model.builder.OrderableListSorter;

public class ProjectImpl implements Project {

    private static final OrderableListSorter<ModuleImpl> modulesSorter = new OrderableListSorter<>(Module.class.getSimpleName());

    private final String name;
    private final GroupImpl configuration;

    private final Set<String> modifiableAfter = new LinkedHashSet<>();
    private final Set<String> after = Collections.unmodifiableSet(modifiableAfter);

    private final List<ModuleImpl> modifiableModules = new ArrayList<>();
    private final List<Module> modules = Collections.unmodifiableList(modifiableModules);
    private final Map<String, ModuleImpl> moduleMap = new HashMap<>();

    public ProjectImpl(final String name, final GroupImpl configuration) {
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
    public Group getGroup() {
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
        modulesSorter.sort(modifiableModules);
    }

    void pushModule(final ModuleImpl module) {
        final String name = module.getName();

        if (moduleMap.containsKey(name)) {
            final String msg = String.format("Module %s already exists while merging projects. Merging of modules is not supported.",
                    ModelUtils.formatModule(module));
            throw new IllegalStateException(msg);
        }

        addModule(name).addAfter(module.getAfter()).pushDefinitions(module);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Project) {
            Project otherProject = (Project)other;
            return getName().equals(otherProject.getName()) &&
                    getGroup().equals(((Project)other).getGroup());
        }
        return false;
    }

    // hashCode() and equals() should be consistent!
    @Override
    public int hashCode() {
        return Objects.hash(name, configuration);
    }
}
