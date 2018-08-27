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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.onehippo.cm.model.impl.definition.ConfigDefinitionImpl;
import org.onehippo.cm.model.impl.definition.NamespaceDefinitionImpl;
import org.onehippo.cm.model.impl.definition.WebFileBundleDefinitionImpl;
import org.onehippo.cm.model.impl.tree.ConfigurationNodeImpl;

import com.google.common.collect.ImmutableSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class ConfigurationModelImplTest {

    @Test
    public void empty_builder() {
        final ConfigurationModelImpl model = new ConfigurationModelImpl().build();

        assertEquals(0, model.getSortedGroups().size());
        assertEquals(0, model.getNamespaceDefinitions().size());

        final ConfigurationNodeImpl root = model.getConfigurationRootNode();
        assertEquals("/", root.getJcrPath().toString());
        assertEquals("", root.getName());
        assertNull(root.getParent());
        assertEquals(0, root.getNodes().size());
        assertEquals(3, root.getProperties().size());
    }

    @Test
    public void new_group() {
        final GroupImpl group = new GroupImpl("c1");
        group.addProject("p1");
        final ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(group).build();

        final GroupImpl consolidated = model.getSortedGroups().get(0);
        assertEquals("c1", consolidated.getName());
        assertEquals("p1", consolidated.getProjects().get(0).getName());
    }

    @Test
    public void separate_groups() {
        final GroupImpl c1 = new GroupImpl("c1");
        final GroupImpl c2 = new GroupImpl("c2");
        c1.addProject("p1");
        c2.addProject("p2");

        ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1).addGroup(c2).build();

        List<GroupImpl> groups = model.getSortedGroups();
        assertEquals(2, groups.size());
        assertEquals("c1", groups.get(0).getName());
        List<ProjectImpl> projects1 = groups.get(0).getProjects();
        assertEquals(1, projects1.size());
        assertEquals("p1", projects1.get(0).getName());

        assertEquals("c2", groups.get(1).getName());
        List<ProjectImpl> projects2 = groups.get(1).getProjects();
        assertEquals(1, projects2.size());
        assertEquals("p2", projects2.get(0).getName());

        // validate 'natural' sorting
        model = new ConfigurationModelImpl().addGroup(c2).addGroup(c1).build();

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

        ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1a).addGroup(c1b).build();

        List<GroupImpl> groups = model.getSortedGroups();
        assertEquals(1, groups.size());
        assertEquals("c1", groups.get(0).getName());
        List<ProjectImpl> projects = groups.get(0).getProjects();
        assertEquals(2, projects.size());
        assertEquals("p1", projects.get(0).getName());
        assertEquals("p2", projects.get(1).getName());

        // validate 'natural' sorting
        model = new ConfigurationModelImpl().addGroup(c1b).addGroup(c1a).build();

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

        ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1).addGroup(c2).build();
        assertEquals("c2", model.getSortedGroups().get(0).getName());
        assertEquals("c1", model.getSortedGroups().get(1).getName());

        model = new ConfigurationModelImpl().addGroup(c2).addGroup(c1).build();
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

        ConfigurationModelImpl model = new ConfigurationModelImpl()
                .addGroup(ca1a)
                .addGroup(ca1b)
                .addGroup(cx1)
                .addGroup(cx2)
                .addGroup(cx3)
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

        ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1a).addGroup(c1b).build();

        List<GroupImpl> groups = model.getSortedGroups();
        assertEquals(1, groups.size());
        assertEquals("c1", groups.get(0).getName());
        List<ProjectImpl> projects = groups.get(0).getProjects();
        assertEquals(1, projects.size());
        assertEquals("p1", projects.get(0).getName());
        List<ModuleImpl> modules = projects.get(0).getModules();
        assertEquals(2, modules.size());
        assertEquals("m1", modules.get(0).getName());
        assertEquals("m2", modules.get(1).getName());

        // validate 'natural' sorting
        model = new ConfigurationModelImpl().addGroup(c1b).addGroup(c1a).build();
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

        ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1a).addGroup(c1b).build();
        List<GroupImpl> groups = model.getSortedGroups();
        List<ProjectImpl> projects = groups.get(0).getProjects();
        assertEquals(2, projects.size());
        assertEquals("p2", projects.get(0).getName());
        assertEquals("p1", projects.get(1).getName());

        model = new ConfigurationModelImpl().addGroup(c1b).addGroup(c1a).build();
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

        ConfigurationModelImpl model = new ConfigurationModelImpl()
                .addGroup(c1a)
                .addGroup(c1b)
                .build();
        List<GroupImpl> groups = model.getSortedGroups();
        assertEquals(1, groups.size());
        List<ProjectImpl> projects = groups.get(0).getProjects();
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

        ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1a);

        try {
            model.addGroup(c1b);
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

        ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1a).addGroup(c1b).build();

        List<GroupImpl> groups = model.getSortedGroups();
        List<ProjectImpl> projects = groups.get(0).getProjects();
        List<ModuleImpl> modules = projects.get(0).getModules();
        assertEquals(2, modules.size());
        assertEquals("m2", modules.get(0).getName());
        assertEquals("m1", modules.get(1).getName());

        model = new ConfigurationModelImpl().addGroup(c1b).addGroup(c1a).build();

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

        ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1a).addGroup(c1b).build();

        List<GroupImpl> groups = model.getSortedGroups();
        List<ProjectImpl> projects = groups.get(0).getProjects();
        List<ModuleImpl> modules = projects.get(0).getModules();

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

        ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1).build();

        assertEquals(2, model.getNamespaceDefinitions().size());

        final List<ConfigDefinitionImpl> definitions = getConfigDefinitionsFromFirstModule(model);

        assertEquals(5, definitions.size());
        String roots = definitions.stream().map(d -> d.getNode().getJcrPath()).collect(Collectors.toList()).toString();
        assertEquals("[/a, /a/b, /a/b/a, /a/b/c, /a/b/c/d]", roots);
    }

    private List<ConfigDefinitionImpl> getConfigDefinitionsFromFirstModule(final ConfigurationModelImpl configurationModel) {
        final ModuleImpl module = configurationModel.getSortedGroups().get(0).getProjects().get(0).getModules().get(0);
        return new ArrayList<>(module.getConfigDefinitions());
    }

    @Test
    public void sort_definitions_from_multiple_files() throws Exception {
        final GroupImpl c1 = new GroupImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/definition-sorter.yaml", m1);
        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/definition-sorter2.yaml", m1);

        ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1).build();

        String namespaces = model.getNamespaceDefinitions().stream().map(NamespaceDefinitionImpl::getPrefix).collect(Collectors.toList()).toString();
        assertEquals("[myhippoproject, hishippoproject]", namespaces);

        final List<ConfigDefinitionImpl> definitions = getConfigDefinitionsFromFirstModule(model);

        assertEquals(9, definitions.size());
        String roots = definitions.stream().map(d -> d.getNode().getJcrPath()).collect(Collectors.toList()).toString();
        assertEquals("[/a, /a/a, /a/b, /a/b/a, /a/b/c, /a/b/c/b, /a/b/c/d, /a/b/c/d/e, /b]", roots);
    }

    @Test
    public void sort_definitions_from_multiple_files_reversed_load_order() throws Exception {
        final GroupImpl c1 = new GroupImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/definition-sorter2.yaml", m1);
        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/definition-sorter.yaml", m1);

        ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1).build();

        String namespaces = model.getNamespaceDefinitions().stream().map(NamespaceDefinitionImpl::getPrefix).collect(Collectors.toList()).toString();
        assertEquals("[myhippoproject, hishippoproject]", namespaces);

        final List<ConfigDefinitionImpl> definitions = getConfigDefinitionsFromFirstModule(model);

        assertEquals(9, definitions.size());
        String roots = definitions.stream().map(d -> d.getNode().getJcrPath()).collect(Collectors.toList()).toString();
        assertEquals("[/a, /a/a, /a/b, /a/b/a, /a/b/c, /a/b/c/b, /a/b/c/d, /a/b/c/d/e, /b]", roots);
    }

    @Test
    public void build_model_using_extension_module() throws Exception {
        final GroupImpl c1 = new GroupImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");
        final ModuleImpl m2 = c1.addProject("p2").addModule("m2");
        m2.setHcmSiteName("m2");

        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/definition-sorter.yaml", m1);
        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/definition-sorter2.yaml", m2);

        ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1).build();
        final ConfigurationNodeImpl nodeFromSecondModule = model.resolveNode("/b");
        assertNotNull(nodeFromSecondModule);

        final List<ConfigDefinitionImpl> definitions = getConfigDefinitionsFromFirstModule(model);
        assertEquals(5, definitions.size());
    }

    @Test
    public void sort_definitions_with_sns() throws Exception {
        final GroupImpl c1 = new GroupImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/definition-sorter-sns.yaml", m1);

        ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1).build();

        final List<ConfigDefinitionImpl> definitions = getConfigDefinitionsFromFirstModule(model);

        assertEquals(11, definitions.size());
        String roots = definitions.stream().map(d -> d.getNode().getJcrPath()).collect(Collectors.toList()).toString();
        assertEquals("[/a, /a/sns, /a/sns[2], /a/sns[3], /a/sns[4], /a/sns[5], /a/sns[6], /a/sns[7], /a/sns[8], /a/sns[9], /a/sns[10]]", roots);
    }

    @Test
    public void reject_duplicate_sns_definition() throws Exception {
        final GroupImpl c1 = new GroupImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/definition-sorter-sns-dup.yaml", m1);

        try {
            new ConfigurationModelImpl().addGroup(c1).build();
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("Duplicate definition root paths '/a/sns' in module 'm1' in source files 'c1/p1/m1 [config: builder/definition-sorter-sns-dup.yaml]' and 'c1/p1/m1 [config: builder/definition-sorter-sns-dup.yaml]'.", e.getMessage());
        }
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

        ConfigurationModelImpl model = new ConfigurationModelImpl();

        try {
            model.addGroup(c1);
            fail("Expect IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Duplicate definition root paths '/a/b' in module 'm1' in source files 'c1/p1/m1 [config: string]' and 'c1/p1/m1 [config: builder/definition-sorter.yaml]'.", e.getMessage());
        }
    }

    @Test
    public void reject_node_type_definitions_in_multiple_sources_of_module() throws Exception {
        final GroupImpl c1 = new GroupImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        final String yaml = "definitions:\n"
                + "  namespace:\n"
                + "    foo:\n"
                + "      uri: foo";

        ModelTestUtils.loadYAMLResource(this.getClass().getClassLoader(), "builder/definition-sorter.yaml", m1);
        ModelTestUtils.loadYAMLString(yaml, m1);

        ConfigurationModelImpl model = new ConfigurationModelImpl();

        try {
            model.addGroup(c1);
            fail("Expect IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Namespaces are specified in multiple sources of a module: 'c1/p1/m1 [config: string]' and 'c1/p1/m1 [config: builder/definition-sorter.yaml]'. To ensure proper ordering, they must be specified in a single source.", e.getMessage());
        }
    }

    @Test
    public void assert_insertion_order_for_namespace() throws Exception {
        final GroupImpl c1 = new GroupImpl("c1");
        final ModuleImpl m1 = c1.addProject("p1").addModule("m1");

        final String yaml = "definitions:\n"
                + "  namespace:\n"
                + "    foo:\n"
                + "      uri: foo\n"
                + "    bar:\n"
                + "      uri: bar\n";

        ModelTestUtils.loadYAMLString(yaml, m1);

        ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1).build();

        String prefixes = model.getNamespaceDefinitions().stream().map(NamespaceDefinitionImpl::getPrefix).collect(Collectors.toList()).toString();
        assertEquals("[foo, bar]", prefixes);
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

        final ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1).build();

        final String webFileBundles = model.getWebFileBundleDefinitions().stream()
                .map(WebFileBundleDefinitionImpl::getName).collect(Collectors.toList()).toString();
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

        final ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1).addGroup(c2).build();

        final String webFileBundles = model.getWebFileBundleDefinitions().stream()
                .map(WebFileBundleDefinitionImpl::getName).collect(Collectors.toList()).toString();
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

        final ConfigurationModelImpl model = new ConfigurationModelImpl().addGroup(c1).addGroup(c2);
        try {
            model.build();
            fail("Expect IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Duplicate web file bundle with name 'name' found in source files 'c1/p1/m1 [config: string]' and 'c2/p2/m2 [config: string]'.",
                    e.getMessage());
        }
    }

}
