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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import org.apache.commons.lang3.tuple.Pair;
import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.impl.model.ModuleImpl;

import static org.onehippo.cm.engine.Constants.DEFAULT_EXPLICIT_SEQUENCING;

public class PathConfigurationReader {

    private final boolean explicitSequencing;

    public static class ReadResult {
        private final Map<String, Configuration> configurations;
        private final Map<Module, ResourceInputProvider> resourceInputProviders;
        public ReadResult(Map<String, Configuration> configurations, Map<Module, ResourceInputProvider> resourceInputProviders) {
            this.configurations = configurations;
            this.resourceInputProviders = resourceInputProviders;
        }
        public Map<String, Configuration> getConfigurations() {
            return configurations;
        }
        public Map<Module, ResourceInputProvider> getResourceInputProviders() {
            return resourceInputProviders;
        }
    }

    public PathConfigurationReader() {
        this(DEFAULT_EXPLICIT_SEQUENCING);
    }

    public PathConfigurationReader(final boolean explicitSequencing) {
        this.explicitSequencing = explicitSequencing;
    }

    public ReadResult read(final Path repoConfigPath) throws IOException, ParserException {
        return read(repoConfigPath, false);
    }

    public ReadResult read(final Path repoConfigPath, final boolean verifyOnly) throws IOException, ParserException {
        final RepoConfigParser parser = new RepoConfigParser(explicitSequencing);
        final Map<String, Configuration> configurations =
                parser.parse(repoConfigPath.toUri().toURL().openStream(), repoConfigPath.toAbsolutePath().toString());
        final boolean hasMultipleModules = FileConfigurationUtils.hasMultipleModules(configurations);
        final Map<Module, ResourceInputProvider> resourceDataProviders = new HashMap<>();

        for (Configuration configuration : configurations.values()) {
            for (Project project : configuration.getProjects()) {
                for (Module module : project.getModules()) {
                    final Path moduleRootPath =
                            FileConfigurationUtils.getModuleBasePath(repoConfigPath, module, hasMultipleModules);
                    final ResourceInputProvider provider = new FileResourceInputProvider(moduleRootPath);
                    resourceDataProviders.put(module, provider);
                    final SourceParser sourceParser = new SourceParser(provider, verifyOnly, explicitSequencing);

                    for (Pair<Path, String> pair : getSourceData(moduleRootPath)) {
                        sourceParser.parse(pair.getLeft().toUri().toURL().openStream(), pair.getRight(), moduleRootPath.resolve(pair.getRight()).toString(), (ModuleImpl) module);
                    }
                }
            }
        }

        return new ReadResult(configurations, resourceDataProviders);
    }

    private List<Pair<Path, String>> getSourceData(final Path modulePath) throws IOException {
        final List<Path> paths = new ArrayList<>();
        final BiPredicate<Path, BasicFileAttributes> matcher =
                (filePath, fileAttr) -> filePath.toString().toLowerCase().endsWith("yaml") && fileAttr.isRegularFile();
        Files.find(modulePath, Integer.MAX_VALUE, matcher).forEachOrdered(paths::add);
        final int modulePathSize = modulePath.getNameCount();

        final List<Pair<Path, String>> result = new ArrayList<>();
        for (Path path : paths) {
            final Path sourcePath = path.subpath(modulePathSize, path.getNameCount());
            result.add(Pair.of(path, sourcePath.toString()));
        }
        return result;
    }

}
