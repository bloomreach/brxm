/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cm.model.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.model.Constants;
import org.onehippo.cm.model.Group;
import org.onehippo.cm.model.Module;
import org.onehippo.cm.model.Project;
import org.onehippo.cm.model.Source;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.serializer.ResourceNameResolverImpl;

public class FileConfigurationUtils {

    public static Path getModuleBasePath(final Path moduleDescriptorPath, final Module module, final boolean configHasMultipleModules) {
        final Project project = module.getProject();
        final Group group = project.getGroup();
        //TODO SS: review this if it still needed for initial esv conversion
        if (Files.isDirectory(moduleDescriptorPath)) {
            if (configHasMultipleModules) {
                return moduleDescriptorPath.resolve(Constants.HCM_CONFIG_FOLDER).resolve(group.getName()).resolve(project.getName()).resolve(module.getName());
            } else {
                return moduleDescriptorPath.resolve(Constants.HCM_CONFIG_FOLDER);
            }
        }
        if (configHasMultipleModules) {
            return moduleDescriptorPath.resolveSibling(Constants.HCM_CONFIG_FOLDER).resolve(group.getName()).resolve(project.getName()).resolve(module.getName());
        } else {
            return moduleDescriptorPath.resolveSibling(Constants.HCM_CONFIG_FOLDER);
        }
    }

    public static Path getModuleContentBasePath(final Path moduleDescriptorPath, final Module module, final boolean configHasMultipleModules) {
        final Project project = module.getProject();
        final Group group = project.getGroup();
        if (Files.isDirectory(moduleDescriptorPath)) {
            if (configHasMultipleModules) {
                return moduleDescriptorPath.resolve(Constants.HCM_CONTENT_FOLDER).resolve(group.getName()).resolve(project.getName()).resolve(module.getName());
            } else {
                return moduleDescriptorPath.resolve(Constants.HCM_CONTENT_FOLDER);
            }
        }
        if (configHasMultipleModules) {
            return moduleDescriptorPath.resolveSibling(Constants.HCM_CONTENT_FOLDER).resolve(group.getName()).resolve(project.getName()).resolve(module.getName());
        } else {
            return moduleDescriptorPath.resolveSibling(Constants.HCM_CONTENT_FOLDER);
        }
    }

    public static Path getResourcePath(final Path basePath, final Source source, final String resourcePath) {
        if (resourcePath.startsWith("/")) {
            return basePath.resolve(StringUtils.stripStart(resourcePath, "/"));
        } else {
            return basePath.resolve(source.getPath()).getParent().resolve(resourcePath);
        }
    }

    public static boolean hasMultipleModules(Map<String, GroupImpl> groups) {
        return groups.values().stream().flatMap(p -> p.getProjects().stream()).mapToInt(p -> p.getModules().size()).sum() > 1;
    }

    /**
     * Use a simple numbering convention to generate a unique path given a candidate path and a test of uniqueness.
     * TODO move to a utilities class?
     * @param candidate a path that might be used directly, if it is already unique, or modified to create a unique path
     * @param isNotUnique a test of uniqueness for a proposed path
     * @param sequence the number of the current attempt, used for loop control -- callers typically pass 0
     * @return a path, based on candidate, that is unique according to isNotUnique
     */
    public static String generateUniquePath(String candidate, Predicate<String> isNotUnique, int sequence) {

        String name = StringUtils.substringBeforeLast(candidate, ResourceNameResolverImpl.SEPARATOR);
        String extension = StringUtils.substringAfterLast(candidate, ResourceNameResolverImpl.SEPARATOR);

        final String newName = name + calculateNameSuffix(sequence) + (!StringUtils.isEmpty(extension) ? ResourceNameResolverImpl.SEPARATOR + extension : StringUtils.EMPTY);
        return isNotUnique.test(newName) ? generateUniquePath(candidate, isNotUnique, sequence + 1) : newName;
    }

    /**
     * Helper for {@link #generateUniquePath(String, Predicate, int)}.
     */
    private static String calculateNameSuffix(int sequence) {
        return sequence == 0 ? StringUtils.EMPTY : ResourceNameResolverImpl.SEQ_PREFIX + Integer.toString(sequence);
    }
}
