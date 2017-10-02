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
package org.onehippo.cm.model.serializer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

import org.onehippo.cm.model.Constants;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.source.SourceImpl;
import org.onehippo.cm.model.source.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.nodes.Node;

import static org.onehippo.cm.model.Constants.DEFAULT_EXPLICIT_SEQUENCING;

public class ModuleWriter {

    private static final Logger log = LoggerFactory.getLogger(ModuleWriter.class);

    public ModuleWriter() {}

    protected void write(final Path destination,
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

    public void writeModule(final ModuleImpl module, final ModuleContext moduleContext)
            throws IOException {
        writeModule(module, DEFAULT_EXPLICIT_SEQUENCING, moduleContext);
    }

    public void writeModule(final ModuleImpl module, final boolean explicitSequencing,
                            final ModuleContext moduleContext) throws IOException {

        moduleContext.collectKnownResourcePaths();

        for (final SourceImpl source : module.getSources()) {
            if (sourceShouldBeSkipped(source)) {
                continue;
            }

            final SourceSerializer sourceSerializer = SourceType.CONFIG == source.getType()
                    ? createSourceSerializer(moduleContext, source, explicitSequencing)
                    : createContentSourceSerializer(moduleContext, source, explicitSequencing);

            final Node node = sourceSerializer.representSource();

            final ResourceOutputProvider outputProvider = moduleContext.getOutputProvider(source);
            try (final OutputStream sourceOutputStream = outputProvider.getSourceOutputStream(source)) {
                sourceSerializer.serializeNode(sourceOutputStream, node);
            }
        }
    }

    protected SourceSerializer createSourceSerializer(final ModuleContext moduleContext, final SourceImpl source,
                                                      final boolean explicitSequencing) {
        return new SourceSerializer(moduleContext, source, explicitSequencing);
    }

    protected ContentSourceSerializer createContentSourceSerializer(final ModuleContext moduleContext,
                                                                    final SourceImpl source, final boolean explicitSequencing) {
        return new ContentSourceSerializer(moduleContext, source, explicitSequencing);
    }

    protected boolean sourceShouldBeSkipped(final SourceImpl source) {
        return false;
    }
}
