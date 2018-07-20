/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cm.model.impl.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.onehippo.cm.model.impl.definition.ConfigDefinitionImpl;
import org.onehippo.cm.model.impl.definition.TreeDefinitionImpl;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.path.JcrPathSegment;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.tree.ConfigurationItemCategory;
import org.onehippo.cm.model.tree.PropertyKind;
import org.onehippo.cm.model.tree.PropertyOperation;
import org.onehippo.cm.model.tree.ValueType;
import org.onehippo.cm.model.util.SnsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.JCR_UUID;
import static org.apache.jackrabbit.JcrConstants.MIX_REFERENCEABLE;
import static org.onehippo.cm.model.Constants.META_IGNORE_REORDERED_CHILDREN;
import static org.onehippo.cm.model.Constants.META_ORDER_BEFORE_FIRST;
import static org.onehippo.cm.model.tree.PropertyOperation.ADD;
import static org.onehippo.cm.model.tree.PropertyOperation.DELETE;
import static org.onehippo.cm.model.tree.PropertyOperation.OVERRIDE;
import static org.onehippo.cm.model.tree.PropertyOperation.REPLACE;
import static org.onehippo.cm.model.util.SnsUtils.createIndexedName;

public class ConfigurationTreeBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationTreeBuilder.class);
    private static final String REP_ROOT_NT = "rep:root";

    private final ConfigurationNodeImpl root;

    private final Map<JcrPath, DefinitionNodeImpl> delayedOrdering = new LinkedHashMap<>();

    private final Map<JcrPath, ConfigurationNodeImpl> deletedNodes = new HashMap<>();
    private final Map<JcrPath, ConfigurationPropertyImpl> deletedProperties = new HashMap<>();

    public ConfigurationTreeBuilder() {
        root = new ConfigurationNodeImpl();
        root.setName(JcrPaths.ROOT_NAME);
        root.setResidualNodeCategory(ConfigurationItemCategory.SYSTEM);
        root.setIgnoreReorderedChildren(true);

        // add required jcr:uuid:
        final ConfigurationPropertyImpl uuidProperty = new ConfigurationPropertyImpl();
        uuidProperty.setName(JCR_UUID);
        uuidProperty.setParent(root);
        uuidProperty.setKind(PropertyKind.SINGLE);
        uuidProperty.setValueType(ValueType.STRING);
        uuidProperty.setValue(new ValueImpl("cafebabe-cafe-babe-cafe-babecafebabe", ValueType.STRING, false, false));
        root.addProperty(JCR_UUID, uuidProperty);

        // add required jcr:primaryType: rep:root
        final ConfigurationPropertyImpl primaryTypeProperty = new ConfigurationPropertyImpl();
        primaryTypeProperty.setName(JCR_PRIMARYTYPE);
        primaryTypeProperty.setParent(root);
        primaryTypeProperty.setKind(PropertyKind.SINGLE);
        primaryTypeProperty.setValueType(ValueType.NAME);
        primaryTypeProperty.setValue(new ValueImpl(REP_ROOT_NT, ValueType.NAME, false, false));
        root.addProperty(JCR_PRIMARYTYPE, primaryTypeProperty);

        // add required jcr:mixinTypes: mix:referenceable
        final ConfigurationPropertyImpl mixinTypesProperty = new ConfigurationPropertyImpl();
        mixinTypesProperty.setName(JCR_MIXINTYPES);
        mixinTypesProperty.setParent(root);
        mixinTypesProperty.setKind(PropertyKind.LIST);
        mixinTypesProperty.setValueType(ValueType.NAME);
        mixinTypesProperty.setValues(Collections.singletonList(new ValueImpl(MIX_REFERENCEABLE, ValueType.NAME, false, false)));
        root.addProperty(JCR_MIXINTYPES, mixinTypesProperty);
    }

    /**
     * Constructor for use when incrementally updating an existing config tree.
     * @param root the existing root node of a previously-built ConfigurationItem tree
     */
    public ConfigurationTreeBuilder(final ConfigurationNodeImpl root) {
        this.root = root;
    }

    /**
     * Call after all calls to {@link #push(ConfigDefinitionImpl)} have been completed for a particular Module.
     * This checks for error conditions that must be validated in the context of a complete set of a Module's definitions.
     * (Currently, this includes only .meta:order-before directives that may span multiple source files.
     * @return this
     */
    public ConfigurationTreeBuilder finishModule() {
        if (!delayedOrdering.isEmpty()) {
            // report on all errors, not just the first one
            List<String> msgs = new ArrayList<>();
            for (final DefinitionNodeImpl definitionNode : delayedOrdering.values()) {
                msgs.add(String.format("Invalid orderBefore: '%s' for node '%s' defined in '%s': no sibling named '%s'.",
                        definitionNode.getOrderBefore(), definitionNode.getJcrPath(), definitionNode.getOrigin(), definitionNode.getOrderBefore()));
            }
            throw new IllegalArgumentException(String.join("\n", msgs));
        }
        return this;
    }

    public ConfigurationNodeImpl build() {
        // validation of the input and construction of the tree happens at "push time".

        pruneDeletedItems(root);
        return root;
    }

    public ConfigurationTreeBuilder push(final ConfigDefinitionImpl definition) {
        final ConfigurationNodeImpl rootForDefinition = getOrCreateRootForDefinition(definition);

        if (rootForDefinition != null) {
            mergeNode(rootForDefinition, definition.getNode());
        }
        return this;
    }


    public Map<JcrPath, ConfigurationNodeImpl> getDeletedNodes() {
        return deletedNodes;
    }

    public Map<JcrPath, ConfigurationPropertyImpl> getDeletedProperties() {
        return deletedProperties;
    }

    /**
     * Recursively merge a tree of {@link DefinitionNodeImpl}s and {@link DefinitionPropertyImpl}s
     * onto the tree of {@link ConfigurationNodeImpl}s and {@link ConfigurationPropertyImpl}s.
     */
    public void mergeNode(final ConfigurationNodeImpl node, final DefinitionNodeImpl definitionNode) {

        if (definitionNode.isDeletedAndEmpty()) {

            if (node.getParent() == null || !node.getParent().isDeleted()) {
                deletedNodes.put(node.getJcrPath(), node);
            }

            final String indexedName = createIndexedName(definitionNode.getName());
            if (SnsUtils.hasSns(indexedName, node.getParent().getNodeNames())) {
                node.addDefinition(definitionNode);
                node.getParent().removeNode(indexedName, true);
            } else {
                final ConfigurationNodeImpl parentNode = node.getParent();
                parentNode.removeNode(definitionNode.getName(), false);
                //create fake delete node here, it will be pruned at the end
                final ConfigurationNodeImpl deleteNode = new ConfigurationNodeImpl();

                node.getDefinitions().forEach(deleteNode::addDefinition);
                deleteNode.addDefinition(definitionNode);
                deleteNode.setName(definitionNode.getName());
                parentNode.addNode(definitionNode.getName(), deleteNode);
                deleteNode.setParent(parentNode);
                markNodeAsDeletedBy(deleteNode, definitionNode);
            }
            return;
        } else if (definitionNode.isDelete() && !node.hasNoJcrNodesOrProperties()) {
            String msg = String.format("%s: Trying to delete AND merge node %s defined before by %s.",
                    definitionNode.getOrigin(), definitionNode.getJcrPath(),
                    // exclude def currently being processed from origin
                    node.getOrigin(definitionNode));
            throw new IllegalArgumentException(msg);
        }

        if (definitionNode.getCategory() != null) {
            if (definitionNode.getCategory() == ConfigurationItemCategory.CONFIG) {
                node.getParent().clearChildNodeCategorySettings(node.getName());
            } else {
                keepOnlyFirstSns(node.getParent(), node.getName());
                markNodeAsDeletedBy(node, definitionNode);
                node.getParent().setChildNodeCategorySettings(node.getName(), definitionNode.getCategory(), definitionNode);
                return;
            }
        }

        if (definitionNode.getResidualChildNodeCategory() != null) {
            node.setResidualNodeCategory(definitionNode.getResidualChildNodeCategory());
            node.addDefinition(definitionNode);
        }

        if (definitionNode.getIgnoreReorderedChildren() != null) {
            if (node.getIgnoreReorderedChildren() != null || definitionNode.getIgnoreReorderedChildren() == node.getIgnoreReorderedChildren()) {
                if (definitionNode.getIgnoreReorderedChildren() == node.getIgnoreReorderedChildren()) {
                    logger.warn("Redundant '{}: {}' for node '{}' defined in '{}'", META_IGNORE_REORDERED_CHILDREN,
                            node.getIgnoreReorderedChildren(), node.getJcrPath(), definitionNode.getOrigin());
                } else {
                    logger.warn("Overriding '{}' for node '{}' defined in '{}' which was {} before.",
                            META_IGNORE_REORDERED_CHILDREN, node.getJcrPath(), definitionNode.getOrigin(),
                            node.getIgnoreReorderedChildren());
                }
            }
            node.setIgnoreReorderedChildren(definitionNode.getIgnoreReorderedChildren());
            node.addDefinition(definitionNode);
        }

        final ConfigurationNodeImpl parent = node.getParent();
        if (parent != null && definitionNode.getOrderBefore() != null) {
            final String orderBefore = definitionNode.getOrderBefore();
            if (parent.getIgnoreReorderedChildren() != null && parent.getIgnoreReorderedChildren()) {
                logger.warn("Potential unnecessary orderBefore: '{}' for node '{}' defined in '{}': parent '{}' already configured with '{}: true'",
                        orderBefore, node.getJcrPath(), definitionNode.getOrigin(), parent.getJcrPath(), META_IGNORE_REORDERED_CHILDREN);
            }
            final boolean orderFirst = META_ORDER_BEFORE_FIRST.equals(orderBefore);
            final String orderBeforeIndexedName = createIndexedName(orderBefore);
            if (!orderFirst) {
                if (node.getName().equals(orderBeforeIndexedName)) {
                    final String msg = String.format("Invalid orderBefore: '%s' for node '%s' defined in '%s': targeting this node itself.",
                            orderBeforeIndexedName, node.getJcrPath(), definitionNode.getOrigin());
                    throw new IllegalArgumentException(msg);
                }
                if (parent.getNode(orderBeforeIndexedName) == null) {
                    // delay reporting an error for a missing sibling until after the module is done processing
                    delayedOrdering.put(definitionNode.getJcrPath().resolveSibling(orderBeforeIndexedName).forceIndices(),
                            definitionNode);
                }
            }
            applyNodeOrdering(node, definitionNode, parent, orderBefore, orderFirst, orderBeforeIndexedName);
            node.addDefinition(definitionNode);
        }

        final JcrPath indexedPath = definitionNode.getJcrPath().forceIndices();
        if (delayedOrdering.containsKey(indexedPath)) {
            // this node was referenced in a delayed ordering, so we need to apply that ordering now
            // apply node ordering from the perspective of the other, saved node
            final DefinitionNodeImpl otherDefNode = delayedOrdering.remove(indexedPath);
            final ConfigurationNodeImpl otherNode = parent.getNode(createIndexedName(otherDefNode.getName()));
            applyNodeOrdering(otherNode, otherDefNode, parent, otherDefNode.getOrderBefore(), false,
                    createIndexedName(otherDefNode.getOrderBefore()));
        }

        for (DefinitionPropertyImpl property: definitionNode.getModifiableProperties().values()) {
            mergeProperty(node, property);
        }

        requirePrimaryType(node, definitionNode);

        for (DefinitionNodeImpl definitionChild : definitionNode.getModifiableNodes().values()) {
            final String indexedName = createIndexedName(definitionChild.getName());
            ConfigurationNodeImpl child = node.getNode(indexedName);
            if (child != null) {
                if (child.isDeleted()) {
                    logger.warn("{} tries to modify already deleted node '{}', skipping.",
                            child.getOrigin(), child.getJcrPath());
                    continue;
                }
            } else {
                if (isAndRemainsNonConfigurationNode(node, indexedName, definitionChild.getCategory())) {
                    logger.warn("{} tries to modify non-configuration node '{}', skipping.",
                            definitionChild.getOrigin(), definitionChild.getJcrPath());
                    continue;
                }
                child = createChildNode(node, indexedName, definitionChild);
                if (child == null) {
                    continue;
                }
            }
            mergeNode(child, definitionChild);
        }
    }

    private void applyNodeOrdering(final ConfigurationNodeImpl node, final DefinitionNodeImpl definitionNode, final ConfigurationNodeImpl parent, final String orderBefore, final boolean orderFirst, final String orderBeforeIndexedName) {
        boolean first = true;
        boolean prevIsSrc = false;
        for (final String name : parent.getNodeNames()) {
            if (name.equals(node.getName())) {
                // current == src
                if (first && orderFirst) {
                    // src already first
                    logger.warn("Unnecessary orderBefore: '' (first) for node '{}' defined in '{}': already first child of '{}'.",
                            node.getJcrPath(), definitionNode.getOrigin(), parent.getJcrPath());
                    break;
                }
                // track src for next loop, once
                prevIsSrc = true;
            } else if (orderFirst) {
                // current != src != first
                parent.orderBefore(node.getName(), name);
                break;
            } else if (name.equals(orderBeforeIndexedName)) {
                // found dest: only reorder if prev != src
                if (prevIsSrc) {
                    // previous was src, current is dest: already is right order
                    logger.warn("Unnecessary orderBefore: '{}' for node '{}' defined in '{}': already ordered before sibling '{}'.",
                            orderBefore, node.getJcrPath(), definitionNode.getOrigin(), orderBeforeIndexedName);
                } else {
                    // dest < src: reorder
                    parent.orderBefore(node.getName(), orderBeforeIndexedName);
                }
                break;
            } else {
                prevIsSrc = false;
            }
            first = false;
        }
    }

    private void keepOnlyFirstSns(final ConfigurationNodeImpl node, final String indexedName) {
        if (SnsUtils.hasSns(indexedName, node.getNodeNames())) {
            final JcrPathSegment nameAndIndex = JcrPaths.getSegment(indexedName);
            final List<String> namesToDelete = new ArrayList<>();
            for (String siblingIndexedName : node.getNodeNames()) {
                final JcrPathSegment siblingNameAndIndex = JcrPaths.getSegment(siblingIndexedName);
                if (siblingNameAndIndex.getName().equals(nameAndIndex.getName()) && siblingNameAndIndex.getIndex() > 1) {
                    namesToDelete.add(siblingIndexedName);
                }
            }
            for (String nameToDelete : namesToDelete) {
                node.removeNode(nameToDelete, false);
            }
        }
    }

    private boolean isAndRemainsNonConfigurationNode(final ConfigurationNodeImpl node,
                                                     final String indexedChildNodeName,
                                                     final ConfigurationItemCategory override) {
        final Pair<ConfigurationItemCategory, DefinitionItemImpl> categoryAndDefinition =
                node.getChildNodeCategorySettings(SnsUtils.getUnindexedName(indexedChildNodeName));

        return categoryAndDefinition != null
                && categoryAndDefinition.getLeft() != ConfigurationItemCategory.CONFIG
                && override != ConfigurationItemCategory.CONFIG;
    }

    // used by DefinitionMergeService to update model in-place
    public ConfigurationTreeBuilder markNodeAsDeletedBy(final ConfigurationNodeImpl node, final DefinitionNodeImpl definitionNode) {
        node.setDeleted(true);
        node.addDefinition(definitionNode);
        node.clearNodes();
        node.clearProperties();
        node.getParent().clearChildNodeCategorySettings(node.getName());
        return this;
    }

    private ConfigurationNodeImpl getOrCreateRootForDefinition(final ConfigDefinitionImpl definition) {
        final DefinitionNodeImpl definitionNode = definition.getNode();
        final JcrPath definitionRootPath = definitionNode.getJcrPath();
        final JcrPathSegment[] pathSegments = definitionRootPath.toArray();
        int segmentsConsumed = 0;

        ConfigurationNodeImpl rootForDefinition = root;
        while (segmentsConsumed < pathSegments.length
                && rootForDefinition.getNode(pathSegments[segmentsConsumed]) != null) {
            rootForDefinition = rootForDefinition.getNode(pathSegments[segmentsConsumed]);
            segmentsConsumed++;
        }

        if (rootForDefinition.isDeleted()) {
            logger.warn("Content definition rooted at '{}' conflicts with deleted node at '{}', skipping.",
                    definitionNode.getJcrPath(), rootForDefinition.getJcrPath());
            return null;
        }

        if (pathSegments.length > segmentsConsumed + 1) {
            // this definition is rooted more than 1 node level deeper than a leaf node of the current tree.
            // that's unsupported, because it is likely to create models that cannot be persisted to JCR.
            String msg = String.format("%s contains definition rooted at unreachable node '%s'. "
                            + "Closest ancestor is at '%s'.", definition.getOrigin(), definitionRootPath,
                    rootForDefinition.getJcrPath());
            throw new IllegalStateException(msg);
        }

        if (pathSegments.length > segmentsConsumed) {
            final JcrPathSegment nodeName = pathSegments[segmentsConsumed];
            if (isAndRemainsNonConfigurationNode(rootForDefinition, nodeName.toString(), definition.getNode().getCategory())) {
                logger.warn("{} tries to modify non-configuration node '{}', skipping.",
                        definition.getOrigin(), definitionRootPath);
                return null;
            }

            if (!CollectionUtils.isEmpty(definitionNode.getProperties()) && !rootForDefinition.isRoot()) {
                //two cases: if same node already exists and belongs to not same extension or core
                //and if parent node belongs to different extension
                //Check for parent definition extension, or if parent defs are empty
                //Consider root '/' does not have definitions
                final TreeDefinitionImpl<?> bucketDefinition = rootForDefinition.getDefinitions().get(0).getDefinition();
                final String parentNodeExtensionName = bucketDefinition.getSource().getModule().getExtensionName();
                final String childDefinitionExtensionName = definition.getSource().getModule().getExtensionName();
                if (parentNodeExtensionName != null && !Objects.equals(childDefinitionExtensionName, parentNodeExtensionName)) {
                    final String errMessage = String.format("Cannot add child config definition '%s' to parent node definition, " +
                                    "as it is defined in different extension: %s -> %s",
                            definition.getNode().getPath(), definition.getSource(), bucketDefinition.getSource());
                    logger.error(errMessage);
                    throw new IllegalArgumentException(errMessage);
                }
            }

            rootForDefinition =
                    createChildNode(rootForDefinition, nodeName.toString(), definition.getNode());
        } else if (rootForDefinition == root && definitionNode.isDelete()) {
            throw new IllegalArgumentException("Deleting the root node is not supported.");
        } else  if (!rootForDefinition.isRoot()) {
            //Config node already exists so
            //validate if existing config node belongs to the same extension group
            final TreeDefinitionImpl<?> bucketDefinition = rootForDefinition.getDefinitions().get(0).getDefinition();
            if (!Objects.equals(definition.getSource().getModule().getExtensionName(),
                    bucketDefinition.getSource().getModule().getExtensionName())) {
                final String errMessage = String.format("Cannot merge config definitions with the same path '%s' defined in different " +
                                "extensions or in both core and an extension: %s -> %s",
                        definition.getNode().getPath(), bucketDefinition.getSource(), definition.getSource());
                logger.error(errMessage);
                throw new IllegalArgumentException(errMessage);
            }
        }

        // track definitions that mention a node as a root path, but don't change any properties
        if (rootForDefinition != null) {
            rootForDefinition.addDefinition(definitionNode);
        }
        return rootForDefinition;
    }

    public ConfigurationNodeImpl createChildNode(final ConfigurationNodeImpl parent, final String name,
                                                  final DefinitionNodeImpl definitionNode) {
        final ConfigurationNodeImpl node = new ConfigurationNodeImpl();

        if (definitionNode.isDelete()) {
            final String msg = String.format("%s: Trying to %sdelete node %s that does not exist.",
                    definitionNode.getOrigin(), definitionNode.isDeletedAndEmpty() ? "" : "merge ", definitionNode.getJcrPath());
            logger.warn(msg);
            if (definitionNode.isDeletedAndEmpty()) {
                return null;
            }
        }

        if (definitionNode.getCategory() != null) {
            if (definitionNode.getCategory() == ConfigurationItemCategory.CONFIG) {
                parent.clearChildNodeCategorySettings(name);
                if (definitionNode.getNodes().isEmpty() && definitionNode.getProperties().isEmpty()) {
                    return null;
                }
            } else {
                parent.setChildNodeCategorySettings(name, definitionNode.getCategory(), definitionNode);
                return null;
            }
        }

        final JcrPathSegment nameAndIndex = JcrPaths.getSegment(name);
        if (nameAndIndex.getIndex() > 1) {
            final String expectedSibling = createIndexedName(nameAndIndex.getName(), nameAndIndex.getIndex() - 1);
            if (parent.getNode(expectedSibling) == null) {
                final String msg = String.format("%s defines node '%s', but no sibling named '%s' was found",
                        definitionNode.getOrigin(), definitionNode.getJcrPath(), expectedSibling);
                throw new IllegalStateException(msg);
            }
        }

        node.setName(name);
        node.setParent(parent);
        node.addDefinition(definitionNode);

        parent.addNode(name, node);

        return node;
    }

    public ConfigurationTreeBuilder mergeProperty(final ConfigurationNodeImpl parent,
                               final DefinitionPropertyImpl definitionProperty) {
        // a node should have a back-reference to any definition that changes its properties
        parent.addDefinition(definitionProperty.getParent());

        final String name = definitionProperty.getName();
        final PropertyOperation op = definitionProperty.getOperation();

        // category rules:
        // 1. a config node cannot have content properties, since properties are impossible to define without a content node
        // 2. a config node may have system properties with or without initial values, which must be preserved in the tree
        // 3. a config node may have a system property that is redefined back to config in a downstream module
        // 4. a config node may have a config property that is redefined to system -- in this case, the previous value
        //    will be treated as an initial value unless an explicit operation: delete is specified

        ConfigurationPropertyImpl property = parent.getProperty(name);
        final ConfigurationItemCategory category = definitionProperty.getCategory();
        if (property != null) {
            final ConfigurationItemCategory configCategory = parent.getChildPropertyCategory(name);
            if (property.isDeleted()) {
                logger.warn("Property '{}' defined in '{}' has already been deleted. This property is not re-created.",
                        property.getJcrPath(), definitionProperty.getOrigin());
                return this;
            }

            // property already exists: first check category override
            if (category != null) {
                if (category == ConfigurationItemCategory.CONTENT) {
                    // it doesn't make sense to define a single property as content
                    logger.warn("Trying to redefine a property on a config node as content '{}', defined in '{}'. Skipping.",
                            definitionProperty.getJcrPath(), definitionProperty.getOrigin());
                    return this;
                }

                if (category != configCategory) {

                    if (category == ConfigurationItemCategory.CONFIG) {
                        // override system -> config: clear/reset category override and existing property values
                        parent.clearChildPropertyCategorySettings(name);
                        property.setValue(null);
                        property.setValues(null);
                    }
                    else {
                        // record the system category override
                        parent.setChildPropertyCategorySettings(name, category, definitionProperty);

                        if (definitionProperty.isEmptySystemProperty()) {
                            // this is a .meta:category system property with an overriding no initial value
                            // meaning the original property (only) is removed, NOT put in the deletedProperties map
                            property.getParent().removeProperty(name);
                            // don't process anything else here
                            return this;
                        }
                    }
                }
            }

            if (op == DELETE) {
                deletedProperties.put(property.getJcrPath(), property);
                property.addDefinition(definitionProperty);

                final ConfigurationNodeImpl parentNode = property.getParent();

                //Create placeholder property
                final ConfigurationPropertyImpl deleteProperty = new ConfigurationPropertyImpl();
                deleteProperty.setParent(parentNode);
                deleteProperty.setName(property.getName());
                deleteProperty.setDeleted(true);
                deleteProperty.setKind(property.getKind());
                deleteProperty.setValueType(property.getValueType());
                property.getDefinitions().forEach(deleteProperty::addDefinition);

                //swap real one property with a placeholder one
                parentNode.removeProperty(property.getName());
                parentNode.addProperty(property.getName(), deleteProperty);

                // no need to clear category on parent
                return this;
            }

            if (property.getKind() != definitionProperty.getKind()) {
                handleKindConflict(property, definitionProperty, op == OVERRIDE);
            }

            if (property.getValueType() != definitionProperty.getValueType()) {
                handleValueTypeConflict(property, definitionProperty, op == OVERRIDE);
            }

            requireOverrideOperationForPrimaryType(definitionProperty, property, op == OVERRIDE);
            requireOverrideOperationForMixinTypes(definitionProperty, property, op);
        } else {
            if (op == DELETE) {
                final String msg = String.format("%s: Trying to delete property %s that does not exist.",
                        definitionProperty.getOrigin(), definitionProperty.getJcrPath());
                logger.warn(msg);
                return this;
            }

            if (category != null) {
                if (category == ConfigurationItemCategory.CONFIG) {
                    // we're recategorizing back to config -- reset previous category info
                    // note: value(s) will be processed below, and the case of an unspecified value is blocked by both
                    //       the parser and the object model impl.
                    parent.clearChildPropertyCategorySettings(name);
                } else if (category == ConfigurationItemCategory.CONTENT) {
                    // it doesn't make sense to define a single property as content
                    // this case is blocked by both the parser and the object model impl, but we check here just to be
                    // extra paranoid about safety
                    logger.warn("Trying to define a property on a config node as content '{}', defined in '{}'. Skipping.",
                            definitionProperty.getJcrPath(), definitionProperty.getOrigin());
                    return this;
                } else {
                    // this must be a system property, which might have an initial value
                    parent.setChildPropertyCategorySettings(name, definitionProperty.getCategory(), definitionProperty);
                    if (definitionProperty.isEmptySystemProperty()) {
                        // this is a .meta:category system property with no value
                        // don't process anything else here
                        return this;
                    }
                }
            }

            // create new property
            property = new ConfigurationPropertyImpl();

            property.setName(name);
            property.setParent(parent);
            property.setKind(definitionProperty.getKind());
            property.setValueType(definitionProperty.getValueType());
        }

        if (op == REPLACE) {
            warnIfValuesAreEqual(definitionProperty, property);
        }

        // a property should have a back-reference to any def that affects it
        property.addDefinition(definitionProperty);

        if (PropertyKind.SINGLE == definitionProperty.getKind()) {
            property.setValue(definitionProperty.getValue());
        } else {
            if (op == ADD) {
                addValues(definitionProperty, property);
            } else {
                property.setValues(definitionProperty.getValues());
            }
        }

        parent.addProperty(name, property);

        return this;
    }

    private void handleKindConflict(final ConfigurationPropertyImpl property,
                                    final DefinitionPropertyImpl definitionProperty, boolean isOverride) {
        if (isOverride) {
            property.setKind(definitionProperty.getKind());
            property.setValue(null);
            property.setValues(null);
        } else {
            final String msg = String.format("Property %s already exists with type '%s', as determined by %s, "
                            + "but type '%s' is requested in %s.",
                    property.getJcrPath(), property.getKind(), property.getOrigin(), definitionProperty.getKind(),
                    definitionProperty.getOrigin());
            throw new IllegalStateException(msg);
        }
    }

    private void handleValueTypeConflict(final ConfigurationPropertyImpl property,
                                         final DefinitionPropertyImpl definitionProperty, boolean isOverride) {
        if (isOverride) {
            property.setValueType(definitionProperty.getValueType());
            property.setValue(null);
            property.setValues(null);
        } else {
            final String msg = String.format("Property %s already exists with value type '%s', "
                            + "as determined by %s, but value type '%s' is requested in %s.",
                    property.getJcrPath(), property.getValueType(), property.getOrigin(),
                    definitionProperty.getValueType(), definitionProperty.getOrigin());
            throw new IllegalStateException(msg);
        }
    }

    private void requireOverrideOperationForPrimaryType(final DefinitionPropertyImpl definitionProperty,
                                                        final ConfigurationPropertyImpl property,
                                                        final boolean isOverride) {
        if (property.getName().equals(JCR_PRIMARYTYPE)
                && !property.getValue().getString().equals(definitionProperty.getValue().getString())
                && !isOverride) {
            final String msg = String.format("Property %s is already defined on node %s as determined by %s, but change is requested in %s. "
                            + "Use 'operation: override' if you really intend to change the value of this property.",
                    JCR_PRIMARYTYPE, property.getParent().getJcrPath(), property.getOrigin(), definitionProperty.getOrigin());
            throw new IllegalStateException(msg);
        }
    }

    private void requireOverrideOperationForMixinTypes(final DefinitionPropertyImpl definitionProperty,
                                                       final ConfigurationPropertyImpl property,
                                                       final PropertyOperation op) {
        if (property.getName().equals(JCR_MIXINTYPES) && op != ADD) {
            final List<String> replacedMixins = definitionProperty.getValues().stream()
                    .map(ValueImpl::getString)
                    .collect(Collectors.toList());
            final List<String> missingMixins = property.getValues().stream()
                    .map(ValueImpl::getString)
                    .filter(mixin -> !replacedMixins.contains(mixin))
                    .collect(Collectors.toList());

            if (missingMixins.size() > 0 && op != OVERRIDE) {
                final String msg = String.format("Property %s is already defined on node %s, and replace operation of "
                                + "%s would remove values %s. Use 'operation: override' if you really intend to remove "
                                + "these values.",
                        JCR_MIXINTYPES, property.getParent().getJcrPath(), definitionProperty.getOrigin(), missingMixins.toString());
                throw new IllegalStateException(msg);
            }
        }
    }

    private void requirePrimaryType(final ConfigurationNodeImpl node, final DefinitionNodeImpl definitionNode) {
        if (node.getProperty(JCR_PRIMARYTYPE) == null) {
            final String msg = String.format("Node '%s' defined at '%s' is missing the required %s property.",
                    definitionNode.getJcrPath(), definitionNode.getOrigin(), JCR_PRIMARYTYPE);
            throw new IllegalStateException(msg);
        }
    }

    private void addValues(final DefinitionPropertyImpl definitionProperty, final ConfigurationPropertyImpl property) {
        // TODO: need to handle PropertyType.SET?

        final List<ValueImpl> existingValues = property.getValues();
        if (existingValues == null) {
            // suppress warning if adding to a system property, since we can't tell the difference between
            // explicit empty and not-specified in this common case
            if (property.getParent().getChildPropertyCategory(property.getName()) != ConfigurationItemCategory.SYSTEM) {
                logger.warn("Property '{}' defined in '{}' claims to ADD values, but property doesn't exist yet. Applying default behaviour.",
                        definitionProperty.getJcrPath(), definitionProperty.getOrigin());
            }
            property.setValues(definitionProperty.getValues());
        } else {
            List<ValueImpl> values = new ArrayList<>(existingValues);
            values.addAll(definitionProperty.getValues());
            property.setValues(values);
        }
    }

    private void warnIfValuesAreEqual(final DefinitionPropertyImpl definitionProperty,
                                      final ConfigurationPropertyImpl property) {
        // suppress warning when auto-export is updating a property
        // TODO reconsider below suppression term as part of HCM-166
        if (property.getOrigin().equals("[" + definitionProperty.getOrigin() + "]")) {
            return;
        }

        if (PropertyKind.SINGLE == property.getKind()) {
            final ValueImpl existingValue = property.getValue();
            if (existingValue != null) {
                if (definitionProperty.getValue().equals(property.getValue())
                        // suppress warning for translations special case, which would be verbose and difficult to avoid
                        && !property.getJcrPath().startsWith("/hippo:configuration/hippo:translations/")) {
                    logger.warn("Property '{}' defined in '{}' specifies value equivalent to existing property, defined in '{}'.",
                            definitionProperty.getJcrPath(), definitionProperty.getOrigin(), property.getOrigin());
                }
            }
        } else {
            final List<ValueImpl> existingValues = property.getValues();
            if (existingValues != null) {
                final List<ValueImpl> definitionValues = definitionProperty.getValues();
                if (existingValues.size() == definitionValues.size()
                        && existingValues.equals(definitionValues)) {
                    logger.warn("Property '{}' defined in '{}' specifies values equivalent to existing property, defined in '{}'.",
                            definitionProperty.getJcrPath(), definitionProperty.getOrigin(), property.getOrigin());
                }
            }
        }
    }

    public void pruneDeletedItems(final ConfigurationNodeImpl node) {
        if (node.isDeleted()) {
            node.getParent().removeNode(node.getName(), false);
            return;
        }

        final List<String> deletedProperties = node.getPropertyNames().stream()
                .filter(propertyName -> node.getProperty(propertyName).isDeleted())
                .collect(Collectors.toList());
        deletedProperties.forEach(node::removeProperty);

        final List<String> children = new ArrayList<>(node.getNodeNames());
        children.forEach(name -> pruneDeletedItems(node.getNode(name)));
    }

}
