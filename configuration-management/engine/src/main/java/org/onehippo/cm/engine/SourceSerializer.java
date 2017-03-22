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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.onehippo.cm.api.model.ConfigDefinition;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.api.model.DefinitionProperty;
import org.onehippo.cm.api.model.NamespaceDefinition;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.api.model.PropertyOperation;
import org.onehippo.cm.api.model.PropertyType;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.api.model.Value;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.onehippo.cm.engine.Constants.DEFAULT_EXPLICIT_SEQUENCING;

public class SourceSerializer extends AbstractBaseSerializer {

    private final static YamlRepresenter representer = new YamlRepresenter();

    public SourceSerializer() {
        this(DEFAULT_EXPLICIT_SEQUENCING);
    }

    public SourceSerializer(final boolean explicitSequencing) {
        super(explicitSequencing);
    }

    public void serialize(final OutputStream outputStream, final Source source, final Consumer<String> resourceConsumer) throws IOException {
        final Node node = representSource(source, resourceConsumer);
        serializeNode(outputStream, node);
    }

    private Node representSource(final Source source, final Consumer<String> resourceConsumer) {
        final List<NodeTuple> configDefinitionTuples = new ArrayList<>();
        final List<NodeTuple> contentDefinitionTuples = new ArrayList<>();
        final List<Node> namespaceDefinitionNodes = new ArrayList<>();
        final List<Node> nodeTypeDefinitionNodes = new ArrayList<>();

        for (Definition definition : source.getDefinitions()) {
            switch (definition.getType()) {
                case CONFIG:
                    configDefinitionTuples.add(representConfigDefinition((ConfigDefinition) definition, resourceConsumer));
                    break;
                case CONTENT:
                    contentDefinitionTuples.add(representContentDefinition((ContentDefinition) definition, resourceConsumer));
                    break;
                case NAMESPACE:
                    namespaceDefinitionNodes.add(representNamespaceDefinition((NamespaceDefinition) definition));
                    break;
                case NODETYPE:
                    nodeTypeDefinitionNodes.add(representNodetypeDefinition((NodeTypeDefinition) definition, resourceConsumer));
                    break;
                default:
                    throw new IllegalArgumentException("Cannot serialize definition, unknown type: " + definition.getType());
            }
        }

        final List<NodeTuple> definitionNodes = new ArrayList<>();
        if (namespaceDefinitionNodes.size() > 0) {
            definitionNodes.add(createStrSeqTuple("namespace", namespaceDefinitionNodes));
        }
        if (nodeTypeDefinitionNodes.size() > 0) {
            definitionNodes.add(createStrSeqTuple("cnd", nodeTypeDefinitionNodes));
        }
        if (configDefinitionTuples.size() > 0) {
            definitionNodes.add(createStrOptionalSequenceTuple("config", configDefinitionTuples));
        }
        if (contentDefinitionTuples.size() > 0) {
            definitionNodes.add(createStrOptionalSequenceTuple("content", contentDefinitionTuples));
        }

        final List<NodeTuple> sourceTuples = new ArrayList<>();
        sourceTuples.add(createStrMapTuple("definitions", definitionNodes));
        return new MappingNode(Tag.MAP, sourceTuples, false);
    }

    private NodeTuple representConfigDefinition(final ConfigDefinition definition, final Consumer<String> resourceConsumer) {
        return representDefinitionNode(definition.getNode(), resourceConsumer);
    }

    private NodeTuple representContentDefinition(final ContentDefinition definition, final Consumer<String> resourceConsumer) {
        return representDefinitionNode(definition.getNode(), resourceConsumer);
    }

    private NodeTuple representDefinitionNode(final DefinitionNode node, final Consumer<String> resourceConsumer) {
        final List<NodeTuple> children = new ArrayList<>(node.getProperties().size() + node.getNodes().size());

        if (node.isDelete()) {
            children.add(representNodeDelete());
        }
        if (node.getOrderBefore().isPresent()) {
            children.add(representNodeOrderBefore(node.getOrderBefore().get()));
        }
        for (DefinitionProperty childProperty : node.getProperties().values()) {
            children.add(representProperty(childProperty, resourceConsumer));
        }
        for (DefinitionNode childNode : node.getNodes().values()) {
            children.add(representDefinitionNode(childNode, resourceConsumer));
        }

        final String name = node.isRoot() ? node.getPath() : "/" + node.getName();
        return createStrOptionalSequenceTuple(name, children);
    }

    private NodeTuple representNodeDelete() {
        return new NodeTuple(createStrScalar(".meta:delete"), new ScalarNode(Tag.BOOL, "true", null, null, null));
    }

    private NodeTuple representNodeOrderBefore(final String name) {
        return createStrStrTuple(".meta:order-before", name);
    }

    private NodeTuple representProperty(final DefinitionProperty property, final Consumer<String> resourceConsumer) {
        if (requiresValueMap(property)) {
            return representPropertyUsingMap(property, resourceConsumer);
        } else {
            return representPropertyUsingScalarOrSequence(property);
        }
    }

    private NodeTuple representPropertyUsingMap(final DefinitionProperty property, final Consumer<String> resourceConsumer) {
        final List<NodeTuple> valueMapTuples = new ArrayList<>(2);

        if (property.getOperation() == PropertyOperation.DELETE) {
            valueMapTuples.add(createStrStrTuple("operation", "delete"));
        } else {
            if (property.getOperation() != PropertyOperation.REPLACE) {
                valueMapTuples.add(createStrStrTuple("operation", property.getOperation().toString().toLowerCase()));
            }
            valueMapTuples.add(createStrStrTuple("type", property.getValueType().name().toLowerCase()));

            final boolean hasResourceValues = hasResourceValues(property);
            final String key;
            if (hasResourceValues) {
                key = "resource";
            } else if (hasPathValues(property)) {
                key = "path";
            } else {
                key = "value";
            }

            if (property.getType() == PropertyType.SINGLE) {
                final Value value = property.getValue();
                valueMapTuples.add(new NodeTuple(createStrScalar(key), representValue(value)));
                if (hasResourceValues) {
                    resourceConsumer.accept(value.getString());
                }
            } else {
                final List<Node> valueNodes = new ArrayList<>(property.getValues().length);
                for (Value value : property.getValues()) {
                    valueNodes.add(representValue(value));
                    if (hasResourceValues) {
                        resourceConsumer.accept(value.getString());
                    }
                }
                valueMapTuples.add(createStrSeqTuple(key, valueNodes, true));
            }
        }

        return new NodeTuple(createStrScalar(property.getName()), new MappingNode(Tag.MAP, valueMapTuples, false));
    }

    private NodeTuple representPropertyUsingScalarOrSequence(final DefinitionProperty property) {
        if (property.getType() == PropertyType.SINGLE) {
            return new NodeTuple(createStrScalar(property.getName()), representValue(property.getValue()));
        } else {
            final List<Node> valueNodes = new ArrayList<>(property.getValues().length);
            for (Value value : property.getValues()) {
                valueNodes.add(representValue(value));
            }
            return createStrSeqTuple(property.getName(), valueNodes, true);
        }
    }

    private boolean requiresValueMap(final DefinitionProperty property) {
        if (property.getOperation() != PropertyOperation.REPLACE) {
            return true;
        }

        if (hasResourceValues(property)) {
            return true;
        }

        if (property.getName().equals(JCR_PRIMARYTYPE) || property.getName().equals(JCR_MIXINTYPES)) {
            return false;
        }

        switch (property.getValueType()) {
            case BINARY:
            case BOOLEAN:
            case DOUBLE:
            case DATE:
            case LONG:
                // these types are auto-detected by the parser, except when they are empty sequences
                return property.getType() != PropertyType.SINGLE && property.getValues().length == 0;
            case STRING:
                return false;
            default:
                return true;
        }
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

    private boolean hasPathValues(final DefinitionProperty property) {
        if (property.getType() == PropertyType.SINGLE) {
            return property.getValue().isPath();
        }

        for (Value value : property.getValues()) {
            if (value.isPath()) return true;
        }

        return false;
    }

    private Node representValue(final Value value) {
        switch (value.getType()) {
            case DECIMAL:
                // Explicitly represent BigDecimal as string; SnakeYaml does not represent BigDecimal nicely
            case REFERENCE:
            case WEAKREFERENCE:
            case URI:
                return representer.represent(value.getString());
            default:
                return representer.represent(value.getObject());
        }
    }

    private Node representNamespaceDefinition(final NamespaceDefinition definition) {
        final List<NodeTuple> tuples = new ArrayList<>(1);
        tuples.add(createStrStrTuple("prefix", definition.getPrefix()));
        tuples.add(createStrStrTuple("uri", definition.getURI().toString()));
        return new MappingNode(Tag.MAP, tuples, false);
    }

    private Node representNodetypeDefinition(final NodeTypeDefinition definition, final Consumer<String> resourceConsumer) {
        if (definition.isResource()) {
            resourceConsumer.accept(definition.getValue());
            final List<NodeTuple> tuples = new ArrayList<>(1);
            tuples.add(createStrStrTuple("resource", definition.getValue()));
            return new MappingNode(Tag.MAP, tuples, false);
        } else {
            return createStrScalar(definition.getValue(), '|');
        }
    }

}
