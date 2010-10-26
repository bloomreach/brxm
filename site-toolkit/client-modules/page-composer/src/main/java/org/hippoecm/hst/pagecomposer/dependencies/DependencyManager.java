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
 * Simple class that keeps a list of dependencies and a developmentMode flag and uses both to write dependencies into
 * a {@link DependencyWriter}
 *
 * @version $Id$
 */
public class DependencyManager {

    private boolean devMode;
    private List<Dependency> dependencies;

    public DependencyManager() {
        this(false);
    }

    public DependencyManager(boolean devMode) {
        dependencies = new LinkedList<Dependency>();
        this.devMode = devMode;
    }

    public void add(Dependency dependency) {
        dependencies.add(dependency);
    }

    public void write(DependencyWriter writer) {
        for (Dependency dependency : dependencies) {
            Collection<Dependency> col = dependency.getDependencies(devMode);
            for (Dependency inner : col) {
                writer.write(inner);
            }
        }
    }

}
