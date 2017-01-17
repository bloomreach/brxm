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

package org.onehippo.cm.engine;

import java.io.StringReader;

import org.junit.Test;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.ProjectImpl;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.SequenceNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SourceValidationTest extends AbstractBaseTest {

    final Yaml yamlParser = new Yaml();
    final ConfigurationParser configurationParser = new ConfigurationParser();
    final ConfigurationImpl configuration = new ConfigurationImpl("configuration");
    final ProjectImpl project = new ProjectImpl("project", configuration);
    final ModuleImpl module = new ModuleImpl("module", project);

    @Test
    public void emptySource() {
        final Node node = yamlParser.compose(new StringReader("# empty document"));

        assertConfigurationException(node, null, "Node is null but requires pair with key 'instructions'");
    }

    @Test
    public void notAMapping() {
        assertConfigurationException("scalar value", "Node must be a mapping");
    }

    @Test
    public void missingKeyInMapping() {
        assertConfigurationException("{ }", "Node must contain pair with key 'instructions'");
    }

    @Test
    public void disallowedKeyInMapping() {
        assertConfigurationException("disallowed: value", "Key 'disallowed' is not allowed");
    }

    @Test
    public void instructionsNotASequence() {
        final Node root = yamlParser.compose(new StringReader("instructions: scalar value"));

        assertConfigurationException(root, instructions(root), "Node must be sequence");
    }

    @Test
    public void instructionNotAMap () {
        final Node root = yamlParser.compose(new StringReader("instructions: [ scalar value ]"));

        assertConfigurationException(root, firstInstruction(root), "Node must be a mapping");
    }

    @Test
    public void instructionEmptyMap() {
        final Node root = yamlParser.compose(new StringReader("instructions: [ { } ]"));

        assertConfigurationException(root, firstInstruction(root), "Map must contain single element");
    }

    @Test
    public void instructionMapTooManyKeys() {
        final Node root = yamlParser.compose(new StringReader("instructions: [ { key1: value1, key2: value2 } ]"));

        assertConfigurationException(root, firstInstruction(root), "Map must contain single element");
    }

    @Test
    public void unknownInstruction() {
        final Node root = yamlParser.compose(new StringReader("instructions: [ key1: value1 ]"));

        assertConfigurationException(root, firstInstructionFirstTuple(root).getKeyNode(),
                "Unknown instruction type 'key1'");
    }

    @Test
    public void namespaceWithScalarValue() {
        final Node root = yamlParser.compose(new StringReader("instructions: [ namespace: scalar value ]"));

        assertConfigurationException(root, firstInstructionFirstTuple(root).getValueNode(), "Node must be sequence");
    }

    @Test
    public void namespaceWithSequenceOfScalar() {
        final Node root = yamlParser.compose(new StringReader("instructions: [ namespace: [ scalar value ] ]"));

        assertConfigurationException(root, firstInstructionFirstTupleFirstValue(root), "Node must be a mapping");
    }

    @Test
    public void namespaceWithMissingPrefix() {
        final Node root = yamlParser.compose(new StringReader("instructions: [ namespace: [ uri: testURI ] ]"));

        assertConfigurationException(root, firstInstructionFirstTupleFirstValue(root),
                "Node must contain pair with key 'prefix'");
    }

    @Test
    public void namespaceWithMissingURI() {
        final Node root = yamlParser.compose(new StringReader("instructions: [ namespace: [ prefix: testPrefix ] ]"));

        assertConfigurationException(root, firstInstructionFirstTupleFirstValue(root),
                "Node must contain pair with key 'uri'");
    }

    @Test
    public void namespaceWithUnsupportedKeys() {
        final String yaml = "instructions:\n"
                + "- namespace:\n"
                + "  - prefix: testPrefix\n"
                + "    uri: testURI\n"
                + "    unsupported: value";

        final Node root = yamlParser.compose(new StringReader(yaml));

        assertConfigurationException(root, firstInstructionFirstTupleFirstValue(root),
                "Key 'unsupported' is not allowed");
    }

    @Test
    public void namespaceWithNonScalarPrefix() {
        final String yaml = "instructions:\n"
                + "- namespace:\n"
                + "  - prefix: [ ]\n"
                + "    uri: testURI";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node namespaceMapping = firstInstructionFirstTupleFirstValue(root);

        assertConfigurationException(root, firstTuple(namespaceMapping).getValueNode(), "Node must be scalar");
    }

    @Test
    public void namespaceWithNonStringPrefix() {
        final String yaml = "instructions:\n"
                + "- namespace:\n"
                + "  - prefix: 25\n"
                + "    uri: testURI";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node namespaceMapping = firstInstructionFirstTupleFirstValue(root);

        assertConfigurationException(root, firstTuple(namespaceMapping).getValueNode(), "Scalar must be a string");
    }

    @Test
    public void namespaceWithNonScalarURI() {
        final String yaml = "instructions:\n"
                + "- namespace:\n"
                + "  - prefix: testPrefix\n"
                + "    uri: { }";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node namespaceMapping = firstInstructionFirstTupleFirstValue(root);

        assertConfigurationException(root, secondTuple(namespaceMapping).getValueNode(), "Node must be scalar");
    }

    @Test
    public void namespaceWithNonStringURI() {
        final String yaml = "instructions:\n"
                + "- namespace:\n"
                + "  - prefix: testPrefix\n"
                + "    uri: 42";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node namespaceMapping = firstInstructionFirstTupleFirstValue(root);

        assertConfigurationException(root, secondTuple(namespaceMapping).getValueNode(), "Scalar must be a string");
    }

    @Test
    public void namespaceWithInvalidURI() {
        final String yaml = "instructions:\n"
                + "- namespace:\n"
                + "  - prefix: testPrefix\n"
                + "    uri: 44:bla?//]";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node namespaceMapping = firstInstructionFirstTupleFirstValue(root);

        assertConfigurationException(root, secondTuple(namespaceMapping).getValueNode(),
                "Scalar must be formatted as an URI");
    }

    @Test
    public void cndWithScalarValue() {
        final Node root = yamlParser.compose(new StringReader("instructions: [ cnd: scalar value ]"));

        assertConfigurationException(root, firstInstructionFirstTuple(root).getValueNode(), "Node must be sequence");
    }

    @Test
    public void cndWithNonStringValue() {
        final Node root = yamlParser.compose(new StringReader("instructions: [ cnd: [ 24 ] ]"));

        assertConfigurationException(root, firstInstructionFirstTupleFirstValue(root), "Scalar must be a string");
    }

    @Test
    public void configWithScalarValue() {
        final Node root = yamlParser.compose(new StringReader("instructions: [ config: scalar value ]"));

        assertConfigurationException(root, firstInstructionFirstTuple(root).getValueNode(), "Node must be sequence");
    }

    @Test
    public void configWithSequenceOfScalar() {
        final Node root = yamlParser.compose(new StringReader("instructions: [ config: [ scalar value ] ]"));

        assertConfigurationException(root, firstInstructionFirstTupleFirstValue(root), "Node must be a mapping");
    }

    @Test
    public void configWithTooManyKeys() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - first: value\n"
                + "    second: value";

        final Node root = yamlParser.compose(new StringReader(yaml));

        assertConfigurationException(root, firstInstructionFirstTupleFirstValue(root),
                "Map must contain single element");
    }

    @Test
    public void configWithDuplicateKeys() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - first: value\n"
                + "  - first: another value";

        final Node root = yamlParser.compose(new StringReader(yaml));

        assertConfigurationException(root, firstInstructionFirstTuple(root).getValueNode(),
                "Ordered map contains key 'first' multiple times");
    }

    @Test
    public void configWithNonScalarKey() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - [ first ]: value";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstTupleFirstValue(root);

        assertConfigurationException(root, firstTuple(config0).getKeyNode(), "Node must be scalar");
    }

    @Test
    public void configWithNonStringKey() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - 25: value";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstTupleFirstValue(root);

        assertConfigurationException(root, firstTuple(config0).getKeyNode(), "Scalar must be a string");
    }

    @Test
    public void configWithScalarDefinition() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /path/to/node: scalar property value";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstTupleFirstValue(root);

        assertConfigurationException(root, firstTuple(config0).getValueNode(), "Node must be sequence");
    }

    @Test
    public void configWithDefinitionWithMultipleKeys() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /path/to/node:\n"
                + "    - property1: value1\n"
                + "      property2: value2";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstTupleFirstValue(root);
        final Node propertyMap = firstValue(firstTuple(config0).getValueNode());

        assertConfigurationException(root, propertyMap, "Map must contain single element");
    }

    @Test
    public void configWithDefinitionWithDuplicateKeys() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /path/to/node:\n"
                + "    - property1: value1\n"
                + "    - property1: value2";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstTupleFirstValue(root);

        assertConfigurationException(root, firstTuple(config0).getValueNode(),
                "Ordered map contains key 'property1' multiple times");
    }

    @Test
    public void configWithDefinitionWithNonScalarKey() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /path/to/node:\n"
                + "    - [ property ]: value";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstTupleFirstValue(root);
        final Node propertyMap = firstValue(firstTuple(config0).getValueNode());

        assertConfigurationException(root, firstTuple(propertyMap).getKeyNode(), "Node must be scalar");
    }

    @Test
    public void configWithDefinitionWithNonStringKey() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /path/to/node:\n"
                + "    - 42: value";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstTupleFirstValue(root);
        final Node propertyMap = firstValue(firstTuple(config0).getValueNode());

        assertConfigurationException(root, firstTuple(propertyMap).getKeyNode(), "Scalar must be a string");
    }

    @Test
    public void configWithDefinitionWithDoubleSlashPathKey() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /path/to/node:\n"
                + "    - //: value";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstTupleFirstValue(root);
        final Node propertyMap = firstValue(firstTuple(config0).getValueNode());

        assertConfigurationException(root, firstTuple(propertyMap).getKeyNode(),
                "Path must not contain (unescaped) double slashes");
    }

    @Test
    public void configWithDefinitionWithPathKeyIncludingDoubleSlashes() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /path/to/node:\n"
                + "    - /path/to//node: value";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstTupleFirstValue(root);
        final Node propertyMap = firstValue(firstTuple(config0).getValueNode());

        assertConfigurationException(root, firstTuple(propertyMap).getKeyNode(),
                "Path must not contain (unescaped) double slashes");
    }

    @Test
    public void configWithDefinitionWithPathKeyIncludingDoubleSlashesAndEscapes() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /path/to/node:\n"
                + "    - /path/to\\\\//node: value";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstTupleFirstValue(root);
        final Node propertyMap = firstValue(firstTuple(config0).getValueNode());

        assertConfigurationException(root, firstTuple(propertyMap).getKeyNode(),
                "Path must not contain (unescaped) double slashes");
    }

    @Test
    public void configWithDefinitionWithTrailingSlashPathKey() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /path/to/node:\n"
                + "    - /path/to/node/: value";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstTupleFirstValue(root);
        final Node propertyMap = firstValue(firstTuple(config0).getValueNode());

        assertConfigurationException(root, firstTuple(propertyMap).getKeyNode(),
                "Path must not end with (unescaped) slash");
    }

    @Test
    public void configWithDefinitionWithUnsupportedScalarType() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /path/to/node:\n"
                + "    - property: ~"; // indicates null

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstTupleFirstValue(root);
        final Node propertyMap = firstValue(firstTuple(config0).getValueNode());

        assertConfigurationException(root, firstTuple(propertyMap).getValueNode(),
                "Tag not recognized: tag:yaml.org,2002:null");
    }

    @Test
    public void configWithDefinitionWithMixedSequenceTypes() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /path/to/node:\n"
                + "    - property: [ true, test ]"; // boolean + string

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstTupleFirstValue(root);
        final Node propertyMap = firstValue(firstTuple(config0).getValueNode());

        assertConfigurationException(root, firstTuple(propertyMap).getValueNode(),
                "Property values must all be of the same type, found value type 'boolean' as well as 'string'");
    }

    @Test
    public void configWithDefinitionWithMissingKey() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /path/to/node:\n"
                + "    - property: { }";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstTupleFirstValue(root);
        final Node propertyMap = firstValue(firstTuple(config0).getValueNode());

        assertConfigurationException(root, firstTuple(propertyMap).getValueNode(),
                "Property values represented as map must have a 'value' or 'resource' key");
    }

    @Test
    public void configWithDefinitionWithUnsupportedKey() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /path/to/node:\n"
                + "    - property:\n"
                + "        unsupported: value";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstTupleFirstValue(root);
        final Node propertyMap = firstValue(firstTuple(config0).getValueNode());

        assertConfigurationException(root, firstTuple(propertyMap).getValueNode(),
                "Key 'unsupported' is not allowed");
    }

    @Test
    public void configWithDefinitionWithUnsupportedType() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /path/to/node:\n"
                + "    - property: { type: unsupported }";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstTupleFirstValue(root);
        final Node propertyMap = firstValue(firstTuple(config0).getValueNode());
        final Node propertyValueMap = firstTuple(propertyMap).getValueNode();

        assertConfigurationException(root, firstTuple(propertyValueMap).getValueNode(),
                "Unrecognized value type: 'unsupported'");
    }

    @Test
    public void configWithDefinitionWithMissingValue() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /path/to/node:\n"
                + "    - property: { type: string }";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstTupleFirstValue(root);
        final Node propertyMap = firstValue(firstTuple(config0).getValueNode());

        assertConfigurationException(root, firstTuple(propertyMap).getValueNode(),
                "Property values represented as map must have a 'value' or 'resource' key");
    }

    @Test
    public void configWithDefinitionWithMapValue() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /path/to/node:\n"
                + "    - property:\n"
                + "        type: string\n"
                + "        value: { }";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstTupleFirstValue(root);
        final Node propertyMap = firstValue(firstTuple(config0).getValueNode());
        final Node propertyValueMap = firstTuple(propertyMap).getValueNode();

        assertConfigurationException(root, secondTuple(propertyValueMap).getValueNode(),
                "Property value in map must be scalar or sequence");
    }

    @Test
    public void configWithDefinitionWithUnsupportedValueType() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /path/to/node:\n"
                + "    - property:\n"
                + "        type: string\n"
                + "        value: ~"; // indicates null

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstTupleFirstValue(root);
        final Node propertyMap = firstValue(firstTuple(config0).getValueNode());
        final Node propertyValueMap = firstTuple(propertyMap).getValueNode();

        assertConfigurationException(root, secondTuple(propertyValueMap).getValueNode(),
                "Tag not recognized: tag:yaml.org,2002:null");
    }

    @Test
    public void configWithDefinitionWithInvalidResourceType() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /path/to/node:\n"
                + "    - property:\n"
                + "        type: string\n"
                + "        resource: 25";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstTupleFirstValue(root);
        final Node propertyMap = firstValue(firstTuple(config0).getValueNode());
        final Node propertyValueMap = firstTuple(propertyMap).getValueNode();

        assertConfigurationException(root, secondTuple(propertyValueMap).getValueNode(),
                "Scalar must be a string");
    }

    @Test
    public void configWithDefinitionWithInvalidResourceValue() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /path/to/node:\n"
                + "    - property:\n"
                + "        type: string\n"
                + "        resource: { }";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstTupleFirstValue(root);
        final Node propertyMap = firstValue(firstTuple(config0).getValueNode());
        final Node propertyValueMap = firstTuple(propertyMap).getValueNode();

        assertConfigurationException(root, secondTuple(propertyValueMap).getValueNode(),
                "Node must be scalar or sequence, found 'mapping'");
    }

    @Test
    public void configWithDefinitionWithInvalidResourcePropertyTypeCombination() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /path/to/node:\n"
                + "    - property:\n"
                + "        type: boolean\n"
                + "        resource: boolean.bin";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstTupleFirstValue(root);
        final Node propertyMap = firstValue(firstTuple(config0).getValueNode());
        final Node propertyValueMap = firstTuple(propertyMap).getValueNode();

        assertConfigurationException(root, firstTuple(propertyValueMap).getValueNode(),
                "Resource can only be used for value type 'binary' or 'string'");
    }

    @Test
    public void configWithDefinitionWithEmptyResourceSequence() {
        final String yaml = "instructions:\n"
                + "- config:\n"
                + "  - /path/to/node:\n"
                + "    - property:\n"
                + "        type: binary\n"
                + "        resource: []";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstTupleFirstValue(root);
        final Node propertyMap = firstValue(firstTuple(config0).getValueNode());
        final Node propertyValueMap = firstTuple(propertyMap).getValueNode();

        assertConfigurationException(root, secondTuple(propertyValueMap).getValueNode(),
                "Resource value must define at least one value");
    }

    private void assertConfigurationException(final String input, final String exceptionMessage) {
        final Node node = yamlParser.compose(new StringReader(input));

        assertConfigurationException(node, node, exceptionMessage);
    }

    private void assertConfigurationException(final Node inputNode, final Node exceptionNode,
                                              final String exceptionMessage) {
        try {
            configurationParser.constructSource("sourcePath", inputNode, module);
            fail("An exception should have occurred");
        } catch (ConfigurationParser.ConfigurationException e) {
            assertEquals(exceptionMessage, e.getMessage());
            assertEquals(exceptionNode, e.getNode());
        }
    }

    private Node firstInstructionFirstTupleFirstValue(final Node root) {
        return firstValue(firstInstructionFirstTuple(root).getValueNode());
    }

    private NodeTuple firstInstructionFirstTuple(final Node root) {
        return firstTuple(firstInstruction(root));
    }

    private Node firstInstruction(final Node root) {
        return firstValue(instructions(root));
    }

    private Node instructions(final Node root) {
        return ((MappingNode)root).getValue().get(0).getValueNode();
    }

    private NodeTuple firstTuple(final Node mapping) {
        return ((MappingNode)mapping).getValue().get(0);
    }

    private NodeTuple secondTuple(final Node mapping) {
        return ((MappingNode)mapping).getValue().get(1);
    }

    private Node firstValue(final Node sequence) {
        return ((SequenceNode)sequence).getValue().get(0);
    }
}
