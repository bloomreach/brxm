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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.EnumSet;
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cm.api.MergedModel;
import org.onehippo.cm.api.ResourceInputProvider;
import org.onehippo.cm.api.model.ConfigurationNode;
import org.onehippo.cm.api.model.ConfigurationProperty;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.DefinitionType;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.NamespaceDefinition;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.api.model.PropertyType;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.api.model.ValueType;
import org.onehippo.cm.api.model.WebFileBundleDefinition;
import org.onehippo.cm.impl.model.ModelUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.webfiles.WebFilesService;
import org.onehippo.repository.bootstrap.util.BootstrapUtils;
import org.onehippo.repository.bootstrap.util.PartialZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.JCR_UUID;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PATHS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_RELATED;
import static org.onehippo.repository.bootstrap.util.BootstrapUtils.getBaseZipFileFromURL;

public class ConfigurationPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationPersistenceService.class);

    private final Session session;
    private final Map<Module, ResourceInputProvider> resourceInputProviders;
    private final List<Pair<ConfigurationProperty, Node>> unprocessedReferences = new ArrayList<>();

    public ConfigurationPersistenceService(final Session session, final Map<Module, ResourceInputProvider> resourceInputProviders) {
        this.session = session;
        this.resourceInputProviders = resourceInputProviders;
    }

    public void apply(final MergedModel model, EnumSet<DefinitionType> includeDefinitionTypes) throws Exception {
        unprocessedReferences.clear();

        if (includeDefinitionTypes.contains(DefinitionType.NAMESPACE)) {
            applyNamespaces(model.getNamespaceDefinitions());
        }
        if (includeDefinitionTypes.contains(DefinitionType.CND)) {
            applyNodeTypes(model.getNodeTypeDefinitions());
        }
        if (includeDefinitionTypes.contains(DefinitionType.CONFIG)) {
            applyNodes(model.getConfigurationRootNode());
            applyUnprocessedReferences();
        }
        if (includeDefinitionTypes.contains(DefinitionType.WEBFILEBUNDLE)) {
            applyWebFileBundles(model.getWebFileBundleDefinitions());
        }
    }

    private void applyNamespaces(final List<? extends NamespaceDefinition> namespaceDefinitions) throws RepositoryException {
        final Set<String> prefixes = new HashSet<>(Arrays.asList(session.getNamespacePrefixes()));

        for (NamespaceDefinition namespaceDefinition : namespaceDefinitions) {
            final String prefix = namespaceDefinition.getPrefix();
            final String uriString = namespaceDefinition.getURI().toString();
            logger.debug(String.format("processing namespace prefix='%s' uri='%s' defined in %s.",
                    prefix, uriString, ModelUtils.formatDefinition(namespaceDefinition)));
            if (prefixes.contains(prefix)) {
                final String repositoryURI = session.getNamespaceURI(prefix);
                if (!uriString.equals(repositoryURI)) {
                    final String msg = String.format(
                            "Failed to process namespace definition defined in %s: namespace with prefix '%s' already exists in repository with different URI. Existing: '%s', from model: '%s'; aborting",
                            ModelUtils.formatDefinition(namespaceDefinition), prefix, repositoryURI, uriString);
                    throw new RuntimeException(msg);
                }
            } else {
                final NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
                namespaceRegistry.registerNamespace(prefix, uriString);
            }
        }
    }

    private void applyNodeTypes(final List<? extends NodeTypeDefinition> nodeTypeDefinitions) throws RepositoryException, IOException {
        for (NodeTypeDefinition nodeTypeDefinition : nodeTypeDefinitions) {
            applyNodeType(nodeTypeDefinition);
        }
    }

    private void applyNodeType(final NodeTypeDefinition nodeTypeDefinition) throws RepositoryException, IOException {
        final String definitionValue = nodeTypeDefinition.getValue();

        final InputStream cndStream;
        if (nodeTypeDefinition.isResource()) {
            logger.debug(String.format("processing cnd '%s' defined in %s.", definitionValue, ModelUtils.formatDefinition(nodeTypeDefinition)));
            cndStream = getResourceInputStream(nodeTypeDefinition.getSource(), definitionValue);
        } else {
            logger.debug(String.format("processing inline cnd defined in %s.", ModelUtils.formatDefinition(nodeTypeDefinition)));
            cndStream = new ByteArrayInputStream(definitionValue.getBytes(StandardCharsets.UTF_8));
        }

        BootstrapUtils.initializeNodetypes(session, cndStream, ModelUtils.formatDefinition(nodeTypeDefinition));
    }

    private void applyNodes(final ConfigurationNode configurationRoot) throws Exception {
        final Node root = session.getRootNode();
        applyProperties(configurationRoot, root);
        applyNodes(configurationRoot, root);
    }

    private void applyNodes(final ConfigurationNode modelNode, final Node jcrNode) throws Exception {
        logger.debug(String.format("processing node '%s' defined in %s.", modelNode.getPath(), ModelUtils.formatDefinitions(modelNode)));
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
                    "Failed to process node '%s' defined in %s: cannot add child node '%s': %s property missing.",
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
                    "Failed to process property '%s' defined in %s: %s",
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
                return modelValue.getString().equals(jcrValue.getString());
            case REFERENCE:
            case WEAKREFERENCE:
                return getReferredNodeIdentifier(modelValue, jcrNode).equals(jcrValue.getString());
            case DECIMAL:
                return modelValue.getObject().equals(jcrValue.getDecimal());
            default:
                final String msg = String.format(
                        "Failed to process property '%s' defined in %s: unsupported value type '%s'.",
                        modelProperty.getPath(), ModelUtils.formatDefinitions(modelProperty), modelValue.getType());
                throw new RuntimeException(msg);
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

    private String getReferredNodeIdentifier(final Value modelValue, final Node jcrNode) throws RepositoryException {
        String identifier = modelValue.getString();
        if (modelValue.isPath()) {
            String nodePath = identifier;
            if (!nodePath.startsWith("/")) {
                // path reference is relative to content definition root path
                final String rootPath = ((ContentDefinition) modelValue.getParent().getDefinition()).getNode().getPath();
                nodePath = rootPath + ("".equals(nodePath) ? "" : "/" + nodePath);
            }
            // lookup node identifier by node path
            identifier = jcrNode.getSession().getNode(nodePath).getIdentifier();
        }
        return identifier;
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
                return factory.createValue(modelValue.getString(), type.ordinal());
            case REFERENCE:
            case WEAKREFERENCE:
                return factory.createValue(getReferredNode(modelValue, jcrNode), type == ValueType.WEAKREFERENCE);
            case DECIMAL:
                return factory.createValue((BigDecimal)modelValue.getObject());
            default:
                final String msg = String.format(
                        "Failed to process property '%s' defined in %s: unsupported value type '%s'.",
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

    private void applyWebFileBundles(final List<WebFileBundleDefinition> webFileBundleDefinitions) throws Exception {
        final WebFilesService service = HippoServiceRegistry.getService(WebFilesService.class);
        if (service == null) {
            final String msg = String.format("Failed to import web file bundles: missing service for '%s'",
                    WebFilesService.class.getName());
            throw new RuntimeException(msg);
        }

        for (WebFileBundleDefinition webFileBundleDefinition : webFileBundleDefinitions) {
            final String bundleName = webFileBundleDefinition.getName();
            logger.debug(String.format("processing web file bundle '%s' defined in %s.", bundleName,
                    ModelUtils.formatDefinition(webFileBundleDefinition)));

            final ResourceInputProvider resourceInputProvider =
                    resourceInputProviders.get(webFileBundleDefinition.getSource().getModule());
            final URL moduleRoot = resourceInputProvider.getModuleRoot();
            if (moduleRoot.toString().contains("jar!")) {
                final PartialZipFile bundleZipFile =
                        new PartialZipFile(getBaseZipFileFromURL(moduleRoot), bundleName);
                service.importJcrWebFileBundle(session, bundleZipFile, true);
            } else if (moduleRoot.toString().startsWith("file:")) {
                final File bundleDir = new File(FileUtils.toFile(moduleRoot), bundleName);
                service.importJcrWebFileBundle(session, bundleDir, true);
            }
        }
    }

}
