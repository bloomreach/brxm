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
package org.onehippo.cms.l10n;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.definition.ConfigDefinitionImpl;
import org.onehippo.cm.model.impl.source.ConfigSourceImpl;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.tree.DefinitionPropertyImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.parser.ConfigSourceParser;
import org.onehippo.cm.model.parser.ParserException;
import org.onehippo.cm.model.serializer.FileResourceOutputProvider;
import org.onehippo.cm.model.serializer.ModuleContext;
import org.onehippo.cm.model.serializer.ModuleWriter;
import org.onehippo.cm.model.source.ResourceInputProvider;
import org.onehippo.cm.model.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang.StringUtils.substringBefore;

public class RepositoryResourceBundle extends ResourceBundle {

    private static final Logger log = LoggerFactory.getLogger(RepositoryResourceBundle.class);
    public static final String HIPPO_CONFIGURATION_HIPPO_TRANSLATIONS = "/hippo:configuration/hippo:translations";
    public static final String JCR_PRIMARY_TYPE = "jcr:primaryType";
    public static final String HIPPOSYS_RESOURCEBUNDLE = "hipposys:resourcebundle";


    RepositoryResourceBundle(final String name, final String fileName, final File file) {
        super(name, fileName, file);
    }

    RepositoryResourceBundle(final String name, final String fileName, final String locale, final Map<String, String> entries) {
        super(name, fileName, locale, entries);
    }

    @Override
    public String getId() {
        return getFileName() + "/" + getName();
    }

    @Override
    public BundleType getType() {
        return BundleType.REPOSITORY;
    }

    @Override
    protected RepositoryBundleSerializer getSerializer() {
        return new RepositoryBundleSerializer();
    }

    private File getBaseDir() {
        return new File(substringBefore(file.getAbsolutePath(), fileName));
    }

    @Override
    public boolean exists() {
        if (!file.exists()) {
            return false;
        }
        try {
            return getSerializer().exists();
        } catch (IOException e) {
            log.error("Failed to determine whether bundle exists", e);
            return false;
        }
    }

    @Override
    public void delete() throws IOException {
        getSerializer().delete();
    }

    static Iterable<ResourceBundle> createAllInstances(final String fileName, final File file, final String locale) throws IOException {
        final Collection<ResourceBundle> bundles = new ArrayList<>();

        final ModuleImpl module = loadBundleModule(file);
        final Stream<DefinitionNodeImpl> configDefinitionStream = module.getConfigSources().stream()
                .flatMap(s -> s.getDefinitions().stream())
                .filter(d -> d instanceof ConfigDefinitionImpl)
                .map(d -> ((ConfigDefinitionImpl) d).getNode())
                .filter(d -> d.getPath().startsWith(HIPPO_CONFIGURATION_HIPPO_TRANSLATIONS));

        configDefinitionStream.forEach(definitionNode -> {
            RepositoryResourceBundleLoader.collectResourceBundles(definitionNode, fileName, Collections.singletonList(locale), bundles);
        });

        for (ResourceBundle bundle : bundles) {
            ((RepositoryResourceBundle) bundle).file = file;
        }
        return bundles;
    }

    private static ModuleImpl loadBundleModule(final File sourceFile) throws IOException {
        final ResourceInputProvider resourceInputProvider = new ResourceInputProvider() {
            @Override
            public boolean hasResource(final Source source, final String resourcePath) {
                return false;
            }

            @Override
            public InputStream getResourceInputStream(final Source source, final String resourcePath) throws IOException {
                throw new IOException("Plain YAML import does not support links to resources");
            }
        };

        final ConfigSourceParser sourceParser = new ConfigSourceParser(resourceInputProvider);
        try {
            try(final InputStream inputStream = Files.newInputStream(sourceFile.toPath())) {
                final ModuleImpl module = createDummyModule();
                sourceParser.parse(inputStream, sourceFile.toPath().getFileName().toString(), sourceFile.toPath().toString(), module);
                return module;
            }

        } catch (ParserException e) {
            throw new RuntimeException(e);
        }
    }

    private static ModuleImpl createDummyModule() {
        return new ModuleImpl("load-module", new ProjectImpl("load-project", new GroupImpl("load-group")));
    }

    public class ExportModuleContext extends ModuleContext {


        ExportModuleContext(ModuleImpl module, Path moduleDescriptorPath) throws IOException {
            super(module, moduleDescriptorPath);
            createOutputProviders(moduleDescriptorPath);
        }

        @Override
        public void createOutputProviders(Path moduleDescriptorPath) {
            configOutputProvider = new FileResourceOutputProvider(moduleDescriptorPath, "");
            contentOutputProvider = new FileResourceOutputProvider(moduleDescriptorPath, "");
        }
    }

    /**
     * Every bundle goes into its own root definitions
     * i.e. /hippo:configuration/hippo:translations/path/to/bundle
     * instead of hierarchy based form and reusing common paths.
     */
    private class RepositoryBundleSerializer extends Serializer {

        @Override
        public void serialize() throws IOException {

            final String bundlePath = constructBundlePath();
            final ModuleImpl module;
            DefinitionNodeImpl bundleNode;
            if (file.exists()) {
                module = loadBundleModule(file);
                ConfigSourceImpl source = module.getConfigSources().iterator().next();
                final Optional<DefinitionNodeImpl> bundleNodeOpt = source.getModifiableDefinitions().stream()
                        .filter(d -> d instanceof ConfigDefinitionImpl)
                        .map(d -> ((ConfigDefinitionImpl) d).getNode())
                        .filter(n -> n.getPath().equals(bundlePath))
                        .findFirst();

                bundleNode = bundleNodeOpt.orElse(source.getOrCreateDefinitionFor(bundlePath));

            } else {
                module = createDummyModule();
                final ConfigSourceImpl configSource = module.addConfigSource(file.toPath().getFileName().toString());
                bundleNode = configSource.getOrCreateDefinitionFor(bundlePath);
            }

            DefinitionNodeImpl localeNode = bundleNode.getNode(locale);
            if (localeNode == null) {
                localeNode = bundleNode.addNode(locale);
            }

            localeNode.getModifiableProperties().clear();
            localeNode.addProperty(JCR_PRIMARY_TYPE, new ValueImpl(HIPPOSYS_RESOURCEBUNDLE));

            for (final Map.Entry<String, String> entry : entries.entrySet()) {
                localeNode.addProperty(entry.getKey(), new ValueImpl(entry.getValue()));
            }

            writeModule(module);
        }

        private void writeModule(final ModuleImpl module) throws IOException {
            final ModuleContext moduleContext = new ExportModuleContext(module, file.toPath().getParent());
            final ModuleWriter moduleWriter = new ModuleWriter();
            moduleWriter.writeModule(module, moduleContext);
        }

        @Override
        protected void deserialize() throws IOException {

            final String bundlePath = constructBundlePath();

            final ModuleImpl module = loadBundleModule(file);

            final List<DefinitionNodeImpl> definitions = module.getConfigSources().iterator().next()
                    .getDefinitions().stream().filter(d -> d instanceof ConfigDefinitionImpl)
                    .map(d -> ((ConfigDefinitionImpl) d).getNode())
                    .filter(n -> n.getPath().startsWith("/hippo:configuration"))
                    .collect(Collectors.toList());
            for (DefinitionNodeImpl node : definitions) {
                if (node.getPath().equals(bundlePath)) {
                    final DefinitionNodeImpl localeNode = node.getNode(locale);
                    if (localeNode != null) {
                        for (DefinitionPropertyImpl property : localeNode.getProperties()) {
                            if (!property.getName().equals(JCR_PRIMARY_TYPE)) {
                                entries.put(property.getName(), property.getValue().getString());
                            }
                        }
                        return;
                    }
                }
            }
        }

        public boolean exists() throws IOException {

            final String bundlePath = constructBundlePath();

            final ModuleImpl module = loadBundleModule(file);
            final ConfigSourceImpl source = module.getConfigSources().iterator().next();

            final Optional<DefinitionNodeImpl> bundleNodeOpt = source.getModifiableDefinitions().stream()
                    .filter(d -> d instanceof ConfigDefinitionImpl)
                    .map(d -> ((ConfigDefinitionImpl) d).getNode())
                    .filter(n -> n.getPath().equals(bundlePath))
                    .findFirst();

            if (!bundleNodeOpt.isPresent()) {
                return false;
            }

            final DefinitionNodeImpl bundleNode = bundleNodeOpt.get();
            return bundleNode.getNode(locale) != null;
        }

        private void delete() throws IOException {
            if (!file.exists()) {
                return;
            }

            final ModuleImpl module = loadBundleModule(file);

            final String bundlePath = constructBundlePath();
            final ConfigSourceImpl source = module.getConfigSources().iterator().next();
            final Optional<DefinitionNodeImpl> bundleNodeOpt = source.getModifiableDefinitions().stream()
                    .filter(d -> d instanceof ConfigDefinitionImpl)
                    .map(d -> ((ConfigDefinitionImpl) d).getNode())
                    .filter(n -> n.getPath().equals(bundlePath))
                    .findFirst();

            if (!bundleNodeOpt.isPresent()) {
                return;
            }

            final DefinitionNodeImpl bundleNode = bundleNodeOpt.get();

            if (bundleNode.getNode(locale) != null) {
                bundleNode.getModifiableNodes().remove(bundleNode.getNode(locale).getName());
                if (bundleNode.getNodes().isEmpty() && source.getDefinitions().size() == 1) {
                    log.debug("Deleting file {}", fileName);
                    Files.deleteIfExists(file.toPath());
                } else {
                    if (bundleNode.isEmpty()) {
                        source.getModifiableDefinitions().remove(bundleNode.getDefinition());
                    }
                    writeModule(module);
                }
            }
        }
    }

    private String constructBundlePath() {
        return Arrays.stream(name.split("\\.")).reduce(HIPPO_CONFIGURATION_HIPPO_TRANSLATIONS, (a, b) -> a + "/" + b);
    }
}
