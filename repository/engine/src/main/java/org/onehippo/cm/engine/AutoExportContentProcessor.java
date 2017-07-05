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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.cm.engine.autoexport.AutoExportConfig;
import org.onehippo.cm.model.ConfigurationItemCategory;
import org.onehippo.cm.model.Group;
import org.onehippo.cm.model.PropertyOperation;
import org.onehippo.cm.model.ValueType;
import org.onehippo.cm.model.impl.ConfigSourceImpl;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.ConfigurationNodeImpl;
import org.onehippo.cm.model.impl.ConfigurationPropertyImpl;
import org.onehippo.cm.model.impl.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.DefinitionPropertyImpl;
import org.onehippo.cm.model.impl.ValueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Sets.newHashSet;
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.JCR_UUID;
import static org.onehippo.cm.engine.Constants.PRODUCT_GROUP_NAME;
import static org.onehippo.cm.engine.ValueProcessor.isKnownDerivedPropertyName;
import static org.onehippo.cm.engine.ValueProcessor.valueFrom;
import static org.onehippo.cm.model.util.ConfigurationModelUtils.getCategoryForNode;
import static org.onehippo.cm.model.util.SnsUtils.createIndexedName;

/**
 * Node auto export
 */
public class AutoExportContentProcessor extends ExportContentProcessor {

    private static final Logger log = LoggerFactory.getLogger(AutoExportContentProcessor.class);

    private ConfigurationModelImpl configurationModel;
    private AutoExportConfig autoExportConfig;
    private static final Set<String> suppressedDelta = newHashSet(
            // TODO: move these somewhere more permanent
            // facet-related generated property
            "hippo:count"
    );

    public AutoExportContentProcessor(final ConfigurationModelImpl configurationModel, final AutoExportConfig autoExportConfig) {
        this.configurationModel = configurationModel;
        this.autoExportConfig = autoExportConfig;
    }

    protected void exportProperties(final Node sourceNode, final DefinitionNodeImpl definitionNode) throws RepositoryException {
        exportPrimaryTypeAndMixins(sourceNode, definitionNode);
        for (final Property property : new PropertyIterable(sourceNode.getProperties())) {
            if (!propertyShouldBeSkipped(property)) {
                exportProperty(property, definitionNode);

            }
        }
        definitionNode.sortProperties();
    }

    protected boolean propertyShouldBeSkipped(Property property) throws RepositoryException {

        if (super.propertyShouldBeSkipped(property)) {
            return true;
        }

        final String propName = property.getName();
        // skip suppressed properties if we're in auto-export mode
        if ((autoExportConfig != null) && suppressedDelta.contains(propName)) {
            return true;
        }
        // use Configuration.filterUuidPaths during auto-export (suppressing export of jcr:uuid)
        return (autoExportConfig != null) && propName.equals(JCR_UUID)
                && autoExportConfig.shouldFilterUuid(property.getNode().getPath());

    }

    /**
     * Generate appropriate Definitions and DefinitionNodes within configSource to represent the difference between
     * the JCR state and the model state for jcrPath and all descendants. In the normal case, this will result in a new
     * definition for each changed Node. However, if a SNS exists anywhere within jcrPath, the definition must have a
     * root above the node with SNSs.
     * @param session
     * @param jcrPath
     * @param configSource
     * @throws RepositoryException
     */
    public void exportConfigNode(final Session session, final String jcrPath, final ConfigSourceImpl configSource)
            throws RepositoryException, IOException {

        // first, check if we should be looking at this node at all
        if (shouldExcludeNode(jcrPath)) {
            return;
        }

        // if the parent node of jcrPath doesn't exist, we are in a strange error state and need to bail out fast
        // we can skip this check for the root node or a top-level node
        if (!jcrPath.equals("/") && (jcrPath.lastIndexOf("/") > 0)) {
            final String parentPath = StringUtils.substringBeforeLast(jcrPath, "/");
            if (!session.nodeExists(parentPath)) {
                // todo: throw a more specific exception that we can catch in EventJournalProcessor
                throw new RepositoryException("Parent path of change is missing for path: " + parentPath);
            }
        }

        // if the jcrPath doesn't exist, we need a delete definition, and that's all
        if (!session.nodeExists(jcrPath)) {
            log.debug("Deleting node: \n\t{}", jcrPath);
            final DefinitionNodeImpl definitionNode = configSource.getOrCreateDefinitionFor(jcrPath);
            definitionNode.delete();
            return;
        }

        final Node jcrNode = session.getNode(jcrPath);
        if (isVirtual(jcrNode)) {
            log.debug("Ignoring node because it is virtual:\n\t{}", jcrPath);
            return;
        }

        final ConfigurationNodeImpl configNode = configurationModel.resolveNode(jcrPath);
        if (configNode == null) {
            // this is a brand new node, so we can skip the delta comparisons and just dump the JCR tree into one def

            String newPath = jcrPath;
            Node newNode = jcrNode;
            // determine the actual 'root' of the new node, ensuring we're not skipping intermediate missing parents
            while (configurationModel.resolveNode(StringUtils.substringBeforeLast(newPath, "/")) == null) {
                newPath = StringUtils.substringBeforeLast(newPath, "/");
            }
            if (!newPath.equals(jcrPath)) {
                // need to export missing parent(s) as well
                newNode = session.getNode(newPath);
            }
            log.debug("Creating new node def without delta: \n\t{}", newPath);

            final DefinitionNodeImpl definitionNode = configSource.getOrCreateDefinitionFor(newPath);

            exportProperties(newNode, definitionNode);
            for (final Node childNode : new NodeIterable(newNode.getNodes())) {
                exportNode(childNode, definitionNode, Collections.emptySet());
            }
        }
        else {
            // otherwise, we need to do a detailed comparison
            exportConfigNodeDelta(jcrNode, configNode, configSource);
        }
    }

    protected boolean shouldExcludeNode(final String jcrPath) {
        if (configurationModel != null) {
            final ConfigurationItemCategory category = getCategoryForNode(jcrPath, configurationModel);
            if (category != ConfigurationItemCategory.CONFIG) {
                log.debug("Ignoring node because of category:{} \n\t{}", category, jcrPath);
                return true;
            }
        }
        if (autoExportConfig != null && autoExportConfig.isExcludedPath(jcrPath)) {
            log.debug("Ignoring node because of auto-export exclusion:\n\t{}", jcrPath);
            return true;
        }
        return false;
    }

    private DefinitionNodeImpl exportPropertiesDelta(final Node jcrNode,
                                                     final ConfigurationNodeImpl configNode,
                                                     final ConfigSourceImpl configSource,
                                                     final ConfigurationModelImpl model)
            throws RepositoryException, IOException {

        DefinitionNodeImpl defNode = exportPrimaryTypeDelta(jcrNode, configNode, configSource);
        defNode = exportMixinsDelta(jcrNode, defNode, configNode, configSource);

        // add new properties
        for (final Property jcrProperty : new PropertyIterable(jcrNode.getProperties())) {

            final String propName = jcrProperty.getName();
            if (propName.equals(JCR_PRIMARYTYPE) || propName.equals(JCR_MIXINTYPES)) {
                continue;
            }
            if (isKnownDerivedPropertyName(propName) || suppressedDelta.contains(propName)) {
                continue;
            }
            // skip protected properties, which are managed internally by JCR and don't make sense in export
            // (except JCR:UUID, which we need to do references properly)
            if (!propName.equals(JCR_UUID) && jcrProperty.getDefinition().isProtected()) {
                continue;
            }
            if (configNode.getChildPropertyCategory(propName) != ConfigurationItemCategory.CONFIG) {
                // skip SYSTEM property
                continue;
            }
            // use Configuration.filterUuidPaths during delta computation (suppressing export of jcr:uuid)
            if (propName.equals(JCR_UUID) && autoExportConfig.shouldFilterUuid(configNode.getPath())) {
                continue;
            }

            if (propName.equals(JCR_UUID) && !configNode.getDefinitions().isEmpty()) {
                Group group = configNode.getDefinitions().get(0).getDefinition().getSource().getModule().getProject().getGroup();
                if (PRODUCT_GROUP_NAME.equals(group.getName())) {
                    continue;
                }
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
                defNode.addProperty(configProperty, null, new ValueImpl[0]).setOperation(PropertyOperation.DELETE);
            }
        }
        return defNode;
    }

    private DefinitionNodeImpl exportPrimaryTypeDelta(final Node jcrNode,
                                                      final ConfigurationNodeImpl configNode,
                                                      final ConfigSourceImpl configSource) throws RepositoryException {
        final String configPrimaryType = configNode.getProperty(JCR_PRIMARYTYPE).getValue().getString();
        if (!jcrNode.getPrimaryNodeType().getName().equals(configPrimaryType)) {
            final DefinitionNodeImpl defNode = configSource.getOrCreateDefinitionFor(jcrNode.getPath());
            final Property primaryTypeProperty = jcrNode.getProperty(JCR_PRIMARYTYPE);
            final ValueImpl value = valueFrom(primaryTypeProperty, defNode);
            defNode.addProperty(JCR_PRIMARYTYPE, value).setOperation(PropertyOperation.OVERRIDE);
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
            return configSource.getOrCreateDefinitionFor(jcrNode.getPath());
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
                                       final ConfigSourceImpl configSource)
            throws RepositoryException, IOException {

        log.debug("Building delta for node: \n\t{}", jcrNode.getPath());

        // first look at properties, since that's the simple case
        DefinitionNodeImpl defNode = exportPropertiesDelta(jcrNode, configNode, configSource, configurationModel);

        // Early check if we will need to care for node ordering (at the end)
        final boolean orderingIsRelevant = jcrNode.getPrimaryNodeType().hasOrderableChildNodes()
                && (configNode.getIgnoreReorderedChildren() == null || !configNode.getIgnoreReorderedChildren());

        // check if we need to add children
        //   and already build an indexed list of non-skipped/ignored jcrChildNodeNames if we need to check node ordering
        final List<String> indexedJcrChildNodeNames = new ArrayList<>();
        for (final Node childNode : new NodeIterable(jcrNode.getNodes())) {
            if (autoExportConfig != null && autoExportConfig.isExcludedPath(childNode.getPath())) {
                log.debug("Ignoring node because of auto-export exclusion:\n\t{}", childNode.getPath());
                continue;
            }
            final String indexedJcrChildNodeName = createIndexedName(childNode);
            final ConfigurationItemCategory category = configNode.getChildNodeCategory(indexedJcrChildNodeName);
            if (category != ConfigurationItemCategory.CONFIG) {
                log.debug("Ignoring child node because of category:{} \n\t{}", category, childNode.getPath());
                continue;
            }

            if (orderingIsRelevant) {
                indexedJcrChildNodeNames.add(indexedJcrChildNodeName);
            }

            final ConfigurationNodeImpl childConfigNode = configNode.getNode(indexedJcrChildNodeName);
            if (childConfigNode == null) {
                // the config doesn't know about this child, so do a full export without delta comparisons
                // yes, defNode is indeed supposed to be the _parent's_ defNode
                defNode = createDefNodeIfNecessary(defNode, jcrNode, configSource);
                exportNode(childNode, defNode, Collections.emptySet());
            } else {
                // call top-level recursion, not this delta method
                exportConfigNode(childNode.getSession(), childNode.getPath(), configSource);
            }
        }

        // check if we need to delete children
        for (final String childConfigNode : configNode.getNodes().keySet()) {
            if (!jcrNode.hasNode(childConfigNode)) {
                final DefinitionNodeImpl childNode = configSource
                        .getOrCreateDefinitionFor(String.join("/", configNode.getPath(), childConfigNode));
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

        if (orderingIsRelevant) {
            updateOrdering(indexedJcrChildNodeNames, configNode, configSource);
        }
    }

    /**
     * Because we've processed jcrNode's children in order, all new definitions (add node) are already in the correct
     * order. To fix the ordering of inserted and reordered nodes, we walk over jcrNode's children again, comparing
     * their order to the order of configNode's children. Where necessary, we insert "order-before" meta-data.
     *
     * example: consider the configuration child nodes
     *
     *  A, B, C
     *
     * and the JCR child nodes
     *
     *  X, A, Y, C, B*, Z
     *
     * where B* has different properties or children from B. When entering this method, the list of child definition
     * nodes already populated is
     *
     *  X, Y, B*, Z
     *
     * (i.e. the new or changed nodes). Below method will add order-before (->) to this list as follows:
     *
     *  X->A, Y->C, B*, Z, C->B*
     *
     * When merging these definitions, this should be sufficient information to put the nodes into the correct order.
     */
    private void updateOrdering(final List<String> indexedJcrChildNodeNames, final ConfigurationNodeImpl configNode,
                                final ConfigSourceImpl configSource) throws RepositoryException {
        final List<String> indexedConfigNodeNames = new ArrayList<>();
        for (String indexedConfigChildNodeName : configNode.getNodes().keySet()) {
            if (indexedJcrChildNodeNames.contains(indexedConfigChildNodeName)) {
                indexedConfigNodeNames.add(indexedConfigChildNodeName);
            }
        }
        final List<String> processedConfigNodeNames = new ArrayList<>();

        for (int jcrIndex = 0, configIndex = 0; jcrIndex < indexedJcrChildNodeNames.size(); jcrIndex++) {
            final String indexedJcrChildNodeName = indexedJcrChildNodeNames.get(jcrIndex);
            if (indexedConfigNodeNames.contains(indexedJcrChildNodeName)) {
                // pre-existing child node
                if (indexedJcrChildNodeName.equals(indexedConfigNodeNames.get(configIndex))) {
                    // at expected index, consume.
                    configIndex++;
                    // also skip all already processed config child nodes
                    while (configIndex < indexedConfigNodeNames.size()
                            && processedConfigNodeNames.contains(indexedConfigNodeNames.get(configIndex))) {
                        configIndex++;
                    }
                } else {
                    // not at expected index, set order-before
                    setOrderBefore(configNode, indexedJcrChildNodeName, indexedJcrChildNodeNames.get(jcrIndex + 1), configSource);
                    processedConfigNodeNames.add(indexedJcrChildNodeName);
                }
            } else {
                if (configIndex < indexedConfigNodeNames.size()) {
                    // not after all configuration nodes, set order-before
                    setOrderBefore(configNode, indexedJcrChildNodeName, indexedJcrChildNodeNames.get(jcrIndex + 1), configSource);
                }
            }
        }
    }

    private void setOrderBefore(final ConfigurationNodeImpl configNode, final String childName, final String beforeName,
                                final ConfigSourceImpl configSource) {
        final DefinitionNodeImpl childNode = configSource
                .getOrCreateDefinitionFor(String.join("/", configNode.getPath(), childName));
        childNode.setOrderBefore(beforeName);
    }

}
