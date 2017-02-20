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
package org.onehippo.cm.migration;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jcr.PropertyType;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.api.model.PropertyOperation;
import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.api.model.ValueType;
import org.onehippo.cm.impl.model.ConfigDefinitionImpl;
import org.onehippo.cm.impl.model.DefinitionNodeImpl;
import org.onehippo.cm.impl.model.DefinitionPropertyImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.SourceImpl;

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.onehippo.cm.api.model.PropertyType.SINGLE;

public class SourceInitializeInstruction extends ContentInitializeInstruction {

    private static final String PATH_REFERENCE_POSTFIX = "___pathreference";

    private EsvNode sourceNode;

    public SourceInitializeInstruction(final EsvNode instructionNode, final Type type,
                                       final InitializeInstruction combinedWith) throws EsvParseException {
        super(instructionNode, type, combinedWith);
    }

    public EsvNode getSourceNode() {
        return sourceNode;
    }

    public void prepareSource(final EsvParser parser) throws IOException, EsvParseException {
        prepareResource(parser.getBaseDir(), true);
        setSourcePath(FilenameUtils.removeExtension(getResourcePath()) + ".yaml");
        sourceNode = parser.parse(new FileInputStream(getResource()), getResource().getCanonicalPath());
        final String contentRoot = normalizePath(getPropertyValue("hippo:contentroot", PropertyType.STRING, true));
        setContentPath(contentRoot + "/" + sourceNode.getName());
    }

    public boolean isDeltaMerge() {
        return sourceNode != null && sourceNode.isDeltaMerge();
    }

    public boolean isDeltaSkip() {
        return sourceNode != null && sourceNode.isDeltaSkip();
    }

    public void processSource(final ModuleImpl module, final Map<String, DefinitionNodeImpl> nodeDefinitions,
                              final Map<DefinitionNodeImpl, Boolean> deltaNodes) throws EsvParseException {
        final SourceImpl source = module.addSource(getSourcePath());
        log.info("Processing " + getType().getPropertyName() + " " + getContentPath() + " from file " + getResourcePath());
        processNode(sourceNode, getContentPath(), source, null, nodeDefinitions, deltaNodes);
    }

    private void processNode(final EsvNode node, final String path, final SourceImpl source, DefinitionNodeImpl parentNode,
                             final Map<String, DefinitionNodeImpl> nodeDefinitions,
                             final Map<DefinitionNodeImpl, Boolean> deltaNodes) throws EsvParseException {

        DefinitionNodeImpl defNode = nodeDefinitions.get(path);
        if (defNode != null && !defNode.isDelete()) {
            if (node.isDeltaSkip()) {
                log.warn("Skipping node " + path + " at " + node.getSourceLocation() + ": already defined before at " +
                        defNode.getSourceLocation() + ".");
                return;
            }
            if (!node.isDeltaMerge()) {
                throw new EsvParseException("Node " + path + " at " + node.getSourceLocation() + " already defined before at " +
                        defNode.getSourceLocation() + ".");
            }
        }
        final boolean newNode = defNode == null || defNode.isDelete();
        if (newNode) {
            String parentPath = null;
            if (parentNode == null && node.getMerge() != null) {
                if (defNode != null && !defNode.isRoot()) {
                    parentNode = (DefinitionNodeImpl) defNode.getParent();
                } else {
                    parentPath = StringUtils.substringBeforeLast(path, "/");
                    if (parentPath.equals("")) {
                        parentPath = "/";
                    }
                    parentNode = nodeDefinitions.get(parentPath);
                }
            }
            if (node.isDeltaInsert() && parentNode == null) {
                ConfigDefinitionImpl def = source.addConfigDefinition();
                parentNode = new DefinitionNodeImpl(parentPath, StringUtils.substringAfterLast(parentPath, "/"), def);
                def.setNode(parentNode);
                parentNode.getSourceLocation().copy(node.getSourceLocation());
                deltaNodes.put(parentNode, null);
//                parentNode.setDelta(true);
                nodeDefinitions.put(parentPath, parentNode);
            }
            if (parentNode != null) {
                defNode = parentNode.addNode(node.getName());
            } else {
                ConfigDefinitionImpl def = source.addConfigDefinition();
                boolean delta = defNode == null && node.getMerge() != null;
                defNode = new DefinitionNodeImpl(path, node.getName(), def);
                def.setNode(defNode);
                if (delta) {
                    deltaNodes.put(defNode, null);
                }
//                defNode.setDelta(delta);
            }
            defNode.getSourceLocation().copy(node.getSourceLocation());

            nodeDefinitions.put(path, defNode);

            if (node.isDeltaInsert()) {
                if (isDelta(defNode, deltaNodes)) {
                    defNode.setOrderBefore(node.getMergeLocation());
                } else {
                    boolean hasAfter = parentNode.getModifiableNodes().size() > 1 &&
                            (node.getMergeLocation().equals("") || parentNode.getModifiableNodes().containsKey(node.getMergeLocation()));
                    if (hasAfter) {
                        LinkedHashMap<String, DefinitionNodeImpl> childNodes = new LinkedHashMap<>(parentNode.getModifiableNodes());
                        childNodes.remove(defNode.getName());
                        parentNode.getModifiableNodes().clear();
                        if (node.getMergeLocation().equals("")) {
                            parentNode.getModifiableNodes().put(defNode.getName(), defNode);
                            parentNode.getModifiableNodes().putAll(childNodes);
                        } else {
                            boolean found = false;
                            for (String name : childNodes.keySet()) {
                                if (!found && node.getMergeLocation().equals(name)) {
                                    found = true;
                                    parentNode.getModifiableNodes().put(defNode.getName(), defNode);
                                }
                                parentNode.getModifiableNodes().put(name, childNodes.get(name));
                            }
                        }
                    } else {
                        log.warn("Ordering node " + path + " defined at " + defNode.getSourceLocation() +
                                " before sibling \"" + node.getMergeLocation() + "\" ignored: sibling not found.");
                    }
                }
            }
        }
        EsvProperty prop = node.getProperty(JCR_PRIMARYTYPE);
        if (prop == null) {
            if (!node.isDeltaCombine()) {
                throw new EsvParseException("Missing required property " + JCR_PRIMARYTYPE + " for node " + path + " at " +
                        node.getSourceLocation());
            }
        }
        for (EsvProperty property : node.getProperties()) {
            processProperty(node, defNode, property);
        }
        for (EsvNode child : node.getChildren()) {
            processNode(child, path + "/" + child.getName(), source, defNode, nodeDefinitions, deltaNodes);
        }
    }

    private void processProperty(final EsvNode node, final DefinitionNodeImpl defNode, final EsvProperty property)
            throws EsvParseException {
        final boolean isPathReference = property.getName().endsWith(PATH_REFERENCE_POSTFIX);
        final String propertyName = isPathReference ? StringUtils.substringBefore(property.getName(), PATH_REFERENCE_POSTFIX) : property.getName();
        DefinitionPropertyImpl prop = defNode.getModifiableProperties().get(propertyName);
        boolean override = false;
        if (prop != null) {
            if (PropertyOperation.DELETE == prop.getOperation()) {
                if (property.getMerge() != null && !property.isMergeOverride()) {
                    throw new EsvParseException("Unsupported delta merging of property " + prop.getPath() + " at " +
                            property.getSourceLocation() + " which has been deleted before at " +
                            prop.getSourceLocation() + ". Requires esv:merge=\"overrride\"");
                } else {
                    // will be replaced with incoming property
                    override = true;
                    prop = null;
                }
            } else {
                if (property.isMergeSkip()) {
                    log.warn("Skipping property " + prop.getPath() + " which already is defined at " + prop.getSourceLocation());
                    return;
                }
                if (PropertyOperation.REPLACE != prop.getOperation()) {
                    throw new EsvParseException("Unsupported delta merging of property " + prop.getPath() + " at " +
                            property.getSourceLocation() + " which already is delta merge defined at " + prop.getSourceLocation());
                }
                if (prop.getValueType().ordinal() != property.getType()) {
                    throw new EsvParseException("Unsupported property " + prop.getPath() + " type change to " +
                            ValueType.values()[property.getType()].name() + " at " + property.getSourceLocation() +
                            " (from " + prop.getValueType().toString() + " at " + prop.getSourceLocation() + ")");
                }
                if ((prop.getType() == SINGLE && property.isMultiple()) || (prop.getType() != SINGLE && property.isSingle())) {
                    // note: won't happen with restricted properties (jcr:primaryType, jcr:mixins)
                    if (!property.isMergeOverride()) {
                        throw new EsvParseException("Unsupported property " + prop.getPath() + " multiplicity change to " +
                                property.isMultiple() + " at " + property.getSourceLocation() +
                                " (from " + !property.isMultiple() + " at " + prop.getSourceLocation() + ")");
                    } else {
                        override = true;
                    }
                }
                if (JCR_PRIMARYTYPE.equals(propertyName)) {
                    String newType = property.getValue();
                    String oldType = prop.getValue().toString();
                    if (!oldType.equals(newType)) {
                        if (!node.isDeltaOverlay()) {
                            throw new EsvParseException("Redefining node " + defNode.getPath() + " type to " + newType + " at " +
                                    property.getSourceLocation() + " (from " + oldType + " at " + prop.getSourceLocation() +
                                    ") not allowed: requires esv:merge=\"overlay\").");
                        } else {
                            override = true;
                        }
                    } else {
                        // no change
                        return;
                    }
                } else if (JCR_MIXINTYPES.equals(propertyName)) {
                    Set<String> oldMixins = Arrays.stream(prop.getValues()).map(Value::getString).collect(Collectors.toSet());
                    Set<String> newMixins = property.getValues().stream().map(EsvValue::getString).collect(Collectors.toSet());
                    if (!oldMixins.equals(newMixins)) {
                        if (!node.isDeltaOverlay()) {
                            throw new EsvParseException("Redefining node " + defNode.getPath() + " mixins to " + newMixins +
                                    " at " + property.getSourceLocation() + " (from " + oldMixins + " at " + prop.getSourceLocation() +
                                    ") not allowed: requires esv:merge=\"overlay\").");
                        } else {
                            override = true;
                        }
                    } else {
                        // no change
                        return;
                    }
                }
            }
        }
        addProperty(defNode, property, propertyName, prop, override ? EsvMerge.OVERRIDE : property.getMerge(), isPathReference);
    }
}
