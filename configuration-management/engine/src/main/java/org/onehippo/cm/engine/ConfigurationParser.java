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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.ProjectImpl;
import org.yaml.snakeyaml.Yaml;
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
        public ConfigurationException(final String message) {
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

    private boolean find(final String string, final String[] array) {
        for (String str : array) {
            if (str.equals(string)) {
                return true;
            }
        }
        return false;
    }

    private boolean find(final String string, final String[] array1, final String[] array2) {
        return find(string, array1) || find(string, array2);
    }

    Map<String, Node> asMapping(final Node node, final String[] requiredNames, final String[] optionalNames) {
        if (node == null) {
            if (requiredNames.length != 0) {
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
            if (!find(key, requiredNames, optionalNames)) {
                throw new ConfigurationException("Element '" + key + "' is not allowed", node);
            }
            result.put(key, tuple.getValueNode());
        }
        for (String requiredName : requiredNames) {
            if (!result.containsKey(requiredName)) {
                throw new ConfigurationException("Node must contain element '" + requiredName + "'", node);
            }
        }
        return result;
    }

    List<Node> asSequence(final Node node) {
        if (node == null) {
            return Collections.emptyList();
        }
        if (node.getNodeId() != NodeId.sequence) {
            throw new ConfigurationException("Node must be sequence", node);
        }
        final SequenceNode sequenceNode = (SequenceNode) node;
        return sequenceNode.getValue();
    }

    ScalarNode asScalar(final Node node) {
        if (node == null) {
            return null;
        }
        if (node.getNodeId() != NodeId.scalar) {
            throw new ConfigurationException("Node must be scalar", node);
        }
        return (ScalarNode) node;
    }

    String asStringScalar(final Node node) {
        final ScalarNode scalarNode = asScalar(node);
        if (!scalarNode.getTag().equals(Tag.STR)) {
            throw new ConfigurationException("Scalar must be a string", node);
        }
        return scalarNode.getValue();
    }

    private List<String> parseAfter(final Node node) {
        // TODO support sequences
        if (node == null) {
            return Collections.emptyList();
        }
        final List<String> result = new ArrayList<>();
        result.add(asStringScalar(node));
        return result;
    }

    void constructModule(final Node src, final ProjectImpl parent) {
        final Map<String, Node> map = asMapping(src, new String[]{"name"}, new String[]{"after"});
        final String name = asStringScalar(map.get("name"));
        final ModuleImpl module = parent.addModule(name);
        module.setAfter(parseAfter(map.get("after")));
    }

    void constructProject(final Node src, final ConfigurationImpl parent) {
        final Map<String, Node> sourceMap = asMapping(src, new String[]{"name", "modules"}, new String[]{"after"});
        final String name = asStringScalar(sourceMap.get("name"));
        final ProjectImpl project = parent.addProject(name);
        project.setAfter(parseAfter(sourceMap.get("after")));

        for (Node moduleNode : asSequence(sourceMap.get("modules"))) {
            constructModule(moduleNode, project);
        }
    }

    void constructConfiguration(final Node src, final Map<String, Configuration> parent) {
        final Map<String, Node> configurationMap = asMapping(src, new String[]{"name", "projects"}, new String[]{"after"});
        final String name = asStringScalar(configurationMap.get("name"));
        final ConfigurationImpl configuration = new ConfigurationImpl(name);
        configuration.setAfter(parseAfter(configurationMap.get("after")));
        parent.put(name, configuration);

        for (Node projectNode : asSequence(configurationMap.get("projects"))) {
            constructProject(projectNode, configuration);
        }
    }

    Map<String, Configuration> parseRepoConfig(final Node src) {
        final Map<String, Configuration> result = new LinkedHashMap<>();
        final Map<String, Node> sourceMap = asMapping(src, new String[]{"configurations"}, new String[0]);

        for (Node configurationNode : asSequence(sourceMap.get("configurations"))) {
            constructConfiguration(configurationNode, result);
        }

        return result;
    }

    void parseSource(final Node source, final Module module) {
        /*
        final Map<String, Node> sourceNode = asMapping(source);
        final List<Node> instructionNodes = asSequence(sourceNode.get("instructions"));

        for (Node instruction : instructionNodes) {
            final Map<String, Node> instructionMap = asMapping(instruction);
            if (instructionMap.size() != 1) {
                throw new ConfigurationException("Instruction must have one key representing its type", instruction);
            }
            final String instructionType = instructionMap.keySet().iterator().next();
            if (!"configuration".equals(instructionType)) {
                throw new ConfigurationException("Instruction must be of type 'configuration', found '" + instructionType + "'", instruction);
            }
            final SourceImpl sourceImpl = new SourceImpl();
            final ConfigurationDefinitionImpl configurationDefinition = new ConfigurationDefinitionImpl();
            configuration.setName(configurationName);
            destination.put(configurationName, configuration);
            final Map<String, Node> configurationDetails = asMapping(instructionMap.get(configurationName));
            if (configurationDetails != null) {
                populateConfiguration(configuration, configurationDetails);
            }
        }
        */
    }

    private ModuleImpl getModuleForSource(final Map<String, Configuration> configurations, final URL url) {
        final String[] parts = url.getPath().split("/");
        if (parts.length < 4) {
            throw new IllegalArgumentException(
                    MessageFormat.format(
                            "URL {} must consist of at least 4 elements, found only {}",
                            url.getPath(), parts.length));
        }
        final String configurationName = parts[parts.length - 4];
        final String projectName = parts[parts.length - 3];
        final String moduleName = parts[parts.length - 2];
        final Configuration configuration = configurations.get(configurationName);
        if (configuration == null) {
            throw new IllegalArgumentException(MessageFormat.format("Configuration '{}' not found", configurationName));
        }
        final Project project = configuration.getProjects().get(projectName);
        if (project == null) {
            throw new IllegalArgumentException(MessageFormat.format("Project '{}' not found in configuration '{}'", projectName, configurationName));
        }
        final Module module = project.getModules().get(moduleName);
        if (module == null) {
            throw new IllegalArgumentException(MessageFormat.format("Module '{}' not found in project '{}'", moduleName, projectName));
        }
        return (ModuleImpl) module;
    }

    List<Module> collectModules(Map<String, Configuration> configurations) {
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
            parseSource(sourceNode, module);
        }

        return configurations;
    }

}
