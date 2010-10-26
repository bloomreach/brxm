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

import java.util.List;

/**
 * This Dependency sole purpose is to provide a parent path context for child dependencies.
 * It doesn't add itself to the list of dependencies returned by {@link #getDependencies(boolean)} and it doesn't
 * implement a String version of itself.
 *
 * @version $Id$
 */
public class PathDependency extends Dependency {

    public PathDependency(String path) {
        super(path);
    }

    public PathDependency(String path, Dependency... dependencies) {
        super(path);
        for (Dependency dependency : dependencies) {
            addDependency(dependency);
        }
    }

    //Nothing to return for a path dependency
    @Override
    public String asString(String path) {
        return null;
    }

    @Override
    protected void addSelf(List<Dependency> all, boolean devMode) {
    }
}
