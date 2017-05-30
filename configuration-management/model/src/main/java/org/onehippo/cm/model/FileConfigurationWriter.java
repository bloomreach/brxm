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

import org.apache.commons.io.IOUtils;
import org.onehippo.cm.model.serializer.ContentSourceSerializer;
import org.onehippo.cm.model.serializer.ModuleDescriptorSerializer;
import org.onehippo.cm.model.serializer.SourceSerializer;
import org.yaml.snakeyaml.nodes.Node;

public class FileConfigurationWriter {

    void write(final Path destination,
               final Map<String, ? extends Group> groups,
               final Map<Module, ModuleContext> moduleContextMap,
               final boolean explicitSequencing) throws IOException {
        final ModuleDescriptorSerializer moduleDescriptorSerializer = new ModuleDescriptorSerializer(explicitSequencing);
        final Path moduleDescriptorPath = destination.resolve(Constants.HCM_MODULE_YAML);

        try (final OutputStream moduleDescriptorOutputStream = new FileOutputStream(moduleDescriptorPath.toFile())) {
            moduleDescriptorSerializer.serialize(moduleDescriptorOutputStream, groups);
        }

        for (Group group : groups.values()) {
            for (Project project : group.getProjects()) {
                for (Module module : project.getModules()) {
                    final ModuleContext moduleContext = moduleContextMap.get(module);
                    moduleContext.createOutputProviders(moduleDescriptorPath);
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

            if (SourceType.CONFIG == source.getType()) {
                sourceSerializer = new SourceSerializer(moduleContext, source, explicitSequencing);
            } else /* SourceType.CONTENT */ {
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
        final InputStream inputStream;
        if (binaryItem.getValue() instanceof JcrBinaryValueImpl) {
            inputStream = binaryItem.getValue().getResourceInputStream();
        } else {
            final byte[] content = (byte[]) binaryItem.getValue().getObject();
            inputStream = new ByteArrayInputStream(content);
        }

        try (final OutputStream resourceOutputStream = moduleContext.getOutputProvider(source).getResourceOutputStream(source, finalName)) {
            IOUtils.copy(inputStream, resourceOutputStream);
        }

        IOUtils.closeQuietly(inputStream);
    }

    private void processCopyItem(Source source, CopyItem copyItem, ModuleContext moduleContext) throws IOException {
        try (
                final InputStream resourceInputStream = copyItem.getValue().getResourceInputStream();
                final OutputStream resourceOutputStream = moduleContext.getOutputProvider(source)
                        .getResourceOutputStream(source, copyItem.getValue().getString())
        ) {
            IOUtils.copy(resourceInputStream, resourceOutputStream);
        }
    }
}
