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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.Project;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

public class ConfigurationImpl implements Configuration {

    private String name;
    private List<String> after = new ArrayList<>();
    private Map<String, Project> projects = new LinkedHashMap<>();

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

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public List<String> getAfter() {
        return unmodifiableList(after);
    }

    public void setAfter(final List<String> after) {
        this.after = new ArrayList<>(after);
    }

    @Override
    public Map<String, Project> getProjects() {
        return unmodifiableMap(projects);
    }

    public void setProjects(final Map<String, Project> projects) {
        this.projects = new LinkedHashMap<>(projects);
    }

    public ProjectImpl addProject(final String name) {
        final ProjectImpl project = new ProjectImpl(name, this);
        projects.put(name, project);
        return project;
    }

    void addProject(final Project project) {
        projects.put(project.getName(), project);
    }

}
