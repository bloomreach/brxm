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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.jcr.Binary;
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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.commons.cnd.ParseException;
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
import org.onehippo.cm.engine.ResourceInputProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;

public class RepositoryFacade {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryFacade.class);

    private final Session session;
    private final Map<Module, ResourceInputProvider> resourceInputProviders;
    private final List<Pair<ConfigurationProperty, Node>> unprocessedReferences = new ArrayList<>();

    protected RepositoryFacade(final Session session,
                               final Map<Module, ResourceInputProvider> resourceInputProviders) {
        this.session = session;
        this.resourceInputProviders = resourceInputProviders;
    }

    public void push(final MergedModel model) throws Exception {
        unprocessedReferences.clear();

        pushNamespaces(model.getNamespaceDefinitions());
        pushNodeTypes(model.getNodeTypeDefinitions());
        pushNodes(model.getConfigurationRootNode());
        pushUnprocessedReferences();
    }

    private void pushNamespaces(final List<? extends NamespaceDefinition> namespaces) throws RepositoryException {
        final Set<String> prefixes = new HashSet<>(Arrays.asList(session.getNamespacePrefixes()));

        for (NamespaceDefinition namespace : namespaces) {
            final String prefix = namespace.getPrefix();
            final String uriString = namespace.getURI().toString();
            if (prefixes.contains(prefix)) {
                final String repositoryURI = session.getNamespaceURI(prefix);
                if (!uriString.equals(repositoryURI)) {
                    final String msg = String.format("Namespace with prefix '%s' already exists in repository with different URI. Existing: '%s', from model: '%s'",
                            prefix, repositoryURI, uriString);
                    throw new IllegalArgumentException(msg);
                }

                prefixes.remove(prefix);
            } else {
                session.setNamespacePrefix(prefix, uriString);
            }
        }

        for (String prefix : prefixes) {
            logger.info("Namespace prefix '{}' is not part of the model, but still exists in the repository.", prefix);
        }
    }

    private void pushNodeTypes(final List<? extends NodeTypeDefinition> nodeTypeDefinitions) throws RepositoryException, ParseException {
        for (NodeTypeDefinition nodeTypeDefinition : nodeTypeDefinitions) {
            pushNodeType(nodeTypeDefinition);
        }
    }

    private static final Pattern CND_NAME_EXTRACTOR = Pattern.compile("\\[[^\\]]+\\]");

    private String generateNameForInlineCnd(final String cnd) {
        final Matcher cndMatcher = CND_NAME_EXTRACTOR.matcher(cnd);
        if (cndMatcher.matches()) {
            return "..." + cnd.substring(cndMatcher.start(), cndMatcher.end()) + "...";
        }
        return "unidentified inline CND";
    }

    private void pushNodeType(final NodeTypeDefinition nodeTypeDefinition) throws RepositoryException, ParseException {
        final String definitionValue = nodeTypeDefinition.getValue();
        final String cndName = nodeTypeDefinition.isResource()
                ? definitionValue
                : generateNameForInlineCnd(definitionValue);
        final InputStream cndStream = nodeTypeDefinition.isResource()
                ? null // TODO read resource
                : new ByteArrayInputStream(definitionValue.getBytes(StandardCharsets.UTF_8));

        // inspired by BootstrapUtils.initializeNodeTypes()
        /*
            below code depends on hippo-repository-engine, which implements the HippoCompactNodeTypeDefReader.

            logger.debug("Initializing nodetypes from {} ", cndName);
            final NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
            final CompactNodeTypeDefReader<QNodeTypeDefinition, CompactNodeTypeDefWriter.NamespaceMapping> cndReader =
                    new HippoCompactNodeTypeDefReader(new InputStreamReader(cndStream), cndName, namespaceRegistry);
            final List<QNodeTypeDefinition> ntdList = cndReader.getNodeTypeDefinitions();
            final NodeTypeRegistry nodeTypeRegistry = ((NodeTypeManagerImpl) session.getWorkspace().getNodeTypeManager()).getNodeTypeRegistry();

            for (QNodeTypeDefinition ntd : ntdList) {
                try {
                    if (!nodeTypeRegistry.isRegistered(ntd.getName())) {
                        logger.debug("Registering node type {}", ntd.getName());
                        nodeTypeRegistry.registerNodeType(ntd);
                    } else {
                        logger.debug("Replacing node type {}", ntd.getName());
                        nodeTypeRegistry.reregisterNodeType(ntd);
                    }
                } catch (InvalidNodeTypeDefException e) {
                    throw new RepositoryException("Invalid node type definition for node type " + ntd.getName(), e);
                }
            }
        */
    }

    private void pushNodes(final ConfigurationNode configurationRoot) throws Exception {
        final Node root = session.getRootNode();
        pushProperties(configurationRoot, root);
        pushNodes(configurationRoot, root);
    }

    private void pushNodes(final ConfigurationNode modelNode, final Node jcrNode) throws Exception {
        final Map<String, Node> retainedChildren = removeNonModelNodes(modelNode, jcrNode);
        final NextChildNameProvider nextChildNameProvider = new NextChildNameProvider(retainedChildren);
        String nextChildIndexedName = nextChildNameProvider.next();

        // iterate over desired list of nodes (in desired order)
        for (String indexedName : modelNode.getNodes().keySet()) {
            final ConfigurationNode modelChild = modelNode.getNodes().get(indexedName);

            // create child if necessary
            final Node jcrChild = retainedChildren.containsKey(indexedName)
                    ? retainedChildren.get(indexedName)
                    : jcrNode.addNode(unindexName(indexedName), getPrimaryType(modelChild));

            nextChildNameProvider.ignore(indexedName); // 'indexedName' is consumed.

            // ensure correct ordering
            if (jcrNode.getPrimaryNodeType().hasOrderableChildNodes()) {
                if (indexedName.equals(nextChildIndexedName)) {
                    // 'nextChildIndexedName' is processed, get new next
                    nextChildIndexedName = nextChildNameProvider.next();
                } else {
                    // jcrChild is not at next child position, move it there
                    jcrNode.orderBefore(indexName(jcrChild), nextChildIndexedName);
                }
            }

            pushProperties(modelChild, jcrChild);
            pushNodes(modelChild, jcrChild);
        }
    }

    private String getPrimaryType(final ConfigurationNode modelNode) {
        if (!modelNode.getProperties().containsKey(JCR_PRIMARYTYPE)) {
            final String msg = String.format("Cannot add child node '%s': %s property missing.",
                    modelNode.getPath(), JCR_PRIMARYTYPE);
            throw new IllegalArgumentException(msg);
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
        private final Set<String> toBeIgnoredIndexedNames = new HashSet<>();

        NextChildNameProvider(final Map<String, Node> childMap) {
            iterator = childMap.keySet().iterator();
        }

        /**
         * @return next child node name in indexed (in case of SNS), jcr-encoded format
         */
        String next() {
            while (iterator.hasNext()) {
                final String next = iterator.next();
                if (!toBeIgnoredIndexedNames.contains(next)) {
                    return next;
                }
            }
            return null;
        }

        void ignore(final String indexedName) {
            toBeIgnoredIndexedNames.add(indexedName);
        }
    }

    private static final Pattern INDEXED_NAME_PATTERN = Pattern.compile("\\[\\d+\\]$");

    private String unindexName(final String jcrName) {
        final Matcher nameMatcher = INDEXED_NAME_PATTERN.matcher(jcrName);

        if (nameMatcher.matches()) {
            return jcrName.substring(0, nameMatcher.start());
        }

        return jcrName;
    }

    private String indexName(final Node node) throws RepositoryException {
        final int index = node.getIndex();

        return node.getName() + (index > 1 ? "[" + index + "]" : "");
    }

    private void pushProperties(final ConfigurationNode source, final Node target) throws Exception {
        removeNonModelProperties(source, target);

        pushPrimaryAndMixinTypes(source, target);

        for (ConfigurationProperty modelProperty : source.getProperties().values()) {
            if (modelProperty.getValueType() == ValueType.REFERENCE ||
                    modelProperty.getValueType() == ValueType.WEAKREFERENCE) {
                unprocessedReferences.add(Pair.of(modelProperty, target));
            } else {
                pushProperty(modelProperty, target);
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

    private void pushPrimaryAndMixinTypes(final ConfigurationNode source, final Node target) throws RepositoryException {
        // TODO: for now ignore root, as it does not have type information in the model yet
        if (target.getDepth() == 0) return;

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

    private void pushProperty(final ConfigurationProperty modelProperty, final Node jcrNode) throws Exception {
        // TODO: discuss how to efficiently filter out protected properties here
        // TODO: the code now "works" as primaryType and mixinTypes are correctly set and valuesAreIdentical returns true
        final Property jcrProperty = getPropertyIfExists(jcrNode, modelProperty.getName());

        if (jcrProperty != null) {
            if (isOverride(modelProperty, jcrProperty)) {
                jcrProperty.remove();
            } else if (valuesAreIdentical(modelProperty, jcrNode, jcrProperty)) {
                return;
            }
        }

        if (modelProperty.getType() == PropertyType.SINGLE) {
            jcrNode.setProperty(modelProperty.getName(), valueFrom(modelProperty, modelProperty.getValue(), jcrNode));
        } else {
            jcrNode.setProperty(modelProperty.getName(), valuesFrom(modelProperty, modelProperty.getValues(), jcrNode));
        }
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
                final String msg = String.format("Unsupported value type '%s'.", modelValue.getType());
                throw new IllegalArgumentException(msg);
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

    private InputStream getResourceInputStream(final ConfigurationProperty modelProperty,
                                               final Value modelValue) throws IOException {
        final List<DefinitionItem> definitions = modelProperty.getDefinitions();
        final DefinitionItem definitionItem = findDefinitionItemForValue(definitions, modelValue);
        if (definitionItem == null) {
            final String msg = String.format(
                    "Cannot find definition item that contributed resource '%s' in node '%s'.",
                    modelValue.getString(),
                    modelProperty.getPath());
            throw new IllegalArgumentException(msg);
        }
        final Source source = definitionItem.getDefinition().getSource();
        return resourceInputProviders.get(source.getModule()).getResourceInputStream(source, modelValue.getString());
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
                final String msg = String.format("Unsupported value type '%s'.", type);
                throw new IllegalArgumentException(msg);
        }
    }

    private Node getReferredNode(final Value modelValue, final Node jcrNode) throws RepositoryException {
        return session.getNodeByIdentifier(getReferredNodeIdentifier(modelValue, jcrNode));
    }

    // TODO: copied from JcrUtils as afaik we do not want a compile dependency on that project yet
    /**
     * Get the property at <code>relPath</code> from <code>baseNode</code> or <code>null</code> if no such property
     * exists.
     *
     * @param baseNode existing node that should be the base for the relative path
     * @param relPath  relative path to the property to get
     * @return the property at <code>relPath</code> from <code>baseNode</code> or <code>null</code> if no such property
     *         exists.
     * @throws RepositoryException in case of exception accessing the Repository
     */
    public static Property getPropertyIfExists(Node baseNode, String relPath) throws RepositoryException {
        try {
            return baseNode.getProperty(relPath);
        } catch (PathNotFoundException e) {
            return null;
        }
    }

    private void pushUnprocessedReferences() throws Exception {
        for (Pair<ConfigurationProperty, Node> unprocessedReference : unprocessedReferences) {
            final ConfigurationProperty configurationProperty = unprocessedReference.getLeft();
            final Node jcrNode = unprocessedReference.getRight();
            pushProperty(configurationProperty, jcrNode);
        }
    }

}
