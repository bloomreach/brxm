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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;

import static org.onehippo.cm.impl.model.builder.sorting.DependencySorter.Sorter.getModuleSorter;

public class SortedProject implements Project {
    
    private Project delegatee;
    private Configuration configuration;
    private final Map<String, Module> sortedModules = new LinkedHashMap<>();

    public SortedProject(final Project delegatee, final Configuration configuration) {
        this.delegatee = delegatee;
        this.configuration = configuration;
        SortedSet<Module> sorted = getModuleSorter().sort(delegatee.getModules());
        if (sorted == null) {
            return;
        }
        for (Module module : sorted) {
            sortedModules.put(module.getName(), new SortedModule(module, delegatee));
        }
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public Map<String, Module> getModules() {
        return sortedModules;
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
