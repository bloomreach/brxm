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
package org.hippoecm.frontend.plugins.yui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wicket.util.string.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YuiDependencyResolver implements Serializable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(YuiDependencyResolver.class);

    private final Map<YuiDependency, Set<YuiDependency>> cachedDependencies = Collections
            .synchronizedMap(new HashMap<YuiDependency, Set<YuiDependency>>());
    private final List<String> cachedNamespaces = Collections.synchronizedList(new LinkedList<String>());
    
    public YuiDependencyResolver() {
        cachedNamespaces.add(YuiHeaderContributor.YUI_NAMESPACE);
    }
    
    public Set<YuiDependency> resolveDependencies(YuiDependency dependency) {
        if (hasDependenciesCached(dependency)) {
            return getCachedDependencies(dependency);
        } else {
            return addDependenciesToCache(dependency);
        }
    }

    private Set<YuiDependency> addDependenciesToCache(YuiDependency dependency) {
        if (!cachedNamespaces.contains(dependency.getNamespace()))
            cachedNamespaces.add(dependency.getNamespace());

        Set<YuiDependency> dependencies = fetchModuleDependencies(dependency);
        cachedDependencies.put(dependency, dependencies);
        return dependencies;
    }

    private Set<YuiDependency> getCachedDependencies(YuiDependency dependency) {
        return cachedDependencies.get(dependency);
    }

    private boolean hasDependenciesCached(YuiDependency dependency) {
        return cachedDependencies.containsKey(dependency);
    }

    private YuiDependency findDependency(String moduleName) {
        for (YuiDependency dep : cachedDependencies.keySet()) {
            if (dep.getModule().equals(moduleName)) {
                return dep;
            }
        }
        return null;
    }

    private Set<YuiDependency> fetchModuleDependencies(YuiDependency dependency) {
        Set<YuiDependency> dependencies = new LinkedHashSet<YuiDependency>();
        doFetchModuleDependencies(dependencies, dependency);
        return dependencies;
    }

    private InputStream findFile(YuiDependency dependency) {
        InputStream is = null;
        List<String> fileNamespaces;
        if (dependency.getNamespace() != null) {
            fileNamespaces = new ArrayList<String>();
            fileNamespaces.add(dependency.getNamespace());
        } else {
            fileNamespaces = cachedNamespaces;
        }
        for (String namespace : fileNamespaces) {
            String[] suffixes = new String[] { "", "-beta", "-experimental" };
            dependency.setNamespace(namespace);

            for (int j = 0; j < suffixes.length; j++) {
                String path = dependency.getModulePath() + suffixes[j] + ".js";
                is = getClass().getResourceAsStream(path);
                if (is == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Unable to find " + path);
                    }
                } else {
                    dependency.setSuffix(suffixes[j]);
                    if (log.isDebugEnabled()) {
                        log.debug("Found " + path);
                    }

                    URL url = getClass().getResource(dependency.getCssPath());
                    dependency.setHasCss(url != null);

                    return is;
                }
            }
        }
        return null;
    }

    private void doFetchModuleDependencies(Set<YuiDependency> dependencies, YuiDependency dependency) {
        InputStream is = findFile(dependency);
        if (is == null) {
            if (log.isInfoEnabled()) {
                log.info("No source found for module " + dependency.getModule() + " in namespaces " + cachedNamespaces);
            }
            dependency.setSourceNotFound(true);
            return;
        }

        BufferedReader buffy = new BufferedReader(new InputStreamReader(is));
        String line;
        Pattern pattern = Pattern.compile("\\* @requires (.*)");
        String modules = "";
        try {
            while ((line = buffy.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Found a match with " + line);
                    }
                    modules = matcher.group(1);
                    break;
                }
            }
            StringTokenizer st = new StringTokenizer(modules, ",");
            while (st.hasMoreElements()) {
                String moduleName = (String) st.nextElement();
                if (!Strings.isEmpty(moduleName)) {
                    moduleName = moduleName.trim().toLowerCase();

                    Set<YuiDependency> newDependencies = null;
                    YuiDependency newDependency = findDependency(moduleName);
                    if (newDependency == null) {
                        newDependency = new YuiDependency(null, moduleName);
                        newDependencies = new LinkedHashSet<YuiDependency>();
                        doFetchModuleDependencies(newDependencies, newDependency);
                        if (newDependency.getNamespace() != null)
                            cachedDependencies.put(newDependency, newDependencies);
                    } else {
                        newDependencies = getCachedDependencies(newDependency);
                    }
                    dependencies.addAll(newDependencies);
                    if (newDependency != null && newDependency.getNamespace() != null)
                        dependencies.add(newDependency);

                }
            }
            return;
        } catch (IOException e) {
            log.error("Error reading module" + dependency.getModule() + " in namespaces " + cachedNamespaces, e);
            return;
        } finally {
            try {
                buffy.close();
            } catch (IOException e) {
                log.error("Enable to close BufferedReader", e);
            }
        }
    }

}
