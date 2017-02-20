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

import org.junit.Test;
import org.onehippo.cm.api.model.ConfigurationNode;
import org.onehippo.cm.api.model.ConfigurationProperty;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.PropertyType;
import org.onehippo.cm.api.model.ValueType;
import org.onehippo.cm.impl.model.ConfigurationNodeImpl;
import org.onehippo.cm.impl.model.ContentDefinitionImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.onehippo.cm.impl.model.ModelTestUtils.parseNoSort;

public class ConfigurationTreeBuilderTest extends AbstractBuilderBaseTest {

    private final ConfigurationTreeBuilder builder = new ConfigurationTreeBuilder();

    @Test
    public void simple_single_definition() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - property1: bla1\n"
                + "    - property2: bla2";

        final List<Definition> definitions = parseNoSort(yaml);
        final ContentDefinitionImpl definition = (ContentDefinitionImpl)definitions.get(0);
        builder.push(definition);
        final ConfigurationNodeImpl root = builder.build();

        assertEquals("[]", sortedCollectionToString(root.getProperties()));
        assertEquals("[a]", sortedCollectionToString(root.getNodes()));
        final ConfigurationNode a = root.getNodes().get("a");
        assertEquals("[property1, property2]", sortedCollectionToString(a.getProperties()));
        assertEquals("[]", sortedCollectionToString(a.getNodes()));
    }

    @Test
    public void complex_single_definition() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - property2: bla2\n"
                + "    - property1: bla1\n"
                + "    - /b:\n"
                + "      - /d:\n"
                + "        - property4: bla4\n"
                + "      - property3: bla3\n"
                + "      - /c:\n"
                + "        - property5: bla5";

        final List<Definition> definitions = parseNoSort(yaml);
        final ContentDefinitionImpl definition = (ContentDefinitionImpl)definitions.get(0);
        builder.push(definition);
        final ConfigurationNodeImpl root = builder.build();

        assertEquals("[]", sortedCollectionToString(root.getProperties()));
        assertEquals("[a]", sortedCollectionToString(root.getNodes()));
        final ConfigurationNode a = root.getNodes().get("a");
        assertEquals("[property2, property1]", sortedCollectionToString(a.getProperties()));
        assertEquals("[b]", sortedCollectionToString(a.getNodes()));
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[property3]", sortedCollectionToString(b.getProperties()));
        assertEquals("[d, c]", sortedCollectionToString(b.getNodes()));
        final ConfigurationNode c = b.getNodes().get("c");
        assertEquals("[property5]", sortedCollectionToString(c.getProperties()));
        assertEquals("[]", sortedCollectionToString(c.getNodes()));
        final ConfigurationNode d = b.getNodes().get("d");
        assertEquals("[property4]", sortedCollectionToString(d.getProperties()));
        assertEquals("[]", sortedCollectionToString(d.getNodes()));
    }

    @Test
    public void invalid_single_definition() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a/b:\n"
                + "    - property1: bla1";

        final List<Definition> definitions = parseNoSort(yaml);
        final ContentDefinitionImpl definition = (ContentDefinitionImpl)definitions.get(0);

        try {
            builder.push(definition);
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("test-configuration/test-project/test-module [string] contains definition rooted at unreachable node '/a/b'. Closest ancestor is at '/'.", e.getMessage());
        }
    }

    @Test
    public void root_node_property() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /:\n"
                + "    - property1: bla1\n"
                + "    - /a:\n"
                + "      - property2: [bla2, bla3]";

        final List<Definition> definitions = parseNoSort(yaml);
        final ContentDefinitionImpl definition = (ContentDefinitionImpl)definitions.get(0);
        builder.push(definition);
        final ConfigurationNodeImpl root = builder.build();

        assertEquals("[property1]", sortedCollectionToString(root.getProperties()));
        assertEquals(PropertyType.SINGLE, root.getProperties().get("property1").getType());
        assertEquals("[a]", sortedCollectionToString(root.getNodes()));
        final ConfigurationNode a = root.getNodes().get("a");
        assertEquals("[property2]", sortedCollectionToString(a.getProperties()));
        assertEquals(PropertyType.LIST, a.getProperties().get("property2").getType());
        assertEquals("[]", sortedCollectionToString(a.getNodes()));
    }

    @Test
    public void merge_two_definitions_same_module() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - property2: bla2\n"
                + "    - property1: bla1\n"
                + "    - /b:\n"
                + "      - /d:\n"
                + "        - property4: bla4\n"
                + "      - property8: bla8\n"
                + "      - /c:\n"
                + "        - property5: bla5\n"
                + "  - /a/d:\n"
                + "    - property7: bla7";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        assertEquals("[]", sortedCollectionToString(root.getProperties()));
        assertEquals("[a]", sortedCollectionToString(root.getNodes()));
        final ConfigurationNode a = root.getNodes().get("a");
        assertEquals("[property2, property1]", sortedCollectionToString(a.getProperties()));
        assertEquals("[b, d]", sortedCollectionToString(a.getNodes()));
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[property8]", sortedCollectionToString(b.getProperties()));
        assertEquals("[d, c]", sortedCollectionToString(b.getNodes()));
        final ConfigurationNode d = a.getNodes().get("d");
        assertEquals("[property7]", sortedCollectionToString(d.getProperties()));
        assertEquals("[]", sortedCollectionToString(d.getNodes()));
    }

    @Test
    public void merge_two_definitions_separate_modules() throws Exception {
        final String yaml1 = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - property2: bla2\n"
                + "    - property1: bla1\n"
                + "    - /b:\n"
                + "      - /d:\n"
                + "        - property4: bla4\n"
                + "      - property8: bla8\n"
                + "      - /c:\n"
                + "        - property5: bla5";

        final List<Definition> definitions1 = parseNoSort(yaml1);
        builder.push((ContentDefinitionImpl)definitions1.get(0));

        final String yaml2 = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - property3: bla3\n"
                + "    - /e:\n"
                + "      - property6: bla6\n"
                + "    - /b:\n"
                + "      - property7: bla7\n"
                + "      - /f:\n"
                + "        - property9: bla9";

        final List<Definition> definitions2 = parseNoSort(yaml2);
        builder.push((ContentDefinitionImpl)definitions2.get(0));
        final ConfigurationNodeImpl root = builder.build();

        assertEquals("[]", sortedCollectionToString(root.getProperties()));
        assertEquals("[a]", sortedCollectionToString(root.getNodes()));
        final ConfigurationNode a = root.getNodes().get("a");
        assertEquals("[property2, property1, property3]", sortedCollectionToString(a.getProperties()));
        assertEquals("[b, e]", sortedCollectionToString(a.getNodes()));
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[property8, property7]", sortedCollectionToString(b.getProperties()));
        assertEquals("[d, c, f]", sortedCollectionToString(b.getNodes()));
        final ConfigurationNode e = a.getNodes().get("e");
        assertEquals("[property6]", sortedCollectionToString(e.getProperties()));
        assertEquals("[]", sortedCollectionToString(e.getNodes()));
        final ConfigurationNode f = b.getNodes().get("f");
        assertEquals("[property9]", sortedCollectionToString(f.getProperties()));
        assertEquals("[]", sortedCollectionToString(f.getNodes()));
    }

    @Test
    public void reorder_existing_node_to_first() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - property1: [bla1]\n"
                + "    - /c:\n"
                + "      - property2: [bla2]\n"
                + "    - /d:\n"
                + "      - property3: [bla3]\n"
                + "  - /a/d:\n"
                + "    - .meta:order-before: b";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        assertEquals("[d, b, c]", sortedCollectionToString(a.getNodes()));
    }

    @Test
    public void reorder_existing_node_to_earlier() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - property1: [bla1]\n"
                + "    - /c:\n"
                + "      - property2: [bla2]\n"
                + "    - /d:\n"
                + "      - property3: [bla3]\n"
                + "    - /e:\n"
                + "      - property4: [bla4]\n"
                + "    - /f:\n"
                + "      - property5: [bla5]\n"
                + "  - /a/e:\n"
                + "    - .meta:order-before: c";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        assertEquals("[b, e, c, d, f]", sortedCollectionToString(a.getNodes()));
    }

    @Test
    public void reorder_existing_node_to_later() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - property1: [bla1]\n"
                + "    - /c:\n"
                + "      - property2: [bla2]\n"
                + "    - /d:\n"
                + "      - property3: [bla3]\n"
                + "    - /e:\n"
                + "      - property4: [bla4]\n"
                + "    - /f:\n"
                + "      - property5: [bla5]\n"
                + "  - /a/c:\n"
                + "    - .meta:order-before: f";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        assertEquals("[b, d, e, c, f]", sortedCollectionToString(a.getNodes()));
    }

    @Test
    public void reorder_new_root() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - /c:\n"
                + "        - property1: [bla1]\n"
                + "      - /d:\n"
                + "        - property2: [bla2]\n"
                + "  - /a/b/e:\n"
                + "    - .meta:order-before: d";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[c, e, d]", sortedCollectionToString(b.getNodes()));
    }

    @Test
    public void reorder_new_child() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - /c:\n"
                + "        - property1: [bla1]\n"
                + "      - /d:\n"
                + "        - property2: [bla2]\n"
                + "  - /a/b:\n"
                + "    - /e:\n"
                + "      - .meta:order-before: c";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[e, c, d]", sortedCollectionToString(b.getNodes()));
    }

    @Test
    public void reorder_existing_child() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - /c:\n"
                + "        - property1: [bla1]\n"
                + "      - /d:\n"
                + "        - property2: [bla2]\n"
                + "  - /a/b:\n"
                + "    - /d:\n"
                + "      - .meta:order-before: c";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[d, c]", sortedCollectionToString(b.getNodes()));
    }

    @Test
    public void delete_leaf_node() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - property1: bla1\n"
                + "    - /c:\n"
                + "      - property2: bla2\n"
                + "    - /d:\n"
                + "      - property3: bla3\n"
                + "  - /a/c:\n"
                + "    - .meta:delete: true";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        assertEquals("[b, d]", sortedCollectionToString(a.getNodes()));
    }

    @Test
    public void delete_subtree() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - /c:\n"
                + "        - property2: bla2\n"
                + "      - /d:\n"
                + "        - property3: bla3\n"
                + "  - /a/b:\n"
                + "    - .meta:delete: true";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        assertEquals("[]", sortedCollectionToString(a.getNodes()));
    }

    @Test
    public void delete_embedded_in_definition_tree() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - /c:\n"
                + "        - property2: bla2\n"
                + "      - /d:\n"
                + "        - property3: bla3\n"
                + "  - /a/b:\n"
                + "    - property1: bla1\n"
                + "    - /c:\n"
                + "      - property4: bla4\n"
                + "    - /d:\n"
                + "      - .meta:delete: true\n"
                + "    - /e:\n"
                + "      - property5: bla5";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        final ConfigurationNode c = b.getNodes().get("c");
        final ConfigurationNode e = b.getNodes().get("e");
        assertEquals("[c, e]", sortedCollectionToString(b.getNodes()));
        assertEquals("[property1]", sortedCollectionToString(b.getProperties()));
        assertEquals("[property2, property4]", sortedCollectionToString(c.getProperties()));
        assertEquals("[property5]", sortedCollectionToString(e.getProperties()));
    }

    @Test
    public void delete_at_root_level() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /:\n"
                + "    - .meta:delete: true";

        final List<Definition> definitions = parseNoSort(yaml);

        try {
            builder.push((ContentDefinitionImpl) definitions.get(0));
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Deleting the root node is not supported.", e.getMessage());
        }
    }

    @Test
    public void modify_deleted_node() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - /c:\n"
                + "        - property1: bla1\n"
                + "  - /a/b:\n"
                + "    - /c:\n"
                + "      - .meta:delete: true\n"
                + "  - /a/b/c:\n"
                + "    - property2: bla2";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        builder.push((ContentDefinitionImpl) definitions.get(2));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[]", sortedCollectionToString(b.getNodes()));
    }

    @Test
    public void definition_root_below_deleted_node() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - /c:\n"
                + "        - property1: bla1\n"
                + "  - /a/b:\n"
                + "    - /c:\n"
                + "      - .meta:delete: true\n"
                + "  - /a/b/c/d:\n"
                + "    - property2: bla2";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        builder.push((ContentDefinitionImpl) definitions.get(2));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[]", sortedCollectionToString(b.getNodes()));
    }

    @Test
    public void non_existent_definition_root_with_delete_flag() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - property1: bla1\n"
                + "  - /a/b/c:\n"
                + "    - .meta:delete: true";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));

        try {
            builder.push((ContentDefinitionImpl) definitions.get(1));
            fail("Should have thrown an exception");
        } catch (IllegalArgumentException e) {
            assertEquals("test-configuration/test-project/test-module [string]: Trying to delete node /a/b/c that does not exist.", e.getMessage());
        }
    }

    @Test
    public void non_existent_definition_node_with_delete_flag() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - property1: bla1\n"
                + "  - /a/b:\n"
                + "    - /c:\n"
                + "      - .meta:delete: true";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));

        try {
            builder.push((ContentDefinitionImpl) definitions.get(1));
            fail("Should have thrown an exception");
        } catch (IllegalArgumentException e) {
            assertEquals("test-configuration/test-project/test-module [string]: Trying to delete node /a/b/c that does not exist.", e.getMessage());
        }
    }

    @Test
    public void double_delete() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - /c:\n"
                + "        - property1: bla1\n"
                + "  - /a/b/c:\n"
                + "    - .meta:delete: true\n"
                + "  - /a/b:\n"
                + "    - /c:\n"
                + "      - .meta:delete: true";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        builder.push((ContentDefinitionImpl) definitions.get(2));

        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[]", sortedCollectionToString(b.getNodes()));
    }

    @Test
    public void modify_deleted_non_root_definition_node() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - /c:\n"
                + "        - /d:\n"
                + "          - property1: bla1\n"
                + "  - /a/b:\n"
                + "    - /c:\n"
                + "      - /d:\n"
                + "        - .meta:delete: true\n"
                + "  - /a/b/c:\n"
                + "    - /d:\n"
                + "      - property2: bla2";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        builder.push((ContentDefinitionImpl) definitions.get(2));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        final ConfigurationNode c = b.getNodes().get("c");
        assertEquals("[]", sortedCollectionToString(c.getNodes()));
    }

    @Test
    public void replace_single_property() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - property1: bla1\n"
                + "    - /b:\n"
                + "      - property2: bla2\n"
                + "  - /a/b:\n"
                + "    - property2: bla3";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        assertEquals("[]", sortedCollectionToString(root.getProperties()));
        assertEquals("[a]", sortedCollectionToString(root.getNodes()));
        final ConfigurationNode a = root.getNodes().get("a");
        assertEquals("[property1]", sortedCollectionToString(a.getProperties()));
        assertEquals("[b]", sortedCollectionToString(a.getNodes()));
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[property2]", sortedCollectionToString(b.getProperties()));
        assertEquals("[]", sortedCollectionToString(b.getNodes()));
        final ConfigurationProperty property2 = b.getProperties().get("property2");
        assertEquals(PropertyType.SINGLE, property2.getType());
        assertEquals(ValueType.STRING, property2.getValueType());
        assertEquals("bla3", property2.getValue().getString());
    }

    @Test
    public void replace_single_property_with_equal_value() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - property1: bla1\n"
                + "  - /a/b:\n"
                + "    - property1: bla1";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[property1]", sortedCollectionToString(b.getProperties()));
        assertEquals("bla1", valueToString(b.getProperties().get("property1")));
    }

    @Test
    public void replace_list_property() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - property1: bla1\n"
                + "    - /b:\n"
                + "      - property2: [bla2]\n"
                + "  - /a/b:\n"
                + "    - property2: [bla3, bla4]";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[property2]", sortedCollectionToString(b.getProperties()));
        final ConfigurationProperty property2 = b.getProperties().get("property2");
        assertEquals(PropertyType.LIST, property2.getType());
        assertEquals(ValueType.STRING, property2.getValueType());
        assertEquals("bla3", property2.getValues()[0].getString());
        assertEquals("bla4", property2.getValues()[1].getString());
    }

    @Test
    public void replace_list_property_with_equal_values() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - property1: [bla1, bla2]\n"
                + "  - /a/b:\n"
                + "    - property1: [bla1, bla2]";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl)definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[property1]", sortedCollectionToString(b.getProperties()));
        assertEquals("[bla1, bla2]", valuesToString(b.getProperties().get("property1")));
    }

    @Test
    public void add_property_values() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - property1: [bla1, bla2]\n"
                + "  - /a/b:\n"
                + "    - property1:\n"
                + "        operation: add\n"
                + "        value: [bla3, bla2]";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[property1]", sortedCollectionToString(b.getProperties()));
        assertEquals("[bla1, bla2, bla3, bla2]", valuesToString(b.getProperties().get("property1")));
    }

    @Test
    public void add_property_values_for_new_property() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - property1: [bla1, bla2]\n"
                + "  - /a/b:\n"
                + "    - property2:\n"
                + "        operation: add\n"
                + "        value: [bla3, bla2]";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[property1, property2]", sortedCollectionToString(b.getProperties()));
        assertEquals("[bla3, bla2]", valuesToString(b.getProperties().get("property2")));
    }

    @Test
    public void reject_property_if_different_kind() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - property1: bla1\n"
                + "    - /b:\n"
                + "      - property2: bla2\n"
                + "  - /a/b:\n"
                + "    - property2: [bla3, bla4]";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));

        try {
            builder.push((ContentDefinitionImpl) definitions.get(1));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("Property /a/b/property2 already exists with type 'single', as determined by [test-configuration/test-project/test-module [string]], but type 'list' is requested in test-configuration/test-project/test-module [string].", e.getMessage());
        }
    }

    @Test
    public void override_property_with_different_kind() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - property1: bla1\n"
                + "  - /a/b:\n"
                + "    - property1:\n"
                + "        operation: override\n"
                + "        value: [bla2, bla3]";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[property1]", sortedCollectionToString(b.getProperties()));
        assertEquals("[bla2, bla3]", valuesToString(b.getProperties().get("property1")));
    }

    @Test
    public void reject_property_if_different_type() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - property1: bla1\n"
                + "    - /b:\n"
                + "      - property2: [bla2]\n"
                + "  - /a/b:\n"
                + "    - property2: [34]";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));

        try {
            builder.push((ContentDefinitionImpl) definitions.get(1));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("Property /a/b/property2 already exists with value type 'string', as determined by [test-configuration/test-project/test-module [string]], but value type 'long' is requested in test-configuration/test-project/test-module [string].", e.getMessage());
        }
    }

    @Test
    public void override_property_with_different_type() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - property1: bla1\n"
                + "  - /a/b:\n"
                + "    - property1:\n"
                + "        operation: override\n"
                + "        type: long\n"
                + "        value: 42";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("42", valueToString(b.getProperties().get("property1")));
    }

    @Test
    public void reject_value_change_for_primary_type() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - jcr:primaryType: bla1\n"
                + "  - /a/b:\n"
                + "    - jcr:primaryType: bla2";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));

        try {
            builder.push((ContentDefinitionImpl) definitions.get(1));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("Property jcr:primaryType is already defined on node /a/b, but change is requested in test-configuration/test-project/test-module [string]. Use 'operation: override' if you really intend to change the value of this property.", e.getMessage());
        }
    }

    @Test
    public void ignore_identical_value_for_primary_type() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - jcr:primaryType: bla1\n"
                + "  - /a/b:\n"
                + "    - jcr:primaryType: bla1";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("bla1", valueToString(b.getProperties().get("jcr:primaryType")));
    }

    @Test
    public void override_primary_type() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - jcr:primaryType: bla1\n"
                + "  - /a/b:\n"
                + "    - jcr:primaryType:\n"
                + "        operation: override\n"
                + "        value: bla2";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("bla2", valueToString(b.getProperties().get("jcr:primaryType")));
    }

    @Test
    public void add_mixins() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - jcr:mixinTypes: [bla1, bla2]\n"
                + "  - /a/b:\n"
                + "    - jcr:mixinTypes:\n"
                + "        operation: add\n"
                + "        value: [bla3, bla4]";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[bla1, bla2, bla3, bla4]", valuesToString(b.getProperties().get("jcr:mixinTypes")));
    }

    @Test
    public void replace_mixins_with_superset() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - jcr:mixinTypes: [bla1, bla2]\n"
                + "  - /a/b:\n"
                + "    - jcr:mixinTypes: [bla2, bla3, bla1]";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[bla2, bla3, bla1]", valuesToString(b.getProperties().get("jcr:mixinTypes")));
    }

    @Test
    public void reject_mixins_if_no_superset() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - jcr:mixinTypes: [bla1, bla2]\n"
                + "  - /a/b:\n"
                + "    - jcr:mixinTypes: [bla3, bla1]";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));

        try {
            builder.push((ContentDefinitionImpl) definitions.get(1));
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            assertEquals("Property jcr:mixinTypes is already defined on node /a/b, and replace operation of test-configuration/test-project/test-module [string] would remove values [bla2]. Use 'operation: override' if you really intend to remove these values.", e.getMessage());
        }
    }

    @Test
    public void override_mixins_with_no_superset() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - jcr:mixinTypes: [bla1, bla2]\n"
                + "  - /a/b:\n"
                + "    - jcr:mixinTypes:\n"
                + "        operation: override\n"
                + "        value: [bla3, bla1]";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[bla3, bla1]", valuesToString(b.getProperties().get("jcr:mixinTypes")));
    }

    @Test
    public void delete_property() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - property1: bla1\n"
                + "  - /a/b:\n"
                + "    - property1:\n"
                + "        operation: delete";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl)definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        assertEquals("[]", sortedCollectionToString(b.getProperties()));
    }

    @Test
    public void delete_non_existent_property() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - property1:\n"
                + "        operation: delete";

        final List<Definition> definitions = parseNoSort(yaml);

        try {
            builder.push((ContentDefinitionImpl) definitions.get(0));
            fail("Should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertEquals("test-configuration/test-project/test-module [string]: Trying to delete property /a/property1 that does not exist.", e.getMessage());
        }
    }

    @Test
    public void modify_deleted_property() throws Exception {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /a:\n"
                + "    - /b:\n"
                + "      - /c:\n"
                + "        - property1: bla1\n"
                + "  - /a/b:\n"
                + "    - /c:\n"
                + "      - property1:\n"
                + "          operation: delete\n"
                + "  - /a/b/c:\n"
                + "    - property1: bla2";

        final List<Definition> definitions = parseNoSort(yaml);

        builder.push((ContentDefinitionImpl) definitions.get(0));
        builder.push((ContentDefinitionImpl) definitions.get(1));
        builder.push((ContentDefinitionImpl) definitions.get(2));
        final ConfigurationNodeImpl root = builder.build();

        final ConfigurationNode a = root.getNodes().get("a");
        final ConfigurationNode b = a.getNodes().get("b");
        final ConfigurationNode c = b.getNodes().get("c");
        assertEquals("[]", sortedCollectionToString(c.getProperties()));
    }
}
