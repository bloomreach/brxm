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
package org.onehippo.cm.migration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.model.impl.definition.AbstractDefinitionImpl;
import org.onehippo.cm.model.impl.definition.ConfigDefinitionImpl;
import org.onehippo.cm.model.impl.definition.TreeDefinitionImpl;
import org.onehippo.cm.model.impl.source.ConfigSourceImpl;
import org.onehippo.cm.model.impl.source.SourceImpl;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.tree.DefinitionPropertyImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.path.JcrPathSegment;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.tree.ConfigurationItemCategory;
import org.onehippo.cm.model.tree.DefinitionNode;
import org.onehippo.cm.model.tree.DefinitionProperty;
import org.onehippo.cm.model.tree.PropertyOperation;
import org.onehippo.cm.model.tree.Value;
import org.onehippo.cm.model.tree.ValueType;
import org.onehippo.cm.model.util.InjectResidualMatchers;
import org.onehippo.cm.model.util.OverrideResidualMatchers;

import static java.util.Collections.emptyList;
import static org.onehippo.cm.model.tree.PropertyKind.SINGLE;

public class ContentInitializeInstruction extends InitializeInstruction {

    protected ContentInitializeInstruction(final EsvNode instructionNode, final Type type,
                                           final InitializeInstruction combinedWith,
                                           final String[] contentRoots, final Set<String> newContentRoots,
                                           final InjectResidualMatchers injectResidualCategoryMatchers,
                                           final Map<String, ConfigurationItemCategory> injectResidualCategoryRegistry,
                                           final OverrideResidualMatchers overrideResidualCategoryMatchers)
            throws EsvParseException {

        super(instructionNode, type, combinedWith, contentRoots, newContentRoots, injectResidualCategoryMatchers,
                injectResidualCategoryRegistry, overrideResidualCategoryMatchers);
    }

    private String getNodePath() {
        String path;
        if (Type.CONTENTDELETE == getType()) {
            path = getContentPath();
        } else {
            path = StringUtils.substringBeforeLast(getContentPath(), "/");
        }
        return path.equals("") ? "/" : path;
    }

    public void processContentInstruction(final ConfigSourceImpl source,
                                          final Map<MinimallyIndexedPath, DefinitionNodeImpl> nodeDefinitions,
                                          Set<DefinitionNode> deltaNodes) throws EsvParseException {
        final String nodePath = getNodePath();
        final String name = StringUtils.substringAfterLast(getContentPath(), "/");

        DefinitionNodeImpl node = nodeDefinitions.get(new MinimallyIndexedPath(nodePath));

        log.info("processing " + getType().getPropertyName() + " " + getContentPath());

        switch (getType()) {
            case CONTENTDELETE:
                if (node != null) {
                    if (node.isDeletedAndEmpty()) {
                        log.warn("Ignoring hippo:contentdelete " + getName() + " at " + getInstructionNode().getSourceLocation() +
                                ": path " + nodePath + " already deleted at " + node.getSourceLocation());
                    } else {
                        final boolean removable = isRemovableNode(node, deltaNodes);
                        log.info((removable ? "Removing" : "Deleting") + " path " + nodePath + " defined at " + node.getSourceLocation() +
                                " by hippo:contentdelete " + getName() + " at " + getInstructionNode().getSourceLocation());
                        if (removable) {
                            nodeDefinitions.remove(new MinimallyIndexedPath(nodePath));
                        }
                        deleteChildren(nodePath + "/", nodeDefinitions);
                        if (deltaNodes.contains(node)) {
                            deltaNodes.remove(node);
                        }
                        if (node.isRoot()) {
                            deleteDefinition(node.getDefinition());
                            if (!removable) {
                                // force add new (root) delete on nodePath, will *replace* nodeDefinition on nodePath
                                node = null;
                            }
                        } else {
                            if (removable) {
                                deleteDefinitionNode(node);
                                DefinitionNodeImpl parentNode = node.getParent();
                                while (parentNode.isEmpty()) {
                                    // empty (delta) parent: remove as well
                                    nodeDefinitions.remove(new MinimallyIndexedPath(parentNode.getJcrPath().toString()));
                                    deltaNodes.remove(parentNode);
                                    if (parentNode.isRoot()) {
                                        // can remove whole of delta definition
                                        deleteDefinition(parentNode.getDefinition());
                                        break;
                                    } else {
                                        // first time redundant
                                        deleteDefinitionNode(node);
                                        node = parentNode;
                                    }
                                }
                            } else {
                                node.delete();
                                node.getSourceLocation().copy(getInstructionNode().getSourceLocation());
                            }
                        }
                        removeInjectResidualCategoryPath(nodePath);
                    }
                } else {
                    if (isContent(nodePath)) {
                        throw new EsvParseException("Cannot process hippo:contentdelete for " + nodePath
                                + " defined at " + getInstructionNode().getSourceLocation()
                                + ": node not found in this module, manual conversion required");
                    }
                    log.info("Adding hippo:contentdelete for " + nodePath + " defined at " + getInstructionNode().getSourceLocation());
                }
                if (node == null) {
                    if (isContent(nodePath)) {
                        throw new EsvParseException("Cannot process hippo:contentdelete for " + nodePath
                                + " defined at " + getInstructionNode().getSourceLocation()
                                + ": node not found in this module, manual conversion required");
                    }
                    ConfigDefinitionImpl def = source.addConfigDefinition();
                    node = new DefinitionNodeImpl(nodePath, name, def);
                    def.setNode(node);
                    node.setDelete(true);
                    node.getSourceLocation().copy(getInstructionNode().getSourceLocation());
                    nodeDefinitions.put(new MinimallyIndexedPath(nodePath), node);
                }
                break;
            case CONTENTPROPDELETE:
                if (node != null) {
                    if (node.isDeletedAndEmpty()) {
                        log.warn("Ignoring hippo:contentpropdelete " + getName() + " for property " + getContentPath() +
                                " at " + getInstructionNode().getSourceLocation() +
                                ":  parent node already deleted at " + node.getSourceLocation());
                        // skip adding delete property
                        node = null;
                    } else {
                        DefinitionPropertyImpl prop = node.getProperty(name);
                        if (prop != null) {
                            if (PropertyOperation.DELETE == prop.getOperation()) {
                                log.warn("Ignoring hippo:contentpropdelete " + getName() + " for property " + getContentPath() +
                                        " at " + getInstructionNode().getSourceLocation() +
                                        ": property already deleted at " + prop.getSourceLocation());
                                // skip adding delete property
                                node = null;
                            } else {
                                final boolean removable = isRemovableProp(prop, deltaNodes);
                                log.info((removable ? "Removing" : "Deleting") + " property " + getContentPath()
                                        + " defined at " + prop.getSourceLocation() + " by hippo:contentpropdelete " +
                                        getName() + " at " + getInstructionNode().getSourceLocation());
                                if (removable) {
                                    node.getModifiableProperties().remove(name);
                                    // no need to add delete property
                                    node = null;
                                }
                            }
                        } else {
                            if (isContent(nodePath)) {
                                throw new EsvParseException("Cannot process hippo:contentpropdelete for "
                                        + nodePath + " defined at " + getInstructionNode().getSourceLocation()
                                        + ": parent node not found in this module, manual conversion required");
                            }
                            log.info("Merging hippo:contentpropdelete for " + nodePath + " defined at " + node.getSourceLocation());
                            // add delete property at the end
                        }
                    }
                } else {
                    if (isContent(nodePath)) {
                        throw new EsvParseException("Cannot process hippo:contentpropdelete for "
                                + nodePath + " defined at " + getInstructionNode().getSourceLocation()
                                + getInstructionNode().getSourceLocation()
                                + ": parent node not found in this module, manual conversion required");
                    }
                    node = findNearestParent(nodePath, nodeDefinitions, deltaNodes);
                    if (node != null) {
                        if (node.isDeletedAndEmpty()) {
                            log.warn("Ignoring hippo:contentpropdelete " + getName() + " for property " + getContentPath() +
                                    " at " + getInstructionNode().getSourceLocation() +
                                    ":  nearest parent node already deleted at " + node.getSourceLocation());
                            // skip adding delete property
                            node = null;
                        } else {
                            if (deltaNodes.contains(node)) {
                                node = addIntermediateParents(node, nodePath, nodeDefinitions, deltaNodes);
                            } else {
                                log.warn("Ignoring hippo:contentpropdelete " + getName() + " for property " + getContentPath() +
                                        " at " + getInstructionNode().getSourceLocation() +
                                        ": no direct or intermediate delta node parent defined.");
                                // skip adding delete property
                                node = null;
                            }
                        }
                    } else {
                        node = addDeltaRootNode(source, nodePath, name, nodeDefinitions, deltaNodes);
                    }
                    if (node != null) {
                        log.info("Adding hippo:contentpropdelete for " + nodePath + " defined at " + getInstructionNode().getSourceLocation());
                    }
                }
                if (node != null) {
                    // use empty list as stand-in for "no value defined"
                    DefinitionPropertyImpl prop = node.addProperty(name, ValueType.STRING, emptyList());
                    prop.setOperation(PropertyOperation.DELETE);
                    prop.getSourceLocation().copy(getInstructionNode().getSourceLocation());
                }
                break;
            case CONTENTPROPSET:
            case CONTENTPROPADD:
                EsvProperty property = getTypeProperty();
                if (!property.isMultiple() && EsvProperty.isKnownMultipleProperty(name)) {
                    property.setMultiple(true);
                }
                DefinitionPropertyImpl prop = null;
                if (node != null) {
                    if (node.isDeletedAndEmpty()) {
                        log.warn("Ignoring " + getType().getPropertyName() + " " + getName() + " for property " + getContentPath() +
                                " at " + getInstructionNode().getSourceLocation() +
                                ":  parent node already deleted at " + node.getSourceLocation());
                        // skip adding delete property
                        node = null;
                    } else {
                        prop = node.getProperty(name);
                        if (prop != null) {
                            if (PropertyOperation.DELETE == prop.getOperation()) {
                                prop = null;
                            } else if (getType() == Type.CONTENTPROPADD || PropertyOperation.OVERRIDE != prop.getOperation()) {
                                // check same type and valueType
                                if (prop.getValueType().ordinal() != property.getType()) {
                                    throw new EsvParseException("Unsupported " + getType().getPropertyName() +
                                            " " + prop.getJcrPath() + " type change to " + ValueType.fromJcrType(property.getType()).name() +
                                            " at " + property.getSourceLocation() + " (from " + prop.getValueType().toString() +
                                            " at " + prop.getSourceLocation() + ")");
                                }
                                if ((prop.getKind() == SINGLE && property.isMultiple()) || (prop.getKind() != SINGLE && !property.isMultiple())) {
                                    throw new EsvParseException("Unsupported " + getType().getPropertyName() + " " + prop.getJcrPath() +
                                            " multiplicity change to " + property.isMultiple() + " at " + property.getSourceLocation() +
                                            " (from " + !property.isMultiple() + " at " + prop.getSourceLocation() + ")");
                                }
                            }
                        }
                        log.info("Merging " + getType().getPropertyName() + " for " + nodePath + " defined at " + node.getSourceLocation());
                    }
                } else {
                    if (isContent(nodePath)) {
                        throw new EsvParseException("Cannot process " + getType().getPropertyName() + " " + getName()
                                + " for " + nodePath + " defined at " + getInstructionNode().getSourceLocation()
                                + ": parent node not found in this module, manual conversion required");
                    }
                    node = findNearestParent(nodePath, nodeDefinitions, deltaNodes);
                    if (node != null) {
                        if (node.isDeletedAndEmpty()) {
                            log.warn("Ignoring " + getType().getPropertyName() + " " + getName() + " for property " + getContentPath() +
                                    " at " + getInstructionNode().getSourceLocation() +
                                    ":  nearest parent node already deleted at " + node.getSourceLocation());
                            // skip adding property
                            node = null;
                        } else {
                            node = addIntermediateParents(node, nodePath, nodeDefinitions, deltaNodes);
                        }
                    } else {
                        node = addDeltaRootNode(source, nodePath, name, nodeDefinitions, deltaNodes);
                    }
                    if (node != null) {
                        log.info("Adding " + getType().getPropertyName() + " for " + nodePath + " defined at " + getInstructionNode().getSourceLocation());
                    }
                }
                if (node != null) {
                    PropertyOperation op = PropertyOperation.REPLACE;
                    if (prop != null && PropertyOperation.OVERRIDE == prop.getOperation()) {
                        op = prop.getOperation();
                    } else if (getType() == Type.CONTENTPROPADD) {
                        op = PropertyOperation.ADD;
                    }
                    addProperty(node, property, name, prop, op, false);
                }
                break;
        }
    }

    protected DefinitionPropertyImpl addProperty(final DefinitionNodeImpl node, final EsvProperty property,
                                                 final String propertyName, final DefinitionPropertyImpl current,
                                                 final PropertyOperation op, final boolean isPathReference)
            throws EsvParseException {
        DefinitionPropertyImpl prop;
        ValueType valueType = isPathReference ? ValueType.REFERENCE : ValueType.fromJcrType(property.getType());
        List<ValueImpl> newValues = property.isMultiple() ? new ArrayList<>(property.getValues().size()) : new ArrayList<>(1);
        for (final EsvValue value : property.getValues()) {
            Object valueObject = value.getValue();
            if (isPathReference) {
                final String referencePath = (String)valueObject;
                if (!referencePath.startsWith("/")) {
                    // While esv path references may be relative, for esv2yaml migration we cannot easily support this
                    // as the esv export *may* generate relative references *outside* the scope of the esv (and thus yaml)
                    // content/config definition itself.
                    // The latter is *not* supported for relative yaml path references, which always must be relative to
                    // the definition root path ("" being used/needed for a relative reference to the root node itself).
                    valueObject = StringUtils.substringBeforeLast(getContentPath(), "/") + "/" + referencePath;
                }
            }
            newValues.add(new ValueImpl(valueObject, valueType, value.isResourcePath(), isPathReference));
        }
        if (current != null && PropertyOperation.ADD == op) {
            List<ValueImpl> oldValues = cloneValues(current.getValues());
            List<ValueImpl> values = new ArrayList<>(oldValues.size() + newValues.size());
            values.addAll(oldValues);
            values.addAll(newValues);
            newValues = values;
        }
        if (property.isMultiple() || newValues.size() > 1) {
            prop = node.addProperty(propertyName, valueType, newValues);
        } else {
            prop = node.addProperty(propertyName, newValues.get(0));
        }
        if (current != null) {
            prop.setOperation(current.getOperation());
        }
        if (PropertyOperation.ADD != op || current == null) {
            prop.setOperation(op);
        }
        prop.getSourceLocation().copy(property.getSourceLocation());
        return prop;
    }

    private List<ValueImpl> cloneValues(final List<ValueImpl> values) {
        final List<ValueImpl> result = new ArrayList<>(values.size());
        for (final Value value : values) {
            result.add(new ValueImpl(value.getObject(), value.getType(), value.isResource(), value.isPath()));
        }
        return result;
    }

    private DefinitionNodeImpl addIntermediateParents(final DefinitionNodeImpl parentNode, final String nodePath,
                                                      final Map<MinimallyIndexedPath, DefinitionNodeImpl> nodeDefinitions,
                                                      final Set<DefinitionNode> deltaNodes) {
        DefinitionNodeImpl node = parentNode;
        JcrPath parentPath = JcrPaths.getPath(nodePath);
        for (JcrPathSegment parent : parentPath.relativize(parentNode.getJcrPath())) {
            node = node.addNode(parent.toString());
            node.getSourceLocation().copy(getInstructionNode().getSourceLocation());
            nodeDefinitions.put(new MinimallyIndexedPath(node.getJcrPath().toString()), node);
            deltaNodes.add(node);
        }
        return node;
    }

    protected DefinitionNodeImpl addDeltaRootNode(final SourceImpl source, final String nodePath, final String name,
                                                  final Map<MinimallyIndexedPath, DefinitionNodeImpl> nodeDefinitions,
                                                  final Set<DefinitionNode> deltaNodes) {
        ConfigDefinitionImpl def = ((ConfigSourceImpl)source).addConfigDefinition();
        DefinitionNodeImpl node = new DefinitionNodeImpl(nodePath, name, def);
        def.setNode(node);
        deltaNodes.add(node);
        node.getSourceLocation().copy(getInstructionNode().getSourceLocation());
        nodeDefinitions.put(new MinimallyIndexedPath(nodePath), node);
        return node;
    }

    private DefinitionNodeImpl findNearestParent(final String nodePath,
                                                 final Map<MinimallyIndexedPath, DefinitionNodeImpl> nodeDefinitions,
                                                 final Set<DefinitionNode> deltaNodes) {
        String path = StringUtils.substringBeforeLast(nodePath, "/");
        if (path.equals("")) {
            path = "/";
        }
        DefinitionNodeImpl parent = nodeDefinitions.get(new MinimallyIndexedPath(path));
        if (parent != null) {
            return parent;
        } else if (!path.equals("/")) {
            return findNearestParent(path, nodeDefinitions, deltaNodes);
        }
        return null;
    }

    private void deleteChildren(final String childPathPrefix, final Map<MinimallyIndexedPath, DefinitionNodeImpl> nodeDefinitions) {
        TreeSet<MinimallyIndexedPath> deletePaths = nodeDefinitions.keySet().stream()
                .filter(p -> p.getPath().startsWith(childPathPrefix))
                .collect(Collectors.toCollection(TreeSet::new));
        for (Iterator<MinimallyIndexedPath> delIter = deletePaths.descendingIterator(); delIter.hasNext(); ) {
            MinimallyIndexedPath delPath = delIter.next();
            DefinitionNodeImpl delNode = nodeDefinitions.get(delPath);
            if (delNode.isRoot()) {
                // remove definition from source
                deleteDefinition(delNode.getDefinition());
            } else {
                deleteDefinitionNode(delNode);
            }
            nodeDefinitions.remove(delPath);
        }
    }

    protected void deleteDefinition(final TreeDefinitionImpl definition) {
        // remove definition from source
        SourceImpl defSource = definition.getSource();
        for (Iterator<AbstractDefinitionImpl> defIterator = defSource.getModifiableDefinitions().iterator(); defIterator.hasNext(); ) {
            if (defIterator.next() == definition) {
                defIterator.remove();
                break;
            }
        }
    }

    private void deleteDefinitionNode(final DefinitionNodeImpl node) {
        // remove definitionNode from parent
        node.getParent().getModifiableNodes().remove(node.getName());
    }

    protected boolean isRemovableProp(final DefinitionProperty property, final Set<DefinitionNode> deltaNodes) {
        return !deltaNodes.contains(property.getParent());
    }

    protected boolean isRemovableNode(final DefinitionNode node, final Set<DefinitionNode> deltaNodes) {
        if (node.isRoot()) {
            return !deltaNodes.contains(node);
        } else {
            return !deltaNodes.contains(node) || !deltaNodes.contains(node.getParent());
        }
    }
}
