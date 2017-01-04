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

import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Source;

public class SortedSource implements Source {

    private final Source delegatee;
    private final Module module;

    public SortedSource(final Source delegatee, final Module module) {
        this.delegatee = delegatee;
        this.module = module;

    }

    @Override
    public String getPath() {
        return delegatee.getPath();
    }

    @Override
    public Module getModule() {
        return module;
    }

    @Override
    public List<Definition> getDefinitions() {
        return null;
    }
}
