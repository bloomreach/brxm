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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.tree.ConfigurationItemCategory;
import org.onehippo.cm.model.util.ConfigurationModelUtils;

import static org.onehippo.cm.engine.autoexport.AutoExportConstants.CONFIG_EXCLUDED_PROPERTY_NAME;
import static org.onehippo.cm.engine.autoexport.AutoExportConstants.CONFIG_FILTER_UUID_PATHS_PROPERTY_NAME;
import static org.onehippo.cm.engine.autoexport.AutoExportConstants.CONFIG_INJECT_RESIDUAL_CHILD_NODE_CATEGORY_PROPERTY_NAME;
import static org.onehippo.cm.engine.autoexport.AutoExportConstants.CONFIG_MODULES_PROPERTY_NAME;
import static org.onehippo.cm.engine.autoexport.AutoExportConstants.CONFIG_OVERRIDE_RESIDUAL_CHILD_NODE_CATEGORY_PROPERTY_NAME;
import static org.onehippo.cm.engine.autoexport.AutoExportServiceImpl.log;

public class AutoExportConfig {

    private final Node node;
    private final String nodePath;
    private Boolean enabled;
    private Long lastRevision;
    private Map<String, Collection<String>> modules;
    private PatternSet exclusionContext;
    private OverrideResidualMatchers overrideResidualContext;
    private InjectResidualMatchers injectResidualMatchers;
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
            final String[] values = getMultipleStringProperty(node, CONFIG_EXCLUDED_PROPERTY_NAME);
            final List<String> excluded = new ArrayList<>(values.length);
            for (final String value : values) {
                excluded.add(value);
                log.debug("excluding path '{}'", value);
            }
            exclusionContext = new PatternSet(excluded);
        }
        return exclusionContext;
    }

    public OverrideResidualMatchers getOverrideResidualMatchers() {
        if (overrideResidualContext == null) {
            final OverrideResidualMatchers matchers = new OverrideResidualMatchers();
            final String[] values =
                    getMultipleStringProperty(node, CONFIG_OVERRIDE_RESIDUAL_CHILD_NODE_CATEGORY_PROPERTY_NAME);
            for (final String value : values) {
                try {
                    matchers.add(value);
                } catch (IllegalArgumentException e) {
                    log.warn("ignoring incorrectly formatted .meta:residual-child-node-category pattern '{}'", value, e);
                }
                log.debug("added .meta:residual-child-node-category override pattern '{}'", value);
            }
            overrideResidualContext = matchers;
        }
        return overrideResidualContext;
    }

    public InjectResidualMatchers getInjectResidualMatchers() {
        if (injectResidualMatchers == null) {
            final InjectResidualMatchers matchers = new InjectResidualMatchers();
            final String[] values =
                    getMultipleStringProperty(node, CONFIG_INJECT_RESIDUAL_CHILD_NODE_CATEGORY_PROPERTY_NAME);
            for (final String value : values) {
                try {
                    matchers.add(value);
                } catch (IllegalArgumentException e) {
                    log.warn("ignoring incorrectly formatted .meta:residual-child-node-category inject pattern '{}'",
                            value, e);
                }
                log.debug("added .meta:residual-child-node-category inject pattern '{}'", value);
            }
            injectResidualMatchers = matchers;
        }
        return injectResidualMatchers;
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
            final String[] values = getMultipleStringProperty(node, CONFIG_FILTER_UUID_PATHS_PROPERTY_NAME);
            for (final String value: values) {
                filterUuidPaths.add(value);
                log.debug("filtering uuid paths below {}", value);
            }
        }
        return filterUuidPaths;
    }

    /**
     * Returns the multiple string property value <code>propertyName</code> from <code>baseNode</code> or an empty array
     * if no such property exists. Slight variation of {@link JcrUtils#getMultipleStringProperty(Node, String, String[])}
     * to also catch and log any {@link RepositoryException}.
     *
     * @param baseNode     existing node that should be the base for the relative path
     * @param propertyName property name of the property to get
     * @return
     */
    private String[] getMultipleStringProperty(final Node baseNode, final String propertyName) {
        final String[] defaultValue = new String[0];
        try {
            return JcrUtils.getMultipleStringProperty(baseNode, propertyName, defaultValue);
        } catch (RepositoryException e) {
            log.error("Failed to get auto export property {}, defaulting to no values", propertyName, e);
            return defaultValue;
        }
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

    /**
     * Determine the category of a node at the specified absolute path. This method differs from
     * {@link ConfigurationModelUtils#getCategoryForNode(String, ConfigurationModel)} in the sense that it
     * also takes the configured overrides for .meta:residual-child-node-category into account.
     *
     * @param absoluteNodePath absolute path to a node
     * @param model            configuration model to check against
     * @return                 category of the node pointed to
     */
    public ConfigurationItemCategory getCategoryForNode(final String absoluteNodePath,
                                                        final ConfigurationModelImpl model) {
        return getCategoryForItem(absoluteNodePath, false, model);
    }

    /**
     * Determine the category of a node at the specified absolute path. This method differs from
     * {@link ConfigurationModelUtils#getCategoryForItem(String, boolean, ConfigurationModel)} in the sense that it
     * also takes the configured overrides for .meta:residual-child-node-category into account.
     *
     * @param absoluteItemPath absolute path a an item
     * @param propertyPath     indicates whether the item is a node or property
     * @param model            configuration model to check against
     * @return                 category of the property pointed to
     */
    public ConfigurationItemCategory getCategoryForItem(final String absoluteItemPath,
                                                        final boolean propertyPath,
                                                        final ConfigurationModel model) {
        final OverrideResidualMatchers matchers = getOverrideResidualMatchers();
        return ConfigurationModelUtils.getCategoryForItem(absoluteItemPath, propertyPath, model, matchers::getMatch);
    }

}
