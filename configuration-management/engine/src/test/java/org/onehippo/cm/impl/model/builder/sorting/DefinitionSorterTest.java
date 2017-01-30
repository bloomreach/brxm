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

package org.onehippo.cm.impl.model.builder.sorting;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.engine.ResourceInputProvider;
import org.onehippo.cm.engine.SourceParser;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.ContentDefinitionImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.ProjectImpl;
import org.onehippo.cm.impl.model.builder.MergedModel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DefinitionSorterTest {

    private final ConfigurationImpl configuration = new ConfigurationImpl("test-configuration");
    private final ProjectImpl project = new ProjectImpl("test-project", configuration);
    private final ModuleImpl module = new ModuleImpl("test-module", project);
    private final DefinitionSorter sorter = new DefinitionSorter();

    @Test
    public void definitions_in_single_source() throws Exception {
        final MergedModel model = new MergedModel();

        loadYAMLFile("builder/definition-sorter.yaml");

        sorter.sort(module, model);

        assertEquals(1, model.getNamespaceDefinitions().size());
        assertEquals(1, model.getNodeTypeDefinitions().size());

        final List<ContentDefinitionImpl> definitions = module.getSortedContentDefinitions();

        assertEquals(5, definitions.size());
        String roots = definitions.stream().map(d -> d.getNode().getPath()).collect(Collectors.toList()).toString();
        assertEquals("[/a, /a/b, /a/b/a, /a/b/c, /a/b/c/d]", roots);
    }

    @Test
    public void definitions_in_multiple_files() throws Exception {
        final MergedModel model = new MergedModel();

        loadYAMLFile("builder/definition-sorter.yaml");
        loadYAMLFile("builder/definition-sorter2.yaml");

        sorter.sort(module, model);

        assertEquals(2, model.getNamespaceDefinitions().size());
        assertEquals(1, model.getNodeTypeDefinitions().size());

        final List<ContentDefinitionImpl> definitions = module.getSortedContentDefinitions();

        assertEquals(9, definitions.size());
        String roots = definitions.stream().map(d -> d.getNode().getPath()).collect(Collectors.toList()).toString();
        assertEquals("[/a, /a/a, /a/b, /a/b/a, /a/b/c, /a/b/c/b, /a/b/c/d, /a/b/c/d/e, /b]", roots);
    }

    @Test
    public void definitions_in_multiple_files_different_load_order() throws Exception {
        final MergedModel model = new MergedModel();

        loadYAMLFile("builder/definition-sorter2.yaml");
        loadYAMLFile("builder/definition-sorter.yaml");

        sorter.sort(module, model);

        assertEquals(2, model.getNamespaceDefinitions().size());
        assertEquals(1, model.getNodeTypeDefinitions().size());

        final List<ContentDefinitionImpl> definitions = module.getSortedContentDefinitions();

        assertEquals(9, definitions.size());
        String roots = definitions.stream().map(d -> d.getNode().getPath()).collect(Collectors.toList()).toString();
        assertEquals("[/a, /a/a, /a/b, /a/b/a, /a/b/c, /a/b/c/b, /a/b/c/d, /a/b/c/d/e, /b]", roots);
    }

    @Test
    public void sources_with_same_root() throws Exception {
        final MergedModel model = new MergedModel();
        final String yaml = "instructions:\n"
                + "  - config:\n"
                + "    - /a/b:\n"
                + "      - propertyX: blaX";

        loadYAMLFile("builder/definition-sorter.yaml");
        loadYAMLString(yaml);

        try {
            sorter.sort(module, model);
            fail("Expect IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Duplicate content root paths '/a/b' in module 'test-module'.", e.getMessage());
        }
    }



    private void loadYAMLFile(final String filePath) throws Exception {
        final InputStream input = getClass().getClassLoader().getResourceAsStream(filePath);
        loadYAMLStream(input, filePath);
    }

    private void loadYAMLString(final String yaml) throws Exception {
        final InputStream input = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
        loadYAMLStream(input, "string");
    }

    private void loadYAMLStream(final InputStream input, final String sourcePath) throws Exception {
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
