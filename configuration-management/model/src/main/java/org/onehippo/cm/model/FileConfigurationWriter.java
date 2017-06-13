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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.onehippo.cm.ResourceInputProvider;
import org.onehippo.cm.model.impl.ValueImpl;
import org.onehippo.cm.model.serializer.AggregatedModulesDescriptorSerializer;
import org.onehippo.cm.model.serializer.ContentSourceSerializer;
import org.onehippo.cm.model.serializer.SourceSerializer;
import org.yaml.snakeyaml.nodes.Node;

import static org.onehippo.cm.model.Constants.DEFAULT_EXPLICIT_SEQUENCING;

public class FileConfigurationWriter {

    private ResourceInputProvider jcrRip;

    public FileConfigurationWriter() {}

    public FileConfigurationWriter(final ResourceInputProvider jcrRip) {
        this.jcrRip = jcrRip;
    }

    void write(final Path destination,
               final Collection<? extends Group> groups,
               final Map<Module, ModuleContext> moduleContextMap,
               final boolean explicitSequencing) throws IOException {
        final AggregatedModulesDescriptorSerializer moduleDescriptorSerializer = new AggregatedModulesDescriptorSerializer(explicitSequencing);
        final Path moduleDescriptorPath = destination.resolve(Constants.HCM_MODULE_YAML);

        try (final OutputStream moduleDescriptorOutputStream = new FileOutputStream(moduleDescriptorPath.toFile())) {
            moduleDescriptorSerializer.serialize(moduleDescriptorOutputStream, groups);
        }

        for (Group group : groups) {
            for (Project project : group.getProjects()) {
                for (Module module : project.getModules()) {
                    final ModuleContext moduleContext = moduleContextMap.get(module);
                    moduleContext.createOutputProviders(moduleDescriptorPath);
                    writeModule(module, explicitSequencing, moduleContext);
                }
            }
        }
    }

    public void writeModule(final Module module, final ModuleContext moduleContext) throws IOException {
        writeModule(module, DEFAULT_EXPLICIT_SEQUENCING, moduleContext);
    }

    public void writeModule(final Module module, final boolean explicitSequencing,
                            final ModuleContext moduleContext) throws IOException {

        // TODO: remove deleted resources first

        moduleContext.collectExistingFilesAndResolveNewResources();

        for (final Source source : module.getSources()) {

            // TODO: optimize by writing only changed sources
            // TODO: what about writes to the same directory as source?

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
                && !value.isNewResource()) {
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
            IOUtils.copy(resourceInputStream, resourceOutputStream);
        }
    }

    // TODO: move this processing somewhere else
    private InputStream getValueInputProvider(final ValueImpl value) throws IOException {
        final String internalResourcePath = value.getInternalResourcePath();
        if (internalResourcePath != null && value.isNewResource()) {
            return jcrRip.getResourceInputStream(null, internalResourcePath);
        }
        else {
            return value.getResourceInputStream();
        }
    }
}
