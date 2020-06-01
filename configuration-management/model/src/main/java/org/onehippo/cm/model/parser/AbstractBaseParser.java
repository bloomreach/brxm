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
package org.onehippo.cm.model.parser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.path.JcrPathSegment;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.source.ResourceInputProvider;
import org.onehippo.cm.model.source.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

/**
 * Base class for various custom SnakeYAML-driven parsers for HCM config, content, and metadata files.
 */
public abstract class AbstractBaseParser {

    static final Logger log = LoggerFactory.getLogger(AbstractBaseParser.class);

    private final boolean explicitSequencing;

    public AbstractBaseParser(final boolean explicitSequencing) {
        this.explicitSequencing = explicitSequencing;
    }

    /**
     * @return Does this parser use explicit '-' style YAML sequences or implicit whitespace-based sequences?
     */
    protected boolean isExplicitSequencing() {
        return explicitSequencing;
    }

    protected Node composeYamlNode(final InputStream inputStream, final String location) throws ParserException {
        log.debug("Parsing YAML source '{}'", location);
        final Yaml yamlParser = new Yaml();
        try {
            return yamlParser.compose(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            final String message = String.format("Failed to parse YAML source '%s'", location);
            throw new ParserException(message, e);
        }
    }

    protected List<NodeTuple> asTuples(final Node node) throws ParserException {
        if (explicitSequencing) {
            if (node.getNodeId() != NodeId.sequence) {
                throw new ParserException("Node must be a sequence", node);
            }
            final List<Node> subNodes = ((SequenceNode) node).getValue();
            final List<NodeTuple> result = new ArrayList<>(subNodes.size());
            final List<String> keys = new ArrayList<>(subNodes.size());
            for (Node subNode : subNodes) {
                if (subNode.getNodeId() != NodeId.mapping) {
                    throw new ParserException("Node must be a mapping", subNode);
                }
                final MappingNode mappingNode = (MappingNode) subNode;
                if (mappingNode.getValue().size() != 1) {
                    throw new ParserException("Map must contain single element", subNode);
                }
                final NodeTuple tuple = ((MappingNode) subNode).getValue().get(0);
                final String tupleKey = asStringScalar(tuple.getKeyNode());
                if (keys.contains(tupleKey)) {
                    throw new ParserException("Ordered map contains key '" + tupleKey + "' multiple times", node);
                }
                result.add(tuple);
                keys.add(tupleKey);
            }
            return result;
        } else {
            if (node.getNodeId() != NodeId.mapping) {
                throw new ParserException("Node must be a mapping", node);
            }
            return ((MappingNode) node).getValue();
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

    protected boolean find(final String string, final String[] array1, final String[] array2) {
        return ArrayUtils.contains(array1, string) || ArrayUtils.contains(array2, string);
    }

    protected List<Node> asSequence(final Node node) throws ParserException {
        if (node == null) {
            return Collections.emptyList();
        }
        if (node.getNodeId() != NodeId.sequence) {
            throw new ParserException("Node must be a sequence", node);
        }
        final SequenceNode sequenceNode = (SequenceNode) node;
        return sequenceNode.getValue();
    }

    protected JcrPath asPathScalar(final Node node, final boolean requireAbsolutePath, final boolean allowSnsIndices) throws ParserException {
        return asPathScalar(asStringScalar(node), node, requireAbsolutePath, allowSnsIndices);
    }
    protected JcrPath asPathScalar(final String path, final Node node, final boolean requireAbsolutePath, final boolean allowSnsIndices) throws ParserException {

        if (requireAbsolutePath && !path.startsWith("/")) {
            throw new ParserException("Path must start with a slash", node);
        }

        if (path.contains("//")) {
            throw new ParserException("Path must not contain double slashes", node);
        }

        if (path.endsWith("/") && !isRootNodePath(path)) {
            throw new ParserException("Path must not end with a slash", node);
        }

        if (path.equals("/") || path.equals("")) {
            return JcrPaths.ROOT;
        }

        final String[] pathSegments;
        if (path.startsWith("/")) {
            pathSegments = path.substring(1).split("/");
        } else {
            pathSegments = path.split("/");
        }

        // somewhat excessive specificity in error checking...
        for (String segment: pathSegments) {
            try {
                final JcrPathSegment parsedName = JcrPaths.getSegment(segment);
                if (!allowSnsIndices && parsedName.getIndex() != 0) {
                    throw new ParserException("Path must not contain name indices", node);
                }
            } catch (IllegalArgumentException e) {
                throw new ParserException("Illegal path segment: " + segment, node);
            }
        }

        return JcrPaths.getPath(path);
    }

    protected boolean isRootNodePath(final String nodePath) {
        return "/".equals(nodePath);
    }

    protected String asResourcePathScalar(final Node node, final Source source, final ResourceInputProvider resourceInputProvider) throws ParserException {
        final String resourcePath = asStringScalar(node);

        if (containsParentSegment(resourcePath)) {
            throw new ParserException("Resource path is not valid: '" + resourcePath
                    + "'; a resource path must not contain ..", node);
        }

        if (!resourceInputProvider.hasResource(source, resourcePath)) {
            throw new ParserException("Cannot find resource '" + resourcePath + "'", node);
        }

        return resourcePath;
    }

    protected boolean containsParentSegment(final String resourceString) {
        for (final String pathElement : resourceString.split("/")) {
            if (pathElement.toString().equals("..")) {
                return true;
            }
        }
        return false;
    }

    protected URI asURIScalar(final Node node) throws ParserException {
        try {
            return new URI(asStringScalar(node));
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

    protected void collectSingleOrSequenceOfStrScalars(final Node node, final Collection<String> collection) throws ParserException {
        if (node == null) {
            return;
        }
        switch (node.getNodeId()) {
            case scalar:
                collection.add(asStringScalar(node));
                break;
            case sequence:
                final List<Node> values = asSequence(node);
                for (Node value : values) {
                    collection.add(asStringScalar(value));
                }
                break;
            default:
                throw new ParserException("Node must be scalar or sequence, found '" + node.getNodeId() + "'", node);
        }
    }

    protected Set<String> asSingleOrSetOfStrScalars(final Node node) throws ParserException {
        if (node == null) {
            return Collections.emptySet();
        }
        final Set<String> result = new LinkedHashSet<>();
        collectSingleOrSequenceOfStrScalars(node, result);
        return result;
    }
}
