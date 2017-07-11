/*
 *  Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.autoexport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.repository.util.JcrUtils;

import static org.onehippo.cm.engine.autoexport.AutoExportServiceImpl.log;
import static org.onehippo.cm.engine.autoexport.AutoExportConstants.CONFIG_EXCLUDED_PROPERTY_NAME;
import static org.onehippo.cm.engine.autoexport.AutoExportConstants.CONFIG_FILTER_UUID_PATHS_PROPERTY_NAME;
import static org.onehippo.cm.engine.autoexport.AutoExportConstants.CONFIG_MODULES_PROPERTY_NAME;

public class AutoExportConfig {

    private final Node node;
    private final String nodePath;
    private Boolean enabled;
    private Long lastRevision;
    private Map<String, Collection<String>> modules;
    private PatternSet exclusionContext;
    private PathsMap filterUuidPaths;
    private PathsMap ignoredPaths = new PathsMap();

    // constructor for testing purposes only
    AutoExportConfig(Boolean enabled, Map<String, Collection<String>> modules, PatternSet exclusionContext, PathsMap filterUuidPaths) {
        this.node = null;
        this.nodePath = null;
        this.enabled = enabled;
        this.modules = modules;
        this.exclusionContext = exclusionContext;
        this.filterUuidPaths = filterUuidPaths;
    }

    public AutoExportConfig(final Node node) throws RepositoryException {
        this.node = node;
        this.nodePath = node.getPath();
    }

    public String getModuleConfigPath() {
        return nodePath;
    }

    public Session getModuleSession() throws RepositoryException {
        return node.getSession();
    }

    public void addIgnoredPaths(final PathsMap ignoredPaths) {
        this.ignoredPaths.addAll(ignoredPaths);
    }

    public boolean isExcludedPath(final String path) {
        return ignoredPaths.matches(path) || getExclusionContext().matches(path);
    }

    public PatternSet getExclusionContext() {
        if (exclusionContext == null) {
            List<String> excluded = Collections.emptyList();
            try {
                if (node.hasProperty(CONFIG_EXCLUDED_PROPERTY_NAME)) {
                    Value[] values = node.getProperty(CONFIG_EXCLUDED_PROPERTY_NAME).getValues();
                    excluded = new ArrayList<>(values.length);
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
            exclusionContext = new PatternSet(excluded);
        }
        return exclusionContext;
    }

    Map<String, Collection<String>> getModules() {
        if (modules == null) {
            modules = new LinkedHashMap<>();
            final ArrayList<String> moduleStrings = getModuleStringsFromNode();
            processModuleStrings(moduleStrings, modules, isEnabled());
        }
        return modules;
    }

    private ArrayList<String> getModuleStringsFromNode() {
        final ArrayList<String> moduleStrings = new ArrayList<>();
        try {
            if (node.hasProperty(CONFIG_MODULES_PROPERTY_NAME)) {
                Value[] values = node.getProperty(CONFIG_MODULES_PROPERTY_NAME).getValues();
                for (Value value : values) {
                    moduleStrings.add(value.getString());
                }
            } else {
                addRepositoryPath("content", "/", modules, isEnabled());
            }
        } catch (RepositoryException e) {
            log.error("Failed to get modules configuration from repository", e);
        }
        return moduleStrings;
    }

    public static void processModuleStrings(final ArrayList<String> moduleStrings,
                                   final Map<String, Collection<String>> modules, final boolean logChanges) {
        boolean rootRepositoryPathIsConfigured = false;
        Collection<String> allRepositoryPaths = new HashSet<>();
        for (String module : moduleStrings) {
            int offset = module.indexOf(":/");
            if (offset == -1) {
                if (logChanges) {
                    log.error("Misconfiguration of " + CONFIG_MODULES_PROPERTY_NAME + " property: expected ':/'");
                }
                continue;
            }
            String modulePath = module.substring(0, offset);
            String repositoryPath = module.substring(offset+1);
            if (!allRepositoryPaths.contains(repositoryPath)) {
                addRepositoryPath(modulePath, repositoryPath, modules, logChanges);
                allRepositoryPaths.add(repositoryPath);
                if (repositoryPath.equals("/")) {
                    rootRepositoryPathIsConfigured = true;
                }
            } else {
                if (logChanges) {
                    log.error("Misconfiguration of " + CONFIG_MODULES_PROPERTY_NAME + " property: the same repository path may not be mapped to multiple modules");
                }
            }
        }
        if (!rootRepositoryPathIsConfigured) {
            if (logChanges) {
                log.warn("Misconfiguration of " + CONFIG_MODULES_PROPERTY_NAME + " property: there must be a module that maps to /");
            }
            addRepositoryPath("content", "/", modules, logChanges);
        }
    }

    private static void addRepositoryPath(final String modulePath, final String repositoryPath,
                                   final Map<String, Collection<String>> modules, final boolean logChanges) {
        Collection<String> repositoryPaths = modules.get(modulePath);
        if (repositoryPaths == null) {
            repositoryPaths = new ArrayList<>();
            modules.put(modulePath, repositoryPaths);
        }
        if (logChanges) {
            log.info("Changes to repository path '{}' will be exported to directory '{}'", repositoryPath, modulePath);
        }
        repositoryPaths.add(repositoryPath);
    }

    PathsMap getFilterUuidPaths() {
        if (filterUuidPaths == null) {
            filterUuidPaths = new PathsMap();
            try {
                if (node.hasProperty(CONFIG_FILTER_UUID_PATHS_PROPERTY_NAME)) {
                    Value[] values = node.getProperty(CONFIG_FILTER_UUID_PATHS_PROPERTY_NAME).getValues();
                    for (int i = 0; i < values.length; i++) {
                        Value value = values[i];
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

    public boolean shouldFilterUuid(final String nodePath) {
        return getFilterUuidPaths().matches(nodePath);
    }

    public synchronized boolean checkEnabled() {
        enabled = null;
        return isEnabled();
    }

    public synchronized boolean isEnabled() {
        if (enabled == null) {
            if ("false".equals(System.getProperty(AutoExportConstants.SYSTEM_PROPERTY_AUTOEXPORT_ENABLED))) {
                enabled = false;
            } else {
                try {
                    enabled = JcrUtils.getBooleanProperty(node, AutoExportConstants.CONFIG_ENABLED_PROPERTY_NAME, false);
                } catch (RepositoryException e) {
                    AutoExportServiceImpl.log.error("Failed to read AutoExport configuration", e);
                    enabled = false;
                }
            }
        }
        return enabled;
    }

    long getLastRevision() throws RepositoryException {
        if (lastRevision == null) {
            lastRevision = JcrUtils.getLongProperty(node, AutoExportConstants.CONFIG_LAST_REVISION_PROPERTY_NAME, -1l);
        }
        return lastRevision;
    }

    /**
     * Sets the lastRevision property, but DOES NOT save the JCR session.
     * @param lastRevision the new value of the lastRevision property
     * @throws RepositoryException
     */
    void setLastRevision(final long lastRevision) throws RepositoryException {
        node.setProperty(AutoExportConstants.CONFIG_LAST_REVISION_PROPERTY_NAME, lastRevision);
        this.lastRevision = lastRevision;
    }

    synchronized void resetLastRevision() {
        lastRevision = null;
    }
}
