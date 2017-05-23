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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.onehippo.cm.ResourceInputProvider;
import org.onehippo.cm.model.ConfigDefinition;
import org.onehippo.cm.model.ContentDefinition;
import org.onehippo.cm.model.Definition;
import org.onehippo.cm.model.Group;
import org.onehippo.cm.model.Module;
import org.onehippo.cm.model.Orderable;
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
        public URL getBaseURL() {
            throw new UnsupportedOperationException();
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

    public static ModuleImpl makeModule(final String moduleName) {
        return new GroupImpl("test-group").addProject("test-project").addModule(moduleName);
    }

    public static List<Definition> parseNoSort(final String yaml) throws Exception {
        return parseNoSort(yaml, false, ConfigDefinition.class);
    }


    public static List<Definition> parseNoSort(final String yaml, final boolean verifyOnly) throws Exception {
        return parseNoSort(yaml, "test-module", verifyOnly, ConfigDefinition.class);
    }

    public static List<Definition> parseNoSort(final String yaml, final boolean verifyOnly,final Class clazz) throws Exception {
        return parseNoSort(yaml, "test-module", verifyOnly, clazz);
    }

    public static List<Definition> parseNoSort(final String yaml, final String moduleName, final Class clazz) throws Exception {
        return parseNoSort(yaml, moduleName, false, clazz);
    }

    public static List<Definition> parseNoSort(final String yaml, final String moduleName, final boolean verifyOnly,final Class clazz) throws Exception {
        final ModuleImpl module = makeModule(moduleName);
        loadYAMLString(yaml, module, verifyOnly, clazz);
        return findByPath(STRING_SOURCE, module.getSources()).getDefinitions();
    }

    public static void loadYAMLResource(final ClassLoader classLoader, final String resourcePath, final ModuleImpl module) throws Exception {
        loadYAMLResource(classLoader, resourcePath, module, false, ConfigDefinition.class);
    }

    public static void loadYAMLResource(final ClassLoader classLoader, final String resourcePath, final ModuleImpl module, final Class clazz) throws Exception {
        loadYAMLResource(classLoader, resourcePath, module, false, clazz);
    }

    public static void loadYAMLResource(final ClassLoader classLoader, final String resourcePath, final ModuleImpl module, final boolean verifyOnly, final Class clazz) throws Exception {
        final InputStream input = classLoader.getResourceAsStream(resourcePath);
        loadYAMLStream(input, resourcePath, module, verifyOnly, clazz);
    }

    public static void loadYAMLString(final String yaml, final ModuleImpl module) throws Exception {
        loadYAMLString(yaml, module, false, ConfigDefinition.class);
    }

    public static void loadYAMLString(final String yaml, final ModuleImpl module, final Class clazz) throws Exception {
        loadYAMLString(yaml, module, false, clazz);
    }

    public static void loadYAMLString(final String yaml, final ModuleImpl module, final boolean verifyOnly, Class clazz) throws Exception {
        final InputStream input = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        loadYAMLStream(input, STRING_SOURCE, module, verifyOnly, clazz);
    }

    private static void loadYAMLStream(final InputStream input, final String sourcePath, final ModuleImpl module, final boolean verifyOnly, final Class clazz) throws Exception {
        if (clazz.equals(ContentDefinition.class)) {
            new ContentSourceParser(resourceInputProvider, verifyOnly).parse(input, sourcePath, sourcePath, module);
        } else {
            new ConfigSourceParser(resourceInputProvider, verifyOnly).parse(input, sourcePath, sourcePath, module);
        }
    }

}
