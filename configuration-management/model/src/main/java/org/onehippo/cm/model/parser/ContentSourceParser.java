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
package org.onehippo.cm.model.parser;

import java.util.List;
import java.util.Map;

import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.source.ContentSourceImpl;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.tree.DefinitionPropertyImpl;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.source.ResourceInputProvider;
import org.onehippo.cm.model.tree.ValueType;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;

import static org.onehippo.cm.model.Constants.META_CATEGORY_KEY;
import static org.onehippo.cm.model.Constants.META_KEY_PREFIX;
import static org.onehippo.cm.model.Constants.META_ORDER_BEFORE_KEY;
import static org.onehippo.cm.model.Constants.OPERATION_KEY;
import static org.onehippo.cm.model.Constants.PATH_KEY;
import static org.onehippo.cm.model.Constants.RESOURCE_KEY;
import static org.onehippo.cm.model.Constants.TYPE_KEY;
import static org.onehippo.cm.model.Constants.VALUE_KEY;

public class ContentSourceParser extends SourceParser {
    public ContentSourceParser(ResourceInputProvider resourceInputProvider) {
        super(resourceInputProvider);
    }

    public ContentSourceParser(ResourceInputProvider resourceInputProvider, boolean verifyOnly) {
        super(resourceInputProvider, verifyOnly);
    }

    public ContentSourceParser(ResourceInputProvider resourceInputProvider, boolean verifyOnly, boolean explicitSequencing) {
        super(resourceInputProvider, verifyOnly, explicitSequencing);
    }

    @Override
    protected void constructSource(final String path, final Node src, final ModuleImpl parent) throws ParserException {
        final List<NodeTuple> tuples = asTuples(src);
        if (tuples.size() > 1) {
            throw new ParserException("Content definitions can only contain single root node");
        }

        final ContentSourceImpl source = parent.addContentSource(path);
        final ContentDefinitionImpl definition = source.addContentDefinition();
        final JcrPath key = asPathScalar(tuples.get(0).getKeyNode(), true, true);
        constructDefinitionNode(key, tuples.get(0).getValueNode(), definition);

        source.markUnchanged();
    }

    @Override
    protected void populateDefinitionNode(final DefinitionNodeImpl definitionNode, final Node node) throws ParserException {
        populateDefinitionNode(definitionNode, node, asTuples(node));
    }

    protected void populateDefinitionNode(final DefinitionNodeImpl definitionNode, final Node node, final List<NodeTuple> tuples) throws ParserException {
        for (NodeTuple tuple : tuples) {
            final String key = asStringScalar(tuple.getKeyNode());
            final Node tupleValue = tuple.getValueNode();

            if (key.equals(META_ORDER_BEFORE_KEY)) {
                final String name = asNodeOrderBeforeValue(tupleValue);
                if (!definitionNode.isRoot()) {
                    throw new ParserException(META_ORDER_BEFORE_KEY + " is not allowed at non root content definition", node);
                } else if (definitionNode.getName().equals(name)) {
                    throw new ParserException("Invalid " + META_ORDER_BEFORE_KEY + " targeting this node itself", node);
                }
                definitionNode.setOrderBefore(name);
            } else if (key.startsWith(META_KEY_PREFIX)) {
                throw new ParserException("Content node cannot contain key '" + key + "'", node);
            } else if (key.startsWith("/")) {
                final String name = key.substring(1);
                constructDefinitionNode(name, tupleValue, definitionNode);
            } else {
                constructDefinitionProperty(key, tupleValue, definitionNode);
            }
        }
    }

    @Override
    protected DefinitionPropertyImpl constructDefinitionPropertyFromMap(final String name, final Node value, final ValueType defaultValueType, final DefinitionNodeImpl parent) throws ParserException {
        final Map<String, Node> map = asMapping(value, new String[0],
                new String[]{META_CATEGORY_KEY,OPERATION_KEY,TYPE_KEY,VALUE_KEY,RESOURCE_KEY,PATH_KEY});

        if (map.keySet().contains(META_CATEGORY_KEY)) {
            throw new ParserException("Key '" + META_CATEGORY_KEY + "' is not allowed for content definition", value);
        }
        if (map.keySet().contains(OPERATION_KEY)) {
            throw new ParserException("Key '" + OPERATION_KEY + "' is not allowed for content definition", value);
        }

        return super.constructDefinitionPropertyFromMap(name, value, defaultValueType, parent);
    }
}
