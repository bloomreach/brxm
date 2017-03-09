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
package org.onehippo.cm.backend;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefReader;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.commons.cnd.TemplateBuilderFactory;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cm.api.MergedModel;
import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.ConfigurationNode;
import org.onehippo.cm.api.model.ConfigurationProperty;
import org.onehippo.cm.api.model.DefinitionItem;
import org.onehippo.cm.api.model.DefinitionProperty;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.NamespaceDefinition;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.api.model.PropertyOperation;
import org.onehippo.cm.api.model.PropertyType;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.api.model.ValueType;
import org.onehippo.cm.impl.model.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.JCR_UUID;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_HOLDER;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_AVAILABILITY;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PATHS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_RELATED;

public class ApplyConfigurationHelper {

    private static final Logger logger = LoggerFactory.getLogger(ApplyConfigurationHelper.class);

    private final Session session;
    private final Map<Module, ResourceInputProvider> resourceInputProviders;
    private final List<Pair<ConfigurationProperty, Node>> unprocessedReferences = new ArrayList<>();

    public ApplyConfigurationHelper(final Session session, final Map<Module, ResourceInputProvider> resourceInputProviders) {
        this.session = session;
        this.resourceInputProviders = resourceInputProviders;
    }

    public void apply(final MergedModel model) throws Exception {
        unprocessedReferences.clear();

        applyNamespaces(model.getNamespaceDefinitions());
        applyNodeTypes(model.getNodeTypeDefinitions());
        applyNodes(model.getConfigurationRootNode());
        applyUnprocessedReferences();
    }

    private void applyNamespaces(final List<? extends NamespaceDefinition> namespaceDefinitions) throws RepositoryException {
        final Set<String> prefixes = new HashSet<>(Arrays.asList(session.getNamespacePrefixes()));

        for (NamespaceDefinition namespaceDefinition : namespaceDefinitions) {
            final String prefix = namespaceDefinition.getPrefix();
            final String uriString = namespaceDefinition.getURI().toString();
            if (prefixes.contains(prefix)) {
                final String repositoryURI = session.getNamespaceURI(prefix);
                if (!uriString.equals(repositoryURI)) {
                    final String msg = String.format(
                            "Failed to process namespace definition defined through %s: namespace with prefix '%s' already exists in repository with different URI. Existing: '%s', from model: '%s'; aborting",
                            ModelUtils.formatDefinition(namespaceDefinition), prefix, repositoryURI, uriString);
                    throw new RuntimeException(msg);
                }
            } else {
                final NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
                namespaceRegistry.registerNamespace(prefix, uriString);
            }
        }
    }

    private void applyNodeTypes(final List<? extends NodeTypeDefinition> nodeTypeDefinitions) throws RepositoryException, ParseException, IOException {
        for (NodeTypeDefinition nodeTypeDefinition : nodeTypeDefinitions) {
            applyNodeType(nodeTypeDefinition);
        }
    }

    private void applyNodeType(final NodeTypeDefinition nodeTypeDefinition) throws RepositoryException, IOException {
        logger.debug(String.format("processing cnd '%s' from %s.", nodeTypeDefinition.getValue(), ModelUtils.formatDefinition(nodeTypeDefinition)));
        final String definitionValue = nodeTypeDefinition.getValue();
        final InputStream cndStream = nodeTypeDefinition.isResource()
                ? getResourceInputStream(nodeTypeDefinition.getSource(), definitionValue)
                : new ByteArrayInputStream(definitionValue.getBytes(StandardCharsets.UTF_8));

        final NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
        final TemplateBuilderFactory factory = new TemplateBuilderFactory(session);

        final CompactNodeTypeDefReader<NodeTypeTemplate, NamespaceRegistry> reader;
        try {
            reader = new CompactNodeTypeDefReader<>(new InputStreamReader(cndStream), "<yaml-reader>",
                    namespaceRegistry, factory);
        } catch (ParseException e) {
            final String msg = String.format("Failed to process CND defined through %s: %s",
                    ModelUtils.formatDefinition(nodeTypeDefinition), e.getMessage());
            throw new RuntimeException(msg, e);
        }

        final List<NodeTypeTemplate> nttList = reader.getNodeTypeDefinitions();
        final NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();

        for (NodeTypeTemplate ntt : nttList) {
            logger.debug(String.format("registering node type '%s'", ntt.getName()));
            nodeTypeManager.registerNodeType(ntt, true);
        }
    }

    private void applyNodes(final ConfigurationNode configurationRoot) throws Exception {
        final Node root = session.getRootNode();
        applyProperties(configurationRoot, root);
        applyNodes(configurationRoot, root);
    }

    private void applyNodes(final ConfigurationNode modelNode, final Node jcrNode) throws Exception {
        logger.debug(String.format("processing node '%s' defined through %s.", modelNode.getPath(), ModelUtils.formatDefinitions(modelNode)));
        final Map<String, Node> retainedChildren = removeNonModelNodes(modelNode, jcrNode);
        final NextChildNameProvider nextChildNameProvider = new NextChildNameProvider(retainedChildren);
        String nextChildName = nextChildNameProvider.next();

        // iterate over desired list of nodes (in desired order)
        for (String name : modelNode.getNodes().keySet()) {
            final ConfigurationNode modelChild = modelNode.getNodes().get(name);

            // create child if necessary
            final Node jcrChild = retainedChildren.containsKey(name)
                    ? retainedChildren.get(name)
                    : jcrNode.addNode(name, getPrimaryType(modelChild));

            nextChildNameProvider.ignore(name); // 'name' is consumed.

            // ensure correct ordering
            if (jcrNode.getPrimaryNodeType().hasOrderableChildNodes()) {
                if (name.equals(nextChildName)) {
                    // 'nextChildName' is processed, get new next
                    nextChildName = nextChildNameProvider.next();
                } else {
                    // jcrChild is not at next child position, move it there
                    jcrNode.orderBefore(jcrChild.getName(), nextChildName);
                }
            }

            applyProperties(modelChild, jcrChild);
            applyNodes(modelChild, jcrChild);
        }
    }

    private String getPrimaryType(final ConfigurationNode modelNode) {
        if (!modelNode.getProperties().containsKey(JCR_PRIMARYTYPE)) {
            final String msg = String.format(
                    "Failed to process node '%s' defined through %s: cannot add child node '%s': %s property missing.",
                    modelNode.getPath(), ModelUtils.formatDefinitions(modelNode), modelNode.getPath(), JCR_PRIMARYTYPE);
            throw new RuntimeException(msg);
        }

        return modelNode.getProperties().get(JCR_PRIMARYTYPE).getValue().getString();
    }

    private Map<String, Node> removeNonModelNodes(final ConfigurationNode modelNode, final Node jcrNode)
            throws RepositoryException {

        final Map<String, Node> retainedChildren = new LinkedHashMap<>();
        final List<Node> nonModelNodes = new ArrayList<>();

        final NodeIterator iterator = jcrNode.getNodes();
        while (iterator.hasNext()) {
            final Node jcrChild = iterator.nextNode();
            if (modelNode.getNodes().containsKey(jcrChild.getName())) {
                retainedChildren.put(jcrChild.getName(), jcrChild);
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

    private void applyProperties(final ConfigurationNode source, final Node target) throws Exception {
        removeNonModelProperties(source, target);

        applyPrimaryAndMixinTypes(source, target);

        for (ConfigurationProperty modelProperty : source.getProperties().values()) {
            if (modelProperty.getValueType() == ValueType.REFERENCE ||
                    modelProperty.getValueType() == ValueType.WEAKREFERENCE) {
                unprocessedReferences.add(Pair.of(modelProperty, target));
            } else {
                applyProperty(modelProperty, target);
            }
        }
    }

    private void removeNonModelProperties(final ConfigurationNode source, final Node target) throws RepositoryException {
        final PropertyIterator iterator = target.getProperties();
        while (iterator.hasNext()) {
            final Property property = iterator.nextProperty();
            if (!property.getDefinition().isProtected()) {
                if (!source.getProperties().containsKey(property.getName())) {
                    property.remove();
                }
            }
        }
    }

    private void applyPrimaryAndMixinTypes(final ConfigurationNode source, final Node target) throws RepositoryException {
        final List<String> jcrMixinTypes = Arrays.stream(target.getMixinNodeTypes())
                .map(NodeType::getName)
                .collect(Collectors.toList());

        final List<String> modelMixinTypes = new ArrayList<>();
        final ConfigurationProperty modelProperty = source.getProperties().get(JCR_MIXINTYPES);
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

    private void applyProperty(final ConfigurationProperty modelProperty, final Node jcrNode) throws Exception {
        final Property jcrProperty = JcrUtils.getPropertyIfExists(jcrNode, modelProperty.getName());

        if (isKnownProtectedPropertyName(modelProperty.getName())) {
            return;
        }

        if (jcrProperty != null) {
            if (isOverride(modelProperty, jcrProperty)) {
                jcrProperty.remove();
            } else if (valuesAreIdentical(modelProperty, jcrNode, jcrProperty)) {
                return;
            }
        }

        try {
            if (modelProperty.getType() == PropertyType.SINGLE) {
                jcrNode.setProperty(modelProperty.getName(), valueFrom(modelProperty, modelProperty.getValue(), jcrNode));
            } else {
                jcrNode.setProperty(modelProperty.getName(), valuesFrom(modelProperty, modelProperty.getValues(), jcrNode));
            }
        } catch (RepositoryException e) {
            String msg = String.format(
                    "Failed to process property '%s' defined through %s: %s",
                    modelProperty.getPath(), ModelUtils.formatDefinitions(modelProperty), e.getMessage());
            throw new RuntimeException(msg, e);
        }
    }

    private boolean isKnownProtectedPropertyName(final String modelPropertyName) {
        final String[] knownProtectedPropertyNames = new String[] {
                JCR_PRIMARYTYPE,
                JCR_MIXINTYPES,
                JCR_UUID,
// todo: actually needed               HIPPO_AVAILABILITY,
                HIPPO_RELATED,
                HIPPO_PATHS,
// todo: needed for now               HIPPOSTD_STATE,
// todo: actually needed               HIPPOSTD_HOLDER,
// todo: needed for now               HIPPOSTD_STATESUMMARY,
                HIPPOSTDPUBWF_PUBLICATION_DATE
        };
        return ArrayUtils.contains(knownProtectedPropertyNames, modelPropertyName);
    }

    private boolean isOverride(final ConfigurationProperty modelProperty,
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

    private boolean valuesAreIdentical(final ConfigurationProperty modelProperty,
                                       final Node jcrNode,
                                       final Property jcrProperty) throws Exception {
        if (isOverride(modelProperty, jcrProperty)) {
            return false;
        }

        if (modelProperty.getType() == PropertyType.SINGLE) {
            return valueIsIdentical(modelProperty, modelProperty.getValue(), jcrNode, jcrProperty.getValue());
        } else {
            final Value[] modelValues = modelProperty.getValues();
            final javax.jcr.Value[] jcrValues = jcrProperty.getValues();
            if (modelValues.length != jcrValues.length) {
                return false;
            }
            for (int i = 0; i < modelValues.length; i++) {
                if (!valueIsIdentical(modelProperty, modelValues[i], jcrNode, jcrValues[i])) {
                    return false;
                }
            }
            return true;
        }
    }

    private boolean valueIsIdentical(final ConfigurationProperty modelProperty,
                                     final Value modelValue,
                                     final Node jcrNode,
                                     final javax.jcr.Value jcrValue) throws Exception {
        if (modelValue.getType().ordinal() != jcrValue.getType()) {
            return false;
        }

        switch (modelValue.getType()) {
            case STRING:
                return getStringValue(modelProperty, modelValue).equals(jcrValue.getString());
            case BINARY:
                try (final InputStream modelInputStream = getBinaryInputStream(modelProperty, modelValue)) {
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
                return modelValue.getString().equals(jcrValue.getString());
            case REFERENCE:
            case WEAKREFERENCE:
                return getReferredNodeIdentifier(modelValue, jcrNode).equals(jcrValue.getString());
            case DECIMAL:
                return modelValue.getObject().equals(jcrValue.getDecimal());
            default:
                final String msg = String.format(
                        "Failed to process property '%s' defined through %s: unsupported value type '%s'.",
                        modelProperty.getPath(), ModelUtils.formatDefinitions(modelProperty), modelValue.getType());
                throw new RuntimeException(msg);
        }
    }

    private String getStringValue(final ConfigurationProperty modelProperty,
                                  final Value modelValue) throws IOException {
        if (modelValue.isResource()) {
            try (final InputStream inputStream = getResourceInputStream(modelProperty, modelValue)) {
                return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            }
        } else {
            return modelValue.getString();
        }
    }

    private InputStream getBinaryInputStream(final ConfigurationProperty modelProperty,
                                             final Value modelValue) throws IOException {
        if (modelValue.isResource()) {
            return getResourceInputStream(modelProperty, modelValue);
        } else {
            return new ByteArrayInputStream((byte[]) modelValue.getObject());
        }
    }

    private InputStream getResourceInputStream(final Source source, final String resourceName) throws IOException {
        return resourceInputProviders.get(source.getModule()).getResourceInputStream(source, resourceName);
    }

    private InputStream getResourceInputStream(final ConfigurationProperty modelProperty,
                                               final Value modelValue) throws IOException {
        final List<DefinitionItem> definitions = modelProperty.getDefinitions();
        final DefinitionItem definitionItem = findDefinitionItemForValue(definitions, modelValue);
        if (definitionItem == null) {
            final String msg = String.format(
                    "Failed to process property '%s' defined through %s: cannot find definition item that contributed resource '%s'",
                    modelProperty.getPath(),
                    ModelUtils.formatDefinitions(modelProperty),
                    modelValue.getString());
            throw new RuntimeException(msg);
        }
        return getResourceInputStream(definitionItem.getDefinition().getSource(), modelValue.getString());
    }

    /**
     * Finds the {@link DefinitionItem} that contributed the given {@link Value} or null if not found. It might be that
     * a value cannot be found because {@link DefinitionItem} later in the model processing order performs a
     * {@link PropertyOperation} delete, replace or override.
     */
    private DefinitionItem findDefinitionItemForValue(final List<DefinitionItem> definitions, final Value value) {
        for (int i = definitions.size() - 1; i > -1 ; i--) {
            final DefinitionProperty definitionProperty = (DefinitionProperty) definitions.get(i);
            if (definitionProperty.getOperation() == PropertyOperation.DELETE) {
                return null;
            }
            if (definitionPropertyContainsValue(definitionProperty, value)) {
                return definitionProperty;
            }
            if (definitionProperty.getOperation() == PropertyOperation.REPLACE
                    || definitionProperty.getOperation() == PropertyOperation.OVERRIDE) {
                return null;
            }
        }

        return null;
    }

    private boolean definitionPropertyContainsValue(final DefinitionProperty definitionProperty, final Value value) {
        // intentionally uses reference equality, not object equality
        // see also the test "expect_value_add_on_resource_to_work" which would fail if object equality would be used
        if (definitionProperty.getType() == PropertyType.SINGLE) {
            return definitionProperty.getValue() == value;
        } else {
            final Value[] values = definitionProperty.getValues();
            for (int i = 0; i < values.length; i++) {
                if (values[i] == value) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getReferredNodeIdentifier(final Value modelValue, final Node jcrNode) throws RepositoryException {
        if (modelValue.isPath()) {
            return resolveReference(modelValue.getString(), jcrNode);
        } else {
            return modelValue.getString();
        }
    }

    private String resolveReference(final String path, final Node jcrNode) throws RepositoryException {
        if (path.startsWith("/")) {
            return jcrNode.getSession().getNode(path).getIdentifier();
        } else {
            return jcrNode.getNode(path).getIdentifier();
        }
    }

    private javax.jcr.Value[] valuesFrom(final ConfigurationProperty modelProperty,
                                         final Value[] modelValues,
                                         final Node jcrNode) throws Exception {
        final javax.jcr.Value[] jcrValues = new javax.jcr.Value[modelValues.length];
        for (int i = 0; i < modelValues.length; i++) {
            jcrValues[i] = valueFrom(modelProperty, modelValues[i], jcrNode);
        }
        return jcrValues;
    }

    private javax.jcr.Value valueFrom(final ConfigurationProperty modelProperty,
                                      final Value modelValue,
                                      final Node jcrNode) throws Exception {
        final ValueFactory factory = session.getValueFactory();
        final ValueType type = modelValue.getType();

        switch (type) {
            case STRING:
                return factory.createValue(getStringValue(modelProperty, modelValue));
            case BINARY:
                final Binary binary = factory.createBinary(getBinaryInputStream(modelProperty, modelValue));
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
                return factory.createValue(modelValue.getString(), type.ordinal());
            case REFERENCE:
            case WEAKREFERENCE:
                return factory.createValue(getReferredNode(modelValue, jcrNode), type == ValueType.WEAKREFERENCE);
            case DECIMAL:
                return factory.createValue((BigDecimal)modelValue.getObject());
            default:
                final String msg = String.format(
                        "Failed to process property '%s' defined through %s: unsupported value type '%s'.",
                        modelProperty.getPath(), ModelUtils.formatDefinitions(modelProperty), type);
                throw new RuntimeException(msg);
        }
    }

    private Node getReferredNode(final Value modelValue, final Node jcrNode) throws RepositoryException {
        return session.getNodeByIdentifier(getReferredNodeIdentifier(modelValue, jcrNode));
    }

    private void applyUnprocessedReferences() throws Exception {
        for (Pair<ConfigurationProperty, Node> unprocessedReference : unprocessedReferences) {
            final ConfigurationProperty configurationProperty = unprocessedReference.getLeft();
            final Node jcrNode = unprocessedReference.getRight();
            applyProperty(configurationProperty, jcrNode);
        }
    }

}
