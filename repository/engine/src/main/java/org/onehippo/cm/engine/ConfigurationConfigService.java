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

package org.onehippo.cm.engine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.core.NodeImpl;
import org.hippoecm.repository.decorating.NodeDecorator;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.cm.model.ConfigurationItemCategory;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.ConfigurationNode;
import org.onehippo.cm.model.ConfigurationProperty;
import org.onehippo.cm.model.FileResourceInputProvider;
import org.onehippo.cm.model.Module;
import org.onehippo.cm.model.NamespaceDefinition;
import org.onehippo.cm.model.NodePathSegment;
import org.onehippo.cm.model.PropertyType;
import org.onehippo.cm.model.Source;
import org.onehippo.cm.model.Value;
import org.onehippo.cm.model.ValueType;
import org.onehippo.cm.model.WebFileBundleDefinition;
import org.onehippo.cm.model.impl.ConfigurationNodeImpl;
import org.onehippo.cm.model.impl.ConfigurationPropertyImpl;
import org.onehippo.cm.model.impl.NodePathSegmentImpl;
import org.onehippo.cm.model.impl.ValueImpl;
import org.onehippo.cm.model.util.SnsUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.webfiles.WebFilesService;
import org.onehippo.repository.bootstrap.util.BootstrapUtils;
import org.onehippo.repository.bootstrap.util.PartialZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.JCR_UUID;
import static org.onehippo.cm.engine.ValueProcessor.determineVerifiedValues;
import static org.onehippo.cm.engine.ValueProcessor.isKnownDerivedPropertyName;
import static org.onehippo.cm.engine.ValueProcessor.isReferenceTypeProperty;
import static org.onehippo.cm.engine.ValueProcessor.isUuidInUse;
import static org.onehippo.cm.engine.ValueProcessor.propertyIsIdentical;
import static org.onehippo.cm.engine.ValueProcessor.valueFrom;
import static org.onehippo.cm.engine.ValueProcessor.valuesFrom;
import static org.onehippo.cm.model.util.FilePathUtils.getParentOrFsRoot;

/**
 * ConfigurationConfigService is responsible for reading and writing Configuration from/to the repository.
 * Access to the repository is provided to this service through the API ({@link javax.jcr.Node} or
 * {@link Session}), this service is stateless.
 */
public class ConfigurationConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationConfigService.class);

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

    void writeWebfiles(final ConfigurationModel model, final Session session) throws IOException {
        if (!model.getWebFileBundleDefinitions().isEmpty()) {
            final WebFilesService service = HippoServiceRegistry.getService(WebFilesService.class);
            if (service == null) {
                log.warn(String.format("Skipping import web file bundles: service '%s' not available.",
                        WebFilesService.class.getName()));
                return;
            }
            for (WebFileBundleDefinition webFileBundleDefinition : model.getWebFileBundleDefinitions()) {
                final String bundleName = webFileBundleDefinition.getName();
                log.debug(String.format("processing web file bundle '%s' defined in %s.", bundleName,
                        webFileBundleDefinition.getOrigin()));

                final Module module = webFileBundleDefinition.getSource().getModule();
                if (module.isArchive()) {
                    final PartialZipFile bundleZipFile = new PartialZipFile(module.getArchiveFile(), bundleName);
                    service.importJcrWebFileBundle(session, bundleZipFile, true);
                } else {
                    final FileResourceInputProvider resourceInputProvider =
                            (FileResourceInputProvider) module.getConfigResourceInputProvider();
                    final Path modulePath = getParentOrFsRoot(resourceInputProvider.getBasePath());
                    service.importJcrWebFileBundle(session, modulePath.resolve(bundleName).toFile(), true);
                }
            }
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
        applyNodeTypes(update.getNamespaceDefinitions(), session);

        final ConfigurationNode baselineRoot = baseline.getConfigurationRootNode();
        final Node targetNode = session.getNode(baselineRoot.getPath().toString());
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

            log.debug(String.format("processing namespace prefix='%s' uri='%s' defined in %s.",
                    prefix, uriString, namespaceDefinition.getOrigin()));

            if (prefixes.contains(prefix)) {
                final String repositoryURI = session.getNamespaceURI(prefix);
                if (!uriString.equals(repositoryURI)) {
                    final String msg = String.format("Failed to process namespace definition defined in %s: " +
                                    "namespace with prefix '%s' already exists in repository with different URI. " +
                                    "Existing: '%s', target: '%s'. Changing existing namespaces is not supported. Aborting.",
                            namespaceDefinition.getOrigin(), prefix, repositoryURI, uriString);
                    throw new ConfigurationRuntimeException(msg);
                }
            } else {
                session.getWorkspace().getNamespaceRegistry().registerNamespace(prefix, uriString);
            }
        }
    }

    private void applyNodeTypes(final List<? extends NamespaceDefinition> nsDefinitions, final Session session)
            throws RepositoryException, IOException {
        for (NamespaceDefinition nsDefinition : nsDefinitions) {
            if (nsDefinition.getCndPath() != null) {
                final String cndPath = nsDefinition.getCndPath().getString();
                if (log.isDebugEnabled()) {
                    log.debug(String.format("processing CND '%s' defined in %s.", cndPath, nsDefinition.getOrigin()));
                }

                // TODO: nodeTypeStream should be closed, right?
                final InputStream nodeTypeStream = getResourceInputStream(nsDefinition.getSource(), cndPath);
                final String cndPathOrigin = String.format("'%s' (%s)", cndPath, nsDefinition.getOrigin());
                BootstrapUtils.initializeNodetypes(session, nodeTypeStream, cndPathOrigin);
            }
        }
    }

    private void computeAndWriteNodeDelta(final ConfigurationNode baselineNode,
                                  final ConfigurationNode updateNode,
                                  final Node targetNode,
                                  final boolean forceApply,
                                  final List<UnprocessedReference> unprocessedReferences)
            throws RepositoryException, IOException {

        if (targetNode.isLocked()) {
            log.warn("Target node {} is locked, skipping it's processing", targetNode.getPath());
            final LockManager lockManager = targetNode.getSession().getWorkspace().getLockManager();
            try {
                final Lock lock = lockManager.getLock(targetNode.getPath());
                if (lock.isDeep()) {
                    return;
                }
            } catch (LockException ignored) {
            }
        } else {
            computeAndWritePrimaryTypeDelta(baselineNode, updateNode, targetNode, forceApply);
            computeAndWriteMixinTypesDelta(baselineNode, updateNode, targetNode, forceApply);
            computeAndWritePropertiesDelta(baselineNode, updateNode, targetNode, forceApply, unprocessedReferences);
        }

        computeAndWriteChildNodesDelta(baselineNode, updateNode, targetNode, forceApply, unprocessedReferences);
    }

    private void computeAndWritePrimaryTypeDelta(final ConfigurationNode baselineNode,
                                                 final ConfigurationNode updateNode,
                                                 final Node targetNode,
                                                 final boolean forceApply)
            throws RepositoryException {

        final ConfigurationProperty baselinePrimaryTypeProperty = baselineNode.getProperty(JCR_PRIMARYTYPE);
        final ConfigurationProperty updatePrimaryTypeProperty = updateNode.getProperty(JCR_PRIMARYTYPE);

        final String updatePrimaryType = updatePrimaryTypeProperty.getValue().getString();
        final String baselinePrimaryType = baselinePrimaryTypeProperty.getValue().getString();

        if (forceApply || !updatePrimaryType.equals(baselinePrimaryType)) {
            final String jcrPrimaryType = targetNode.getPrimaryNodeType().getName();
            if (jcrPrimaryType.equals(updatePrimaryType)) {
                return;
            }

            if (!jcrPrimaryType.equals(baselinePrimaryType)) {
                final String msg = forceApply
                        ? String.format("[OVERRIDE] Primary type '%s' of node '%s' is adjusted to '%s' as defined in %s.",
                                jcrPrimaryType, updateNode.getPath(), updatePrimaryType,
                        updatePrimaryTypeProperty.getOrigin())
                        : String.format("[OVERRIDE] Primary type '%s' of node '%s' has been changed from '%s'."
                                + "Overriding to type '%s', defined in %s.",
                                jcrPrimaryType, updateNode.getPath(), baselinePrimaryType, updatePrimaryType,
                        updatePrimaryTypeProperty.getOrigin());
                log.info(msg);
            }
            targetNode.setPrimaryType(updatePrimaryType);
        }
    }

    private void computeAndWriteMixinTypesDelta(final ConfigurationNode baselineNode,
                                                final ConfigurationNode updateNode,
                                                final Node targetNode,
                                                final boolean forceApply) throws RepositoryException {

        final ConfigurationProperty baselineMixinProperty = baselineNode.getProperty(JCR_MIXINTYPES);
        final ConfigurationProperty updateMixinProperty = updateNode.getProperty(JCR_MIXINTYPES);

        // Update the mixin types, if needed
        final Value[] updateMixinValues = updateMixinProperty != null ? updateMixinProperty.getValues() : new Value[0];
        final Value[] baselineMixinValues = baselineMixinProperty != null ? baselineMixinProperty.getValues() : new Value[0];

        // Add / restore mixin types
        for (Value mixinValue : updateMixinValues) {
            final String mixin = mixinValue.getString();
            if (!hasMixin(targetNode, mixin)) {
                if (forceApply) {
                    if (hasMixin(baselineMixinValues, mixin)) {
                        final String msg = String.format("[OVERRIDE] Mixin '%s' has been removed from node '%s', " +
                                        "but is re-added because it is defined at %s.",
                                mixin, updateNode.getPath(), updateMixinProperty.getOrigin());
                        log.info(msg);
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
                                jcrMixin, updateNode.getPath(), updateNode.getOrigin());
                        log.info(msg);
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
        return Arrays.stream(node.getMixinNodeTypes()).anyMatch(mixinType -> mixinType.getName().equals(mixin));
    }

    private boolean hasMixin(final Value[] mixinValues, final String mixin) {
        return Arrays.stream(mixinValues).anyMatch(mixinValue -> mixinValue.getString().equals(mixin));
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

        final Map<String, ? extends ConfigurationProperty> updateProperties = updateNode.getProperties();
        final Map<String, ? extends ConfigurationProperty> baselineProperties = baselineNode.getProperties();

        for (String propertyName : updateProperties.keySet()) {
            if (propertyName.equals(JCR_PRIMARYTYPE) || propertyName.equals(JCR_MIXINTYPES)) {
                continue; // we have already addressed these properties
            }
            if (propertyName.equals(JCR_UUID)) {
                continue; // nor need to look at immutable jcr:uuid
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
            for (String propertyName : getDeletablePropertyNames(targetNode)) {
                if (!updateProperties.containsKey(propertyName)
                        && updateNode.getChildPropertyCategory(propertyName) == ConfigurationItemCategory.CONFIG) {
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

    private List<String> getDeletablePropertyNames(final Node node) throws RepositoryException {
        final List<String> names = new ArrayList<>();
        for (Property property : new PropertyIterable(node.getProperties())) {
            final ItemDefinition definition = property.getDefinition();
            final String name = property.getName();
            if (!definition.isProtected() && !definition.isMandatory() && !isKnownDerivedPropertyName(name)) {
                names.add(name);
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

        final Map<String, ? extends ConfigurationNode> updateChildren = updateNode.getNodes();
        final Map<String, ? extends ConfigurationNode> baselineChildren = baselineNode.getNodes();

        // Add or update child nodes

        for (String indexedChildName : updateChildren.keySet()) {
            ConfigurationNode baselineChild = baselineChildren.get(indexedChildName);
            final ConfigurationNode updateChild = updateChildren.get(indexedChildName);
            final NodePathSegment nameAndIndex = NodePathSegmentImpl.get(indexedChildName);
            final Node existingChildNode = getChildWithIndex(targetNode, nameAndIndex.getName(), nameAndIndex.getIndex());
            final Node childNode;

            if (existingChildNode == null) {
                // need to add node
                final String childPrimaryType = updateChild.getProperty(JCR_PRIMARYTYPE).getValue().getString();
                final String childName = nameAndIndex.getName();

                if (baselineChild == null) {
                    baselineChild = newChildOfType(childPrimaryType);
                } else {
                    if (forceApply) {
                        final String msg = String.format("[OVERRIDE] Node '%s' has been removed, " +
                                        "but will be re-added due to definition %s.",
                                updateChild.getPath(), updateChild.getOrigin());
                        log.info(msg);
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
                    if (updateNode.getChildNodeCategory(indexedChildName) == ConfigurationItemCategory.CONFIG) {
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
            final NodePathSegment nameAndIndex = NodePathSegmentImpl.get(indexedChildName);
            final Node childNode = getChildWithIndex(targetNode, nameAndIndex.getName(), nameAndIndex.getIndex());
            if (childNode != null) {
                if (!baselineChildren.containsKey(indexedChildName)) {
                    final String msg = String.format("[OVERRIDE] Child node '%s' exists, " +
                                    "but will be deleted while processing the children of node '%s' defined in %s.",
                            indexedChildName, updateNode.getPath(), updateNode.getOrigin());
                    log.info(msg);
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
        final ConfigurationProperty uuidProperty = childModelNode.getProperty(JCR_UUID);
        if (uuidProperty != null) {
            final String uuid = uuidProperty.getValue().getString();
            if (!isUuidInUse(uuid, parentNode.getSession())) {
                // uuid not in use: create node with the requested uuid
                final NodeImpl parentNodeImpl = (NodeImpl) NodeDecorator.unwrap(parentNode);
                return parentNodeImpl.addNodeWithUuid(childName, childPrimaryType, uuid);
            } else {
                log.warn(String.format("Specified jcr:uuid %s for node '%s' defined in %s already in use: "
                                + "a new jcr:uuid will be generated instead.",
                        uuid, childModelNode.getPath(), childModelNode.getOrigin()));
            }
        }
        return parentNode.addNode(childName, childPrimaryType);
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
            if (indexedTargetNodeChildNames.contains(indexedModelName)) {
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

        final Property jcrProperty = JcrUtils.getPropertyIfExists(jcrNode, updateProperty.getName());

        if (jcrProperty != null && jcrProperty.getDefinition().isProtected()) {
            return; // protected properties cannot be update, no need to even check or try
        }

        // pre-process the values of the property to address reference type values
        final Session session = jcrNode.getSession();
        final List<Value> verifiedUpdateValues = determineVerifiedValues(updateProperty, session);

        if (jcrProperty != null) {
            if (propertyIsIdentical(jcrProperty, updateProperty, verifiedUpdateValues)) {
                return; // no update needed
            }

            if (baselineProperty != null) {
                // property should already exist, and so it does. But has it been changed?
                if (!propertyIsIdentical(jcrProperty, baselineProperty)) {
                    final String msg = String.format("[OVERRIDE] Property '%s' has been changed in the repository, " +
                                    "and will be overridden due to definition %s.",
                            updateProperty.getPath(), updateProperty.getOrigin());
                    log.info(msg);
                }
            } else {
                // property should not yet exist, but does, with a different value.
                // This should not trigger an [OVERRIDE] message if the property is autoCreated and its parent
                // has just been created.
                if (!(jcrProperty.getDefinition().isAutoCreated() && jcrNode.isNew())) {
                    final String msg = String.format("[OVERRIDE] Property '%s' has been created in the repository, " +
                                    "and will be overridden due to definition %s.",
                            updateProperty.getPath(), updateProperty.getOrigin());
                    log.info(msg);
                }
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
                        updateProperty.getPath(), updateProperty.getOrigin());
                log.info(msg);
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
                    updateProperty.getPath(), updateProperty.getOrigin(), e.getMessage());
            throw new ConfigurationRuntimeException(msg, e);
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

        if (jcrProperty.getDefinition().isProtected()) {
            return; // protected properties cannot be removed, no need to even check or try
        }

        if (baselineProperty != null) {
            if (!propertyIsIdentical(jcrProperty, baselineProperty)) {
                final String msg = String.format("[OVERRIDE] Property '%s' originally defined in %s has been changed, " +
                                "but will be deleted because it no longer is part of the configuration model.",
                        baselineProperty.getPath(), baselineProperty.getOrigin());
                log.info(msg);
            }
        } else {
            final String msg = String.format("[OVERRIDE] Property '%s' of node '%s' has been added to the repository, " +
                            "but will be deleted because it is not defined in %s.",
                    propertyName, jcrNode.getPath(), modelNode.getOrigin());
            log.info(msg);
        }

        jcrProperty.remove();
    }

    private InputStream getResourceInputStream(final Source source, final String resourceName) throws IOException {
        return source.getModule().getConfigResourceInputProvider().getResourceInputStream(source, resourceName);
    }

    private void postProcessReferences(final List<UnprocessedReference> references)
            throws RepositoryException, IOException {
        for (UnprocessedReference reference : references) {
            updateProperty(reference.updateProperty, reference.baselineProperty, reference.targetNode);
        }
    }
}
