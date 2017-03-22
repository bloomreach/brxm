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
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.onehippo.cm.engine.Constants.DEFAULT_EXPLICIT_SEQUENCING;

public class SourceValidationTest extends AbstractBaseTest {

    final Yaml yamlParser = new Yaml();
    final ConfigurationImpl configuration = new ConfigurationImpl("configuration");
    final ProjectImpl project = new ProjectImpl("project", configuration);
    final ModuleImpl module = new ModuleImpl("module", project);

    @Test
    public void emptySource() {
        final Node node = yamlParser.compose(new StringReader("# empty document"));

        assertParserException(node, null, "Node is null but requires pair with key 'definitions'");
    }

    @Test
    public void notAMapping() {
        assertParserException("scalar value", "Node must be a mapping");
    }

    @Test
    public void missingKeyInMapping() {
        assertParserException("{ }", "Node must contain pair with key 'definitions'");
    }

    @Test
    public void disallowedKeyInMapping() {
        assertParserException("disallowed: value", "Key 'disallowed' is not allowed");
    }

    @Test
    public void instructionsNotAMap() {
        final Node root = yamlParser.compose(new StringReader("definitions: scalar value"));

        assertParserException(root, instructions(root), "Node must be a mapping");
    }

    @Test
    public void unknownInstruction() {
        final Node root = yamlParser.compose(new StringReader("definitions: { key1: value1 }"));

        assertParserException(root, instructions(root), "Key 'key1' is not allowed");
    }

    @Test
    public void namespaceWithScalarValue() {
        final Node root = yamlParser.compose(new StringReader("definitions: { namespace: scalar value }"));

        assertParserException(root, firstInstructionTuple(root).getValueNode(), "Node must be a sequence");
    }

    @Test
    public void namespaceWithSequenceOfScalar() {
        final Node root = yamlParser.compose(new StringReader("definitions: { namespace: [ scalar value ] }"));

        assertParserException(root, firstInstructionFirstValue(root), "Node must be a mapping");
    }

    @Test
    public void namespaceWithMissingPrefix() {
        final Node root = yamlParser.compose(new StringReader("definitions: { namespace: [ uri: testURI ] }"));

        assertParserException(root, firstInstructionFirstValue(root),
                "Node must contain pair with key 'prefix'");
    }

    @Test
    public void namespaceWithMissingURI() {
        final Node root = yamlParser.compose(new StringReader("definitions: { namespace: [ prefix: testPrefix ] }"));

        assertParserException(root, firstInstructionFirstValue(root),
                "Node must contain pair with key 'uri'");
    }

    @Test
    public void namespaceWithUnsupportedKeys() {
        final String yaml = "definitions:\n"
                + "  namespace:\n"
                + "  - prefix: testPrefix\n"
                + "    uri: testURI\n"
                + "    unsupported: value";

        final Node root = yamlParser.compose(new StringReader(yaml));

        assertParserException(root, firstInstructionFirstValue(root),
                "Key 'unsupported' is not allowed");
    }

    @Test
    public void namespaceWithNonScalarPrefix() {
        final String yaml = "definitions:\n"
                + "  namespace:\n"
                + "  - prefix: [ ]\n"
                + "    uri: testURI";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node namespaceMapping = firstInstructionFirstValue(root);

        assertParserException(root, firstTuple(namespaceMapping).getValueNode(), "Node must be scalar");
    }

    @Test
    public void namespaceWithNonStringPrefix() {
        final String yaml = "definitions:\n"
                + "  namespace:\n"
                + "  - prefix: 25\n"
                + "    uri: testURI";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node namespaceMapping = firstInstructionFirstValue(root);

        assertParserException(root, firstTuple(namespaceMapping).getValueNode(), "Scalar must be a string");
    }

    @Test
    public void namespaceWithNonScalarURI() {
        final String yaml = "definitions:\n"
                + "  namespace:\n"
                + "  - prefix: testPrefix\n"
                + "    uri: { }";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node namespaceMapping = firstInstructionFirstValue(root);

        assertParserException(root, secondTuple(namespaceMapping).getValueNode(), "Node must be scalar");
    }

    @Test
    public void namespaceWithNonStringURI() {
        final String yaml = "definitions:\n"
                + "  namespace:\n"
                + "  - prefix: testPrefix\n"
                + "    uri: 42";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node namespaceMapping = firstInstructionFirstValue(root);

        assertParserException(root, secondTuple(namespaceMapping).getValueNode(), "Scalar must be a string");
    }

    @Test
    public void namespaceWithInvalidURI() {
        final String yaml = "definitions:\n"
                + "  namespace:\n"
                + "  - prefix: testPrefix\n"
                + "    uri: 44:bla?//]";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node namespaceMapping = firstInstructionFirstValue(root);

        assertParserException(root, secondTuple(namespaceMapping).getValueNode(),
                "Scalar must be formatted as an URI");
    }

    @Test
    public void cndWithScalarValue() {
        final Node root = yamlParser.compose(new StringReader("definitions: { cnd: scalar value }"));

        assertParserException(root, firstInstructionTuple(root).getValueNode(), "Node must be a sequence");
    }

    @Test
    public void cndWithNonStringValue() {
        final Node root = yamlParser.compose(new StringReader("definitions: { cnd: [ 24 ] }"));

        assertParserException(root, firstInstructionFirstValue(root), "Scalar must be a string");
    }

    @Test
    public void cndWithNonStringResourceValue() {
        final Node root = yamlParser.compose(new StringReader("definitions: { cnd: [ resource: 24 ] }"));

        final Node cnd = firstInstructionFirstValue(root);

        assertParserException(root, firstTuple(cnd).getValueNode(), "Scalar must be a string");
    }

    @Test
    public void cndWithNotExistingResourceValue() {
        final Node root = yamlParser.compose(new StringReader("definitions: { cnd: [ resource: foo.txt ] }"));

        final Node cnd = firstInstructionFirstValue(root);

        assertParserException(root, firstTuple(cnd).getValueNode(), "Cannot find resource 'foo.txt'");
    }

    @Test
    public void cndWithNeitherScalarNorMap() {
        final String yaml = "definitions:\n"
                + "  cnd:\n"
                + "  - [ sequence ]\n";

        final Node root = yamlParser.compose(new StringReader(yaml));

        assertParserException(root, firstInstructionFirstValue(root),
                "CND definition item must be a string or a map with key 'resource'");
    }

    @Test
    public void configWithScalarValue() {
        final Node root = yamlParser.compose(new StringReader("definitions: { config: scalar value }"));

        assertParserException(root, firstInstructionTuple(root).getValueNode(), "Node must be a mapping");
    }

    @Test
    public void configWithMapOfScalar() {
        final Node root = yamlParser.compose(new StringReader("definitions: { config: { scalar value } }"));

        assertParserException(root, firstConfigTuple(root).getKeyNode(), "Path must start with a slash");
    }

    @Test
    public void configWithNonScalarKey() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    [ first ]: value";

        final Node root = yamlParser.compose(new StringReader(yaml));

        assertParserException(root, firstConfigTuple(root).getKeyNode(), "Node must be scalar");
    }

    @Test
    public void configWithNonStringKey() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    25: value";

        final Node root = yamlParser.compose(new StringReader(yaml));

        assertParserException(root, firstConfigTuple(root).getKeyNode(), "Scalar must be a string");
    }

    @Test
    public void configWithRelativePathKey() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    path/to/node: value";

        final Node root = yamlParser.compose(new StringReader(yaml));

        assertParserException(root, firstConfigTuple(root).getKeyNode(), "Path must start with a slash");
    }

    @Test
    public void configWithDoubleSlashPathKey() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    //:\n"
                + "      property: value";

        final Node root = yamlParser.compose(new StringReader(yaml));

        assertParserException(root, firstConfigTuple(root).getKeyNode(), "Path must not contain double slashes");
    }

    @Test
    public void configWithPathKeyIncludingDoubleSlashes() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to//node:\n"
                + "      property: value";

        final Node root = yamlParser.compose(new StringReader(yaml));

        assertParserException(root, firstConfigTuple(root).getKeyNode(), "Path must not contain double slashes");
    }

    @Test
    public void configWithPathKeyWithTrailingSlash() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node/:\n"
                + "      property: value";

        final Node root = yamlParser.compose(new StringReader(yaml));

        assertParserException(root, firstConfigTuple(root).getKeyNode(), "Path must not end with a slash");
    }

    @Test
    public void configWithScalarInstructionLen() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node: scalar property value";

        final Node root = yamlParser.compose(new StringReader(yaml));

        assertParserException(root, firstConfigTuple(root).getValueNode(), "Node must be a mapping");
    }

    @Test
    public void configWithInstructionWithNonScalarKey() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      [ property ]: value";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();

        assertParserException(root, firstTuple(nodeMap).getKeyNode(), "Node must be scalar");
    }

    @Test
    public void configWithInstructionWithNonStringKey() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      42: value";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();

        assertParserException(root, firstTuple(nodeMap).getKeyNode(), "Scalar must be a string");
    }

    @Test
    public void configWithInstructionWithUnsupportedScalarType() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property: ~"; // indicates null

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyValue = firstTuple(nodeMap).getValueNode();

        assertParserException(root, propertyValue, "Tag not recognized: tag:yaml.org,2002:null");
    }

    @Test
    public void configWithInstructionWithBigInteger() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property: 31415926535897932384626433832795028841971";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyValue = firstTuple(nodeMap).getValueNode();

        assertParserException(root, propertyValue, "Value is too big to fit into a long, use a property of type decimal");
    }

    @Test
    public void configWithInstructionWithMixedSequenceTypes() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property: [ true, test ]"; // boolean + string

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyValue = firstTuple(nodeMap).getValueNode();

        assertParserException(root, propertyValue,
                "Property values must all be of the same type, found value type 'boolean' as well as 'string'");
    }

    @Test
    public void configWithInstructionWithMissingKey() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property: { }";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();

        assertParserException(root, propertyMap, "Property map must have either a 'value', 'resource' or 'path' key");
    }

    @Test
    public void configWithInstructionWithUnsupportedKey() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property:\n"
                + "        unsupported: value";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();

        assertParserException(root, propertyMap, "Key 'unsupported' is not allowed");
    }

    @Test
    public void configWithInstructionWithUnsupportedType() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property: { type: unsupported }";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();
        final Node propertyTypeValue = firstTuple(propertyMap).getValueNode();

        assertParserException(root, propertyTypeValue, "Unrecognized value type: 'unsupported'");
    }

    @Test
    public void configWithInstructionWithMissingValue() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property: { type: string }";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();

        assertParserException(root, propertyMap, "Property map must have either a 'value', 'resource' or 'path' key");
    }

    @Test
    public void configWithInstructionWithMultipleValueKeys() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property:\n"
                + "        type: string\n"
                + "        resource: []\n"
                + "        value: []";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();

        assertParserException(root, propertyMap, "Property map must have either a 'value', 'resource' or 'path' key");
    }

    @Test
    public void configWithInstructionWithMapValue() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property:\n"
                + "        type: string\n"
                + "        value: { }";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();
        final Node propertyMapValue = secondTuple(propertyMap).getValueNode();

        assertParserException(root, propertyMapValue, "Property value in map must be scalar or sequence, found 'mapping'");
    }

    @Test
    public void configWithInstructionWithUnsupportedValueType() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property:\n"
                + "        type: string\n"
                + "        value: ~"; // indicates null

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();
        final Node propertyMapValue = secondTuple(propertyMap).getValueNode();

        assertParserException(root, propertyMapValue, "Tag not recognized: tag:yaml.org,2002:null");
    }

    @Test
    public void configWithInstructionWithTypeValueMismatch() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property:\n"
                + "        type: string\n"
                + "        value: 42";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();
        final Node propertyMapValue = secondTuple(propertyMap).getValueNode();

        assertParserException(root, propertyMapValue,
                "Property value is not of the correct type, expected 'string', found 'long'");
    }

    @Test
    public void configWithInstructionWithNonNumericDecimalValue() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property:\n"
                + "        type: decimal\n"
                + "        value: '42a'";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();
        final Node propertyMapValue = secondTuple(propertyMap).getValueNode();

        assertParserException(root, propertyMapValue, "Could not parse scalar value as BigDecimal: 42a");
    }

    @Test
    public void configWithInstructionWithNonStringDecimalValue() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property:\n"
                + "        type: decimal\n"
                + "        value: 42";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();
        final Node propertyMapValue = secondTuple(propertyMap).getValueNode();

        assertParserException(root, propertyMapValue, "Scalar must be a string");
    }

    @Test
    public void configWithInstructionWithIncorrectUriValue() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property:\n"
                + "        type: uri\n"
                + "        value: ':'";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();
        final Node propertyMapValue = secondTuple(propertyMap).getValueNode();

        assertParserException(root, propertyMapValue, "Scalar must be formatted as an URI");
    }

    @Test
    public void configWithInstructionWithIncorrectReferenceValue() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property:\n"
                + "        type: reference\n"
                + "        value: abc";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();
        final Node propertyMapValue = secondTuple(propertyMap).getValueNode();

        assertParserException(root, propertyMapValue, "Could not parse scalar value as Reference (UUID): abc");
    }

    @Test
    public void configWithInstructionWithPathWithIncorrectType() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property:\n"
                + "        type: string\n"
                + "        path: /path/to/node";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();
        final Node propertyPathValue = secondTuple(propertyMap).getValueNode();

        assertParserException(root, propertyPathValue,
                "Path values can only be used for value type 'reference' or 'weakreference'");
    }

    @Test
    public void configWithInstructionWithPathMap() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property:\n"
                + "        type: reference\n"
                + "        path: { }";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();
        final Node propertyPathValue = secondTuple(propertyMap).getValueNode();

        assertParserException(root, propertyPathValue, "Path value must be scalar or sequence, found 'mapping'");
    }

    @Test
    public void configWithInstructionWithEmptyPathSequence() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property:\n"
                + "        type: reference\n"
                + "        path: [ ]";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();
        final Node propertyPathValue = secondTuple(propertyMap).getValueNode();

        assertParserException(root, propertyPathValue, "Path value must define at least one value");
    }

    @Test
    public void configWithInstructionWithIncorrectWeakReferenceValue() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property:\n"
                + "        type: weakreference\n"
                + "        value: abc";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();
        final Node propertyMapValue = secondTuple(propertyMap).getValueNode();

        assertParserException(root, propertyMapValue, "Could not parse scalar value as WeakReference (UUID): abc");
    }

    @Test
    public void configWithInstructionWithInvalidResourceType() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property:\n"
                + "        type: string\n"
                + "        resource: 25";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();
        final Node propertyResourceValue = secondTuple(propertyMap).getValueNode();

        assertParserException(root, propertyResourceValue, "Scalar must be a string");
    }

    @Test
    public void configWithInstructionWithInvalidResourceValue() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property:\n"
                + "        type: string\n"
                + "        resource: { }";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();
        final Node propertyResourceValue = secondTuple(propertyMap).getValueNode();

        assertParserException(root, propertyResourceValue,
                "Resource value must be scalar or sequence, found 'mapping'");
    }

    @Test
    public void configWithInstructionWithInvalidResourcePropertyTypeCombination() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property:\n"
                + "        type: boolean\n"
                + "        resource: boolean.bin";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();
        final Node propertyResourceValue = secondTuple(propertyMap).getValueNode();

        assertParserException(root, propertyResourceValue,
                "Resource values can only be used for value type 'binary' or 'string'");
    }

    @Test
    public void configWithInstructionWithEmptyResourceSequence() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property:\n"
                + "        type: binary\n"
                + "        resource: []";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();
        final Node propertyResourceValue = secondTuple(propertyMap).getValueNode();

        assertParserException(root, propertyResourceValue, "Resource value must define at least one value");
    }

    @Test
    public void configWithInstructionWithParentPathElementResource() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property:\n"
                + "        type: binary\n"
                + "        resource: ../etc/passwd";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();
        final Node propertyResourceValue = secondTuple(propertyMap).getValueNode();

        assertParserException(root, propertyResourceValue,
                "Resource path is not valid: '../etc/passwd'; a resource path must not contain ..");
    }

    @Test
    public void configWithInstructionWithNotExistingResource() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property:\n"
                + "        type: binary\n"
                + "        resource: foo.txt";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();
        final Node propertyResourceValue = secondTuple(propertyMap).getValueNode();

        assertParserException(root, propertyResourceValue, "Cannot find resource 'foo.txt'");
    }

    @Test
    public void configWithInstructionWithNodeDeleteNonScalar() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      .meta:delete: [true]";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node metaDeleteValue = firstTuple(nodeMap).getValueNode();

        assertParserException(root, metaDeleteValue, "Node must be scalar");
    }

    @Test
    public void configWithInstructionWithNodeDeleteNonBoolean() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      .meta:delete: 42";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node metaDeleteValue = firstTuple(nodeMap).getValueNode();

        assertParserException(root, metaDeleteValue, "Value for .meta:delete must be boolean value 'true'");
    }

    @Test
    public void configWithInstructionWithNodeDeleteFalse() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      .meta:delete: false";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node metaDeleteValue = firstTuple(nodeMap).getValueNode();

        assertParserException(root, metaDeleteValue, "Value for .meta:delete must be boolean value 'true'");
    }

    @Test
    public void configWithInstructionWithNodeDeleteAndMore() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      .meta:delete: true\n"
                + "      property: value";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();

        assertParserException(root, nodeMap, "Node cannot contain '.meta:delete' and other keys");
    }

    @Test
    public void configWithInstructionWithOrderBeforeNonScalar() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      .meta:order-before: [node]";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node metaOrderBeforeValue = firstTuple(nodeMap).getValueNode();

        assertParserException(root, metaOrderBeforeValue, "Node must be scalar");
    }

    @Test
    public void configWithInstructionWithOrderBeforeNonString() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      .meta:order-before: 42";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node metaOrderBeforeValue = firstTuple(nodeMap).getValueNode();

        assertParserException(root, metaOrderBeforeValue, "Scalar must be a string");
    }

    @Test
    public void configWithInstructionWithNullOrderBefore() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      .meta:order-before: !!null";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node metaOrderBeforeValue = firstTuple(nodeMap).getValueNode();

        assertParserException(root, metaOrderBeforeValue, "Scalar must be a string");
    }

    @Test
    public void configWithInstructionWithOrderBeforeSelf() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      .meta:order-before: node";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();

        assertParserException(root, nodeMap, "Invalid .meta:order-before targeting this node itself");
    }

    @Test
    public void configWithInstructionWithOrderBeforeFirst() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      .meta:order-before: ''";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final ScalarNode orderBefore = (ScalarNode)firstTuple(nodeMap).getValueNode();
        assertEquals(orderBefore.getValue(), "");
    }

    @Test
    public void configWithInstructionWithNonScalarPropertyOperation() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property:\n"
                + "        operation: [unknown]";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();

        assertParserException(root, firstTuple(propertyMap).getValueNode(), "Node must be scalar");
    }

    @Test
    public void configWithInstructionWithUnknownPropertyOperation() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property:\n"
                + "        operation: unknown";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();

        assertParserException(root, firstTuple(propertyMap).getValueNode(), "Unrecognized property operation: 'unknown'");
    }

    @Test
    public void configWithInstructionWithPropertyDeleteAndMore() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property:\n"
                + "        operation: delete\n"
                + "        value: str";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();

        assertParserException(root, propertyMap, "Property map cannot contain 'operation: delete' and other keys");
    }

    @Test
    public void configWithInstructionWithPropertyValueAddAndSingleScalar() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      property:\n"
                + "        operation: add\n"
                + "        value: str";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();

        assertParserException(root, propertyMap, "Property map with operation 'add' must have a sequence for 'value', 'resource' or 'path'");
    }

    @Test
    public void configWithInstructionWithPrimaryTypePropertyWithNonNameType() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      jcr:primaryType:\n"
                + "        type: string\n"
                + "        value: str";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();

        assertParserException(root, propertyMap, "Property 'jcr:primaryType' must be of type 'name'");
    }

    @Test
    public void configWithInstructionWithPrimaryTypePropertyWithNonScalar() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      jcr:primaryType:\n"
                + "        value: [str]";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();

        assertParserException(root, propertyMap, "Property 'jcr:primaryType' must be property type 'single'");
    }

    @Test
    public void configWithInstructionWithPrimaryTypePropertyWithDelete() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      jcr:primaryType:\n"
                + "        operation: delete";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();

        assertParserException(root, propertyMap,
                "Property 'jcr:primaryType' supports only the following operations: replace, override");
    }

    @Test
    public void configWithInstructionWithPrimaryTypePropertyWithAdd() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      jcr:primaryType:\n"
                + "        operation: add\n"
                + "        value: ['some:type']";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();

        assertParserException(root, propertyMap,
                "Property 'jcr:primaryType' supports only the following operations: replace, override");
    }

    @Test
    public void configWithInstructionWithMixinTypesPropertyWithNonNameType() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      jcr:mixinTypes:\n"
                + "        type: string\n"
                + "        value: ['some:type']";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();

        assertParserException(root, propertyMap, "Property 'jcr:mixinTypes' must be of type 'name'");
    }

    @Test
    public void configWithInstructionWithMixinTypesPropertyWithNonSequence() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      jcr:mixinTypes:\n"
                + "        type: name\n"
                + "        value: 'some:type'";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();

        assertParserException(root, propertyMap, "Property 'jcr:mixinTypes' must be property type 'list'");
    }

    @Test
    public void configWithInstructionWithMixinTypesPropertyWithDelete() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    /path/to/node:\n"
                + "      jcr:mixinTypes:\n"
                + "        operation: delete";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node nodeMap = firstConfigTuple(root).getValueNode();
        final Node propertyMap = firstTuple(nodeMap).getValueNode();

        assertParserException(root, propertyMap,
                "Property 'jcr:mixinTypes' supports only the following operations: add, replace, override");
    }

    // start set for "explicit sequencing"

    @Test
    public void configWithDuplicateKeysInRoot() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "  - first: value\n"
                + "  - first: another value";

        final Node root = yamlParser.compose(new StringReader(yaml));

        assertParserException(root, firstInstructionTuple(root).getValueNode(),
                "Ordered map contains key 'first' multiple times", true);
    }

    @Test
    public void configWithDuplicateKeysInInstruction() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "  - /path/to/node:\n"
                + "    - property1: value1\n"
                + "    - property1: value2";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstValue(root);

        assertParserException(root, firstTuple(config0).getValueNode(),
                "Ordered map contains key 'property1' multiple times", true);
    }

    @Test
    public void configWithTooManyKeysInRoot() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "  - first: value\n"
                + "    second: value";

        final Node root = yamlParser.compose(new StringReader(yaml));

        assertParserException(root, firstInstructionFirstValue(root), "Map must contain single element", true);
    }

    @Test
    public void configWithTooManyKeysInInstruction() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "  - /path/to/node:\n"
                + "    - first: value\n"
                + "      second: value";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstValue(root);
        final Node propertyMap = firstValue(firstTuple(config0).getValueNode());

        assertParserException(root, propertyMap, "Map must contain single element", true);
    }

    @Test
    public void configWithScalarInstruction() {
        final String yaml = "definitions:\n"
                + "  config:\n"
                + "    - /path/to/node: scalar property value";

        final Node root = yamlParser.compose(new StringReader(yaml));
        final Node config0 = firstInstructionFirstValue(root);

        assertParserException(root, firstTuple(config0).getValueNode(), "Node must be a sequence", true);
    }

    private void assertParserException(final String input, final String exceptionMessage) {
        final Node node = yamlParser.compose(new StringReader(input));

        assertParserException(node, node, exceptionMessage);
    }

    private void assertParserException(final Node inputNode,
                                       final Node exceptionNode,
                                       final String exceptionMessage) {
        assertParserException(inputNode, exceptionNode, exceptionMessage, DEFAULT_EXPLICIT_SEQUENCING);
    }

    private void assertParserException(final Node inputNode,
                                       final Node exceptionNode,
                                       final String exceptionMessage,
                                       final boolean explicitSequencing) {
        try {
            final SourceParser sourceParser = new SourceParser(DUMMY_RESOURCE_INPUT_PROVIDER, false, explicitSequencing);
            sourceParser.constructSource("sourcePath", inputNode, module);
            fail("An exception should have occurred");
        } catch (ParserException e) {
            assertEquals(exceptionMessage, e.getMessage());
            assertEquals(exceptionNode, e.getNode());
        }
    }

    private Node instructions(final Node root) {
        return ((MappingNode)root).getValue().get(0).getValueNode();
    }

    private NodeTuple firstInstructionTuple(final Node root) {
        return firstTuple(instructions(root));
    }

    private NodeTuple firstConfigTuple(final Node root) {
        return firstTuple(firstInstructionTuple(root).getValueNode());
    }

    private Node firstInstructionFirstValue(final Node root) {
        return firstValue(firstInstructionTuple(root).getValueNode());
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
