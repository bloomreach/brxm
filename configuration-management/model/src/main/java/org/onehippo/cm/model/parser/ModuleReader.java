/*
 *  Copyright 2017-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.parser;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.onehippo.cm.model.Constants;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.serializer.ModuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cm.model.util.FilePathUtils.unixPath;

public class ModuleReader {

    private static final Logger log = LoggerFactory.getLogger(ModuleReader.class);

    private final boolean explicitSequencing;
    private final boolean contentSourceHeadOnly;

    public ModuleReader() {
        this(Constants.DEFAULT_EXPLICIT_SEQUENCING);
    }

    public ModuleReader(final boolean explicitSequencing) {
        this(explicitSequencing, false);
    }

    public ModuleReader(final boolean explicitSequencing, final boolean contentSourceHeadOnly) {
        this.explicitSequencing = explicitSequencing;
        this.contentSourceHeadOnly = contentSourceHeadOnly;
    }

    protected boolean isExplicitSequencing() {
        return explicitSequencing;
    }

    protected boolean isContentSourceHeadOnly() {
        return contentSourceHeadOnly;
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
    public ModuleContext read(final Path moduleDescriptorPath, final boolean verifyOnly) throws IOException, ParserException {
        return read(moduleDescriptorPath, verifyOnly, null, null);
    }

    public ModuleContext read(final Path moduleDescriptorPath, final boolean verifyOnly,
                           final String siteName, final JcrPath hstRoot) throws IOException, ParserException {
        final ModuleImpl module = readDescriptor(moduleDescriptorPath, siteName);

        module.setHstRoot(hstRoot);

        final ModuleContext moduleContext = new ModuleContext(module, moduleDescriptorPath);
        readModule(module, moduleContext, verifyOnly);
        return moduleContext;
    }

    public ModuleContext readReplacement(final Path moduleDescriptorPath, final ConfigurationModelImpl comparisonModel,
                                         final String siteName) throws IOException, ParserException {
        final ModuleImpl module = readDescriptor(moduleDescriptorPath, siteName);

        // if there was already a matching module in the comparisonModule, use the hst root info from that one
        final Optional<ModuleImpl> existingModule = comparisonModel.getModulesStream().filter(Predicate.isEqual(module)).findFirst();
        if (siteName != null) {
            existingModule.ifPresent(comparisonModule -> {
                module.setHstRoot(comparisonModule.getHstRoot());
            });
        }

        final ModuleContext moduleContext = new ModuleContext(module, moduleDescriptorPath);
        readModule(module, moduleContext, true);
        return moduleContext;
    }

    ModuleImpl readDescriptor(final Path moduleDescriptorPath, final String siteName) throws IOException, ParserException {
        try (InputStream inputStream = Files.newInputStream(moduleDescriptorPath.toRealPath());
             final InputStream moduleDescriptorInputStream = new BufferedInputStream(inputStream)) {

            final ModuleDescriptorParser moduleDescriptorParser = new ModuleDescriptorParser(explicitSequencing);

            try {
                return moduleDescriptorParser.parse(moduleDescriptorInputStream, moduleDescriptorPath.toAbsolutePath().toString(), siteName);
            } catch (ParserException e) {
                log.error("Error while parsing module descriptor {} for site: {}",
                        moduleDescriptorPath.toUri().toString(), siteName);
                e.setSource(moduleDescriptorPath.toUri().toString());
                throw e;
            }
        }
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
        log.debug("Reading module: {}", module.getFullName());
        // Set the input providers on the Module directly, so it doesn't need to be held in a Map on ConfigurationModel
        module.setConfigResourceInputProvider(moduleContext.getConfigInputProvider());
        module.setContentResourceInputProvider(moduleContext.getContentInputProvider());
        processConfigSources(verifyOnly, module, moduleContext);
        processContentSources(verifyOnly, module, moduleContext);
        processActionsList(module, moduleContext);
    }

    protected void processActionsList(final ModuleImpl module, final ModuleContext moduleContext) throws ParserException, IOException {
        final Path actionsDescriptorPath = moduleContext.getActionsDescriptorPath();
        if (Files.exists(actionsDescriptorPath)) {
            final ActionListParser parser = new ActionListParser();
            try(InputStream inputStream = Files.newInputStream(actionsDescriptorPath)) {
                parser.parse(inputStream, actionsDescriptorPath.toString(), module);
            }
        }
    }

    protected SourceParser getConfigSourceParser(final ModuleContext moduleContext, boolean verifyOnly) {
        return new ConfigSourceParser(moduleContext.getConfigInputProvider(), verifyOnly, isExplicitSequencing());
    }

    protected SourceParser getContentSourceParser(final ModuleContext moduleContext, boolean verifyOnly) {
        return isContentSourceHeadOnly()
                ? new ContentSourceHeadParser(moduleContext.getContentInputProvider(), verifyOnly, isExplicitSequencing())
                : new ContentSourceParser(moduleContext.getContentInputProvider(), verifyOnly, isExplicitSequencing());
    }

    protected void processContentSources(final boolean verifyOnly, final ModuleImpl module, final ModuleContext moduleContext) throws IOException, ParserException {
        final Path contentBasePath = moduleContext.getContentRoot();
        if (Files.exists(contentBasePath)) {
            final SourceParser contentSourceParser = getContentSourceParser(moduleContext, verifyOnly);
            parseSources(module, contentBasePath, contentSourceParser);
        }
    }

    protected void processConfigSources(final boolean verifyOnly, final ModuleImpl module, final ModuleContext moduleContext) throws IOException, ParserException {
        final Path configBasePath = moduleContext.getConfigRoot();
        if (Files.exists(configBasePath)) {
            final SourceParser configSourceParser = getConfigSourceParser(moduleContext, verifyOnly);
            parseSources(module, configBasePath, configSourceParser);
        }
    }

    protected void parseSources(ModuleImpl module, Path basePath, SourceParser sourceParser) throws IOException, ParserException {
        final List<Pair<Path, String>> contentSourceData = getSourceData(basePath);
        for (Pair<Path, String> pair : contentSourceData) {
            try {
                try (final InputStream sourceInputStream = Files.newInputStream(pair.getLeft())) {
                    sourceParser.parse(new BufferedInputStream(sourceInputStream), pair.getRight(), pair.getLeft().toString(), module);
                }
            } catch (ParserException e) {
                log.error("Error while parsing source: {}:{}", module.getFullName(), pair.getLeft().toString());
                // add information about the module and source to the parser exception
                e.setSource("[" + module.getFullName() + ": " + pair.getRight() + "]");
                throw(e);
            }
        }
    }

    // used to collect all sources and then sort them in processing order appropriately.
    protected List<Pair<Path, String>> getSourceData(final Path basePath) throws IOException {
        final List<Path> paths = new ArrayList<>();
        final BiPredicate<Path, BasicFileAttributes> matcher =
                (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.getFileName().toString().toLowerCase().endsWith(Constants.YAML_EXT);
        try(Stream<Path> pathStream = Files.find(basePath, Integer.MAX_VALUE, matcher)) {
            pathStream.forEachOrdered(paths::add);
        }
        final int modulePathSize = basePath.getNameCount();

        final List<Pair<Path, String>> result = new ArrayList<>();
        for (Path path : paths) {
            final Path sourcePath = path.subpath(modulePathSize, path.getNameCount());
            result.add(Pair.of(path, unixPath(sourcePath.toString())));
        }
        return result;
    }

}
