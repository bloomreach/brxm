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
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.core.NodeImpl;
import org.hippoecm.repository.decorating.NodeDecorator;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.cm.model.ActionType;
import org.onehippo.cm.model.ContentDefinition;
import org.onehippo.cm.model.DefinitionNode;
import org.onehippo.cm.model.DefinitionProperty;
import org.onehippo.cm.model.Module;
import org.onehippo.cm.model.PropertyType;
import org.onehippo.cm.model.SnsUtils;
import org.onehippo.cm.model.Value;
import org.onehippo.cm.model.impl.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.ContentSourceImpl;
import org.onehippo.cm.model.impl.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.DefinitionPropertyImpl;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.ValueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.JCR_UUID;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATESUMMARY;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PATHS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_RELATED;
import static org.onehippo.cm.model.ActionType.DELETE;
import static org.onehippo.cm.model.ValueType.REFERENCE;
import static org.onehippo.cm.model.ValueType.WEAKREFERENCE;

/**
 * Applies definition nodes to JCR
 */
public class JcrContentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(JcrContentProcessingService.class);

    private final ValueProcessor valueProcessor;
    private final Collection<Pair<DefinitionProperty, Node>> unprocessedReferences = new ArrayList<>();

    private static final String[] knownDerivedPropertyNames = new String[]{
            HIPPO_RELATED,
            HIPPO_PATHS,
            HIPPOSTD_STATESUMMARY
    };

    public JcrContentProcessingService(final ValueProcessor valueProcessor) {
        this.valueProcessor = valueProcessor;
    }

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
        applyNode(modelNode, parentNode, actionType);
    }

    /**
     * Export specified node
     *
     * @param node
     * @return
     */
    public synchronized Module exportNode(final Node node) throws RepositoryException {
        final ModuleImpl module = new ModuleImpl("export-module", new ProjectImpl("export-project", new GroupImpl("export-group")));
        final ContentSourceImpl contentSource = module.addContentSource("content.yaml");
        final ContentDefinitionImpl contentDefinition = contentSource.addContentDefinition();

        final DefinitionNodeImpl definitionNode = new DefinitionNodeImpl(node.getPath(), createNodeName(node), contentDefinition);
        contentDefinition.setNode(definitionNode);

        processProperties(node, definitionNode);

        for (final Node childNode : new NodeIterable(node.getNodes())) {
            exportNode(childNode, definitionNode);
        }

        return module;
    }

    private void exportNode(final Node sourceNode, final DefinitionNodeImpl parentNode) throws RepositoryException {

        final DefinitionNodeImpl definitionNode = parentNode.addNode(createNodeName(sourceNode));

        processProperties(sourceNode, definitionNode);

        for (final Node childNode : new NodeIterable(sourceNode.getNodes())) {
            exportNode(childNode, definitionNode);
        }
    }

    private String createNodeName(final Node sourceNode) throws RepositoryException {
        return sourceNode.getName(); //TODO SS deal with SNS SnsUtils.createIndexedName(sourceNode);
    }

    private void processProperties(final Node sourceNode, final DefinitionNodeImpl definitionNode) throws RepositoryException {
        for (final Property property : new PropertyIterable(sourceNode.getProperties())) {
            if (property.isMultiple()) {
                //TODO SS: process multiple value property
            } else {
                final ValueImpl value = valueProcessor.valueFrom(property.getValue());
                final DefinitionPropertyImpl targetProperty = definitionNode.addProperty(property.getName(), value);
                value.setParent(targetProperty);
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
            applyNode(definitionNode, actionType, session);
            applyUnprocessedReferences();
        } catch (Exception e) {
            logger.error(String.format("Content definition processing failes: %s", definitionNode.getName()), e);
            if (e instanceof RepositoryException) {
                throw (RepositoryException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private void applyNode(final DefinitionNode definitionNode, final ActionType actionType, final Session session) throws Exception {
        if (itemToDeleteDoesNotExist(definitionNode, actionType, session)) {
            return;
        }
        final Node parentNode = calculateRootNode(definitionNode, session);
        applyNode(definitionNode, parentNode, actionType);
    }

    private boolean itemToDeleteDoesNotExist(final DefinitionNode definitionNode,
                                             final ActionType actionType,
                                             final Session session) throws RepositoryException {
        return actionType == DELETE && !session.nodeExists(definitionNode.getPath());
    }

    private Node calculateRootNode(DefinitionNode definitionNode, final Session session) throws RepositoryException {
        String path = Paths.get(definitionNode.getPath()).getParent().toString();
        return session.getNode(path);
    }

    private void applyNode(final DefinitionNode definitionNode, final Node parentNode, final ActionType actionType) throws RepositoryException, IOException {

        final Session session = parentNode.getSession();
        final String nodePath = definitionNode.getPath();
        final boolean nodeExists = session.nodeExists(nodePath);

        if (nodeExists) {
            switch (actionType) {
                case APPEND:
                    throw new ItemExistsException(String.format("Node already exists at path %s", nodePath));
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
        logger.debug(String.format("processing node '%s' defined in %s.", modelNode.getPath(), modelNode.getOrigin()));
        for (final String name : modelNode.getNodes().keySet()) {
            final DefinitionNode modelChild = modelNode.getNodes().get(name);
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
        final DefinitionProperty uuidProperty = modelNode.getProperties().get(JCR_UUID);
        if (uuidProperty != null) {
            final String uuid = uuidProperty.getValue().getString();
            if (!isUuidInUse(uuid, parentNode.getSession())) {
                // uuid not in use: create node with the requested uuid
                final NodeImpl parentNodeImpl = (NodeImpl) NodeDecorator.unwrap(parentNode);
                return parentNodeImpl.addNodeWithUuid(name, primaryType, uuid);
            }
            logger.warn(String.format("Specified jcr:uuid %s for node '%s' defined in %s already in use: "
                            + "a new jcr:uuid will be generated instead.",
                    uuid, modelNode.getPath(), modelNode.getOrigin()));
        }
        // create node with a new uuid
        return parentNode.addNode(name, primaryType);
    }

    private boolean isUuidInUse(final String uuid, final Session session) throws RepositoryException {
        try {
            session.getNodeByIdentifier(uuid);
            return true;
        } catch (ItemNotFoundException e) {
            return false;
        }
    }

    private String getPrimaryType(final DefinitionNode modelNode) {
        if (!modelNode.getProperties().containsKey(JCR_PRIMARYTYPE)) {
            final String msg = String.format(
                    "Failed to process node '%s' defined in %s: cannot add child node '%s': %s property missing.",
                    modelNode.getPath(), modelNode.getOrigin(), modelNode.getPath(), JCR_PRIMARYTYPE);
            throw new RuntimeException(msg);
        }

        return modelNode.getProperties().get(JCR_PRIMARYTYPE).getValue().getString();
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
        final DefinitionProperty modelProperty = source.getProperties().get(JCR_MIXINTYPES);
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

        final String modelPrimaryType = source.getProperties().get(JCR_PRIMARYTYPE).getValue().getString();
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
            if (modelValues.size() > 0) {
                if (modelProperty.getType() == PropertyType.SINGLE) {
                    jcrNode.setProperty(modelProperty.getName(), valueProcessor.valueFrom(modelValues.get(0), jcrNode.getSession()));
                } else {
                    jcrNode.setProperty(modelProperty.getName(), valueProcessor.valuesFrom(modelValues, jcrNode.getSession()));
                }
            }
        } catch (RepositoryException e) {
            String msg = String.format(
                    "Failed to process property '%s' defined in %s: %s",
                    modelProperty.getPath(), modelProperty.getOrigin(), e.getMessage());
            throw new RuntimeException(msg, e);
        }
    }

    private void collectVerifiedValue(final DefinitionProperty modelProperty, final Value value, final List<Value> modelValues,
                                      final Session session)
            throws RepositoryException {
        if (isReferenceTypeProperty(modelProperty)) {
            final String uuid = getVerifiedReferenceIdentifier(modelProperty, value, session);
            if (uuid != null) {
                modelValues.add(new VerifiedReferenceValue(value, uuid));
            }
        } else {
            modelValues.add(value);
        }
    }

    private String getVerifiedReferenceIdentifier(final DefinitionProperty modelProperty,
                                                  final Value modelValue,
                                                  final Session session)
            throws RepositoryException {
        String identifier = modelValue.getString();
        if (modelValue.isPath()) {
            String nodePath = identifier;
            if (!nodePath.startsWith("/")) {
                // path reference is relative to content definition root path
                final String rootPath = ((ContentDefinition) modelValue.getParent().getDefinition()).getNode().getPath();
                final StringBuilder pathBuilder = new StringBuilder(rootPath);
                if (!StringUtils.EMPTY.equals(nodePath)) {
                    if (!"/".equals(rootPath)) {
                        pathBuilder.append("/");
                    }
                    pathBuilder.append(nodePath);
                }
                nodePath = pathBuilder.toString();
            }
            // lookup node identifier by node path
            try {
                identifier = session.getNode(nodePath).getIdentifier();
            } catch (PathNotFoundException e) {
                logger.warn(String.format("Path reference '%s' for property '%s' defined in %s not found: skipping.",
                        nodePath, modelProperty.getPath(), modelProperty.getOrigin()));
                return null;
            }
        } else {
            try {
                session.getNodeByIdentifier(identifier);
            } catch (ItemNotFoundException e) {
                logger.warn(String.format("Reference %s for property '%s' defined in %s not found: skipping.",
                        identifier, modelProperty.getPath(), modelProperty.getOrigin()));
                return null;
            }
        }
        return identifier;
    }


    private boolean isReferenceTypeProperty(final DefinitionProperty modelProperty) {
        return (modelProperty.getValueType() == REFERENCE || modelProperty.getValueType() == WEAKREFERENCE);
    }

    private boolean isKnownDerivedPropertyName(final String modelPropertyName) {
        return ArrayUtils.contains(knownDerivedPropertyNames, modelPropertyName);
    }

    private void applyUnprocessedReferences() throws Exception {
        for (Pair<DefinitionProperty, Node> unprocessedReference : unprocessedReferences) {
            final DefinitionProperty DefinitionProperty = unprocessedReference.getLeft();
            final Node jcrNode = unprocessedReference.getRight();
            applyProperty(DefinitionProperty, jcrNode);
        }
    }
}
