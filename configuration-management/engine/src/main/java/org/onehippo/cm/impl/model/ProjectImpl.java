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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;

import static java.util.Collections.unmodifiableList;

public class ProjectImpl implements Project {

    private String name;
    private ConfigurationImpl configuration;
    private List<String> after = new ArrayList<>();
    private Map<String, Module> modules = new LinkedHashMap<>();

    public ProjectImpl(final String name, final ConfigurationImpl configuration) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter 'name' cannot be null");
        }
        this.name = name;

        if (configuration == null) {
            throw new IllegalArgumentException("Parameter 'configuration' cannot be null");
        }
        this.configuration = configuration;

        this.configuration.addProject(this);
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
    public List<String> getAfter() {
        return unmodifiableList(after);
    }

    public void setAfter(final List<String> after) {
        this.after = new ArrayList<>(after);
    }

    @Override
    public Map<String, Module> getModules() {
        return Collections.unmodifiableMap(modules);
    }

    public ModuleImpl addModule(final String name) {
        final ModuleImpl module = new ModuleImpl(name, this);
        modules.put(name, module);
        return module;
    }

    void addModule(final Module module) {
        modules.put(module.getName(), module);
    }

}
