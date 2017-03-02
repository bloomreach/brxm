/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cm.impl.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Orderable;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.engine.ResourceInputProvider;
import org.onehippo.cm.engine.SourceParser;

public class ModelTestUtils {

    private static final String STRING_SOURCE = "string";

    private final static ResourceInputProvider resourceInputProvider = new ResourceInputProvider() {
        @Override
        public boolean hasResource(final Source source, final String resourcePath) {
            return !resourcePath.contains("not");
        }
        @Override
        public InputStream getResourceInputStream(final Source source, final String resourcePath) throws IOException {
            final Module module = source.getModule();
            final Project project = module.getProject();
            final Configuration configuration = project.getConfiguration();
            final String content = configuration.getName() + "/" + project.getName() + "/" + module.getName() + "/"
                    + source.getPath() + "/" + resourcePath;
            return IOUtils.toInputStream(content, StandardCharsets.UTF_8);
        }
    };


    public static <T extends Orderable> T findByName(final String name, final Collection<T> entries) {
        return findInCollection(name, entries, Orderable::getName);
    }

    public static <T extends Source> T findByPath(final String name, final Collection<T> entries) {
        return findInCollection(name, entries, Source::getPath);
    }

    private static <T> T findInCollection(final String id, final Collection<T> entries, Function<T, String> identifier) {
        return entries.stream().collect(Collectors.toMap(identifier, t -> t)).get(id);
    }

    public static ResourceInputProvider getTestResourceInputProvider() {
        return resourceInputProvider;
    }

    private static ModuleImpl makeModule(final String moduleName) {
        return new ConfigurationImpl("test-configuration").addProject("test-project").addModule(moduleName);
    }

    public static List<Definition> parseNoSort(final String yaml) throws Exception {
        return parseNoSort(yaml, false);
    }

    public static List<Definition> parseNoSort(final String yaml, final boolean verifyOnly) throws Exception {
        return parseNoSort(yaml, "test-module", verifyOnly);
    }

    public static List<Definition> parseNoSort(final String yaml, final String moduleName) throws Exception {
        return parseNoSort(yaml, moduleName, false);
    }

    public static List<Definition> parseNoSort(final String yaml, final String moduleName, final boolean verifyOnly) throws Exception {
        final ModuleImpl module = makeModule(moduleName);
        loadYAMLString(yaml, module, verifyOnly);
        return findByPath(STRING_SOURCE, module.getSources()).getDefinitions();
    }

    public static void loadYAMLResource(final ClassLoader classLoader, final String resourcePath, final ModuleImpl module) throws Exception {
        loadYAMLResource(classLoader, resourcePath, module, false);
    }

    public static void loadYAMLResource(final ClassLoader classLoader, final String resourcePath, final ModuleImpl module, final boolean verifyOnly) throws Exception {
        final InputStream input = classLoader.getResourceAsStream(resourcePath);
        loadYAMLStream(input, resourcePath, module, verifyOnly);
    }

    public static void loadYAMLString(final String yaml, final ModuleImpl module) throws Exception {
        loadYAMLString(yaml, module, false);
    }

    public static void loadYAMLString(final String yaml, final ModuleImpl module, final boolean verifyOnly) throws Exception {
        final InputStream input = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        loadYAMLStream(input, STRING_SOURCE, module, verifyOnly);
    }

    private static void loadYAMLStream(final InputStream input, final String sourcePath, final ModuleImpl module, final boolean verifyOnly) throws Exception {
        new SourceParser(resourceInputProvider, verifyOnly).parse(input, sourcePath, sourcePath, module);
    }

}
