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
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.impl.model.builder.OrderableListSorter;

public class ConfigurationImpl implements Configuration {

    private static final OrderableListSorter<ProjectImpl> projectsSorter = new OrderableListSorter<>(Project.class.getSimpleName());

    private final String name;

    private final Set<String> modifiableAfter = new LinkedHashSet<>();
    private final Set<String> after = Collections.unmodifiableSet(modifiableAfter);

    private final List<ProjectImpl> modifiableProjects = new ArrayList<>();
    private final List<Project> projects = Collections.unmodifiableList(modifiableProjects);
    private final Map<String, ProjectImpl> projectMap = new HashMap<>();

    public ConfigurationImpl(final String name) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter 'name' cannot be null");
        }
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<String> getAfter() {
        return after;
    }

    public ConfigurationImpl addAfter(final Set<String> after) {
        modifiableAfter.addAll(after);
        return this;
    }

    @Override
    public List<Project> getProjects() {
        return projects;
    }

    public List<ProjectImpl> getModifiableProjects() {
        return modifiableProjects;
    }

    public ProjectImpl addProject(final String name) {
        final ProjectImpl project = new ProjectImpl(name, this);
        projectMap.put(name, project);
        modifiableProjects.add(project);
        return project;
    }

    public void sortProjects() {
        projectsSorter.sort(modifiableProjects);
        modifiableProjects.forEach(ProjectImpl::sortModules);
    }

    public void pushProject(final ProjectImpl project) {
        final String name = project.getName();
        final ProjectImpl consolidated = projectMap.containsKey(name) ? projectMap.get(name) : addProject(name);

        consolidated.addAfter(project.getAfter());
        project.getModifiableModules().forEach(consolidated::pushModule);
    }
}
