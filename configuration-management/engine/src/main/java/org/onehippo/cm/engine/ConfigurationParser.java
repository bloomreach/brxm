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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.Value;
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

class ConfigurationParser {

    class ConfigurationException extends RuntimeException {
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
    }

    static class MyConstructor extends Constructor {
        Object constructScalarNode(final ScalarNode node) {
            Construct constructor = getConstructor(node);
            return constructor.construct(node);
        }
    }

    private MyConstructor constructor = new MyConstructor();

    Map<String, Configuration> parse(final URL repoConfigUrl, final List<URL> sourceUrls) throws IOException {
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
        configuration.setAfter(parseAfter(configurationMap.get("after")));
        parent.put(name, configuration);

        for (Node projectNode : asSequence(configurationMap.get("projects"))) {
            constructProject(projectNode, configuration);
        }
    }

    private void constructProject(final Node src, final ConfigurationImpl parent) {
        final Map<String, Node> sourceMap = asMapping(src, new String[]{"name", "modules"}, new String[]{"after"});
        final String name = asStringScalar(sourceMap.get("name"));
        final ProjectImpl project = parent.addProject(name);
        project.setAfter(parseAfter(sourceMap.get("after")));

        for (Node moduleNode : asSequence(sourceMap.get("modules"))) {
            constructModule(moduleNode, project);
        }
    }

    private void constructModule(final Node src, final ProjectImpl parent) {
        final Map<String, Node> map = asMapping(src, new String[]{"name"}, new String[]{"after"});
        final String name = asStringScalar(map.get("name"));
        final ModuleImpl module = parent.addModule(name);
        module.setAfter(parseAfter(map.get("after")));
    }

    private List<String> parseAfter(final Node node) {
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
                throw new ConfigurationException("'after' value must be scalar or sequence, found '" + node.getNodeId() + "'", node);
        }
        return result;
    }

    private void constructSource(final String path, final Node src, final ModuleImpl parent) {
        final Map<String, Node> sourceMap = asMapping(src, new String[]{"instructions"}, null);
        final SourceImpl source = parent.addSource(path);

        for (Node instructionNode : asSequence(sourceMap.get("instructions"))) {
            constructInstruction(instructionNode, source);
        }
    }

    private void constructInstruction(final Node src, final SourceImpl parent) {
        final Map<String, Node> sourceMap = asMapping(src);

        if (sourceMap.size() != 1) {
            throw new ConfigurationException("Instruction map must contain single element defining the instruction type", src);
        }

        final String instructionName = sourceMap.keySet().iterator().next();
        final Node instructionNode = sourceMap.get(instructionName);
        switch (instructionName) {
            case "config":
                constructConfigDefinitions(instructionNode, parent);
                break;
            case "content":
                constructContentDefinitions(instructionNode, parent);
                break;
            default:
                throw new ConfigurationException("Unknown instruction type '" + instructionName + "'", src);
        }
    }

    private void constructConfigDefinitions(final Node src, final SourceImpl parent) {
        for (Node node : asSequence(src)) {
            final Map<String, Node> map = asSingleItemMap(node);
            final String path = map.keySet().iterator().next();
            final Node value = map.get(path);
            final ConfigDefinitionImpl definition = parent.addConfigDefinition();
            constructDefinitionNode(path, value, definition);
        }
    }

    private void constructContentDefinitions(final Node src, final SourceImpl parent) {
        for (Node node : asSequence(src)) {
            final Map<String, Node> map = asSingleItemMap(node);
            final String path = map.keySet().iterator().next();
            final Node value = map.get(path);
            final ContentDefinitionImpl definition = parent.addContentDefinition();
            constructDefinitionNode(path, value, definition);
        }
    }

    private void constructDefinitionNode(final String name, final Node value, final ContentDefinitionImpl definition) {
        final DefinitionNodeImpl node = new DefinitionNodeImpl(name, name, definition);
        definition.setNode(node);
        populateDefinitionNode(node, value);
    }

    private void populateDefinitionNode(final DefinitionNodeImpl node, final Node value) {
        final Map<String, Node> children = asSequenceOfSingleItemMaps(value);
        for (String key : children.keySet()) {
            if (key.startsWith("/")) {
                final String name = key.substring(1);
                constructDefinitionNode(name, children.get(key), node);
            } else {
                constructDefinitionProperty(key, children.get(key), node);
            }
        }
    }

    private void constructDefinitionNode(final String name, final Node value, final DefinitionNodeImpl parent) {
        final DefinitionNodeImpl node = parent.addNode(name);
        populateDefinitionNode(node, value);
    }

    private void constructDefinitionProperty(final String name, final Node value, final DefinitionNodeImpl parent) {
        if (value.getNodeId() == NodeId.scalar) {
            parent.addProperty(name, constructValue(value));
        } else if (value.getNodeId() == NodeId.sequence) {
            final List<Node> valueNodes = asSequence(value);
            final List<Value> values = new ArrayList<>(valueNodes.size());
            for (Node valueNode : valueNodes) {
                values.add(constructValue(valueNode));
            }
            parent.addProperty(name, values.toArray(new Value[values.size()]));
        } else {
            throw new ConfigurationException("Property value must be scalar or sequence", value);
        }
    }

    private Value constructValue(final Node node) {
        final ScalarNode scalar = asScalar(node);
        final Object object = constructor.constructScalarNode(scalar);

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
            return new ValueImpl((Integer) object);
        }
        if (Tag.STR.equals(scalar.getTag())) {
            return new ValueImpl((String) object);
        }
        if (Tag.TIMESTAMP.equals(scalar.getTag())) {
            return new ValueImpl((Date) object);
        }

        throw new ConfigurationException("Tag not recognized: " + scalar.getTag(), node);
    }

    private Map<String, Node> asMapping(final Node node) {
        return asMapping(node, null, null);
    }

    private Map<String, Node> asMapping(final Node node, String[] requiredNames, String[] optionalNames) {
        if (requiredNames == null) requiredNames = new String[0];
        if (optionalNames == null) optionalNames = new String[0];
        final boolean checkSchema = requiredNames.length > 0 || optionalNames.length > 0;

        if (node == null) {
            if (checkSchema && requiredNames.length > 0) {
                throw new ConfigurationException("Node is null but requires element '" + requiredNames[0] + "'");
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
            if (checkSchema && !find(key, requiredNames, optionalNames)) {
                throw new ConfigurationException("Element '" + key + "' is not allowed", node);
            }
            result.put(key, tuple.getValueNode());
        }
        if (checkSchema) {
            for (String requiredName : requiredNames) {
                if (!result.containsKey(requiredName)) {
                    throw new ConfigurationException("Node must contain element '" + requiredName + "'", node);
                }
            }
        }
        return result;
    }

    private boolean find(final String string, final String[] array1, final String[] array2) {
        return find(string, array1) || find(string, array2);
    }

    private boolean find(final String string, final String[] array) {
        for (String str : array) {
            if (str.equals(string)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Node> asSingleItemMap(final Node node) {
        final Map<String, Node> map = asMapping(node);
        if (map.size() != 1) {
            throw new ConfigurationException("Map must contain single element", node);
        }
        return map;
    }

    private Map<String,Node> asSequenceOfSingleItemMaps(final Node node) {
        final Map<String, Node> result = new LinkedHashMap<>();
        for (Node child : asSequence(node)) {
            final Map<String, Node> map = asSingleItemMap(child);
            final String key = map.keySet().iterator().next();
            if (result.containsKey(key)) {
                throw new ConfigurationException("Map contains key '" + key + "' multiple times", node);
            }
            result.putAll(map);
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
