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

package org.onehippo.cm.model.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.onehippo.cm.ResourceInputProvider;
import org.onehippo.cm.model.Group;
import org.onehippo.cm.model.Module;
import org.onehippo.cm.model.OrderableByName;
import org.onehippo.cm.model.Project;
import org.onehippo.cm.model.Source;
import org.onehippo.cm.model.parser.ConfigSourceParser;
import org.onehippo.cm.model.parser.ContentSourceParser;

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
            final Group group = project.getGroup();
            final String content = group.getName() + "/" + project.getName() + "/" + module.getName() + "/"
                    + source.getPath() + "/" + resourcePath;
            return IOUtils.toInputStream(content, StandardCharsets.UTF_8);
        }

        @Override
        public Path getResourcePath(final Source source, final String resourcePath) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getResourceModulePath(final Source source, final String resourcePath) {
            throw new UnsupportedOperationException();
        }
    };


    public static <T extends OrderableByName> T findByName(final String name, final Collection<T> entries) {
        return findInCollection(name, entries, OrderableByName::getName);
    }

    public static <T extends SourceImpl> T findByPath(final String name, final Collection<T> entries) {
        return findInCollection(name, entries, SourceImpl::getPath);
    }

    private static <T> T findInCollection(final String id, final Collection<T> entries, Function<T, String> identifier) {
        return entries.stream().collect(Collectors.toMap(identifier, t -> t)).get(id);
    }

    public static ResourceInputProvider getTestResourceInputProvider() {
        return resourceInputProvider;
    }

    public static ModuleImpl makeModule(final String moduleName) {
        return new GroupImpl("test-group").addProject("test-project").addModule(moduleName);
    }

    public static List<AbstractDefinitionImpl> parseNoSort(final String yaml) throws Exception {
        return parseNoSort(yaml, false, true);
    }


    public static List<AbstractDefinitionImpl> parseNoSort(final String yaml, final boolean verifyOnly) throws Exception {
        return parseNoSort(yaml, "test-module", verifyOnly, true);
    }

    public static List<AbstractDefinitionImpl> parseNoSort(final String yaml, final boolean verifyOnly,final boolean config) throws Exception {
        return parseNoSort(yaml, "test-module", verifyOnly, config);
    }

    public static List<AbstractDefinitionImpl> parseNoSort(final String yaml, final String moduleName, final boolean config) throws Exception {
        return parseNoSort(yaml, moduleName, false, config);
    }

    public static List<AbstractDefinitionImpl> parseNoSort(final String yaml, final String moduleName, final boolean verifyOnly,final boolean config) throws Exception {
        final ModuleImpl module = makeModule(moduleName);
        loadYAMLString(yaml, module, verifyOnly, config);
        return findByPath(STRING_SOURCE, module.getSources()).getDefinitions();
    }

    public static void loadYAMLResource(final ClassLoader classLoader, final String resourcePath, final ModuleImpl module) throws Exception {
        loadYAMLResource(classLoader, resourcePath, module, false, true);
    }

    public static void loadYAMLResource(final ClassLoader classLoader, final String resourcePath, final ModuleImpl module, final boolean config) throws Exception {
        loadYAMLResource(classLoader, resourcePath, module, false, config);
    }

    public static void loadYAMLResource(final ClassLoader classLoader, final String resourcePath, final ModuleImpl module, final boolean verifyOnly, final boolean config) throws Exception {
        final InputStream input = classLoader.getResourceAsStream(resourcePath);
        loadYAMLStream(input, resourcePath, module, verifyOnly, config);
    }

    public static void loadYAMLString(final String yaml, final ModuleImpl module) throws Exception {
        loadYAMLString(yaml, module, false, true);
    }

    public static void loadYAMLString(final String yaml, final ModuleImpl module, final boolean config) throws Exception {
        loadYAMLString(yaml, module, false, config);
    }

    public static void loadYAMLString(final String yaml, final ModuleImpl module, final boolean verifyOnly, final boolean config) throws Exception {
        final InputStream input = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        loadYAMLStream(input, STRING_SOURCE, module, verifyOnly, config);
    }

    private static void loadYAMLStream(final InputStream input, final String sourcePath, final ModuleImpl module, final boolean verifyOnly, final boolean config) throws Exception {
        if (config) {
            new ConfigSourceParser(resourceInputProvider, verifyOnly).parse(input, sourcePath, sourcePath, module);
        } else {
            new ContentSourceParser(resourceInputProvider, verifyOnly).parse(input, sourcePath, sourcePath, module);
        }
    }

}
