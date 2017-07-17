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
package org.onehippo.cm.model.parser;

import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.model.definition.ActionItem;
import org.onehippo.cm.model.definition.ActionType;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.definition.ActionItemImpl;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;

import static org.onehippo.cm.model.Constants.ACTION_LISTS_NODE;

/**
 * Parse an action list file, like for example
 *
 * action-lists:
 * - 1.0:
 * /content/path1: reload
 * /content/pathX: reload
 * - 1.1:
 * /content/path2: reload
 * - 1.2:
 * /content/path3: delete
 */
public class ActionListParser extends AbstractBaseParser {

    public ActionListParser() {
        super(false);
    }

    public void parse(final InputStream inputStream, final String location, final ModuleImpl module) throws ParserException {

        final Node node = composeYamlNode(inputStream, location);
        final Map<String, Node> sourceMap = asMapping(node, new String[]{ACTION_LISTS_NODE}, null);
        for (final Node versionNode : asSequence(sourceMap.get(ACTION_LISTS_NODE))) {

            final MappingNode mappingNode = (MappingNode) versionNode;
            final NodeTuple nodeTuple = mappingNode.getValue().get(0);

            final String strVersion = asScalar(nodeTuple.getKeyNode()).getValue();
            final Double version = Double.parseDouble(strVersion);

            final Set<ActionItem> actionItems = collectActionItems(nodeTuple.getValueNode());
            module.getActionsMap().put(version, actionItems);
        }
    }

    private Set<ActionItem> collectActionItems(final Node node) throws ParserException {
        final Set<ActionItem> actionItems = new LinkedHashSet<>();
        for (NodeTuple tuple : asTuples(node)) {
            final String path = asPathScalar(tuple.getKeyNode(), true, false);
            final ActionItem actionItem = asActionItem(tuple.getValueNode(), path);
            if (!actionItems.add(actionItem)) {
                throw new RuntimeException(String.format("Duplicate items are not allowed: %s", actionItem));
            }
        }
        return actionItems;
    }

    private ActionItem asActionItem(final Node node, final String path) throws ParserException {
        String action = asStringScalar(node);
        ActionType type = ActionType.valueOf(StringUtils.upperCase(action));
        if (type == ActionType.APPEND) {
            throw new RuntimeException("APPEND action type can't be specified in action lists file");
        }
        return new ActionItemImpl(path, type);
    }
}
