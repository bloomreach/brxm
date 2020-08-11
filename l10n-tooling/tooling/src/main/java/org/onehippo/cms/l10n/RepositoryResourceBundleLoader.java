/*
 *  Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Stream;

import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.definition.ConfigDefinitionImpl;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.tree.DefinitionPropertyImpl;
import org.onehippo.cm.model.parser.ModuleReader;
import org.onehippo.cm.model.parser.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms.l10n.RepositoryResourceBundle.HIPPO_CONFIGURATION_HIPPO_TRANSLATIONS;
import static org.onehippo.cms.l10n.RepositoryResourceBundle.JCR_PRIMARY_TYPE;

class RepositoryResourceBundleLoader extends ResourceBundleLoader {

    private static final Logger log = LoggerFactory.getLogger(RepositoryResourceBundleLoader.class);
    public static final String YAML_EXT = ".yaml";
    public static final String HCM_MODULE_YAML = "hcm-module" + YAML_EXT;

    RepositoryResourceBundleLoader(final Collection<String> locales, final ClassLoader classLoader) {
        super(locales, classLoader);
    }

    @Override
    protected void collectResourceBundles(final ArtifactInfo artifactInfo, final Collection<ResourceBundle> bundles) throws IOException {
        // for all jars with hcm-module.yaml files ...
        try {
            final File jarFile = artifactInfo.getJarFile();
            try (final FileSystem fileSystem = createZipFileSystem(jarFile.toPath().toString())) {
                java.nio.file.Path descriptor = fileSystem.getPath(HCM_MODULE_YAML);

                if (Files.exists(descriptor)) {
                    final ModuleReader moduleReader = new ModuleReader();
                    final ModuleImpl module = moduleReader.read(fileSystem.getPath(HCM_MODULE_YAML), false).getModule();

                    final Stream<DefinitionNodeImpl> configDefinitionStream = module.getConfigSources().stream()
                            .flatMap(s -> s.getDefinitions().stream())
                            .filter(d -> d instanceof ConfigDefinitionImpl)
                            .map(d -> ((ConfigDefinitionImpl) d).getNode())
                            .filter(d -> d.getPath().startsWith(HIPPO_CONFIGURATION_HIPPO_TRANSLATIONS));
                    configDefinitionStream.forEach(definitionNode ->
                            collectResourceBundles(definitionNode, definitionNode.getDefinition().getSource().getPath(), locales, bundles));
                }
            }

        } catch (URISyntaxException | ParserException e) {
            throw new IOException(e);
        } catch (FileSystemAlreadyExistsException e) {
            // CMS-13468 Method is not threadsafe when loading modules such as hippo-cms7-commons multiple times
            log.warn("Re-loading filesystem failed for " + artifactInfo.getCoordinates());
        }
    }

    private static FileSystem createZipFileSystem(final String zipFilename) throws IOException {
        final java.nio.file.Path path = Paths.get(zipFilename);
        final URI uri = URI.create("jar:file:" + path.toUri().getPath());
        final Map<String, String> env = new HashMap<>();
        return FileSystems.newFileSystem(uri, env);
    }

    static void collectResourceBundles(final DefinitionNodeImpl node, final String fileName, final Collection<String> locales, final Collection<ResourceBundle> bundles) {

        Path path = new Path();
        String bundlePath = node.getPath().replace(HIPPO_CONFIGURATION_HIPPO_TRANSLATIONS, "");
        if (bundlePath.startsWith("/")) {
            bundlePath = bundlePath.substring(1);
        }

        final String[] chunks = bundlePath.split("/");
        for (String chunk : chunks) {
            path.push(chunk);
        }

        collectResourceBundles(node, fileName, locales, path, bundles);
    }

    private static void collectResourceBundles(final DefinitionNodeImpl node, final String fileName, final Collection<String> locales, final Path path, final Collection<ResourceBundle> bundles) {

        final Map<String, String> entries = new HashMap<>();

        if (node.getNodes().size() > 0) {
            for (DefinitionNodeImpl childNode : node.getNodes()) {
                path.push(childNode.getName());
                collectResourceBundles(childNode, fileName, locales, path, bundles);
                path.pop();
            }
        }

        if (node.getProperties().size() > 0) {
            for (DefinitionPropertyImpl property : node.getProperties()) {
                if (!property.getName().equals(JCR_PRIMARY_TYPE)) {
                    entries.put(property.getName(), property.getValue().getString());
                }
            }
        }

        if (!entries.isEmpty() && locales.contains(path.toLocale())) {
            bundles.add(new RepositoryResourceBundle(path.toBundleName(), fileName, path.toLocale(), entries));
        }
    }

    /**
     * Utility for keeping track of the path inside a repository resource bundle import file.
     */
    static class Path {

        private final Stack<String> elements = new Stack<>();

        Path() {
        }

        private void push(String element) {
            elements.push(element);
        }

        private void pop() {
            elements.pop();
        }

        private String peek() {
            return elements.peek();
        }

        String toBundleName() {
            final StringBuilder sb = new StringBuilder();
            final Iterator<String> iterator = elements.iterator();
            while (iterator.hasNext()) {
                final String element = iterator.next();
                if (iterator.hasNext()) {
                    if (sb.length() > 0) {
                        sb.append('.');
                    }
                    sb.append(element);
                }
            }
            return sb.toString();
        }

        String toLocale() {
            return peek();
        }
    }

}
