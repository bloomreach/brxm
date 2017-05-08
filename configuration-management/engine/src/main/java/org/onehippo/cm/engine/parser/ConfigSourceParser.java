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
package org.onehippo.cm.engine.parser;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.Definition;
import org.onehippo.cm.api.model.DefinitionType;
import org.onehippo.cm.api.model.ValueType;
import org.onehippo.cm.impl.model.ConfigDefinitionImpl;
import org.onehippo.cm.impl.model.DefinitionNodeImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.SourceImpl;
import org.onehippo.cm.impl.model.WebFileBundleDefinitionImpl;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import static org.onehippo.cm.engine.Constants.DEFINITIONS;
import static org.onehippo.cm.engine.Constants.META_DELETE_KEY;
import static org.onehippo.cm.engine.Constants.META_IGNORE_REORDERED_CHILDREN;
import static org.onehippo.cm.engine.Constants.META_ORDER_BEFORE_KEY;
import static org.onehippo.cm.engine.Constants.PREFIX_KEY;
import static org.onehippo.cm.engine.Constants.RESOURCE_KEY;
import static org.onehippo.cm.engine.Constants.URI_KEY;

public class ConfigSourceParser extends SourceParser {

    public ConfigSourceParser(ResourceInputProvider resourceInputProvider) {
        super(resourceInputProvider);
    }

    public ConfigSourceParser(ResourceInputProvider resourceInputProvider, boolean verifyOnly) {
        super(resourceInputProvider, verifyOnly);
    }

    public ConfigSourceParser(ResourceInputProvider resourceInputProvider, boolean verifyOnly, boolean explicitSequencing) {
        super(resourceInputProvider, verifyOnly, explicitSequencing);
    }

    @Override
    protected void constructSource(final String path, final Node src, final ModuleImpl parent) throws ParserException {
        final Map<String, Node> sourceMap = asMapping(src, new String[]{DEFINITIONS}, null);
        final SourceImpl source = parent.addConfigSource(path);

        final Map<String, Node> definitionsMap = asMapping(sourceMap.get(DEFINITIONS), null,
                DefinitionType.CONFIG_NAMES);

        for (String definitionName : definitionsMap.keySet()) {
            final Node definitionNode = definitionsMap.get(definitionName);
            switch (DefinitionType.valueOf(definitionName.toUpperCase())) {
                case NAMESPACE:
                    constructNamespaceDefinitions(definitionNode, source);
                    break;
                case CND:
                    constructNodeTypeDefinitions(definitionNode, source);
                    break;
                case CONFIG:
                    constructConfigDefinitions(definitionNode, source);
                    break;
                case WEBFILEBUNDLE:
                    constructWebFileBundleDefinition(definitionNode, source);
                    break;
            }
        }
    }

    private void constructNamespaceDefinitions(final Node src, final SourceImpl parent) throws ParserException {
        for (Node node : asSequence(src)) {
            final Map<String, Node> namespaceMap = asMapping(node, new String[]{PREFIX_KEY, URI_KEY}, null);
            final String prefix = asStringScalar(namespaceMap.get(PREFIX_KEY));
            final URI uri = asURIScalar(namespaceMap.get(URI_KEY));
            parent.addNamespaceDefinition(prefix, uri);
        }
    }

    private void constructNodeTypeDefinitions(final Node src, final SourceImpl parent) throws ParserException {
        for (Node node : asSequence(src)) {
            switch (node.getNodeId()) {
                case scalar:
                    final String cndString = asStringScalar(node);
                    parent.addNodeTypeDefinition(cndString, false);
                    break;
                case mapping:
                    final Map<String, Node> map = asMapping(node, new String[]{RESOURCE_KEY}, new String[0]);
                    final String resource = asResourcePathScalar(map.get(RESOURCE_KEY), parent, resourceInputProvider);
                    parent.addNodeTypeDefinition(resource, true);
                    break;
                default:
                    throw new ParserException("CND definition item must be a string or a map with key '"+RESOURCE_KEY+"'", node);
            }
        }
    }

    private void constructConfigDefinitions(final Node src, final SourceImpl parent) throws ParserException {
        for (NodeTuple nodeTuple : asTuples(src)) {
            final ConfigDefinitionImpl definition = parent.addConfigDefinition();
            final String key = asPathScalar(nodeTuple.getKeyNode(), true, false);
            constructDefinitionNode(key, nodeTuple.getValueNode(), definition);
        }
    }

    @Override
    protected void populateDefinitionNode(final DefinitionNodeImpl definitionNode, final Node node) throws ParserException {
        final List<NodeTuple> tuples = asTuples(node);
        for (NodeTuple tuple : tuples) {
            final String key = asStringScalar(tuple.getKeyNode());
            final Node tupleValue = tuple.getValueNode();
            if (key.equals(META_DELETE_KEY)) {
                if (!verifyOnly) {
                    if (tuples.size() > 1) {
                        throw new ParserException("Node cannot contain '"+META_DELETE_KEY+"' and other keys", node);
                    }
                }
                final boolean delete = asNodeDeleteValue(tupleValue);
                definitionNode.setDelete(delete);
            } else if (key.equals(META_ORDER_BEFORE_KEY)) {
                final String name = asNodeOrderBeforeValue(tupleValue);
                if (definitionNode.getName().equals(name)) {
                    throw new ParserException("Invalid "+META_ORDER_BEFORE_KEY+" targeting this node itself", node);
                }
                definitionNode.setOrderBefore(name);
            } else if (key.equals(META_IGNORE_REORDERED_CHILDREN)) {
                final Boolean ignoreReorderedChildren = (Boolean)constructValueFromScalar(tupleValue, ValueType.BOOLEAN).getObject();
                definitionNode.setIgnoreReorderedChildren(ignoreReorderedChildren);
            } else if (key.startsWith("/")) {
                final String name = key.substring(1);
                constructDefinitionNode(name, tupleValue, definitionNode);
            } else {
                constructDefinitionProperty(key, tupleValue, definitionNode);
            }
        }
    }

    private boolean asNodeDeleteValue(final Node node) throws ParserException {
        final ScalarNode scalar = asScalar(node);
        final Object object = scalarConstructor.constructScalarNode(scalar);
        if (!object.equals(true)) {
            throw new ParserException("Value for "+META_DELETE_KEY+" must be boolean value 'true'", node);
        }
        return true;
    }

    private String asNodeOrderBeforeValue(final Node node) throws ParserException {
        return asStringScalar(node);
    }

    private void constructWebFileBundleDefinition(final Node definitionNode, final SourceImpl source) throws ParserException {
        final List<Node> nodes = asSequence(definitionNode);
        for (Node node : nodes) {
            final String name = asStringScalar(node);
            for (Definition definition : source.getModifiableDefinitions()) {
                if (definition instanceof WebFileBundleDefinitionImpl) {
                    final WebFileBundleDefinitionImpl existingDefinition = (WebFileBundleDefinitionImpl) definition;
                    if (existingDefinition.getName().equals(name)) {
                        throw new ParserException("Duplicate web file bundle name '" + name + "'", node);
                    }
                }
            }
            source.addWebFileBundleDefinition(name);
        }
    }
}
