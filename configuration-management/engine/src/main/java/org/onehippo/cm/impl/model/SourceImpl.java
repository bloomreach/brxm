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

import java.util.List;

import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Source;

public class SourceImpl implements Source {

    private String path;
    private Module module;
    private List<Definition> definitions;

    @Override
    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    @Override
    public Module getModule() {
        return module;
    }

    public void setModule(final Module module) {
        this.module = module;
    }

    @Override
    public List<Definition> getDefinitions() {
        return definitions;
    }

    public void setDefinitions(final List<Definition> definitions) {
        this.definitions = definitions;
    }
}
