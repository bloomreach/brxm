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

package org.onehippo.cm.model.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.ConfigurationNode;
import org.onehippo.cm.model.ContentDefinition;
import org.onehippo.cm.model.Group;
import org.onehippo.cm.model.Module;
import org.onehippo.cm.model.NamespaceDefinition;
import org.onehippo.cm.model.NodeTypeDefinition;
import org.onehippo.cm.model.Project;
import org.onehippo.cm.model.WebFileBundleDefinition;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.ModelTestUtils;

import com.google.common.collect.ImmutableSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.onehippo.cm.model.impl.ModelTestUtils.loadYAMLResource;
import static org.onehippo.cm.model.impl.ModelTestUtils.loadYAMLString;

public class ConfigurationModelBuilderTest extends AbstractBuilderBaseTest {

    @Test
    public void empty_builder() {
        final ConfigurationModel model = new ConfigurationModelBuilder().build();

        assertEquals(0, model.getSortedGroups().size());
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
    public void new_group() {
        final GroupImpl group = new GroupImpl("c1");
        group.addProject("p1");
        final ConfigurationModel model = new ConfigurationModelBuilder().push(group).build();

        final Group consolidated = model.getSortedGroups().get(0);
        assertEquals("c1", consolidated.getName());
        assertEquals("p1", consolidated.getProjects().get(0).getName());
    }

    @Test
    public void separate_groups() {
        final GroupImpl c1 = new GroupImpl("c1");
        final GroupImpl c2 = new GroupImpl("c2");
        c1.addProject("p1");
        c2.addProject("p2");

        ConfigurationModel model = new ConfigurationModelBuilder().push(c1).push(c2).build();

        List<Group> groups = model.getSortedGroups();
        assertEquals(2, groups.size());
        assertEquals("c1", groups.get(0).getName());
        List<Project> projects1 = groups.get(0).getProjects();
        assertEquals(1, projects1.size());
        assertEquals("p1", projects1.get(0).getName());

        assertEquals("c2", groups.get(1).getName());
        List<Project> projects2 = groups.get(1).getProjects();
        assertEquals(1, projects2.size());
        assertEquals("p2", projects2.get(0).getName());

        // validate 'natural' sorting
        model = new ConfigurationModelBuilder().push(c2).push(c1).build();

        groups = model.getSortedGroups();
        assertEquals(2, groups.size());
        assertEquals("c1", groups.get(0).getName());
        projects1 = groups.get(0).getProjects();
        assertEquals(1, projects1.size());
        assertEquals("p1", projects1.get(0).getName());

        assertEquals("c2", groups.get(1).getName());
        projects2 = groups.get(1).getProjects();
        assertEquals(1, projects2.size());
        assertEquals("p2", projects2.get(0).getName());
    }

    @Test
    public void merged_groups() {
        final GroupImpl c1a = new GroupImpl("c1");
        final GroupImpl c1b = new GroupImpl("c1");
        c1a.addProject("p1");
        c1b.addProject("p2");

        ConfigurationModel model = new ConfigurationModelBuilder().push(c1a).push(c1b).build();

        List<Group> groups = model.getSortedGroups();
        assertEquals(1, groups.size());
        assertEquals("c1", groups.get(0).getName());
        List<Project> projects = groups.get(0).getProjects();
        assertEquals(2, projects.size());
        assertEquals("p1", projects.get(0).getName());
        assertEquals("p2", projects.get(1).getName());

        // validate 'natural' sorting
        model = new ConfigurationModelBuilder().push(c1b).push(c1a).build();

        groups = model.getSortedGroups();
        assertEquals(1, groups.size());
        assertEquals("c1", groups.get(0).getName());
        projects = groups.get(0).getProjects();
        assertEquals(2, projects.size());
        assertEquals("p1", projects.get(0).getName());
        assertEquals("p2", projects.get(1).getName());
    }

    @Test
    public void dependent_groups() {
        final GroupImpl c1 = new GroupImpl("c1").addAfter(ImmutableSet.of("c2"));
        final GroupImpl c2 = new GroupImpl("c2");

        ConfigurationModel model = new ConfigurationModelBuilder().push(c1).push(c2).build();
        assertEquals("c2", model.getSortedGroups().get(0).getName());
        assertEquals("c1", model.getSortedGroups().get(1).getName());

        model = new ConfigurationModelBuilder().push(c2).push(c1).build();
        assertEquals("c2", model.getSortedGroups().get(0).getName());
        assertEquals("c1", model.getSortedGroups().get(1).getName());
    }

    @Test
    public void merged_group_dependencies() {
        final GroupImpl ca1a = new GroupImpl("ca1").addAfter(ImmutableSet.of("cx1", "cx2"));
        final GroupImpl ca1b = new GroupImpl("ca1").addAfter(ImmutableSet.of("cx1", "cx3"));
        final GroupImpl cx1 = new GroupImpl("cx1");
        final GroupImpl cx2 = new GroupImpl("cx2");
        final GroupImpl cx3 = new GroupImpl("cx3").addAfter(ImmutableSet.of("cx2"));

        ConfigurationModel model = new ConfigurationModelBuilder()
                .push(ca1a)
                .push(ca1b)
                .push(cx1)
                .push(cx2)
                .push(cx3)
                .build();

        assertEquals(4, model.getSortedGroups().size());
        assertEquals("cx1", model.getSortedGroups().get(0).getName());
        assertEquals("cx2", model.getSortedGroups().get(1).getName());
        assertEquals("cx3", model.getSortedGroups().get(2).getName());
        assertEquals("ca1", model.getSortedGroups().get(3).getName());
    }

    @Test
    public void merged_projects() {
        final GroupImpl c1a = new GroupImpl("c1");
        final GroupImpl c1b = new GroupImpl("c1");
        c1a.addProject("p1").addModule("m1");
        c1b.addProject("p1").addModule("m2");

        ConfigurationModel model = new ConfigurationModelBuilder().push(c1a).push(c1b).build();

        List<Group> groups = model.getSortedGroups();
        assertEquals(1, groups.size());
        assertEquals("c1", groups.get(0).getName());
        List<Project> projects = groups.get(0).getProjects();
        assertEquals(1, projects.size());
        assertEquals("p1", projects.get(0).getName());
        List<Module> modules = projects.get(0).getModules();
        assertEquals(2, modules.size());
        assertEquals("m1", modules.get(0).getName());
        assertEquals("m2", modules.get(1).getName());

        // validate 'natural' sorting
        model = new ConfigurationModelBuilder().push(c1b).push(c1a).build();
        groups = model.getSortedGroups();
        assertEquals(1, groups.size());
        assertEquals("c1", groups.get(0).getName());
        projects = groups.get(0).getProjects();
        assertEquals(1, projects.size());
        assertEquals("p1", projects.get(0).getName());
        modules = projects.get(0).getModules();
        assertEquals(2, modules.size());
        assertEquals("m1", modules.get(0).getName());
        assertEquals("m2", modules.get(1).getName());
    }

    @Test
    public void dependent_projects() {
        final GroupImpl c1a = new GroupImpl("c1");
        final GroupImpl c1b = new GroupImpl("c1");
        c1a.addProject("p1").addAfter(ImmutableSet.of("p2"));
        c1b.addProject("p2");

        ConfigurationModel model = new ConfigurationModelBuilder().push(c1a).push(c1b).build();
        List<Group> groups = model.getSortedGroups();
        List<Project> projects = groups.get(0).getProjects();
        assertEquals(2, projects.size());
        assertEquals("p2", projects.get(0).getName());
        assertEquals("p1", projects.get(1).getName());

        model = new ConfigurationModelBuilder().push(c1b).push(c1a).build();
        groups = model.getSortedGroups();
        projects = groups.get(0).getProjects();
        assertEquals(2, projects.size());
        assertEquals("p2", projects.get(0).getName());
        assertEquals("p1", projects.get(1).getName());
    }

    @Test
    public void merged_project_dependencies() {
        final GroupImpl c1a = new GroupImpl("c1");
        final GroupImpl c1b = new GroupImpl("c1");

        c1a.addProject("pa1").addAfter(ImmutableSet.of("px1", "px2"));
        c1a.addProject("px1");
        c1a.addProject("px3").addAfter(ImmutableSet.of("px2"));
        c1b.addProject("pa1").addAfter(ImmutableSet.of("px1", "px3"));
        c1b.addProject("px2");

        ConfigurationModel model = new ConfigurationModelBuilder()
                .push(c1a)
                .push(c1b)
                .build();
        List<Group> groups = model.getSortedGroups();
        assertEquals(1, groups.size());
        List<Project> projects = groups.get(0).getProjects();
        assertEquals("px1", projects.get(0).getName());
        assertEquals("px2", projects.get(1).getName());
        assertEquals("px3", projects.get(2).getName());
        assertEquals("pa1", projects.get(3).getName());
    }

    @Test
    public void modules_cannot_be_merged() {
        final GroupImpl c1a = new GroupImpl("c1");
        final GroupImpl c1b = new GroupImpl("c1");
        c1a.addProject("p1").addModule("m1");
        c1b.addProject("p1").addModule("m1");

        ConfigurationModelBuilder builder = new ConfigurationModelBuilder().push(c1a);

        try {
            builder.push(c1b);
        } catch (IllegalStateException e) {
            assertEquals("Module c1/p1/m1 already exists while merging projects. Merging of modules is not supported.", e.getMessage());
        }
    }

    @Test
    public void dependent_modules() {
        final GroupImpl c1a = new GroupImpl("c1");
        final GroupImpl c1b = new GroupImpl("c1");
        c1a.addProject("p1").addModule("m1").addAfter(ImmutableSet.of("m2"));
        c1b.addProject("p1").addModule("m2");

        ConfigurationModel model = new ConfigurationModelBuilder().push(c1a).push(c1b).build();

        List<Group> groups = model.getSortedGroups();
        List<Project> projects = groups.get(0).getProjects();
        List<Module> modules = projects.get(0).getModules();
        assertEquals(2, modules.size());
        assertEquals("m2", modules.get(0).getName());
        assertEquals("m1", modules.get(1).getName());

        model = new ConfigurationModelBuilder().push(c1b).push(c1a).build();

        groups = model.getSortedGroups();
        projects = groups.get(0).getProjects();
        modules = projects.get(0).getModules();
        assertEquals(2, modules.size());
        assertEquals("m2", modules.get(0).getName());
        assertEquals("m1", modules.get(1).getName());
    }

    @Test
    public void merged_module_dependencies() {
        final GroupImpl c1a = new GroupImpl("c1");
        final GroupImpl c1b = new GroupImpl("c1");

        final ProjectImpl p1a = c1a.addProject("p1");
        final ProjectImpl p1b = c1b.addProject("p1");

        p1a.addModule("ma1").addAfter(ImmutableSet.of("mx1", "mx3"));
        p1a.addModule("mx1");
        p1a.addModule("mx3").addAfter(ImmutableSet.of("mx2"));
        p1b.addModule("mx2");

        ConfigurationModel model = new ConfigurationModelBuilder().push(c1a).push(c1b).build();

        List<Group> groups = model.getSortedGroups();
        List<Project> projects = groups.get(0).getProjects();
        List<Module> modules = projects.get(0).getModules();

        assertEquals("mx1", modules.get(0).getName());
        assertEquals("mx2", modules.get(1).getName());
        assertEquals("mx3", modules.get(2).getName());
        assertEquals("ma1", modules.get(3).getName());
    }

    @Test
    public void sort_definitions_of_single_source() throws Exception {
        final GroupImpl c1 = new GroupImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/definition-sorter.yaml", m1);

        ConfigurationModel model = new ConfigurationModelBuilder().push(c1).build();

        assertEquals(1, model.getNamespaceDefinitions().size());
        assertEquals(1, model.getNodeTypeDefinitions().size());

        final List<ContentDefinition> definitions = getContentDefinitionsFromFirstModule(model);

        assertEquals(5, definitions.size());
        String roots = definitions.stream().map(d -> d.getNode().getPath()).collect(Collectors.toList()).toString();
        assertEquals("[/a, /a/b, /a/b/a, /a/b/c, /a/b/c/d]", roots);
    }

    private List<ContentDefinition> getContentDefinitionsFromFirstModule(final ConfigurationModel configurationModel) {
        final Module module = configurationModel.getSortedGroups().get(0).getProjects().get(0).getModules().get(0);
        return new ArrayList<>(((ModuleImpl) module).getConfigDefinitions());
    }

    @Test
    public void sort_definitions_from_multiple_files() throws Exception {
        final GroupImpl c1 = new GroupImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/definition-sorter.yaml", m1);
        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/definition-sorter2.yaml", m1);

        ConfigurationModel model = new ConfigurationModelBuilder().push(c1).build();

        String namespaces = model.getNamespaceDefinitions().stream().map(NamespaceDefinition::getPrefix).collect(Collectors.toList()).toString();
        assertEquals("[myhippoproject, hishippoproject]", namespaces);
        assertEquals(1, model.getNodeTypeDefinitions().size());

        final List<ContentDefinition> definitions = getContentDefinitionsFromFirstModule(model);

        assertEquals(9, definitions.size());
        String roots = definitions.stream().map(d -> d.getNode().getPath()).collect(Collectors.toList()).toString();
        assertEquals("[/a, /a/a, /a/b, /a/b/a, /a/b/c, /a/b/c/b, /a/b/c/d, /a/b/c/d/e, /b]", roots);
    }

    @Test
    public void sort_definitions_from_multiple_files_reversed_load_order() throws Exception {
        final GroupImpl c1 = new GroupImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/definition-sorter2.yaml", m1);
        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/definition-sorter.yaml", m1);

        ConfigurationModel model = new ConfigurationModelBuilder().push(c1).build();

        String namespaces = model.getNamespaceDefinitions().stream().map(NamespaceDefinition::getPrefix).collect(Collectors.toList()).toString();
        assertEquals("[myhippoproject, hishippoproject]", namespaces);
        assertEquals(1, model.getNodeTypeDefinitions().size());

        final List<ContentDefinition> definitions = getContentDefinitionsFromFirstModule(model);

        assertEquals(9, definitions.size());
        String roots = definitions.stream().map(d -> d.getNode().getPath()).collect(Collectors.toList()).toString();
        assertEquals("[/a, /a/a, /a/b, /a/b/a, /a/b/c, /a/b/c/b, /a/b/c/d, /a/b/c/d/e, /b]", roots);
    }

    @Test
    public void sources_with_same_root() throws Exception {
        final GroupImpl c1 = new GroupImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a/b:\n"
                + "      propertyX: blaX";

        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/definition-sorter.yaml", m1);
        ModelTestUtils.loadYAMLString(yaml, m1);

        ConfigurationModelBuilder builder = new ConfigurationModelBuilder();

        try {
            builder.push(c1);
            fail("Expect IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Duplicate content root paths '/a/b' in module 'm1' in source files 'c1/p1/m1 [string]' and 'c1/p1/m1 [builder/definition-sorter.yaml]'.", e.getMessage());
        }
    }

    @Test
    public void reject_node_type_definitions_in_multiple_sources_of_module() throws Exception {
        final GroupImpl c1 = new GroupImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        final String yaml = "definitions:\n"
                + "  cnd:\n"
                + "  - dummy CND content";

        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/definition-sorter.yaml", m1);
        ModelTestUtils.loadYAMLString(yaml, m1);

        ConfigurationModelBuilder builder = new ConfigurationModelBuilder();

        try {
            builder.push(c1);
            fail("Expect IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("CNDs are specified in multiple sources of a module: 'c1/p1/m1 [string]' and 'c1/p1/m1 [builder/definition-sorter.yaml]'. For proper ordering, they must be specified in a single source.", e.getMessage());
        }
    }

    @Test
    public void assert_insertion_order_for_cnd() throws Exception {
        final GroupImpl c1 = new GroupImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        final String yaml = "definitions:\n"
                + "  cnd:\n"
                + "  - dummy CND content\n"
                + "  - alphabetically earlier dummy CND";

        ModelTestUtils.loadYAMLString(yaml, m1);

        ConfigurationModel model = new ConfigurationModelBuilder().push(c1).build();

        String nodeTypes = model.getNodeTypeDefinitions().stream().map(NodeTypeDefinition::getValue).collect(Collectors.toList()).toString();
        assertEquals("[dummy CND content, alphabetically earlier dummy CND]", nodeTypes);
    }

    @Test
    public void assert_insertion_order_for_webfile_bundles() throws Exception {
        final GroupImpl c1 = new GroupImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        final String yaml = "definitions:\n"
                + "  webfilebundle:\n"
                + "  - dummy\n"
                + "  - another";

        ModelTestUtils.loadYAMLString(yaml, m1);

        final ConfigurationModel model = new ConfigurationModelBuilder().push(c1).build();

        final String webFileBundles = model.getWebFileBundleDefinitions().stream()
                .map(WebFileBundleDefinition::getName).collect(Collectors.toList()).toString();
        assertEquals("[dummy, another]", webFileBundles);
    }

    @Test
    public void assert_insertion_order_for_webfile_bundles_over_modules() throws Exception {
        final GroupImpl c1 = new GroupImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        final String yaml1 = "definitions:\n"
                + "  webfilebundle:\n"
                + "  - dummy\n";

        ModelTestUtils.loadYAMLString(yaml1, m1);

        final GroupImpl c2 = new GroupImpl("c2");
        final ModuleImpl m2 = c2.addProject("p2").addModule("m2");

        final String yaml2 = "definitions:\n"
                + "  webfilebundle:\n"
                + "  - another";

        ModelTestUtils.loadYAMLString(yaml2, m2);

        final ConfigurationModel model = new ConfigurationModelBuilder().push(c1).push(c2).build();

        final String webFileBundles = model.getWebFileBundleDefinitions().stream()
                .map(WebFileBundleDefinition::getName).collect(Collectors.toList()).toString();
        assertEquals("[dummy, another]", webFileBundles);
    }

    @Test
    public void reject_identical_names_for_webfile_bundles_over_modules() throws Exception {
        final GroupImpl c1 = new GroupImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        final String yaml = "definitions:\n"
                + "  webfilebundle:\n"
                + "  - name";

        ModelTestUtils.loadYAMLString(yaml, m1);

        final GroupImpl c2 = new GroupImpl("c2");
        final ModuleImpl m2 = c2.addProject("p2").addModule("m2");

        ModelTestUtils.loadYAMLString(yaml, m2);

        final ConfigurationModelBuilder builder = new ConfigurationModelBuilder().push(c1).push(c2);
        try {
            builder.build();
            fail("Expect IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Duplicate web file bundle with name 'name' found in source files 'c1/p1/m1 [string]' and 'c2/p2/m2 [string]'.",
                    e.getMessage());
        }
    }

}
