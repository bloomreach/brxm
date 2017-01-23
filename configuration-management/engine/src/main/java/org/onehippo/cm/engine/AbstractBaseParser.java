/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

public abstract class AbstractBaseParser {

    protected Node composeYamlNode(final InputStream inputStream) throws ParserException {
        final Yaml yamlParser = new Yaml();
        try {
            return yamlParser.compose(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            throw new ParserException("Failed to parse YAML input", e);
        }
    }

    /**
     * Returns the given <code>node</code> as a mapping, if and only if the given node is a mapping that contains all
     * keys in <code>requiredKeys</code>, and does not contain any keys other than those in <code>requiredKeys</code>
     * or <code>optionalKeys</code>. When called with empty or null for both arrays, all string keys are allowed.
     * The use case of a specific set of required keys and "any" optional keys is not yet supported.
     * @param node
     * @param requiredKeys
     * @param optionalKeys
     * @return
     */
    protected Map<String, Node> asMapping(final Node node, String[] requiredKeys, String[] optionalKeys) throws ParserException {
        if (requiredKeys == null) requiredKeys = new String[0];
        if (optionalKeys == null) optionalKeys = new String[0];
        final boolean checkSchema = requiredKeys.length > 0 || optionalKeys.length > 0;

        if (node == null) {
            if (requiredKeys.length > 0) {
                throw new ParserException("Node is null but requires pair with key '" + requiredKeys[0] + "'");
            }
            return Collections.emptyMap();
        }
        if (node.getNodeId() != NodeId.mapping) {
            throw new ParserException("Node must be a mapping", node);
        }

        final MappingNode mappingNode = (MappingNode) node;
        final Map<String, Node> result = new LinkedHashMap<>(mappingNode.getValue().size());
        for (NodeTuple tuple : mappingNode.getValue()) {
            final String key = asStringScalar(tuple.getKeyNode());
            if (checkSchema && !find(key, requiredKeys, optionalKeys)) {
                throw new ParserException("Key '" + key + "' is not allowed", node);
            }
            result.put(key, tuple.getValueNode());
        }

        for (String requiredKey : requiredKeys) {
            if (!result.containsKey(requiredKey)) {
                throw new ParserException("Node must contain pair with key '" + requiredKey + "'", node);
            }
        }

        return result;
    }

    private boolean find(final String string, final String[] array1, final String[] array2) {
        return ArrayUtils.contains(array1, string) || ArrayUtils.contains(array2, string);
    }

    protected Pair<Node, Node> asMappingWithSingleKey(final Node node) throws ParserException {
        if (node.getNodeId() != NodeId.mapping) {
            throw new ParserException("Node must be a mapping", node);
        }
        final List<NodeTuple> tuples = ((MappingNode) node).getValue();
        if (tuples.size() != 1) {
            throw new ParserException("Map must contain single element", node);
        }
        final NodeTuple tuple = tuples.get(0);
        return Pair.of(tuple.getKeyNode(), tuple.getValueNode());
    }

    // See http://yaml.org/type/omap.html
    protected Map<Node, Node> asOrderedMap(final Node node) throws ParserException {
        final Set<String> keys = new HashSet<>();
        final Map<Node, Node> result = new LinkedHashMap<>();
        for (Node child : asSequence(node)) {
            final Pair<Node, Node> pair = asMappingWithSingleKey(child);
            final String key = asStringScalar(pair.getKey());
            if (!keys.add(key)) {
                throw new ParserException("Ordered map contains key '" + key + "' multiple times", node);
            }
            result.put(pair.getKey(), pair.getValue());
        }
        return result;
    }

    protected List<Node> asSequence(final Node node) throws ParserException {
        if (node == null) {
            return Collections.emptyList();
        }
        if (node.getNodeId() != NodeId.sequence) {
            throw new ParserException("Node must be sequence", node);
        }
        final SequenceNode sequenceNode = (SequenceNode) node;
        return sequenceNode.getValue();
    }

    protected String asNameScalar(final Node node) throws ParserException {
        final String name = asStringScalar(node);

        // todo: decide on validation

        return name;
    }

    protected String asPathScalar(final Node node) throws ParserException {
        final String path = asStringScalar(node);

        if (!path.startsWith("/")) {
            throw new ParserException("Path must start with a slash", node);
        }

        // 'path' represents a node-path, where slashes indicate the root node or node name borders.
        // we validate that slashes *inside* node names are \-escaped correctly:
        int slash = 0; // count consecutive slashes
        boolean escaped = false;
        for (int i = 0; i < path.length(); i++) {
            switch (path.charAt(i)) {
                case '/':
                    if (slash > 0) {
                        throw new ParserException("Path must not contain (unescaped) double slashes", node);
                    }
                    if (!escaped) {
                        slash++;
                    }
                    escaped = false;
                    break;
                case '\\':
                    slash = 0;
                    escaped = !escaped;
                    break;
                default:
                    slash = 0;
                    escaped = false;
                    break;
            }
        }
        if (slash > 0 && !isRootNodePath(path)) {
            throw new ParserException("Path must not end with (unescaped) slash", node);
        }

        return path;
    }

    private boolean isRootNodePath(final String nodePath) {
        return "/".equals(nodePath);
    }

    protected URI asURIScalar(final Node node) throws ParserException {
        final ScalarNode scalarNode = asScalar(node);
        if (!scalarNode.getTag().equals(Tag.STR)) {
            throw new ParserException("Scalar must be a string", node);
        }
        try {
            return new URI(scalarNode.getValue());
        } catch (final URISyntaxException e) {
            throw new ParserException("Scalar must be formatted as an URI", node);
        }
    }

    protected ScalarNode asScalar(final Node node) throws ParserException {
        if (node == null) {
            return null;
        }
        if (node.getNodeId() != NodeId.scalar) {
            throw new ParserException("Node must be scalar", node);
        }
        return (ScalarNode) node;
    }

    protected String asStringScalar(final Node node) throws ParserException {
        final ScalarNode scalarNode = asScalar(node);
        if (!scalarNode.getTag().equals(Tag.STR)) {
            throw new ParserException("Scalar must be a string", node);
        }
        return scalarNode.getValue();
    }

    protected List<String> asSingleOrSequenceOfStrScalars(final Node node) throws ParserException {
        if (node == null) {
            return Collections.emptyList();
        }
        final List<String> result = new ArrayList<>();
        switch (node.getNodeId()) {
            case scalar:
                result.add(asStringScalar(node));
                break;
            case sequence:
                final List<Node> values = asSequence(node);
                for (Node value : values) {
                    result.add(asStringScalar(value));
                }
                break;
            default:
                throw new ParserException("Node must be scalar or sequence, found '" + node.getNodeId() + "'", node);
        }
        return result;
    }

}
