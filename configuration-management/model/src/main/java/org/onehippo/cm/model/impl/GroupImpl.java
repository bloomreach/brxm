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

import org.onehippo.cm.model.Group;
import org.onehippo.cm.model.Project;

public class GroupImpl implements Group {

    private static final OrderableByNameListSorter<Project> projectsSorter = new OrderableByNameListSorter<>(Project.class);

    private final String name;

    // Non-final because of annoying code-flow problem in ModuleReader.readReplacement()
    private String hcmSiteName;

    private final Set<String> modifiableAfter = new LinkedHashSet<>();
    private final Set<String> after = Collections.unmodifiableSet(modifiableAfter);

    private final List<ProjectImpl> modifiableProjects = new ArrayList<>();
    private final List<ProjectImpl> projects = Collections.unmodifiableList(modifiableProjects);
    private final Map<String, ProjectImpl> projectMap = new HashMap<>();

    public GroupImpl(final String name) {
        this(name, null);
    }

    public GroupImpl(final String name, final String hcmSiteName) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter 'name' cannot be null");
        }
        this.name = name;
        this.hcmSiteName = hcmSiteName;
    }

    public GroupImpl(final String name, final String hcmSiteName, final String... after) {
        this(name, hcmSiteName);
        Collections.addAll(modifiableAfter, after);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getHcmSiteName() {
        return hcmSiteName;
    }

    // Exists only because of annoying code-flow problem in ModuleReader.readReplacement()
    // Should not be used for any other purpose!
    public void setHcmSiteName(final String hcmSiteName) {
        this.hcmSiteName = hcmSiteName;
    }

    @Override
    public Set<String> getAfter() {
        return after;
    }

    public GroupImpl addAfter(final Set<String> after) {
        modifiableAfter.addAll(after);
        return this;
    }

    @Override
    public List<ProjectImpl> getProjects() {
        return projects;
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

    public ProjectImpl getOrAddProject(final String name) {
        return projectMap.containsKey(name) ? projectMap.get(name) : addProject(name);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof Group) {
            final Group otherGroup = (Group) other;
            return this.getName().equals(otherGroup.getName())
                    && Objects.equals(getHcmSiteName(), otherGroup.getHcmSiteName());
        }
        return false;
    }

    // hashCode() and equals() should be consistent!
    @Override
    public int hashCode() {
        return Objects.hash(name, hcmSiteName);
    }

    @Override
    public String toString() {
        return "GroupImpl{" +
                "hcmSiteName='" + ((hcmSiteName==null)?"core":hcmSiteName) + '\'' +
                "name='" + name + '\'' +
                '}';
    }
}
