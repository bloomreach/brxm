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
import org.onehippo.cm.api.model.Group;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.impl.model.GroupImpl;

import static org.onehippo.cm.engine.Constants.HCM_CONFIG_FOLDER;
import static org.onehippo.cm.engine.Constants.HCM_CONTENT_FOLDER;

public class FileConfigurationUtils {

    public static Path getModuleBasePath(final Path moduleDescriptorPath, final Module module, final boolean configHasMultipleModules) {
        final Project project = module.getProject();
        final Group group = project.getGroup();
        //TODO SS: review this if it still needed for initial esv conversion
        if (Files.isDirectory(moduleDescriptorPath)) {
            if (configHasMultipleModules) {
                return moduleDescriptorPath.resolve(HCM_CONFIG_FOLDER).resolve(group.getName()).resolve(project.getName()).resolve(module.getName());
            } else {
                return moduleDescriptorPath.resolve(HCM_CONFIG_FOLDER);
            }
        }
        if (configHasMultipleModules) {
            return moduleDescriptorPath.resolveSibling(HCM_CONFIG_FOLDER).resolve(group.getName()).resolve(project.getName()).resolve(module.getName());
        } else {
            return moduleDescriptorPath.resolveSibling(HCM_CONFIG_FOLDER);
        }
    }

    public static Path getModuleContentBasePath(final Path moduleDescriptorPath, final Module module, final boolean configHasMultipleModules) {
        final Project project = module.getProject();
        final Group group = project.getGroup();
        if (Files.isDirectory(moduleDescriptorPath)) {
            if (configHasMultipleModules) {
                return moduleDescriptorPath.resolve(HCM_CONTENT_FOLDER).resolve(group.getName()).resolve(project.getName()).resolve(module.getName());
            } else {
                return moduleDescriptorPath.resolve(HCM_CONTENT_FOLDER);
            }
        }
        if (configHasMultipleModules) {
            return moduleDescriptorPath.resolveSibling(HCM_CONTENT_FOLDER).resolve(group.getName()).resolve(project.getName()).resolve(module.getName());
        } else {
            return moduleDescriptorPath.resolveSibling(HCM_CONTENT_FOLDER);
        }
    }

    public static Path getResourcePath(final Path basePath, final Source source, final String resourcePath) {
        if (resourcePath.startsWith("/")) {
            return basePath.resolve(StringUtils.stripStart(resourcePath, "/"));
        } else {
            return basePath.resolve(source.getPath()).getParent().resolve(resourcePath);
        }
    }

    public static boolean hasMultipleModules(Map<String, GroupImpl> configurations) {
        return configurations.values().stream().flatMap(p -> p.getProjects().stream()).mapToInt(p -> p.getModules().size()).sum() > 1;
    }
}
