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
package org.onehippo.cm.model.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.onehippo.cm.model.Module;
import org.onehippo.cm.model.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectImpl implements Project {

    private static final Logger log = LoggerFactory.getLogger(ProjectImpl.class);

    private static final OrderableByNameListSorter<Module> modulesSorter = new OrderableByNameListSorter<>(Module.class);

    private final String name;
    private final GroupImpl group;

    private final Set<String> modifiableAfter = new LinkedHashSet<>();
    private final Set<String> after = Collections.unmodifiableSet(modifiableAfter);

    private final List<ModuleImpl> modifiableModules = new ArrayList<>();
    private final List<ModuleImpl> modules = Collections.unmodifiableList(modifiableModules);
    private final Map<String, ModuleImpl> moduleMap = new HashMap<>();

    public ProjectImpl(final String name, final GroupImpl group) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter 'name' cannot be null");
        }
        this.name = name;

        if (group == null) {
            throw new IllegalArgumentException("Parameter 'group' cannot be null");
        }
        this.group = group;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public GroupImpl getGroup() {
        return group;
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
    public List<ModuleImpl> getModules() {
        return modules;
    }

    public ModuleImpl getModule(final String name) {
        return moduleMap.get(name);
    }

    private void verifyNewModule(final String name) {
        if (moduleMap.containsKey(name)) {
            final String msg = String.format("Module %s already exists while merging projects. Merging of modules is not supported.",
                    moduleMap.get(name).getFullName());
            throw new IllegalStateException(msg);
        }
    }

    public ModuleImpl addModule(final String name, final String hcmSiteName) {
        verifyNewModule(name);
        final ModuleImpl module = new ModuleImpl(name, this, hcmSiteName);
        moduleMap.put(name, module);
        modifiableModules.add(module);
        return module;
    }

    public void sortModules() {
        modulesSorter.sort(modifiableModules);
    }

    void addModule(final ModuleImpl other) {
        verifyNewModule(other.getName());
        // TODO: do we really need to clone the module here?
        // TODO: this makes us choose which Module back-reference will be retained in Sources
        final ModuleImpl module = new ModuleImpl(other, this);
        moduleMap.put(module.getName(), module);
        modifiableModules.add(module);
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
        return Objects.hash(name, group);
    }

    @Override
    public String toString() {
        return "ProjectImpl{" +
                "name='" + name + '\'' +
                ", group=" + group +
                '}';
    }
}
