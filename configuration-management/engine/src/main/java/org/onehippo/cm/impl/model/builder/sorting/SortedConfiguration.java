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
package org.onehippo.cm.impl.model.builder.sorting;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.Project;

import static java.util.Collections.unmodifiableMap;
import static org.onehippo.cm.impl.model.builder.sorting.DependencySorter.Sorter.getProjectSorter;

public class SortedConfiguration implements Configuration {

    private Configuration delegatee;
    private final Map<String, Project> sortedProjects = new LinkedHashMap<>();

    public SortedConfiguration(final Configuration delegatee) {
        this.delegatee = delegatee;
        SortedSet<Project> sorted = getProjectSorter().sort(delegatee.getProjects());
        if (sorted == null) {
            return;
        }
        for (Project project : sorted) {
            sortedProjects.put(project.getName(), new SortedProject(project, this));
        }
    }

    @Override
    public Map<String, Project> getProjects() {
        return unmodifiableMap(sortedProjects);
    }

    @Override
    public String getName() {
        return delegatee.getName();
    }

    @Override
    public List<String> getAfter() {
        return delegatee.getAfter();
    }
}
