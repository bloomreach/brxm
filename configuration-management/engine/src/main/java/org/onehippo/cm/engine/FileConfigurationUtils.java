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

import java.nio.file.Path;
import java.util.Map;

import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;

public class FileConfigurationUtils {

    public static Path getModuleBasePath(final Path repoConfigPath, final Module module, final boolean configHasMultipleModules) {
        final Project project = module.getProject();
        final Configuration configuration = project.getConfiguration();
        if (configHasMultipleModules) {
            return repoConfigPath.getParent().resolve("repo-config").resolve(configuration.getName()).resolve(project.getName()).resolve(module.getName());
        } else {
            return repoConfigPath.getParent().resolve("repo-config");
        }
    }

    public static boolean hasMultipleModules(Map<String, Configuration> configurations) {
        int count = 0;
        for (Configuration configuration : configurations.values()) {
            for (Project project : configuration.getProjects().values()) {
                count += project.getModules().size();
            }
        }
        return count > 1;
    }

}
