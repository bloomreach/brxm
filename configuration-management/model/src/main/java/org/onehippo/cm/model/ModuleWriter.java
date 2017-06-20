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

import org.apache.commons.io.IOUtils;
import org.onehippo.cm.ResourceInputProvider;
import org.onehippo.cm.model.impl.ValueImpl;
import org.onehippo.cm.model.serializer.ContentSourceSerializer;
import org.onehippo.cm.model.serializer.ModuleDescriptorSerializer;
import org.onehippo.cm.model.serializer.SourceSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.nodes.Node;

import static org.onehippo.cm.model.Constants.DEFAULT_EXPLICIT_SEQUENCING;

public class ModuleWriter {

    private static final Logger log = LoggerFactory.getLogger(ModuleWriter.class);

    public ModuleWriter() {}

    void write(final Path destination,
               final ModuleContext moduleContext,
               final boolean explicitSequencing) throws IOException {
        final Path moduleDescriptorPath = destination.resolve(Constants.HCM_MODULE_YAML);

        final ModuleDescriptorSerializer moduleDescriptorSerializer = new ModuleDescriptorSerializer(explicitSequencing);
        try (final OutputStream moduleDescriptorOutputStream = new FileOutputStream(moduleDescriptorPath.toFile())) {
            moduleDescriptorSerializer.serialize(moduleDescriptorOutputStream, moduleContext.getModule());
        }
        moduleContext.createOutputProviders(moduleDescriptorPath);
        writeModule(moduleContext.getModule(), explicitSequencing, moduleContext);
    }

    public void writeModule(final Module module, final ModuleContext moduleContext)
            throws IOException {
        writeModule(module, DEFAULT_EXPLICIT_SEQUENCING, moduleContext);
    }

    public void writeModule(final Module module, final boolean explicitSequencing,
                            final ModuleContext moduleContext) throws IOException {

        moduleContext.collectExistingFilesAndResolveNewResources();

        for (final Source source : module.getSources()) {
            if (sourceShouldBeSkipped(source)) {
                continue;
            }

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
                }
                else if (resource instanceof BinaryItem) {
                    processBinaryItem(source, (BinaryItem) resource, moduleContext);
                }
            }
        }
    }

    boolean sourceShouldBeSkipped(final Source source) {
        return false;
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
            // todo move this logic to ValueImpl.getResourceInputStream(), so we can avoid the ugly cast above
            final byte[] content = (byte[]) binaryItem.getValue().getObject();
            inputStream = new ByteArrayInputStream(content);
        }

        try (final OutputStream resourceOutputStream = moduleContext.getOutputProvider(source).getResourceOutputStream(source, finalName)) {
            IOUtils.copy(inputStream, resourceOutputStream);
        }

        IOUtils.closeQuietly(inputStream);
    }

    void processCopyItem(Source source, CopyItem copyItem, ModuleContext moduleContext) throws IOException {
        final ResourceInputProvider rip =  copyItem.getValue().getResourceInputProvider();
        final ResourceOutputProvider outputProvider = moduleContext.getOutputProvider(source);

        // todo: this is ugly and needs to be replaced by a solution that doesn't require this cast
        final ValueImpl value = (ValueImpl) copyItem.getValue();
        if (rip instanceof FileResourceInputProvider && outputProvider instanceof FileResourceOutputProvider
                // don't try to do a basePath comparison if this is actually backed by the JCR
                && value.getInternalResourcePath() == null) {
            final FileResourceInputProvider frip = (FileResourceInputProvider) rip;
            final FileResourceOutputProvider fout = (FileResourceOutputProvider) outputProvider;

            if (frip.getBasePath().equals(fout.getModulePath())) {
                // don't copy when src and dest are the same file
                return;
            }
        }

        try (final InputStream resourceInputStream = getValueInputProvider(value);
             final OutputStream resourceOutputStream =
                     outputProvider.getResourceOutputStream(source, copyItem.getValue().getString())) {
            // TODO: after Java 9, use InputStream.transferTo()
            IOUtils.copy(resourceInputStream, resourceOutputStream);
        }
    }

    // TODO: move this processing somewhere else
    InputStream getValueInputProvider(final ValueImpl value) throws IOException {
        return value.getResourceInputStream();
    }
}
