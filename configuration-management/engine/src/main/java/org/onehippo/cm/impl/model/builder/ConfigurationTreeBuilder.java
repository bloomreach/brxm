/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cm.impl.model.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.onehippo.cm.api.model.ConfigurationProperty;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.PropertyType;
import org.onehippo.cm.impl.model.ConfigurationNodeImpl;
import org.onehippo.cm.impl.model.ConfigurationPropertyImpl;
import org.onehippo.cm.impl.model.ContentDefinitionImpl;
import org.onehippo.cm.impl.model.DefinitionItemImpl;
import org.onehippo.cm.impl.model.DefinitionNodeImpl;
import org.onehippo.cm.impl.model.DefinitionPropertyImpl;

public class ConfigurationTreeBuilder {

    public void addDefinition(final ContentDefinitionImpl definition, ConfigurationNodeImpl root) {
        final ConfigurationNodeImpl definitionRoot = getOrCreateDefinitionRoot(definition, root);

        augment(definitionRoot, definition.getModifiableNode());
    }

    private void augment(final ConfigurationNodeImpl node, final DefinitionNodeImpl definitionNode) {
        for (Map.Entry<String, DefinitionPropertyImpl> propertyEntry : definitionNode.getModifiableProperties().entrySet()) {
            createProperty(node, propertyEntry);
        }

        final Map<String, ConfigurationNodeImpl> children = node.getModifiableNodes();
        for (Map.Entry<String, DefinitionNodeImpl> nodeEntry : definitionNode.getModifiableNodes().entrySet()) {
            final String name = nodeEntry.getKey();
            final DefinitionNodeImpl definitionChild = nodeEntry.getValue();
            ConfigurationNodeImpl child;
            if (children.containsKey(name)) {
                child = children.get(name);
            } else {
                child = createChildNode(node, definitionChild);
            }
            augment(child, definitionChild);
        }
    }

    private ConfigurationNodeImpl getOrCreateDefinitionRoot(final ContentDefinition definition,
                                                            final ConfigurationNodeImpl root) {
        final String definitionRootPath = definition.getNode().getPath();
        final String[] pathSegments = getPathSegments(definitionRootPath);
        int segmentsConsumed = 0;

        ConfigurationNodeImpl definitionRoot = root;
        while (segmentsConsumed < pathSegments.length
                && definitionRoot.getNodes().containsKey(pathSegments[segmentsConsumed])) {
            definitionRoot = definitionRoot.getModifiableNodes().get(pathSegments[segmentsConsumed]);
            segmentsConsumed++;
        }

        if (pathSegments.length > segmentsConsumed + 1) {
            // this definition is rooted more than 1 node level deeper than a current leaf node of the model.
            // that's unsupported, because it is likely to create models that cannot be persisted to JCR.
            String msg = String.format("Source '%s' contains definition rooted at unreachable node '%s'. "
                    + "Closest ancestor is at '%s'.", definition.getSource().getPath(), definitionRootPath,
                      definitionRoot.getPath());
            throw new IllegalStateException(msg);
        }

        if (pathSegments.length > segmentsConsumed) {
            definitionRoot = createChildNode(definitionRoot, pathSegments[segmentsConsumed]);
        }

        return definitionRoot;
    }

    private ConfigurationNodeImpl createChildNode(final ConfigurationNodeImpl parent,
                                                  final DefinitionItemImpl definitionNode) {
        final ConfigurationNodeImpl node = createChildNode(parent, definitionNode.getName());
        node.addDefinitionItem(definitionNode);
        return node;
    }

    private ConfigurationNodeImpl createChildNode(final ConfigurationNodeImpl parent, final String name) {
        final ConfigurationNodeImpl node = new ConfigurationNodeImpl();
        final boolean parentIsRoot = parent.getParent() == null;

        node.setName(name);
        node.setParent(parent);
        node.setPath((parentIsRoot ? "" : parent.getPath()) + "/" + name);

        parent.addNode(name, node);

        return node;
    }

    private ConfigurationPropertyImpl createProperty(final ConfigurationNodeImpl parent,
                                                     final Map.Entry<String, DefinitionPropertyImpl> entry) {
        final Map<String, ConfigurationProperty> properties = parent.getProperties();
        final String name = entry.getKey();
        if (properties.containsKey(name)) {
            final String msg = String.format("Node '%s' already has property '%s'.", parent.getPath(), name);
            throw new IllegalStateException(msg);
        }

        final DefinitionPropertyImpl definitionProperty = entry.getValue();
        final ConfigurationPropertyImpl property = new ConfigurationPropertyImpl();

        property.setName(name);
        property.setParent(parent);
        property.setPath(parent.getPath() + "/" + name);
        property.addDefinitionItem(definitionProperty);
        property.setType(definitionProperty.getType());
        property.setValueType(definitionProperty.getValueType());
        if (PropertyType.SINGLE == definitionProperty.getType()) {
            property.setValue(definitionProperty.getValue());
        } else {
            property.setValues(definitionProperty.getValues());
        }

        parent.addProperty(name, property);

        return property;
    }

    private String[] getPathSegments(final String path) {
        if ("/".equals(path)) {
            return new String[0];
        }

        List<String> segments = new ArrayList<>();
        int segmentStart = 1;
        int segmentEnd = nextUnescapedSlash(path, segmentStart);
        while (segmentEnd > segmentStart) {
            segments.add(path.substring(segmentStart, segmentEnd));
            if (segmentEnd >= path.length()) {
                break;
            }
            segmentStart = segmentEnd + 1;
            segmentEnd = nextUnescapedSlash(path, segmentStart);
        }
        return segments.toArray(new String[segments.size()]);
    }

    // TODO: collocate below logic with path validation from AbstractBaseParser - encoding/escaping is TBD.
    private int nextUnescapedSlash(final String path, final int offset) {
        boolean escaped = false;
        for (int i = offset + 1; i < path.length(); i++) {
            switch (path.charAt(i)) {
                case '/':
                    if (!escaped) {
                        return i;
                    }
                    escaped = false;
                    break;
                case '\\':
                    escaped = !escaped;
                    break;
                default:
                    escaped = false;
                    break;
            }
        }
        return path.length();
    }
}
