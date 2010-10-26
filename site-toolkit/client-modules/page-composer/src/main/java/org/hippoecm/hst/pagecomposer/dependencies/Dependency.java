/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.pagecomposer.dependencies;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Composite type for setting up a client-side dependency graph
 *
 * @version $Id$
 */
public abstract class Dependency {

    private String path;
    private List<Dependency> dependencies;
    private Dependency parent;

    public Dependency(String path) {
        this.path = path;
    }

    /**
     * Return all dependencies. Might include self (subclasses decide)
     *
     * @param devMode If set dependencies will return debug sources instead of production sources
     * @return All dependencies of this dependency
     */
    public Collection<Dependency> getDependencies(boolean devMode) {
        List<Dependency> all = new LinkedList<Dependency>();
        addSelf(all, devMode);
        if (dependencies != null) {
            for (Dependency d : dependencies) {
                all.addAll(d.getDependencies(devMode));
            }
        }
        return all;
    }

    /**
     * Return a string representation of this dependency
     *
     * @param path The (optional) path that should be used. This can be a different value than the internal path field,
     *             because it can be parsed by the {@link DependencyWriter}
     * @return A String representation of this dependency
     */
    public abstract String asString(String path);

    /**
     * Add a dependency
     *
     * @param dep Dependency to add
     */
    protected void addDependency(Dependency dep) {
        if (dependencies == null) {
            dependencies = new LinkedList<Dependency>();
        }
        dependencies.add(dep);
        dep.setParent(this);
    }

    /**
     * Method for adding self to dependencies returned by {@link #getDependencies(boolean devMode)}. This way subclasses
     * can decide if they should be included or not.
     *
     * @param all List of previously resolved dependencies
     * @param devMode Switch indicating if we want debug sources or not
     */
    protected void addSelf(List<Dependency> all, boolean devMode) {
        all.add(this);
    }

    /**
     * Returns the computed path value for this dependency. If path field starts with a slash,
     * the parent path is ignored, otherwise the local path is concatenated with the parent path.
     *
     * @return Computed path for this dependency, or null if no path set.
     */
    protected String getPath() {
        if (path == null) {
            return null;
        }
        String parentPath = getParentPath();
        if (parentPath != null && !path.startsWith("/")) {
            return parentPath + path;
        }
        return path;
    }

    /**
     * Compute parent path
     *
     * @return Parent path
     */
    private String getParentPath() {
        if (parent == null) {
            return null;
        }
        String parentPath = parent.getPath();
        if (parentPath != null && !parentPath.endsWith("/")) {
            return parentPath + "/";
        }
        return parentPath;
    }

    protected void setParent(Dependency parent) {
        this.parent = parent;
    }

    protected Dependency getParent() {
        return parent;
    }

}
