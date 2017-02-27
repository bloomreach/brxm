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

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.onehippo.cm.api.model.ConfigurationNode;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.ContentDefinitionImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.NamespaceDefinitionImpl;
import org.onehippo.cm.impl.model.ProjectImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.onehippo.cm.impl.model.ModelTestUtils.loadYAMLResource;
import static org.onehippo.cm.impl.model.ModelTestUtils.loadYAMLString;

public class MergedModelBuilderTest extends AbstractBuilderBaseTest {

    @Test
    public void empty_builder() {
        final MergedModel model = new MergedModelBuilder().build();

        assertEquals(0, model.getSortedConfigurations().size());
        assertEquals(0, model.getNamespaceDefinitions().size());
        assertEquals(0, model.getNodeTypeDefinitions().size());

        final ConfigurationNode root = model.getConfigurationRootNode();
        assertEquals("/", root.getPath());
        assertEquals("", root.getName());
        assertNull(root.getParent());
        assertEquals(0, root.getNodes().size());
        assertEquals(2, root.getProperties().size());
    }

    @Test
    public void new_configuration() {
        final ConfigurationImpl configuration = new ConfigurationImpl("c1");
        configuration.addProject("p1");
        final MergedModel model = new MergedModelBuilder().push(configuration).build();

        final ConfigurationImpl consolidated = model.getSortedConfigurations().get(0);
        assertEquals("c1", consolidated.getName());
        assertEquals("p1", consolidated.getModifiableProjects().get(0).getName());
    }

    @Test
    public void separate_configurations() {
        final ConfigurationImpl c1 = new ConfigurationImpl("c1");
        final ConfigurationImpl c2 = new ConfigurationImpl("c2");
        c1.addProject("p1");
        c2.addProject("p2");

        MergedModel model = new MergedModelBuilder().push(c1).push(c2).build();

        List<ConfigurationImpl> configurations = model.getSortedConfigurations();
        assertEquals(2, configurations.size());
        assertEquals("c1", configurations.get(0).getName());
        List<ProjectImpl> projects1 = configurations.get(0).getModifiableProjects();
        assertEquals(1, projects1.size());
        assertEquals("p1", projects1.get(0).getName());

        assertEquals("c2", configurations.get(1).getName());
        List<ProjectImpl> projects2 = configurations.get(1).getModifiableProjects();
        assertEquals(1, projects2.size());
        assertEquals("p2", projects2.get(0).getName());

        // validate 'natural' sorting
        model = new MergedModelBuilder().push(c2).push(c1).build();

        configurations = model.getSortedConfigurations();
        assertEquals(2, configurations.size());
        assertEquals("c1", configurations.get(0).getName());
        projects1 = configurations.get(0).getModifiableProjects();
        assertEquals(1, projects1.size());
        assertEquals("p1", projects1.get(0).getName());

        assertEquals("c2", configurations.get(1).getName());
        projects2 = configurations.get(1).getModifiableProjects();
        assertEquals(1, projects2.size());
        assertEquals("p2", projects2.get(0).getName());
    }

    @Test
    public void merged_configurations() {
        final ConfigurationImpl c1a = new ConfigurationImpl("c1");
        final ConfigurationImpl c1b = new ConfigurationImpl("c1");
        c1a.addProject("p1");
        c1b.addProject("p2");

        MergedModel model = new MergedModelBuilder().push(c1a).push(c1b).build();

        List<ConfigurationImpl> configurations = model.getSortedConfigurations();
        assertEquals(1, configurations.size());
        assertEquals("c1", configurations.get(0).getName());
        List<ProjectImpl> projects = configurations.get(0).getModifiableProjects();
        assertEquals(2, projects.size());
        assertEquals("p1", projects.get(0).getName());
        assertEquals("p2", projects.get(1).getName());

        // validate 'natural' sorting
        model = new MergedModelBuilder().push(c1b).push(c1a).build();

        configurations = model.getSortedConfigurations();
        assertEquals(1, configurations.size());
        assertEquals("c1", configurations.get(0).getName());
        projects = configurations.get(0).getModifiableProjects();
        assertEquals(2, projects.size());
        assertEquals("p1", projects.get(0).getName());
        assertEquals("p2", projects.get(1).getName());
    }

    @Test
    public void dependent_configurations() {
        final ConfigurationImpl c1 = new ConfigurationImpl("c1").addAfter(ImmutableSet.of("c2"));
        final ConfigurationImpl c2 = new ConfigurationImpl("c2");

        MergedModel model = new MergedModelBuilder().push(c1).push(c2).build();
        assertEquals("c2", model.getSortedConfigurations().get(0).getName());
        assertEquals("c1", model.getSortedConfigurations().get(1).getName());

        model = new MergedModelBuilder().push(c2).push(c1).build();
        assertEquals("c2", model.getSortedConfigurations().get(0).getName());
        assertEquals("c1", model.getSortedConfigurations().get(1).getName());
    }

    @Test
    public void merged_configuration_dependencies() {
        final ConfigurationImpl ca1a = new ConfigurationImpl("ca1").addAfter(ImmutableSet.of("cx1", "cx2"));
        final ConfigurationImpl ca1b = new ConfigurationImpl("ca1").addAfter(ImmutableSet.of("cx1", "cx3"));
        final ConfigurationImpl cx1 = new ConfigurationImpl("cx1");
        final ConfigurationImpl cx2 = new ConfigurationImpl("cx2");
        final ConfigurationImpl cx3 = new ConfigurationImpl("cx3").addAfter(ImmutableSet.of("cx2"));

        MergedModel model = new MergedModelBuilder()
                .push(ca1a)
                .push(ca1b)
                .push(cx1)
                .push(cx2)
                .push(cx3)
                .build();

        assertEquals(4, model.getSortedConfigurations().size());
        assertEquals("cx1", model.getSortedConfigurations().get(0).getName());
        assertEquals("cx2", model.getSortedConfigurations().get(1).getName());
        assertEquals("cx3", model.getSortedConfigurations().get(2).getName());
        assertEquals("ca1", model.getSortedConfigurations().get(3).getName());
    }

    @Test
    public void merged_projects() {
        final ConfigurationImpl c1a = new ConfigurationImpl("c1");
        final ConfigurationImpl c1b = new ConfigurationImpl("c1");
        c1a.addProject("p1").addModule("m1");
        c1b.addProject("p1").addModule("m2");

        MergedModel model = new MergedModelBuilder().push(c1a).push(c1b).build();

        List<ConfigurationImpl> configurations = model.getSortedConfigurations();
        assertEquals(1, configurations.size());
        assertEquals("c1", configurations.get(0).getName());
        List<ProjectImpl> projects = configurations.get(0).getModifiableProjects();
        assertEquals(1, projects.size());
        assertEquals("p1", projects.get(0).getName());
        List<ModuleImpl> modules = projects.get(0).getModifiableModules();
        assertEquals(2, modules.size());
        assertEquals("m1", modules.get(0).getName());
        assertEquals("m2", modules.get(1).getName());

        // validate 'natural' sorting
        model = new MergedModelBuilder().push(c1b).push(c1a).build();
        configurations = model.getSortedConfigurations();
        assertEquals(1, configurations.size());
        assertEquals("c1", configurations.get(0).getName());
        projects = configurations.get(0).getModifiableProjects();
        assertEquals(1, projects.size());
        assertEquals("p1", projects.get(0).getName());
        modules = projects.get(0).getModifiableModules();
        assertEquals(2, modules.size());
        assertEquals("m1", modules.get(0).getName());
        assertEquals("m2", modules.get(1).getName());
    }

    @Test
    public void dependent_projects() {
        final ConfigurationImpl c1a = new ConfigurationImpl("c1");
        final ConfigurationImpl c1b = new ConfigurationImpl("c1");
        c1a.addProject("p1").addAfter(ImmutableSet.of("p2"));
        c1b.addProject("p2");

        MergedModel model = new MergedModelBuilder().push(c1a).push(c1b).build();
        List<ConfigurationImpl> configurations = model.getSortedConfigurations();
        List<ProjectImpl> projects = configurations.get(0).getModifiableProjects();
        assertEquals(2, projects.size());
        assertEquals("p2", projects.get(0).getName());
        assertEquals("p1", projects.get(1).getName());

        model = new MergedModelBuilder().push(c1b).push(c1a).build();
        configurations = model.getSortedConfigurations();
        projects = configurations.get(0).getModifiableProjects();
        assertEquals(2, projects.size());
        assertEquals("p2", projects.get(0).getName());
        assertEquals("p1", projects.get(1).getName());
    }

    @Test
    public void merged_project_dependencies() {
        final ConfigurationImpl c1a = new ConfigurationImpl("c1");
        final ConfigurationImpl c1b = new ConfigurationImpl("c1");

        c1a.addProject("pa1").addAfter(ImmutableSet.of("px1", "px2"));
        c1a.addProject("px1");
        c1a.addProject("px3").addAfter(ImmutableSet.of("px2"));
        c1b.addProject("pa1").addAfter(ImmutableSet.of("px1", "px3"));
        c1b.addProject("px2");

        MergedModel model = new MergedModelBuilder()
                .push(c1a)
                .push(c1b)
                .build();
        List<ConfigurationImpl> configurations = model.getSortedConfigurations();
        assertEquals(1, configurations.size());
        List<ProjectImpl> projects = configurations.get(0).getModifiableProjects();
        assertEquals("px1", projects.get(0).getName());
        assertEquals("px2", projects.get(1).getName());
        assertEquals("px3", projects.get(2).getName());
        assertEquals("pa1", projects.get(3).getName());
    }

    @Test
    public void modules_cannot_be_merged() {
        final ConfigurationImpl c1a = new ConfigurationImpl("c1");
        final ConfigurationImpl c1b = new ConfigurationImpl("c1");
        c1a.addProject("p1").addModule("m1");
        c1b.addProject("p1").addModule("m1");

        MergedModelBuilder builder = new MergedModelBuilder().push(c1a);

        try {
            builder.push(c1b);
        } catch (IllegalStateException e) {
            assertEquals("Module c1/p1/m1 already exists while merging projects. Merging of modules is not supported.", e.getMessage());
        }
    }

    @Test
    public void dependent_modules() {
        final ConfigurationImpl c1a = new ConfigurationImpl("c1");
        final ConfigurationImpl c1b = new ConfigurationImpl("c1");
        c1a.addProject("p1").addModule("m1").addAfter(ImmutableSet.of("m2"));
        c1b.addProject("p1").addModule("m2");

        MergedModel model = new MergedModelBuilder().push(c1a).push(c1b).build();

        List<ConfigurationImpl> configurations = model.getSortedConfigurations();
        List<ProjectImpl> projects = configurations.get(0).getModifiableProjects();
        List<ModuleImpl> modules = projects.get(0).getModifiableModules();
        assertEquals(2, modules.size());
        assertEquals("m2", modules.get(0).getName());
        assertEquals("m1", modules.get(1).getName());

        model = new MergedModelBuilder().push(c1b).push(c1a).build();

        configurations = model.getSortedConfigurations();
        projects = configurations.get(0).getModifiableProjects();
        modules = projects.get(0).getModifiableModules();
        assertEquals(2, modules.size());
        assertEquals("m2", modules.get(0).getName());
        assertEquals("m1", modules.get(1).getName());
    }

    @Test
    public void merged_module_dependencies() {
        final ConfigurationImpl c1a = new ConfigurationImpl("c1");
        final ConfigurationImpl c1b = new ConfigurationImpl("c1");

        final ProjectImpl p1a = c1a.addProject("p1");
        final ProjectImpl p1b = c1b.addProject("p1");

        p1a.addModule("ma1").addAfter(ImmutableSet.of("mx1", "mx3"));
        p1a.addModule("mx1");
        p1a.addModule("mx3").addAfter(ImmutableSet.of("mx2"));
        p1b.addModule("mx2");

        MergedModel model = new MergedModelBuilder().push(c1a).push(c1b).build();

        List<ConfigurationImpl> configurations = model.getSortedConfigurations();
        List<ProjectImpl> projects = configurations.get(0).getModifiableProjects();
        List<ModuleImpl> modules = projects.get(0).getModifiableModules();

        assertEquals("mx1", modules.get(0).getName());
        assertEquals("mx2", modules.get(1).getName());
        assertEquals("mx3", modules.get(2).getName());
        assertEquals("ma1", modules.get(3).getName());
    }

    @Test
    public void sort_definitions_of_single_source() throws Exception {
        final ConfigurationImpl c1 = new ConfigurationImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        loadYAMLResource(this.getClass().getClassLoader(), "builder/definition-sorter.yaml", m1);

        MergedModel model = new MergedModelBuilder().push(c1).build();

        assertEquals(1, model.getNamespaceDefinitions().size());
        assertEquals(1, model.getNodeTypeDefinitions().size());

        final List<ContentDefinitionImpl> definitions = model.getSortedConfigurations().get(0)
                .getModifiableProjects().get(0)
                .getModifiableModules().get(0)
                .getContentDefinitions();

        assertEquals(5, definitions.size());
        String roots = definitions.stream().map(d -> d.getNode().getPath()).collect(Collectors.toList()).toString();
        assertEquals("[/a, /a/b, /a/b/a, /a/b/c, /a/b/c/d]", roots);
    }

    @Test
    public void sort_definitions_from_multiple_files() throws Exception {
        final ConfigurationImpl c1 = new ConfigurationImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        loadYAMLResource(this.getClass().getClassLoader(), "builder/definition-sorter.yaml", m1);
        loadYAMLResource(this.getClass().getClassLoader(), "builder/definition-sorter2.yaml", m1);

        MergedModel model = new MergedModelBuilder().push(c1).build();

        String namespaces = model.getNamespaceDefinitions().stream().map(NamespaceDefinitionImpl::getPrefix).collect(Collectors.toList()).toString();
        assertEquals("[myhippoproject, hishippoproject]", namespaces);
        assertEquals(1, model.getNodeTypeDefinitions().size());

        final List<ContentDefinitionImpl> definitions = model.getSortedConfigurations().get(0)
                .getModifiableProjects().get(0)
                .getModifiableModules().get(0)
                .getContentDefinitions();

        assertEquals(9, definitions.size());
        String roots = definitions.stream().map(d -> d.getNode().getPath()).collect(Collectors.toList()).toString();
        assertEquals("[/a, /a/a, /a/b, /a/b/a, /a/b/c, /a/b/c/b, /a/b/c/d, /a/b/c/d/e, /b]", roots);
    }

    @Test
    public void sort_definitions_from_multiple_files_reversed_load_order() throws Exception {
        final ConfigurationImpl c1 = new ConfigurationImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        loadYAMLResource(this.getClass().getClassLoader(), "builder/definition-sorter2.yaml", m1);
        loadYAMLResource(this.getClass().getClassLoader(), "builder/definition-sorter.yaml", m1);

        MergedModel model = new MergedModelBuilder().push(c1).build();

        String namespaces = model.getNamespaceDefinitions().stream().map(NamespaceDefinitionImpl::getPrefix).collect(Collectors.toList()).toString();
        assertEquals("[myhippoproject, hishippoproject]", namespaces);
        assertEquals(1, model.getNodeTypeDefinitions().size());

        final List<ContentDefinitionImpl> definitions = model.getSortedConfigurations().get(0)
                .getModifiableProjects().get(0)
                .getModifiableModules().get(0)
                .getContentDefinitions();

        assertEquals(9, definitions.size());
        String roots = definitions.stream().map(d -> d.getNode().getPath()).collect(Collectors.toList()).toString();
        assertEquals("[/a, /a/a, /a/b, /a/b/a, /a/b/c, /a/b/c/b, /a/b/c/d, /a/b/c/d/e, /b]", roots);
    }

    @Test
    public void sources_with_same_root() throws Exception {
        final ConfigurationImpl c1 = new ConfigurationImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        final String yaml = "instructions:\n"
                + "  - config:\n"
                + "    - /a/b:\n"
                + "      - propertyX: blaX";

        loadYAMLResource(this.getClass().getClassLoader(), "builder/definition-sorter.yaml", m1);
        loadYAMLString(yaml, m1);

        MergedModelBuilder builder = new MergedModelBuilder();

        try {
            builder.push(c1);
            fail("Expect IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Duplicate content root paths '/a/b' in module 'm1'.", e.getMessage());
        }
    }

    @Test
    public void reject_node_type_definitions_in_multiple_sources_of_module() throws Exception {
        final ConfigurationImpl c1 = new ConfigurationImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        final String yaml = "instructions:\n"
                + "  - cnd:\n"
                + "    - dummy CND content";

        loadYAMLResource(this.getClass().getClassLoader(), "builder/definition-sorter.yaml", m1);
        loadYAMLString(yaml, m1);

        MergedModelBuilder builder = new MergedModelBuilder();

        try {
            builder.push(c1);
            fail("Expect IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("CNDs are specified in multiple sources of a module: c1/p1/m1 [string] and c1/p1/m1 [builder/definition-sorter.yaml]. For proper ordering, they must be specified in a single source.", e.getMessage());
        }
    }

    @Test
    public void assert_insertion_order_for_cnd() throws Exception {
        final ConfigurationImpl c1 = new ConfigurationImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        final String yaml = "instructions:\n"
                + "  - cnd:\n"
                + "    - dummy CND content\n"
                + "    - alphabetically earlier dummy CND";

        loadYAMLString(yaml, m1);

        MergedModel model = new MergedModelBuilder().push(c1).build();

        String nodeTypes = model.getNodeTypeDefinitions().stream().map(NodeTypeDefinition::getValue).collect(Collectors.toList()).toString();
        assertEquals("[dummy CND content, alphabetically earlier dummy CND]", nodeTypes);
    }
}
