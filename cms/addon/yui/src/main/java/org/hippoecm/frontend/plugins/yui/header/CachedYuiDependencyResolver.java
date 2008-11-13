/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.yui.header;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.Application;
import org.onehippo.yui.YuiDependency;
import org.onehippo.yui.YuiDependencyResolver;
import org.onehippo.yui.YuiNamespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CachedYuiDependencyResolver {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(CachedYuiDependencyResolver.class);

    private static final Map<String, Set<YuiDependency>> modulesLoaded = Collections
            .synchronizedMap(new HashMap<String, Set<YuiDependency>>());

    private static boolean cacheEnabled = true;
    private static YuiDependencyResolver dependencyResolver = null;

    public static Set<YuiDependency> getDependencies(YuiNamespace ns, String module) {
        return getDependencies(new YuiDependency(ns, module));
    }

    public static Set<YuiDependency> getDependencies(YuiDependency dependency) {
        if (dependencyResolver == null) {
            cacheEnabled = Application.get().getConfigurationType().equals(Application.DEPLOYMENT);
            dependencyResolver = new YuiDependencyResolver(cacheEnabled);
        }

        if (cacheEnabled && modulesLoaded.containsKey(dependency.getModule())) {
            return modulesLoaded.get(dependency.getModule());
        }

        Set<YuiDependency> dependencies = dependencyResolver.resolveDependencies(dependency);
        if (dependency.getOptionalDependencies() != null) {
            for (YuiDependency optsDependency : dependency.getOptionalDependencies()) {
                dependencies.add(optsDependency);
            }
        }
        dependencies.add(dependency);
        if (cacheEnabled)
            modulesLoaded.put(dependency.getModule(), dependencies);
        return dependencies;
    }

}
