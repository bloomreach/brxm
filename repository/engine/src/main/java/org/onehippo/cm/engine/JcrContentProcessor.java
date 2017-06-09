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
package org.onehippo.cm.engine;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.core.NodeImpl;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.decorating.NodeDecorator;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.cm.model.ActionType;
import org.onehippo.cm.model.ConfigurationItemCategory;
import org.onehippo.cm.model.DefinitionNode;
import org.onehippo.cm.model.DefinitionProperty;
import org.onehippo.cm.model.DefinitionType;
import org.onehippo.cm.model.PropertyOperation;
import org.onehippo.cm.model.PropertyType;
import org.onehippo.cm.model.Value;
import org.onehippo.cm.model.ValueType;
import org.onehippo.cm.model.impl.AbstractDefinitionImpl;
import org.onehippo.cm.model.impl.ConfigDefinitionImpl;
import org.onehippo.cm.model.impl.ConfigSourceImpl;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.ConfigurationNodeImpl;
import org.onehippo.cm.model.impl.ConfigurationPropertyImpl;
import org.onehippo.cm.model.impl.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.ContentSourceImpl;
import org.onehippo.cm.model.impl.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.DefinitionPropertyImpl;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.ValueImpl;
import org.onehippo.cm.model.util.SnsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.JCR_UUID;
import static org.onehippo.cm.engine.ValueProcessor.collectVerifiedValue;
import static org.onehippo.cm.engine.ValueProcessor.isKnownDerivedPropertyName;
import static org.onehippo.cm.engine.ValueProcessor.isReferenceTypeProperty;
import static org.onehippo.cm.engine.ValueProcessor.isUuidInUse;
import static org.onehippo.cm.engine.ValueProcessor.valueFrom;
import static org.onehippo.cm.engine.ValueProcessor.valuesFrom;
import static org.onehippo.cm.model.ActionType.APPEND;
import static org.onehippo.cm.model.ActionType.DELETE;
import static org.onehippo.cm.model.Constants.YAML_EXT;
import static org.onehippo.cm.model.util.ConfigurationModelUtils.getCategoryForNode;
import static org.onehippo.cm.model.util.SnsUtils.createIndexedName;

/**
 * Applies definition nodes to JCR
 */
public class JcrContentProcessor {

    private static final Logger log = LoggerFactory.getLogger(JcrContentProcessor.class);

    private final Collection<Pair<DefinitionProperty, Node>> unprocessedReferences = new ArrayList<>();

    /**
     * Import definition under the rootNode
     *
     * @param modelNode
     * @param parentNode
     */
    public synchronized void importNode(final DefinitionNode modelNode, final Node parentNode, final ActionType actionType) throws RepositoryException, IOException {
        if (actionType == DELETE) {
            throw new IllegalArgumentException("DELETE action is not supported for import operation");
        }

        final DefinitionNodeImpl newNode = constructNewParentNode(modelNode, parentNode.getPath());

        final Session session = parentNode.getSession();
        validateAppendAction(newNode.getPath(), actionType, session);

        applyNode(newNode, parentNode, actionType);
    }

    /**
     * Validate if node exists and current action type is APPEND
     * @param nodePath Node path
     * @param actionType current action type
     * @param session current session
     * @throws RepositoryException if node exists and action type is APPEND
     */
    private void validateAppendAction(final String nodePath, final ActionType actionType, final Session session) throws RepositoryException {
        final boolean nodeExists = session.nodeExists(nodePath);
        if (nodeExists && actionType == APPEND) {
            throw new ItemExistsException(String.format("Node already exists at path %s", nodePath));
        }
    }

    /**
     * Create new definition node under given path path
     * @param modelNode {@link DefinitionNode} source node
     * @param path new path
     * @return Node under the new path
     */
    private DefinitionNodeImpl constructNewParentNode(final DefinitionNode modelNode, final String path) throws RepositoryException {

        final String newPath = constructNodePath(path, modelNode.getName());

        final DefinitionNodeImpl node = (DefinitionNodeImpl) modelNode;
        final DefinitionNodeImpl newNode = new DefinitionNodeImpl(newPath, modelNode.getName(), node.getDefinition());
        newNode.getModifiableNodes().putAll(node.getModifiableNodes());
        newNode.getModifiableProperties().putAll(node.getModifiableProperties());
        newNode.setOrderBefore(node.getOrderBefore());
        newNode.setIgnoreReorderedChildren(node.getIgnoreReorderedChildren());
        return newNode;
    }

    private String constructNodePath(final String path, final String nodeName) {
        return path.equals("/") ? "/" + nodeName : path + "/" + nodeName;
    }

    /**
     * Export specified node
     *
     * @param node
     * @return
     */
    public ModuleImpl exportNode(final Node node) throws RepositoryException {
        final ModuleImpl module = new ModuleImpl("export-module", new ProjectImpl("export-project", new GroupImpl("export-group")));
        module.setConfigResourceInputProvider(new JcrResourceInputProvider(node.getSession()));
        module.setContentResourceInputProvider(module.getConfigResourceInputProvider());
        final String sourceFilename = mapNodeNameToFileName(NodeNameCodec.decode(node.getName()));
        final ContentSourceImpl contentSource = module.addContentSource(sourceFilename +YAML_EXT);
        final ContentDefinitionImpl contentDefinition = contentSource.addContentDefinition();

        exportNode(node, contentDefinition);

        return module;
    }

    protected String mapNodeNameToFileName(String part) {
        return part.contains(":") ? part.replace(":", "-") : part;
    }

    public DefinitionNodeImpl exportNode(final Node node, final ContentDefinitionImpl contentDefinition) throws RepositoryException {
        if (isVirtual(node)) {
            throw new ConfigurationRuntimeException("Virtual node cannot be exported: " + node.getPath());
        }

        // Creating a definition with path 'rooted' at the node itself, without possible SNS index: we're not supporting indexed path elements
        final DefinitionNodeImpl definitionNode = new DefinitionNodeImpl("/"+node.getName(), node.getName(), contentDefinition);
        contentDefinition.setNode(definitionNode);

        processProperties(node, definitionNode);

        for (final Node childNode : new NodeIterable(node.getNodes())) {
            exportNode(childNode, definitionNode);
        }
        return definitionNode;
    }

    public DefinitionNodeImpl exportNode(final Node sourceNode, final DefinitionNodeImpl parentNode) throws RepositoryException {

        if (!isVirtual(sourceNode)) {
            final DefinitionNodeImpl definitionNode = parentNode.addNode(createNodeName(sourceNode));

            processProperties(sourceNode, definitionNode);

            for (final Node childNode : new NodeIterable(sourceNode.getNodes())) {
                exportNode(childNode, definitionNode);
            }
            return definitionNode;
        }
        return null;
    }

    private boolean isVirtual(final Node node) throws RepositoryException {
        return ((HippoNode)node).isVirtual();
    }

    private String createNodeName(final Node sourceNode) throws RepositoryException {
        final String name = sourceNode.getName();
        if (sourceNode.getIndex() > 1) {
            return name+"["+sourceNode.getIndex()+"]";
        } else {
            if (sourceNode.getDefinition().allowsSameNameSiblings() && sourceNode.getParent().hasNode(name+"[2]")) {
                return name+"[1]";
            }
        }
        return name;
    }

    private void processProperties(final Node sourceNode, final DefinitionNodeImpl definitionNode) throws RepositoryException {

        processPrimaryTypeAndMixins(sourceNode, definitionNode);

        for (final Property property : new PropertyIterable(sourceNode.getProperties())) {
            if (property.getName().equals(JCR_PRIMARYTYPE) || property.getName().equals(JCR_MIXINTYPES)) {
                continue; //Already processed those properties
            }

            if (isKnownDerivedPropertyName(property.getName())) {
                continue;
            }

            exportProperty(property, definitionNode);
        }
    }

    private DefinitionPropertyImpl exportProperty(final Property property, DefinitionNodeImpl definitionNode) throws RepositoryException {
        if (property.isMultiple()) {
            return definitionNode.addProperty(property.getName(), ValueType.fromJcrType(property.getType()),
                    valuesFrom(property, definitionNode));
        } else {
            final ValueImpl value = valueFrom(property, definitionNode);
            final DefinitionPropertyImpl targetProperty = definitionNode.addProperty(property.getName(), value);
            value.setParent(targetProperty);
            return targetProperty;
        }
    }

    private void processPrimaryTypeAndMixins(final Node sourceNode, final DefinitionNodeImpl definitionNode) throws RepositoryException {

        final Property primaryTypeProperty = sourceNode.getProperty(JCR_PRIMARYTYPE);
        final ValueImpl value = valueFrom(primaryTypeProperty, definitionNode);
        definitionNode.addProperty(primaryTypeProperty.getName(), value);

        final NodeType[] mixinNodeTypes = sourceNode.getMixinNodeTypes();
        if (mixinNodeTypes.length > 0) {
            final List<ValueImpl> values = new ArrayList<>();
            for (final NodeType mixinNodeType : mixinNodeTypes) {
                values.add(new ValueImpl(mixinNodeType.getName()));
            }
            definitionNode.addProperty(JCR_MIXINTYPES, ValueType.STRING, values.toArray(new ValueImpl[values.size()]));
        }
    }


    /**
     * Generate appropriate Definitions and DefinitionNodes within configSource to represent the difference between
     * the JCR state and the model state for jcrPath and all descendants. In the normal case, this will result in a new
     * definition for each changed Node. However, if a SNS exists anywhere within jcrPath, the definition must have a
     * root above the node with SNSs.
     * @param session
     * @param jcrPath
     * @param configSource
     * @param model
     * @throws RepositoryException
     */
    // TODO
    public void exportConfigNode(final Session session, final String jcrPath, final ConfigSourceImpl configSource,
                                 final ConfigurationModelImpl model) throws RepositoryException, IOException {

        // first, check if we should be looking at this node at all
        final ConfigurationItemCategory category = getCategoryForNode(jcrPath, model);
        if (category != ConfigurationItemCategory.CONFIG) {
            log.debug("Ignoring node because of category:{} \n\t{}", category, jcrPath);
            return;
        }

        // if the jcrPath doesn't exist, we need a delete definition, and that's all
        // todo: do we really need to check every path segment?
        if (!session.nodeExists(jcrPath)) {
            log.debug("Deleting node: \n\t{}", jcrPath);
            final DefinitionNodeImpl definitionNode = getOrCreateDefinition(jcrPath, configSource);
            definitionNode.delete();
            return;
        }

        final Node jcrNode = session.getNode(jcrPath);
        final ConfigurationNodeImpl configNode = model.resolveNode(jcrPath);
        if (configNode == null) {
            // this is a brand new node, so we can skip the delta comparisons and just dump the JCR tree into one def
            log.debug("Creating new node def without delta: \n\t{}", jcrPath);

            final DefinitionNodeImpl definitionNode = getOrCreateDefinition(jcrPath, configSource);

            // TODO: respect auto-export ignores and config categories
            // TODO: what if the last-defined ancestor has a runtime or content category?
            processProperties(jcrNode, definitionNode);
            for (final Node childNode : new NodeIterable(jcrNode.getNodes())) {
                exportNode(childNode, definitionNode);
            }
        }
        else {
            // otherwise, we need to do a detailed comparison
            exportConfigNodeDelta(jcrNode, configNode, configSource, model);
        }
    }

//        ConfigurationNodeImpl configNode = model.getConfigurationRootNode();
//        Node jcrNode = session.getRootNode();
//
//        String[] pathSegments = jcrPath.substring(1).split("/");
//
//        if (jcrPath.equals("/")) {
//            pathSegments = new String[0];
//        }
//
//        // scan path segments until we find
//        // 1. an indexed node, indicating a SNS issue
//        // 2. a node that doesn't exist in the JCR, so we need to mark it as delete
//        // 3. a node that doesn't exist in the model, so we can define it and all children without a delta comparison
//        // 4. the end of the path
//        boolean deletedNode = false;
//        int pathIndex = 0;
//        for (; pathIndex < pathSegments.length; pathIndex++) {
//            final String childName = pathSegments[pathIndex];
//
//            if (childName.contains("[")) {
//                // we cannot create a definition path for an indexed name
//                break;
//            }
//
//            final String indexedChildName = createIndexedName(childName);
//            if (!jcrNode.hasNode(indexedChildName)) {
//                deletedNode = true;
//                break;
//            }
//
//            configNode = configNode.getNode(indexedChildName);
//            if (configNode == null) {
//                // not a delta: no config defined yet
//                break;
//            }
//
//            jcrNode = jcrNode.getNode(indexedChildName);
//        }
//        final String definitionNodePath = jcrNode.getPath() + (deletedNode ? "/" + pathSegments[pathIndex] : "");
//
//        DefinitionNodeImpl definitionNode = getOrCreateConfigDefinitionNodeForPath(definitionNodePath, configSource);
//        log.debug("Using definitionNode: {}", definitionNode.getPath());
//
//        if (deletedNode) {
//            log.debug("Deleting: {}", definitionNode.getPath());
//            definitionNode.setDelete(true);
//            return;
//        }
//
//        // TODO: this seems to largely duplicate the logic in the loop above. can we simplify?
//        if (configNode != null) {
//            for (; pathIndex < pathSegments.length; pathIndex++) {
//                final String childName = pathSegments[pathIndex];
//
//                DefinitionNodeImpl childDefNode = definitionNode.getNodes().get(childName);
//                if (childDefNode == null) {
//                    childDefNode = definitionNode.addNode(childName);
//                }
//                definitionNode = childDefNode;
//
//                final String indexedChildName = createIndexedName(childName);
//                if (!jcrNode.hasNode(indexedChildName)) {
//                    deletedNode = true;
//                    break;
//                }
//
//                configNode = configNode.getNode(indexedChildName);
//                if (configNode == null) {
//                    // not a delta: no config defined yet
//                    break;
//                }
//            }
//        }
//        if (deletedNode) {
//            log.debug("Deleting: {}", definitionNode.getPath());
//            definitionNode.setDelete(true);
//            return;
//        }
//
//        if (configNode == null) {
//            // this is a brand new node, so we can skip the delta comparisons and just dump the JCR version into a def
//            log.debug("Creating new node def without delta: {}", definitionNode.getPath());
//            processProperties(jcrNode, definitionNode);
//            for (final Node childNode : new NodeIterable(jcrNode.getNodes())) {
//                exportNode(childNode, definitionNode);
//            }
//        }
//        else {
//            // otherwise, we need to do a detailed comparison
//            exportConfigNodeDelta(jcrNode, definitionNode, configNode);
//        }
//        if (definitionNode.isEmpty()) {
//            // TODO: remove empty delta definitionNode as the change seemingly was a false positive
//        }
//
//    private DefinitionNodeImpl getOrCreateConfigDefinitionNodeForPath(final String path,
//                                                                      final ConfigSourceImpl configSource) {
//        ConfigDefinitionImpl definition = null;
//        for (AbstractDefinitionImpl def : configSource.getModifiableDefinitions()) {
//            if (def.getType() == DefinitionType.CONFIG) {
//                ConfigDefinitionImpl configDef = (ConfigDefinitionImpl)def;
//                if (path.startsWith(configDef.getNode().getPath())) {
//                    if (definition == null) {
//                        definition = configDef;
//                    } else if (configDef.getNode().getPath().length() > definition.getNode().getPath().length()) {
//                        definition = configDef;
//                    }
//                }
//            }
//        }
//        if (definition == null) {
//            definition = configSource.addConfigDefinition();
//            definition.setNode(new DefinitionNodeImpl(path, StringUtils.substringAfterLast(path, "/"), definition));
//        }
//        // TODO: it seems that it would be possible for this node to not correspond to the supplied path param
//        // TODO: why would we want to allow that?  if we don't, this implementation can be much simpler
//        return definition.getNode();
//    }

    /**
     * Get or create a definition in configSource to contain data for jcrPath. This may need to create a definition
     * for an ancestor of jcrPath in order to comply with the requirement that indexed paths may not be used as a
     * definition root.
     * @param jcrPath the path for which we want a definition
     * @param configSource the source in which we want a definition
     * @return a DefinitionNodeImpl corresponding to the jcrPath, which may or may not be a root
     */
    private DefinitionNodeImpl getOrCreateDefinition(final String jcrPath, final ConfigSourceImpl configSource) {
        final String[] pathSegments = jcrPath.substring(1).split("/");

        // default to the full path, unless we find a SNS index that we need to deal with
        String defRoot = jcrPath;
        List<String> remainder = Collections.emptyList();

        // scan the path segments for a SNS index
        for (int idx = 0; idx < pathSegments.length; idx++) {
            final String name = pathSegments[idx];

            if (name.contains("[")) {
                // if we find one, back up to the parent and use that as the def root
                defRoot = String.join("/", Arrays.asList(pathSegments).subList(0, idx));
                remainder = Arrays.asList(pathSegments).subList(idx, pathSegments.length);
                break;
            }
        }

        // try to find an existing definition for defRoot
        for (AbstractDefinitionImpl def : configSource.getModifiableDefinitions()) {
            if (def.getType().equals(DefinitionType.CONFIG)) {
                final ConfigDefinitionImpl configDef = (ConfigDefinitionImpl) def;
                if (configDef.getModifiableNode().getPath().equals(defRoot)) {
                    // if we find one, then walk down the remainder to the node we really want
                    return buildRemainderNodes(configDef.getModifiableNode(), remainder);
                }
            }
        }

        // if we haven't returned yet, we didn't find a matching def for defRoot
        // build a new def and any required descendant nodes
        final DefinitionNodeImpl defNode = configSource.addConfigDefinition().setRoot(defRoot);
        return buildRemainderNodes(defNode, remainder);
    }

    /**
     * Helper for {@link #getOrCreateDefinition(String, ConfigSourceImpl)} -- builds out nodes under configDef as
     * necessary.
     * @param node starting node (typically a configDef root)
     * @param remainder possibly-empty list of path segments needed below node
     * @return
     */
    private DefinitionNodeImpl buildRemainderNodes(DefinitionNodeImpl node, final List<String> remainder) {
        for (final String segment : remainder) {
            if (node.getNodes().containsKey(segment)) {
                node = node.addNode(segment);
            }
            else {
                node = node.getNode(segment);
            }
        }
        if (node == null) {
            log.error("Produced a null result for path: {}!",
                    node.getPath()+"/"+String.join("/", remainder.toArray(new String[0])),
                    new IllegalStateException());
        }
        return node;
    }

    private DefinitionNodeImpl exportPropertiesDelta(final Node jcrNode,
                                                     final ConfigurationNodeImpl configNode,
                                                     final ConfigSourceImpl configSource, final ConfigurationModelImpl model) throws RepositoryException, IOException {

        DefinitionNodeImpl defNode = exportPrimaryTypeDelta(jcrNode, configNode, configSource);
        defNode = exportMixinsDelta(jcrNode, defNode, configNode, configSource);

        for (final Property jcrProperty : new PropertyIterable(jcrNode.getProperties())) {

            final String propName = jcrProperty.getName();
            if (propName.equals(JCR_PRIMARYTYPE) || propName.equals(JCR_MIXINTYPES)) {
                continue;
            }
            if (isKnownDerivedPropertyName(propName)) {
                continue;
            }
            if (configNode.getChildPropertyCategory(propName) != ConfigurationItemCategory.CONFIG) {
                // skip RUNTIME property
                continue;
            }
            ConfigurationPropertyImpl configProperty = configNode.getProperty(propName);
            if (configProperty == null) {
                // full export
                defNode = createDefNodeIfNecessary(defNode, jcrNode, configSource);
                exportProperty(jcrProperty, defNode);
            } else {
                // delta export
                defNode = exportPropertyDelta(jcrProperty, configProperty, defNode, configSource);
            }
        }

        // delete removed properties
        for (final String configProperty : configNode.getProperties().keySet()) {
            if (!jcrNode.hasProperty(configProperty)) {
                defNode = createDefNodeIfNecessary(defNode, jcrNode, configSource);
                defNode.addProperty(configProperty, null).setOperation(PropertyOperation.DELETE);
            }
        }
        return defNode;
    }

    private DefinitionNodeImpl exportPrimaryTypeDelta(final Node jcrNode,
                                        final ConfigurationNodeImpl configNode,
                                        final ConfigSourceImpl configSource) throws RepositoryException {
        final String configPrimaryType = configNode.getProperty(JCR_PRIMARYTYPE).getValue().getString();
        if (!jcrNode.getPrimaryNodeType().getName().equals(configPrimaryType)) {
            final DefinitionNodeImpl defNode = getOrCreateDefinition(jcrNode.getPath(), configSource);
            final Property primaryTypeProperty = jcrNode.getProperty(JCR_PRIMARYTYPE);
            final ValueImpl value = valueFrom(primaryTypeProperty, defNode);
            defNode.addProperty(primaryTypeProperty.getName(), value).setOperation(PropertyOperation.OVERRIDE);
            return defNode;
        }
        else {
            return null;
        }
    }

    private DefinitionNodeImpl exportMixinsDelta(final Node jcrNode, DefinitionNodeImpl definitionNode,
                                   final ConfigurationNodeImpl configNode, final ConfigSourceImpl configSource) throws RepositoryException {
        final ConfigurationPropertyImpl mixinsProperty = configNode.getProperty(JCR_MIXINTYPES);
        final NodeType[] mixinNodeTypes = jcrNode.getMixinNodeTypes();
        if (mixinNodeTypes.length > 0) {
            final Set<String> jcrMixins = new HashSet<>();
            for (final NodeType mixinNodeType : mixinNodeTypes) {
                jcrMixins.add(mixinNodeType.getName());
            }
            PropertyOperation op = null;
            if (mixinsProperty != null) {
                if (Arrays.stream(mixinsProperty.getValues()).anyMatch(v -> !jcrMixins.contains(v.getString()))) {
                    op = PropertyOperation.OVERRIDE;
                } else {
                    Arrays.stream(mixinsProperty.getValues()).forEach(v->jcrMixins.remove(v.getString()));
                    if (!jcrMixins.isEmpty()) {
                        op = PropertyOperation.ADD;
                    }
                }
            }
            if (!jcrMixins.isEmpty()) {
                definitionNode = createDefNodeIfNecessary(definitionNode, jcrNode, configSource);
                DefinitionPropertyImpl propertyDef = definitionNode.addProperty(JCR_MIXINTYPES,
                        ValueType.STRING,jcrMixins.stream().map(ValueImpl::new).toArray(ValueImpl[]::new));
                if (op != null) {
                    propertyDef.setOperation(op);
                }
            }
        } else if (mixinsProperty != null) {
            definitionNode = createDefNodeIfNecessary(definitionNode, jcrNode, configSource);
            definitionNode.addProperty(JCR_MIXINTYPES, ValueType.STRING, new ValueImpl[0])
                    .setOperation(PropertyOperation.DELETE);
        }
        return definitionNode;
    }

    private DefinitionNodeImpl createDefNodeIfNecessary(final DefinitionNodeImpl definitionNode,
                                                        final Node jcrNode,
                                                        final ConfigSourceImpl configSource) throws RepositoryException {
        if (definitionNode == null) {
            return getOrCreateDefinition(jcrNode.getPath(), configSource);
        }
        else {
            return definitionNode;
        }
    }

    private DefinitionNodeImpl exportPropertyDelta(final Property property, final ConfigurationPropertyImpl configProperty,
                                     DefinitionNodeImpl definitionNode, final ConfigSourceImpl configSource)
            throws RepositoryException, IOException {
        // export property delta
        if (!ValueProcessor.propertyIsIdentical(property, configProperty)) {
            definitionNode = createDefNodeIfNecessary(definitionNode, property.getParent(), configSource);

            // todo: handle references properly
            // todo: preserve values where possible
            // todo: preserve resource path hints
            // todo: use add operation where possible
            final DefinitionPropertyImpl defProp = exportProperty(property, definitionNode);

            // use override operation where necessary
            if ((property.isMultiple() != configProperty.isMultiple())
                    || (ValueType.fromJcrType(property.getType()) != configProperty.getValueType())) {
                defProp.setOperation(PropertyOperation.OVERRIDE);
            }
        }
        return definitionNode;
    }

    private void exportConfigNodeDelta(final Node jcrNode, final ConfigurationNodeImpl configNode,
                                       final ConfigSourceImpl configSource, final ConfigurationModelImpl model)
            throws RepositoryException, IOException {

        log.debug("Building delta for node: \n\t{}", jcrNode.getPath());

        // first look at properties, since that's the simple case
        DefinitionNodeImpl defNode = exportPropertiesDelta(jcrNode, configNode, configSource, model);

        // check if we need to add children
        // TODO: handle ordering changes
        // TODO: handle SNS properly
        for (final Node childNode : new NodeIterable(jcrNode.getNodes())) {
            final ConfigurationItemCategory category = configNode.getChildNodeCategory(createIndexedName(childNode));
            if (category != ConfigurationItemCategory.CONFIG) {
                log.debug("Ignoring child node because of category:{} \n\t{}", category, childNode.getPath());
                break;
            }

            final ConfigurationNodeImpl childConfigNode = configNode.getNode(createIndexedName(childNode));
            if (childConfigNode == null) {
                // the config doesn't know about this child, so do a full export without delta comparisons
                // yes, defNode is indeed supposed to be the _parent's_ defNode
                defNode = createDefNodeIfNecessary(defNode, jcrNode, configSource);
                exportNode(childNode, defNode);
            }
            else {
                // call top-level recursion, not this method
                exportConfigNode(childNode.getSession(), childNode.getPath(), configSource, model);
            }
        }

        // check if we need to delete children
        for (final String childConfigNode : configNode.getNodes().keySet()) {
            final ConfigurationItemCategory category = configNode.getChildNodeCategory(childConfigNode);
            if (category != ConfigurationItemCategory.CONFIG) {
                log.debug("Ignoring child node because of category:{} \n\t{}", category, configNode+"/"+childConfigNode);
                break;
            }

            if (!jcrNode.hasNode(childConfigNode)) {
                final DefinitionNodeImpl childNode =
                        getOrCreateDefinition(String.join("/", configNode.getPath(), childConfigNode), configSource);
                if (childNode == null) {
                    log.error("Produced a null result for path: {}!",
                            jcrNode.getPath()+"/"+childConfigNode,
                            new IllegalStateException());
                }
                else {
                    childNode.delete();
                }
            }
        }
    }

    /**
     * Append definition node using specified action strategy
     *
     * @param definitionNode
     * @param actionType
     * @throws RepositoryException
     */
    public synchronized void apply(final DefinitionNode definitionNode, final ActionType actionType, final Session session) throws RepositoryException {
        if (actionType == null) {
            throw new IllegalArgumentException("Action type cannot be null");
        }

        try {
            validateAppendAction(definitionNode.getPath(), actionType, session);
            if (actionType == DELETE && !session.nodeExists(definitionNode.getPath())) {
                return;
            }

            final Node parentNode = calculateParentNode(definitionNode, session);
            applyNode(definitionNode, parentNode, actionType);
            applyUnprocessedReferences();
        } catch (Exception e) {
            log.error(String.format("Content definition processing failed: %s", definitionNode.getName()), e);
            if (e instanceof RepositoryException) {
                throw (RepositoryException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private Node calculateParentNode(DefinitionNode definitionNode, final Session session) throws RepositoryException {
        String path = Paths.get(definitionNode.getPath()).getParent().toString();
        return session.getNode(path);
    }

    private void applyNode(final DefinitionNode definitionNode, final Node parentNode, final ActionType actionType) throws RepositoryException, IOException {

        final Session session = parentNode.getSession();
        final String nodePath = constructNodePath(parentNode.getPath(), definitionNode.getName());
        final boolean nodeExists = session.nodeExists(nodePath);

        if (nodeExists) {
            switch (actionType) {
                case APPEND:
                    //Happens only in case when subnode is autocreated
                case RELOAD:
                    session.getNode(nodePath).remove();
                    break;
                case DELETE:
                    session.getNode(nodePath).remove();
                    return;
                default:
                    throw new IllegalArgumentException(String.format("Action type '%s' is not supported", actionType));
            }
        }

        final Node jcrNode = addNode(parentNode, definitionNode);
        applyProperties(definitionNode, jcrNode);
        applyChildNodes(definitionNode, jcrNode, actionType);
    }

    private void applyChildNodes(final DefinitionNode modelNode, final Node jcrNode, final ActionType actionType) throws RepositoryException, IOException {
        log.debug(String.format("processing node '%s' defined in %s.", modelNode.getPath(), modelNode.getOrigin()));
        for (final String name : modelNode.getNodes().keySet()) {
            final DefinitionNode modelChild = modelNode.getNode(name);
            applyNode(modelChild, jcrNode, actionType);
        }
    }

    /**
     * Adding a child node with optionally a configured jcr:uuid
     * <p>
     * If a configured uuid already is in use, a warning will be logged and a new jcr:uuid will be generated instead.
     * </p>
     *
     * @param parentNode the parent node for the child node
     * @param modelNode  the configuration for the child node
     * @return the new JCR Node
     * @throws Exception
     */
    private Node addNode(final Node parentNode, final DefinitionNode modelNode) throws RepositoryException {
        final String name = SnsUtils.getUnindexedName(modelNode.getName());
        final String primaryType = getPrimaryType(modelNode);
        final DefinitionProperty uuidProperty = modelNode.getProperty(JCR_UUID);
        if (uuidProperty != null) {
            final String uuid = uuidProperty.getValue().getString();
            if (!isUuidInUse(uuid, parentNode.getSession())) {
                // uuid not in use: create node with the requested uuid
                final NodeImpl parentNodeImpl = (NodeImpl) NodeDecorator.unwrap(parentNode);
                return parentNodeImpl.addNodeWithUuid(name, primaryType, uuid);
            }
            log.warn(String.format("Specified jcr:uuid %s for node '%s' defined in %s already in use: "
                            + "a new jcr:uuid will be generated instead.",
                    uuid, modelNode.getPath(), modelNode.getOrigin()));
        }
        // create node with a new uuid
        return parentNode.addNode(name, primaryType);
    }

    private String getPrimaryType(final DefinitionNode modelNode) {
        if (modelNode.getProperty(JCR_PRIMARYTYPE) == null) {
            final String msg = String.format(
                    "Failed to process node '%s' defined in %s: cannot add child node '%s': %s property missing.",
                    modelNode.getPath(), modelNode.getOrigin(), modelNode.getPath(), JCR_PRIMARYTYPE);
            throw new RuntimeException(msg);
        }

        return modelNode.getProperty(JCR_PRIMARYTYPE).getValue().getString();
    }

    private void applyProperties(final DefinitionNode source, final Node targetNode) throws RepositoryException, IOException {
        applyPrimaryAndMixinTypes(source, targetNode);

        for (DefinitionProperty modelProperty : source.getProperties().values()) {
            if (isReferenceTypeProperty(modelProperty)) {
                unprocessedReferences.add(Pair.of(modelProperty, targetNode));
            } else {
                applyProperty(modelProperty, targetNode);
            }
        }
    }

    private void applyPrimaryAndMixinTypes(final DefinitionNode source, final Node target) throws RepositoryException {
        final List<String> jcrMixinTypes = Arrays.stream(target.getMixinNodeTypes())
                .map(NodeType::getName)
                .collect(Collectors.toList());

        final List<String> modelMixinTypes = new ArrayList<>();
        final DefinitionProperty modelProperty = source.getProperty(JCR_MIXINTYPES);
        if (modelProperty != null) {
            for (Value value : modelProperty.getValues()) {
                modelMixinTypes.add(value.getString());
            }
        }

        for (String modelMixinType : modelMixinTypes) {
            if (jcrMixinTypes.contains(modelMixinType)) {
                jcrMixinTypes.remove(modelMixinType);
            } else {
                target.addMixin(modelMixinType);
            }
        }

        final String modelPrimaryType = source.getProperty(JCR_PRIMARYTYPE).getValue().getString();
        final String jcrPrimaryType = target.getPrimaryNodeType().getName();
        if (!jcrPrimaryType.equals(modelPrimaryType)) {
            target.setPrimaryType(modelPrimaryType);
        }

        for (String mixinType : jcrMixinTypes) {
            target.removeMixin(mixinType);
        }
    }

    private void applyProperty(final DefinitionProperty modelProperty, final Node jcrNode) throws RepositoryException, IOException {
        final Property jcrProperty = JcrUtils.getPropertyIfExists(jcrNode, modelProperty.getName());

        if (jcrProperty != null && jcrProperty.getDefinition().isProtected()) {
            return;
        }

        if (isKnownDerivedPropertyName(modelProperty.getName())) {
            return;
        }

        final List<Value> modelValues = new ArrayList<>();
        if (modelProperty.getType() == PropertyType.SINGLE) {
            collectVerifiedValue(modelProperty, modelProperty.getValue(), modelValues, jcrNode.getSession());
        } else {
            for (Value value : modelProperty.getValues()) {
                collectVerifiedValue(modelProperty, value, modelValues, jcrNode.getSession());
            }
        }

        try {
            if (modelProperty.getType() == PropertyType.SINGLE) {
                if (modelValues.size() > 0) {
                    jcrNode.setProperty(modelProperty.getName(), valueFrom(modelValues.get(0), jcrNode.getSession()));
                }
            } else {
                jcrNode.setProperty(modelProperty.getName(), valuesFrom(modelValues, jcrNode.getSession()));
            }
        } catch (RepositoryException e) {
            String msg = String.format(
                    "Failed to process property '%s' defined in %s: %s",
                    modelProperty.getPath(), modelProperty.getOrigin(), e.getMessage());
            throw new RuntimeException(msg, e);
        }
    }

    private void applyUnprocessedReferences() throws Exception {
        for (Pair<DefinitionProperty, Node> unprocessedReference : unprocessedReferences) {
            final DefinitionProperty DefinitionProperty = unprocessedReference.getLeft();
            final Node jcrNode = unprocessedReference.getRight();
            applyProperty(DefinitionProperty, jcrNode);
        }
    }
}
