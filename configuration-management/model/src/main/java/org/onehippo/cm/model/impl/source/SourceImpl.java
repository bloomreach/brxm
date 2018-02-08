/*
 *  Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.impl.source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.definition.AbstractDefinitionImpl;
import org.onehippo.cm.model.source.Source;

public abstract class SourceImpl implements Source {

    final private String path;
    final private String folderPath;
    private ModuleImpl module;
    List<AbstractDefinitionImpl> modifiableDefinitions = new ArrayList<>();
    private List<AbstractDefinitionImpl> definitions = Collections.unmodifiableList(modifiableDefinitions);

    /**
     * Tracking boolean used for optimizing FileConfigurationWriter.write().
     * Defaults to true, because newly created Sources are trivially changed from their saved state (none).
     */
    private boolean hasChangedSinceLoad = true;

    public SourceImpl(final String path, final ModuleImpl module) {
        if (path == null) {
            throw new IllegalArgumentException("Parameter 'path' cannot be null");
        }
        this.path = StringUtils.stripStart(path, "/");

        if (module == null) {
            throw new IllegalArgumentException("Parameter 'module' cannot be null");
        }
        this.module = module;
        this.folderPath = path.contains("/") ? StringUtils.substringBeforeLast(path, "/") : "";
    }

    @Override
    public String getPath() {
        return path;
    }

    public String getFolderPath() {
        return folderPath;
    }

    @Override
    public ModuleImpl getModule() {
        return module;
    }

    /**
     * Should be used only when cloning a module via {@link ModuleImpl#ModuleImpl(ModuleImpl, ProjectImpl)}.
     * @param module
     */
    public void setModule(final ModuleImpl module) {
        this.module = module;
    }

    @Override
    public List<AbstractDefinitionImpl> getDefinitions() {
        return definitions;
    }

    // todo: replace all usages with addDefinition()/removeDefinition() to limit reordering
    public List<AbstractDefinitionImpl> getModifiableDefinitions() {
        return modifiableDefinitions;
    }

    public void removeDefinition(AbstractDefinitionImpl definition) {
        if (modifiableDefinitions.remove(definition)) {
            markChanged();
        }
    }

    /**
     * Convert a resource path which may be relative to this Source to a module-base-relative path.
     * @param resourcePath the resource path
     * @return a path relative to the appropriate module base and starting with '/'
     */
    public String toModulePath(final String resourcePath) {
        if (resourcePath.startsWith("/")) {
            return resourcePath;
        }
        else {
            return StringUtils.prependIfMissing(folderPath + "/" + resourcePath, "/");
        }
    }

    /**
     * Reset flag indicating that this Source has changed since being loaded.
     */
    public void markUnchanged() {
        hasChangedSinceLoad = false;
    }

    /**
     * Mark this Source as having been changed since being loaded.
     */
    public void markChanged() {
        hasChangedSinceLoad = true;
    }

    public boolean hasChangedSinceLoad() {
        return hasChangedSinceLoad;
    }

    @Override
    public String toString() {
        return "SourceImpl{" + (hasChangedSinceLoad?"CHANGED ":"") + "path='" + path + '\'' + ", module=" + module + '}';
    }

    @Override
    public String getOrigin() {
        return module.getFullName() + " [" +
                // include source type: config or content
                getType().toString().toLowerCase()
                + ": " + path
                + ']';
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
