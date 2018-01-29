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
package org.onehippo.cm.engine.autoexport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.cm.engine.JcrContentExporter;
import org.onehippo.cm.engine.ValueProcessor;
import org.onehippo.cm.model.Group;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.impl.source.ConfigSourceImpl;
import org.onehippo.cm.model.impl.tree.ConfigurationNodeImpl;
import org.onehippo.cm.model.impl.tree.ConfigurationPropertyImpl;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.tree.DefinitionPropertyImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.tree.ConfigurationItemCategory;
import org.onehippo.cm.model.tree.PropertyOperation;
import org.onehippo.cm.model.tree.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.emptyList;
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.jackrabbit.JcrConstants.JCR_UUID;
import static org.onehippo.cm.engine.Constants.PRODUCT_GROUP_NAME;
import static org.onehippo.cm.engine.ValueProcessor.valueFrom;
import static org.onehippo.cm.model.tree.ConfigurationItemCategory.CONTENT;
import static org.onehippo.cm.model.tree.ConfigurationItemCategory.SYSTEM;
import static org.onehippo.cm.model.util.SnsUtils.createIndexedName;

/**
 * Node auto export
 */
public class AutoExportConfigExporter extends JcrContentExporter {

    private static final Logger log = LoggerFactory.getLogger(AutoExportConfigExporter.class);

    // this local field just allows us to avoid casting where we need the full AutoExportConfig API
    private AutoExportConfig exportConfig;

    private ConfigurationModelImpl configurationModel;
    private PathsMap addedContent;
    private PathsMap deletedContent;

    AutoExportConfigExporter(final ConfigurationModelImpl configurationModel, final AutoExportConfig exportConfig,
                                    final PathsMap addedContent, final PathsMap deletedContent) {
        super(exportConfig);
        this.exportConfig = exportConfig;
        this.configurationModel = configurationModel;
        this.addedContent = addedContent;
        this.deletedContent = deletedContent;
    }

    protected boolean shouldExcludeProperty(Property property) throws RepositoryException {
        // super implements default excludes and ExportConfig excludes
        if (super.shouldExcludeProperty(property)) {
            return true;
        }

        // default to exporting anything not explicitly suppressed
        return false;
    }

    protected boolean shouldExcludeNode(final String jcrPath) {
        // super implements default excludes and ExportConfig excludes
        if (super.shouldExcludeNode(jcrPath)) {
            return true;
        }

        if (configurationModel != null) {
            // use getCategoryForItem from ExportConfig to account for possible exporter category overrides
            final ConfigurationItemCategory category = exportConfig.getCategoryForItem(jcrPath, false, configurationModel);
            if (category != ConfigurationItemCategory.CONFIG) {
                log.debug("Ignoring node because of category:{} \n\t{}", category, jcrPath);
                return true;
            }
        }
        return false;
    }

    /**
     * Generate appropriate Definitions and DefinitionNodes within configSource to represent the difference between
     * the JCR state and the model state for jcrPath and all descendants. In the normal case, this will result in a new
     * definition for each changed Node. However, if a SNS exists anywhere within jcrPath, the definition must have a
     * root above the node with SNSs.
     */
    void exportConfigNode(final Session session, final String jcrPath, final ConfigSourceImpl configSource)
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
                // todo: throw a more specific exception to be catched by invoker
                throw new RepositoryException("Parent path of change is missing for path: " + parentPath);
            }
        }

        /* TODO: AutoExportConfigExporter is *currently* only invoked with *changed* paths, not *deleted* paths
                 This is non-optimal, as it requires adding the parent path as 'changed', which therefore also
                 causes all other siblings to be 'evaluated' for changes where there (at least likely) are none.
                 If/when that is improved the following section needs to be re-enabled, but *then* handling of
                 possible child *content* paths also need to be recorded as well, see checkDeletedContentChildren(JcrPath)

                 In the meantime, because the following section now is commented out, passing in a path for a
                 deleted node *will* cause a RepositoryException (ItemNotFoundException) to be thrown!

        // if the jcrPath doesn't exist, we need a delete definition, and that's all
        if (!session.nodeExists(jcrPath)) {
            log.debug("Deleting node: \n\t{}", jcrPath);
            final DefinitionNodeImpl definitionNode = configSource.getOrCreateDefinitionFor(jcrPath);
            definitionNode.delete();
            return;
        }
         */

        final Node jcrNode = session.getNode(jcrPath);
        if (isVirtual(jcrNode)) {
            log.debug("Ignoring node because it is virtual:\n\t{}", jcrPath);
            return;
        }


        final ConfigurationNodeImpl configNode = configurationModel.resolveNode(jcrPath);
        if (configNode == null) {
            //is it a deleted node or subnode?
            ConfigurationNodeImpl deletedNode = configurationModel.resolveDeletedNode(JcrPaths.getPath(jcrPath));
            deletedNode = deletedNode != null ? deletedNode : configurationModel.resolveDeletedSubNode(JcrPaths.getPath(jcrPath));
            if (deletedNode != null) {
                //run export delta against deleted node
                exportConfigNodeDelta(jcrNode, deletedNode, configSource);
                return;
            }

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

            exportNode(newNode, null, configSource);
        }
        else {
            // otherwise, we need to do a detailed comparison
            exportConfigNodeDelta(jcrNode, configNode, configSource);
        }
    }

    /**
     * Full export of a new node, which is a replacement implementation of {@link JcrContentExporter#exportNode(Node, DefinitionNodeImpl, Set)}
     * to also inject .meta:residual-child-node-category properties in definitions matching the patterns from the
     * auto-export configuration (REPO-1730).
     * <p>In case the category "content" is injected, skip exporting child nodes and instead record these child nodes paths
     * as added content paths to be handled by the DefinitionMergeService later.</p>
     * <p>In case the category "system" is injected, skip exporting child nodes altogether.</p>
     */
    protected void exportNode(final Node jcrNode, final DefinitionNodeImpl parentDefinition,
                              final ConfigSourceImpl configSource) throws RepositoryException {
        if (!isVirtual(jcrNode) && !shouldExcludeNode(jcrNode.getPath())) {

            final DefinitionNodeImpl definitionNode =
                    parentDefinition != null
                            ? parentDefinition.addNode(createNodeName(jcrNode))
                            : configSource.getOrCreateDefinitionFor(jcrNode.getPath());

            exportProperties(jcrNode, definitionNode);

            final String jcrPath = jcrNode.getPath();

            final ConfigurationItemCategory inject =
                    exportConfig.getInjectResidualMatchers().getMatch(jcrPath, jcrNode.getPrimaryNodeType().getName());

            if (inject == CONTENT || inject == SYSTEM) {
                // for hst:hosts, we need to support both injecting and overriding at the same time
                final ConfigurationItemCategory override = exportConfig.getOverrideResidualMatchers().getMatch(jcrPath);
                final ConfigurationItemCategory effective = override != null ? override : inject;
                if (effective == CONTENT) {
                    for (final Node child : new NodeIterable(jcrNode.getNodes())) {
                        final String childPath = child.getPath();
                        // make sure child node is not virtual nor pre-excluded
                        // NOTE: this is explicitly NOT using shouldExcludeNode(String), because of the category check there
                        if (!(isVirtual(child) || exportConfig.isExcludedPath(childPath))) {
                            // found a new content root node: record it (if not already recorded)
                            if (!addedContent.matches(childPath)) {
                                addedContent.add(childPath);
                            }
                        }
                    }
                }
                definitionNode.setResidualChildNodeCategory(inject);
                if (effective != ConfigurationItemCategory.CONFIG) {
                    // skip config export of !CONFIG children
                    return;
                }
            }

            for (final Node childNode : new NodeIterable(jcrNode.getNodes())) {
                exportNode(childNode, definitionNode, configSource);
            }
        }
    }

    protected void exportProperties(final Node sourceNode, final DefinitionNodeImpl definitionNode) throws RepositoryException {
        exportPrimaryTypeAndMixins(sourceNode, definitionNode);
        for (final Property property : new PropertyIterable(sourceNode.getProperties())) {
            if (!shouldExcludeProperty(property)) {
                exportProperty(property, definitionNode);
            }
        }
        definitionNode.sortProperties();
    }

    protected DefinitionNodeImpl exportPropertiesDelta(final Node jcrNode,
                                                       final ConfigurationNodeImpl configNode,
                                                       final ConfigSourceImpl configSource,
                                                       final ConfigurationModelImpl model)
            throws RepositoryException, IOException {

        DefinitionNodeImpl defNode = exportPrimaryTypeDelta(jcrNode, configNode, configSource);
        defNode = exportMixinsDelta(jcrNode, defNode, configNode, configSource);

        // add new properties
        for (final Property jcrProperty : new PropertyIterable(jcrNode.getProperties())) {

            // first, check general exclusion rules
            if (shouldExcludeProperty(jcrProperty)) {
                continue;
            }

            final String propName = jcrProperty.getName();
            // skip SYSTEM properties
            if (configNode.getChildPropertyCategory(propName) != ConfigurationItemCategory.CONFIG) {
                continue;
            }

            // don't export UUID for any nodes included in product config
            // NOTE: this doesn't work perfectly, since shouldExcludeProperty() isn't aware of this
            //       check when exporting new nodes added while working on the product itself
            if (propName.equals(JCR_UUID) && !configNode.getDefinitions().isEmpty()) {
                Group group = configNode.getDefinitions().get(0).getDefinition().getSource().getModule().getProject().getGroup();
                if (PRODUCT_GROUP_NAME.equals(group.getName())) {
                    continue;
                }
            }

            ConfigurationPropertyImpl configProperty = configNode.getProperty(propName);
            if (configProperty == null) {
                final ConfigurationPropertyImpl deletedProperty =
                        configurationModel.resolveDeletedProperty(configNode.getJcrPath().resolve(propName));
                if (deletedProperty != null) {
                    //It is a deleted property, so do delta export
                    defNode = exportPropertyDelta(jcrProperty, deletedProperty, defNode, configSource);
                } else {
                    // full export
                    defNode = createDefNodeIfNecessary(defNode, jcrNode, configSource);
                    exportProperty(jcrProperty, defNode);
                }
            } else {
                // delta export
                defNode = exportPropertyDelta(jcrProperty, configProperty, defNode, configSource);
            }
        }

        // delete removed properties
        for (final ConfigurationPropertyImpl configProperty: configNode.getProperties()) {
            final String configPropertyName = configProperty.getName();
            if (!jcrNode.hasProperty(configPropertyName)) {
                defNode = createDefNodeIfNecessary(defNode, jcrNode, configSource);
                defNode.addProperty(configPropertyName, null, emptyList()).setOperation(PropertyOperation.DELETE);
            }
        }
        return defNode;
    }

    protected DefinitionNodeImpl exportPrimaryTypeDelta(final Node jcrNode,
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

    protected DefinitionNodeImpl exportMixinsDelta(final Node jcrNode, DefinitionNodeImpl definitionNode,
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
                if (mixinsProperty.getValues().stream().anyMatch(v -> !jcrMixins.contains(v.getString()))) {
                    op = PropertyOperation.OVERRIDE;
                } else {
                    mixinsProperty.getValues().stream().forEach(v->jcrMixins.remove(v.getString()));
                    if (!jcrMixins.isEmpty()) {
                        op = PropertyOperation.ADD;
                    }
                }
            }
            if (!jcrMixins.isEmpty()) {
                definitionNode = createDefNodeIfNecessary(definitionNode, jcrNode, configSource);
                DefinitionPropertyImpl propertyDef = definitionNode.addProperty(JCR_MIXINTYPES,
                        ValueType.NAME, jcrMixins.stream().map(ValueImpl::new).collect(Collectors.toList()));
                if (op != null) {
                    propertyDef.setOperation(op);
                }
            }
        } else if (mixinsProperty != null) {
            definitionNode = createDefNodeIfNecessary(definitionNode, jcrNode, configSource);
            definitionNode.addProperty(JCR_MIXINTYPES, ValueType.NAME, emptyList())
                    .setOperation(PropertyOperation.DELETE);
        }
        return definitionNode;
    }

    protected DefinitionNodeImpl createDefNodeIfNecessary(final DefinitionNodeImpl definitionNode,
                                                          final Node jcrNode,
                                                          final ConfigSourceImpl configSource) throws RepositoryException {
        if (definitionNode == null) {
            return configSource.getOrCreateDefinitionFor(jcrNode.getPath());
        }
        else {
            return definitionNode;
        }
    }

    protected DefinitionNodeImpl exportPropertyDelta(final Property property, final ConfigurationPropertyImpl configProperty,
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
        } else if (configurationModel.resolveDeletedProperty(configProperty.getJcrPath()) != null) {
            //Create restore property with empty value
            definitionNode = createDefNodeIfNecessary(definitionNode, property.getParent(), configSource);
            exportProperty(property, definitionNode);
        }
        return definitionNode;
    }

    protected void exportConfigNodeDelta(final Node jcrNode, final ConfigurationNodeImpl configNode,
                                         final ConfigSourceImpl configSource)
            throws RepositoryException, IOException {

        log.debug("Building delta for node: \n\t{}", jcrNode.getPath());

        // first look at properties, since that's the simple case
        DefinitionNodeImpl defNode = exportPropertiesDelta(jcrNode, configNode, configSource, configurationModel);
        if (defNode == null && configurationModel.resolveNode(configNode.getJcrPath()) == null) {
            defNode = configSource.getOrCreateDefinitionFor(jcrNode.getPath());
        }

        // Early check if we will need to care for node ordering (at the end)
        final boolean orderingIsRelevant = jcrNode.getPrimaryNodeType().hasOrderableChildNodes()
                && (configNode.getIgnoreReorderedChildren() == null || !configNode.getIgnoreReorderedChildren());

        // check if we need to add children
        //   and already build an indexed list of non-skipped/ignored jcrChildNodeNames if we need to check node ordering
        final List<String> indexedJcrChildNodeNames = new ArrayList<>();
        for (final Node childNode : new NodeIterable(jcrNode.getNodes())) {
            if (isVirtual(childNode) || shouldExcludeNode(childNode.getPath())) {
                continue;
            }

            final String indexedJcrChildNodeName = createIndexedName(childNode);
            if (orderingIsRelevant) {
                indexedJcrChildNodeNames.add(indexedJcrChildNodeName);
            }

            final ConfigurationNodeImpl childConfigNode = configNode.getNode(indexedJcrChildNodeName);
            if (childConfigNode == null) {
                boolean nodeIsDeleted = configurationModel.getDeletedConfigNodes().keySet().stream()
                        .anyMatch(configNode.getJcrPath().resolve(indexedJcrChildNodeName)::startsWith);
                if (nodeIsDeleted) {
                    // call top-level recursion, not this delta method
                    exportConfigNode(childNode.getSession(), childNode.getPath(), configSource);
                } else {
                    // the config doesn't know about this child, or wasn't deleted so do a full export without delta comparisons
                    // yes, defNode is indeed supposed to be the _parent's_ defNode
                    defNode = createDefNodeIfNecessary(defNode, jcrNode, configSource);
                    exportNode(childNode, defNode, (ConfigSourceImpl)null);
                }
            } else {
                // call top-level recursion, not this delta method
                exportConfigNode(childNode.getSession(), childNode.getPath(), configSource);
            }
        }

        // check if we need to delete children
        for (final ConfigurationNodeImpl childConfigNode : configNode.getNodes()) {
            if (!jcrNode.hasNode(childConfigNode.getName())) {
                final DefinitionNodeImpl childNode = configSource
                        .getOrCreateDefinitionFor(childConfigNode.getJcrPath());
                if (childNode == null) {
                    log.error("Produced a null result for path: {}!",
                            childConfigNode.getJcrPath(),
                            new IllegalStateException());
                }
                else {
                    checkDeletedContentChildren(childNode.getJcrPath());
                    childNode.delete();
                }
            }
        }

        if (orderingIsRelevant) {
            updateOrdering(indexedJcrChildNodeNames, configNode, configSource);
        }
    }

    /*
     * When a config node is deleted (in jcr), check if there were child nodes which mapped to *content* definitions,
     * and if so record these as 'to be deleted' content paths for the DefinitionMergeService to handle later.
     */
    protected void checkDeletedContentChildren(final JcrPath deletedConfig) throws RepositoryException {
        for (final ContentDefinitionImpl contentDefinition : configurationModel.getContentDefinitions()) {
            final JcrPath contentRootPath = contentDefinition.getNode().getJcrPath();
            final String contentRoot = contentRootPath.toMinimallyIndexedPath().toString();
            if (contentRootPath.startsWith(deletedConfig) && !deletedContent.matches(contentRoot)) {
                // content root found as child of a deleted config path, which itself, or a parent path, hasn't been recorded as deleted yet
                deletedContent.removeChildren(contentRoot);
                deletedContent.add(contentRoot);
            }
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
    protected void updateOrdering(final List<String> indexedJcrChildNodeNames, final ConfigurationNodeImpl configNode,
                                  final ConfigSourceImpl configSource) throws RepositoryException {
        final List<String> indexedConfigNodeNames = new ArrayList<>();
        for (final ConfigurationNodeImpl childConfigNode : configNode.getNodes()) {
            final String indexedConfigChildNodeName = childConfigNode.getName();
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

    protected void setOrderBefore(final ConfigurationNodeImpl configNode, final String childName, final String beforeName,
                                  final ConfigSourceImpl configSource) {
        final DefinitionNodeImpl childNode = configSource
                .getOrCreateDefinitionFor(configNode.getJcrPath().resolve(childName));
        childNode.setOrderBefore(beforeName);
    }
}
