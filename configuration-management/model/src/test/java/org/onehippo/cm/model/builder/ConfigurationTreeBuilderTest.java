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

import java.util.List;

import org.junit.Test;
import org.onehippo.cm.model.ConfigurationItemCategory;
import org.onehippo.cm.model.ConfigurationNode;
import org.onehippo.cm.model.ConfigurationProperty;
import org.onehippo.cm.model.Definition;
import org.onehippo.cm.model.PropertyType;
import org.onehippo.cm.model.ValueType;
import org.onehippo.cm.model.impl.ConfigurationNodeImpl;
import org.onehippo.cm.model.impl.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.ModelTestUtils;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.cm.model.impl.ModelTestUtils.parseNoSort;

public class ConfigurationTreeBuilderTest extends AbstractBuilderBaseTest {

    private final ConfigurationTreeBuilder builder = new ConfigurationTreeBuilder();

    @Test
    public void simple_single_definition() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property1: bla1\n"
                + "      property2: bla2";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);
        final ContentDefinitionImpl definition = (ContentDefinitionImpl)definitions.get(0);
        builder.push(definition);
        final ConfigurationNodeImpl root = builder.build();

        assertEquals("[jcr:primaryType, jcr:mixinTypes]", sortedCollectionToString(root.getProperties()));
        assertEquals("[a[1]]", sortedCollectionToString(root.getNodes()));
        final ConfigurationNode a = root.getNodes().get("a[1]");
        assertEquals("[jcr:primaryType, property1, property2]", sortedCollectionToString(a.getProperties()));
        assertEquals("[]", sortedCollectionToString(a.getNodes()));
    }

    @Test
    public void complex_single_definition() throws Exception {
        final String yaml
                = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property2: bla2\n"
                + "      property1: bla1\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        /d:\n"
                + "          jcr:primaryType: foo\n"
                + "          property4: bla4\n"
                + "        property3: bla3\n"
                + "        /c:\n"
                + "          jcr:primaryType: foo\n"
                + "          property5: bla5";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);
        final ContentDefinitionImpl definition = (ContentDefinitionImpl)definitions.get(0);
        builder.push(definition);
        final ConfigurationNodeImpl root = builder.build();

        assertEquals("[jcr:primaryType, jcr:mixinTypes]", sortedCollectionToString(root.getProperties()));
        assertEquals("[a[1]]", sortedCollectionToString(root.getNodes()));
        final ConfigurationNode a = root.getNodes().get("a[1]");
        assertEquals("[jcr:primaryType, property2, property1]", sortedCollectionToString(a.getProperties()));
        assertEquals("[b[1]]", sortedCollectionToString(a.getNodes()));
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[jcr:primaryType, property3]", sortedCollectionToString(b.getProperties()));
        assertEquals("[d[1], c[1]]", sortedCollectionToString(b.getNodes()));
        final ConfigurationNode c = b.getNodes().get("c[1]");
        assertEquals("[jcr:primaryType, property5]", sortedCollectionToString(c.getProperties()));
        assertEquals("[]", sortedCollectionToString(c.getNodes()));
        final ConfigurationNode d = b.getNodes().get("d[1]");
        assertEquals("[jcr:primaryType, property4]", sortedCollectionToString(d.getProperties()));
        assertEquals("[]", sortedCollectionToString(d.getNodes()));
    }

    @Test
    public void invalid_single_definition() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a/b:\n"
                + "      property1: bla1";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);
        final ContentDefinitionImpl definition = (ContentDefinitionImpl)definitions.get(0);

        try {
            builder.push(definition);
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("test-group/test-project/test-module [string] contains definition rooted at unreachable node '/a/b'. Closest ancestor is at '/'.", e.getMessage());
        }
    }

    @Test
    public void root_node_property() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /:\n"
                + "      property1: bla1\n"
                + "      /a:\n"
                + "        jcr:primaryType: foo\n"
                + "        property2: [bla2, bla3]";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);
        final ContentDefinitionImpl definition = (ContentDefinitionImpl)definitions.get(0);
        builder.push(definition);
        final ConfigurationNodeImpl root = builder.build();

        assertEquals("[jcr:primaryType, jcr:mixinTypes, property1]", sortedCollectionToString(root.getProperties()));
        assertEquals(PropertyType.SINGLE, root.getProperties().get("property1").getType());
        assertEquals("[a[1]]", sortedCollectionToString(root.getNodes()));
        final ConfigurationNode a = root.getNodes().get("a[1]");
        assertEquals("[jcr:primaryType, property2]", sortedCollectionToString(a.getProperties()));
        assertEquals(PropertyType.LIST, a.getProperties().get("property2").getType());
        assertEquals("[]", sortedCollectionToString(a.getNodes()));
    }

    @Test
    public void merge_two_definitions_same_module() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property2: bla2\n"
                + "      property1: bla1\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        /d:\n"
                + "          jcr:primaryType: foo\n"
                + "          property4: bla4\n"
                + "        property8: bla8\n"
                + "        /c:\n"
                + "          jcr:primaryType: foo\n"
                + "          property5: bla5\n"
                + "    /a/d:\n"
                + "      jcr:primaryType: foo\n"
                + "      property7: bla7";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        assertEquals("[jcr:primaryType, jcr:mixinTypes]", sortedCollectionToString(root.getProperties()));
        assertEquals("[a[1]]", sortedCollectionToString(root.getNodes()));
        final ConfigurationNode a = root.getNodes().get("a[1]");
        assertEquals("[jcr:primaryType, property2, property1]", sortedCollectionToString(a.getProperties()));
        assertEquals("[b[1], d[1]]", sortedCollectionToString(a.getNodes()));
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[jcr:primaryType, property8]", sortedCollectionToString(b.getProperties()));
        assertEquals("[d[1], c[1]]", sortedCollectionToString(b.getNodes()));
        final ConfigurationNode d = a.getNodes().get("d[1]");
        assertEquals("[jcr:primaryType, property7]", sortedCollectionToString(d.getProperties()));
        assertEquals("[]", sortedCollectionToString(d.getNodes()));
    }

    @Test
    public void merge_two_definitions_separate_modules() throws Exception {
        final String yaml1 = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property2: bla2\n"
                + "      property1: bla1\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        /d:\n"
                + "          jcr:primaryType: foo\n"
                + "          property4: bla4\n"
                + "        property8: bla8\n"
                + "        /c:\n"
                + "          jcr:primaryType: foo\n"
                + "          property5: bla5";

        final List<Definition> definitions1 = ModelTestUtils.parseNoSort(yaml1);
        builder.push((ContentDefinitionImpl)definitions1.get(0));

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      property3: bla3\n"
                + "      /e:\n"
                + "        jcr:primaryType: foo\n"
                + "        property6: bla6\n"
                + "      /b:\n"
                + "        property7: bla7\n"
                + "        /f:\n"
                + "          jcr:primaryType: foo\n"
                + "          property9: bla9";

        final List<Definition> definitions2 = ModelTestUtils.parseNoSort(yaml2);
        builder.push((ContentDefinitionImpl)definitions2.get(0));
        final ConfigurationNodeImpl root = builder.build();

        assertEquals("[jcr:primaryType, jcr:mixinTypes]", sortedCollectionToString(root.getProperties()));
        assertEquals("[a[1]]", sortedCollectionToString(root.getNodes()));
        final ConfigurationNode a = root.getNodes().get("a[1]");
        assertEquals("[jcr:primaryType, property2, property1, property3]", sortedCollectionToString(a.getProperties()));
        assertEquals("[b[1], e[1]]", sortedCollectionToString(a.getNodes()));
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[jcr:primaryType, property8, property7]", sortedCollectionToString(b.getProperties()));
        assertEquals("[d[1], c[1], f[1]]", sortedCollectionToString(b.getNodes()));
        final ConfigurationNode e = a.getNodes().get("e[1]");
        assertEquals("[jcr:primaryType, property6]", sortedCollectionToString(e.getProperties()));
        assertEquals("[]", sortedCollectionToString(e.getNodes()));
        final ConfigurationNode f = b.getNodes().get("f[1]");
        assertEquals("[jcr:primaryType, property9]", sortedCollectionToString(f.getProperties()));
        assertEquals("[]", sortedCollectionToString(f.getNodes()));
    }

    @Test
    public void reorder_first_node_to_first() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "      /c:\n"
                + "        jcr:primaryType: foo\n"
                + "      /d:\n"
                + "        jcr:primaryType: foo\n"
                + "    /a/b:\n"
                + "      .meta:order-before: ''";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ContentDefinitionImpl) definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.startsWith("Unnecessary orderBefore: '' (first) for node '/a/b'")));
        }
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        assertEquals("[b[1], c[1], d[1]]", sortedCollectionToString(a.getNodes()));
    }

    @Test
    public void reorder_node_unnecessary() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "      /c:\n"
                + "        jcr:primaryType: foo\n"
                + "      /d:\n"
                + "        jcr:primaryType: foo\n"
                + "    /a/b:\n"
                + "      .meta:order-before: 'c'";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ContentDefinitionImpl) definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.startsWith("Unnecessary orderBefore: 'c' for node '/a/b'")));
        }
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        assertEquals("[b[1], c[1], d[1]]", sortedCollectionToString(a.getNodes()));
    }

    @Test
    public void reorder_existing_node_to_first() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "      /c:\n"
                + "        jcr:primaryType: foo\n"
                + "      /d:\n"
                + "        jcr:primaryType: foo\n"
                + "    /a/d:\n"
                + "      .meta:order-before: b";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        assertEquals("[d[1], b[1], c[1]]", sortedCollectionToString(a.getNodes()));
    }

    @Test
    public void reorder_existing_node_to_earlier() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "      /c:\n"
                + "        jcr:primaryType: foo\n"
                + "      /d:\n"
                + "        jcr:primaryType: foo\n"
                + "      /e:\n"
                + "        jcr:primaryType: foo\n"
                + "      /f:\n"
                + "        jcr:primaryType: foo\n"
                + "    /a/e:\n"
                + "      .meta:order-before: c";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        assertEquals("[b[1], e[1], c[1], d[1], f[1]]", sortedCollectionToString(a.getNodes()));
    }

    @Test
    public void reorder_existing_node_to_later() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "      /c:\n"
                + "        jcr:primaryType: foo\n"
                + "      /d:\n"
                + "        jcr:primaryType: foo\n"
                + "      /e:\n"
                + "        jcr:primaryType: foo\n"
                + "      /f:\n"
                + "        jcr:primaryType: foo\n"
                + "    /a/c:\n"
                + "      .meta:order-before: f";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        assertEquals("[b[1], d[1], e[1], c[1], f[1]]", sortedCollectionToString(a.getNodes()));
    }

    @Test
    public void reorder_new_root() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        /c:\n"
                + "          jcr:primaryType: foo\n"
                + "        /d:\n"
                + "          jcr:primaryType: foo\n"
                + "    /a/b/e:\n"
                + "      jcr:primaryType: foo\n"
                + "      .meta:order-before: d";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[c[1], e[1], d[1]]", sortedCollectionToString(b.getNodes()));
    }

    @Test
    public void reorder_new_child() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        /c:\n"
                + "          jcr:primaryType: foo\n"
                + "        /d:\n"
                + "          jcr:primaryType: foo\n"
                + "    /a/b:\n"
                + "      /e:\n"
                + "        jcr:primaryType: foo\n"
                + "        .meta:order-before: c";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[e[1], c[1], d[1]]", sortedCollectionToString(b.getNodes()));
    }

    @Test
    public void reorder_existing_child() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        /c:\n"
                + "          jcr:primaryType: foo\n"
                + "        /d:\n"
                + "          jcr:primaryType: foo\n"
                + "    /a/b:\n"
                + "      /d:\n"
                + "        .meta:order-before: c";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[d[1], c[1]]", sortedCollectionToString(b.getNodes()));
    }

    @Test
    public void delete_leaf_node() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "      /c:\n"
                + "        jcr:primaryType: foo\n"
                + "      /d:\n"
                + "        jcr:primaryType: foo\n"
                + "    /a/c:\n"
                + "      .meta:delete: true";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        assertEquals("[b[1], d[1]]", sortedCollectionToString(a.getNodes()));
    }

    @Test
    public void delete_subtree() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        /c:\n"
                + "          jcr:primaryType: foo\n"
                + "        /d:\n"
                + "          jcr:primaryType: foo\n"
                + "    /a/b:\n"
                + "      .meta:delete: true";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        assertEquals("[]", sortedCollectionToString(a.getNodes()));
    }

    @Test
    public void delete_embedded_in_definition_tree() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        /c:\n"
                + "          jcr:primaryType: foo\n"
                + "          property2: bla2\n"
                + "        /d:\n"
                + "          jcr:primaryType: foo\n"
                + "          property3: bla3\n"
                + "    /a/b:\n"
                + "      property1: bla1\n"
                + "      /c:\n"
                + "        property4: bla4\n"
                + "      /d:\n"
                + "        .meta:delete: true\n"
                + "      /e:\n"
                + "        jcr:primaryType: foo\n"
                + "        property5: bla5";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        final ConfigurationNode c = b.getNodes().get("c[1]");
        final ConfigurationNode e = b.getNodes().get("e[1]");
        assertEquals("[c[1], e[1]]", sortedCollectionToString(b.getNodes()));
        assertEquals("[jcr:primaryType, property1]", sortedCollectionToString(b.getProperties()));
        assertEquals("[jcr:primaryType, property2, property4]", sortedCollectionToString(c.getProperties()));
        assertEquals("[jcr:primaryType, property5]", sortedCollectionToString(e.getProperties()));
    }

    @Test
    public void delete_at_root_level() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /:\n"
                + "      .meta:delete: true";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        try {
            builder.push((ContentDefinitionImpl) definitions.get(0));
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Deleting the root node is not supported.", e.getMessage());
        }
    }

    @Test
    public void modify_deleted_node() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        /c:\n"
                + "          jcr:primaryType: foo\n"
                + "    /a/b:\n"
                + "      /c:\n"
                + "        .meta:delete: true\n"
                + "    /a/b/c:\n"
                + "      property2: bla2";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        builder.push((ContentDefinitionImpl) definitions.get(2));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[]", sortedCollectionToString(b.getNodes()));
    }

    @Test
    public void definition_root_below_deleted_node() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        /c:\n"
                + "          jcr:primaryType: foo\n"
                + "    /a/b:\n"
                + "      /c:\n"
                + "        .meta:delete: true\n"
                + "    /a/b/c/d:\n"
                + "          jcr:primaryType: foo";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        builder.push((ContentDefinitionImpl) definitions.get(2));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[]", sortedCollectionToString(b.getNodes()));
    }

    @Test
    public void non_existent_definition_root_with_delete_flag() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "    /a/b/c:\n"
                + "      .meta:delete: true";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ContentDefinitionImpl) definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("test-group/test-project/test-module [string]: Trying to delete node /a/b/c that does not exist.")));
        }

        final ConfigurationNodeImpl root = builder.build();
        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[]", sortedCollectionToString(b.getNodes()));
    }

    @Test
    public void non_existent_definition_root_with_delete_and_merge() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "    /a/b/c:\n"
                + "      .meta:delete: true\n"
                + "      jcr:primaryType: foo";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml, true);

        builder.push((ContentDefinitionImpl) definitions.get(0));

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ContentDefinitionImpl) definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("test-group/test-project/test-module [string]: Trying to merge delete node /a/b/c that does not exist.")));
        }

        final ConfigurationNodeImpl root = builder.build();
        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[c[1]]", sortedCollectionToString(b.getNodes()));
    }

    @Test
    public void non_existent_definition_node_with_delete_flag() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        property1: bla1\n"
                + "    /a/b:\n"
                + "      /c:\n"
                + "        .meta:delete: true";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ContentDefinitionImpl) definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("test-group/test-project/test-module [string]: Trying to delete node /a/b/c that does not exist.")));
        }

        final ConfigurationNodeImpl root = builder.build();
        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[]", sortedCollectionToString(b.getNodes()));
    }

    @Test
    public void non_existent_definition_node_with_delete_and_merge() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        property1: bla1\n"
                + "    /a/b:\n"
                + "      /c:\n"
                + "        .meta:delete: true\n"
                + "        jcr:primaryType: foo";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml, true);

        builder.push((ContentDefinitionImpl) definitions.get(0));

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ContentDefinitionImpl) definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("test-group/test-project/test-module [string]: Trying to merge delete node /a/b/c that does not exist.")));
        }

        final ConfigurationNodeImpl root = builder.build();
        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[c[1]]", sortedCollectionToString(b.getNodes()));
    }

    @Test
    public void existing_definition_root_with_delete_and_merge() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo";

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      /b:\n"
                + "        .meta:delete: true\n"
                + "        jcr:primaryType: foo";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);
        final List<Definition> definitions2 = ModelTestUtils.parseNoSort(yaml2, true);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        try {
            builder.push((ContentDefinitionImpl) definitions2.get(0));
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertEquals("test-group/test-project/test-module [string]: Trying to delete AND merge node /a/b defined before by [test-group/test-project/test-module [string]].", e.getMessage());
        }
    }

    @Test
    public void existing_definition_with_delete_and_merge() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo";

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a/b:\n"
                + "      .meta:delete: true\n"
                + "      jcr:primaryType: foo";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);
        final List<Definition> definitions2 = ModelTestUtils.parseNoSort(yaml2, true);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        try {
            builder.push((ContentDefinitionImpl) definitions2.get(0));
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertEquals("test-group/test-project/test-module [string]: Trying to delete AND merge node /a/b defined before by [test-group/test-project/test-module [string]].", e.getMessage());
        }
    }

    @Test
    public void double_delete() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        /c:\n"
                + "          jcr:primaryType: foo\n"
                + "    /a/b/c:\n"
                + "      .meta:delete: true\n"
                + "    /a/b:\n"
                + "      /c:\n"
                + "        .meta:delete: true";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        builder.push((ContentDefinitionImpl) definitions.get(2));

        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[]", sortedCollectionToString(b.getNodes()));
    }

    @Test
    public void modify_deleted_non_root_definition_node() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        /c:\n"
                + "          jcr:primaryType: foo\n"
                + "          /d:\n"
                + "            jcr:primaryType: foo\n"
                + "    /a/b:\n"
                + "      /c:\n"
                + "        /d:\n"
                + "          .meta:delete: true\n"
                + "    /a/b/c:\n"
                + "      /d:\n"
                + "        property2: bla2";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        builder.push((ContentDefinitionImpl) definitions.get(2));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        final ConfigurationNode c = b.getNodes().get("c[1]");
        assertEquals("[]", sortedCollectionToString(c.getNodes()));
    }

    @Test
    public void replace_single_property() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property1: bla1\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        property2: bla2\n"
                + "    /a/b:\n"
                + "      property2: bla3";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        assertEquals("[jcr:primaryType, jcr:mixinTypes]", sortedCollectionToString(root.getProperties()));
        assertEquals("[a[1]]", sortedCollectionToString(root.getNodes()));
        final ConfigurationNode a = root.getNodes().get("a[1]");
        assertEquals("[jcr:primaryType, property1]", sortedCollectionToString(a.getProperties()));
        assertEquals("[b[1]]", sortedCollectionToString(a.getNodes()));
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[jcr:primaryType, property2]", sortedCollectionToString(b.getProperties()));
        assertEquals("[]", sortedCollectionToString(b.getNodes()));
        final ConfigurationProperty property2 = b.getProperties().get("property2");
        assertEquals(PropertyType.SINGLE, property2.getType());
        assertEquals(ValueType.STRING, property2.getValueType());
        assertEquals("bla3", property2.getValue().getString());
    }

    @Test
    public void replace_single_property_from_same_source_with_equal_value_gives_warning() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        property1: bla1\n"
                + "    /a/b:\n"
                + "      property1: bla1";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ContentDefinitionImpl)definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("Property '/a/b/property1' defined in 'test-group/test-project/test-module [string]' specifies value equivalent to existing property.")));
        }

        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[jcr:primaryType, property1]", sortedCollectionToString(b.getProperties()));
        assertEquals("bla1", valueToString(b.getProperties().get("property1")));
    }

    @Test
    public void replace_single_property_from_different_sources_with_equal_value_gives_warning() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        property1: bla1\n";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a/b:\n"
                + "      property1: bla1";

        final List<Definition> definitions2 = ModelTestUtils.parseNoSort(yaml2);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ContentDefinitionImpl)definitions2.get(0));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("Property '/a/b/property1' defined in 'test-group/test-project/test-module [string]' specifies value equivalent to existing property.")));
        }

        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[jcr:primaryType, property1]", sortedCollectionToString(b.getProperties()));
        assertEquals("bla1", valueToString(b.getProperties().get("property1")));
    }

    @Test
    public void replace_resource_with_equal_value_from_same_source_gives_warning() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        property1:\n"
                + "          type: string\n"
                + "          resource: resource.txt\n"
                + "    /a/b:\n"
                + "      property1:\n"
                + "        type: string\n"
                + "        resource: resource.txt";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ContentDefinitionImpl)definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("Property '/a/b/property1' defined in 'test-group/test-project/test-module [string]' specifies value equivalent to existing property.")));
        }

        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[jcr:primaryType, property1]", sortedCollectionToString(b.getProperties()));
        assertEquals("resource.txt", valueToString(b.getProperties().get("property1")));
    }

    @Test
    public void replace_resource_with_equal_value_from_different_sources_gives_no_warning() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        property1:\n"
                + "          type: string\n"
                + "          resource: resource.txt\n";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a/b:\n"
                + "      property1:\n"
                + "        type: string\n"
                + "        resource: resource.txt";

        final List<Definition> definitions2 = ModelTestUtils.parseNoSort(yaml2);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ContentDefinitionImpl)definitions2.get(0));
            assertEquals(0, interceptor.getEvents().size());
        }

        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[jcr:primaryType, property1]", sortedCollectionToString(b.getProperties()));
        assertEquals("resource.txt", valueToString(b.getProperties().get("property1")));
    }

    @Test
    public void replace_list_property() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property1: bla1\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        property2: [bla2]\n"
                + "    /a/b:\n"
                + "      property2: [bla3, bla4]";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[jcr:primaryType, property2]", sortedCollectionToString(b.getProperties()));
        final ConfigurationProperty property2 = b.getProperties().get("property2");
        assertEquals(PropertyType.LIST, property2.getType());
        assertEquals(ValueType.STRING, property2.getValueType());
        assertEquals("bla3", property2.getValues()[0].getString());
        assertEquals("bla4", property2.getValues()[1].getString());
    }

    @Test
    public void replace_list_property_with_equal_values() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        property1: [bla1, bla2]\n"
                + "    /a/b:\n"
                + "      property1: [bla1, bla2]";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[jcr:primaryType, property1]", sortedCollectionToString(b.getProperties()));
        assertEquals("[bla1, bla2]", valuesToString(b.getProperties().get("property1")));
    }

    @Test
    public void add_property_values() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        property1: [bla1, bla2]\n"
                + "    /a/b:\n"
                + "      property1:\n"
                + "        operation: add\n"
                + "        value: [bla3, bla2]";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[jcr:primaryType, property1]", sortedCollectionToString(b.getProperties()));
        assertEquals("[bla1, bla2, bla3, bla2]", valuesToString(b.getProperties().get("property1")));
    }

    @Test
    public void add_property_values_for_new_property() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        property1: [bla1, bla2]\n"
                + "    /a/b:\n"
                + "      property2:\n"
                + "        operation: add\n"
                + "        value: [bla3, bla2]";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[jcr:primaryType, property1, property2]", sortedCollectionToString(b.getProperties()));
        assertEquals("[bla3, bla2]", valuesToString(b.getProperties().get("property2")));
    }

    @Test
    public void reject_property_if_different_kind() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property1: bla1\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        property2: bla2\n"
                + "    /a/b:\n"
                + "      property2: [bla3, bla4]";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));

        try {
            builder.push((ContentDefinitionImpl) definitions.get(1));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("Property /a/b/property2 already exists with type 'single', as determined by [test-group/test-project/test-module [string]], but type 'list' is requested in test-group/test-project/test-module [string].", e.getMessage());
        }
    }

    @Test
    public void override_property_with_different_kind() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        property1: bla1\n"
                + "    /a/b:\n"
                + "      property1:\n"
                + "        operation: override\n"
                + "        value: [bla2, bla3]";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[jcr:primaryType, property1]", sortedCollectionToString(b.getProperties()));
        assertEquals("[bla2, bla3]", valuesToString(b.getProperties().get("property1")));
    }

    @Test
    public void reject_property_if_different_type() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property1: bla1\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        property2: [bla2]\n"
                + "    /a/b:\n"
                + "      property2: [34]";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));

        try {
            builder.push((ContentDefinitionImpl) definitions.get(1));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("Property /a/b/property2 already exists with value type 'string', as determined by [test-group/test-project/test-module [string]], but value type 'long' is requested in test-group/test-project/test-module [string].", e.getMessage());
        }
    }

    @Test
    public void override_property_with_different_type() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        property1: bla1\n"
                + "    /a/b:\n"
                + "      property1:\n"
                + "        operation: override\n"
                + "        type: long\n"
                + "        value: 42";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("42", valueToString(b.getProperties().get("property1")));
    }

    @Test
    public void reject_value_change_for_primary_type() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: bla1\n"
                + "    /a/b:\n"
                + "      jcr:primaryType: bla2";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));

        try {
            builder.push((ContentDefinitionImpl) definitions.get(1));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("Property jcr:primaryType is already defined on node /a/b as determined by [test-group/test-project/test-module [string]], but change is requested in test-group/test-project/test-module [string]. Use 'operation: override' if you really intend to change the value of this property.", e.getMessage());
        }
    }

    @Test
    public void ignore_identical_value_for_primary_type() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: bla1\n"
                + "    /a/b:\n"
                + "      jcr:primaryType: bla1";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("bla1", valueToString(b.getProperties().get("jcr:primaryType")));
    }

    @Test
    public void override_primary_type() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: bla1\n"
                + "    /a/b:\n"
                + "      jcr:primaryType:\n"
                + "        operation: override\n"
                + "        value: bla2";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("bla2", valueToString(b.getProperties().get("jcr:primaryType")));
    }

    @Test
    public void add_mixins() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        jcr:mixinTypes: [bla1, bla2]\n"
                + "    /a/b:\n"
                + "      jcr:mixinTypes:\n"
                + "        operation: add\n"
                + "        value: [bla3, bla4]";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[bla1, bla2, bla3, bla4]", valuesToString(b.getProperties().get("jcr:mixinTypes")));
    }

    @Test
    public void replace_mixins_with_superset() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        jcr:mixinTypes: [bla1, bla2]\n"
                + "    /a/b:\n"
                + "      jcr:mixinTypes: [bla2, bla3, bla1]";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[bla2, bla3, bla1]", valuesToString(b.getProperties().get("jcr:mixinTypes")));
    }

    @Test
    public void reject_mixins_if_no_superset() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        jcr:mixinTypes: [bla1, bla2]\n"
                + "    /a/b:\n"
                + "      jcr:mixinTypes: [bla3, bla1]";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));

        try {
            builder.push((ContentDefinitionImpl) definitions.get(1));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("Property jcr:mixinTypes is already defined on node /a/b, and replace operation of test-group/test-project/test-module [string] would remove values [bla2]. Use 'operation: override' if you really intend to remove these values.", e.getMessage());
        }
    }

    @Test
    public void override_mixins_with_no_superset() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        jcr:mixinTypes: [bla1, bla2]\n"
                + "    /a/b:\n"
                + "      jcr:mixinTypes:\n"
                + "        operation: override\n"
                + "        value: [bla3, bla1]";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[bla3, bla1]", valuesToString(b.getProperties().get("jcr:mixinTypes")));
    }

    @Test
    public void delete_property() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        property1: bla1\n"
                + "    /a/b:\n"
                + "      property1:\n"
                + "        operation: delete";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        assertEquals("[jcr:primaryType]", sortedCollectionToString(b.getProperties()));
    }

    @Test
    public void delete_non_existent_property() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property1:\n"
                + "        operation: delete";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ContentDefinitionImpl) definitions.get(0));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("test-group/test-project/test-module [string]: Trying to delete property /a/property1 that does not exist.")));
        }
    }

    @Test
    public void modify_deleted_property() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        /c:\n"
                + "          jcr:primaryType: foo\n"
                + "          property1: bla1\n"
                + "    /a/b:\n"
                + "      /c:\n"
                + "        property1:\n"
                + "          operation: delete\n"
                + "    /a/b/c:\n"
                + "      property1: bla2";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        builder.push((ContentDefinitionImpl) definitions.get(2));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        final ConfigurationNode b = a.getNodes().get("b[1]");
        final ConfigurationNode c = b.getNodes().get("c[1]");
        assertEquals("[jcr:primaryType]", sortedCollectionToString(c.getProperties()));
    }

    @Test
    public void redundant_ignore_reordered_children1() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        .meta:ignore-reordered-children: false\n"
                + "    /a/b:\n"
                + "      .meta:ignore-reordered-children: false";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ContentDefinitionImpl) definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.startsWith("Redundant '.meta:ignore-reordered-children: false' for node '/a/b'")));
        }
    }

    @Test
    public void redundant_ignore_reordered_children2() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        .meta:ignore-reordered-children: true\n"
                + "    /a/b:\n"
                + "      .meta:ignore-reordered-children: true";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ContentDefinitionImpl) definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.startsWith("Redundant '.meta:ignore-reordered-children: true' for node '/a/b'")));
        }
    }

    @Test
    public void overriding_ignore_reordered_children1() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        .meta:ignore-reordered-children: true\n"
                + "    /a/b:\n"
                + "      .meta:ignore-reordered-children: false";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ContentDefinitionImpl) definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.startsWith("Overriding '.meta:ignore-reordered-children' for node '/a/b'")));
        }
    }

    @Test
    public void overriding_ignore_reordered_children2() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        .meta:ignore-reordered-children: false\n"
                + "    /a/b:\n"
                + "      .meta:ignore-reordered-children: true";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ContentDefinitionImpl) definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.startsWith("Overriding '.meta:ignore-reordered-children' for node '/a/b'")));
        }
    }

    @Test
    public void reorder_node_with_ignore_reordered_children_unnecessary() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      .meta:ignore-reordered-children: true\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "      /c:\n"
                + "        jcr:primaryType: foo\n"
                + "      /d:\n"
                + "        jcr:primaryType: foo\n"
                + "    /a/b:\n"
                + "      .meta:order-before: 'c'";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ContentDefinitionImpl) definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("Potential unnecessary orderBefore: 'c' for node '/a/b' defined in " +
                            "'test-group/test-project/test-module [string]': " +
                            "parent '/a' already configured with '.meta:ignore-reordered-children: true'")));
        }
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        assertEquals("[b[1], c[1], d[1]]", sortedCollectionToString(a.getNodes()));
    }

    @Test
    public void simple_sns_definition() throws Exception {
        final String yaml
                = "definitions:\n"
                + "  config:\n"
                + "    /test:\n"
                + "      jcr:primaryType: nt:unstructured\n"
                + "      /sns[1]:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property1: value1\n"
                + "      /sns[2]:\n"
                + "        jcr:primaryType: nt:unstructured\n"
                + "        property2: value2\n"
                + "";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);
        final ContentDefinitionImpl definition = (ContentDefinitionImpl)definitions.get(0);
        builder.push(definition);
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode test = root.getNodes().get("test[1]");
        assertEquals("[sns[1], sns[2]]", sortedCollectionToString(test.getNodes()));
        assertEquals("[jcr:primaryType, property1]", sortedCollectionToString(test.getNodes().get("sns[1]").getProperties()));
        assertEquals("[jcr:primaryType, property2]", sortedCollectionToString(test.getNodes().get("sns[2]").getProperties()));
    }

    final String snsFixture = "definitions:\n"
            + "  config:\n"
            + "    /a:\n"
            + "      jcr:primaryType: foo\n"
            + "      /sns[1]:\n"
            + "        jcr:primaryType: foo\n"
            + "        property1: value1\n"
            + "      /sns[2]:\n"
            + "        jcr:primaryType: foo\n"
            + "        property2: value2\n"
            + "      /sns[3]:\n"
            + "        jcr:primaryType: foo\n"
            + "        property3: value3\n";

    @Test
    public void delete_first_sns() throws Exception {
        final List<Definition> definition1 = ModelTestUtils.parseNoSort(snsFixture);
        builder.push((ContentDefinitionImpl) definition1.get(0));

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      /sns[1]:\n"
                + "        .meta:delete: true";
        final List<Definition> definition2 = ModelTestUtils.parseNoSort(yaml2);
        builder.push((ContentDefinitionImpl) definition2.get(0));

        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        assertEquals("[sns[1], sns[2]]", sortedCollectionToString(a.getNodes()));
        assertEquals("[jcr:primaryType, property2]", sortedCollectionToString(a.getNodes().get("sns[1]").getProperties()));
        assertEquals("[jcr:primaryType, property3]", sortedCollectionToString(a.getNodes().get("sns[2]").getProperties()));
    }

    @Test
    public void delete_middle_sns() throws Exception {
        final List<Definition> definition1 = ModelTestUtils.parseNoSort(snsFixture);
        builder.push((ContentDefinitionImpl) definition1.get(0));

        final String delete = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      /sns[2]:\n"
                + "        .meta:delete: true";
        final List<Definition> definition2 = ModelTestUtils.parseNoSort(delete);
        builder.push((ContentDefinitionImpl) definition2.get(0));

        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        assertEquals("[sns[1], sns[2]]", sortedCollectionToString(a.getNodes()));
        assertEquals("[jcr:primaryType, property1]", sortedCollectionToString(a.getNodes().get("sns[1]").getProperties()));
        assertEquals("[jcr:primaryType, property3]", sortedCollectionToString(a.getNodes().get("sns[2]").getProperties()));
    }

    @Test
    public void delete_last_sns() throws Exception {
        final List<Definition> definition1 = ModelTestUtils.parseNoSort(snsFixture);
        builder.push((ContentDefinitionImpl) definition1.get(0));

        final String delete = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      /sns[3]:\n"
                + "        .meta:delete: true";
        final List<Definition> definition2 = ModelTestUtils.parseNoSort(delete);
        builder.push((ContentDefinitionImpl) definition2.get(0));

        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        assertEquals("[sns[1], sns[2]]", sortedCollectionToString(a.getNodes()));
        assertEquals("[jcr:primaryType, property1]", sortedCollectionToString(a.getNodes().get("sns[1]").getProperties()));
        assertEquals("[jcr:primaryType, property2]", sortedCollectionToString(a.getNodes().get("sns[2]").getProperties()));
    }

    @Test
    public void delete_all_sns() throws Exception {
        final List<Definition> definition1 = ModelTestUtils.parseNoSort(snsFixture);
        builder.push((ContentDefinitionImpl) definition1.get(0));

        final String delete = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      /sns[1]:\n"
                + "        .meta:delete: true";
        final List<Definition> definition2 = ModelTestUtils.parseNoSort(delete);
        builder.push((ContentDefinitionImpl) definition2.get(0));
        builder.push((ContentDefinitionImpl) definition2.get(0));
        builder.push((ContentDefinitionImpl) definition2.get(0));

        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        assertEquals("[]", sortedCollectionToString(a.getNodes()));
    }

    @Test
    public void reorder_existing_sns_to_earlier() throws Exception {
        final List<Definition> definition1 = ModelTestUtils.parseNoSort(snsFixture);
        builder.push((ContentDefinitionImpl) definition1.get(0));

        final String orderBefore = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      /sns[3]:\n"
                + "        .meta:order-before: sns[1]";
        final List<Definition> definition2 = ModelTestUtils.parseNoSort(orderBefore);
        builder.push((ContentDefinitionImpl) definition2.get(0));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        assertEquals("[sns[1], sns[2], sns[3]]", sortedCollectionToString(a.getNodes()));
        assertEquals("[jcr:primaryType, property3]", sortedCollectionToString(a.getNodes().get("sns[1]").getProperties()));
        assertEquals("[jcr:primaryType, property1]", sortedCollectionToString(a.getNodes().get("sns[2]").getProperties()));
        assertEquals("[jcr:primaryType, property2]", sortedCollectionToString(a.getNodes().get("sns[3]").getProperties()));
    }

    @Test
    public void reorder_existing_sns_to_later() throws Exception {
        final List<Definition> definition1 = ModelTestUtils.parseNoSort(snsFixture);
        builder.push((ContentDefinitionImpl) definition1.get(0));

        final String orderBefore = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      /sns[1]:\n"
                + "        .meta:order-before: sns[3]";
        final List<Definition> definition2 = ModelTestUtils.parseNoSort(orderBefore);
        builder.push((ContentDefinitionImpl) definition2.get(0));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a[1]");
        assertEquals("[sns[1], sns[2], sns[3]]", sortedCollectionToString(a.getNodes()));
        assertEquals("[jcr:primaryType, property2]", sortedCollectionToString(a.getNodes().get("sns[1]").getProperties()));
        assertEquals("[jcr:primaryType, property1]", sortedCollectionToString(a.getNodes().get("sns[2]").getProperties()));
        assertEquals("[jcr:primaryType, property3]", sortedCollectionToString(a.getNodes().get("sns[3]").getProperties()));
    }

    @Test
    public void reject_sns_if_lower_index_is_missing() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /sns[2]:\n"
                + "        jcr:primaryType: foo\n";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);

        try {
            builder.push((ContentDefinitionImpl) definitions.get(0));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("test-group/test-project/test-module [string] defines node '/a/sns[2]', but no sibling named 'sns[1]' was found", e.getMessage());
        }
    }

    @Test
    public void node_meta_category_override_not_allowed() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      .meta:category: runtime\n";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ContentDefinitionImpl) definitions.get(0));

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      .meta:category: content\n";

        final List<Definition> definitions2 = ModelTestUtils.parseNoSort(yaml2);

        try {
            builder.push((ContentDefinitionImpl) definitions2.get(0));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("test-group/test-project/test-module [string]: overriding .meta:category is not supported; was set to runtime on /a by test-group/test-project/test-module [string].", e.getMessage());
        }
    }

    @Test
    public void node_with_meta_category_override_cannot_be_merged_with() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      .meta:category: runtime\n";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ContentDefinitionImpl) definitions.get(0));

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      property: value\n";

        final List<Definition> definitions2 = ModelTestUtils.parseNoSort(yaml2);

        try {
            builder.push((ContentDefinitionImpl) definitions2.get(0));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("test-group/test-project/test-module [string]: trying to add configuration on path /a while it had set '.meta:category: runtime' by test-group/test-project/test-module [string].", e.getMessage());
        }
    }

    @Test
    public void existing_node_cannot_have_meta_category_merged_onto() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property: value\n";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ContentDefinitionImpl) definitions.get(0));

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      .meta:category: runtime\n";

        final List<Definition> definitions2 = ModelTestUtils.parseNoSort(yaml2);

        try {
            builder.push((ContentDefinitionImpl) definitions2.get(0));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("test-group/test-project/test-module [string]: overriding .meta:category is not supported; '/a' was contributed as configuration by [test-group/test-project/test-module [string]].", e.getMessage());
        }
    }


    @Test
    public void property_meta_category_override_not_allowed() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property:\n"
                + "        .meta:category: runtime\n";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ContentDefinitionImpl) definitions.get(0));

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      property:\n"
                + "        .meta:category: content\n";

        final List<Definition> definitions2 = ModelTestUtils.parseNoSort(yaml2);

        try {
            builder.push((ContentDefinitionImpl) definitions2.get(0));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("test-group/test-project/test-module [string]: overriding .meta:category is not supported; was set to runtime on /a/property by test-group/test-project/test-module [string].", e.getMessage());
        }
    }

    @Test
    public void property_with_meta_category_override_cannot_be_merged_with() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property:\n"
                + "        .meta:category: runtime\n";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ContentDefinitionImpl) definitions.get(0));

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      property: value\n";

        final List<Definition> definitions2 = ModelTestUtils.parseNoSort(yaml2);

        try {
            builder.push((ContentDefinitionImpl) definitions2.get(0));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("test-group/test-project/test-module [string]: trying to add configuration on path /a/property while it had set '.meta:category: runtime' by test-group/test-project/test-module [string].", e.getMessage());
        }
    }

    @Test
    public void existing_property_cannot_have_meta_category_merged_onto() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property: value\n";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ContentDefinitionImpl) definitions.get(0));

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      property:\n"
                + "        .meta:category: runtime\n";

        final List<Definition> definitions2 = ModelTestUtils.parseNoSort(yaml2);

        try {
            builder.push((ContentDefinitionImpl) definitions2.get(0));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("test-group/test-project/test-module [string]: overriding .meta:category is not supported; '/a/property' was contributed as configuration by [test-group/test-project/test-module [string]].", e.getMessage());
        }
    }

    @Test
    public void meta_residual_child_node_category_override_not_allowed() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      .meta:residual-child-node-category: runtime\n";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ContentDefinitionImpl) definitions.get(0));

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      .meta:residual-child-node-category: content\n";

        final List<Definition> definitions2 = ModelTestUtils.parseNoSort(yaml2);

        try {
            builder.push((ContentDefinitionImpl) definitions2.get(0));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("test-group/test-project/test-module [string]: overriding .meta:residual-child-node-category is not supported; node '/a' was set to runtime by test-group/test-project/test-module [string].", e.getMessage());
        }
    }

    @Test
    public void test_root_residual_child_settings() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /:\n"
                + "      default: value\n"
                + "      override:\n"
                + "        .meta:category: content\n"
                + "      /default:\n"
                + "        jcr:primaryType: foo\n"
                + "      /override:\n"
                + "        .meta:category: content\n";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ContentDefinitionImpl) definitions.get(0));
        final ConfigurationNodeImpl root = builder.build();

        assertEquals(ConfigurationItemCategory.CONFIGURATION, root.getChildNodeCategory("default[1]"));
        assertEquals(ConfigurationItemCategory.RUNTIME,       root.getChildNodeCategory("non-existing-node[1]"));
        assertEquals(ConfigurationItemCategory.CONTENT,       root.getChildNodeCategory("override[1]"));
        assertEquals(ConfigurationItemCategory.CONTENT,       root.getChildNodeCategory("override[2]"));
        assertEquals(ConfigurationItemCategory.CONFIGURATION, root.getChildPropertyCategory("default"));
        assertEquals(ConfigurationItemCategory.CONFIGURATION, root.getChildPropertyCategory("non-existing-property"));
        assertEquals(ConfigurationItemCategory.CONTENT,       root.getChildPropertyCategory("override"));
    }

    @Test
    public void test_regular_node_residual_child_settings() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /node:\n"
                + "      jcr:primaryType: foo\n"
                + "      default: value\n"
                + "      override:\n"
                + "        .meta:category: content\n"
                + "      /default:\n"
                + "        jcr:primaryType: foo\n"
                + "      /override:\n"
                + "        .meta:category: content\n";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ContentDefinitionImpl) definitions.get(0));
        final ConfigurationNodeImpl root = builder.build();
        final ConfigurationNode node = root.getNodes().get("node[1]");

        assertEquals(ConfigurationItemCategory.CONFIGURATION, node.getChildNodeCategory("default[1]"));
        assertEquals(ConfigurationItemCategory.CONFIGURATION, node.getChildNodeCategory("non-existing-node[1]"));
        assertEquals(ConfigurationItemCategory.CONTENT,       node.getChildNodeCategory("override[1]"));
        assertEquals(ConfigurationItemCategory.CONTENT,       node.getChildNodeCategory("override[2]"));
        assertEquals(ConfigurationItemCategory.CONFIGURATION, node.getChildPropertyCategory("default"));
        assertEquals(ConfigurationItemCategory.CONFIGURATION, node.getChildPropertyCategory("non-existing-property"));
        assertEquals(ConfigurationItemCategory.CONTENT,       node.getChildPropertyCategory("override"));
    }

    @Test
    public void test_node_with_explicit_residual() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /node:\n"
                + "      jcr:primaryType: foo\n"
                + "      .meta:residual-child-node-category: content\n"
                + "      default: value\n"
                + "      override:\n"
                + "        .meta:category: runtime\n"
                + "      /default:\n"
                + "        jcr:primaryType: foo\n"
                + "      /override:\n"
                + "        .meta:category: runtime\n";

        final List<Definition> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ContentDefinitionImpl) definitions.get(0));
        final ConfigurationNodeImpl root = builder.build();
        final ConfigurationNode node = root.getNodes().get("node[1]");

        assertEquals(ConfigurationItemCategory.CONFIGURATION, node.getChildNodeCategory("default[1]"));
        assertEquals(ConfigurationItemCategory.CONTENT,       node.getChildNodeCategory("non-existing-node[1]"));
        assertEquals(ConfigurationItemCategory.RUNTIME,       node.getChildNodeCategory("override[1]"));
        assertEquals(ConfigurationItemCategory.RUNTIME,       node.getChildNodeCategory("override[2]"));
        assertEquals(ConfigurationItemCategory.CONFIGURATION, node.getChildPropertyCategory("default"));
        assertEquals(ConfigurationItemCategory.CONFIGURATION, node.getChildPropertyCategory("non-existing-property"));
        assertEquals(ConfigurationItemCategory.RUNTIME,       node.getChildPropertyCategory("override"));
    }

}
