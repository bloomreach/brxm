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
import java.util.WeakHashMap;

import org.apache.wicket.Application;
import org.onehippo.yui.YuiDependency;
import org.onehippo.yui.YuiDependencyResolver;
import org.onehippo.yui.YuiNamespace;

public class CachedYuiDependencyResolver {

    private static final long serialVersionUID = 1L;

    private static class CacheEntry {
        private final Map<String, Set<YuiDependency>> modulesLoaded = Collections
        .synchronizedMap(new HashMap<String, Set<YuiDependency>>());

        private boolean cacheEnabled = true;
        private YuiDependencyResolver dependencyResolver = null;

        CacheEntry(Application application) {
            cacheEnabled = application.getConfigurationType().equals(Application.DEPLOYMENT);
            dependencyResolver = new YuiDependencyResolver(cacheEnabled);
        }
        
        Set<YuiDependency> getDependencies(YuiNamespace ns, String module) {
            if (cacheEnabled && modulesLoaded.containsKey(module)) {
                return modulesLoaded.get(module);
            }

            Set<YuiDependency> dependencies = dependencyResolver.resolveDependencies(ns, module);
            if (cacheEnabled) {
                modulesLoaded.put(module, dependencies);
            }
            return dependencies;
        }
    }


    private static WeakHashMap<Application, CacheEntry> cache = new WeakHashMap<Application, CacheEntry>();
    
    public static Set<YuiDependency> getDependencies(YuiNamespace ns, String module) {
        Application application = Application.get();
        synchronized (cache) {
            if (!cache.containsKey(application)) {
                cache.put(application, new CacheEntry(application));
            }
    
            return cache.get(application).getDependencies(ns, module);
        }
    }

}
