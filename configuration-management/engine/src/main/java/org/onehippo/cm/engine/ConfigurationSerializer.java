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
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.onehippo.cm.api.model.ConfigDefinition;
import org.onehippo.cm.api.model.Configuration;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.api.model.DefinitionProperty;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.NamespaceDefinition;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.api.model.Orderable;
import org.onehippo.cm.api.model.Project;
import org.onehippo.cm.api.model.PropertyType;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.api.model.Value;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;
import org.yaml.snakeyaml.serializer.Serializer;

public class ConfigurationSerializer {

    public void serializeNode(final Path destination, final Map<String, Configuration> configurations) throws IOException {
        serializeRepoConfig(destination, configurations);

        for (Configuration configuration : configurations.values()) {
            for (Project project: configuration.getProjects().values()) {
                for (Module module : project.getModules().values()) {
                    for (Source source : module.getSources().values()) {
                        serializeSource(destination, source);
                    }
                }
            }
        }
    }

    private void serializeRepoConfig(final Path destination, final Map<String, Configuration> configurations) throws IOException {
        final Node node = representRepoConfig(configurations);
        serializeNode(destination.resolve("repo-config.yaml"), node);
    }

    private void serializeSource(final Path destination, final Source source) throws IOException {
        final Path sourcePath = Paths.get(source.getPath());
        if (sourcePath.isAbsolute()) {
            throw new IOException("Source must not specify an absolute path: " + source.getPath());
        }

        final Node node = representSource(source);
        serializeNode(destination.resolve("repo-config").resolve(sourcePath), node);
    }

    private void serializeNode(final Path path, final Node node) throws IOException {
        Files.createDirectories(path.getParent());
        final Writer writer = new PrintWriter(path.toFile(), StandardCharsets.UTF_8.toString());
        final DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setIndicatorIndent(2);
        dumperOptions.setIndent(4);
        final Resolver resolver = new Resolver();
        final Serializer serializer = new Serializer(new Emitter(writer, dumperOptions), resolver, dumperOptions, null);

        serializer.open();
        serializer.serialize(node);
        serializer.close();
    }

    private Node representRepoConfig(final Map<String, Configuration> configurations) {
        final List<NodeTuple> rootTuples = new ArrayList<>();

        final List<Node> configurationNodes = new ArrayList<>();
        for (Configuration configuration : configurations.values()) {
            configurationNodes.add(representConfiguration(configuration));
        }
        rootTuples.add(createStrSeqTuple("configurations", configurationNodes));

        return new MappingNode(Tag.MAP, rootTuples, false);
    }

    private Node representConfiguration(final Configuration configuration) {
        final List<NodeTuple> tuples = new ArrayList<>();
        tuples.addAll(representOrderable(configuration));

        final List<Node> projectNodes = new ArrayList<>();
        for (Project project: configuration.getProjects().values()) {
            projectNodes.add(representProject(project));
        }
        tuples.add(createStrSeqTuple("projects", projectNodes));

        return new MappingNode(Tag.MAP, tuples, false);
    }

    private Node representProject(final Project project) {
        final List<NodeTuple> tuples = new ArrayList<>();
        tuples.addAll(representOrderable(project));

        final List<Node> moduleNodes = new ArrayList<>();
        for (Module module : project.getModules().values()) {
            moduleNodes.add(representModule(module));
        }
        tuples.add(createStrSeqTuple("modules", moduleNodes));

        return new MappingNode(Tag.MAP, tuples, false);
    }

    private Node representModule(final Module module) {
        final List<NodeTuple> tuples = new ArrayList<>();
        tuples.addAll(representOrderable(module));
        return new MappingNode(Tag.MAP, tuples, false);
    }

    private List<NodeTuple> representOrderable(final Orderable orderable) {
        final List<NodeTuple> tuples = new ArrayList<>();
        tuples.add(createStrStrTuple("name", orderable.getName()));

        final List<String> afters = orderable.getAfter();
        switch (afters.size()) {
            case 0:
                break;
            case 1:
                tuples.add(createStrStrTuple("after", afters.get(0)));
                break;
            default:
                final List<Node> afterNodes = new ArrayList<>();
                for (String after : afters) {
                    afterNodes.add(new ScalarNode(Tag.STR, after, null, null, null));
                }
                tuples.add(createStrSeqTuple("after", afterNodes, true));
                break;
        }
        return tuples;
    }

    private Node representSource(final Source source) {
        final List<Node> configDefinitionNodes = new ArrayList<>();
        final List<Node> contentDefinitionNodes = new ArrayList<>();
        final List<Node> namespaceDefinitionNodes = new ArrayList<>();
        final List<Node> nodeTypeDefinitionNodes = new ArrayList<>();

        for (Definition definition : source.getDefinitions()) {
            switch (definition.getType()) {
                case CONFIG:
                    configDefinitionNodes.add(representConfigDefinition((ConfigDefinition) definition));
                    break;
                case CONTENT:
                    contentDefinitionNodes.add(representContentDefinition((ContentDefinition) definition));
                    break;
                case NAMESPACE:
                    namespaceDefinitionNodes.add(representNamespaceDefinition((NamespaceDefinition) definition));
                    break;
                case NODETYPE:
                    nodeTypeDefinitionNodes.add(representNodetypeDefinition((NodeTypeDefinition) definition));
                    break;
                default:
                    throw new IllegalArgumentException("Cannot serialize definition, unknown type: " + definition.getType());
            }
        }

        final List<Node> definitionNodes = new ArrayList<>();
        if (namespaceDefinitionNodes.size() > 0) {
            final List<NodeTuple> nodeTypeTuples = new ArrayList<>();
            nodeTypeTuples.add(createStrSeqTuple("namespace", namespaceDefinitionNodes));
            definitionNodes.add(new MappingNode(Tag.MAP, nodeTypeTuples, false));
        }
        if (nodeTypeDefinitionNodes.size() > 0) {
            final List<NodeTuple> nodeTypeTuples = new ArrayList<>();
            nodeTypeTuples.add(createStrSeqTuple("cnd", nodeTypeDefinitionNodes));
            definitionNodes.add(new MappingNode(Tag.MAP, nodeTypeTuples, false));
        }
        if (configDefinitionNodes.size() > 0) {
            final List<NodeTuple> configTuples = new ArrayList<>();
            configTuples.add(createStrSeqTuple("config", configDefinitionNodes));
            definitionNodes.add(new MappingNode(Tag.MAP, configTuples, false));
        }
        if (contentDefinitionNodes.size() > 0) {
            final List<NodeTuple> contentTuples = new ArrayList<>();
            contentTuples.add(createStrSeqTuple("content", contentDefinitionNodes));
            definitionNodes.add(new MappingNode(Tag.MAP, contentTuples, false));
        }

        final List<NodeTuple> sourceTuples = new ArrayList<>();
        sourceTuples.add(createStrSeqTuple("instructions", definitionNodes));
        return new MappingNode(Tag.MAP, sourceTuples, false);
    }

    private Node representConfigDefinition(final ConfigDefinition definition) {
        return representDefinitionNode(definition.getNode());
    }

    private Node representContentDefinition(final ContentDefinition definition) {
        return representDefinitionNode(definition.getNode());
    }

    private Node representDefinitionNode(final DefinitionNode node) {
        final List<Node> children = new ArrayList<>(node.getProperties().size() + node.getNodes().size());

        for (DefinitionProperty childProperty : node.getProperties().values()) {
            children.add(representProperty(childProperty));
        }
        for (DefinitionNode childNode : node.getNodes().values()) {
            children.add(representDefinitionNode(childNode));
        }

        final List<NodeTuple> tuples = new ArrayList<>(1);
        final String name = node.isRoot() ? node.getPath() : "/" + node.getName();
        tuples.add(createStrSeqTuple(name, children));
        return new MappingNode(Tag.MAP, tuples, false);
    }

    private Node representProperty(final DefinitionProperty property) {
        if (requiresValueMap(property)) {
            return representPropertyUsingMap(property);
        } else {
            return representPropertyUsingScalarOrSequence(property);
        }
    }

    private boolean requiresValueMap(final DefinitionProperty property) {
        if (property.getType() == PropertyType.SINGLE) {
            return property.getValue().isResource();
        }

        if (property.getValues().length == 0) {
            return true;
        }

        return hasResourceValues(property);
    }

    private Node representPropertyUsingMap(final DefinitionProperty property) {
        final List<NodeTuple> valueMapTuples = new ArrayList<>(2);
        valueMapTuples.add(createStrStrTuple("type", property.getValueType().name().toLowerCase()));

        final String key = hasResourceValues(property) ? "resource" : "value";

        if (property.getType() == PropertyType.SINGLE) {
            final Value value = property.getValue();
            valueMapTuples.add(new NodeTuple(createStrScalar(key), representValue(value)));
        } else {
            final List<Node> valueNodes = new ArrayList<>(property.getValues().length);
            for (Value value : property.getValues()) {
                valueNodes.add(representValue(value));
            }
            valueMapTuples.add(createStrSeqTuple(key, valueNodes, true));
        }

        final List<NodeTuple> propertyTuples = new ArrayList<>(1);
        propertyTuples.add(new NodeTuple(
                createStrScalar(property.getName()),
                new MappingNode(Tag.MAP, valueMapTuples, false)));
        return new MappingNode(Tag.MAP, propertyTuples, false);
    }

    private Node representPropertyUsingScalarOrSequence(final DefinitionProperty property) {
        final List<NodeTuple> tuples = new ArrayList<>(1);

        if (property.getType() == PropertyType.SINGLE) {
            tuples.add(new NodeTuple(createStrScalar(property.getName()), representValue(property.getValue())));
        } else {
            final List<Node> valueNodes = new ArrayList<>(property.getValues().length);
            for (Value value : property.getValues()) {
                valueNodes.add(representValue(value));
            }
            tuples.add(createStrSeqTuple(property.getName(), valueNodes, true));
        }

        return new MappingNode(Tag.MAP, tuples, false);
    }

    private boolean hasResourceValues(final DefinitionProperty property) {
        if (property.getType() == PropertyType.SINGLE) {
            return property.getValue().isResource();
        }

        for (Value value : property.getValues()) {
            if (value.isResource()) return true;
        }

        return false;
    }

    private Node representValue(final Value value) {
        final Representer representer = new Representer();
        return representer.represent(value.getObject());
    }

    private Node representNamespaceDefinition(final NamespaceDefinition definition) {
        final List<NodeTuple> tuples = new ArrayList<>(1);
        tuples.add(createStrStrTuple("prefix", definition.getPrefix()));
        tuples.add(createStrStrTuple("uri", definition.getURI().toString()));
        return new MappingNode(Tag.MAP, tuples, false);
    }

    private Node representNodetypeDefinition(final NodeTypeDefinition definition) {
        return createStrScalar(definition.getCndString(), '|');
    }

    private static NodeTuple createStrStrTuple(final String key, final String value) {
        return new NodeTuple(createStrScalar(key), createStrScalar(value));
    }

    private static NodeTuple createStrSeqTuple(final String key, final List<Node> value) {
        return createStrSeqTuple(key, value, false);
    }

    private static NodeTuple createStrSeqTuple(final String key, final List<Node> value, final boolean flowStyle) {
        return new NodeTuple(createStrScalar(key), new SequenceNode(Tag.SEQ, value, flowStyle));
    }

    private static ScalarNode createStrScalar(final String str) {
        return new ScalarNode(Tag.STR, str, null, null, null);
    }

    private static ScalarNode createStrScalar(final String str, final Character style) {
        return new ScalarNode(Tag.STR, str, null, null, style);
    }

}
