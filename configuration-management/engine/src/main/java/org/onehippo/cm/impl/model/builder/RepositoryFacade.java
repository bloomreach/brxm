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
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
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
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.commons.cnd.ParseException;
import org.onehippo.cm.api.model.ConfigurationNode;
import org.onehippo.cm.api.model.ConfigurationProperty;
import org.onehippo.cm.api.model.NamespaceDefinition;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.api.model.PropertyType;
import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.api.model.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;

public class RepositoryFacade {
    private static final Logger logger = LoggerFactory.getLogger(RepositoryFacade.class);
    private final Session session;

    public RepositoryFacade(final Session session) {
        this.session = session;
    }

    public void push(final MergedModel model) throws RepositoryException, ParseException {
        pushNamespaces(model.getNamespaceDefinitions());
        pushNodeTypes(model.getNodeTypeDefinitions());
        pushNodes(model.getConfigurationRootNode());
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

    private void pushNodes(final ConfigurationNode root) throws RepositoryException {
        final Node node = session.getRootNode();

        pushProperties(root, node);
        pushNodes(root, node);
    }

    private void pushNodes(final ConfigurationNode modelNode, final Node jcrNode) throws RepositoryException {
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
            if (indexedName.equals(nextChildIndexedName)) {
                // 'nextChildIndexedName' is processed, get new next
                nextChildIndexedName = nextChildNameProvider.next();
            } else {
                // jcrChild is not at next child position, move it there
                jcrNode.orderBefore(indexName(jcrChild), nextChildIndexedName);
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

        // compile map of uniquely named child nodes
        final Map<String, Node> existingChildren = new LinkedHashMap<>();
        final NodeIterator iterator = jcrNode.getNodes();
        while (iterator.hasNext()) {
            final Node jcrChild = iterator.nextNode();
            existingChildren.put(indexName(jcrChild), jcrChild);
        }

        // compile map of retained nodes
        final Map<String, Node> retainedChildren = new LinkedHashMap<>();
        for (String indexedName : modelNode.getNodes().keySet()) {
            if (existingChildren.containsKey(indexedName)) {
                retainedChildren.put(indexedName, existingChildren.get(indexedName));
                existingChildren.remove(indexedName);
            }
        }

        // remove nodes unknown to the model
        for (final String indexedName : existingChildren.keySet()) {
            existingChildren.get(indexedName).remove();
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

    private void pushProperties(final ConfigurationNode source, final Node target) throws RepositoryException {
        final Map<String, Property> existingProperties = new HashMap<>();
        final PropertyIterator iterator = target.getProperties();
        while (iterator.hasNext()) {
            final Property property = iterator.nextProperty();
            existingProperties.put(property.getName(), property);
        }

        for (String name : source.getProperties().keySet()) {
            final ConfigurationProperty property = source.getProperties().get(name);

            if (name.equals(JCR_PRIMARYTYPE)) {
                final String modelPrimaryType = property.getValue().getString();
                final String jcrPrimaryType = target.getPrimaryNodeType().getName();
                if (!jcrPrimaryType.equals(modelPrimaryType)) {
                    target.setPrimaryType(modelPrimaryType);
                }
            } else if (name.equals(JCR_MIXINTYPES)) {
                final List<String> jcrMixinTypes = Arrays.stream(target.getMixinNodeTypes())
                        .map(NodeType::getName)
                        .collect(Collectors.toList());
                final List<String> modelMixinTypes = Arrays.stream(property.getValues())
                        .map(Value::getString)
                        .collect(Collectors.toList());

                for (String modelMixinType : modelMixinTypes) {
                    if (jcrMixinTypes.contains(modelMixinType)) {
                        jcrMixinTypes.remove(modelMixinType);
                    } else {
                        target.addMixin(modelMixinType);
                    }
                }
                for (String mixinType : jcrMixinTypes) {
                    target.removeMixin(mixinType);
                }
            } else {
                if (existingProperties.containsKey(name)) {
                    // TODO: don't do that if the values are identical?
                    final Property existingProperty = existingProperties.get(name);
                    existingProperty.remove();
                    existingProperties.remove(name);
                }

                if (PropertyType.SINGLE == property.getType()) {
                    target.setProperty(name, valueFrom(property.getValue(), property.getValueType()));
                } else {
                    target.setProperty(name, valuesFrom(property.getValues(), property.getValueType()));
                }
            }
        }

        // delete all existing propertie that are not part of the source model
        for (String name : existingProperties.keySet()) {
            existingProperties.remove(name);
        }
    }

    private javax.jcr.Value[] valuesFrom(final Value[] pValues, final ValueType type) throws RepositoryException {
        final List<javax.jcr.Value> valueList = new ArrayList<>();
        for (Value pValue : pValues) {
            valueList.add(valueFrom(pValue, type));
        }
        return valueList.toArray(new javax.jcr.Value[valueList.size()]);
    }

    private javax.jcr.Value valueFrom(final Value pValue, final ValueType type) throws RepositoryException {
        Value value = pValue;
        if (pValue.isResource()) {
            // TODO: read value form resource!
            // value = ...
        }

        return singleValueFrom(value, type);
    }

    private javax.jcr.Value singleValueFrom(final Value sourceValue, final ValueType type) throws RepositoryException {
        final ValueFactory factory = session.getValueFactory();
        switch (type) {
            case URI:
                return factory.createValue(sourceValue.toString());
            case BOOLEAN:
                return factory.createValue((Boolean)sourceValue.getObject());
            case LONG:
                return factory.createValue((Long)sourceValue.getObject());
            case DOUBLE:
                return factory.createValue((Double)sourceValue.getObject());
            case DECIMAL:
                return factory.createValue((BigDecimal)sourceValue.getObject());
            case BINARY:
                return factory.createValue((Binary)sourceValue.getObject());
            case DATE:
                return factory.createValue((Calendar)sourceValue.getObject());
            case REFERENCE:
            case WEAKREFERENCE:
                final String uuid = sourceValue.getString();
                final Node node = session.getNodeByIdentifier(uuid);
                return factory.createValue(node, type == ValueType.WEAKREFERENCE);
            case PATH: // TODO: do anything special here?
            case STRING:
            case NAME:
                return factory.createValue(sourceValue.getString());
            default:
                final String msg = String.format("Unsupported value type '%s'.", type);
                throw new IllegalArgumentException(msg);
        }
    }
}
