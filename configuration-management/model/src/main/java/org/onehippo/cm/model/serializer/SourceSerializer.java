/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;

import org.onehippo.cm.model.definition.DefinitionType;
import org.onehippo.cm.model.impl.definition.AbstractDefinitionImpl;
import org.onehippo.cm.model.impl.definition.ConfigDefinitionImpl;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.definition.NamespaceDefinitionImpl;
import org.onehippo.cm.model.impl.definition.WebFileBundleDefinitionImpl;
import org.onehippo.cm.model.impl.source.SourceImpl;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.tree.DefinitionPropertyImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.tree.ConfigurationItemCategory;
import org.onehippo.cm.model.tree.PropertyKind;
import org.onehippo.cm.model.tree.PropertyOperation;
import org.onehippo.cm.model.tree.Value;
import org.onehippo.cm.model.tree.ValueType;
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
import static org.onehippo.cm.model.Constants.HST_HST_PATH;
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

    protected final ModuleContext moduleContext;
    protected SourceImpl source;

    public SourceSerializer(ModuleContext moduleContext, SourceImpl source, boolean explicitSequencing) {
        super(explicitSequencing);
        this.moduleContext = moduleContext;
        this.source = source;
    }

    public Node representSource() {
        final List<NodeTuple> configDefinitionTuples = new ArrayList<>();
        final List<NodeTuple> contentDefinitionTuples = new ArrayList<>();
        final List<NodeTuple> namespaceDefinitionTuples = new ArrayList<>();
        final List<Node> webFilesDefinitionNodes = new ArrayList<>();

        for (AbstractDefinitionImpl definition : source.getDefinitions()) {
            switch (definition.getType()) {
                case CONFIG:
                    configDefinitionTuples.add(representConfigDefinition((ConfigDefinitionImpl) definition));
                    break;
                case CONTENT:
                    contentDefinitionTuples.add(representContentDefinition((ContentDefinitionImpl) definition));
                    break;
                case NAMESPACE:
                    namespaceDefinitionTuples.add(representNamespaceDefinition((NamespaceDefinitionImpl) definition));
                    break;
                case WEBFILEBUNDLE:
                    webFilesDefinitionNodes.add(representWebFilesDefinition((WebFileBundleDefinitionImpl) definition));
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
        if (webFilesDefinitionNodes.size() == 1) {
            definitionNodes.add(new NodeTuple(createStrScalar(DefinitionType.WEBFILEBUNDLE.toString()), webFilesDefinitionNodes.get(0)));
        }
        else if (webFilesDefinitionNodes.size() > 1) {
            definitionNodes.add(createStrSeqTuple(DefinitionType.WEBFILEBUNDLE.toString(), webFilesDefinitionNodes, true));
        }

        final List<NodeTuple> sourceTuples = new ArrayList<>();
        sourceTuples.add(createStrMapTuple(DEFINITIONS, definitionNodes));
        return new MappingNode(Tag.MAP, sourceTuples, false);
    }

    protected NodeTuple representConfigDefinition(final ConfigDefinitionImpl definition) {
        return representDefinitionNode(definition.getNode());
    }

    protected NodeTuple representContentDefinition(final ContentDefinitionImpl definition) {
        return representDefinitionNode(definition.getNode());
    }

    protected NodeTuple representDefinitionNode(final DefinitionNodeImpl node) {
        final List<NodeTuple> children = new ArrayList<>(node.getProperties().size() + node.getNodes().size());

        if (node.isDelete()) {
            children.add(representNodeDelete());
        } else {
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
            for (DefinitionPropertyImpl childProperty : node.getProperties()) {
                children.add(representProperty(childProperty));
            }
            for (DefinitionNodeImpl childNode : node.getNodes()) {
                children.add(representDefinitionNode(childNode));
            }
        }

        return createStrOptionalSequenceTuple(getName(node), children);
    }

    protected String getName(DefinitionNodeImpl node) {
        // root defs get a full path, but nested defs just get one relative path segment
        if (!node.isRoot()) {
            return "/" + node.getName();
        }
        else {
            // Adjust HST root for output
            final JcrPath inPath = node.getJcrPath();
            final JcrPath hstRoot = node.getDefinition().getSource().getModule().getHstRoot();
            return getStandardizedHstPath(inPath, hstRoot).toString();
        }
    }

    /**
     * If a path has a root node that matches a known HST root node, swap it for the default HST root for
     * purposes of pattern-matching.
     * @param inPath the path to potentially swap for a new root node
     * @return a new path based on the HST default root node
     */
    protected JcrPath getStandardizedHstPath(final JcrPath inPath, final JcrPath hstRoot) {
        if (!inPath.isRoot()
                && hstRoot != null
                && inPath.subpath(0, 1).equals(hstRoot)) {
            if (inPath.getSegmentCount() == 1) {
                return HST_HST_PATH;
            }
            else {
                return HST_HST_PATH.resolve(inPath.subpath(1));
            }
        } else {
            return inPath;
        }
    }

    protected NodeTuple representNodeDelete() {
        return new NodeTuple(createStrScalar(META_DELETE_KEY), new ScalarNode(Tag.BOOL, "true", null, null, null));
    }

    protected NodeTuple representNodeOrderBefore(final String name) {
        return createStrStrTuple(META_ORDER_BEFORE_KEY, name);
    }

    protected NodeTuple representNodeIgnoreReorderedChildren(final Boolean ignoreReorderedChildren) {
        return new NodeTuple(createStrScalar(META_IGNORE_REORDERED_CHILDREN), new ScalarNode(Tag.BOOL, ignoreReorderedChildren.toString(), null, null, null));
    }

    protected NodeTuple representCategory(final String metaDataField, final ConfigurationItemCategory category) {
        return createStrStrTuple(metaDataField, category.toString());
    }

    protected NodeTuple representProperty(final DefinitionPropertyImpl property) {
        if (requiresValueMap(property)) {
            return representPropertyUsingMap(property);
        } else {
            return representPropertyUsingScalarOrSequence(property);
        }
    }

    protected NodeTuple representPropertyUsingMap(final DefinitionPropertyImpl property) {
        final List<NodeTuple> valueMapTuples = new ArrayList<>(2);

        if (property.getOperation() == PropertyOperation.DELETE) {
            valueMapTuples.add(createStrStrTuple(OPERATION_KEY, property.getOperation().toString()));
        } else {
            // .meta:category is no longer mutually-exclusive with value etc.
            if (property.getCategory() != null) {
                valueMapTuples.add(representCategory(META_CATEGORY_KEY, property.getCategory()));
            }
            if (property.isEmptySystemProperty()) {
                // this is a .meta:category system property with no specified value -- don't output anything else here
            } else {
                // otherwise, we need to process operation, type, and value(s)
                if (property.getOperation() != PropertyOperation.REPLACE) {
                    valueMapTuples.add(createStrStrTuple(OPERATION_KEY, property.getOperation().toString()));
                }
                if (ValueType.NAME != property.getValueType() ||
                        !(JCR_PRIMARYTYPE.equals(property.getName()) || JCR_MIXINTYPES.equals(property.getName()))) {
                    valueMapTuples.add(createStrStrTuple(TYPE_KEY, property.getValueType().name().toLowerCase()));
                }

                final boolean exposeAsResource = hasResourceValues(property) || isBinaryProperty(property);
                final String key;
                if (exposeAsResource) {
                    key = RESOURCE_KEY;
                } else if (hasPathValues(property)) {
                    key = PATH_KEY;
                } else {
                    key = VALUE_KEY;
                }

                if (property.getKind() == PropertyKind.SINGLE) {

                    final ValueImpl value = property.getValue();
                    final Node valueNode = representValue(value);
                    final ScalarNode keyNode = createStrScalar(key);
                    valueMapTuples.add(new NodeTuple(keyNode, valueNode));

                    if (exposeAsResource) {
                        processSingleResource(value, valueNode);
                    }
                } else {
                    final List<Node> valueNodes = new ArrayList<>(property.getValues().size());
                    for (ValueImpl value : property.getValues()) {
                        final Node valueNode = representValue(value);
                        valueNodes.add(valueNode);

                        if (isBinaryEmbedded(value)) {
                            serializeBinaryValue(((ScalarNode) valueNode).getValue(), value);
                        }
                        else if (exposeAsResource) {
                            serializeResourceValue(value);
                        }
                    }

                    valueMapTuples.add(createStrSeqTuple(key, valueNodes, true));
                }
            }
        }
        return new NodeTuple(createStrScalar(property.getName()), new MappingNode(Tag.MAP, valueMapTuples, false));
    }

    protected void processSingleResource(ValueImpl value, Node valueNode) {
        if (isBinaryEmbedded(value)) {
            serializeBinaryValue(((ScalarNode) valueNode).getValue(), value);
        } else {
            serializeResourceValue(value);
        }
    }

    protected boolean isBinaryEmbedded(ValueImpl value) {
        return value.getType() == ValueType.BINARY && !value.isResource();
    }

    protected boolean isBinaryProperty(final DefinitionPropertyImpl property) {
        if (property.getKind() == PropertyKind.SINGLE) {
            return property.getValueType() == ValueType.BINARY;
        }
        return property.getValues().stream().anyMatch(value -> value.getType() == ValueType.BINARY);
    }

    protected NodeTuple representPropertyUsingScalarOrSequence(final DefinitionPropertyImpl property) {
        if (property.getKind() == PropertyKind.SINGLE) {
            return new NodeTuple(createStrScalar(property.getName()), representValue(property.getValue()));
        } else {
            final List<Node> valueNodes = new ArrayList<>(property.getValues().size());
            for (ValueImpl value : property.getValues()) {
                valueNodes.add(representValue(value));
            }
            return createStrSeqTuple(property.getName(), valueNodes, true);
        }
    }

    protected boolean requiresValueMap(final DefinitionPropertyImpl property) {

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
                return property.getKind() != PropertyKind.SINGLE && property.getValues().size() == 0;
            case STRING:
                return shouldHaveExplicitType(property);
            case BINARY:
            default:
                return true;
        }
    }

    protected boolean shouldHaveExplicitType(final DefinitionPropertyImpl property) {
        if (property.getKind() == PropertyKind.SINGLE) {
            final String propertyValue = property.getValue().getString();
            return !StreamReader.isPrintable(propertyValue);
        } else {
            return !allElementsArePrintable(property);
        }
    }

    protected boolean allElementsArePrintable(DefinitionPropertyImpl property) {
        return property.getValues().stream().map(Value::getString).allMatch(StreamReader::isPrintable);
    }

    protected boolean hasResourceValues(final DefinitionPropertyImpl property) {
        if (property.getKind() == PropertyKind.SINGLE) {
            return property.getValue().isResource();
        }

        for (Value value : property.getValues()) {
            if (value.isResource()) return true;
        }

        return false;
    }

    protected boolean hasPathValues(final DefinitionPropertyImpl property) {
        if (property.getKind() == PropertyKind.SINGLE) {
            return property.getValue().isPath();
        }

        for (Value value : property.getValues()) {
            if (value.isPath()) return true;
        }

        return false;
    }

    protected Node representValue(final ValueImpl value) {
        if (value.isNewResource()) {
            moduleContext.resolveNewResourceValuePath(source, value);
        }
        switch (value.getType()) {
            case DECIMAL:
                // Explicitly represent BigDecimal as string; SnakeYaml does not represent BigDecimal nicely
            case REFERENCE:
            case WEAKREFERENCE:
            case URI:
                return representer.represent(value.getString());
            case BINARY:
                String nodeValue = !value.isResource() || value.isNewResource() ?
                        moduleContext.generateUniqueName(source, value) : value.getString();
                return representer.represent(nodeValue);
            default:
                return representer.represent(value.getObject());
        }
    }

    protected NodeTuple representNamespaceDefinition(final NamespaceDefinitionImpl definition) {
        final List<NodeTuple> children = new ArrayList<>(2);
        children.add(createStrStrTuple(URI_KEY, definition.getURI().toString()));
        if (definition.getCndPath() != null) {
            serializeResourceValue(definition.getCndPath());
            children.add(createStrStrTuple(CND_KEY, definition.getCndPath().getString()));
        }
        return new NodeTuple(createStrScalar(definition.getPrefix()), new MappingNode(Tag.MAP, children, false));
    }

    protected Node representWebFilesDefinition(final WebFileBundleDefinitionImpl definition) {
        return createStrScalar(definition.getName());
    }

    protected void serializeBinaryValue(final String finalName, final ValueImpl value) {
        moduleContext.serializeBinaryValue(source, finalName, value);
    }

    protected void serializeResourceValue(final ValueImpl value) {
        moduleContext.serializeResourceValue(source, value);
    }
}
