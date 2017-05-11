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
package org.onehippo.cm.backend;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jcr.Binary;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.core.NodeImpl;
import org.hippoecm.repository.decorating.NodeDecorator;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.api.model.DefinitionProperty;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.PropertyType;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.api.model.ValueType;
import org.onehippo.cm.engine.SnsUtils;
import org.onehippo.cm.impl.model.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.JCR_UUID;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATESUMMARY;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PATHS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_RELATED;

/**
 * Applies content definitions to JCR module
 */
public class JcrContentProcessingService implements ContentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(JcrContentProcessingService.class);

    private final Session session;
    private final Map<Module, ResourceInputProvider> resourceInputProviders;
    private final List<Pair<DefinitionProperty, Node>> unprocessedReferences = new ArrayList<>();

    private static final String[] knownDerivedPropertyNames = new String[] {
            HIPPO_RELATED,
            HIPPO_PATHS,
            HIPPOSTD_STATESUMMARY
    };

    public JcrContentProcessingService(Session session, Map<Module, ResourceInputProvider> resourceInputProviders) {
        this.session = session;
        this.resourceInputProviders = resourceInputProviders;
    }

    @Override
    public synchronized void apply(final Source source, final List<ContentDefinition> sourceDefinitions) {
        for (ContentDefinition definition : sourceDefinitions) {
            try {
                processContentDefinition(definition.getNode());
            } catch (Exception e) {
                logger.error(String.format("Content definition processing failes: %s", definition.getNode().getName()), e);
                throw new RuntimeException(e);
            }
        }
    }

    private void processContentDefinition(DefinitionNode definitionNode) throws Exception {
        applyNodes(definitionNode);
        applyUnprocessedReferences();
    }

    private void applyNodes(final DefinitionNode definitionNode) throws Exception {


        final Node rootNode = calculateRootNode(definitionNode);

        //todo SS: check if node we're going to create, actually exists

        final Node node = addNode(rootNode, definitionNode);
        applyProperties(definitionNode, node);
        applyNodes(definitionNode, node);
    }

    private Node calculateRootNode(DefinitionNode definitionNode) throws RepositoryException {
        ContentDefinition definition = (ContentDefinition)definitionNode.getDefinition();
        String path = Paths.get(definition.getNode().getPath()).getParent().toString();
        return session.getNode(path);
    }

    private void applyNodes(final DefinitionNode modelNode, final Node jcrNode) throws Exception {
        logger.debug(String.format("processing node '%s' defined in %s.", modelNode.getPath(), ModelUtils.formatDefinition(modelNode.getDefinition())));
        final Map<String, Node> retainedChildren = removeNonModelNodes(modelNode, jcrNode);
        final NextChildNameProvider nextChildNameProvider = new NextChildNameProvider(retainedChildren);
        String nextChildName = nextChildNameProvider.next();

        // iterate over desired list of nodes (in desired order)
        for (String name : modelNode.getNodes().keySet()) {
            final DefinitionNode modelChild = modelNode.getNodes().get(name);

            // create child if necessary
            //todo SS: check if node we're going to create, actually exists
            final Node jcrChild = retainedChildren.containsKey(name)
                    ? retainedChildren.get(name)
                    : addNode(jcrNode, modelChild);

            nextChildNameProvider.ignore(name); // 'name' is consumed.

            // ensure correct ordering
            if (jcrNode.getPrimaryNodeType().hasOrderableChildNodes()) {
                if (name.equals(nextChildName)) {
                    // 'nextChildName' is processed, get new next
                    nextChildName = nextChildNameProvider.next();
                } else {
                    // jcrChild is not at next child position, move it there
                    jcrNode.orderBefore(SnsUtils.createIndexedName(jcrChild), nextChildName);
                }
            }

            applyProperties(modelChild, jcrChild);
            applyNodes(modelChild, jcrChild);
        }
    }

    /**
     * Adding a child node with optionally a configured jcr:uuid
     * <p>
     * If a configured uuid already is in use, a warning will be logged and a new jcr:uuid will be generated instead.
     * </p>
     * @param parentNode the parent node for the child node
     * @param modelNode the configuration for the child node
     * @return the new JCR Node
     * @throws Exception
     */
    private Node addNode(final Node parentNode, final DefinitionNode modelNode) throws RepositoryException {
        final String name = SnsUtils.getUnindexedName(modelNode.getName());
        final String primaryType = getPrimaryType(modelNode);
        final DefinitionProperty uuidProperty = modelNode.getProperties().get(JCR_UUID);
        if (uuidProperty != null) {
            final String uuid = uuidProperty.getValue().getString();
            if (!isUuidInUse(uuid)) {
                // uuid not in use: create node with the requested uuid
                final NodeImpl parentNodeImpl = (NodeImpl) NodeDecorator.unwrap(parentNode);
                return parentNodeImpl.addNodeWithUuid(name, primaryType, uuid);
            }
            logger.warn(String.format("Specified jcr:uuid %s for node '%s' defined in %s already in use: "
                            + "a new jcr:uuid will be generated instead.",
                    uuid, modelNode.getPath(), ModelUtils.formatDefinition(modelNode.getDefinition())));
        }
        // create node with a new uuid
        return parentNode.addNode(name, primaryType);
    }

    private boolean isUuidInUse(final String uuid) throws RepositoryException {
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
                    modelNode.getPath(), ModelUtils.formatDefinition(modelNode.getDefinition()), modelNode.getPath(), JCR_PRIMARYTYPE);
            throw new RuntimeException(msg);
        }

        return modelNode.getProperties().get(JCR_PRIMARYTYPE).getValue().getString();
    }

    private Map<String, Node> removeNonModelNodes(final DefinitionNode modelNode, final Node jcrNode)
            throws RepositoryException {

        //TODO SS: review if it is still needed
        final Map<String, Node> retainedChildren = new LinkedHashMap<>();
        final List<Node> nonModelNodes = new ArrayList<>();

        final NodeIterator iterator = jcrNode.getNodes();
        while (iterator.hasNext()) {
            final Node jcrChild = iterator.nextNode();
            final String indexedName = SnsUtils.createIndexedName(jcrChild);
            if (modelNode.getNodes().containsKey(indexedName)) {
                retainedChildren.put(indexedName, jcrChild);
            } else {
                nonModelNodes.add(jcrChild);
            }
        }

        // remove nodes unknown to the model, except top-level nodes
        if (jcrNode.getDepth() > 0) {
            for (final Node nonModelNode : nonModelNodes) {
                nonModelNode.remove();
            }
        }

        return retainedChildren;
    }

    private void applyProperties(final DefinitionNode source, final Node target) throws Exception {
//        removeNonModelProperties(source, target);

        applyPrimaryAndMixinTypes(source, target);

        for (DefinitionProperty modelProperty : source.getProperties().values()) {
            if (isReferenceTypeProperty(modelProperty)) {
                unprocessedReferences.add(Pair.of(modelProperty, target));
            } else {
                applyProperty(modelProperty, target);
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

    private void removeNonModelProperties(final DefinitionNode source, final Node target) throws RepositoryException {
        final PropertyIterator iterator = target.getProperties();
        while (iterator.hasNext()) {
            final Property property = iterator.nextProperty();
            if (!property.getDefinition().isProtected() && !isKnownDerivedPropertyName(property.getName())) {
                if (!source.getProperties().containsKey(property.getName())) {
                    property.remove();
                }
            }
        }
    }

    private void applyProperty(final DefinitionProperty modelProperty, final Node jcrNode) throws Exception {
        final Property jcrProperty = JcrUtils.getPropertyIfExists(jcrNode, modelProperty.getName());

        if (jcrProperty != null && jcrProperty.getDefinition().isProtected()) {
            return;
        }

        if (isKnownDerivedPropertyName(modelProperty.getName())) {
            return;
        }

        final List<Value> modelValues = new ArrayList<>();
        if (modelProperty.getType() == PropertyType.SINGLE) {
            collectVerifiedValue(modelProperty, modelProperty.getValue(), modelValues);
        } else {
            for (Value value : modelProperty.getValues()) {
                collectVerifiedValue(modelProperty, value, modelValues);
            }
        }

        if (jcrProperty != null) {
            if (isOverride(modelProperty, jcrProperty)) {
                jcrProperty.remove();
            } else if (valuesAreIdentical(modelProperty, modelValues, jcrProperty)) {
                return;
            }
        }
        try {
            if (modelProperty.getType() == PropertyType.SINGLE) {
                if (modelValues.size() > 0) {
                    jcrNode.setProperty(modelProperty.getName(), valueFrom(modelProperty, modelValues.get(0)));
                }
            } else {
                jcrNode.setProperty(modelProperty.getName(), valuesFrom(modelProperty, modelValues));
            }
        } catch (RepositoryException e) {
            String msg = String.format(
                    "Failed to process property '%s' defined in %s: %s",
                    modelProperty.getPath(), ModelUtils.formatDefinition(modelProperty.getDefinition()), e.getMessage());
            throw new RuntimeException(msg, e);
        }
    }

    private void collectVerifiedValue(final DefinitionProperty modelProperty, final Value value, final List<Value> modelValues)
            throws RepositoryException {
        if (isReferenceTypeProperty(modelProperty)) {
            final String uuid = getVerifiedReferenceIdentifier(modelProperty, value);
            if (uuid != null) {
                modelValues.add(new VerifiedReferenceValue(value, uuid));
            }
        } else {
            modelValues.add(value);
        }
    }

    private String getVerifiedReferenceIdentifier(final DefinitionProperty modelProperty, final Value modelValue)
            throws RepositoryException {
        String identifier = modelValue.getString();
        if (modelValue.isPath()) {
            String nodePath = identifier;
            if (!nodePath.startsWith("/")) {
                // path reference is relative to content definition root path
                final String rootPath = ((ContentDefinition) modelValue.getParent().getDefinition()).getNode().getPath();
                final StringBuilder pathBuilder = new StringBuilder(rootPath);
                if (!"".equals(nodePath)) {
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
                        nodePath, modelProperty.getPath(), ModelUtils.formatDefinition(modelProperty.getDefinition())));
                return null;
            }
        } else {
            try {
                session.getNodeByIdentifier(identifier);
            } catch (ItemNotFoundException e) {
                logger.warn(String.format("Reference %s for property '%s' defined in %s not found: skipping.",
                        identifier, modelProperty.getPath(), ModelUtils.formatDefinition(modelProperty.getDefinition())));
                return null;
            }
        }
        return identifier;
    }


    private boolean isReferenceTypeProperty(final DefinitionProperty modelProperty) {
        return (modelProperty.getValueType() == ValueType.REFERENCE ||
                modelProperty.getValueType() == ValueType.WEAKREFERENCE);
    }

    private boolean isKnownDerivedPropertyName(final String modelPropertyName) {
        return ArrayUtils.contains(knownDerivedPropertyNames, modelPropertyName);
    }

    private boolean isOverride(final DefinitionProperty modelProperty,
                               final Property jcrProperty) throws RepositoryException {
        if (modelProperty.getValueType().ordinal() != jcrProperty.getType()) {
            return true;
        }
        if (modelProperty.getType() == PropertyType.SINGLE) {
            return jcrProperty.isMultiple();
        } else {
            return !jcrProperty.isMultiple();
        }
    }

    private javax.jcr.Value[] valuesFrom(final DefinitionProperty modelProperty,
                                         final List<Value> modelValues) throws Exception {
        final javax.jcr.Value[] jcrValues = new javax.jcr.Value[modelValues.size()];
        for (int i = 0; i < jcrValues.length; i++) {
            jcrValues[i] = valueFrom(modelProperty, modelValues.get(i));
        }
        return jcrValues;
    }

    private javax.jcr.Value valueFrom(final DefinitionProperty modelProperty,
                                      final Value modelValue) throws Exception {
        final ValueFactory factory = session.getValueFactory();
        final ValueType type = modelValue.getType();

        switch (type) {
            case STRING:
                return factory.createValue(getStringValue(modelValue));
            case BINARY:
                final Binary binary = factory.createBinary(getBinaryInputStream(modelValue));
                try {
                    return factory.createValue(binary);
                } finally {
                    binary.dispose();
                }
            case LONG:
                return factory.createValue((Long)modelValue.getObject());
            case DOUBLE:
                return factory.createValue((Double)modelValue.getObject());
            case DATE:
                return factory.createValue((Calendar)modelValue.getObject());
            case BOOLEAN:
                return factory.createValue((Boolean)modelValue.getObject());
            case URI:
            case NAME:
            case PATH:
            case REFERENCE:
            case WEAKREFERENCE:
                // REFERENCE and WEAKREFERENCE type values already are resolved to hold a validated uuid
                return factory.createValue(modelValue.getString(), type.ordinal());
            case DECIMAL:
                return factory.createValue((BigDecimal)modelValue.getObject());
            default:
                final String msg = String.format(
                        "Failed to process property '%s' defined in %s: unsupported value type '%s'.",
                        modelProperty.getPath(), ModelUtils.formatDefinition(modelProperty.getDefinition()), type);
                throw new RuntimeException(msg);
        }
    }

    private void applyUnprocessedReferences() throws Exception {
        for (Pair<DefinitionProperty, Node> unprocessedReference : unprocessedReferences) {
            final DefinitionProperty DefinitionProperty = unprocessedReference.getLeft();
            final Node jcrNode = unprocessedReference.getRight();
            applyProperty(DefinitionProperty, jcrNode);
        }
    }

    private String getStringValue(final Value modelValue) throws IOException {
        if (modelValue.isResource()) {
            try (final InputStream inputStream = getResourceInputStream(modelValue)) {
                return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            }
        } else {
            return modelValue.getString();
        }
    }

    private InputStream getBinaryInputStream(final Value modelValue) throws IOException {
        if (modelValue.isResource()) {
            return getResourceInputStream(modelValue);
        } else {
            return new ByteArrayInputStream((byte[]) modelValue.getObject());
        }
    }


    private InputStream getResourceInputStream(final Source source, final String resourceName) throws IOException {
        return resourceInputProviders.get(source.getModule()).getResourceInputStream(source, resourceName);
    }

    private InputStream getResourceInputStream(final Value modelValue) throws IOException {
        return getResourceInputStream(modelValue.getParent().getDefinition().getSource(), modelValue.getString());
    }

    private boolean valuesAreIdentical(final DefinitionProperty modelProperty, final List<Value> modelValues,
                                       final Property jcrProperty) throws Exception {
        if (modelProperty.getType() == PropertyType.SINGLE) {
            if (modelValues.size() > 0) {
                return valueIsIdentical(modelProperty, modelProperty.getValue(), jcrProperty.getValue());
            } else {
                // No modelValue indicates that a reference failed verification (of UUID or path).
                // We leave the current reference (existing or not) unchanged and return true to
                // short-circuit further processing of this modelProperty.
                return true;
            }
        } else {
            final javax.jcr.Value[] jcrValues = jcrProperty.getValues();
            if (modelValues.size() != jcrValues.length) {
                return false;
            }
            for (int i = 0; i < jcrValues.length; i++) {
                if (!valueIsIdentical(modelProperty, modelValues.get(i), jcrValues[i])) {
                    return false;
                }
            }
            return true;
        }
    }

    private boolean valueIsIdentical(final DefinitionProperty modelProperty,
                                     final Value modelValue,
                                     final javax.jcr.Value jcrValue) throws Exception {
        if (modelValue.getType().ordinal() != jcrValue.getType()) {
            return false;
        }

        switch (modelValue.getType()) {
            case STRING:
                return getStringValue(modelValue).equals(jcrValue.getString());
            case BINARY:
                try (final InputStream modelInputStream = getBinaryInputStream(modelValue)) {
                    final Binary jcrBinary = jcrValue.getBinary();
                    try (final InputStream jcrInputStream = jcrBinary.getStream()) {
                        return IOUtils.contentEquals(modelInputStream, jcrInputStream);
                    } finally {
                        jcrBinary.dispose();
                    }
                }
            case LONG:
                return modelValue.getObject().equals(jcrValue.getLong());
            case DOUBLE:
                return modelValue.getObject().equals(jcrValue.getDouble());
            case DATE:
                return modelValue.getObject().equals(jcrValue.getDate());
            case BOOLEAN:
                return modelValue.getObject().equals(jcrValue.getBoolean());
            case URI:
            case NAME:
            case PATH:
            case REFERENCE:
            case WEAKREFERENCE:
                // REFERENCE and WEAKREFERENCE type values already are resolved to hold a validated uuid
                return modelValue.getString().equals(jcrValue.getString());
            case DECIMAL:
                return modelValue.getObject().equals(jcrValue.getDecimal());
            default:
                final String msg = String.format(
                        "Failed to process property '%s' defined in %s: unsupported value type '%s'.",
                        modelProperty.getPath(), ModelUtils.formatDefinition(modelProperty.getDefinition()), modelValue.getType());
                throw new RuntimeException(msg);
        }
    }

    private static class NextChildNameProvider {
        private final Iterator<String> iterator;
        private final Set<String> toBeIgnoredNames = new HashSet<>();

        NextChildNameProvider(final Map<String, Node> childMap) {
            iterator = childMap.keySet().iterator();
        }

        /**
         * @return next child node name
         */
        String next() {
            while (iterator.hasNext()) {
                final String next = iterator.next();
                if (!toBeIgnoredNames.contains(next)) {
                    return next;
                }
            }
            return null;
        }

        void ignore(final String name) {
            toBeIgnoredNames.add(name);
        }
    }


}
