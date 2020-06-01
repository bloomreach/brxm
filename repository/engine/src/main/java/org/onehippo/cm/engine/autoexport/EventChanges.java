/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.RevisionEvent;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.tree.ConfigurationItemCategory;

/**
 * Tracks and maps changed jcr paths from EventJournalProcessor events into separate added, changed or deleted PathsMaps,
 * as well as splits them between CONFIG and CONTENT changes.
 * In addition, it also tracks changes to a namespace/cnd (prefix), which change 'events' are produced via
 * the {@link NodeTypeChangesMonitor}
 */
class EventChanges {

    /**
     * struct class keeping added, changed, deleted PathMaps for CONFIG or CONTENT changes
     */
    private static class Paths {
        PathsMap added = new PathsMap();
        PathsMap changed = new PathsMap();
        PathsMap deleted = new PathsMap();

        public boolean isEmpty() {
            return added.isEmpty() && changed.isEmpty() && deleted.isEmpty();
        }
    }

    private final AutoExportConfig autoExportConfig;
    private final ConfigurationModelImpl model;
    private long creationTime;

    private Set<String> changedNsPrefixes = new HashSet<>();
    private final Paths configPaths = new Paths();
    private final Paths contentPaths = new Paths();

    protected EventChanges(final AutoExportConfig autoExportConfig, final ConfigurationModelImpl model) {
        this.autoExportConfig = autoExportConfig;
        this.model = model;
        this.creationTime = System.currentTimeMillis();
    }

    public boolean isEmpty() {
        return changedNsPrefixes.isEmpty() && configPaths.isEmpty() && contentPaths.isEmpty();
    }

    public Set<String> getChangedNsPrefixes() {
        return changedNsPrefixes;
    }

    public Set<String> getChangedConfig() {
        return configPaths.changed.getPaths();
    }

    public Set<String> getAddedContent() {
        return contentPaths.added.getPaths();
    }

    public Set<String> getChangedContent() {
        return contentPaths.changed.getPaths();
    }

    public Set<String> getDeletedContent() {
        return contentPaths.deleted.getPaths();
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void recordChangedNsPrefix(final String changedNsPrefix) {
        changedNsPrefixes.add(changedNsPrefix);
    }

    public void recordEvent(final RevisionEvent event, final String eventPath, final boolean addedNode,
                            final boolean deletedNode, final boolean isPropertyEvent) throws RepositoryException {

        if (!autoExportConfig.isExcludedPath(eventPath)) {

            // use getCategoryForItem from AutoExportConfig as that also takes into account category overrides
            final ConfigurationItemCategory category = autoExportConfig.getCategoryForItem(eventPath, isPropertyEvent, model);

            if (category != ConfigurationItemCategory.SYSTEM) {
                Paths paths = category == ConfigurationItemCategory.CONFIG ? configPaths : contentPaths;
                if (!paths.added.matches(eventPath)) {
                    if (addedNode) {
                        recordAddedNode(paths, eventPath);
                    } else if (deletedNode) {
                        recordDeletedNode(paths, eventPath);
                        // Note: if a *config* path is deleted, it also may have *content* children, so we need to check and register such deleted content subpaths
                        // to be thereafter processed by the DefinitionMergeService
                        if (category == ConfigurationItemCategory.CONFIG) {
                            checkDeletedContentChildren(JcrPaths.getPath(eventPath));
                        }
                    }
                    final String parentPath = eventPath.substring(0, eventPath.lastIndexOf('/') == 0 ? 1 : eventPath.lastIndexOf('/'));
                    if (category == ConfigurationItemCategory.CONFIG) {
                        // for config, we record the parent path of all nodes or properties as changed to be visited by the AutoExportConfigExporter
                        recordChangedNode(paths, parentPath, true);
                    } else if (!addedNode && !deletedNode) {
                        // for content, we only record the actual path of changed nodes (not properties)
                        recordChangedNode(paths, isPropertyEvent ? parentPath : eventPath, false);
                    }
                }
            }
        }
    }

    /*
     * When a config node is deleted (in jcr), check if there were child nodes which mapped to *content* definitions,
     * and if so record these as 'to be deleted' content paths for the DefinitionMergeService to handle later.
     */
    protected void checkDeletedContentChildren(final JcrPath deletedConfig) throws RepositoryException {
        for (final ContentDefinitionImpl contentDefinition : model.getContentDefinitions()) {
            final JcrPath contentRootPath = contentDefinition.getNode().getJcrPath();
            final String contentRoot = contentRootPath.suppressIndices().toString();
            if (contentRootPath.startsWith(deletedConfig) && !contentPaths.deleted.matches(contentRoot)) {
                // content root found as child of a deleted config path, which itself, or a parent path, hasn't been recorded as deleted yet
                contentPaths.deleted.removeChildren(contentRoot);
                contentPaths.deleted.add(contentRoot);
            }
        }
    }

    /* Merge new (current) changes onto pending changes */
    public void mergeCurrentChanges(EventChanges currentChanges) {
        changedNsPrefixes.addAll(currentChanges.getChangedNsPrefixes());
        mergePaths(configPaths, currentChanges.configPaths, true);
        mergePaths(contentPaths, currentChanges.contentPaths, false);
    }

    private void mergePaths(final Paths paths, final Paths currentPaths, final boolean isConfig) {
        for (final String deletedPath : currentPaths.deleted) {
            recordDeletedNode(paths, deletedPath);
        }
        for (final String addedPath : currentPaths.added) {
            if (!paths.added.matches(addedPath)) {
                recordAddedNode(paths, addedPath);
            }
        }
        for (final String changedPath : currentPaths.changed) {
            recordChangedNode(paths, changedPath, isConfig);
        }
    }

    private void recordAddedNode(final Paths paths, final String nodePath) {
        // precondition of this method call is !added.matches(nodePath)
        paths.added.removeChildren(nodePath);
        paths.added.add(nodePath);
        // cleanup a previously-encountered events for descendants which are now obsolete or redundant
        paths.deleted.removeChildren(nodePath);
        paths.deleted.remove(nodePath);
        paths.changed.removeChildren(nodePath);
        paths.changed.remove(nodePath);
    }

    private void recordDeletedNode(final Paths paths, final String nodePath) {
        if (!paths.deleted.matches(nodePath)) {
            paths.deleted.removeChildren(nodePath);
            paths.deleted.add(nodePath);
        }
        // clean up previously-recorded events for descendants, which are now redundant,
        // since this delete will clear out all descendants anyway
        paths.added.removeChildren(nodePath);
        paths.added.remove(nodePath);
        paths.changed.removeChildren(nodePath);
        paths.changed.remove(nodePath);
    }

    private boolean recordChangedNode(final Paths paths, final String nodePath, final boolean isConfig) {
        if (isConfig) {
            // config paths can/should be collapsed by root path
            paths.changed.removeChildren(nodePath);
        }
        return paths.changed.add(nodePath);
    }
}
