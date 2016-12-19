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

import java.util.List;
import java.util.Map;

import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Source;


public class SortedModule implements Module {
    private Module delegatee;
    private Project project;

    public SortedModule(final Module delegatee, final Project project) {
        this.delegatee = delegatee;
        this.project = project;
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public Map<String, Source> getSources() {
        return delegatee.getSources();
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
