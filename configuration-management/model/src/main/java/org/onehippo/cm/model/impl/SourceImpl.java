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
import java.util.List;
import java.util.Objects;

import org.onehippo.cm.model.Source;

public abstract class SourceImpl implements Source {

    private String path;
    private ModuleImpl module;
    List<AbstractDefinitionImpl> modifiableDefinitions = new ArrayList<>();
    private List<AbstractDefinitionImpl> definitions = Collections.unmodifiableList(modifiableDefinitions);

    public SourceImpl(final String path, final ModuleImpl module) {
        if (path == null) {
            throw new IllegalArgumentException("Parameter 'path' cannot be null");
        }
        this.path = path;

        if (module == null) {
            throw new IllegalArgumentException("Parameter 'module' cannot be null");
        }
        this.module = module;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public ModuleImpl getModule() {
        return module;
    }

    @Override
    public List<AbstractDefinitionImpl> getDefinitions() {
        return definitions;
    }

    public List<AbstractDefinitionImpl> getModifiableDefinitions() {
        return modifiableDefinitions;
    }

    @Override
    public String toString() {
        return "SourceImpl{" + "path='" + path + '\'' + ", module=" + module + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SourceImpl)) return false;
        if (!(this.getClass() == o.getClass())) return false;
        SourceImpl source = (SourceImpl) o;
        return Objects.equals(path, source.path) &&
                Objects.equals(module, source.module);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, module, this.getClass());
    }
}
