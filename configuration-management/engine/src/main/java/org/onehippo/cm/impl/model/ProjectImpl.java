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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableList;

public class ProjectImpl implements Project {

    private String name;
    private Configuration configuration;
    private List<String> after;
    private Map<String, Module> modules;

    @Override
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public List<String> getAfter() {
        if (after == null) {
            return emptyList();
        }
        return unmodifiableList(after);
    }

    public void setAfter(final List<String> after) {
        this.after = after;
    }

    @Override
    public Map<String, Module> getModules() {
        if (modules ==null) {
            return emptyMap();
        }
        return Collections.unmodifiableMap(modules);
    }

    public void setModules(final Map<String, Module> modules) {
        this.modules = modules;
    }

}
