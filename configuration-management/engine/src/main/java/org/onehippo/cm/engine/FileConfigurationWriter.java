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

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.engine.snakeyaml.MutableScalarNode;
import org.yaml.snakeyaml.nodes.Node;

import static org.onehippo.cm.engine.Constants.DEFAULT_EXPLICIT_SEQUENCING;

public class FileConfigurationWriter {

    private static final String PATH_DELIMITER = "/";
    private static final String NS_DELIMITER = ":";
    private static final String EXT_BINARY = ".bin";
    private final boolean explicitSequencing;

    public FileConfigurationWriter() {
        this(DEFAULT_EXPLICIT_SEQUENCING);
    }

    public FileConfigurationWriter(final boolean explicitSequencing) {
        this.explicitSequencing = explicitSequencing;
    }

    public void write(final Path destination,
                      final Map<String, Configuration> configurations,
                      final Map<Module, ResourceInputProvider> resourceInputProviders) throws IOException {
        final RepoConfigSerializer repoConfigSerializer = new RepoConfigSerializer(explicitSequencing);
        final SourceSerializer sourceSerializer = new SourceSerializer(explicitSequencing);
        final Path repoConfigPath = destination.resolve(Constants.REPO_CONFIG_YAML);

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

                    writeModule(module, modulePath, sourceSerializer, resourceInputProvider, resourceOutputProvider);
                }
            }
        }
    }

    public void writeModule(final Module module, final Path modulePath, final SourceSerializer sourceSerializer,
                            final ResourceInputProvider resourceInputProvider,
                            final ResourceOutputProvider resourceOutputProvider) throws IOException {

        final ResourceNameResolver moduleNameResolver = new ResourceNameResolverImpl();

        for (final Source source : module.getSources()) {
            final List<PostProcessItem> resources = new ArrayList<>();

                final Node node = sourceSerializer.representSource(source, resources::add);

                for (final PostProcessItem resource : resources) {
                    if (resource instanceof CopyItem) {
                        processCopyItem(resourceInputProvider, resourceOutputProvider, source, (CopyItem) resource);
                    } else if (resource instanceof BinaryItem) {
                        processBinaryItem(source, resourceOutputProvider, (BinaryItem) resource, moduleNameResolver);
                    } else if (resource instanceof BinaryArrayItem) {
                        processBinaryArray(source, resourceOutputProvider, (BinaryArrayItem) resource, moduleNameResolver);
                    }
                }

            try (final OutputStream sourceOutputStream = getSourceOutputStream(modulePath, source)) {
                sourceSerializer.serializeNode(sourceOutputStream, node);
            }

        }
    }

    private void processBinaryArray(Source source, ResourceOutputProvider resourceOutputProvider, BinaryArrayItem resource, ResourceNameResolver nameResolver) throws IOException {

        List<BinaryItem> items = resource.getItems();
        for (int i = 0; i < items.size(); i++) {
            BinaryItem binaryItem = items.get(i);
            processBinaryItem(source, resourceOutputProvider, binaryItem, nameResolver, true, i);
        }
    }

    private void processBinaryItem(Source source, ResourceOutputProvider resourceOutputProvider,
                                   BinaryItem binaryItem, ResourceNameResolver nameResolver) throws IOException {
        processBinaryItem(source, resourceOutputProvider, binaryItem, nameResolver, false, 0);
    }

    private void processBinaryItem(Source source, ResourceOutputProvider resourceOutputProvider,
                                   BinaryItem binaryItem, ResourceNameResolver nameResolver, boolean isArrayItem, int arrIndex) throws IOException {

        String filePath = constructFilePath(binaryItem, isArrayItem, arrIndex);

        final String finalName = nameResolver.generateName(filePath);
        final String fileName = finalName + EXT_BINARY;

        final MutableScalarNode node = binaryItem.getNode();
        node.setValue(fileName);

        final byte[] content = (byte[]) binaryItem.getValue().getObject();

        try (final OutputStream resourceOutputStream = resourceOutputProvider.getResourceOutputStream(source, fileName);
             final ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            IOUtils.copy(inputStream, resourceOutputStream);
        }
    }

    private String constructFilePath(BinaryItem binaryItem, boolean isArrayItem, int arrIndex) {
        final String propertyPath = binaryItem.getValue().getParent().getPath();
        String filePath = constructPathFromJcrPath(propertyPath);

        if (isArrayItem) {
            filePath += ResourceNameResolverImpl.SEQ_ARRAY_PREFIX + arrIndex + ResourceNameResolverImpl.SEQ_ARRAY_SUFFIX;
        }
        return filePath;
    }

    private String constructPathFromJcrPath(String jcrPath) {
        final String[] split = jcrPath.split(PATH_DELIMITER);
        return Arrays.stream(split).map(this::normalizeJcrName).collect(Collectors.joining(PATH_DELIMITER));
    }

    private String normalizeJcrName(String part) {
        return part.contains(NS_DELIMITER) ? part.substring(part.indexOf(NS_DELIMITER) + 1) : part;
    }

    private void processCopyItem(ResourceInputProvider resourceInputProvider, ResourceOutputProvider resourceOutputProvider,
                                 Source source, CopyItem copyItem) throws IOException {
        try (
                final InputStream resourceInputStream =
                        resourceInputProvider.getResourceInputStream(source, copyItem.getSourceLocation() );
                final OutputStream resourceOutputStream =
                        resourceOutputProvider.getResourceOutputStream(source, copyItem.getSourceLocation())
        ) {
            IOUtils.copy(resourceInputStream, resourceOutputStream);
        }
    }

    private OutputStream getSourceOutputStream(final Path modulePath, final Source source) throws IOException {
        final Path sourceDestPath = modulePath.resolve(source.getPath());
        Files.createDirectories(sourceDestPath.getParent());
        return new FileOutputStream(sourceDestPath.toFile());
    }
}
