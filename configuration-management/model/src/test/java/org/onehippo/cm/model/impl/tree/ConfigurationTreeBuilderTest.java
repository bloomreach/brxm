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

package org.onehippo.cm.model.impl.tree;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.onehippo.cm.model.impl.ModelTestUtils;
import org.onehippo.cm.model.impl.definition.AbstractDefinitionImpl;
import org.onehippo.cm.model.impl.definition.ConfigDefinitionImpl;
import org.onehippo.cm.model.parser.ParserException;
import org.onehippo.cm.model.tree.ConfigurationItemCategory;
import org.onehippo.cm.model.tree.ModelItem;
import org.onehippo.cm.model.tree.PropertyKind;
import org.onehippo.cm.model.tree.Value;
import org.onehippo.cm.model.tree.ValueType;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConfigurationTreeBuilderTest {

    private final ConfigurationTreeBuilder builder = new ConfigurationTreeBuilder();

    private String valuesToString(final ConfigurationPropertyImpl property) {
        return property.getValues().stream().map(Value::getString).collect(Collectors.joining(", ", "[", "]"));
    }

    private String valueToString(final ConfigurationPropertyImpl property) {
        return property.getValue().getString();
    }

    public static String collectionToString(final Collection<? extends ModelItem> collection) {
        return collection.stream().map(ModelItem::getName).collect(Collectors.joining(", ", "[", "]"));
    }

    @Test
    public void simple_single_definition() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property1: bla1\n"
                + "      property2: bla2";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);
        final ConfigDefinitionImpl definition = (ConfigDefinitionImpl)definitions.get(0);
        builder.push(definition);
        final ConfigurationNodeImpl root = builder.finishModule().build();

        assertEquals("[jcr:uuid, jcr:primaryType, jcr:mixinTypes]", collectionToString(root.getProperties()));
        assertEquals("[a[1]]", collectionToString(root.getNodes()));
        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[jcr:primaryType, property1, property2]", collectionToString(a.getProperties()));
        assertEquals("[]", collectionToString(a.getNodes()));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);
        final ConfigDefinitionImpl definition = (ConfigDefinitionImpl)definitions.get(0);
        builder.push(definition);
        final ConfigurationNodeImpl root = builder.finishModule().build();

        assertEquals("[jcr:uuid, jcr:primaryType, jcr:mixinTypes]", collectionToString(root.getProperties()));
        assertEquals("[a[1]]", collectionToString(root.getNodes()));
        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[jcr:primaryType, property2, property1]", collectionToString(a.getProperties()));
        assertEquals("[b[1]]", collectionToString(a.getNodes()));
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[jcr:primaryType, property3]", collectionToString(b.getProperties()));
        assertEquals("[d[1], c[1]]", collectionToString(b.getNodes()));
        final ConfigurationNodeImpl c = b.getNode("c[1]");
        assertEquals("[jcr:primaryType, property5]", collectionToString(c.getProperties()));
        assertEquals("[]", collectionToString(c.getNodes()));
        final ConfigurationNodeImpl d = b.getNode("d[1]");
        assertEquals("[jcr:primaryType, property4]", collectionToString(d.getProperties()));
        assertEquals("[]", collectionToString(d.getNodes()));
    }

    @Test
    public void invalid_single_definition() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a/b:\n"
                + "      property1: bla1";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);
        final ConfigDefinitionImpl definition = (ConfigDefinitionImpl)definitions.get(0);

        try {
            builder.push(definition);
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("test-group/test-project/test-module [config: string] contains definition rooted at unreachable node '/a/b'. Closest ancestor is at '/'.", e.getMessage());
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);
        final ConfigDefinitionImpl definition = (ConfigDefinitionImpl)definitions.get(0);
        builder.push(definition);
        final ConfigurationNodeImpl root = builder.finishModule().build();

        assertEquals("[jcr:uuid, jcr:primaryType, jcr:mixinTypes, property1]", collectionToString(root.getProperties()));
        assertEquals(PropertyKind.SINGLE, root.getProperty("property1").getKind());
        assertEquals("[a[1]]", collectionToString(root.getNodes()));
        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[jcr:primaryType, property2]", collectionToString(a.getProperties()));
        assertEquals(PropertyKind.LIST, a.getProperty("property2").getKind());
        assertEquals("[]", collectionToString(a.getNodes()));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        assertEquals("[jcr:uuid, jcr:primaryType, jcr:mixinTypes]", collectionToString(root.getProperties()));
        assertEquals("[a[1]]", collectionToString(root.getNodes()));
        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[jcr:primaryType, property2, property1]", collectionToString(a.getProperties()));
        assertEquals("[b[1], d[1]]", collectionToString(a.getNodes()));
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[jcr:primaryType, property8]", collectionToString(b.getProperties()));
        assertEquals("[d[1], c[1]]", collectionToString(b.getNodes()));
        final ConfigurationNodeImpl d = a.getNode("d[1]");
        assertEquals("[jcr:primaryType, property7]", collectionToString(d.getProperties()));
        assertEquals("[]", collectionToString(d.getNodes()));
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

        final List<AbstractDefinitionImpl> definitions1 = ModelTestUtils.parseNoSort(yaml1);
        builder.push((ConfigDefinitionImpl)definitions1.get(0));

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

        final List<AbstractDefinitionImpl> definitions2 = ModelTestUtils.parseNoSort(yaml2);
        builder.push((ConfigDefinitionImpl)definitions2.get(0));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        assertEquals("[jcr:uuid, jcr:primaryType, jcr:mixinTypes]", collectionToString(root.getProperties()));
        assertEquals("[a[1]]", collectionToString(root.getNodes()));
        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[jcr:primaryType, property2, property1, property3]", collectionToString(a.getProperties()));
        assertEquals("[b[1], e[1]]", collectionToString(a.getNodes()));
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[jcr:primaryType, property8, property7]", collectionToString(b.getProperties()));
        assertEquals("[d[1], c[1], f[1]]", collectionToString(b.getNodes()));
        final ConfigurationNodeImpl e = a.getNode("e[1]");
        assertEquals("[jcr:primaryType, property6]", collectionToString(e.getProperties()));
        assertEquals("[]", collectionToString(e.getNodes()));
        final ConfigurationNodeImpl f = b.getNode("f[1]");
        assertEquals("[jcr:primaryType, property9]", collectionToString(f.getProperties()));
        assertEquals("[]", collectionToString(f.getNodes()));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.startsWith("Unnecessary orderBefore: '' (first) for node '/a/b'")));
        }
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[b[1], c[1], d[1]]", collectionToString(a.getNodes()));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.startsWith("Unnecessary orderBefore: 'c' for node '/a/b'")));
        }
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[b[1], c[1], d[1]]", collectionToString(a.getNodes()));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[d[1], b[1], c[1]]", collectionToString(a.getNodes()));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[b[1], e[1], c[1], d[1], f[1]]", collectionToString(a.getNodes()));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[b[1], d[1], e[1], c[1], f[1]]", collectionToString(a.getNodes()));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[c[1], e[1], d[1]]", collectionToString(b.getNodes()));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[e[1], c[1], d[1]]", collectionToString(b.getNodes()));

        assertEquals("a definition adding only child nodes should be listed as a definition for the containing node",
                2, b.getDefinitions().size());
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        final ConfigurationNodeImpl d = b.getNode("d[1]");
        assertEquals("[d[1], c[1]]", collectionToString(b.getNodes()));

        assertEquals("a definition only reordering child nodes should be listed as a definition for the containing node",
                2, b.getDefinitions().size());

        assertEquals("a definition reordering a node SHOULD be listed as a definition for the node",
                2, d.getDefinitions().size());
    }

    @Test
    public void reorder_override() throws Exception {
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
                + "        /e:\n"
                + "          jcr:primaryType: foo\n"
                + "    /a/b:\n"
                + "      /d:\n"
                + "        .meta:order-before: c\n"
                + "    /a/b/d:\n"
                + "      .meta:order-before: e";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        builder.push((ConfigDefinitionImpl)definitions.get(2));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[c[1], d[1], e[1]]", collectionToString(b.getNodes()));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[b[1], d[1]]", collectionToString(a.getNodes()));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[]", collectionToString(a.getNodes()));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        final ConfigurationNodeImpl c = b.getNode("c[1]");
        final ConfigurationNodeImpl e = b.getNode("e[1]");
        assertEquals("[c[1], e[1]]", collectionToString(b.getNodes()));
        assertEquals("[jcr:primaryType, property1]", collectionToString(b.getProperties()));
        assertEquals("[jcr:primaryType, property2, property4]", collectionToString(c.getProperties()));
        assertEquals("[jcr:primaryType, property5]", collectionToString(e.getProperties()));
    }

    @Test
    public void delete_at_root_level() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /:\n"
                + "      .meta:delete: true";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        try {
            builder.push((ConfigDefinitionImpl)definitions.get(0));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        builder.push((ConfigDefinitionImpl)definitions.get(2));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[]", collectionToString(b.getNodes()));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        builder.push((ConfigDefinitionImpl)definitions.get(2));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[]", collectionToString(b.getNodes()));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("test-group/test-project/test-module [config: string]: Trying to delete node /a/b/c that does not exist.")));
        }

        final ConfigurationNodeImpl root = builder.finishModule().build();
        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[]", collectionToString(b.getNodes()));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml, true);

        builder.push((ConfigDefinitionImpl)definitions.get(0));

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("test-group/test-project/test-module [config: string]: Trying to merge delete node /a/b/c that does not exist.")));
        }

        final ConfigurationNodeImpl root = builder.finishModule().build();
        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[c[1]]", collectionToString(b.getNodes()));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("test-group/test-project/test-module [config: string]: Trying to delete node /a/b/c that does not exist.")));
        }

        final ConfigurationNodeImpl root = builder.finishModule().build();
        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[]", collectionToString(b.getNodes()));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml, true);

        builder.push((ConfigDefinitionImpl)definitions.get(0));

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("test-group/test-project/test-module [config: string]: Trying to merge delete node /a/b/c that does not exist.")));
        }

        final ConfigurationNodeImpl root = builder.finishModule().build();
        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[c[1]]", collectionToString(b.getNodes()));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);
        final List<AbstractDefinitionImpl> definitions2 = ModelTestUtils.parseNoSort(yaml2, true);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        try {
            builder.push((ConfigDefinitionImpl)definitions2.get(0));
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertEquals("test-group/test-project/test-module [config: string]: Trying to delete AND merge node /a/b defined before by [test-group/test-project/test-module [config: string]].", e.getMessage());
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);
        final List<AbstractDefinitionImpl> definitions2 = ModelTestUtils.parseNoSort(yaml2, true);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        try {
            builder.push((ConfigDefinitionImpl)definitions2.get(0));
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertEquals("test-group/test-project/test-module [config: string]: Trying to delete AND merge node /a/b defined before by [test-group/test-project/test-module [config: string]].", e.getMessage());
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions.get(2));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("[test-group/test-project/test-module [config: string], test-group/test-project/test-module [config: string]] tries to modify already deleted node '/a/b/c', skipping.")));
        }

        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[]", collectionToString(b.getNodes()));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions.get(2));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("[test-group/test-project/test-module [config: string], test-group/test-project/test-module [config: string]] tries to modify already deleted node '/a/b/c/d', skipping.")));
        }

        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        final ConfigurationNodeImpl c = b.getNode("c[1]");
        assertEquals("[]", collectionToString(c.getNodes()));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        assertEquals("[jcr:uuid, jcr:primaryType, jcr:mixinTypes]", collectionToString(root.getProperties()));
        assertEquals("[a[1]]", collectionToString(root.getNodes()));
        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[jcr:primaryType, property1]", collectionToString(a.getProperties()));
        assertEquals("[b[1]]", collectionToString(a.getNodes()));
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[jcr:primaryType, property2]", collectionToString(b.getProperties()));
        assertEquals("[]", collectionToString(b.getNodes()));
        final ConfigurationPropertyImpl property2 = b.getProperty("property2");
        assertEquals(PropertyKind.SINGLE, property2.getKind());
        assertEquals(ValueType.STRING, property2.getValueType());
        assertEquals("bla3", property2.getValue().getString());

        assertEquals("a definition modifying a property should be listed as a definition for the containing node",
                2, b.getDefinitions().size());

        assertEquals("a definition modifying a property should be listed as a definition for the property",
                2, property2.getDefinitions().size());
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));

        builder.push((ConfigDefinitionImpl)definitions.get(1));
/* TODO: restore after HCM-166
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("Property '/a/b/property1' defined in 'test-group/test-project/test-module [config: string]' specifies value equivalent to existing property, defined in '[test-group/test-project/test-module [config: string]]'.")));
        }
*/

        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[jcr:primaryType, property1]", collectionToString(b.getProperties()));
        assertEquals("bla1", valueToString(b.getProperty("property1")));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a/b:\n"
                + "      property1: bla1";

        final List<AbstractDefinitionImpl> definitions2 = ModelTestUtils.parseNoSort(yaml2);

        builder.push((ConfigDefinitionImpl)definitions2.get(0));
/* TODO: restore after HCM-166
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions2.get(0));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("Property '/a/b/property1' defined in 'test-group/test-project/test-module [config: string]' specifies value equivalent to existing property, defined in '[test-group/test-project/test-module [config: string]]'.")));
        }
*/

        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[jcr:primaryType, property1]", collectionToString(b.getProperties()));
        assertEquals("bla1", valueToString(b.getProperty("property1")));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));

        builder.push((ConfigDefinitionImpl)definitions.get(1));
/* TODO: restore after HCM-166
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions.get(1));
            assertTrue(interceptor.messages()
                   .anyMatch(m->m.equals("Property '/a/b/property1' defined in 'test-group/test-project/test-module [config: string]' specifies value equivalent to existing property, defined in '[test-group/test-project/test-module [config: string]]'.")));
        }
*/

        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[jcr:primaryType, property1]", collectionToString(b.getProperties()));
        assertEquals("resource.txt", valueToString(b.getProperty("property1")));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a/b:\n"
                + "      property1:\n"
                + "        type: string\n"
                + "        resource: resource.txt";

        final List<AbstractDefinitionImpl> definitions2 = ModelTestUtils.parseNoSort(yaml2);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions2.get(0));
            assertEquals(0, interceptor.getEvents().size());
        }

        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[jcr:primaryType, property1]", collectionToString(b.getProperties()));
        assertEquals("resource.txt", valueToString(b.getProperty("property1")));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[jcr:primaryType, property2]", collectionToString(b.getProperties()));
        final ConfigurationPropertyImpl property2 = b.getProperty("property2");
        assertEquals(PropertyKind.LIST, property2.getKind());
        assertEquals(ValueType.STRING, property2.getValueType());
        assertEquals("bla3", property2.getValues().get(0).getString());
        assertEquals("bla4", property2.getValues().get(1).getString());
    }


    @Test
    public void empty_list_property_with_explicit_config_category() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property1: bla1\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        property2: \n"
                + "          .meta:category: config\n"
                + "          value: []\n";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[jcr:primaryType, property2]", collectionToString(b.getProperties()));
        final ConfigurationPropertyImpl property2 = b.getProperty("property2");
        assertEquals(PropertyKind.LIST, property2.getKind());
        assertEquals(ValueType.STRING, property2.getValueType());
        assertEquals(0, property2.getValues().size());
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
/* TODO: restore after HCM-166
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("Property '/a/b/property1' defined in 'test-group/test-project/test-module [config: string]' specifies values equivalent to existing property, defined in '[test-group/test-project/test-module [config: string]]'.")));
        }
*/
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[jcr:primaryType, property1]", collectionToString(b.getProperties()));
        assertEquals("[bla1, bla2]", valuesToString(b.getProperty("property1")));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[jcr:primaryType, property1]", collectionToString(b.getProperties()));
        assertEquals("[bla1, bla2, bla3, bla2]", valuesToString(b.getProperty("property1")));

        assertEquals("a definition modifying a property should be listed as a definition for the containing node",
                2, b.getDefinitions().size());
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("Property '/a/b/property2' defined in 'test-group/test-project/test-module [config: string]' claims to ADD values, but property doesn't exist yet. Applying default behaviour.")));
        }
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[jcr:primaryType, property1, property2]", collectionToString(b.getProperties()));
        assertEquals("[bla3, bla2]", valuesToString(b.getProperty("property2")));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));

        try {
            builder.push((ConfigDefinitionImpl)definitions.get(1));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("Property /a/b/property2 already exists with type 'single', as determined by [test-group/test-project/test-module [config: string]], but type 'list' is requested in test-group/test-project/test-module [config: string].", e.getMessage());
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[jcr:primaryType, property1]", collectionToString(b.getProperties()));
        assertEquals("[bla2, bla3]", valuesToString(b.getProperty("property1")));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));

        try {
            builder.push((ConfigDefinitionImpl)definitions.get(1));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("Property /a/b/property2 already exists with value type 'string', as determined by [test-group/test-project/test-module [config: string]], but value type 'long' is requested in test-group/test-project/test-module [config: string].", e.getMessage());
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("42", valueToString(b.getProperty("property1")));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));

        try {
            builder.push((ConfigDefinitionImpl)definitions.get(1));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("Property jcr:primaryType is already defined on node /a/b as determined by [test-group/test-project/test-module [config: string]], but change is requested in test-group/test-project/test-module [config: string]. Use 'operation: override' if you really intend to change the value of this property.", e.getMessage());
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
/* TODO: restore after HCM-166
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("Property '/a/b/jcr:primaryType' defined in 'test-group/test-project/test-module [config: string]' specifies value equivalent to existing property, defined in '[test-group/test-project/test-module [config: string]]'.")));
        }
*/
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("bla1", valueToString(b.getProperty("jcr:primaryType")));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("bla2", valueToString(b.getProperty("jcr:primaryType")));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[bla1, bla2, bla3, bla4]", valuesToString(b.getProperty("jcr:mixinTypes")));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[bla2, bla3, bla1]", valuesToString(b.getProperty("jcr:mixinTypes")));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));

        try {
            builder.push((ConfigDefinitionImpl)definitions.get(1));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("Property jcr:mixinTypes is already defined on node /a/b, and replace operation of test-group/test-project/test-module [config: string] would remove values [bla2]. Use 'operation: override' if you really intend to remove these values.", e.getMessage());
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[bla3, bla1]", valuesToString(b.getProperty("jcr:mixinTypes")));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        assertEquals("[jcr:primaryType]", collectionToString(b.getProperties()));
    }

    @Test
    public void delete_non_existent_property() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property1:\n"
                + "        operation: delete";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions.get(0));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("test-group/test-project/test-module [config: string]: Trying to delete property /a/property1 that does not exist.")));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions.get(2));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("Property '/a/b/c/property1' defined in 'test-group/test-project/test-module [config: string]' has already been deleted. This property is not re-created.")));
        }
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        final ConfigurationNodeImpl b = a.getNode("b[1]");
        final ConfigurationNodeImpl c = b.getNode("c[1]");
        assertEquals("[jcr:primaryType]", collectionToString(c.getProperties()));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions.get(1));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions.get(1));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions.get(1));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions.get(1));
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

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("Potential unnecessary orderBefore: 'c' for node '/a/b' defined in " +
                            "'test-group/test-project/test-module [config: string]': " +
                            "parent '/a' already configured with '.meta:ignore-reordered-children: true'")));
        }
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[b[1], c[1], d[1]]", collectionToString(a.getNodes()));
    }

    private final String snsFixture = "definitions:\n"
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
    public void simple_sns_definition() throws Exception {
        final List<AbstractDefinitionImpl> definition1 = ModelTestUtils.parseNoSort(snsFixture);
        builder.push((ConfigDefinitionImpl)definition1.get(0));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl test = root.getNode("a[1]");
        assertEquals("[sns[1], sns[2], sns[3]]", collectionToString(test.getNodes()));
        assertEquals("[jcr:primaryType, property1]", collectionToString(test.getNode("sns[1]").getProperties()));
        assertEquals("[jcr:primaryType, property2]", collectionToString(test.getNode("sns[2]").getProperties()));
        assertEquals("[jcr:primaryType, property3]", collectionToString(test.getNode("sns[3]").getProperties()));
    }

    @Test
    public void no_index_merges_with_index_1() throws Exception {
        final List<AbstractDefinitionImpl> definition1 = ModelTestUtils.parseNoSort(snsFixture);
        builder.push((ConfigDefinitionImpl)definition1.get(0));

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      /sns[1]:\n"
                + "        property2: value2";
        final List<AbstractDefinitionImpl> definition2 = ModelTestUtils.parseNoSort(yaml2);
        builder.push((ConfigDefinitionImpl)definition2.get(0));

        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[jcr:primaryType, property1, property2]", collectionToString(a.getNode("sns[1]").getProperties()));
    }

    @Test
    public void delete_first_sns() throws Exception {
        final List<AbstractDefinitionImpl> definition1 = ModelTestUtils.parseNoSort(snsFixture);
        builder.push((ConfigDefinitionImpl)definition1.get(0));

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      /sns:\n"
                + "        .meta:delete: true";
        final List<AbstractDefinitionImpl> definition2 = ModelTestUtils.parseNoSort(yaml2);
        builder.push((ConfigDefinitionImpl)definition2.get(0));

        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[sns[1], sns[2]]", collectionToString(a.getNodes()));
        assertEquals("[jcr:primaryType, property2]", collectionToString(a.getNode("sns[1]").getProperties()));
        assertEquals("[jcr:primaryType, property3]", collectionToString(a.getNode("sns[2]").getProperties()));
    }

    @Test
    public void delete_middle_sns() throws Exception {
        final List<AbstractDefinitionImpl> definition1 = ModelTestUtils.parseNoSort(snsFixture);
        builder.push((ConfigDefinitionImpl)definition1.get(0));

        final String delete = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      /sns[2]:\n"
                + "        .meta:delete: true";
        final List<AbstractDefinitionImpl> definition2 = ModelTestUtils.parseNoSort(delete);
        builder.push((ConfigDefinitionImpl)definition2.get(0));

        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[sns[1], sns[2]]", collectionToString(a.getNodes()));
        assertEquals("[jcr:primaryType, property1]", collectionToString(a.getNode("sns[1]").getProperties()));
        assertEquals("[jcr:primaryType, property3]", collectionToString(a.getNode("sns[2]").getProperties()));
    }

    @Test
    public void delete_last_sns() throws Exception {
        final List<AbstractDefinitionImpl> definition1 = ModelTestUtils.parseNoSort(snsFixture);
        builder.push((ConfigDefinitionImpl)definition1.get(0));

        final String delete = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      /sns[3]:\n"
                + "        .meta:delete: true";
        final List<AbstractDefinitionImpl> definition2 = ModelTestUtils.parseNoSort(delete);
        builder.push((ConfigDefinitionImpl)definition2.get(0));

        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[sns[1], sns[2]]", collectionToString(a.getNodes()));
        assertEquals("[jcr:primaryType, property1]", collectionToString(a.getNode("sns[1]").getProperties()));
        assertEquals("[jcr:primaryType, property2]", collectionToString(a.getNode("sns[2]").getProperties()));
    }

    @Test
    public void delete_all_sns() throws Exception {
        final List<AbstractDefinitionImpl> definition1 = ModelTestUtils.parseNoSort(snsFixture);
        builder.push((ConfigDefinitionImpl)definition1.get(0));

        final String delete = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      /sns[1]:\n"
                + "        .meta:delete: true";
        final List<AbstractDefinitionImpl> definition2 = ModelTestUtils.parseNoSort(delete);
        builder.push((ConfigDefinitionImpl)definition2.get(0));
        builder.push((ConfigDefinitionImpl)definition2.get(0));
        builder.push((ConfigDefinitionImpl)definition2.get(0));

        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[]", collectionToString(a.getNodes()));
    }

    @Test
    public void reorder_existing_sns_to_earlier() throws Exception {
        final List<AbstractDefinitionImpl> definition1 = ModelTestUtils.parseNoSort(snsFixture);
        builder.push((ConfigDefinitionImpl)definition1.get(0));

        final String orderBefore = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      /sns[3]:\n"
                + "        .meta:order-before: sns[1]";
        final List<AbstractDefinitionImpl> definition2 = ModelTestUtils.parseNoSort(orderBefore);
        builder.push((ConfigDefinitionImpl)definition2.get(0));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[sns[1], sns[2], sns[3]]", collectionToString(a.getNodes()));
        assertEquals("[jcr:primaryType, property3]", collectionToString(a.getNode("sns[1]").getProperties()));
        assertEquals("[jcr:primaryType, property1]", collectionToString(a.getNode("sns[2]").getProperties()));
        assertEquals("[jcr:primaryType, property2]", collectionToString(a.getNode("sns[3]").getProperties()));
    }

    @Test
    public void reorder_existing_sns_to_later() throws Exception {
        final List<AbstractDefinitionImpl> definition1 = ModelTestUtils.parseNoSort(snsFixture);
        builder.push((ConfigDefinitionImpl)definition1.get(0));

        final String orderBefore = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      /sns[1]:\n"
                + "        .meta:order-before: sns[3]";
        final List<AbstractDefinitionImpl> definition2 = ModelTestUtils.parseNoSort(orderBefore);
        builder.push((ConfigDefinitionImpl)definition2.get(0));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[sns[1], sns[2], sns[3]]", collectionToString(a.getNodes()));
        assertEquals("[jcr:primaryType, property2]", collectionToString(a.getNode("sns[1]").getProperties()));
        assertEquals("[jcr:primaryType, property1]", collectionToString(a.getNode("sns[2]").getProperties()));
        assertEquals("[jcr:primaryType, property3]", collectionToString(a.getNode("sns[3]").getProperties()));
    }

    @Test
    public void reject_sns_in_node_if_lower_index_is_missing() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /sns[2]:\n"
                + "        jcr:primaryType: foo\n";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        try {
            builder.push((ConfigDefinitionImpl)definitions.get(0));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("test-group/test-project/test-module [config: string] defines node '/a/sns[2]', but no sibling named 'sns[1]' was found", e.getMessage());
        }
    }

    @Test
    public void reject_sns_in_root_if_lower_index_is_missing() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "    /a/sns[2]:\n"
                + "      jcr:primaryType: foo\n";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        try {
            builder.push((ConfigDefinitionImpl)definitions.get(1));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("test-group/test-project/test-module [config: string] defines node '/a/sns[2]', but no sibling named 'sns[1]' was found", e.getMessage());
        }
    }

    @Test
    public void property_initial_value_for_system_property() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property:\n"
                + "        .meta:category: system\n"
                + "        value: value\n";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ConfigDefinitionImpl)definitions.get(0));

        final ConfigurationNodeImpl root = builder.finishModule().build();
        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[jcr:primaryType, property]", collectionToString(a.getProperties()));
        assertEquals(ConfigurationItemCategory.SYSTEM, a.getChildPropertyCategory("property"));
        assertEquals("value", valueToString(a.getProperty("property")));
    }

    @Test
    public void property_empty_initial_values_for_system_property() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property:\n"
                + "        .meta:category: system\n"
                + "        value: []\n";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ConfigDefinitionImpl)definitions.get(0));

        final ConfigurationNodeImpl root = builder.finishModule().build();
        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[jcr:primaryType, property]", collectionToString(a.getProperties()));
        assertEquals(ConfigurationItemCategory.SYSTEM, a.getChildPropertyCategory("property"));
        assertEquals("[]", valuesToString(a.getProperty("property")));
    }

    @Test
    public void property_meta_category_override_to_system() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property: value\n";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ConfigDefinitionImpl)definitions.get(0));

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      property:\n"
                + "        .meta:category: system\n";

        final List<AbstractDefinitionImpl> definitions2 = ModelTestUtils.parseNoSort(yaml2);
        builder.push((ConfigDefinitionImpl)definitions2.get(0));

        final ConfigurationNodeImpl root = builder.finishModule().build();
        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[jcr:primaryType]", collectionToString(a.getProperties()));
        assertEquals(ConfigurationItemCategory.SYSTEM, a.getChildPropertyCategory("property"));
    }

    @Test
    public void property_meta_category_override_to_system_with_different_type_fail() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property: value\n";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ConfigDefinitionImpl)definitions.get(0));

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      property:\n"
                + "        .meta:category: system\n"
                + "        value: [value]\n";

        final List<AbstractDefinitionImpl> definitions2 = ModelTestUtils.parseNoSort(yaml2);
        try {
            builder.push((ConfigDefinitionImpl)definitions2.get(0));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("Property /a/property already exists with type 'single', as determined by [test-group/test-project/test-module [config: string]], but type 'list' is requested in test-group/test-project/test-module [config: string]."
                    , e.getMessage());
        }
    }

    @Test
    public void property_meta_category_override_to_system_with_different_type_override() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property: value\n";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ConfigDefinitionImpl)definitions.get(0));

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      property:\n"
                + "        .meta:category: system\n"
                + "        operation: override\n"
                + "        value: [value]\n";

        final List<AbstractDefinitionImpl> definitions2 = ModelTestUtils.parseNoSort(yaml2);
        builder.push((ConfigDefinitionImpl)definitions2.get(0));

        final ConfigurationNodeImpl root = builder.finishModule().build();
        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[jcr:primaryType, property]", collectionToString(a.getProperties()));
        assertEquals("[value]", valuesToString(a.getProperty("property")));
        assertEquals(ConfigurationItemCategory.SYSTEM, a.getChildPropertyCategory("property"));
    }

    @Test
    public void property_meta_category_override_from_empty_system_to_config() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property:\n"
                + "        .meta:category: system\n";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ConfigDefinitionImpl)definitions.get(0));

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      property:\n"
                + "        .meta:category: config\n"
                + "        type: boolean\n"
                + "        value: true\n";

        final List<AbstractDefinitionImpl> definitions2 = ModelTestUtils.parseNoSort(yaml2);
        builder.push((ConfigDefinitionImpl)definitions2.get(0));

        final ConfigurationNodeImpl root = builder.finishModule().build();
        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[jcr:primaryType, property]", collectionToString(a.getProperties()));
        assertEquals("true", valueToString(a.getProperty("property")));
        assertEquals(ConfigurationItemCategory.CONFIG, a.getChildPropertyCategory("property"));
    }

    @Test
    public void property_meta_category_override_from_system_to_config() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property:\n"
                + "        .meta:category: system\n"
                + "        value: value\n";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ConfigDefinitionImpl)definitions.get(0));

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      property:\n"
                + "        .meta:category: config\n"
                + "        value: updated\n";

        final List<AbstractDefinitionImpl> definitions2 = ModelTestUtils.parseNoSort(yaml2);
        builder.push((ConfigDefinitionImpl)definitions2.get(0));

        final ConfigurationNodeImpl root = builder.finishModule().build();
        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[jcr:primaryType, property]", collectionToString(a.getProperties()));
        assertEquals("updated", valueToString(a.getProperty("property")));
        assertEquals(ConfigurationItemCategory.CONFIG, a.getChildPropertyCategory("property"));
    }

    @Test
    public void property_meta_category_override_from_system_to_config_different_type_fail() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property:\n"
                + "        .meta:category: system\n"
                + "        value: value\n";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ConfigDefinitionImpl)definitions.get(0));

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      property:\n"
                + "        .meta:category: config\n"
                + "        value: [value]\n";

        final List<AbstractDefinitionImpl> definitions2 = ModelTestUtils.parseNoSort(yaml2);
        try {
            builder.push((ConfigDefinitionImpl)definitions2.get(0));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("Property /a/property already exists with type 'single', as determined by [test-group/test-project/test-module [config: string]], but type 'list' is requested in test-group/test-project/test-module [config: string]."
                    , e.getMessage());
        }
    }

    @Test
    public void property_meta_category_configuration_without_value_fail() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      property:\n"
                + "        .meta:category: config\n";

        try {
            ModelTestUtils.parseNoSort(yaml);
            fail("Should have thrown exception");
        } catch (ParserException e) {
            assertEquals("Property '.meta:category: config' requires specifying replacement/overriding value(s)", e.getMessage());
        }
    }

    @Test
    public void node_meta_category_override_to_system() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "      /c:\n"
                + "        jcr:primaryType: foo\n"
                + "      /d:\n"
                + "        jcr:primaryType: foo\n";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ConfigDefinitionImpl)definitions.get(0));

        // test both as root and as node as they have different code paths
        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      /b:\n"
                + "        .meta:category: system\n"
                + "    /a/c:\n"
                + "      .meta:category: system\n";

        final List<AbstractDefinitionImpl> definitions2 = ModelTestUtils.parseNoSort(yaml2);
        builder.push((ConfigDefinitionImpl)definitions2.get(0));
        builder.push((ConfigDefinitionImpl)definitions2.get(1));

        final ConfigurationNodeImpl root = builder.finishModule().build();
        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[d[1]]", collectionToString(a.getNodes()));
        assertEquals(ConfigurationItemCategory.SYSTEM, a.getChildNodeCategory("b[1]"));
        assertEquals(ConfigurationItemCategory.SYSTEM, a.getChildNodeCategory("c[1]"));
    }

    @Test
    public void node_meta_category_override_to_runtime_removes_all_sns() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /c[1]:\n"
                + "        jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "      /c[2]:\n"
                + "        jcr:primaryType: foo\n"
                + "      /d:\n"
                + "        jcr:primaryType: foo\n"
                + "      /c[3]:\n"
                + "        jcr:primaryType: foo\n"
                + "    /a/c:\n"
                + "      .meta:category: system\n";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);

        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));

        final ConfigurationNodeImpl root = builder.finishModule().build();
        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[b[1], d[1]]", collectionToString(a.getNodes()));
        assertEquals(ConfigurationItemCategory.SYSTEM, a.getChildNodeCategory("c[1]"));
    }

    @Test
    public void definitions_below_non_configuration_node_are_ignored() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      .meta:category: system\n"
                + "    /b:\n"
                + "      jcr:primaryType: foo\n"
                + "      /c:\n"
                + "        .meta:category: system\n";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));

        // test both as root and as node as they have different code paths
        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      property: value\n"
                + "    /b:\n"
                + "      /c:\n"
                + "        property: value";

        final List<AbstractDefinitionImpl> definitions2 = ModelTestUtils.parseNoSort(yaml2);

        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions2.get(0));
            builder.push((ConfigDefinitionImpl)definitions2.get(1));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("test-group/test-project/test-module [config: string] tries to modify non-configuration node '/a', skipping.")));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("test-group/test-project/test-module [config: string] tries to modify non-configuration node '/b/c', skipping.")));
        }

        final String yaml3 = "definitions:\n"
                + "  config:\n"
                + "    /a/b:\n"
                + "      jcr:primaryType: foo";

        final List<AbstractDefinitionImpl> definitions3 = ModelTestUtils.parseNoSort(yaml3);

        try {
            builder.push((ConfigDefinitionImpl)definitions3.get(0));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("test-group/test-project/test-module [config: string] contains definition rooted at unreachable node '/a/b'. Closest ancestor is at '/'.", e.getMessage());
        }

        final ConfigurationNodeImpl root = builder.finishModule().build();
        assertEquals("[b[1]]", collectionToString(root.getNodes()));
        assertEquals("[jcr:uuid, jcr:primaryType, jcr:mixinTypes]", collectionToString(root.getProperties()));
    }

    @Test
    public void node_meta_category_override_from_runtime_to_configuration() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        .meta:category: system\n"
                + "      /c:\n"
                + "        .meta:category: system\n";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ConfigDefinitionImpl)definitions.get(0));

        final String yaml2 = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      /b:\n"
                + "        .meta:category: config\n"
                + "        jcr:primaryType: foo\n"
                + "        property: value\n"
                + "      /c:\n"
                + "        .meta:category: config\n";

        final List<AbstractDefinitionImpl> definitions2 = ModelTestUtils.parseNoSort(yaml2);
        builder.push((ConfigDefinitionImpl)definitions2.get(0));

        final ConfigurationNodeImpl root = builder.finishModule().build();
        final ConfigurationNodeImpl a = root.getNode("a[1]");
        assertEquals("[b[1]]", collectionToString(a.getNodes()));
        assertEquals("[jcr:primaryType, property]", collectionToString(a.getNode("b[1]").getProperties()));
        assertEquals(ConfigurationItemCategory.CONFIG, a.getChildNodeCategory("b[1]"));
        assertEquals(ConfigurationItemCategory.CONFIG, a.getChildNodeCategory("c[1]"));
    }

    @Test
    public void meta_residual_child_node_category_override() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /a:\n"
                + "      jcr:primaryType: foo\n"
                + "      /b:\n"
                + "        jcr:primaryType: foo\n"
                + "        .meta:residual-child-node-category: content\n"
                + "    /a/b:\n"
                + "      .meta:residual-child-node-category: system\n";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.push((ConfigDefinitionImpl)definitions.get(1));

        final ConfigurationNodeImpl root = builder.finishModule().build();
        assertEquals(ConfigurationItemCategory.SYSTEM, root.getNode("a[1]").getNode("b[1]").getResidualNodeCategory());
    }

    @Test
    public void test_root_residual_child_settings() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /:\n"
                + "      default: value\n"
                + "      override:\n"
                + "        .meta:category: system\n"
                + "      /default:\n"
                + "        jcr:primaryType: foo\n"
                + "      /override:\n"
                + "        .meta:category: content\n";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ConfigDefinitionImpl)definitions.get(0));
        final ConfigurationNodeImpl root = builder.finishModule().build();

        assertEquals(ConfigurationItemCategory.CONFIG,  root.getChildNodeCategory("default[1]"));
        assertEquals(ConfigurationItemCategory.SYSTEM, root.getChildNodeCategory("non-existing-node[1]"));
        assertEquals(ConfigurationItemCategory.CONTENT, root.getChildNodeCategory("override[1]"));
        assertEquals(ConfigurationItemCategory.CONTENT, root.getChildNodeCategory("override[2]"));
        assertEquals(ConfigurationItemCategory.CONFIG,  root.getChildPropertyCategory("default"));
        assertEquals(ConfigurationItemCategory.CONFIG,  root.getChildPropertyCategory("non-existing-property"));
        assertEquals(ConfigurationItemCategory.SYSTEM, root.getChildPropertyCategory("override"));
    }

    @Test
    public void test_regular_node_residual_child_settings() throws Exception {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /node:\n"
                + "      jcr:primaryType: foo\n"
                + "      default: value\n"
                + "      override:\n"
                + "        .meta:category: system\n"
                + "      /default:\n"
                + "        jcr:primaryType: foo\n"
                + "      /override:\n"
                + "        .meta:category: content\n";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ConfigDefinitionImpl)definitions.get(0));
        final ConfigurationNodeImpl root = builder.finishModule().build();
        final ConfigurationNodeImpl node = root.getNode("node[1]");

        assertEquals(ConfigurationItemCategory.CONFIG,  node.getChildNodeCategory("default[1]"));
        assertEquals(ConfigurationItemCategory.CONFIG,  node.getChildNodeCategory("non-existing-node[1]"));
        assertEquals(ConfigurationItemCategory.CONTENT, node.getChildNodeCategory("override[1]"));
        assertEquals(ConfigurationItemCategory.CONTENT, node.getChildNodeCategory("override[2]"));
        assertEquals(ConfigurationItemCategory.CONFIG,  node.getChildPropertyCategory("default"));
        assertEquals(ConfigurationItemCategory.CONFIG,  node.getChildPropertyCategory("non-existing-property"));
        assertEquals(ConfigurationItemCategory.SYSTEM, node.getChildPropertyCategory("override"));
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
                + "        .meta:category: system\n"
                + "      /default:\n"
                + "        jcr:primaryType: foo\n"
                + "      /override:\n"
                + "        .meta:category: system\n";

        final List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ConfigDefinitionImpl)definitions.get(0));
        final ConfigurationNodeImpl root = builder.finishModule().build();
        final ConfigurationNodeImpl node = root.getNode("node[1]");

        assertEquals(ConfigurationItemCategory.CONFIG,  node.getChildNodeCategory("default[1]"));
        assertEquals(ConfigurationItemCategory.CONTENT, node.getChildNodeCategory("non-existing-node[1]"));
        assertEquals(ConfigurationItemCategory.SYSTEM, node.getChildNodeCategory("override[1]"));
        assertEquals(ConfigurationItemCategory.SYSTEM, node.getChildNodeCategory("override[2]"));
        assertEquals(ConfigurationItemCategory.CONFIG,  node.getChildPropertyCategory("default"));
        assertEquals(ConfigurationItemCategory.CONFIG,  node.getChildPropertyCategory("non-existing-property"));
        assertEquals(ConfigurationItemCategory.SYSTEM, node.getChildPropertyCategory("override"));
    }

    @Test
    public void test_delayed_ordering_success() throws Exception {
        String yaml = "definitions:\n"
                + "  config:\n"
                + "    /node:\n"
                + "      jcr:primaryType: foo\n"
                + "      /first:\n"
                + "        jcr:primaryType: foo\n"
                + "        .meta:order-before: second\n";

        List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ConfigDefinitionImpl)definitions.get(0));

        yaml = "definitions:\n"
                + "  config:\n"
                + "    /node:\n"
                + "      /second:\n"
                + "        jcr:primaryType: foo\n";
        definitions = ModelTestUtils.parseNoSort(yaml);
        try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ConfigurationTreeBuilder.class).build()) {
            builder.push((ConfigDefinitionImpl)definitions.get(0));
            assertTrue(interceptor.messages()
                    .anyMatch(m->m.equals("Unnecessary orderBefore: 'second' for node '/node/first' defined in 'test-group/test-project/test-module [config: string]': already ordered before sibling 'second[1]'.")));
        }
        final ConfigurationNodeImpl root = builder.finishModule().build();
        final ConfigurationNodeImpl node = root.getNode("node[1]");

        final Iterator<ConfigurationNodeImpl> nodeIterator = node.getNodes().iterator();
        assertEquals("first[1]", nodeIterator.next().getName());
        assertEquals("second[1]", nodeIterator.next().getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_delayed_ordering_failure() throws Exception {
        String yaml = "definitions:\n"
                + "  config:\n"
                + "    /node:\n"
                + "      jcr:primaryType: foo\n"
                + "      /first:\n"
                + "        jcr:primaryType: foo\n"
                + "        .meta:order-before: doesnotexist\n";

        List<AbstractDefinitionImpl> definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ConfigDefinitionImpl)definitions.get(0));

        yaml = "definitions:\n"
                + "  config:\n"
                + "    /node:\n"
                + "      /second:\n"
                + "        jcr:primaryType: foo\n";
        definitions = ModelTestUtils.parseNoSort(yaml);
        builder.push((ConfigDefinitionImpl)definitions.get(0));
        builder.finishModule().build();
    }
}
