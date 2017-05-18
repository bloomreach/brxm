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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Binary;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
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
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.cm.api.model.ConfigurationItemCategory;
import org.onehippo.cm.api.model.ConfigurationModel;
import org.onehippo.cm.api.model.ConfigurationNode;
import org.onehippo.cm.api.model.ConfigurationProperty;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.NamespaceDefinition;
import org.onehippo.cm.api.model.NodeTypeDefinition;
import org.onehippo.cm.api.model.PropertyType;
import org.onehippo.cm.api.model.Source;
import org.onehippo.cm.api.model.Value;
import org.onehippo.cm.api.model.ValueType;
import org.onehippo.cm.engine.SnsUtils;
import org.onehippo.cm.impl.model.ConfigurationNodeImpl;
import org.onehippo.cm.impl.model.ConfigurationPropertyImpl;
import org.onehippo.cm.impl.model.ModelUtils;
import org.onehippo.cm.impl.model.ValueImpl;
import org.onehippo.repository.bootstrap.util.BootstrapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.JCR_UUID;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATESUMMARY;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PATHS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_RELATED;

/**
 * ConfigurationConfigService is responsible for reading and writing Configuration from/to the repository.
 * Access to the repository is provided to this service through the API ({@link javax.jcr.Node} or
 * {@link Session}), this service is stateless.
 *
 * TODO: Currently, there also exists a ConfigurationPersistenceService. Its content should be moved here, in time.
 */
public class ConfigurationConfigService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationConfigService.class);
    private static final String[] knownDerivedPropertyNames = new String[] {
            HIPPO_RELATED,
            HIPPO_PATHS,
            HIPPOSTD_STATESUMMARY
    };

    private static class UnprocessedReference {
        final ConfigurationProperty updateProperty;
        final ConfigurationProperty baselineProperty;
        final Node targetNode;

        UnprocessedReference(ConfigurationProperty updateProperty, ConfigurationProperty baselineProperty, Node targetNode) {
            this.updateProperty = updateProperty;
            this.baselineProperty = baselineProperty;
            this.targetNode = targetNode;
        }
    }

    /**
     * Compute the difference between the baseline configuration and the update configuration, and apply it to the
     * repository.
     *
     * @param baseline baseline configuration for computing the delta
     * @param update   updated configuration, potentially different from baseline
     * @param session  JCR session to write to the repository. The caller has to take care of #save-ing any changes.
     * @param forceApply flag indicating that runtime changes to configuration data should be reverted
     * @throws RepositoryException in case of failure reading from / writing to the repository
     * @throws IOException in case of failure reading external resources
     */
    void computeAndWriteDelta(final ConfigurationModel baseline,
                              final ConfigurationModel update,
                              final Session session,
                              final boolean forceApply) throws RepositoryException, IOException {

        // Note: Namespaces, once they are in the repository, cannot be changed or removed.
        //       Therefore, both the baseline configuration and the forceApply flag are immaterial
        //       to the handling of namespaces. The same applies to node types, at least, as far as
        //       BootstrapUtils#initializeNodetypes offers support.
        applyNamespaces(update.getNamespaceDefinitions(), session);
        applyNodeTypes(update.getNodeTypeDefinitions(), session);

        final ConfigurationNode baselineRoot = baseline.getConfigurationRootNode();
        final Node targetNode = session.getNode(baselineRoot.getPath());
        final List<UnprocessedReference> unprocessedReferences = new ArrayList<>();

        computeAndWriteNodeDelta(baselineRoot, update.getConfigurationRootNode(), targetNode, forceApply, unprocessedReferences);
        postProcessReferences(unprocessedReferences);
    }

    private void applyNamespaces(final List<? extends NamespaceDefinition> namespaceDefinitions, final Session session)
            throws RepositoryException {
        final Set<String> prefixes = new HashSet<>(Arrays.asList(session.getNamespacePrefixes()));

        for (NamespaceDefinition namespaceDefinition : namespaceDefinitions) {
            final String prefix = namespaceDefinition.getPrefix();
            final String uriString = namespaceDefinition.getURI().toString();

            logger.debug(String.format("processing namespace prefix='%s' uri='%s' defined in %s.",
                    prefix, uriString, ModelUtils.formatDefinition(namespaceDefinition)));

            if (prefixes.contains(prefix)) {
                final String repositoryURI = session.getNamespaceURI(prefix);
                if (!uriString.equals(repositoryURI)) {
                    final String msg = String.format("Failed to process namespace definition defined in %s: " +
                                    "namespace with prefix '%s' already exists in repository with different URI. " +
                                    "Existing: '%s', target: '%s'. Changing existing namespaces is not supported. Aborting.",
                            ModelUtils.formatDefinition(namespaceDefinition), prefix, repositoryURI, uriString);
                    throw new RuntimeException(msg);
                }
            } else {
                session.getWorkspace().getNamespaceRegistry().registerNamespace(prefix, uriString);
            }
        }
    }

    private void applyNodeTypes(final List<NodeTypeDefinition> nodeTypes, final Session session) throws RepositoryException, IOException {
        for (NodeTypeDefinition nodeType : nodeTypes) {
            if (logger.isDebugEnabled()) {
                final String cndLabel = nodeType.isResource()
                        ? String.format("CND '%s'", nodeType.getValue()) : "inline CND";
                logger.debug(String.format("processing %s defined in %s.", cndLabel,
                        ModelUtils.formatDefinition(nodeType)));
            }

            // TODO: nodeTypeStream should be closed, right?
            final InputStream nodeTypeStream = nodeType.isResource()
                    ? getResourceInputStream(nodeType.getSource(), nodeType.getValue())
                    : new ByteArrayInputStream(nodeType.getValue().getBytes(StandardCharsets.UTF_8));
            BootstrapUtils.initializeNodetypes(session, nodeTypeStream, ModelUtils.formatDefinition(nodeType));
        }
    }

    private void computeAndWriteNodeDelta(final ConfigurationNode baselineNode,
                                  final ConfigurationNode updateNode,
                                  final Node targetNode,
                                  final boolean forceApply,
                                  final List<UnprocessedReference> unprocessedReferences)
            throws RepositoryException, IOException {

        computeAndWritePrimaryTypeDelta(baselineNode, updateNode, targetNode, forceApply);
        computeAndWriteMixinTypesDelta(baselineNode, updateNode, targetNode, forceApply);
        computeAndWritePropertiesDelta(baselineNode, updateNode, targetNode, forceApply, unprocessedReferences);
        computeAndWriteChildNodesDelta(baselineNode, updateNode, targetNode, forceApply, unprocessedReferences);
    }

    private void computeAndWritePrimaryTypeDelta(final ConfigurationNode baselineNode,
                                                 final ConfigurationNode updateNode,
                                                 final Node targetNode,
                                                 final boolean forceApply)
            throws RepositoryException {

        final Map<String, ConfigurationProperty> updateProperties = updateNode.getProperties();
        final Map<String, ConfigurationProperty> baselineProperties = baselineNode.getProperties();

        final String updatePrimaryType = updateProperties.get(JCR_PRIMARYTYPE).getValue().getString();
        final String baselinePrimaryType = baselineProperties.get(JCR_PRIMARYTYPE).getValue().getString();

        if (forceApply || !updatePrimaryType.equals(baselinePrimaryType)) {
            final String jcrPrimaryType = targetNode.getPrimaryNodeType().getName();
            if (jcrPrimaryType.equals(updatePrimaryType)) {
                return;
            }

            if (!jcrPrimaryType.equals(baselinePrimaryType)) {
                final String msg = forceApply
                        ? String.format("[OVERRIDE] Primary type '%s' of node '%s' is adjusted to '%s' as defined in %s.",
                                jcrPrimaryType, updateNode.getPath(), updatePrimaryType,
                                ModelUtils.formatDefinitions(updateProperties.get(JCR_PRIMARYTYPE)))
                        : String.format("[OVERRIDE] Primary type '%s' of node '%s' has been changed from '%s'."
                                + "Overriding to type '%s', defined in %s.",
                                jcrPrimaryType, updateNode.getPath(), baselinePrimaryType, updatePrimaryType,
                                ModelUtils.formatDefinitions(updateProperties.get(JCR_PRIMARYTYPE)));
                logger.info(msg);
            }
            targetNode.setPrimaryType(updatePrimaryType);
        }
    }

    private void computeAndWriteMixinTypesDelta(final ConfigurationNode baselineNode,
                                                final ConfigurationNode updateNode,
                                                final Node targetNode,
                                                final boolean forceApply) throws RepositoryException {

        final Map<String, ConfigurationProperty> updateProperties = updateNode.getProperties();
        final Map<String, ConfigurationProperty> baselineProperties = baselineNode.getProperties();

        // Update the mixin types, if needed
        final Value[] updateMixinValues = updateProperties.containsKey(JCR_MIXINTYPES)
                ? updateProperties.get(JCR_MIXINTYPES).getValues() : new Value[0];
        final Value[] baselineMixinValues = baselineProperties.containsKey((JCR_MIXINTYPES))
                ? baselineProperties.get(JCR_MIXINTYPES).getValues() : new Value[0];

        // Add / restore mixin types
        for (Value mixinValue : updateMixinValues) {
            final String mixin = mixinValue.getString();
            if (!hasMixin(targetNode, mixin)) {
                if (forceApply) {
                    if (hasMixin(baselineMixinValues, mixin)) {
                        final String msg = String.format("[OVERRIDE] Mixin '%s' has been removed from node '%s', " +
                                        "but is re-added because it is defined at %s.",
                                mixin, updateNode.getPath(), ModelUtils.formatDefinitions(updateProperties.get(JCR_MIXINTYPES)));
                        logger.info(msg);
                    }
                    targetNode.addMixin(mixin);
                } else {
                    // only add the mixin in case of a delta with the baseline
                    if (!hasMixin(baselineMixinValues, mixin)) {
                        targetNode.addMixin(mixin);
                    }
                }
            }
        }

        // Remove / clean up mixin types
        if (forceApply) {
            for (NodeType mixinType : targetNode.getMixinNodeTypes()) {
                final String jcrMixin = mixinType.getName();
                if (!hasMixin(updateMixinValues, jcrMixin)) {
                    if (!hasMixin(baselineMixinValues, jcrMixin)) {
                        final String msg = String.format("[OVERRIDE] Mixin '%s' has been added to node '%s'," +
                                        " but is removed because it is not present in definition %s.",
                                jcrMixin, updateNode.getPath(), ModelUtils.formatDefinitions(updateNode));
                        logger.info(msg);
                    }

                    removeMixin(targetNode, jcrMixin);
                }
            }
        } else {
            for (Value baselineMixinValue : baselineMixinValues) {
                final String baselineMixin = baselineMixinValue.getString();
                if (!hasMixin(updateMixinValues, baselineMixin)) {
                    removeMixin(targetNode, baselineMixin);
                }
            }
        }
    }

    private boolean hasMixin(final Node node, final String mixin) throws RepositoryException {
        for (NodeType mixinType : node.getMixinNodeTypes()) {
            if (mixinType.getName().equals(mixin)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMixin(final Value[] mixinValues, final String mixin) {
        for (Value mixinValue : mixinValues) {
            if (mixinValue.getString().equals(mixin)) {
                return true;
            }
        }
        return false;
    }

    private void removeMixin(final Node node, final String mixin) throws RepositoryException {
        for (NodeType mixinType : node.getMixinNodeTypes()) {
            if (mixinType.getName().equals(mixin)) {
                node.removeMixin(mixin);
                return;
            }
        }
    }

    private void computeAndWritePropertiesDelta(final ConfigurationNode baselineNode,
                                                 final ConfigurationNode updateNode,
                                                 final Node targetNode,
                                                 final boolean forceApply,
                                                 final List<UnprocessedReference> unprocessedReferences)
            throws RepositoryException, IOException {

        final Map<String, ConfigurationProperty> updateProperties = updateNode.getProperties();
        final Map<String, ConfigurationProperty> baselineProperties = baselineNode.getProperties();

        for (String propertyName : updateProperties.keySet()) {
            if (propertyName.equals(JCR_PRIMARYTYPE) || propertyName.equals(JCR_MIXINTYPES)) {
                continue; // we have already addressed these properties
            }

            final ConfigurationProperty updateProperty = updateProperties.get(propertyName);
            final ConfigurationProperty baselineProperty = baselineProperties.get(propertyName);

            if (forceApply || baselineProperty == null || !propertyIsIdentical(updateProperty, baselineProperty)) {
                if (isReferenceTypeProperty(updateProperty)) {
                    unprocessedReferences.add(new UnprocessedReference(updateProperty, baselineProperty, targetNode));
                } else {
                    updateProperty(updateProperty, baselineProperty, targetNode);
                }
            }
        }

        // Remove deleted properties
        if (forceApply) {
            for (String propertyName : getPropertyNames(targetNode)) {
                if (!updateProperties.containsKey(propertyName)
                        && updateNode.getChildCategory(propertyName) == ConfigurationItemCategory.CONFIGURATION) {
                    removeProperty(propertyName, baselineProperties.get(propertyName), targetNode, updateNode);
                }
            }
        } else {
            for (String propertyName : baselineProperties.keySet()) {
                if (!propertyName.equals(JCR_PRIMARYTYPE)
                        && !propertyName.equals(JCR_MIXINTYPES)
                        && !updateProperties.containsKey(propertyName)) {
                    removeProperty(propertyName, baselineProperties.get(propertyName), targetNode, updateNode);
                }
            }
        }
    }

    private List<String> getPropertyNames(final Node node) throws RepositoryException {
        final List<String> names = new ArrayList<>();
        for (Property property : new PropertyIterable(node.getProperties())) {
            final String name = property.getName();
            if (!property.getDefinition().isProtected() && !isKnownDerivedPropertyName(name)) {
                names.add(property.getName());
            }
        }
        return names;
    }

    private void computeAndWriteChildNodesDelta(final ConfigurationNode baselineNode,
                                                final ConfigurationNode updateNode,
                                                final Node targetNode,
                                                final boolean forceApply,
                                                final List<UnprocessedReference> unprocessedReferences)
            throws RepositoryException, IOException {

        final Map<String, ConfigurationNode> updateChildren = updateNode.getNodes();
        final Map<String, ConfigurationNode> baselineChildren = baselineNode.getNodes();

        // Add or update child nodes

        for (String indexedChildName : updateChildren.keySet()) {
            ConfigurationNode baselineChild = baselineChildren.get(indexedChildName);
            final ConfigurationNode updateChild = updateChildren.get(indexedChildName);
            final Pair<String, Integer> nameAndIndex = SnsUtils.splitIndexedName(indexedChildName);
            final Node existingChildNode = getChildWithIndex(targetNode, nameAndIndex.getLeft(), nameAndIndex.getRight());
            final Node childNode;

            if (existingChildNode == null) {
                // need to add node
                final String childPrimaryType = updateChild.getProperties().get(JCR_PRIMARYTYPE).getValue().getString();
                final String childName = nameAndIndex.getLeft();

                if (baselineChild == null) {
                    baselineChild = newChildOfType(childPrimaryType);
                } else {
                    if (forceApply) {
                        final String msg = String.format("[OVERRIDE] Node '%s' has been removed, " +
                                        "but will be re-added due to definition %s.",
                                updateChild.getPath(), ModelUtils.formatDefinitions(updateChild));
                        logger.info(msg);
                    } else {
                        // In the baseline, the child exists. Some of its properties or (nested) children may
                        // have changed, but the current implementation considers the manual removal of the
                        // child node more important, so we don't compute if there is a difference between
                        // baselineChild and updateChild, and refrain from re-adding the deleted child node.
                        continue;
                    }
                }
                childNode = addNode(targetNode, childName, childPrimaryType, updateChild);
            } else {
                if (baselineChild == null) {
                    final String msg = String.format("[OVERRIDE] Node '%s' has been added, " +
                            "but will be re-created due to the incoming definition %s.",
                            updateChild.getPath(), ModelUtils.formatDefinitions(updateChild));
                    logger.info(msg);

                    final String childPrimaryType = existingChildNode.getPrimaryNodeType().getName();
                    baselineChild = newChildOfType(childPrimaryType);
                }
                childNode = existingChildNode;
            }

            // recurse
            computeAndWriteNodeDelta(baselineChild, updateChild, childNode, forceApply, unprocessedReferences);
        }

        // Remove child nodes that are not / no longer in the model.

        final List<String> indexedNamesOfToBeRemovedChildren = new ArrayList<>();
        if (forceApply) {
            for (Node childNode : new NodeIterable(targetNode.getNodes())) {
                final String indexedChildName = SnsUtils.createIndexedName(childNode);
                if (!updateChildren.containsKey(indexedChildName)) {
                    if (updateNode.getChildCategory(indexedChildName) == ConfigurationItemCategory.CONFIGURATION) {
                        indexedNamesOfToBeRemovedChildren.add(indexedChildName);
                    }
                }
            }
        } else {
            // Note: SNS is supported because the node's name includes the index.
            //       But it's brittle: basically, we can only correctly remove SNS children if we
            //       remove the children with the highest index from the baseline configuration only,
            //       while additional runtime/repository SNS nodes have not been added, or at the end only.
            //       This is why we process the child nodes of the baseline model in *reverse* order.

            final List<String> reversedIndexedBaselineChildNames = new ArrayList<>(baselineChildren.keySet());
            Collections.reverse(reversedIndexedBaselineChildNames);

            for (String indexedChildName : reversedIndexedBaselineChildNames) {
                if (!updateChildren.containsKey(indexedChildName)) {
                    indexedNamesOfToBeRemovedChildren.add(indexedChildName);
                }
            }
        }

        for (String indexedChildName : indexedNamesOfToBeRemovedChildren) {
            final Pair<String, Integer> nameAndIndex = SnsUtils.splitIndexedName(indexedChildName);
            final Node childNode = getChildWithIndex(targetNode, nameAndIndex.getLeft(), nameAndIndex.getRight());
            if (childNode != null) {
                if (!baselineChildren.containsKey(indexedChildName)) {
                    final String msg = String.format("[OVERRIDE] Child node '%s' exists, " +
                                    "but will be deleted while processing the children of node '%s' defined in %s.",
                            indexedChildName, updateNode.getPath(), ModelUtils.formatDefinitions(updateNode));
                    logger.info(msg);
                } else {
                    // [OVERRIDE] We don't currently check if the removed node has changes compared to the baseline.
                    //            Such a check would be rather invasive (potentially full sub-tree compare)
                }
                childNode.remove();
            }
        }

        // Care for node ordering?
        final boolean orderingIsRelevant = targetNode.getPrimaryNodeType().hasOrderableChildNodes()
                && (updateNode.getIgnoreReorderedChildren() == null || !updateNode.getIgnoreReorderedChildren());
        if (orderingIsRelevant && updateChildren.size() > 0) {
            reorderChildren(targetNode, new ArrayList<>(updateChildren.keySet()));
        }
    }

    private Node addNode(final Node parentNode, final String childName, final String childPrimaryType,
                         final ConfigurationNode childModelNode) throws RepositoryException {
        final ConfigurationProperty uuidProperty = childModelNode.getProperties().get(JCR_UUID);
        if (uuidProperty != null) {
            final String uuid = uuidProperty.getValue().getString();
            if (!isUuidInUse(uuid, parentNode.getSession())) {
                // uuid not in use: create node with the requested uuid
                final NodeImpl parentNodeImpl = (NodeImpl) NodeDecorator.unwrap(parentNode);
                return parentNodeImpl.addNodeWithUuid(childName, childPrimaryType, uuid);
            } else {
                logger.warn(String.format("Specified jcr:uuid %s for node '%s' defined in %s already in use: "
                                + "a new jcr:uuid will be generated instead.",
                        uuid, childModelNode.getPath(), ModelUtils.formatDefinitions(childModelNode)));
            }
        }
        return parentNode.addNode(childName, childPrimaryType);
    }

    private boolean isUuidInUse(final String uuid, final Session session) throws RepositoryException {
        try {
            session.getNodeByIdentifier(uuid);
            return true;
        } catch (ItemNotFoundException e) {
            return false;
        }
    }

    private ConfigurationNode newChildOfType(final String primaryType) {
        final ConfigurationNodeImpl child = new ConfigurationNodeImpl();
        final ConfigurationPropertyImpl property = new ConfigurationPropertyImpl();
        property.setName(JCR_PRIMARYTYPE);
        property.setType(PropertyType.SINGLE);
        property.setValueType(ValueType.NAME);
        property.setValue(new ValueImpl(primaryType));
        child.addProperty(property.getName(), property);
        return child;
    }

    /**
     * Put nodes into the appropriate order:
     *
     * We bring the children of parent into the desired order specified by indexedModelChildNames
     * by ignoring/skipping non-model child nodes of parent, such that these children
     * stay "in place", rather than trickle down to the end of parent's list of children.
     */
    private void reorderChildren(final Node parent, final List<String> indexedModelChildNames) throws RepositoryException {
        final List<String> indexedTargetNodeChildNames = new ArrayList<>();
        final NodeIterator targetChildNodes = parent.getNodes();
        while (targetChildNodes.hasNext()) {
            final Node targetChildNode = targetChildNodes.nextNode();
            indexedTargetNodeChildNames.add(SnsUtils.createIndexedName(targetChildNode));
        }

        for (int modelIndex = 0, targetIndex = 0; modelIndex < indexedModelChildNames.size(); modelIndex++) {
            final String indexedModelName = indexedModelChildNames.get(modelIndex);
            if (!indexedTargetNodeChildNames.contains(indexedModelName)) {
                continue; // child may have been removed manually...
            }

            String indexedTargetName = indexedTargetNodeChildNames.get(targetIndex);
            while (indexedModelChildNames.indexOf(indexedTargetName) < modelIndex) {
                targetIndex++; // skip target node, it isn't part of the model.
                indexedTargetName = indexedTargetNodeChildNames.get(targetIndex);
            }

            if (indexedTargetName.equals(indexedModelName)) {
                // node is at appropriate position, do nothing
                targetIndex++;
            } else {
                // node is at a later position, reorder it
                parent.orderBefore(indexedModelName, indexedTargetName);
            }
        }
    }

    private Node getChildWithIndex(final Node parent, final String name, final int index) throws RepositoryException {
        final NodeIterator existingSnsNodes = parent.getNodes(name);
        Node sibling = null;
        for (int i = 0; i < index; i++) {
            if (existingSnsNodes.hasNext()) {
                sibling = existingSnsNodes.nextNode();
            } else {
                return null;
            }
        }
        return sibling;
    }

    private void updateProperty(final ConfigurationProperty updateProperty,
                                final ConfigurationProperty baselineProperty,
                                final Node jcrNode)
            throws RepositoryException, IOException {

        if (isKnownDerivedPropertyName(updateProperty.getName())) {
            return; // TODO: should derived properties even be an allowed part of the configuration model?
        }

        // pre-process the values of the property to address reference type values
        final Session session = jcrNode.getSession();
        final List<Value> verifiedUpdateValues = determineVerifiedValues(updateProperty, session);

        final Property jcrProperty = JcrUtils.getPropertyIfExists(jcrNode, updateProperty.getName());
        if (jcrProperty != null) {
            if (propertyIsIdentical(jcrProperty, updateProperty, verifiedUpdateValues)) {
                return; // no update needed
            }

            if (baselineProperty != null) {
                // property should already exist, and so it does. But has it been changed?
                final List<Value> verifiedBaselineValues = determineVerifiedValues(baselineProperty, session);
                if (!propertyIsIdentical(jcrProperty, baselineProperty, verifiedBaselineValues)) {
                    final String msg = String.format("[OVERRIDE] Property '%s' has been changed in the repository, " +
                                    "and will be overridden due to definition %s.",
                            updateProperty.getPath(), ModelUtils.formatDefinitions(updateProperty));
                    logger.info(msg);
                }
            } else {
                // property should not yet exist, but actually does
                final String msg = String.format("[OVERRIDE] Property '%s' has been created in the repository, " +
                                "and will be overridden due to definition %s.",
                        updateProperty.getPath(), ModelUtils.formatDefinitions(updateProperty));
                logger.info(msg);
            }

            // TODO: is this check adding sufficient value, or can/should we always remove the old property?
            if (updateProperty.getValueType().ordinal() != jcrProperty.getType()
                    || updateProperty.isMultiple() != jcrProperty.isMultiple()) {
                jcrProperty.remove();
            }
        } else {
            if (baselineProperty != null) {
                // property should already exist, doesn't.
                final String msg = String.format("[OVERRIDE] Property '%s' has been deleted from the repository, " +
                                "and will be re-added due to definition %s.",
                        updateProperty.getPath(), ModelUtils.formatDefinitions(updateProperty));
                logger.info(msg);
            }
        }

        try {
            if (updateProperty.isMultiple()) {
                jcrNode.setProperty(updateProperty.getName(), valuesFrom(updateProperty, verifiedUpdateValues, session));
            } else {
                if (verifiedUpdateValues.size() > 0) {
                    jcrNode.setProperty(updateProperty.getName(), valueFrom(updateProperty, verifiedUpdateValues.get(0), session));
                }
            }
        } catch (RepositoryException e) {
            String msg = String.format(
                    "Failed to process property '%s' defined in %s: %s",
                    updateProperty.getPath(), ModelUtils.formatDefinitions(updateProperty), e.getMessage());
            throw new RuntimeException(msg, e);
        }
    }

    private void removeProperty(final String propertyName,
                                final ConfigurationProperty baselineProperty,
                                final Node jcrNode,
                                final ConfigurationNode modelNode) throws RepositoryException, IOException {
        final Property jcrProperty = JcrUtils.getPropertyIfExists(jcrNode, propertyName);
        if (jcrProperty == null) {
            return; // Successful merge, no action needed.
        }

        if (baselineProperty != null) {
            final Session session = jcrNode.getSession();
            final List<Value> verifiedBaselineValues = determineVerifiedValues(baselineProperty, session);
            if (!propertyIsIdentical(jcrProperty, baselineProperty, verifiedBaselineValues)) {
                final String msg = String.format("[OVERRIDE] Property '%s' originally defined in %s has been changed, " +
                                "but will be deleted because it no longer is part of the configuration model.",
                        baselineProperty.getPath(), ModelUtils.formatDefinitions(baselineProperty));
                logger.info(msg);
            }
        } else {
            final String msg = String.format("[OVERRIDE] Property '%s' of node '%s' has been added to the repository, " +
                            "but will be deleted because it is not defined in %s.",
                    propertyName, jcrNode.getPath(), ModelUtils.formatDefinitions(modelNode));
            logger.info(msg);
        }

        jcrProperty.remove();
    }

    private boolean isKnownDerivedPropertyName(final String modelPropertyName) {
        return ArrayUtils.contains(knownDerivedPropertyNames, modelPropertyName);
    }

    private List<Value> determineVerifiedValues(final ConfigurationProperty property, final Session session)
            throws RepositoryException {

        final List<Value> verifiedValues = new ArrayList<>();
        if (property.isMultiple()) {
            for (Value value : property.getValues()) {
                collectVerifiedValue(property, value, verifiedValues, session);
            }
        } else {
            collectVerifiedValue(property, property.getValue(), verifiedValues, session);
        }
        return verifiedValues;
    }

    private void collectVerifiedValue(final ConfigurationProperty modelProperty, final Value value, final List<Value> modelValues, final Session session)
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

    private boolean isReferenceTypeProperty(final ConfigurationProperty modelProperty) {
        return (modelProperty.getValueType() == ValueType.REFERENCE ||
                modelProperty.getValueType() == ValueType.WEAKREFERENCE);
    }

    private String getVerifiedReferenceIdentifier(final ConfigurationProperty modelProperty, final Value modelValue, final Session session)
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
                        nodePath, modelProperty.getPath(), ModelUtils.formatDefinitions(modelProperty)));
                return null;
            }
        } else {
            try {
                session.getNodeByIdentifier(identifier);
            } catch (ItemNotFoundException e) {
                logger.warn(String.format("Reference %s for property '%s' defined in %s not found: skipping.",
                        identifier, modelProperty.getPath(), ModelUtils.formatDefinitions(modelProperty)));
                return null;
            }
        }
        return identifier;
    }

    private boolean propertyIsIdentical(final ConfigurationProperty p1, final ConfigurationProperty p2) throws IOException {
        return p1.getType() == p2.getType()
                && p1.getValueType() == p2.getValueType()
                && valuesAreIdentical(p1, p2);
    }

    private boolean valuesAreIdentical(final ConfigurationProperty p1, final ConfigurationProperty p2) throws IOException {
        if (p1.isMultiple()) {
            final Value[] v1 = p1.getValues();
            final Value[] v2 = p2.getValues();

            if (v1.length != v2.length) {
                return false;
            }
            for (int i = 0; i < v1.length; i++) {
                if (!valueIsIdentical(v1[i], v2[i])) {
                    return false;
                }
            }
            return true;
        }
        return valueIsIdentical(p1.getValue(), p2.getValue());
    }

    private boolean valueIsIdentical(final Value v1, final Value v2) throws IOException {
        // Type equality at the property level is sufficient, no need to check for type equality at value level.

        switch (v1.getType()) {
            case STRING:
                return getStringValue(v1).equals(getStringValue(v2));
            case BINARY:
                try (final InputStream v1InputStream = getBinaryInputStream(v1);
                     final InputStream v2InputStream = getBinaryInputStream(v2)) {
                    return IOUtils.contentEquals(v1InputStream, v2InputStream);
                }
            case URI:
            case NAME:
            case PATH:
            case REFERENCE:
            case WEAKREFERENCE:
                return v1.getString().equals(v2.getString());
            default:
                return v1.getObject().equals(v2.getObject());
        }
    }

    private boolean propertyIsIdentical(final Property jcrProperty, final ConfigurationProperty modelProperty,
                                        final List<Value> modelValues) throws RepositoryException, IOException {
        return modelProperty.getValueType().ordinal() == jcrProperty.getType()
                && modelProperty.isMultiple() == jcrProperty.isMultiple()
                && valuesAreIdentical(modelProperty, modelValues, jcrProperty);
    }

    private boolean valuesAreIdentical(final ConfigurationProperty modelProperty, final List<Value> modelValues,
                                       final Property jcrProperty) throws RepositoryException, IOException {
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

    private boolean valueIsIdentical(final ConfigurationProperty modelProperty,
                                     final Value modelValue,
                                     final javax.jcr.Value jcrValue) throws RepositoryException, IOException {
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

    private InputStream getResourceInputStream(final Value modelValue) throws IOException {
        return getResourceInputStream(modelValue.getParent().getDefinition().getSource(), modelValue.getString());
    }

    private InputStream getResourceInputStream(final Source source, final String resourceName) throws IOException {
        return source.getModule().getConfigResourceInputProvider().getResourceInputStream(source, resourceName);
    }

    private javax.jcr.Value[] valuesFrom(final ConfigurationProperty modelProperty,
                                         final List<Value> modelValues,
                                         final Session session) throws RepositoryException, IOException {
        final javax.jcr.Value[] jcrValues = new javax.jcr.Value[modelValues.size()];
        for (int i = 0; i < jcrValues.length; i++) {
            jcrValues[i] = valueFrom(modelProperty, modelValues.get(i), session);
        }
        return jcrValues;
    }

    private javax.jcr.Value valueFrom(final ConfigurationProperty modelProperty,
                                      final Value modelValue, final Session session)
            throws RepositoryException, IOException {
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
                        modelProperty.getPath(), ModelUtils.formatDefinitions(modelProperty), type);
                throw new RuntimeException(msg);
        }
    }

    private void postProcessReferences(final List<UnprocessedReference> references)
            throws RepositoryException, IOException {
        for (UnprocessedReference reference : references) {
            updateProperty(reference.updateProperty, reference.baselineProperty, reference.targetNode);
        }
    }
}
