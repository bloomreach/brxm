/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.autoexport;

import static org.onehippo.cms7.autoexport.AutoExportModule.log;
import static org.onehippo.cms7.autoexport.Constants.CONFIG_ENABLED_PROPERTY_NAME;
import static org.onehippo.cms7.autoexport.Constants.CONFIG_EXCLUDED_PROPERTY_NAME;
import static org.onehippo.cms7.autoexport.Constants.CONFIG_FILTER_UUID_PATHS_PROPERTY_NAME;
import static org.onehippo.cms7.autoexport.Constants.CONFIG_MODULES_PROPERTY_NAME;
import static org.onehippo.cms7.autoexport.Constants.CONFIG_NODE_PATH;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.observation.Event;

public class Configuration {

    private final Session session;

    private Boolean enabled;
    private Map<String, Collection<String>> modules;
    private ExclusionContext exclusionContext;
    private List<String> filterUuidPaths;
    
    Configuration(Session session) {
        this.session = session;
    }

    // constructor for testing purposes
    Configuration(Boolean enabled, Map<String, Collection<String>> modules, ExclusionContext exclusionContext, List<String> filterUuidPaths) {
        this.enabled = enabled;
        this.modules = modules;
        this.exclusionContext = exclusionContext;
        this.filterUuidPaths = filterUuidPaths;
        session = null;
    }
    
    ExclusionContext getExclusionContext() {
        if (exclusionContext == null) {
            List<String> excluded = Collections.emptyList();
            try {
                Node node = session.getNode(CONFIG_NODE_PATH);
                if (node.hasProperty(CONFIG_EXCLUDED_PROPERTY_NAME)) {
                    Value[] values = node.getProperty(CONFIG_EXCLUDED_PROPERTY_NAME).getValues();
                    excluded = new ArrayList<String>(values.length);
                    for (Value value : values) {
                        String exclude = value.getString();
                        excluded.add(exclude);
                        if (log.isDebugEnabled()) {
                            log.debug("excluding path " + exclude);
                        }
                    }
                }
            } catch (RepositoryException e) {
                log.error("Failed to get excluded paths from repository", e);
            }
            exclusionContext = new ExclusionContext(excluded);
        }
        return exclusionContext;
    }

    Map<String, Collection<String>> getModules() {
        if (modules == null) {
            modules = new HashMap<String, Collection<String>>();
            try {
                Node node = session.getNode(CONFIG_NODE_PATH);
                if (node.hasProperty(CONFIG_MODULES_PROPERTY_NAME)) {
                    boolean rootRepositoryPathIsConfigured = false;
                    Collection<String> allRepositoryPaths = new HashSet<String>();
                    Value[] values = node.getProperty(CONFIG_MODULES_PROPERTY_NAME).getValues();
                    for (Value value : values) {
                        String module = value.getString();
                        int offset = module.indexOf(":/");
                        if (offset == -1) {
                            log.error("Misconfiguration of " + CONFIG_MODULES_PROPERTY_NAME + " property: expected ':/'");
                            continue;
                        }
                        String modulePath = module.substring(0, offset);
                        String repositoryPath = module.substring(offset+1);
                        if (!allRepositoryPaths.contains(repositoryPath)) {
                            addRepositoryPath(modulePath, repositoryPath);
                            allRepositoryPaths.add(repositoryPath);
                            if (repositoryPath.equals("/")) {
                                rootRepositoryPathIsConfigured = true;
                            }
                        } else {
                            log.error("Misconfiguration of " + CONFIG_MODULES_PROPERTY_NAME + " property: the same repository path may not be mapped to multiple modules");
                        }
                    }
                    if (!rootRepositoryPathIsConfigured) {
                        log.warn("Misconfiguration of " + CONFIG_MODULES_PROPERTY_NAME + " property: there must be a module that maps to /");
                        addRepositoryPath("content", "/");
                    }
                } else {
                    addRepositoryPath("content", "/");
                }
            } catch (RepositoryException e) {
                log.error("Failed to get modules configuration from repository", e);
            }
        }
        return modules;
    }
    
    Boolean isExportEnabled() {
        if (enabled == null) {
            try {
                Node node = session.getNode(CONFIG_NODE_PATH);
                enabled = node.getProperty(CONFIG_ENABLED_PROPERTY_NAME).getBoolean();
            } catch (PathNotFoundException e) {
                enabled = false;
                log.debug("No such item: " + CONFIG_NODE_PATH + "/" + CONFIG_ENABLED_PROPERTY_NAME);
            } catch (RepositoryException e) {
                enabled = false;
                log.error("Exception while reading export enabled flag.", e);
            }
        }
        return enabled;
    }

    List<String> getFilterUuidPaths() {
        if (filterUuidPaths == null) {
            filterUuidPaths = Collections.emptyList();
            try {
                Node node = session.getNode(CONFIG_NODE_PATH);
                if (node.hasProperty(CONFIG_FILTER_UUID_PATHS_PROPERTY_NAME)) {
                    Value[] values = node.getProperty(CONFIG_FILTER_UUID_PATHS_PROPERTY_NAME).getValues();
                    filterUuidPaths = new ArrayList<String>(values.length);
                    for (Value value : values) {
                        String filterUuidPath = value.getString();
                        filterUuidPaths.add(filterUuidPath);
                        if (log.isDebugEnabled()) {
                            log.debug("filtering uuid paths below " + filterUuidPath);
                        }
                    }
                }
            } catch (RepositoryException e) {
                log.error("Failed to get filter uuid paths from repository", e);
            }
        }
        return filterUuidPaths;
    }

    void handleConfigurationEvent(Event event) {
        try {
            if (event.getPath().equals(CONFIG_NODE_PATH + "/" + CONFIG_ENABLED_PROPERTY_NAME)) {
                if (log.isDebugEnabled()) {
                    log.debug("Enabled flag changed");
                }
                enabled = null;
            }
            if (event.getPath().equals(CONFIG_NODE_PATH + "/" + CONFIG_EXCLUDED_PROPERTY_NAME)) {
                if (log.isDebugEnabled()) {
                    log.debug("Excluded property changed");
                }
                exclusionContext = null;
            }
            if (event.getPath().equals(CONFIG_NODE_PATH + "/" + CONFIG_FILTER_UUID_PATHS_PROPERTY_NAME)) {
                if (log.isDebugEnabled()) {
                    log.debug("Filteruuidpaths property changed");
                }
                filterUuidPaths = null;
            }
        } catch (RepositoryException e) {
            log.error("Exception while handling configuration event", e);
        }
    }
    
    private void addRepositoryPath(String modulePath, String repositoryPath) {
        Collection<String> repositoryPaths = modules.get(modulePath);
        if (repositoryPaths == null) {
            repositoryPaths = new ArrayList<String>();
            modules.put(modulePath, repositoryPaths);
        }
        if (isExportEnabled()) {
            log.info("Changes to repository path '{}' will be exported to directory '{}'", repositoryPath, modulePath);
        }
        repositoryPaths.add(repositoryPath);
    }


}
