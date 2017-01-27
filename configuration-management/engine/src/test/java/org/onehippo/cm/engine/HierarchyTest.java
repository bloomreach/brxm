/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cm.engine;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;
import org.onehippo.cm.api.model.ConfigDefinition;
import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.NamespaceDefinition;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.api.model.ValueType;

import static org.junit.Assert.assertEquals;

public class HierarchyTest extends AbstractBaseTest {

    @Test
    public void expect_hierarchy_test_loads() throws IOException, ParserException {
        final FileConfigurationReader.ReadResult result = readFromResource("/parser/hierarchy_test/repo-config.yaml");
        final Map<String, Configuration> configurations = result.getConfigurations();
        assertEquals(2, configurations.size());

        final Configuration base = assertConfiguration(configurations, "base", new String[0], 1);
        final Project project1 = assertProject(base, "project1", new String[0], 1);
        final Module module1 = assertModule(project1, "module1", new String[0], 2);
        final Source source1 = assertSource(module1, "config.yaml", 5);

        final NamespaceDefinition namespace = assertDefinition(source1, 0, NamespaceDefinition.class);
        assertEquals("myhippoproject", namespace.getPrefix());
        assertEquals("http://www.onehippo.org/myhippoproject/nt/1.0", namespace.getURI().toString());

        final NodeTypeDefinition nodeType = assertDefinition(source1, 1, NodeTypeDefinition.class);
        assertEquals(
                "<'hippo'='http://www.onehippo.org/jcr/hippo/nt/2.0.4'>\n" +
                        "<'myhippoproject'='http://www.onehippo.org/myhippoproject/nt/1.0'>\n" +
                        "[myhippoproject:basedocument] > hippo:document orderable\n",
                nodeType.getValue());
        assertEquals(false, nodeType.isResource());

        final NodeTypeDefinition nodeTypeFromResource = assertDefinition(source1, 2, NodeTypeDefinition.class);
        assertEquals("cnd.txt", nodeTypeFromResource.getValue());
        assertEquals(true, nodeTypeFromResource.isResource());

        final ConfigDefinition definition1 = assertDefinition(source1, 3, ConfigDefinition.class);
        final DefinitionNode rootDefinition1 = assertNode(definition1, "/", "", definition1, false, 3, 1);
        assertProperty(rootDefinition1, "/root-level-property", "root-level-property",
                definition1, ValueType.STRING, "root-level-property-value");
        final DefinitionNode nodeWithSingleProperty = assertNode(rootDefinition1, "/node-with-single-property",
                "node-with-single-property", false, definition1, false, 0, 1);
        assertProperty(nodeWithSingleProperty, "/node-with-single-property/property", "property",
                definition1, ValueType.STRING, "node-with-single-property-value");
        final DefinitionNode nodeWithMultipleProperties = assertNode(rootDefinition1, "/node-with-multiple-properties",
                "node-with-multiple-properties", false, definition1, false, 0, 3);
        assertProperty(nodeWithMultipleProperties, "/node-with-multiple-properties/single", "single",
                definition1, ValueType.STRING, "value1");
        assertProperty(nodeWithMultipleProperties, "/node-with-multiple-properties/multiple", "multiple",
                definition1, ValueType.STRING, new String[]{"value2","value3"});
        assertProperty(nodeWithMultipleProperties, "/node-with-multiple-properties/empty-multiple", "empty-multiple",
                definition1, ValueType.STRING, new String[0]);
        final DefinitionNode nodeWithSubNode =
                assertNode(rootDefinition1, "/node-with-sub-node", "node-with-sub-node", false, definition1, false, 1, 0);
        final DefinitionNode subNode =
                assertNode(nodeWithSubNode, "/node-with-sub-node/sub-node", "sub-node", false, definition1, false, 0, 1);
        assertProperty(subNode, "/node-with-sub-node/sub-node/property", "property", definition1, ValueType.STRING, "sub-node-value");

        final ContentDefinition contentDefinition = assertDefinition(source1, 4, ContentDefinition.class);
        assertNode(contentDefinition, "/content/documents/myhippoproject", "myhippoproject", contentDefinition, false, 0, 1);

        final Source source2 = assertSource(module1, "folder/resources.yaml", 1);
        final ConfigDefinition definition2 = assertDefinition(source2, 0, ConfigDefinition.class);
        final DefinitionNode resourceNode = assertNode(definition2, "/resources", "resources", definition2, false, 0, 4);
        assertProperty(resourceNode, "/resources/single-value-string-resource", "single-value-string-resource",
                definition2, false, ValueType.STRING, "string1.txt", true, false);
        assertProperty(resourceNode, "/resources/single-value-binary-resource", "single-value-binary-resource",
                definition2, false, ValueType.BINARY, "image1.png", true, false);
        assertProperty(resourceNode, "/resources/multi-value-resource-1", "multi-value-resource-1", definition2, false,
                ValueType.STRING, new String[]{"string2.txt"}, true, false);
        assertProperty(resourceNode, "/resources/multi-value-resource-2", "multi-value-resource-2", definition2, false,
                ValueType.STRING, new String[]{"folder/string.txt","folder/folder/string.txt"}, true, false);

        final Configuration myhippoproject = assertConfiguration(configurations, "myhippoproject", new String[]{"base"}, 1);
        final Project project2 = assertProject(myhippoproject, "project2", new String[]{"project1", "foo/bar"}, 1);
        final Module module2 = assertModule(project2, "module2", new String[0], 1);
        final Source baseSource = assertSource(module2, "config.yaml", 1);
        final ConfigDefinition baseDefinition = assertDefinition(baseSource, 0, ConfigDefinition.class);

        final DefinitionNode rootDefinition2 =
                assertNode(baseDefinition, "/node-with-sub-node/sub-node", "sub-node", baseDefinition, false, 0, 1);
        assertProperty(rootDefinition2, "/node-with-sub-node/sub-node/property", "property", baseDefinition, ValueType.STRING, "override");
    }

}
