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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.onehippo.cm.api.model.PropertyOperation;
import org.onehippo.cm.api.model.PropertyType;
import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.impl.model.ConfigurationNodeImpl;
import org.onehippo.cm.impl.model.ConfigurationPropertyImpl;
import org.onehippo.cm.impl.model.ContentDefinitionImpl;
import org.onehippo.cm.impl.model.DefinitionNodeImpl;
import org.onehippo.cm.impl.model.DefinitionPropertyImpl;
import org.onehippo.cm.impl.model.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.onehippo.cm.api.model.PropertyOperation.ADD;
import static org.onehippo.cm.api.model.PropertyOperation.DELETE;
import static org.onehippo.cm.api.model.PropertyOperation.OVERRIDE;

class ConfigurationTreeBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationTreeBuilder.class);
    private final ConfigurationNodeImpl root = new ConfigurationNodeImpl();

    ConfigurationTreeBuilder() {
        root.setPath("/");
        root.setName("");
    }

    ConfigurationNodeImpl build() {
        // validation of the input and construction of the tree happens at "push time".

        pruneDeletedItems(root);
        return root;
    }

    void push(final ContentDefinitionImpl definition) {
        final ConfigurationNodeImpl rootForDefinition = getOrCreateRootForDefinition(definition);

        if (rootForDefinition != null) {
            mergeNode(rootForDefinition, definition.getModifiableNode());
        }
    }

    /**
     * Recursively merge a tree of {@link DefinitionNodeImpl}s and {@link DefinitionPropertyImpl}s
     * onto the tree of {@link ConfigurationNodeImpl}s and {@link ConfigurationPropertyImpl}s.
     */
    private void mergeNode(final ConfigurationNodeImpl node, final DefinitionNodeImpl definitionNode) {
        if (definitionNode.isDelete()) {
            markNodeAsDeletedBy(node, definitionNode);
            return;
        }

        for (DefinitionPropertyImpl property: definitionNode.getModifiableProperties().values()) {
            mergeProperty(node, property);
        }

        final Map<String, ConfigurationNodeImpl> children = node.getModifiableNodes();
        for (DefinitionNodeImpl definitionChild : definitionNode.getModifiableNodes().values()) {
            final String name = definitionChild.getName();
            final ConfigurationNodeImpl child;
            if (children.containsKey(name)) {
                child = children.get(name);
                if (child.isDeleted()) {
                    logger.warn("Trying to modify already deleted node '{}', skipping.", child.getPath());
                    continue;
                }
            } else {
                child = createChildNode(node, definitionChild.getName(), definitionChild);
            }
            mergeNode(child, definitionChild);
        }
    }

    private void markNodeAsDeletedBy(final ConfigurationNodeImpl node, final DefinitionNodeImpl definitionNode) {
        node.setDeleted(true);
        node.addDefinitionItem(definitionNode);
        node.clearNodes();
        node.clearProperties();
    }

    private ConfigurationNodeImpl getOrCreateRootForDefinition(final ContentDefinitionImpl definition) {
        final DefinitionNodeImpl definitionNode = definition.getModifiableNode();
        final String definitionRootPath = definitionNode.getPath();
        final String[] pathSegments = getPathSegments(definitionRootPath);
        int segmentsConsumed = 0;

        ConfigurationNodeImpl rootForDefinition = root;
        while (segmentsConsumed < pathSegments.length
                && rootForDefinition.getNodes().containsKey(pathSegments[segmentsConsumed])) {
            rootForDefinition = rootForDefinition.getModifiableNodes().get(pathSegments[segmentsConsumed]);
            segmentsConsumed++;
        }

        if (rootForDefinition.isDeleted()) {
            logger.warn("Content definition rooted at '{}' conflicts with deleted node at '{}', skipping.",
                    definitionNode.getPath(), rootForDefinition.getPath());
            return null;
        }

        if (pathSegments.length > segmentsConsumed + 1) {
            // this definition is rooted more than 1 node level deeper than a leaf node of the current tree.
            // that's unsupported, because it is likely to create models that cannot be persisted to JCR.
            final String culprit = ModelUtils.formatDefinition(definition);
            String msg = String.format("%s contains definition rooted at unreachable node '%s'. "
                    + "Closest ancestor is at '%s'.", culprit, definitionRootPath,
                      rootForDefinition.getPath());
            throw new IllegalStateException(msg);
        }

        if (pathSegments.length > segmentsConsumed) {
            rootForDefinition = createChildNode(rootForDefinition, pathSegments[segmentsConsumed], definition.getModifiableNode());
        } else {
            if (rootForDefinition == root && definitionNode.isDelete()) {
                throw new IllegalArgumentException("Deleting the root node is not supported.");
            }

            if (pathSegments.length > 0) {
                final ConfigurationNodeImpl parent = rootForDefinition.getModifiableParent();
                definitionNode.getOrderBefore()
                        .ifPresent(destChildName -> parent.orderBefore(pathSegments[pathSegments.length - 1], destChildName));
            }
        }

        return rootForDefinition;
    }

    private ConfigurationNodeImpl createChildNode(final ConfigurationNodeImpl parent, final String name,
                                                  final DefinitionNodeImpl definitionNode) {
        final ConfigurationNodeImpl node = new ConfigurationNodeImpl();
        final boolean parentIsRoot = parent.getParent() == null;

        if (definitionNode.isDelete()) {
            final String culprit = ModelUtils.formatDefinition(definitionNode.getDefinition());
            final String msg = String.format("Not yet created node '%s' has delete-flag set in '%s'.",
                    definitionNode.getPath(), culprit);
            throw new IllegalArgumentException(msg);
        }

        node.setName(name);
        node.setParent(parent);
        node.setPath((parentIsRoot ? "" : parent.getPath()) + "/" + name);
        node.addDefinitionItem(definitionNode);

        parent.addNode(name, node);

        definitionNode.getOrderBefore()
                .ifPresent(destChildName -> parent.orderBefore(name, destChildName));

        return node;
    }

    private void mergeProperty(final ConfigurationNodeImpl parent,
                               final DefinitionPropertyImpl definitionProperty) {
        final Map<String, ConfigurationPropertyImpl> properties = parent.getModifiableProperties();
        final String name = definitionProperty.getName();
        final PropertyOperation op = definitionProperty.getOperation();

        ConfigurationPropertyImpl property;
        if (properties.containsKey(name)) {
            property = properties.get(name);

            if (property.isDeleted()) {
                final String culprit = ModelUtils.formatDefinition(definitionProperty.getDefinition());
                logger.warn("Property '{}' defined in '{}' has already been deleted. This property is not re-created.",
                        property.getPath(), culprit);
            }

            if (op == DELETE) {
                property.setDeleted(true);
                property.addDefinitionItem(definitionProperty);
                return;
            }

            if (property.getType() != definitionProperty.getType()) {
                if (op == OVERRIDE) {
                    property.setType(definitionProperty.getType());
                    property.setValue(null);
                    property.setValues(null);
                } else {
                    final String culprit = ModelUtils.formatDefinition(definitionProperty.getDefinition());
                    final String msg = String.format("Property %s already exists with type %s, but type %s is requested in %s.",
                            property.getPath(), property.getType(), definitionProperty.getType(), culprit);
                    throw new IllegalStateException(msg);
                }
            }

            if (property.getValueType() != definitionProperty.getValueType()) {
                if (op == OVERRIDE) {
                    property.setValueType(definitionProperty.getValueType());
                    property.setValue(null);
                    property.setValues(null);
                } else {
                    final String culprit = ModelUtils.formatDefinition(definitionProperty.getDefinition());
                    final String msg = String.format("Property %s already exists with value type %s, but value type %s is requested in %s.",
                            property.getPath(), property.getValueType(), definitionProperty.getValueType(), culprit);
                    throw new IllegalStateException(msg);
                }
            }

            requireOverrideOperationForPrimaryType(definitionProperty, property, op == OVERRIDE);
            requireOverrideOperationForMixinTypes(definitionProperty, property, op);
        } else {
            if (op == DELETE) {
                final String culprit = ModelUtils.formatDefinition(definitionProperty.getDefinition());
                final String msg = String.format("Not yet created property '%s' specifies delete operation in '%s'.",
                        definitionProperty.getPath(), culprit);
                throw new IllegalArgumentException(msg);
            }

            // create new property
            property = new ConfigurationPropertyImpl();

            property.setName(name);
            property.setParent(parent);
            property.setPath(definitionProperty.getPath());
            property.setType(definitionProperty.getType());
            property.setValueType(definitionProperty.getValueType());
        }

        warnIfValuesAreEqual(definitionProperty, property);
        property.addDefinitionItem(definitionProperty);
        if (PropertyType.SINGLE == definitionProperty.getType()) {
            property.setValue(definitionProperty.getValue());
        } else {
            if (op == ADD) {
                addValues(definitionProperty, property);
            } else {
                property.setValues(definitionProperty.getValues());
            }
        }

        parent.addProperty(name, property);
    }

    private void requireOverrideOperationForPrimaryType(final DefinitionPropertyImpl definitionProperty,
                                                        final ConfigurationPropertyImpl property,
                                                        final boolean isOverride) {
        if (property.getName().equals(JCR_PRIMARYTYPE)
                && !property.getValue().getString().equals(definitionProperty.getValue().getString())
                && !isOverride) {
            final String culprit = ModelUtils.formatDefinition(definitionProperty.getDefinition());
            final String msg = String.format("Property %s is already defined on node %s, but change is requested in %s. "
                            + "Use 'operation: override' if you really intend to change the value of this property.",
                    JCR_PRIMARYTYPE, property.getParent().getPath(), culprit);
            throw new IllegalStateException(msg);
        }
    }

    private void requireOverrideOperationForMixinTypes(final DefinitionPropertyImpl definitionProperty,
                                                       final ConfigurationPropertyImpl property,
                                                       final PropertyOperation op) {
        if (property.getName().equals(JCR_MIXINTYPES) && op != ADD) {
            final List<String> replacedMixins = Arrays.stream(definitionProperty.getValues())
                    .map(Value::getString)
                    .collect(Collectors.toList());
            final List<String> missingMixins = Arrays.stream(property.getValues())
                    .map(Value::getString)
                    .filter(mixin -> !replacedMixins.contains(mixin))
                    .collect(Collectors.toList());

            if (missingMixins.size() > 0 && op != OVERRIDE) {
                final String culprit = ModelUtils.formatDefinition(definitionProperty.getDefinition());
                final String msg = String.format("Property %s is already defined on node %s, and replace operation of "
                                + "%s would remove values %s. Use 'operation: override' if you really intend to remove "
                                + "these values.",
                        JCR_MIXINTYPES, property.getParent().getPath(), culprit, missingMixins.toString());
                throw new IllegalStateException(msg);
            }
        }
    }

    private void addValues(final DefinitionPropertyImpl definitionProperty, final ConfigurationPropertyImpl property) {
        // TODO: need to handle PropertyType.SET?

        final Value[] existingValues = property.getValues();
        if (existingValues == null) {
            final String culprit = ModelUtils.formatDefinition(definitionProperty.getDefinition());
            logger.warn("Property '{}' defined in '{}' claims to ADD values, but no values are present. Applying default behaviour.",
                    definitionProperty.getPath(), culprit);
            property.setValues(definitionProperty.getValues());
        } else {
            List<Value> values = Arrays.stream(existingValues).collect(Collectors.toList());
            values.addAll(Arrays.asList(definitionProperty.getValues()));
            property.setValues(values.toArray(new Value[values.size()]));
        }
    }

    private void warnIfValuesAreEqual(final DefinitionPropertyImpl definitionProperty,
                                      final ConfigurationPropertyImpl property) {
        if (PropertyType.SINGLE == property.getType()) {
            final Value existingValue = property.getValue();
            if (existingValue != null) {
                if (definitionProperty.getValue().equals(property.getValue())) {
                    final String culprit = ModelUtils.formatDefinition(definitionProperty.getDefinition());
                    logger.warn("Property '{}' defined in '{}' specifies value equivalent to existing property.",
                            definitionProperty.getPath(), culprit);
                }
            }
        } else {
            final Value[] existingValues = property.getValues();
            if (existingValues != null) {
                final Value[] definitionValues = definitionProperty.getValues();
                if (existingValues.length == definitionValues.length) {
                    for (int i = 0; i < existingValues.length; i++) {
                        if (!existingValues[i].equals(definitionValues[i])) {
                            return;
                        }
                    }
                    final String culprit = ModelUtils.formatDefinition(definitionProperty.getDefinition());
                    logger.warn("Property '{}' defined in '{}' specifies values equivalent to existing property.",
                            definitionProperty.getPath(), culprit);
                }
            }
        }
    }

    private String[] getPathSegments(final String path) {
        if ("/".equals(path)) {
            return new String[0];
        }

        return path.substring(1).split("/");
    }

    private void pruneDeletedItems(final ConfigurationNodeImpl node) {
        if (node.isDeleted()) {
            node.getModifiableParent().removeNode(node.getName());
            return;
        }

        final Map<String, ConfigurationPropertyImpl> propertyMap = node.getModifiableProperties();
        final List<String> deletedProperties = propertyMap.keySet().stream()
                .filter(propertyName -> propertyMap.get(propertyName).isDeleted())
                .collect(Collectors.toList());
        deletedProperties.forEach(propertyMap::remove);

        final Map<String, ConfigurationNodeImpl> childMap = node.getModifiableNodes();
        final List<String> children = childMap.keySet().stream().collect(Collectors.toList());
        children.forEach(name -> pruneDeletedItems(childMap.get(name)));
    }

}
