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
import java.util.List;
import java.util.stream.Collectors;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.core.NodeImpl;
import org.hippoecm.repository.decorating.NodeDecorator;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cm.model.ActionType;
import org.onehippo.cm.model.DefinitionNode;
import org.onehippo.cm.model.DefinitionProperty;
import org.onehippo.cm.model.PropertyType;
import org.onehippo.cm.model.Value;
import org.onehippo.cm.model.impl.DefinitionNodeImpl;
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

/**
 * Applies definition nodes to JCR
 */
public class JcrContentProcessor {

    private static final Logger log = LoggerFactory.getLogger(JcrContentProcessor.class);

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

        final Collection<Pair<DefinitionProperty, Node>> unprocessedReferences = new ArrayList<>();
        applyNode(newNode, parentNode, actionType, unprocessedReferences);
        applyUnprocessedReferences(unprocessedReferences);
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
            final Collection<Pair<DefinitionProperty, Node>> unprocessedReferences = new ArrayList<>();
            applyNode(definitionNode, parentNode, actionType, unprocessedReferences);
            applyUnprocessedReferences(unprocessedReferences);
        } catch (Exception e) {
            log.warn(String.format("Content definition processing failed: %s", definitionNode.getName()), e);
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

    private void applyNode(final DefinitionNode definitionNode, final Node parentNode, final ActionType actionType,
                           final Collection<Pair<DefinitionProperty, Node>> unprocessedReferences) throws RepositoryException, IOException {

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
        applyProperties(definitionNode, jcrNode, unprocessedReferences);
        applyChildNodes(definitionNode, jcrNode, actionType, unprocessedReferences);
    }

    private void applyChildNodes(final DefinitionNode modelNode, final Node jcrNode, final ActionType actionType,
                                 final Collection<Pair<DefinitionProperty, Node>> unprocessedReferences) throws RepositoryException, IOException {
        log.debug(String.format("processing node '%s' defined in %s.", modelNode.getPath(), modelNode.getOrigin()));
        for (final String name : modelNode.getNodes().keySet()) {
            final DefinitionNode modelChild = modelNode.getNode(name);
            applyNode(modelChild, jcrNode, actionType, unprocessedReferences);
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

    private void applyProperties(final DefinitionNode source, final Node targetNode,
                                 final Collection<Pair<DefinitionProperty, Node>> unprocessedReferences)
            throws RepositoryException, IOException {
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

    private void applyUnprocessedReferences(final Collection<Pair<DefinitionProperty, Node>> unprocessedReferences)
            throws RepositoryException, IOException {
        for (Pair<DefinitionProperty, Node> unprocessedReference : unprocessedReferences) {
            final DefinitionProperty DefinitionProperty = unprocessedReference.getLeft();
            final Node jcrNode = unprocessedReference.getRight();
            applyProperty(DefinitionProperty, jcrNode);
        }
    }
}
