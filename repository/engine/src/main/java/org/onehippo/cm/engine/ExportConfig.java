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
package org.onehippo.cm.engine;

import org.onehippo.cm.engine.autoexport.PathsMap;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.tree.ConfigurationItemCategory;
import org.onehippo.cm.model.util.ConfigurationModelUtils;
import org.onehippo.cm.model.util.PatternSet;

public class ExportConfig {

    private PathsMap ignoredPaths = new PathsMap();
    private PatternSet exclusionContext;
    private PatternSet filterUuidPaths;

    public void addIgnoredPaths(final PathsMap ignoredPaths) {
        this.ignoredPaths.addAll(ignoredPaths);
    }

    public PatternSet getExclusionContext() {
        return exclusionContext;
    }

    public void setExclusionContext(final PatternSet exclusionContext) {
        this.exclusionContext = exclusionContext;
    }

    public boolean isExcludedPath(final String path) {
        if (ignoredPaths.matches(path)) {
            return true;
        }
        final PatternSet exclusionContext = getExclusionContext();
        return exclusionContext != null && exclusionContext.matches(path);
    }

    public PatternSet getFilterUuidPaths() {
        return filterUuidPaths;
    }

    public void setFilterUuidPaths(final PatternSet filterUuidPaths) {
        this.filterUuidPaths = filterUuidPaths;
    }

    public boolean shouldFilterUuid(final String nodePath) {
        final PatternSet filterUuidPaths = getFilterUuidPaths();
        return filterUuidPaths != null && filterUuidPaths.matches(nodePath);
    }

    /**
     * Determine the category of a node or property at the specified absolute path. This method simply delegates to
     * {@link ConfigurationModelUtils#getCategoryForItem(String, boolean, ConfigurationModel)}, but custom ExporterConfig
     * implementations like {@link org.onehippo.cm.engine.autoexport.AutoExportConfig} can override this to
     * provide custom handling.
     *
     * @param absoluteNodePath absolute path to a node
     * @param propertyPath     indicates whether the item is a node or property
     * @param model            configuration model to check against
     * @return                 category of the node or property pointed to
     */
    public ConfigurationItemCategory getCategoryForItem(final String absoluteNodePath,
                                                        final boolean propertyPath,
                                                        final ConfigurationModel model) {
        return ConfigurationModelUtils.getCategoryForItem(absoluteNodePath, propertyPath, model);
    }
}
