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
package org.onehippo.cm.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import org.apache.commons.lang3.tuple.Pair;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.parser.ActionListParser;
import org.onehippo.cm.model.parser.ConfigSourceParser;
import org.onehippo.cm.model.parser.ContentSourceParser;
import org.onehippo.cm.model.parser.AggregatedModulesDescriptorParser;
import org.onehippo.cm.model.parser.ParserException;
import org.onehippo.cm.model.parser.SourceParser;
import org.onehippo.cm.model.util.FileConfigurationUtils;

import com.google.common.collect.Maps;

public class PathConfigurationReader {

    private final boolean explicitSequencing;

    public static class ReadResult {
        private final Collection<GroupImpl> groups;
        private final Map<Module, ModuleContext> moduleContexts;

        public ReadResult(Collection<GroupImpl> groups, Map<Module, ModuleContext> moduleContexts) {
            this.groups = groups;
            this.moduleContexts = moduleContexts;
        }

        // note: this method is currently only used for testing purposes
        public Map<String, GroupImpl> getGroupsMap() {
            return Maps.uniqueIndex(groups, GroupImpl::getName);
        }

        public Collection<GroupImpl> getGroups() {
            return groups;
        }

        public Map<Module, ModuleContext> getModuleContexts() {
            return moduleContexts;
        }
    }

    public PathConfigurationReader() {
        this(Constants.DEFAULT_EXPLICIT_SEQUENCING);
    }

    public PathConfigurationReader(final boolean explicitSequencing) {
        this.explicitSequencing = explicitSequencing;
    }

    public ReadResult read(final Path moduleDescriptorPath) throws IOException, ParserException {
        return read(moduleDescriptorPath, false);
    }

    /**
     * Read the module dependency data to extract the raw components of a configuration model.
     * These raw portions of config will be assembled into a ConfigurationModel later.
     *
     * @param moduleDescriptorPath
     * @param verifyOnly
     * @return
     * @throws IOException
     * @throws ParserException
     */
    public ReadResult read(final Path moduleDescriptorPath, final boolean verifyOnly) throws IOException, ParserException {
        final InputStream moduleDescriptorInputStream = moduleDescriptorPath.toUri().toURL().openStream();

        // todo switch to single-module alternate parser
        final AggregatedModulesDescriptorParser moduleDescriptorParser = new AggregatedModulesDescriptorParser(explicitSequencing);
        final Collection<GroupImpl> groups = moduleDescriptorParser
                .parse(moduleDescriptorInputStream, moduleDescriptorPath.toAbsolutePath().toString());

        final boolean hasMultipleModules = FileConfigurationUtils.hasMultipleModules(groups);
        final Map<Module, ModuleContext> moduleContexts = new HashMap<>();

        for (GroupImpl group : groups) {
            for (ProjectImpl project : group.getProjects()) {
                for (ModuleImpl module : project.getModules()) {

                    final ModuleContext moduleContext = new ModuleContext(module, moduleDescriptorPath, hasMultipleModules);
                    moduleContexts.put(module, moduleContext);

                    // Set the input providers on the Module directly, so it doesn't need to be held in a Map on ConfigurationModel
                    readModule(module, moduleContext, verifyOnly);
                }
            }
        }

        return new ReadResult(groups, moduleContexts);
    }

    /**
     * Reads the single module config/content definitions
     * @param module
     * @param moduleContext
     * @param verifyOnly
     * @throws IOException
     * @throws ParserException
     */
    public void readModule(final ModuleImpl module, final ModuleContext moduleContext, final boolean verifyOnly) throws IOException, ParserException {
        module.setConfigResourceInputProvider(moduleContext.getConfigInputProvider());
        module.setContentResourceInputProvider(moduleContext.getContentInputProvider());
        processConfigSources(verifyOnly, module, moduleContext);
        processContentSources(verifyOnly, module, moduleContext);
        processActionsList(module, moduleContext);
    }

    private void processActionsList(final ModuleImpl module, final ModuleContext moduleContext) throws ParserException, IOException {
        final Path actionsDescriptorPath = moduleContext.getActionsDescriptorPath();
        if (Files.exists(actionsDescriptorPath)) {
            final ActionListParser parser = new ActionListParser();
            parser.parse(actionsDescriptorPath.toUri().toURL().openStream(), actionsDescriptorPath.toString(), module);
        }
    }

    private void processContentSources(final boolean verifyOnly, final ModuleImpl module, final ModuleContext moduleContext) throws IOException, ParserException {
        final Path contentRootPath = moduleContext.getContentRoot();
        if (Files.exists(contentRootPath)) {
            final SourceParser contentSourceParser = new ContentSourceParser(moduleContext.getContentInputProvider(), verifyOnly, explicitSequencing);
            parseSources(module, contentRootPath, contentSourceParser);
        }
    }

    private void processConfigSources(final boolean verifyOnly, final ModuleImpl module, final ModuleContext moduleContext) throws IOException, ParserException {
        final Path configRootPath = moduleContext.getConfigRoot();
        if (Files.exists(configRootPath)) {
            final SourceParser configSourceParser = new ConfigSourceParser(moduleContext.getConfigInputProvider(), verifyOnly, explicitSequencing);
            parseSources(module, configRootPath, configSourceParser);
        }
    }

    private void parseSources(ModuleImpl module, Path rootPath, SourceParser sourceParser) throws IOException, ParserException {
        final List<Pair<Path, String>> contentSourceData = getSourceData(rootPath);
        for (Pair<Path, String> pair : contentSourceData) {
            sourceParser.parse(pair.getLeft().toUri().toURL().openStream(), pair.getRight(), rootPath.resolve(pair.getRight()).toString(), module);
        }
    }

    //TODO use to collect all content sources and then sort them in processing order appropriately.
    private List<Pair<Path, String>> getSourceData(final Path modulePath) throws IOException {
        final List<Path> paths = new ArrayList<>();
        final BiPredicate<Path, BasicFileAttributes> matcher =
                (filePath, fileAttr) -> filePath.toString().toLowerCase().endsWith(Constants.YAML_EXT) && fileAttr.isRegularFile();
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
