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
package org.onehippo.cm.model.serializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.onehippo.cm.model.BinaryItem;
import org.onehippo.cm.model.ConfigDefinition;
import org.onehippo.cm.model.ConfigurationItemCategory;
import org.onehippo.cm.model.ContentDefinition;
import org.onehippo.cm.model.CopyItem;
import org.onehippo.cm.model.Definition;
import org.onehippo.cm.model.DefinitionNode;
import org.onehippo.cm.model.DefinitionProperty;
import org.onehippo.cm.model.DefinitionType;
import org.onehippo.cm.model.ModuleContext;
import org.onehippo.cm.model.NamespaceDefinition;
import org.onehippo.cm.model.PostProcessItem;
import org.onehippo.cm.model.PropertyOperation;
import org.onehippo.cm.model.PropertyType;
import org.onehippo.cm.model.Source;
import org.onehippo.cm.model.Value;
import org.onehippo.cm.model.ValueType;
import org.onehippo.cm.model.WebFileBundleDefinition;
import org.onehippo.cm.model.mapper.ValueFileMapperProvider;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.reader.StreamReader;

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.onehippo.cm.model.Constants.CND_KEY;
import static org.onehippo.cm.model.Constants.DEFINITIONS;
import static org.onehippo.cm.model.Constants.META_CATEGORY_KEY;
import static org.onehippo.cm.model.Constants.META_DELETE_KEY;
import static org.onehippo.cm.model.Constants.META_IGNORE_REORDERED_CHILDREN;
import static org.onehippo.cm.model.Constants.META_ORDER_BEFORE_KEY;
import static org.onehippo.cm.model.Constants.META_RESIDUAL_CHILD_NODE_CATEGORY_KEY;
import static org.onehippo.cm.model.Constants.OPERATION_KEY;
import static org.onehippo.cm.model.Constants.PATH_KEY;
import static org.onehippo.cm.model.Constants.RESOURCE_KEY;
import static org.onehippo.cm.model.Constants.TYPE_KEY;
import static org.onehippo.cm.model.Constants.URI_KEY;
import static org.onehippo.cm.model.Constants.VALUE_KEY;

public class SourceSerializer extends AbstractBaseSerializer {

    private final static YamlRepresenter representer = new YamlRepresenter();

    private final ModuleContext moduleContext;
    protected final Source source;
    private final ValueFileMapperProvider mapperProvider = ValueFileMapperProvider.getInstance();


    public SourceSerializer(ModuleContext moduleContext, Source source, boolean explicitSequencing) {
        super(explicitSequencing);
        this.moduleContext = moduleContext;
        this.source = source;
    }

    public Node representSource(final Consumer<PostProcessItem> resourceConsumer) {
        final List<NodeTuple> configDefinitionTuples = new ArrayList<>();
        final List<NodeTuple> contentDefinitionTuples = new ArrayList<>();
        final List<NodeTuple> namespaceDefinitionTuples = new ArrayList<>();
        final List<Node> webFilesDefinitionNodes = new ArrayList<>();

        for (Definition definition : source.getDefinitions()) {
            switch (definition.getType()) {
                case CONFIG:
                    configDefinitionTuples.add(representConfigDefinition((ConfigDefinition) definition, resourceConsumer));
                    break;
                case CONTENT:
                    contentDefinitionTuples.add(representContentDefinition((ContentDefinition) definition, resourceConsumer));
                    break;
                case NAMESPACE:
                    namespaceDefinitionTuples.add(representNamespaceDefinition((NamespaceDefinition) definition, resourceConsumer));
                    break;
                case WEBFILEBUNDLE:
                    webFilesDefinitionNodes.add(representWebFilesDefinition((WebFileBundleDefinition) definition));
                    break;
                default:
                    throw new IllegalArgumentException("Cannot serialize definition, unknown type: " + definition.getType());
            }
        }

        final List<NodeTuple> definitionNodes = new ArrayList<>();
        if (namespaceDefinitionTuples.size() > 0) {
            definitionNodes.add(createStrOptionalSequenceTuple(DefinitionType.NAMESPACE.toString(), namespaceDefinitionTuples));
        }
        if (configDefinitionTuples.size() > 0) {
            definitionNodes.add(createStrOptionalSequenceTuple(DefinitionType.CONFIG.toString(), configDefinitionTuples));
        }
        if (contentDefinitionTuples.size() > 0) {
            definitionNodes.add(createStrOptionalSequenceTuple(DefinitionType.CONTENT.toString(), contentDefinitionTuples));
        }
        if (webFilesDefinitionNodes.size() > 0) {
            definitionNodes.add(createStrSeqTuple(DefinitionType.WEBFILEBUNDLE.toString(), webFilesDefinitionNodes));
        }

        final List<NodeTuple> sourceTuples = new ArrayList<>();
        sourceTuples.add(createStrMapTuple(DEFINITIONS, definitionNodes));
        return new MappingNode(Tag.MAP, sourceTuples, false);
    }

    private NodeTuple representConfigDefinition(final ConfigDefinition definition, final Consumer<PostProcessItem> resourceConsumer) {
        return representDefinitionNode(definition.getNode(), resourceConsumer);
    }

    protected NodeTuple representContentDefinition(final ContentDefinition definition, final Consumer<PostProcessItem> resourceConsumer) {
        return representDefinitionNode(definition.getNode(), resourceConsumer);
    }

    private NodeTuple representDefinitionNode(final DefinitionNode node, final Consumer<PostProcessItem> resourceConsumer) {
        final List<NodeTuple> children = new ArrayList<>(node.getProperties().size() + node.getNodes().size());

        if (node.isDelete()) {
            children.add(representNodeDelete());
        }
        if (node.getOrderBefore() != null) {
            children.add(representNodeOrderBefore(node.getOrderBefore()));
        }
        if (node.getIgnoreReorderedChildren() != null) {
            children.add(representNodeIgnoreReorderedChildren(node.getIgnoreReorderedChildren()));
        }
        if (node.getCategory() != null) {
            children.add(representCategory(META_CATEGORY_KEY, node.getCategory()));
        }
        if (node.getResidualChildNodeCategory() != null) {
            children.add(representCategory(META_RESIDUAL_CHILD_NODE_CATEGORY_KEY, node.getResidualChildNodeCategory()));
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
        return new NodeTuple(createStrScalar(META_DELETE_KEY), new ScalarNode(Tag.BOOL, "true", null, null, null));
    }

    private NodeTuple representNodeOrderBefore(final String name) {
        return createStrStrTuple(META_ORDER_BEFORE_KEY, name);
    }

    private NodeTuple representNodeIgnoreReorderedChildren(final Boolean ignoreReorderedChildren) {
        return new NodeTuple(createStrScalar(META_IGNORE_REORDERED_CHILDREN), new ScalarNode(Tag.BOOL, ignoreReorderedChildren.toString(), null, null, null));
    }

    private NodeTuple representCategory(final String metaDataField, final ConfigurationItemCategory category) {
        return createStrStrTuple(metaDataField, category.toString());
    }

    private NodeTuple representProperty(final DefinitionProperty property, final Consumer<PostProcessItem> resourceConsumer) {
        if (requiresValueMap(property)) {
            return representPropertyUsingMap(property, resourceConsumer);
        } else {
            return representPropertyUsingScalarOrSequence(property);
        }
    }

    private NodeTuple representPropertyUsingMap(final DefinitionProperty property, final Consumer<PostProcessItem> resourceConsumer) {
        final List<NodeTuple> valueMapTuples = new ArrayList<>(2);

        if (property.getCategory() != null) {
            valueMapTuples.add(representCategory(META_CATEGORY_KEY, property.getCategory()));
        } else if (property.getOperation() == PropertyOperation.DELETE) {
            valueMapTuples.add(createStrStrTuple(OPERATION_KEY, property.getOperation().toString()));
        } else {
            if (property.getOperation() != PropertyOperation.REPLACE) {
                valueMapTuples.add(createStrStrTuple(OPERATION_KEY, property.getOperation().toString()));
            }
            valueMapTuples.add(createStrStrTuple(TYPE_KEY, property.getValueType().name().toLowerCase()));

            final boolean exposeAsResource = hasResourceValues(property) || isBinaryProperty(property);
            final String key;
            if (exposeAsResource) {
                key = RESOURCE_KEY;
            } else if (hasPathValues(property)) {
                key = PATH_KEY;
            } else {
                key = VALUE_KEY;
            }

            if (property.getType() == PropertyType.SINGLE) {

                final Value value = property.getValue();
                final Node valueNode = representValue(value);
                final ScalarNode keyNode = createStrScalar(key);
                valueMapTuples.add(new NodeTuple(keyNode, valueNode));

                if (exposeAsResource) {
                    processSingleResource(resourceConsumer, value, valueNode);
                }
            } else {
                final List<Node> valueNodes = new ArrayList<>(property.getValues().length);
                for (Value value : property.getValues()) {
                    final Node valueNode = representValue(value);
                    valueNodes.add(valueNode);

                    if (isBinaryEmbedded(value)) {
                        resourceConsumer.accept(new BinaryItem(value, (ScalarNode) valueNode));
                    }
                    else if (exposeAsResource) {
                        resourceConsumer.accept(new CopyItem(value));
                    }
                }

                valueMapTuples.add(createStrSeqTuple(key, valueNodes, true));
            }
        }

        return new NodeTuple(createStrScalar(property.getName()), new MappingNode(Tag.MAP, valueMapTuples, false));
    }

    private void processSingleResource(Consumer<PostProcessItem> resourceConsumer, Value value, Node valueNode) {
        final PostProcessItem postProcessItem = isBinaryEmbedded(value) ? new BinaryItem(value, (ScalarNode) valueNode) : new CopyItem(value);
        resourceConsumer.accept(postProcessItem);
    }

    private boolean isBinaryEmbedded(Value value) {
        return value.getType() == ValueType.BINARY && !value.isResource();
    }

    private boolean isBinaryProperty(final DefinitionProperty property) {
        if (property.getType() == PropertyType.SINGLE) {
            return property.getValueType() == ValueType.BINARY;
        }
        return Arrays.stream(property.getValues()).anyMatch(value -> value.getType() == ValueType.BINARY);
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

        if (property.getOperation() != PropertyOperation.REPLACE
                || hasResourceValues(property)
                || hasPathValues(property)
                || property.getCategory() != null) {
            return true;
        }

        if (property.getName().equals(JCR_PRIMARYTYPE) || property.getName().equals(JCR_MIXINTYPES)) {
            return false;
        }

        switch (property.getValueType()) {
            case BOOLEAN:
            case DOUBLE:
            case DATE:
            case LONG:
                // these types are auto-detected by the parser, except when they are empty sequences
                return property.getType() != PropertyType.SINGLE && property.getValues().length == 0;
            case STRING:
                return shouldHaveExplicitType(property);
            case BINARY:
            default:
                return true;
        }
    }

    private boolean shouldHaveExplicitType(final DefinitionProperty property) {
        if (property.getType() == PropertyType.SINGLE) {
            final String propertyValue = property.getValue().getString();
            return !StreamReader.isPrintable(propertyValue);
        } else {
            return !allElementsArePrintable(property);
        }
    }

    private boolean allElementsArePrintable(DefinitionProperty property) {
        return Arrays.stream(property.getValues()).map(Value::getString).allMatch(StreamReader::isPrintable);
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
            case BINARY:
                String nodeValue = value.isResource() ? value.getString() : moduleContext.generateUniqueName(source, mapperProvider.generateName(value));
                return representer.represent(nodeValue);
            default:
                return representer.represent(value.getObject());
        }
    }

    private NodeTuple representNamespaceDefinition(final NamespaceDefinition definition, final Consumer<PostProcessItem> resourceConsumer) {
        final List<NodeTuple> children = new ArrayList<>(2);
        children.add(createStrStrTuple(URI_KEY, definition.getURI().toString()));
        if (definition.getCndPath() != null) {
            resourceConsumer.accept(new CopyItem(definition.getCndPath()));
            children.add(createStrStrTuple(CND_KEY, definition.getCndPath().getString()));
        }
        return new NodeTuple(createStrScalar(definition.getPrefix()), new MappingNode(Tag.MAP, children, false));
    }

    private Node representWebFilesDefinition(final WebFileBundleDefinition definition) {
        return createStrScalar(definition.getName());
    }

}
