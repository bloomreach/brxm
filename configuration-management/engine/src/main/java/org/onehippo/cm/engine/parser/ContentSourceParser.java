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

import java.util.List;
import java.util.Map;

import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.ValueType;
import org.onehippo.cm.impl.model.ContentDefinitionImpl;
import org.onehippo.cm.impl.model.ContentSourceImpl;
import org.onehippo.cm.impl.model.DefinitionNodeImpl;
import org.onehippo.cm.impl.model.DefinitionPropertyImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;

import static org.onehippo.cm.engine.Constants.META_KEY_PREFIX;
import static org.onehippo.cm.engine.Constants.OPERATION_KEY;
import static org.onehippo.cm.engine.Constants.PATH_KEY;
import static org.onehippo.cm.engine.Constants.RESOURCE_KEY;
import static org.onehippo.cm.engine.Constants.TYPE_KEY;
import static org.onehippo.cm.engine.Constants.VALUE_KEY;

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

        final ContentSourceImpl source = (ContentSourceImpl) parent.addContentSource(path);
        final Map<String, Node> contentNode = asMapping(src, null, null);

        final ContentDefinitionImpl definition = source.addContentDefinition();
        final String key = validatePath(contentNode.keySet().iterator().next(), true);
        constructDefinitionNode(key, contentNode.values().iterator().next(), definition);
    }

    private void constructContentDefinitions(final Node src, final ContentSourceImpl parent) throws ParserException {
        for (NodeTuple nodeTuple : asTuples(src)) {
            final ContentDefinitionImpl definition = parent.addContentDefinition();
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

            if (key.startsWith(META_KEY_PREFIX)) {
                throw new ParserException("Content node cannot contain key '" + key + "'", node);
            }

            if (key.startsWith("/")) {
                final String name = key.substring(1);
                constructDefinitionNode(name, tupleValue, definitionNode);
            } else {
                constructDefinitionProperty(key, tupleValue, definitionNode);
            }
        }
    }

    @Override
    DefinitionPropertyImpl constructDefinitionPropertyFromMap(final String name, final Node value, final ValueType defaultValueType, final DefinitionNodeImpl parent) throws ParserException {
        final Map<String, Node> map = asMapping(value, new String[0],
                new String[]{OPERATION_KEY,TYPE_KEY,VALUE_KEY,RESOURCE_KEY,PATH_KEY});
        if (map.keySet().contains(OPERATION_KEY)) {
            throw new ParserException("Operation key is not allowed for content definition", value);
        }
        return super.constructDefinitionPropertyFromMap(name, value, defaultValueType, parent);
    }
}
