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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Source;

public class FileConfigurationWriter {

    private static class FileResourceOutputProvider implements ResourceOutputProvider {
        private final Path modulePath;

        private FileResourceOutputProvider(final Path modulePath) {
            this.modulePath = modulePath;
        }

        @Override
        public OutputStream getResourceOutputStream(final Source source, final String resourcePath) throws IOException {
            final Path path = FileConfigurationUtils.getResourcePath(modulePath, source, resourcePath);
            Files.createDirectories(path.getParent());
            return new FileOutputStream(path.toFile());
        }
    }

    public void write(final Path destination,
                      final Map<String, Configuration> configurations,
                      final Map<Module, ResourceInputProvider> resourceInputProviders) throws IOException {
        final RepoConfigSerializer repoConfigSerializer = new RepoConfigSerializer();
        final SourceSerializer sourceSerializer = new SourceSerializer();
        final Path repoConfigPath = destination.resolve("repo-config.yaml");

        try (final OutputStream repoConfigOutputStream = new FileOutputStream(repoConfigPath.toFile())) {
            repoConfigSerializer.serialize(repoConfigOutputStream, configurations);
        }

        final boolean hasMultipleModules = FileConfigurationUtils.hasMultipleModules(configurations);

        for (Configuration configuration : configurations.values()) {
            for (Project project : configuration.getProjects()) {
                for (Module module : project.getModules()) {
                    final Path modulePath =
                            FileConfigurationUtils.getModuleBasePath(repoConfigPath, module, hasMultipleModules);
                    final ResourceInputProvider resourceInputProvider = resourceInputProviders.get(module);
                    final ResourceOutputProvider resourceOutputProvider = new FileResourceOutputProvider(modulePath);

                    for (Source source : module.getSources()) {
                        final List<String> resources = new ArrayList<>();
                        try (final OutputStream sourceOutputStream = getSourceOutputStream(modulePath, source)) {
                            sourceSerializer.serialize(sourceOutputStream, source, resources::add);
                        }

                        for (String resource : resources) {
                            try (
                                    final InputStream resourceInputStream =
                                            resourceInputProvider.getResourceInputStream(source, resource);
                                    final OutputStream resourceOutputStream =
                                            resourceOutputProvider.getResourceOutputStream(source, resource)
                            ) {
                                IOUtils.copy(resourceInputStream, resourceOutputStream);
                            }
                        }
                    }
                }
            }
        }
    }

    private OutputStream getSourceOutputStream(final Path modulePath, final Source source) throws IOException {
        final Path sourceDestPath = modulePath.resolve(source.getPath());
        Files.createDirectories(sourceDestPath.getParent());
        return new FileOutputStream(sourceDestPath.toFile());
    }

}
