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

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.ContentDefinitionImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.ProjectImpl;
import org.onehippo.cm.impl.model.builder.AbstractBuilderBaseTest;
import org.onehippo.cm.impl.model.builder.MergedModel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class DefinitionSorterTest extends AbstractBuilderBaseTest {

    private final ModuleImpl module = makeModule();
    private final DefinitionSorter sorter = new DefinitionSorter();

    @Test
    public void definitions_in_single_source() throws Exception {
        final MergedModel model = new MergedModel();

        loadYAMLFile("builder/definition-sorter.yaml", module);

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

        loadYAMLFile("builder/definition-sorter.yaml", module);
        loadYAMLFile("builder/definition-sorter2.yaml", module);

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

        loadYAMLFile("builder/definition-sorter2.yaml", module);
        loadYAMLFile("builder/definition-sorter.yaml", module);

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

        loadYAMLFile("builder/definition-sorter.yaml", module);
        loadYAMLString(yaml, module);

        try {
            sorter.sort(module, model);
            fail("Expect IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Duplicate content root paths '/a/b' in module 'test-module'.", e.getMessage());
        }
    }
}
