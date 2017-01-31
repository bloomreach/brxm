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

package org.onehippo.cm.impl.model.builder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.engine.ResourceInputProvider;
import org.onehippo.cm.engine.SourceParser;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.ProjectImpl;

public abstract class AbstractBuilderBaseTest {
    private final String STRING_SOURCE = "string";

    protected ModuleImpl makeModule() {
        final ConfigurationImpl configuration = new ConfigurationImpl("test-configuration");
        final ProjectImpl project = new ProjectImpl("test-project", configuration);
        return new ModuleImpl("test-module", project);
    }

    protected String sortedCollectionToString(final Map<String, ? extends Object> map) {
        return map.keySet().stream().collect(Collectors.toList()).toString();
    }

    protected List<Definition> parseNoSort(final String yaml) throws Exception {
        final ModuleImpl module = makeModule();
        loadYAMLString(yaml, module);
        return module.getSources().get(STRING_SOURCE).getDefinitions();
    }

    protected void loadYAMLFile(final String filePath, final ModuleImpl module) throws Exception {
        final InputStream input = getClass().getClassLoader().getResourceAsStream(filePath);
        loadYAMLStream(input, filePath, module);
    }

    protected void loadYAMLString(final String yaml, final ModuleImpl module) throws Exception {
        final InputStream input = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        loadYAMLStream(input, STRING_SOURCE, module);
    }

    private static void loadYAMLStream(final InputStream input, final String sourcePath, final ModuleImpl module) throws Exception {
        new SourceParser(new ResourceInputProvider() {
            @Override
            public boolean hasResource(final Source source, final String resourcePath) {
                return false;
            }

            @Override
            public InputStream getResourceInputStream(final Source source, final String resourcePath) throws IOException {
                return null;
            }
        }).parse(sourcePath, input, module);
    }
}
