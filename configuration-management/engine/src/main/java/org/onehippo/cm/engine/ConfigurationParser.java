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
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.api.model.ValueType;
import org.onehippo.cm.impl.model.ConfigDefinitionImpl;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.ContentDefinitionImpl;
import org.onehippo.cm.impl.model.DefinitionNodeImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.ProjectImpl;
import org.onehippo.cm.impl.model.SourceImpl;
import org.onehippo.cm.impl.model.ValueImpl;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

public class ConfigurationParser {

    static class ConfigurationException extends RuntimeException {
        private final Node node;
        ConfigurationException(final String message) {
            this(message, null);
        }
        ConfigurationException(final String message, final Node node) {
            super(message);
            this.node = node;
        }
        Node getNode() {
            return node;
        }
        @Override
        public String toString() {
            if (node == null) {
                return getClass().getName() + ": " + getMessage();
            } else {
                return getClass().getName() + ": " + getMessage() + node.getStartMark().toString();
            }
        }
    }

    // After the compose step, SnakeYaml does not yet provide parsed scalar values. An extension of the Constructor
    // class is needed to access the protected Constructor#construct method which uses the built-in parsers for the
    // known basic scalar types. The additional check for the ConstructYamlTimestamp is done as the constructor for
    // timestamp returns a Date by internally constructing a Calendar.
    private static final class ScalarConstructor extends Constructor {
        Object constructScalarNode(final ScalarNode node) {
            final Construct constructor = getConstructor(node);
            final Object object = constructor.construct(node);
            if (constructor instanceof ConstructYamlTimestamp) {
                return ((ConstructYamlTimestamp)constructor).getCalendar().clone();
            }
            return object;
        }
    }

    private static final ScalarConstructor scalarConstructor = new ScalarConstructor();

    public Map<String, Configuration> parse(final URL repoConfigUrl, final List<URL> sourceUrls) throws IOException {
        final Yaml yamlParser = new Yaml();
        final Node repoConfigNode = yamlParser.compose(new InputStreamReader(repoConfigUrl.openStream(), StandardCharsets.UTF_8));

        final Map<String, Configuration> configurations = parseRepoConfig(repoConfigNode);
        final List<Module> modules = collectModules(configurations);

        for (URL url : sourceUrls) {
            final ModuleImpl module;
            if (modules.size() == 1) {
                module = (ModuleImpl) modules.get(0);
            } else {
                module = getModuleForSource(configurations, url);
            }
            final Node sourceNode = yamlParser.compose(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
            constructSource(calculateRelativePath(repoConfigUrl, url), sourceNode, module);
        }

        return configurations;
    }

    private Map<String, Configuration> parseRepoConfig(final Node src) {
        final Map<String, Configuration> result = new LinkedHashMap<>();
        final Map<String, Node> sourceMap = asMapping(src, new String[]{"configurations"}, null);

        for (Node configurationNode : asSequence(sourceMap.get("configurations"))) {
            constructConfiguration(configurationNode, result);
        }

        return result;
    }

    private void constructConfiguration(final Node src, final Map<String, Configuration> parent) {
        final Map<String, Node> configurationMap = asMapping(src, new String[]{"name", "projects"}, new String[]{"after"});
        final String name = asStringScalar(configurationMap.get("name"));
        final ConfigurationImpl configuration = new ConfigurationImpl(name);
        configuration.setAfter(asSingleOrSequenceOfStrScalars(configurationMap.get("after")));
        parent.put(name, configuration);

        for (Node projectNode : asSequence(configurationMap.get("projects"))) {
            constructProject(projectNode, configuration);
        }
    }

    private void constructProject(final Node src, final ConfigurationImpl parent) {
        final Map<String, Node> sourceMap = asMapping(src, new String[]{"name", "modules"}, new String[]{"after"});
        final String name = asStringScalar(sourceMap.get("name"));
        final ProjectImpl project = parent.addProject(name);
        project.setAfter(asSingleOrSequenceOfStrScalars(sourceMap.get("after")));

        for (Node moduleNode : asSequence(sourceMap.get("modules"))) {
            constructModule(moduleNode, project);
        }
    }

    private void constructModule(final Node src, final ProjectImpl parent) {
        final Map<String, Node> map = asMapping(src, new String[]{"name"}, new String[]{"after"});
        final String name = asStringScalar(map.get("name"));
        final ModuleImpl module = parent.addModule(name);
        module.setAfter(asSingleOrSequenceOfStrScalars(map.get("after")));
    }

    protected void constructSource(final String path, final Node src, final ModuleImpl parent) {
        final Map<String, Node> sourceMap = asMapping(src, new String[]{"instructions"}, null);
        final SourceImpl source = parent.addSource(path);

        final Map<Node, Node> definitions = asOrderedMap(sourceMap.get("instructions"));
        for (Node definitionKeyNode : definitions.keySet()) {
            final String definitionName = asStringScalar(definitionKeyNode);
            final Node definitionNode = definitions.get(definitionKeyNode);
            switch (definitionName) {
                case "namespace":
                    constructNamespaceDefinitions(definitionNode, source);
                    break;
                case "cnd":
                    constructNodeTypeDefinitions(definitionNode, source);
                    break;
                case "config":
                    constructConfigDefinitions(definitionNode, source);
                    break;
                case "content":
                    constructContentDefinitions(definitionNode, source);
                    break;
                default:
                    throw new ConfigurationException("Unknown instruction type '" + definitionName + "'", definitionKeyNode);
            }
        }
    }

    private void constructNamespaceDefinitions(final Node src, final SourceImpl parent) {
        for (Node node : asSequence(src)) {
            final Map<String, Node> namespaceMap = asMapping(node, new String[]{"prefix", "uri"}, null);
            final String prefix = asStringScalar(namespaceMap.get("prefix"));
            final URI uri = asURIScalar(namespaceMap.get("uri"));
            parent.addNamespaceDefinition(prefix, uri);
        }
    }

    private void constructNodeTypeDefinitions(final Node src, final SourceImpl parent) {
        for (Node node : asSequence(src)) {
            final String cndString = asStringScalar(node);
            parent.addNodeTypeDefinition(cndString);
        }
    }

    private void constructConfigDefinitions(final Node src, final SourceImpl parent) {
        final Map<Node, Node> definitions = asOrderedMap(src);
        for (Node keyNode : definitions.keySet()) {
            final ConfigDefinitionImpl definition = parent.addConfigDefinition();
            final String key = asPathScalar(keyNode);
            constructDefinitionNode(key, definitions.get(keyNode), definition);
        }
    }

    private void constructContentDefinitions(final Node src, final SourceImpl parent) {
        final Map<Node, Node> definitions = asOrderedMap(src);
        for (Node keyNode : definitions.keySet()) {
            final ContentDefinitionImpl definition = parent.addContentDefinition();
            final String key = asPathScalar(keyNode);
            constructDefinitionNode(key, definitions.get(keyNode), definition);
        }
    }

    private void constructDefinitionNode(final String path, final Node value, final ContentDefinitionImpl definition) {
        final String name = StringUtils.substringAfterLast(path, "/");
        final DefinitionNodeImpl definitionNode = new DefinitionNodeImpl(path, name, definition);
        definition.setNode(definitionNode);
        populateDefinitionNode(definitionNode, value);
    }

    private void populateDefinitionNode(final DefinitionNodeImpl definitionNode, final Node value) {
        final Map<Node, Node> children = asOrderedMap(value);
        for (Node keyNode : children.keySet()) {
            final String key = asPathScalar(keyNode);
            if (key.startsWith("/")) {
                final String name = key.substring(1);
                constructDefinitionNode(name, children.get(keyNode), definitionNode);
            } else {
                constructDefinitionProperty(key, children.get(keyNode), definitionNode);
            }
        }
    }

    private void constructDefinitionNode(final String name, final Node value, final DefinitionNodeImpl parent) {
        final DefinitionNodeImpl node = parent.addNode(name);
        populateDefinitionNode(node, value);
    }

    private void constructDefinitionProperty(final String name, final Node value, final DefinitionNodeImpl parent) {
        switch (value.getNodeId()) {
            case scalar:
                constructDefinitionPropertyFromScalar(name, value, parent);
                break;
            case sequence:
                constructDefinitionPropertyFromSequence(name, value, parent);
                break;
            case mapping:
                constructDefinitionPropertyFromMap(name, value, parent);
                break;
            default:
                throw new ConfigurationException("Property value must be scalar, sequence or map", value);
        }
    }

    private void constructDefinitionPropertyFromScalar(final String name, final Node value, final DefinitionNodeImpl parent) {
        parent.addProperty(name, constructValueFromScalar(value));
    }

    private void constructDefinitionPropertyFromSequence(final String name, final Node value, final DefinitionNodeImpl parent) {
        final Value[] values = constructValuesFromSequence(value);

        // TODO: "use a map" should be more instructive, maybe including an example.
        // TODO:   Once the parsing has stabilized, if this error is still here, improve the wording.
        if (values.length == 0) {
            throw new ConfigurationException("Property values represented as sequence must have at least 1 value; to represent 0 values, use a map", value);
        }

        parent.addProperty(name, values[0].getType(), values);
    }

    private void constructDefinitionPropertyFromMap(final String name, final Node value, final DefinitionNodeImpl parent) {
        final Map<String, Node> map = asMapping(value, new String[]{"type"}, new String[]{"value","resource"});
        final ValueType valueType = constructValueType(map.get("type"));

        if (map.keySet().contains("value")) {
            final Node valueNode = map.get("value");
            if (valueNode.getNodeId() == NodeId.scalar) {
                final Value propertyValue = constructValueFromScalar(valueNode);
                parent.addProperty(name, propertyValue);
            } else if (valueNode.getNodeId() == NodeId.sequence) {
                final Value[] propertyValues = constructValuesFromSequence(valueNode);
                parent.addProperty(name, valueType, propertyValues);
            } else {
                throw new ConfigurationException("Property value in map must be scalar or sequence", valueNode);
            }
        } else if (map.keySet().contains("resource")) {
            // TODO: due to the logic of constructValueType, the following check is never true. Keep it?
            if (!(valueType == ValueType.STRING || valueType == ValueType.BINARY)) {
                throw new ConfigurationException("Resource can only be used for value type 'binary' or 'string'", value);
            }
            final Node resourceNode = map.get("resource");
            final List<String> resources = asSingleOrSequenceOfStrScalars(resourceNode);
            final Value[] resourceValues = new Value[resources.size()];
            for (int i = 0; i < resources.size(); i++) {
                // add check resource exists
                resourceValues[i] = new ValueImpl(valueType, resources.get(i));
            }
            switch (resourceNode.getNodeId()) {
                case scalar:
                    parent.addProperty(name, resourceValues[0]);
                    break;
                case sequence:
                    parent.addProperty(name, valueType, resourceValues);
                    break;
                // TODO: due to the logic in asSingleOrSequenceOfStrScalars, the default case is never reached. Keep it?
                default:
                    throw new ConfigurationException("Resource value must be scalar or sequence", resourceNode);
            }
        } else {
            throw new ConfigurationException("Property values represented as map must have a 'value' or 'resource' key", value);
        }
    }

    private Value constructValueFromScalar(final Node node) {
        final ScalarNode scalar = asScalar(node);
        final Object object = scalarConstructor.constructScalarNode(scalar);

        if (Tag.BINARY.equals(scalar.getTag())) {
            return new ValueImpl((byte[]) object);
        }
        if (Tag.BOOL.equals(scalar.getTag())) {
            return new ValueImpl((Boolean) object);
        }
        if (Tag.FLOAT.equals(scalar.getTag())) {
            return new ValueImpl((Double) object);
        }
        if (Tag.INT.equals(scalar.getTag())) {
            return new ValueImpl(((Integer) object).longValue());
        }
        if (Tag.STR.equals(scalar.getTag())) {
            return new ValueImpl((String) object);
        }
        if (Tag.TIMESTAMP.equals(scalar.getTag())) {
            return new ValueImpl((Calendar) object);
        }

        throw new ConfigurationException("Tag not recognized: " + scalar.getTag(), node);
    }

    private Value[] constructValuesFromSequence(final Node value) {
        final List<Node> valueNodes = asSequence(value);

        ValueType valueType = null;
        final Value[] values = new Value[valueNodes.size()];
        for (int i = 0; i < valueNodes.size(); i++) {
            values[i] = constructValueFromScalar(valueNodes.get(i));
            if (valueType == null) {
                valueType = values[i].getType();
            } else if (valueType != values[i].getType()) {
                throw new ConfigurationException(
                        MessageFormat.format(
                                "Property values must all be of the same type, found value type ''{0}'' as well as ''{1}''",
                                valueType,
                                values[i].getType()),
                        value);
            }
        }

        return values;
    }

    private ValueType constructValueType(final Node node) {
        final String type = asStringScalar(node);
        switch (type) {
            case "binary": return ValueType.BINARY;
            case "string": return ValueType.STRING;
        }
        throw new ConfigurationException("Unrecognized value type: '" + type + "'", node);
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
    private Map<String, Node> asMapping(final Node node, String[] requiredKeys, String[] optionalKeys) {
        if (requiredKeys == null) requiredKeys = new String[0];
        if (optionalKeys == null) optionalKeys = new String[0];
        final boolean checkSchema = requiredKeys.length > 0 || optionalKeys.length > 0;

        if (node == null) {
            if (requiredKeys.length > 0) {
                throw new ConfigurationException("Node is null but requires pair with key '" + requiredKeys[0] + "'");
            }
            return Collections.emptyMap();
        }
        if (node.getNodeId() != NodeId.mapping) {
            throw new ConfigurationException("Node must be a mapping", node);
        }

        final MappingNode mappingNode = (MappingNode) node;
        final Map<String, Node> result = new LinkedHashMap<>(mappingNode.getValue().size());
        for (NodeTuple tuple : mappingNode.getValue()) {
            final String key = asStringScalar(tuple.getKeyNode());
            if (checkSchema && !find(key, requiredKeys, optionalKeys)) {
                throw new ConfigurationException("Key '" + key + "' is not allowed", node);
            }
            result.put(key, tuple.getValueNode());
        }

        for (String requiredKey : requiredKeys) {
            if (!result.containsKey(requiredKey)) {
                throw new ConfigurationException("Node must contain pair with key '" + requiredKey + "'", node);
            }
        }

        return result;
    }

    private boolean find(final String string, final String[] array1, final String[] array2) {
        return ArrayUtils.contains(array1, string) || ArrayUtils.contains(array2, string);
    }

    private Pair<Node, Node> asMappingWithSingleKey(final Node node) {
        if (node.getNodeId() != NodeId.mapping) {
            throw new ConfigurationException("Node must be a mapping", node);
        }
        final List<NodeTuple> tuples = ((MappingNode) node).getValue();
        if (tuples.size() != 1) {
            throw new ConfigurationException("Map must contain single element", node);
        }
        final NodeTuple tuple = tuples.get(0);
        return Pair.of(tuple.getKeyNode(), tuple.getValueNode());
    }

    // See http://yaml.org/type/omap.html
    private Map<Node, Node> asOrderedMap(final Node node) {
        final Set<String> keys = new HashSet<>();
        final Map<Node, Node> result = new LinkedHashMap<>();
        for (Node child : asSequence(node)) {
            final Pair<Node, Node> pair = asMappingWithSingleKey(child);
            final String key = asStringScalar(pair.getKey());
            if (!keys.add(key)) {
                throw new ConfigurationException("Ordered map contains key '" + key + "' multiple times", node);
            }
            result.put(pair.getKey(), pair.getValue());
        }
        return result;
    }

    private List<Node> asSequence(final Node node) {
        if (node == null) {
            return Collections.emptyList();
        }
        if (node.getNodeId() != NodeId.sequence) {
            throw new ConfigurationException("Node must be sequence", node);
        }
        final SequenceNode sequenceNode = (SequenceNode) node;
        return sequenceNode.getValue();
    }

    private ScalarNode asScalar(final Node node) {
        if (node == null) {
            return null;
        }
        if (node.getNodeId() != NodeId.scalar) {
            throw new ConfigurationException("Node must be scalar", node);
        }
        return (ScalarNode) node;
    }

    private String asStringScalar(final Node node) {
        final ScalarNode scalarNode = asScalar(node);
        if (!scalarNode.getTag().equals(Tag.STR)) {
            throw new ConfigurationException("Scalar must be a string", node);
        }
        return scalarNode.getValue();
    }

    private String asPathScalar(final Node node) {
        final String stringValue = asStringScalar(node);

        boolean escape = false;
        boolean slash = false;
        for (int i = 0; i < stringValue.length(); i++) {
            char c = stringValue.charAt(i);
            if (c == '\\') {
                escape = !escape;
                slash = false;
            } else if (c == '/') {
                if (slash) {
                    throw new ConfigurationException("Path must not contain (unescaped) double slashes", node);
                }
                if (!escape) {
                    slash = true;
                }
                escape = false;
            } else {
                slash = false;
                escape = false;
            }
        }
        // TODO: or do we want to ignore trailing single slashes (be lenient)
        if (slash && stringValue.length() > 1) {
            throw new ConfigurationException("Path must not end with (unescaped) slash", node);
        }

        return stringValue;
    }

    private List<String> asSingleOrSequenceOfStrScalars(final Node node) {
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
                throw new ConfigurationException("Node must be scalar or sequence, found '" + node.getNodeId() + "'", node);
        }
        return result;
    }

    private URI asURIScalar(final Node node) {
        final ScalarNode scalarNode = asScalar(node);
        if (!scalarNode.getTag().equals(Tag.STR)) {
            throw new ConfigurationException("Scalar must be a string", node);
        }
        try {
            return new URI(scalarNode.getValue());
        } catch (final URISyntaxException e) {
            throw new ConfigurationException("Scalar must be formatted as an URI", node);
        }
    }

    private List<Module> collectModules(Map<String, Configuration> configurations) {
        final List<Module> modules = new ArrayList<>();
        for (Configuration configuration : configurations.values()) {
            for (Project project : configuration.getProjects().values()) {
                for (Module module : project.getModules().values()) {
                    modules.add(module);
                }
            }
        }
        return modules;
    }

    private ModuleImpl getModuleForSource(final Map<String, Configuration> configurations, final URL url) {
        final String[] parts = url.getPath().split("/");
        if (parts.length < 4) {
            throw new IllegalArgumentException(
                    MessageFormat.format(
                            "URL ''{0}'' must consist of at least 4 elements, found {1} element(s)",
                            url.getPath(), parts.length));
        }
        final String configurationName = parts[parts.length - 4];
        final String projectName = parts[parts.length - 3];
        final String moduleName = parts[parts.length - 2];
        final Configuration configuration = configurations.get(configurationName);
        if (configuration == null) {
            throw new IllegalArgumentException(MessageFormat.format("Configuration ''{0}'' not found", configurationName));
        }
        final Project project = configuration.getProjects().get(projectName);
        if (project == null) {
            throw new IllegalArgumentException(MessageFormat.format("Project ''{0}'' not found in configuration ''{1}''", projectName, configurationName));
        }
        final Module module = project.getModules().get(moduleName);
        if (module == null) {
            throw new IllegalArgumentException(MessageFormat.format("Module ''{0}'' not found in project ''{1}''", moduleName, projectName));
        }
        return (ModuleImpl) module;
    }

    private String calculateRelativePath(final URL repoConfigURL, final URL sourceURL) throws IOException {
        final int position = repoConfigURL.getPath().lastIndexOf("repo-config.yaml");
        if (position == -1) {
            throw new IOException("URL does not end with 'repo-config.yaml': " + repoConfigURL.getPath());
        }
        final String prefix = repoConfigURL.getPath().substring(0, position) + "repo-config/";
        if (!sourceURL.getPath().startsWith(prefix)) {
            throw new IOException("URLs do not start with expected prefix: " + sourceURL.getPath() + "; expected prefix " + prefix);
        }
        return sourceURL.getPath().substring(prefix.length());
    }

}
