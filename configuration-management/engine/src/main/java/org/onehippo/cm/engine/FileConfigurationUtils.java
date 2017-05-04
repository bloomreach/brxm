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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Source;

import static org.onehippo.cm.engine.Constants.REPO_CONFIG_FOLDER;
import static org.onehippo.cm.engine.Constants.REPO_CONTENT_FOLDER;

public class FileConfigurationUtils {

    public static Path getModuleBasePath(final Path repoConfigFilePath, final Module module, final boolean configHasMultipleModules) {
        final Project project = module.getProject();
        final Configuration configuration = project.getConfiguration();
        //TODO SS: review this if it still needed for initial esv conversion
        if (Files.isDirectory(repoConfigFilePath)) {
            if (configHasMultipleModules) {
                return repoConfigFilePath.resolve(REPO_CONFIG_FOLDER).resolve(configuration.getName()).resolve(project.getName()).resolve(module.getName());
            } else {
                return repoConfigFilePath.resolve(REPO_CONFIG_FOLDER);
            }
        }
        if (configHasMultipleModules) {
            return repoConfigFilePath.resolveSibling(REPO_CONFIG_FOLDER).resolve(configuration.getName()).resolve(project.getName()).resolve(module.getName());
        } else {
            return repoConfigFilePath.resolveSibling(REPO_CONFIG_FOLDER);
        }
    }

    public static Path getModuleContentBasePath(final Path repoConfigPath, final Module module, final boolean configHasMultipleModules) {
        final Project project = module.getProject();
        final Configuration configuration = project.getConfiguration();
        if (Files.isDirectory(repoConfigPath)) {
            if (configHasMultipleModules) {
                return repoConfigPath.resolve(REPO_CONTENT_FOLDER).resolve(configuration.getName()).resolve(project.getName()).resolve(module.getName());
            } else {
                return repoConfigPath.resolve(REPO_CONTENT_FOLDER);
            }
        }
        if (configHasMultipleModules) {
            return repoConfigPath.resolveSibling(REPO_CONTENT_FOLDER).resolve(configuration.getName()).resolve(project.getName()).resolve(module.getName());
        } else {
            return repoConfigPath.resolveSibling(REPO_CONTENT_FOLDER);
        }
    }

    public static Path getResourcePath(final Path modulePath, final Source source, final String resourcePath) {
        if (resourcePath.startsWith("/")) {
            return modulePath.resolve(StringUtils.stripStart(resourcePath, "/"));
        } else {
            return modulePath.resolve(source.getPath()).getParent().resolve(resourcePath);
        }
    }

    public static boolean hasMultipleModules(Map<String, Configuration> configurations) {
        return configurations.values().stream().flatMap(p -> p.getProjects().stream()).mapToInt(p -> p.getModules().size()).sum() > 1;
    }
}
