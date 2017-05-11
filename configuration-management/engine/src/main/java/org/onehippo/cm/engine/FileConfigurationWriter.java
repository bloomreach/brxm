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

import org.apache.commons.io.IOUtils;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.engine.serializer.ContentSourceSerializer;
import org.onehippo.cm.engine.serializer.RepoConfigSerializer;
import org.onehippo.cm.engine.serializer.SourceSerializer;
import org.onehippo.cm.impl.model.ConfigSourceImpl;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.yaml.snakeyaml.nodes.Node;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileConfigurationWriter {

    void write(final Path destination,
               final Map<String, ConfigurationImpl> configurations,
               final Map<Module, ModuleContext> moduleContextMap,
               final boolean explicitSequencing) throws IOException {
        final RepoConfigSerializer repoConfigSerializer = new RepoConfigSerializer(explicitSequencing);
        final Path repoConfigPath = destination.resolve(Constants.REPO_CONFIG_YAML);

        try (final OutputStream repoConfigOutputStream = new FileOutputStream(repoConfigPath.toFile())) {
            repoConfigSerializer.serialize(repoConfigOutputStream, configurations);
        }

        for (ConfigurationImpl configuration : configurations.values()) {
            for (Project project : configuration.getProjects()) {
                for (Module module : project.getModules()) {
                    final ModuleContext moduleContext = moduleContextMap.get(module);
                    moduleContext.createOutputProviders(repoConfigPath);
                    writeModule(module, explicitSequencing, moduleContext);
                }
            }
        }
    }

    public void writeModule(final Module module, final boolean explicitSequencing,
                            final ModuleContext moduleContext) throws IOException {

        moduleContext.addExistingFilesToKnownList();

        for (final Source source : module.getSources()) {

            final SourceSerializer sourceSerializer;

            if (source instanceof ConfigSourceImpl) {
                sourceSerializer = new SourceSerializer(moduleContext, source, explicitSequencing);
            } else {
                sourceSerializer = new ContentSourceSerializer(moduleContext, source, explicitSequencing);
            }

            final List<PostProcessItem> resources = new ArrayList<>();
            final Node node = sourceSerializer.representSource(resources::add);

            try (final OutputStream sourceOutputStream = getSourceOutputStream(moduleContext, source)) {
                sourceSerializer.serializeNode(sourceOutputStream, node);
            }

            for (final PostProcessItem resource : resources) {
                if (resource instanceof CopyItem) {
                    processCopyItem(source, (CopyItem) resource, moduleContext);
                } else if (resource instanceof BinaryItem) {
                    processBinaryItem(source, (BinaryItem) resource, moduleContext);
                }
            }
        }
    }

    private OutputStream getSourceOutputStream(final ModuleContext moduleContext, final Source source) throws IOException {
        final Path modulePath = ((FileResourceOutputProvider)moduleContext.getOutputProvider(source)).getModulePath();
        final Path sourceDestPath = modulePath.resolve(source.getPath());
        Files.createDirectories(sourceDestPath.getParent());
        return new FileOutputStream(sourceDestPath.toFile());
    }

    private void processBinaryItem(Source source, BinaryItem binaryItem, ModuleContext moduleContext) throws IOException {

        final String finalName = binaryItem.getNode().getValue();
        final byte[] content = (byte[]) binaryItem.getValue().getObject();

        try (final OutputStream resourceOutputStream = moduleContext.getOutputProvider(source).getResourceOutputStream(source, finalName);
             final ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            IOUtils.copy(inputStream, resourceOutputStream);
        }
    }

    private void processCopyItem(Source source, CopyItem copyItem, ModuleContext moduleContext) throws IOException {
        try (
                final InputStream resourceInputStream = moduleContext.getInputProvider(source)
                        .getResourceInputStream(source, copyItem.getSourceLocation());
                final OutputStream resourceOutputStream = moduleContext.getOutputProvider(source)
                        .getResourceOutputStream(source, copyItem.getSourceLocation())
        ) {
            IOUtils.copy(resourceInputStream, resourceOutputStream);
        }
    }
}
