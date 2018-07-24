/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.time.StopWatch;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cm.engine.ConfigurationContentService;
import org.onehippo.cm.engine.JcrContentExporter;
import org.onehippo.cm.engine.ValueProcessor;
import org.onehippo.cm.engine.autoexport.orderbeforeholder.ContentOrderBeforeHolder;
import org.onehippo.cm.engine.autoexport.orderbeforeholder.LocalConfigOrderBeforeHolder;
import org.onehippo.cm.engine.autoexport.orderbeforeholder.OrderBeforeHolder;
import org.onehippo.cm.engine.autoexport.orderbeforeholder.OrderBeforeUtils;
import org.onehippo.cm.engine.autoexport.orderbeforeholder.UpstreamConfigOrderBeforeHolder;
import org.onehippo.cm.model.definition.ActionItem;
import org.onehippo.cm.model.definition.ActionType;
import org.onehippo.cm.model.definition.Definition;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.definition.AbstractDefinitionImpl;
import org.onehippo.cm.model.impl.definition.ConfigDefinitionImpl;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.definition.NamespaceDefinitionImpl;
import org.onehippo.cm.model.impl.source.ConfigSourceImpl;
import org.onehippo.cm.model.impl.source.ContentSourceImpl;
import org.onehippo.cm.model.impl.source.SourceImpl;
import org.onehippo.cm.model.impl.tree.ConfigurationItemImpl;
import org.onehippo.cm.model.impl.tree.ConfigurationNodeImpl;
import org.onehippo.cm.model.impl.tree.ConfigurationPropertyImpl;
import org.onehippo.cm.model.impl.tree.ConfigurationTreeBuilder;
import org.onehippo.cm.model.impl.tree.DefinitionItemImpl;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.tree.DefinitionPropertyImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.path.JcrPathSegment;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.source.Source;
import org.onehippo.cm.model.source.SourceType;
import org.onehippo.cm.model.tree.DefinitionNode;
import org.onehippo.cm.model.tree.ModelProperty;
import org.onehippo.cm.model.tree.PropertyOperation;
import org.onehippo.cm.model.util.FilePathUtils;
import org.onehippo.cm.model.util.PatternSet;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.onehippo.cm.engine.autoexport.AutoExportConstants.DEFAULT_MAIN_CONFIG_FILE;
import static org.onehippo.cm.model.Constants.HST_HST_PATH;
import static org.onehippo.cm.model.Constants.HST_PREFIX;
import static org.onehippo.cm.model.definition.DefinitionType.NAMESPACE;
import static org.onehippo.cm.model.tree.ConfigurationItemCategory.SYSTEM;
import static org.onehippo.cm.model.tree.PropertyOperation.OVERRIDE;
import static org.onehippo.cm.model.tree.PropertyOperation.REPLACE;
import static org.onehippo.cms7.utilities.io.FilePathUtils.cleanFilePath;

public class DefinitionMergeService {

    private static final Logger log = LoggerFactory.getLogger(DefinitionMergeService.class);

    private static class ModuleMapping {
        final String mvnPath;
        final Collection<String> repositoryPaths;

        final PatternSet pathPatterns;

        ModuleMapping(final String mvnPath, final Collection<String> repositoryPaths) {
            this.mvnPath = mvnPath;
            this.repositoryPaths = repositoryPaths;

            List<String> patterns = new ArrayList<>();
            for (String repositoryPath : repositoryPaths) {
                patterns.add(repositoryPath);
                patterns.add(repositoryPath.equals("/") ? "/**" : repositoryPath + "/**");
            }
            pathPatterns = new PatternSet(patterns);
        }

        boolean matchesPath(JcrPath path) {
            return pathPatterns.matches(path.toString());
        }
    }

    private ModuleMapping defaultModuleMapping = null;
    private final HashMap<String, ModuleMapping> moduleMappings = new HashMap<>();

    private final Map<String, ModuleImpl> toExport = new LinkedHashMap<>();
    private final AutoExportConfig autoExportConfig;
    private final ConfigurationModelImpl model;
    private final Session jcrSession;

    // Registry for paths of nodes for which the child nodes need to be reordered
    private Set<JcrPath> reorderRegistry = new HashSet<>();

    /**
     * @param autoExportConfig the current auto-export module config, which includes module:path mappings and exclusions
     * @param baseline the full model out of which to pull the exported modules
     * @param jcrSession JCR session to be used for regenerating changed content sources
     */
    public DefinitionMergeService(final AutoExportConfig autoExportConfig,
                                  final ConfigurationModelImpl baseline,
                                  final Session jcrSession) {
        this.jcrSession = jcrSession;
        this.autoExportConfig = autoExportConfig;

        // preprocess config mapping paths to moduleMapping objects
        // note: this is very similar to the old auto-export EventProcessor init
        for (Map.Entry<String, Collection<String>> entry : autoExportConfig.getModules().entrySet()) {
            String modulePath = entry.getKey();
            Collection<String> repositoryPaths = entry.getValue();
            ModuleMapping mapping = new ModuleMapping(modulePath, repositoryPaths);
            if (repositoryPaths.contains("/")) {
                defaultModuleMapping = mapping;
            } else {
                moduleMappings.put(mapping.mvnPath, mapping);
            }
        }
        if (defaultModuleMapping == null) {
            throw new IllegalStateException("The repository root path must be mapped to an exported module!");
        }

        // find the modules that are configured for auto-export and also have a mvnPath indicating a source location
        final Set<String> configuredMvnPaths = new HashSet<>();
        configuredMvnPaths.addAll(moduleMappings.keySet());
        configuredMvnPaths.add(defaultModuleMapping.mvnPath);

        // collect and clone the modules to be exported, then build a copy of the baseline using the clones
        // this clone will be modified in-place to keep an internally consistent view of the new definitions, but the
        // given baseline model will be kept unchanged to avoid unintended side-effects in case of errors
        extractExportModules(baseline, configuredMvnPaths, true);
        model = rebuild(baseline);

        // rebuilding the model weirdly moves all of the sources to new ModuleImpl instances, so we need to
        // throw away the references in toExport and grab them again
        extractExportModules(model, configuredMvnPaths, false);
    }

    /**
     * Build a convenient map of exported modules by mvnPath, optionally cloning the exported modules.
     * @param baseline the full model out of which to pull the exported modules
     * @param configuredMvnPaths mvnPath values that match with exported modules
     * @param clone should the extracted module be cloned?
     */
    protected void extractExportModules(final ConfigurationModelImpl baseline,
                                        final Set<String> configuredMvnPaths,
                                        final boolean clone) {
        toExport.clear();
        for (final ModuleImpl m : baseline.getModules()) {
            if (m.getMvnPath() != null && configuredMvnPaths.contains(m.getMvnPath())) {
                toExport.put(m.getMvnPath(), clone ? m.clone() : m);
            }
        }
    }

    /**
     * Create a new ConfigurationModel with the updated definitions in the toExport modules.
     * @param baseline the existing model upon which we'll base the new one
     * @return the new ConfigurationModel, which references Sources from the old Modules in baseline and toExport
     */
    protected ConfigurationModelImpl rebuild(final ConfigurationModelImpl baseline) {
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // note: we assume that the original baseline will perform any required cleanup in close(), so we don't need
        //       to copy FileSystems etc. here
        final ConfigurationModelImpl model = new ConfigurationModelImpl();
        toExport.values().forEach(model::addReplacementModule);
        baseline.getSortedGroups().forEach(model::addGroup);
        model.build();

        stopWatch.stop();
        log.debug("Model rebuilt for auto-export merge in {}", stopWatch.toString());
        return model;
    }

    /**
     * Given a baseline B, a set of changes to that baseline in the current JCR runtime (R) R∆B expressed as a
     * ModuleImpl, and a set of destination modules Sm, produce a new version of the destination modules Sm' such that
     * B-Sm+Sm' = B+R∆B. Also, make a best effort for Sources and Definitions in Sm' to be as minimally changed compared
     * to the corresponding Sources and Definitions in Sm as possible (for stable output), and for any new Sources and
     * Definitions to follow the sorting schemes encoded in {@link LocationMapper}.
     * @param changes R∆B expressed as a Module with one ConfigSource with zero-or-more Definitions and zero-or-more ContentSources
     * @return a new version of each toMerge module, if revisions are necessary
     */
    public Collection<ModuleImpl> mergeChangesToModules(final ModuleImpl changes,
                                                        final Set<String> contentAdded,
                                                        final Set<String> contentChanged,
                                                        final Set<String> contentDeleted) {
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        log.debug("Merging changes to modules: {}", toExport.values());
        log.debug("Content added: {} changed: {} deleted: {}", contentAdded, contentChanged, contentDeleted);

        // make sure the changes module has all the definitions nicely sorted
        changes.build();

        // merge namespace definitions first
        for (final NamespaceDefinitionImpl nsd : changes.getNamespaceDefinitions()) {
            mergeNamespace(nsd);
        }

        // note: it doesn't make sense to auto-export webfilebundle definitions, so that definition type isn't handled

        // reset the reorderRegistry for a new merge run
        reorderRegistry.clear();

        // merge config changes
        // ConfigDefinitions are already sorted by root path
        for (final ConfigDefinitionImpl change : changes.getConfigDefinitions()) {
            // run the full and complex merge logic, recursively
            mergeConfigDefinitionNode(change.getNode());
        }

        // merge content changes
        mergeContentDefinitions(contentAdded, contentChanged, contentDeleted);

        // map maintaining for each content definition that must be re-exported the order before that must be exported
        final Map<JcrPath, String> contentOrderBefores = new HashMap<>();

        reorder(contentOrderBefores);

        exportChangedContentSources(contentOrderBefores);

        toExport.values().forEach(ModuleImpl::build);

        stopWatch.stop();
        log.info("Completed full auto-export merge in {}", stopWatch.toString());

        return toExport.values();
    }

    /**
     * Reorder all paths in reorderRegistry.
     *
     * Creates and/or updates local config definitions in the AutoExport modules, and flags the relevant sources
     * as modified. Populates the list contentOrderBefores for local content definitions and flags the relevant
     * sources as modified.
     */
    private void reorder(final Map<JcrPath, String> contentOrderBefores) {

        final List<String> sortedModules = new ArrayList<>();
        model.getModulesStream().forEach((module) -> sortedModules.add(module.getFullName()));

        for (final JcrPath path : reorderRegistry) {
            try {
                final Node jcrNode = jcrSession.getNode(path.toString());
                boolean orderingIsRelevant = jcrNode.getPrimaryNodeType().hasOrderableChildNodes();

                final ConfigurationNodeImpl configurationNode = model.resolveNode(path);
                if (configurationNode != null) {
                    orderingIsRelevant &= (configurationNode.getIgnoreReorderedChildren() == null
                            || !configurationNode.getIgnoreReorderedChildren());
                }

                if (orderingIsRelevant) {
                    reorder(path, jcrNode, configurationNode, contentOrderBefores, sortedModules);
                }
            } catch (PathNotFoundException ignore) {
                log.warn("Could not find path '{}', skipping", path.toString());
                return;
            } catch (RepositoryException e) {
                throw new IllegalStateException("Unexpected RepositoryException while reordering node " + path, e);
            }
        }
    }

    /**
     * Reorders the children of the node at the given path.
     *
     * Creates and/or updates local config definitions in the AutoExport modules, and flags the relevant sources
     * as modified. Populates the list contentOrderBefores for local content definitions and flags the relevant
     * sources as modified.
     */
    private void reorder(final JcrPath path,
                         final Node jcrNode,
                         final ConfigurationNodeImpl configurationNode,
                         final Map<JcrPath, String> contentOrderBefores,
                         final List<String> sortedModules) throws RepositoryException {

        /* The algorithm works essentially in these steps:
         *  1) determine the expected ordering from JCR
         *  2) build up an intermediate state by applying config and content definitions that contribute nodes that
         *     have 'path' as parent
         *  3) log warnings for any discrepancies between the final intermediate state and the expected ordering
         *     (see #logWarningsForDiscrepencies for details)
         *
         *  To build up the intermediate state, perform these steps:
         *  1) collect all upstream and local config definitions that contribute to the ordering of these nodes;
         *     each definition is captured in a OrderBeforeHolder object, which, depending on the type, keeps state
         *  2) apply all definitions in the same order as if they would be applied to JCR and if the result is not what
         *     is expected, add additional order before instructions:
         *      a) apply all upstream config definitions, build up the result in intermediate
         *      b) determine if there any names in intermediate that are incorrectly ordered, if so, remove those from
         *         intermediate and add an additional _local_ config holders to adjust their order
         *      c) apply all local config definitions, add those to intermediate and record order before if needed
         *      d) 'finish' each holder to write its state and/or do cleanup
         *  3) apply upstream content definitions, add those to intermediate
         *  4) apply local content definitions, add those to intermediate and record order before if needed
         *
         *  Note that lists of config and content holders are sorted using the same rules that are used by the
         *  ConfigurationTreeBuilder. See ConfigOrderBeforeHolder#compareTo and ContentOrderBeforeHolder#compareTo
         *  for details.
         */

        log.debug("Reordering node {}", path.toString());

        final ImmutableList<JcrPathSegment> expected = getExpectedOrder(jcrNode, configurationNode);
        final List<JcrPathSegment> intermediate = new LinkedList<>();

        final Holders configHolders = createConfigHolders(configurationNode, expected, sortedModules);

        configHolders.upstream.forEach((holder) -> holder.apply(expected, intermediate));

        updateStateForIncorrectlyOrderedUpstream(path, configurationNode, expected, sortedModules,
                intermediate, configHolders.local);

        configHolders.local.forEach((holder) -> holder.apply(expected, intermediate));
        configHolders.finish();

        applyUpstreamContentDefinitions(path, intermediate);
        applyLocalContentDefinitions(path, expected, intermediate, contentOrderBefores);

        logWarningsForDiscrepencies(path, expected, intermediate);
    }

    /**
     * Inspects the names in intermediate to validate they are in the correct order. Any names that are not in the
     * correct order are removed from intermediate and a new holder is created for it. As the last step, the list of
     * holders is sorted again to ensure they are in their processing order.
     * @param path          the path to the node being sorted
     * @param expected      the expected ordering
     * @param sortedModules the list of all modules, sorted according to their processing order
     * @param intermediate  (in & out) the intermediate order of the sub nodes of the given path
     * @param holders       (in & out) the set of holders for the given path
     */
    private void updateStateForIncorrectlyOrderedUpstream(final JcrPath path,
                                                          final ConfigurationNodeImpl configurationNode,
                                                          final ImmutableList<JcrPathSegment> expected,
                                                          final List<String> sortedModules,
                                                          final List<JcrPathSegment> intermediate,
                                                          final List<OrderBeforeHolder> holders) {

        // if there are 0 or just 1 upstream items in intermediate, they are in the correct order
        if (intermediate.size() < 2) {
            return;
        }

        final List<JcrPathSegment> incorrectlyOrdered = getIncorrectlyOrdered(expected, intermediate);

        intermediate.removeAll(incorrectlyOrdered);

        for (final JcrPathSegment childName : incorrectlyOrdered) {
            final ConfigurationNodeImpl childCfgNode = configurationNode.getNode(childName);
            final Optional<DefinitionNodeImpl> maybeChildDefNode = getLastLocalDef(childCfgNode);
            final DefinitionNodeImpl childDefNode =
                    maybeChildDefNode.orElseGet(() -> getOrCreateLocalDef(path.resolve(childName), null));
            final int moduleIndex =
                    sortedModules.indexOf(childDefNode.getDefinition().getSource().getModule().getFullName());
            holders.add(new LocalConfigOrderBeforeHolder(moduleIndex, childDefNode,
                    (deleteDefItem) -> removeOneDefinitionItem(deleteDefItem, new ArrayList<>())));
        }

        holders.sort(Comparator.naturalOrder());
    }

    /**
     * Returns the list of segments in 'intermediate' that are not in same order as seen in 'expected'
     */
    static List<JcrPathSegment> getIncorrectlyOrdered(final ImmutableList<JcrPathSegment> expected,
                                                      final List<JcrPathSegment> intermediate) {

        if (intermediate.isEmpty()) {
            return Collections.emptyList();
        }
        final List<JcrPathSegment> incorrectlyOrdered = new ArrayList<>();
        int lastCorrectIndex = expected.indexOf(intermediate.get(0));
        for (int i = 1; i < intermediate.size(); i++) {
            final JcrPathSegment current = intermediate.get(i);
            final int currentIndex = expected.indexOf(current);
            if (currentIndex > lastCorrectIndex) {
                lastCorrectIndex = currentIndex;
            } else {
                incorrectlyOrdered.add(current);
            }
        }

        return incorrectlyOrdered;
    }

    /**
     * Return the list of segments in 'jcrNode' that can be sorted. Currently, nodes that are categorized as 'system'
     * cannot be sorted, those are not returned.
     */
    private ImmutableList<JcrPathSegment> getExpectedOrder(final Node jcrNode, final ConfigurationNodeImpl configurationNode)
            throws RepositoryException {

        final List<JcrPathSegment> expectedOrder = new ArrayList<>();

        for (final Node child : new NodeIterable(jcrNode.getNodes())) {
            final JcrPathSegment segment = JcrPaths.getSegment(child);
            if (configurationNode != null) {
                if (configurationNode.getChildNodeCategory(segment) == SYSTEM) {
                    log.info("Not including node '{}' while reordering '{}'; the node is category 'system'",
                            segment.toString(), jcrNode.getPath());
                    continue;
                }
            }
            expectedOrder.add(segment);
        }

        return ImmutableList.copyOf(expectedOrder);
    }

    /**
     * Find all upstream content definitions that have a root node that has 'path' as its parent and add them to
     * intermediate using their order before info.
     * @param path          the path to the node being sorted
     * @param intermediate  (in & out) the intermediate order of the sub nodes of the given path
     */
    private void applyUpstreamContentDefinitions(final JcrPath path, final List<JcrPathSegment> intermediate) {
        final Consumer<ContentDefinitionImpl> consumer = (cdi) -> OrderBeforeUtils.insert(cdi.getNode(), intermediate);

        model.getModulesStream()
                .filter(m -> !toExport.values().contains(m))
                .forEach(m -> processContentDefinitions(m, path, intermediate, consumer));
    }

    /**
     * Find all local content definitions that have a root node that has 'path' as its parent and add them to
     * intermediate, use 'expected' to determine if the content definition must be re-exported with a different
     * order before than is currently used. If the definition must be re-exported, updates 'contentOrderBefores' and
     * marks the source as changed.
     * @param path                the path to the node being sorted
     * @param expected            the expected ordering
     * @param intermediate        (in & out) the intermediate order of the sub nodes of the given path
     * @param contentOrderBefores (in & out) map maintaining for each content definition that must be re-exported the
     *                            order before that must be exported
     */
    private void applyLocalContentDefinitions(final JcrPath path,
                                              final ImmutableList<JcrPathSegment> expected,
                                              final List<JcrPathSegment> intermediate,
                                              final Map<JcrPath, String> contentOrderBefores) {

        for (final ModuleImpl module : toExport.values()) {
            // Create the holders for local content definitions
            final List<ContentOrderBeforeHolder> holders = new ArrayList<>();
            final Consumer<ContentDefinitionImpl> consumer =
                    (cdi) -> holders.add(new ContentOrderBeforeHolder(cdi, contentOrderBefores));
            processContentDefinitions(module, path, intermediate, consumer);

            // Sort the list on just the lexical ordering of the path (#processContentDefinitions visits items based on
            // their original processing order which takes their order before into account)
            holders.sort(Comparator.naturalOrder());

            // Apply each holder and save its state
            for (final ContentOrderBeforeHolder holder : holders) {
                holder.apply(expected, intermediate);
                holder.finish();
            }
        }
    }

    /**
     * Helper method to process the content definitions that have a root node that have 'path' as its parent. Paths
     * that are deleted using content actions are removed from 'intermediate', for each content definition that must
     * be added, 'consumer' is called.
     */
    private void processContentDefinitions(final ModuleImpl module,
                                           final JcrPath path,
                                           final List<JcrPathSegment> intermediate,
                                           final Consumer<ContentDefinitionImpl> consumer) {

        // Analogous to the logic in ConfigurationContentService.apply(ModuleImpl, ConfigurationModel, Session, boolean)
        // See also REPO-1833

        final List<ActionItem> actionItems =
                ConfigurationContentService.collectNewActions(null, module.getActionsMap());

        for (final ActionItem actionItem : actionItems) {
            final JcrPath actionItemJcrPath = actionItem.getPath();
            if (actionItem.getType() == ActionType.DELETE && isRelevantContentDefinition(actionItemJcrPath, path)) {
                intermediate.remove(actionItemJcrPath.getLastSegment());
            }
        }

        final List<ContentDefinitionImpl> sortedDefinitions =
                ConfigurationContentService.getSortedDefinitions(module.getContentDefinitions(), false);

        for (final ContentDefinitionImpl contentDefinition : sortedDefinitions) {
            if (isRelevantContentDefinition(contentDefinition.getNode().getJcrPath(), path)) {
                // Precise action type does not really matter at this point, reload and append are treated the same
                // Relies on the assumption that deleted paths are no longer present as definitions
                // See also REPO-1833
                consumer.accept(contentDefinition);
            }
        }
    }

    private boolean isRelevantContentDefinition(final JcrPath definitionPath, final JcrPath parentPath) {
        // TODO: actually, any ancestor of the parentPath is potentially relevant, but we would have to dig for the
        // TODO: specific node that matters, and we don't have that info for upstream content
        return definitionPath.startsWith(parentPath)
                && definitionPath.getSegmentCount() == parentPath.getSegmentCount() + 1;
    }

    private void logWarningsForDiscrepencies(final JcrPath path, final ImmutableList<JcrPathSegment> expected, final List<JcrPathSegment> intermediate) {
        // There can be names in 'expected' that are not contributed by a definition that a root with 'path' as parent.
        // #getExpectedOrder filters out any 'system' node names, but there may also be names in 'expected' that are
        // contributed by content definitions with a root that is higher up in the content tree.

        final List<JcrPathSegment> notContributedByParent = new ArrayList<>(expected);
        notContributedByParent.removeAll(intermediate);
        for (final JcrPathSegment name : notContributedByParent) {
            log.warn("While reordering '{}': cannot guarantee correct ordering of node '{}' contributed by a higher content definition",
                    path.toString(), name.toString());
        }

        // If during local development, a dev temporarily disables AutoExport, then re-orders in JCR nodes at 'path'
        // that are root of an upstream content definition, then removes the 'autoexport:lastrevision' property,
        // then re-enable AutoExport again, and then cause a change at 'path', then it is possible for 'intermediate'
        // to not be in the exact order as 'expected'.
        //
        // Or -- there is a bug in the reordering mechanism :-/

        final List<JcrPathSegment> incorrectlyOrdered = getIncorrectlyOrdered(expected, intermediate);
        if (incorrectlyOrdered.size() > 0) {
            log.warn("While reordering '{}': intermediate ({}) and expected ({}) are ordered differently, most likely caused by manually updating 'autoexport:lastrevision'",
                    path.toString(), intermediate.toString(), expected.toString());
        }
    }
    private static class Holders {

        final List<OrderBeforeHolder> upstream = new ArrayList<>();
        final List<OrderBeforeHolder> local = new ArrayList<>();
        void finish() {
            upstream.forEach(OrderBeforeHolder::finish);
            local.forEach(OrderBeforeHolder::finish);
        }
    }

    private Holders createConfigHolders(final ConfigurationNodeImpl configurationNode,
                                        final ImmutableList<JcrPathSegment> expected,
                                        final List<String> sortedModules) {

        final Holders holders = new Holders();

        if (configurationNode == null) {
            return holders;
        }

        // Maintain a set of local nodes definitions who's children must be sorted according to the expected ordering
        final Set<DefinitionNodeImpl> reorderChildDefinitions = new HashSet<>();

        // Iterate over all expected child names, check if the child name is config, and if so, create holders for
        // upstream and local definitions that contributed to the configuration node
        for (final JcrPathSegment childName : expected) {
            final ConfigurationNodeImpl childNode = configurationNode.getNode(childName);
            if (childNode == null) {
                continue;
            }
            boolean primaryTypeSeen = false;
            for (final DefinitionNodeImpl childDefNode : childNode.getDefinitions()) {
                final boolean isLocal = isLocalDef().test(childDefNode);

                if (isLocal && !childDefNode.isRoot()) {
                    reorderChildDefinitions.add(childDefNode.getParent());
                }

                boolean createHolder = false;
                if (isNewNodeDefinition(childDefNode) && !primaryTypeSeen) {
                    createHolder = true;
                    primaryTypeSeen = true;
                }
                if (childDefNode.getOrderBefore() != null && !isLocal) {
                    createHolder = true;
                }

                if (createHolder) {
                    final String moduleName = childDefNode.getDefinition().getSource().getModule().getFullName();
                    final int moduleIndex = sortedModules.indexOf(moduleName);
                    if (isLocal) {
                        holders.local.add(new LocalConfigOrderBeforeHolder(moduleIndex, childDefNode,
                                (deleteDefItem) -> removeOneDefinitionItem(deleteDefItem, new ArrayList<>())));
                    } else {
                        holders.upstream.add(new UpstreamConfigOrderBeforeHolder(moduleIndex, childDefNode));
                    }
                } else {
                    // Remove any other local order-before definitions, if they turned out to be needed to reorder
                    // an upstream node, they will be recreated in a later step
                    if (isLocal && childDefNode.getOrderBefore() != null) {
                        childDefNode.setOrderBefore(null);
                        childDefNode.getDefinition().getSource().markChanged();
                        if (childDefNode.isEmpty()) {
                            removeOneDefinitionItem(childDefNode, new ArrayList<>());
                        }
                    }
                }
            }
        }

        reorderChildDefinitions(reorderChildDefinitions, expected);

        holders.local.sort(Comparator.naturalOrder());
        holders.upstream.sort(Comparator.naturalOrder());

        return holders;
    }

    /**
     * Re-orders the child definitions for each of the given parent nodes in the expected order and flags their source
     * as changed.
     */
    private void reorderChildDefinitions(final Set<DefinitionNodeImpl> parents,
                                         final ImmutableList<JcrPathSegment> expected) {
        for (final DefinitionNodeImpl parent : parents) {
            log.debug("Reordering nodes within definition at path '{}' rooted at '{}' in file '{}'",
                    parent.getPath(), parent.getDefinition().getRootPath(), parent.getSourceLocation());
            parent.reorder(expected);
            parent.getDefinition().getSource().markChanged();
        }
    }

    private void exportChangedContentSources(final Map<JcrPath, String> contentOrderBefores) {

        final Set<JcrPath> allExportableContentPaths = collectContentSourcesByNodePath(true).keySet();

        getChangedContentSourcesStream().forEach(source -> {
            final ContentDefinitionImpl def = source.getContentDefinition();
            final JcrPath defPath = def.getNode().getJcrPath();

            // exclude all paths that have their own sources
            final Set<String> excludedPaths = allExportableContentPaths.stream()
                    // (but don't exclude what we're exporting!)
                    .filter(isEqual(defPath).negate())
                    .map(JcrPath::toString).collect(toImmutableSet());

            try {
                new JcrContentExporter(autoExportConfig).exportNode(
                        jcrSession.getNode(defPath.toString()), def, true, contentOrderBefores.get(defPath), excludedPaths);
            }
            catch (RepositoryException e) {
                throw new RuntimeException("Exception while regenerating changed content source file for " + defPath, e);
            }
        });
    }

    /**
     * Merge a single namespace definition into the appropriate toExport module.
     * @param nsd the definition to merge
     *
     */
    protected void mergeNamespace(final NamespaceDefinitionImpl nsd) {

        // find all corresponding definitions by namespace prefix
        final List<NamespaceDefinitionImpl> nsDefs =
                model.getNamespaceDefinitions().stream()
                        .filter(namespaceDefinition -> namespaceDefinition.getPrefix().equals(nsd.getPrefix()))
                        .collect(toList());

        final NamespaceDefinitionImpl lastNsDef = isNotEmpty(nsDefs) ? nsDefs.get(nsDefs.size() - 1): null;

        // clone the CndPath Value which retains a "foreign source" back-reference for use later when copying data
        final ValueImpl cndPath = nsd.getCndPath().clone();

        if (lastNsDef != null) {
            // this is an update to an existing namespace def
            // find the corresponding source by path
            final SourceImpl oldSource = lastNsDef.getSource();

            // this source will have a reference to a module in the baseline, not our clones, so lookup by mvnPath
            final ModuleImpl newModule = toExport.get(oldSource.getModule().getMvnPath());

            // short-circuit this loop iteration if we cannot create a valid merged definition
            if (newModule == null) {
                log.warn("Cannot merge a namespace: {} that belongs to an upstream module", nsd.getPrefix());
                createNsDefinition(nsd, cndPath);
                return;
            }

            // since the old module had a source at this path, we can assume the new module cloned from it does, too
            final ConfigSourceImpl newSource = newModule.getConfigSource(oldSource.getPath()).get();

            log.debug("Merging namespace definition: {} to module: {} aka {} in file {}",
                    nsd.getPrefix(), newModule.getMvnPath(), newModule.getFullName(), newSource.getPath());

            final List<AbstractDefinitionImpl> defs = newSource.getModifiableDefinitions();
            for (int i = 0; i < defs.size(); i++) {
                Definition def = defs.get(i);

                // find the corresponding def within the source by type and namespace prefix
                if (def.getType().equals(NAMESPACE) && ((NamespaceDefinitionImpl)def).getPrefix().equals(nsd.getPrefix())) {
                    // replace the def with a clone of the new def
                    final NamespaceDefinitionImpl newNsd =
                            new NamespaceDefinitionImpl(newSource, nsd.getPrefix(), nsd.getURI(), cndPath);
                    defs.set(i, newNsd);
                    newSource.markChanged();
                }
            }
        }
        else {
            createNsDefinition(nsd, cndPath);
        }
    }

    private void createNsDefinition(final NamespaceDefinitionImpl nsd, final ValueImpl cndPath) {
        // this is a new namespace def -- pretend that it is a node under /hippo:namespaces for sake of file mapping
        final JcrPath incomingPath = JcrPaths.getPath("/hippo:namespaces", nsd.getPrefix());

        // what module should we put it in?
        final ModuleImpl newModule = getModuleByAutoExportConfig(incomingPath);

        // what source should we put it in?
        final ConfigSourceImpl newSource;
        if (newModule.getNamespaceDefinitions().isEmpty()) {
            // We don't have any namespaces yet, so we can generate a new source and put it there
            newSource = createConfigSourceIfNecessary(DEFAULT_MAIN_CONFIG_FILE, newModule);
        }
        else {
            // if we have any existing namespace definitions in the destination module, we have to keep all
            // new namespaces in that same source file, due to validation rules on our model
            newSource = newModule.getNamespaceDefinitions().get(0).getSource();
        }

        log.debug("Creating new namespace definition: {} in module: {} aka {} in file {}",
                nsd.getPrefix(), newModule.getMvnPath(), newModule.getFullName(), newSource.getPath());

        newSource.addNamespaceDefinition(nsd.getPrefix(), nsd.getURI(), cndPath);
    }

    /**
     * Recursively and incrementally merge an incoming "diff" definition into existing toExport modules. Note: this
     * method typically expects to operate on one and only one DefinitionNodeImpl per recursive step, and for the
     * model's configurationNode tree to be updated to match the new state of definitions before the recursive call
     * is made as the final step of this method. The only exception to these expectations is that createNewNode()
     * will fully handle all child nodes of the new "diff" definition in a single recursive step, performing the
     * relevant recursion itself via recursiveAdd()+recursiveCopy() as needed. There is now also a gap in
     * {@link #mergePropertyThatShouldExist(DefinitionPropertyImpl, ConfigurationNodeImpl, ConfigurationPropertyImpl)},
     * where properties that are removed from local modules are not updated in the ConfigurationNode tree.
     * @param incomingDefNode a "diff" definition that should be merged into the toExport modules
     */
    protected void mergeConfigDefinitionNode(final DefinitionNodeImpl incomingDefNode) {
        log.debug("Merging config change for path: {}", incomingDefNode.getJcrPath());

        // this is a tripwire for testing error handling via AutoExportIntegrationTest.merge_error_handling()
        // to run the test, uncomment the following 3 lines and remove the @Ignore annotation on that test
//        if (incomingDefNode.getJcrPath().equals("/config/TestNodeThatShouldCauseAnExceptionOnlyInTesting")) {
//            throw new RuntimeException("this is a simulated failure!");
//        }

        // check whether the incoming def represents a restore of a previously-deleted node, and if so, restore it
        if (restoreDeletedNodesIfNecessary(incomingDefNode)) {
            return;
        }

        // TODO: could a restored-deleted node incorrectly fall into the createNewNode() case?
        final boolean nodeIsNew = isNewNodeDefinition(incomingDefNode);
        if (nodeIsNew) {
            createNewNode(incomingDefNode);
        }
        else {
            // if the incoming node is not new, we should expect its path to exist -- find it
            final JcrPath incomingDefPath = incomingDefNode.getJcrPath();
            final ConfigurationNodeImpl incomingConfigNode = model.resolveNode(incomingDefPath);

            if (incomingConfigNode == null) {
                throw new IllegalStateException("Cannot modify a node that doesn't exist in baseline: " + incomingDefPath);
            }

            log.debug("Changed path has existing definition: {}", incomingDefPath);

            // is this a delete?
            if (incomingDefNode.isDelete()) {
                // handle node delete
                final DefinitionNodeImpl deleteDef = deleteNode(incomingDefNode, incomingConfigNode);

                // incremental update of model
                new ConfigurationTreeBuilder(model.getConfigurationRootNode())
                        .markNodeAsDeletedBy(incomingConfigNode, deleteDef).pruneDeletedItems(incomingConfigNode);

                // don't bother checking any other properties or children -- they're gone
                return;
            }

            if (incomingDefNode.getOrderBefore() != null) {
                reorderRegistry.add(incomingDefNode.getJcrPath().getParent());
            }


            final List<DefinitionPropertyImpl> properties =
                    new ArrayList<>(incomingDefNode.getModifiableProperties().values());

            // handle properties, then child nodes
            for (final DefinitionPropertyImpl defProperty : properties) {
                // handle properties on an existing node
                mergeProperty(defProperty, incomingConfigNode);
            }

            // any child node here may or may not be new -- do full recursion
            for (final DefinitionNodeImpl childNodeDef : incomingDefNode.getNodes()) {
                mergeConfigDefinitionNode(childNodeDef);
            }
        }
    }

    /**
     * Check the incomingDefNode to see if it represents the re-creation (restore) of a node that was defined in an
     * upstream (not-exported) module, but then explicitly deleted, and restore it if necessary. This check should be
     * performed as a first step (and possibly only step) in processing each "diff" definition.
     * @param incomingDefNode a "diff" definition that may or may not represent a restore of a previously-deleted node
     * @return true if this method has performed all necessary processing of incomingDefNode, false if further work is
     *              still required
     */
    private boolean restoreDeletedNodesIfNecessary(final DefinitionNodeImpl incomingDefNode) {
        // TODO: should a restore also trigger a reorder?
        // is it a root deleted node?
        ConfigurationNodeImpl topDeletedConfigNode = model.resolveDeletedNode(incomingDefNode.getJcrPath());

        boolean isChildNodeDeleted = false;
        boolean nodeRestore = topDeletedConfigNode != null;
        if (!nodeRestore) {
            // maybe it is a (sub)child node of deleted node
            topDeletedConfigNode = model.resolveDeletedSubNodeRoot(incomingDefNode.getJcrPath());
            nodeRestore = isChildNodeDeleted = topDeletedConfigNode != null;
        }

        if (nodeRestore) {
            if (model.resolveNode(incomingDefNode.getJcrPath()) == null) {
                // this is root deleted node, restore config model subtree
                log.debug("Previously-deleted node detected; restoring: {}", topDeletedConfigNode.getPath());
                restoreDeletedTree(topDeletedConfigNode);
            }

            reorderRegistry.add(incomingDefNode.getJcrPath().getParent());

            // delete parent and child delete definitions if exists
            removeDeleteDefinition(incomingDefNode.getJcrPath(), isChildNodeDeleted);

            if (incomingDefNode.getNodes().isEmpty() && incomingDefNode.getProperties().isEmpty() && !incomingDefNode.isDelete()) {
                // Nothing to do here, so return
                return true;
            }
        }
        return false;
    }

    /**
     * Given a previously-retrieved "shadow node", representing the merged state of a previously-deleted config node,
     * restore it to its previous position in the configNode tree, so that it can be used as a baseline for further
     * diff processing.
     * @param topDeletedConfigNode a previously-retrieved (via model.resolveDeletedNode()) configuration node
     */
    private void restoreDeletedTree(final ConfigurationNodeImpl topDeletedConfigNode) {
        final JcrPath parentNodePath = topDeletedConfigNode.getJcrPath().getParent();
        final ConfigurationNodeImpl parentNode = model.resolveNode(parentNodePath);
        parentNode.addNode(topDeletedConfigNode.getName(), topDeletedConfigNode);
    }

    /**
     * Removes node delete definition from the source as it will be superseded with a recreated node.
     * @param path root of the deleted node
     * @param isChildNode this is child of the deleted node
     */
    private void removeDeleteDefinition(final JcrPath path, final boolean isChildNode) {
        DefinitionNodeImpl definitionNode = null;

        // TODO: why scan definitions when we already have a back-reference from configNode.getDefinitions()?
        for (ModuleImpl exportModule : toExport.values()) {
            definitionNode = findDefinitionNode(path, exportModule);
            if (definitionNode != null) {
                break;
            }
        }

        //check if definition is in autoexport modules
        if (definitionNode == null && !isChildNode) {
            throw new RuntimeException(String.format("Could not find node '%s' in autoexport modules, " +
                            "is it part of upstream modules?", path));
        } else if (definitionNode == null) {
            //This is a deleted child node, it may not exist
            return;
        }

        // we change the source, so mark it as changed
        // TODO: why is this outside the "if" block?
        definitionNode.getDefinition().getSource().markChanged();
        if (definitionNode.isDelete()) {
            log.debug("Removing obsolete delete def for restored node: {}", definitionNode.getPath());
            removeOneDefinitionItem(definitionNode, new ArrayList<>());

            // update the configNode to erase the back-reference to the removed definition
            model.resolveNode(path).removeDefinition(definitionNode);
        }
    }

    /**
     * Find DefinitonNode in module
     * @param path path of definition node
     * @param module module to search definition in
     */
    private DefinitionNodeImpl findDefinitionNode(final JcrPath path, final ModuleImpl module) {
        for (ConfigDefinitionImpl configDefinition : module.getConfigDefinitions()) {
            final DefinitionNodeImpl definitionNode = configDefinition.getNode();
            if (path.equals(definitionNode.getJcrPath())) {
                return definitionNode;
            } else if (path.startsWith(definitionNode.getJcrPath())) {
                final JcrPath pathDiff = definitionNode.getJcrPath().relativize(path);
                DefinitionNodeImpl currentNode = configDefinition.getNode();
                for (final JcrPathSegment jcrPathSegment : pathDiff) {
                    currentNode = currentNode.getModifiableNodes().getOrDefault(jcrPathSegment.toString(),
                            currentNode.getModifiableNodes().get(jcrPathSegment.forceIndex().toString()));
                    if (currentNode == null) {
                        break; //wrong path
                    } else if (currentNode.getJcrPath().equals(path)) {
                        return currentNode;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Does this DefinitionNode represent a newly-created node, or does it reference an existing node defined upstream?
     * @param defNode a DefinitionNode to test for newness
     * @return true iff this is a reference to a brand-new node, not referenced upstream yet
     */
    protected boolean isNewNodeDefinition(final DefinitionNodeImpl defNode) {
        // the node is a new node iff a jcr:primaryType is defined here and it's not an override or delete
        return !defNode.isDelete()
                && defNode.getProperty(JCR_PRIMARYTYPE) != null
                && !defNode.getProperty(JCR_PRIMARYTYPE).getOperation().equals(OVERRIDE);
    }

    /**
     * Create a new DefinitionNode in one of the toExport modules for a brand-new node, not mentioned anywhere else.
     * @param incomingDefNode the DefinitionNode from the diff that we are merging
     *
     */
    protected void createNewNode(final DefinitionNodeImpl incomingDefNode) {
        // if the incoming node path is new, we should expect its parent to exist -- find it
        final JcrPath incomingPath = incomingDefNode.getJcrPath();
        final JcrPath parentPath = incomingPath.getParent();
        final ConfigurationNodeImpl existingParent = model.resolveNode(parentPath);

        if (existingParent == null) {
            throw new IllegalStateException("Cannot add a node whose parent doesn't exist in baseline: " + incomingPath);
        }

        log.debug("Changed path is newly defined: {}", incomingPath);

        // does LocationMapper think that this path should be a new context path?
        // if so, create a new ConfigDefinition rather than attempting to add to an existing one
        if (shouldPathCreateNewSource(incomingPath)) {
            // we don't care if there's an existing def -- LocationMapper is making us split to a new file
            createNewDefAndUpdateModel(incomingDefNode);
        }
        else {
            // where was the parent node mentioned?
            // is one of the existing defs for the parent in the toExport modules? grab the last one
            final Optional<DefinitionNodeImpl> maybeDef = getLastLocalDef(existingParent);

            if (maybeDef.isPresent()) {
                // since we have a parent defNode in a valid module, use that for this new child
                final DefinitionNodeImpl parentDefNode = maybeDef.get();

                // we know that this is the only place that mentions this node, because it's new
                // -- put all descendent properties and nodes in this def
                final List<DefinitionNodeImpl> newDefs = new ArrayList<>();
                recursiveAdd(incomingDefNode, parentDefNode, newDefs);

                // update model -- the existing parent def was already merged to the configNode tree, so it needs special handling
                final DefinitionNodeImpl newChildOfExistingDef = parentDefNode.getNode(incomingDefNode.getName());
                final ConfigurationTreeBuilder builder = new ConfigurationTreeBuilder(model.getConfigurationRootNode());
                final ConfigurationNodeImpl newConfigNode =
                        builder.createChildNode(existingParent, newChildOfExistingDef.getName(), newChildOfExistingDef);
                builder.mergeNode(newConfigNode, newChildOfExistingDef);

                // now update all the other (possible) defs that were split off into new source files
                for (final DefinitionNodeImpl newDef : newDefs) {
                    builder.push((ConfigDefinitionImpl) newDef.getDefinition());
                }
            }
            else {
                // there's no existing parent defNode that we can reuse, so we need a new definition
                createNewDefAndUpdateModel(incomingDefNode);
            }
        }
    }

    /**
     * Helper for {@link #createNewNode(DefinitionNodeImpl)}.
     * @param incomingDefNode diff node to be copied into a new def (or defs)
     */
    protected void createNewDefAndUpdateModel(final DefinitionNodeImpl incomingDefNode) {
        final List<DefinitionNodeImpl> newDefs =
                createNewDef(incomingDefNode, true, null, new ArrayList<>());

        // update model
        final ConfigurationTreeBuilder builder = new ConfigurationTreeBuilder(model.getConfigurationRootNode());
        for (final DefinitionNodeImpl newDef : newDefs) {
            builder.push((ConfigDefinitionImpl) newDef.getDefinition());
        }
    }

    /**
     * Find the module within toExport that should match the given path according to the AutoExport module config.
     * @param path the path to test
     * @return a single Module that represents the best match for this path
     */
    protected ModuleImpl getModuleByAutoExportConfig(final JcrPath path) {
        // TODO extra logic from EventProcessor.getModuleForPath() and getModuleForNSPrefix()
        return moduleMappings.values().stream()
                .filter(mapping -> mapping.matchesPath(path))
                .map(mapping -> toExport.get(mapping.mvnPath))
                .findFirst().orElseGet(()->toExport.get(defaultModuleMapping.mvnPath));
    }

    /**
     * Get a source within a given module to use when adding a new node definition. The source is chosen based on
     * the conventions from the AutoExport LocationMapper class, and it might in theory already exist.
     * @param path the JCR path of the new node we'll be defining
     * @param module the module where we want this definition to live
     * @return a new or existing ConfigSourceImpl
     */
    protected ConfigSourceImpl getSourceForNewConfig(final JcrPath path, final ModuleImpl module) {
        // what does LocationMapper say?
        final String sourcePath = getFilePathByLocationMapper(path);
        return createConfigSourceIfNecessary(sourcePath, module);
    }

    /**
     * Get a source within a given module to use when adding a new node definition, creating it if necessary.
     * @param sourcePath the desired module-config-root-relative path
     * @param module the module where we want this definition to live
     * @return a Source that can be used to create new definitions
     */
    protected ConfigSourceImpl createConfigSourceIfNecessary(final String sourcePath, final ModuleImpl module) {
        // does this Source already exist?
        return module.getConfigSource(sourcePath)
                // if not, add it
                .orElseGet(() -> module.addConfigSource(sourcePath));
    }

    /**
     * Does LocationMapper think that this path should be defined in a new context root path in a new file?
     * @param incomingPath the JCR node path to test
     * @return true iff this path should go in a new file, different than its parent node
     */
    protected boolean shouldPathCreateNewSource(JcrPath incomingPath) {

        // If we see a node that we want to treat as an HST root, swap it with the default HST root path
        // for purposes of the test in the final line of this method.
        incomingPath = swapHstRootForDefault(incomingPath);

        // for the sake of creating new source files, we always want to use the minimally-indexed path
        // to avoid annoying and unnecessary "[1]" tags on filenames
        String minimallyIndexedPath = incomingPath.suppressIndices().toString();

        // the actual test...
        return JcrPaths.getPath(LocationMapper.contextNodeForPath(minimallyIndexedPath, true))
                .equals(incomingPath);
    }

    /**
     * If a path has a root node that matches the "hst:" prefix, swap it for the default HST root for
     * purposes of pattern-matching.
     * @param incomingPath the path to potentially swap for a new root node
     * @return a new path based on the HST default root node
     */
    private JcrPath swapHstRootForDefault(final JcrPath incomingPath) {
        //TODO SS: Enhance location mapper to use nodetype matchers

        if (!incomingPath.isRoot()) {
            final String rootNode = incomingPath.getSegment(0).toString();
            if (rootNode.startsWith(HST_PREFIX)) {
                if (incomingPath.getSegmentCount() == 1) {
                    return HST_HST_PATH;
                }
                else {
                    return HST_HST_PATH.resolve(incomingPath.subpath(1));
                }
            }
        }
        return incomingPath;
    }

    /**
     * Lookup the file path that the old AutoExport LocationMapper class would recommend for the given JCR path,
     * then adjust it to use a YAML extension instead of an XML extension.
     * @param path the JCR path for which we want to generate a new source file
     * @return a module-base-relative path with no leading slash for a potentially new yaml source file
     */
    protected String getFilePathByLocationMapper(final JcrPath path) {
        // If we see a node that we want to treat as an HST root, swap it with the default HST root path
        // so that the location mapper rules will match with it.
        final String normalizedPath = swapHstRootForDefault(path).suppressIndices().toString();

        String yamlFile = LocationMapper.fileForPath(normalizedPath, true);
        if (yamlFile == null) {
            return "main.yaml";
        }
        return cleanFilePath(yamlFile);
    }

    /**
     * Create a new ConfigDefinition to contain the contents of the given DefinitionNode, which may be copied here.
     * When copying, this will also create new definitions in new source files for descendant nodes as determined via
     * {@link #shouldPathCreateNewSource(JcrPath)}.
     * @param incomingDefNode a DefinitionNode that will be copied to form the content of the new ConfigDefinition
     * @param copyContents should the contents of the incomingDefNode be recursively copied into the new def?
     * @param parentNodeModule the module where the parent node def is located, if it hasn't been merged to configNodes yet
     * @param newDefs accumulator List for root definition nodes of definitions created here (or by recursive descent)
     */
    protected List<DefinitionNodeImpl> createNewDef(final DefinitionNodeImpl incomingDefNode,
                                                    final boolean copyContents,
                                                    final ModuleImpl parentNodeModule,
                                                    final List<DefinitionNodeImpl> newDefs) {

        final JcrPath incomingPath = incomingDefNode.getJcrPath();

        log.debug("Creating new top-level definition for path: {} ...", incomingPath);

        // create the new ConfigDefinition and add it to the source
        // we know that this is the only place that mentions this node, because it's new
        // TODO discuss with Peter; it need not be new
        // -- put all descendent properties and nodes in this def
        //... but when we create the def, make sure to walk up until we don't have an indexed node in the def root
        final DefinitionNodeImpl newRootNode = getOrCreateLocalDef(incomingPath, parentNodeModule);

        final Source source = newRootNode.getDefinition().getSource();
        log.debug("... stored in {}/hcm-config/{}", source.getModule().getName(), source.getPath());

        newDefs.add(newRootNode);
        if (copyContents) {
            recursiveCopy(incomingDefNode, newRootNode, newDefs);
        }
        return newDefs;
    }

    /**
     * Get or create a definition in the local modules to contain data for jcrPath.
     * Note: this method performs a three-step check for what module to use similar to
     * {@link #createNewContentSource(JcrPath, SortedMap)}, except we also need to handle the JCR root node here.
     * @param path the path for which we want a definition
     * @return a DefinitionNodeImpl corresponding to the jcrPath, which may or may not be a root and may or not may be
     * empty
     * @param parentNodeModule the module where the parent node def is located, if it hasn't been merged to configNodes yet
     */
    protected DefinitionNodeImpl getOrCreateLocalDef(final JcrPath path, ModuleImpl parentNodeModule) {
        // what module should we put it in, according to normal auto-export config rules?
        final ModuleImpl defaultModule = getModuleByAutoExportConfig(path);

        // for the JCR root node, the auto-export config is the only possible answer
        if (path.isRoot()) {
            parentNodeModule = defaultModule;
        }

        // where is the parent of this path initially defined?
        // if the caller already handed us a value, use it
        // this might be a spin-off of a recursiveCopy that hasn't been merged to the configNode tree yet!
        if (parentNodeModule == null) {
            parentNodeModule = findModuleOfParent(path, defaultModule);
        }

        // is the parent of this path downstream from the proposed destModule?
        // if so, use the module where the parent is defined
        final ModuleImpl destModule = chooseMostDownstream(defaultModule, parentNodeModule);

        // what source should we put it in?
        final ConfigSourceImpl destSource = getSourceForNewConfig(path, destModule);

        // get an appropriate definition on that source
        return destSource.getOrCreateDefinitionFor(path);
    }

    /**
     * Utility to search for the Module where the parent node of path is first defined in the config definitions.
     * @param path the path whose parent's source module we want to find
     * @param defaultModule a default module to use if the parent cannot be found
     * @return the ModuleImpl where the parent node of the given path is first defined, or defaultModule
     * @throws IllegalStateException iff the path's parent is not actually a content node
     */
    protected ModuleImpl findModuleOfParent(final JcrPath path, final ModuleImpl defaultModule) {
        final ModuleImpl parentNodeModule;
        final ConfigurationNodeImpl configNode = model.resolveNode(path.getParent());
        if (configNode == null) {
            throw new IllegalStateException("Path does not have a config parent: " + path.toString());
        }

        // search the parent node's defs in reverse order
        final List<DefinitionNodeImpl> defs = configNode.getDefinitions();
        parentNodeModule = reverseStream(defs).filter(this::isNewNodeDefinition).findFirst()
            .map(defNode -> defNode.getDefinition().getSource().getModule())
                // this should be impossible, but we'll default to the old behavior, just in case
                .orElse(defaultModule);
        return parentNodeModule;
    }

    /**
     * Tiny utility method to stream the elements of a list in reverse order.
     * @param list the list to stream in reverse -- ideally should be {@link RandomAccess} (or small) for performance
     * @param <T> the element type of list
     * @return a Stream of list's contents in reverse index order
     */
    protected static <T> Stream<T> reverseStream(List<T> list) {
        final int limit=list.size()-1;
        return IntStream.rangeClosed(0, limit).mapToObj(i -> list.get(limit - i));
    }

    /**
     * Pick the most-downstream module according to module dependencies (and default module sorting).
     * @param modA one module within the model, which must also be in toExport
     * @param modB another module within the model
     * @return the most-downstream module according to module dependencies (and default module sorting)
     */
    protected ModuleImpl chooseMostDownstream(ModuleImpl modA, final ModuleImpl modB) {
        // short-circuit in case where both reference the same Module, which must be okay
        if (modA != modB) {
            for (ModuleImpl module : model.getModules()) {
                if (module == modB) {
                    // found modB first, which means modA is after and therefore best
                    return modA;
                }
                if (module == modA) {
                    // found modA first, which means modA is too early -- use modB instead
                    // since modA is being exported, and modB is after it, we know that modB is being exported
                    log.debug("Redirecting new def to module where parent node is defined: {} => {}", modA, modB);
                    return modB;
                }
            }
        }
        return modA;
    }

    /**
     * Recursively copy the new def as a child-plus-descendants of this node.
     * This will also create new definitions in new source files for descendant nodes as determined via
     * {@link #shouldPathCreateNewSource(JcrPath)}.
     * @param from the definition we want to copy as a child of toParent
     * @param toParent the parent of the desired new definition node
     * @param newDefs accumulator List for root definition nodes of definitions created here (or by recursive descent),
     *               already populated with properties and descendants
     */
    protected void recursiveAdd(final DefinitionNodeImpl from,
                                final DefinitionNodeImpl toParent,
                                final List<DefinitionNodeImpl> newDefs) {

        log.debug("Adding new node definition to existing definition: {}", from.getJcrPath());

        // mark source changed
        toParent.getDefinition().getSource().markChanged();

        // if order-before is set, we need to do an insert, not an add-at-end
        final DefinitionNodeImpl to;
        if (from.getOrderBefore() != null) {
            log.debug("Inserting before: {}", from.getOrderBefore());

            to = toParent.addNodeBefore(from.getName(), from.getOrderBefore());

            // if the new node does not have order-before set, we don't need to copy and keep order-before anymore
            if (to.getOrderBefore() == null) {
                from.setOrderBefore(null);
            }
            else {
                log.debug("Couldn't find node that we want to insert before");
            }
        }
        else {
            to = toParent.addNode(from.getName());
        }

        recursiveCopy(from, to, newDefs);
    }

    /**
     * Recursively copy all properties and descendants of the from-node to the to-node.
     * Creates new definitions as required by LocationMapper.
     * @param from the definition we want to copy
     * @param to the definition we are copying into
     * @param newDefs accumulator List for root definition nodes of definitions created here (or by recursive descent)
     */
    protected void recursiveCopy(final DefinitionNodeImpl from, final DefinitionNodeImpl to,
                                 final List<DefinitionNodeImpl> newDefs) {

        // Add the 'to' path to the reorder registry, whether it is a delete, or if new content gets copied in here
        reorderRegistry.add(to.getJcrPath().getParent());

        if (from.isDelete()) {
            // delete clears everything, so there's no point continuing with other properties or recursion
            to.delete();
            return;
        }

        to.setOrderBefore(from.getOrderBefore());
        to.setIgnoreReorderedChildren(from.getIgnoreReorderedChildren());
        to.setResidualChildNodeCategory(from.getResidualChildNodeCategory());
        to.setCategory(from.getCategory());

        // copy properties using special method that migrates resources properly
        for (final DefinitionPropertyImpl fromProperty : from.getProperties()) {
            to.addProperty(fromProperty);
        }

        for (final DefinitionNodeImpl childNode : from.getNodes()) {
            // for each new childNode, we need to check if LocationMapper wants a new source file
            final JcrPath incomingPath = childNode.getJcrPath();
            if (shouldPathCreateNewSource(incomingPath)) {
                // yes, we need a new definition in a new source file
                // we need to know what module the parent node's def is in to place the new one properly!
                createNewDef(childNode, true, to.getDefinition().getSource().getModule(), newDefs);
            } else {
                // no, just keep adding to the current destination defNode
                recursiveAdd(childNode, to, newDefs);
            }
        }
    }

    /**
     * Handle a diff entry indicating that a single node should be deleted.
     * @param defNode a DefinitionNode from the diff, describing a single to-be-deleted node
     * @param configNode the ConfigurationNode corresponding to the to-be-deleted node in the current config model
     */
    protected DefinitionNodeImpl deleteNode(final DefinitionNodeImpl defNode,
                                            final ConfigurationNodeImpl configNode) {

        log.debug("Deleting node: {}", defNode.getJcrPath());

        reorderRegistry.add(defNode.getJcrPath().getParent());

        final List<DefinitionNodeImpl> defsForConfigNode = configNode.getDefinitions();

        // if last existing node def is upstream,
        final boolean lastDefIsUpstream = !isLastDefLocal(defsForConfigNode);
        if (lastDefIsUpstream) {
            log.debug("Last def for node is upstream of export: {}", defNode.getJcrPath());

            // create new defnode w/ delete
            final DefinitionNodeImpl newDef = createNewDef(defNode, true, null,
                    // there should be only a single def in this case, since deletes do not have recursive children
                    new ArrayList<>(1)).get(0);

            // we know that there was no local def for the node we're deleting, but there may be defs for its children
            // so for all descendants, remove all definitions and possibly sources
            removeDescendantDefinitions(configNode, new ArrayList<>());

            return newDef;
        }
        else {
            // there are local node defs for this node
            // are there ONLY local node defs?
            final List<DefinitionNodeImpl> localDefs = getLocalDefs(defsForConfigNode);
            final boolean onlyLocalDefs = (localDefs.size() == defsForConfigNode.size());
            if (onlyLocalDefs) {
                log.debug("Only local defs for node: {}", defNode.getJcrPath());

                // since there's only local defs, we want this node to disappear from the record completely
                // i.e. "some" = "all" defs, in this case
                removeSomeDefsAndDescendants(configNode, defsForConfigNode, new ArrayList<>());

                return defNode;
            }
            else {
                log.debug("Both local and upstream defs for node: {}", defNode.getJcrPath());

                // since there's also an upstream def, we want to collapse all local references to a single delete def
                // if exists, change one local def to delete and remove other properties and subnodes
                final DefinitionNodeImpl defToKeep = localDefs.get(0);

                // mark chosen node as a delete
                final ConfigDefinitionImpl defToKeepDefinition = (ConfigDefinitionImpl) defToKeep.getDefinition();
                log.debug("Marking delete on node: {} from definition of: {} in source: {}",
                        defToKeep.getJcrPath(),
                        defToKeepDefinition.getNode().getJcrPath(),
                        defToKeepDefinition.getSource().getPath());
                defToKeep.delete();
                defToKeepDefinition.getSource().markChanged();

                // remove all other defs and children (but not the first one, that we are keeping)
                final List<DefinitionNodeImpl> localDefsExceptFirst = localDefs.subList(1, localDefs.size());
                removeSomeDefsAndDescendants(configNode, localDefsExceptFirst, new ArrayList<>());

                return defToKeep;
            }
        }
    }

    /**
     * Remove some (perhaps all) definitions associated with configNode, plus all definitions associated with
     * descendants of configNode. Recurs down the ConfigurationNode tree and up the DefinitionItem tree(s).
     * Call this method to start the entire removal process, possibly preserving one or more existing definitions.
     * @param configNode the configNode whose descendants we're deleting
     * @param defsToRemove the subset of configNode.getDefinitions() that we want to delete
     * @param alreadyRemoved an accumulator for Definitions whose children we don't have to check,
 *                           because the root is already gone
     */
    protected void removeSomeDefsAndDescendants(final ConfigurationNodeImpl configNode,
                                                final List<? extends DefinitionItemImpl> defsToRemove,
                                                final List<AbstractDefinitionImpl> alreadyRemoved) {
        log.debug("Removing defs and children for node: {} with exceptions: {}", configNode.getJcrPath(), alreadyRemoved);

        for (final DefinitionItemImpl definitionItem : defsToRemove) {
            removeOneDefinitionItem(definitionItem, alreadyRemoved);
        }

        // we don't need to handle properties specifically, because we will remove all the nodes that contain them

        // scan downwards for child definitions, which could be rooted on the children directly
        removeDescendantDefinitions(configNode, alreadyRemoved);
    }

    /**
     * Remove child definitions from the given configNode, because configNode is being deleted.
     * This method recurs down the ConfigurationNode tree, and then recurs up the DefinitionNode tree(s) to clean up
     * any parent DefinitionNodes, Definitions, or Sources that may have been made empty.
     * @param configNode the node being deleted
     * @param alreadyRemoved an accumulator for Definitions whose children we don't have to check,
     *                           because the root is already gone
     */
    protected void removeDescendantDefinitions(final ConfigurationNodeImpl configNode,
                                               final List<AbstractDefinitionImpl> alreadyRemoved) {
        log.debug("Removing child defs for node: {} with exceptions: {}", configNode.getJcrPath(), alreadyRemoved);

        for (final ConfigurationNodeImpl childConfigNode : configNode.getNodes()) {
            for (final DefinitionNodeImpl childDefItem : childConfigNode.getDefinitions()) {
                // if child's DefinitionNode was part of a parent Definition, it may have already been removed
                // also check the definition belongs to one of autoexport modules
                final AbstractDefinitionImpl childDefinition = childDefItem.getDefinition();
                if (!alreadyRemoved.contains(childDefinition)
                        && isAutoExportModule(childDefinition.getSource().getModule())) {
                    // otherwise, remove it now
                    removeOneDefinitionItem(childDefItem, alreadyRemoved);
                }
            }
            removeDescendantDefinitions(childConfigNode, alreadyRemoved);
        }

        purgeDeletedNodesInSource(configNode);
    }

    /**
     * Delete all deleted child nodes from a source
     * @param configNode
     *
     */
    private void purgeDeletedNodesInSource(final ConfigurationNodeImpl configNode) {
        final List<AbstractDefinitionImpl> removed = new ArrayList<>();
        for (ModuleImpl module : toExport.values()) {
            final List<ConfigDefinitionImpl> configDefinitions = module.getConfigDefinitions();
            final List<ConfigDefinitionImpl> itemsToClean = configDefinitions.stream().filter(d ->
                    d.getNode().isDelete() && !Objects.equals(d.getNode().getJcrPath(), configNode.getJcrPath())
                            && d.getNode().getJcrPath().startsWith(configNode.getJcrPath())).collect(Collectors.toList());
            itemsToClean.forEach(item -> removeOneDefinitionItem(item.getNode(), removed));
        }
    }

    private boolean isAutoExportModule(final ModuleImpl candidate) {
        return toExport.values().contains(candidate);
    }

    /**
     * Remove one definition item, either by removing it from its parent or (if root) removing the entire definition.
     * Recurs up the DefinitionItem tree to clean up newly-emptied items.
     * @param definitionItem the node or property to remove
     * @param alreadyRemoved an accumulator for Definitions whose children we don't have to check,
     *                           because the root is already gone
     */
    protected void removeOneDefinitionItem(final DefinitionItemImpl definitionItem,
                                           final List<AbstractDefinitionImpl> alreadyRemoved) {

        log.debug("Removing one def item for node: {} with exceptions: {}", definitionItem.getJcrPath(), alreadyRemoved);

        // remove the node itself
        // if this node is the root
        if (definitionItem.isRoot()) {
            // remove the definition
            final ConfigDefinitionImpl definition = (ConfigDefinitionImpl) definitionItem.getDefinition();
            removeDefinition(definition);
            alreadyRemoved.add(definition);
        }
        else {
            // otherwise, remove from parent
            removeFromParentDefinitionItem(definitionItem, alreadyRemoved);
        }

    }

    /**
     * Remove a DefinitionItem from its parent. This method assumes that you've already checked that the parent exists.
     * Recurs up the DefinitionItem tree to clean up newly-emptied items.
     * @param definitionItem the node or property to remove
     * @param alreadyRemoved an accumulator for Definitions whose children we don't have to check,
     *                           because the root is already gone
     */
    protected void removeFromParentDefinitionItem(final DefinitionItemImpl definitionItem,
                                                  final List<AbstractDefinitionImpl> alreadyRemoved) {
        final ConfigDefinitionImpl definition = (ConfigDefinitionImpl) definitionItem.getDefinition();
        final SourceImpl source = definition.getSource();
        final ModuleImpl module = source.getModule();

        // check if the definition is in one of the toExport modules -- if not, we can't change it
        if (!toExport.containsValue(module)) {
            throw new IllegalStateException
                    ("Cannot change a definition from module that is not being merged: " + module.getFullName()
                            + " for node: " + definitionItem.getJcrPath());
        }
        log.debug("Removing definition item for: {} from definition of: {} in source: {}",
                definitionItem.getJcrPath(),
                definition.getNode().getJcrPath(), source.getPath());

        final DefinitionNodeImpl parentNode = definitionItem.getParent();
        if (definitionItem instanceof DefinitionNode) {
            // remove the node from its parent
            // todo: one of very few remaining uses of getModifiableNodes()
            parentNode.getModifiableNodes().remove(definitionItem.getName());

            // remove referenced resources
            removeResources((DefinitionNodeImpl) definitionItem);
        }
        else {
            // remove the property from its parent
            // todo: one of very few remaining uses of getModifiableProperties()
            parentNode.getModifiableProperties().remove(definitionItem.getName());

            // remove referenced resources
            removeResources((DefinitionPropertyImpl) definitionItem);
        }
        source.markChanged();

        // if this was the last item in the parent node ...
        if (parentNode.isEmpty()) {
            // ... remove the parent node and keep moving up
            removeOneDefinitionItem(parentNode, alreadyRemoved);
        }
    }

    /**
     * Remove an entire Definition, and if it is the last Definition in its Source, also remove the Source.
     * @param definition the definition to remove
     */
    protected void removeDefinition(final ConfigDefinitionImpl definition) {
        // remove the definition from its source and from its module
        final SourceImpl source = definition.getSource();
        final ModuleImpl module = source.getModule();

        // check if the definition is in one of the toExport modules -- if not, we can't change it
        if (!toExport.containsValue(module)) {
            throw new IllegalStateException
                    ("Cannot remove a definition from module that is not being merged: " + module.getFullName()
                            + " for node: " + definition.getNode().getJcrPath());
        }
        log.debug("Removing definition for node: {} from source: {}", definition.getNode().getJcrPath(), source.getPath());

        source.removeDefinition(definition);
        module.getConfigDefinitions().remove(definition);

        // remove referenced resources
        removeResources(definition.getNode());

        // if the definition was the last one from its source
        if (source.getDefinitions().size() == 0) {
            log.debug("Removing source: {}", source.getPath());

            // remove the source from its module
            module.getModifiableSources().remove(source);
            module.addConfigResourceToRemove("/" + source.getPath());
        }
    }

    /**
     * Merge an incoming property change into toExport modules.
     * @param defProperty the incoming property change
     * @param configNode the ConfigurationNode representing the parent node of defProperty
     */
    protected void mergeProperty(final DefinitionPropertyImpl defProperty,
                                 final ConfigurationNodeImpl configNode) {

        log.debug("Merging property: {} with operation: {}", defProperty.getJcrPath(), defProperty.getOperation());

        final ConfigurationPropertyImpl configProperty = configNode.getProperty(defProperty.getName());

        if (configProperty == null) {
            final ConfigurationPropertyImpl deletedProperty = model.resolveDeletedProperty(defProperty.getJcrPath());
            try {
                if (deletedProperty != null && ValueProcessor.propertyIsIdentical(defProperty, deletedProperty)) {
                    //we're in property restore mode and diff is null, just remove the delete operation
                    final Optional<DefinitionPropertyImpl> maybeLocalPropertyDef = getLastLocalDef(deletedProperty);
                    if (maybeLocalPropertyDef.isPresent()) {
                        removeFromParentDefinitionItem(maybeLocalPropertyDef.get(), new ArrayList<>());
                    } else {
                        log.error("Delete definition for property {} is not found", defProperty.getJcrPath());
                    }
                    return;
                }
            } catch (IOException ignored) {
                //Should not happen
            }
        }
        //If defProperty is undefined, just delete definition
        switch (defProperty.getOperation()) {
            case REPLACE:
            case ADD:
            case OVERRIDE:
                mergePropertyThatShouldExist(defProperty, configNode, configProperty);
                break;
            default:
                // case DELETE:
                deleteProperty(defProperty, configNode, configProperty);
                break;
        }
    }

    protected void mergePropertyThatShouldExist(final DefinitionPropertyImpl defProperty,
                                                final ConfigurationNodeImpl configNode,
                                                final ConfigurationPropertyImpl configProperty) {
        final boolean propertyExists = (configProperty != null);
        if (propertyExists) {
            // this is an existing property being replaced
            log.debug(".. which already exists", defProperty.getJcrPath());

            // is there a local def for this specific property?
            final Optional<DefinitionPropertyImpl> maybeLocalPropertyDef = getLastLocalDef(configProperty);
            if (maybeLocalPropertyDef.isPresent()) {
                // yes, there's a local def for the specific property
                final DefinitionPropertyImpl localPropDef = maybeLocalPropertyDef.get();

                log.debug(".. and already has a local property def in: {} from source: {}",
                        localPropDef.getDefinition().getNode().getJcrPath(),
                        localPropDef.getDefinition().getSource().getPath());

                final List<DefinitionPropertyImpl> defsForConfigProperty = configProperty.getDefinitions();

                // cases:
                // 1. local is replace and only def, diff is override => replace
                if (localPropDef.getOperation() == REPLACE
                        && defProperty.getOperation() == PropertyOperation.OVERRIDE
                        && defsForConfigProperty.size() == 1) {
                    defProperty.setOperation(REPLACE);
                }
                // 2. local is replace and not only def, diff is override => override (do nothing)

                if (localPropDef.getOperation() == PropertyOperation.OVERRIDE) {
                    // 3. local is override, diff is replace => override
                    if (defProperty.getOperation() == REPLACE) {
                        defProperty.setOperation(PropertyOperation.OVERRIDE);
                    }

                    if (defProperty.getOperation() == PropertyOperation.OVERRIDE) {
                        final DefinitionPropertyImpl nextUpDefProperty =
                                defsForConfigProperty.get(defsForConfigProperty.size() - 2);
                        final boolean diffMatchesNextUp =
                                defProperty.getKind() == nextUpDefProperty.getKind()
                                        && defProperty.getValueType() == nextUpDefProperty.getValueType();
                        if (diffMatchesNextUp) {
                            // 4. local is override, diff is override, upstream is same as diff => replace
                            defProperty.setOperation(REPLACE);
                        }
                        // 5. local is override, diff is override, upstream is still different => override (do nothing)
                    }
                }

                // 6. local is add, diff is replace => replace (do nothing)
                // 7. local is add, diff is override => override (do nothing)
                // 8. local is add, diff is add => add (do nothing)
                // 9. local is replace, diff is replace => replace (do nothing)
                // 10. local is replace, diff is add => replace (updateFrom handles)
                // 11. local is override, diff is add => override (updateFrom handles)

                // change local def to reflect new state
                localPropDef.updateFrom(defProperty);
                localPropDef.getDefinition().getSource().markChanged();

                // now that we know what the new value will be, check whether that is identical with the upstream value
                // in that case, we can remove our newly-redundant local property definition(s)
                final ConfigurationPropertyImpl upstreamProperty = buildUpstreamProperty(configProperty);
                final boolean localPropertyShouldBeRemoved = upstreamProperty != null
                        && propertiesAreIdentical(localPropDef, upstreamProperty);

                if (localPropertyShouldBeRemoved) {
                    removeUpstreamPropertyFromSource(configNode, defProperty, localPropDef.getName());

                    // in this case, it is logically impossible for another change to affect this property, and we
                    // rebuild the whole tree anyway, so we don't bother updating the "live" ConfigurationModel tree
                } else {
                    // update the model incrementally, since a new local def should be available for other props
                    final ConfigurationTreeBuilder builder = new ConfigurationTreeBuilder(model.getConfigurationRootNode());

                    // build the property back up from scratch using all of the definitions
                    configNode.removeProperty(defProperty.getName());
                    for (final DefinitionPropertyImpl def : defsForConfigProperty) {
                        builder.mergeProperty(configNode, def);
                    }
                }
            }
            else {
                // no, there's no local def for the specific property
                log.debug("... but has no local def yet");

                addLocalProperty(defProperty, configNode);
            }
        }
        else {
            // this is a totally new property
            // note: this is effectively unreachable for case: OVERRIDE
            log.debug(".. which is totally new", defProperty.getJcrPath());

            addLocalProperty(defProperty, configNode);
        }
    }

    /**
     * Remove upstream property from the autoexport modules sources
     * @param configNode Parent node
     * @param incomingPropertyDef
     * @param upstreamPropertyName Name of the property
     */

    private void removeUpstreamPropertyFromSource(final ConfigurationNodeImpl configNode,
                                                  final DefinitionPropertyImpl incomingPropertyDef,
                                                  final String upstreamPropertyName) {

        final List<DefinitionNodeImpl> localNodeDefs = Lists.reverse(getLocalDefs(configNode.getDefinitions()));

        boolean isLastDef = true;

        final Map<String, DefinitionPropertyImpl> incomingPropertyDefs = incomingPropertyDef
                .getParent().getModifiableProperties();
        incomingPropertyDefs.remove(upstreamPropertyName);
        for (final DefinitionNodeImpl localNodeDef : localNodeDefs) {

            if (localNodeDef.getProperty(upstreamPropertyName) != null) {
                //Remove the property definition from node definition
                localNodeDef.getModifiableProperties().remove(upstreamPropertyName);

                final Map<String, DefinitionPropertyImpl> localNodeProperties = localNodeDef
                        .getModifiableProperties().entrySet().stream()
                        .filter(e -> !e.getKey().equals(JcrConstants.JCR_PRIMARY_TYPE))
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

                if (localNodeProperties.isEmpty() && (!isLastDef || incomingPropertyDefs.isEmpty())) {
                    //If there is no more property definitions within current local node definition
                    //and if it is not last definition or there are no new incoming properties
                    //then remove definition
                    removeDefinition(localNodeDef);
                }
            }

            isLastDef = false;
        }
    }

    private void removeDefinition(final DefinitionNodeImpl moduleNodeDef) {
        final DefinitionNodeImpl rootNode = moduleNodeDef.getDefinition().getNode();
        rootNode.getModifiableNodes().remove(moduleNodeDef.getName());

        if (rootNode.getNodes().isEmpty()) {
            removeDefinition((ConfigDefinitionImpl) moduleNodeDef.getDefinition());
        }
    }

    /**
     * Build property by using only definitions from upstream modules
     * @param configProperty source property
     * @return property constructed from only upstream modules. Null if there is no upstream definitions for configProperty
     */
    private ConfigurationPropertyImpl buildUpstreamProperty(final ConfigurationPropertyImpl configProperty) {
        final ConfigurationNodeImpl configurationNode = new ConfigurationNodeImpl();
        final ConfigurationTreeBuilder propertyMerger = new ConfigurationTreeBuilder(configurationNode);
        configProperty.getDefinitions().stream()
                .filter(isUpstreamDef())
                .forEach(def -> propertyMerger.mergeProperty(configurationNode, def));
        final ConfigurationNodeImpl buildNode = propertyMerger.build();
        return buildNode.getProperty(configProperty.getName());
    }

    /**
     * Validate if properties and their's types equal
     * @param property1
     * @param property2
     */
    private boolean propertiesAreIdentical(ModelProperty property1, ModelProperty property2) {
        try {
            return ValueProcessor.propertyIsIdentical(property1, property2);
        } catch (IOException e) {
            throw new RuntimeException("Could not compare properties", e);
        }
    }

    protected void deleteProperty(final DefinitionPropertyImpl defProperty,
                                  final ConfigurationNodeImpl configNode,
                                  final ConfigurationPropertyImpl configProperty) {
        final boolean propertyExists = (configProperty != null);
        if (!propertyExists) {
            throw new IllegalArgumentException("Cannot delete a property that doesn't exist in config model!");
        }

        final List<DefinitionPropertyImpl> defsForConfigProperty = configProperty.getDefinitions();
        final boolean lastDefIsUpstream = !isLastDefLocal(defsForConfigProperty);

        // add local property
        if (lastDefIsUpstream) {
            addLocalProperty(defProperty, configNode);
        }
        else {
            final List<DefinitionPropertyImpl> localDefs = getLocalDefs(defsForConfigProperty);
            final boolean onlyLocalDefs = (localDefs.size() == defsForConfigProperty.size());

            // remove all but the first local def
            final DefinitionPropertyImpl firstLocalDef = localDefs.get(0);
            firstLocalDef.getDefinition().getSource().markChanged();

            for (final DefinitionPropertyImpl localDef : localDefs.subList(1, localDefs.size())) {
                removeFromParentDefinitionItem(localDef, new ArrayList<>());
            }

            // clear the property in the model
            configNode.removeProperty(defProperty.getName());

            if (onlyLocalDefs) {
                // if the first local def is the only def left, remove that, too
                removeFromParentDefinitionItem(firstLocalDef, new ArrayList<>());
            }
            else {
                // otherwise, replace first local def with delete
                firstLocalDef.updateFrom(defProperty);
            }
        }
    }

    /**
     * Add a local definition of a given property, either by adding to an existing definition for the
     * containing node, or by creating a new definition for the containing node and adding to that.
     * @param defProperty the property to add
     * @param configNode the ConfigurationNode for the containing node
     */
    protected void addLocalProperty(final DefinitionPropertyImpl defProperty,
                                    final ConfigurationNodeImpl configNode) {
        // is there a local def for the parent node, where I can put this property?
        final Optional<DefinitionNodeImpl> maybeLocalNodeDef = getLastLocalDef(configNode);
        if (maybeLocalNodeDef.isPresent()) {
            // yes, there's a local def for parent node -- add the property
            final DefinitionNodeImpl definitionNode = maybeLocalNodeDef.get();

            log.debug("Adding new local property: {} in existing def: {} from source: {}",
                    defProperty.getJcrPath(),
                    definitionNode.getDefinition().getNode().getJcrPath(),
                    definitionNode.getDefinition().getSource().getPath());

            final DefinitionPropertyImpl newProperty = definitionNode.addProperty(defProperty);
            definitionNode.getDefinition().getSource().markChanged();

            // update the model incrementally, since a new local def should be available for other props
            new ConfigurationTreeBuilder(model.getConfigurationRootNode())
                    .mergeProperty(configNode, newProperty).pruneDeletedItems(configNode);
        }
        else {
            // no, there's no local def for parent node
            // create a new local definition with this property
            final DefinitionNodeImpl newDefNode =
                    createNewDef(defProperty.getParent(), false, null,
                            // there will be only a single def in this case, since we're explicitly not recursing
                            new ArrayList<>(1)).get(0);

            log.debug("Adding new local def for property: {} in source: {}", defProperty.getJcrPath(),
                    newDefNode.getDefinition().getSource().getPath());

            newDefNode.addProperty(defProperty);

            // update the model incrementally, since a new local def should be available for other props
            new ConfigurationTreeBuilder(model.getConfigurationRootNode())
                    .push((ConfigDefinitionImpl) newDefNode.getDefinition()).pruneDeletedItems(configNode);
        }
    }

    protected <C extends ConfigurationItemImpl<D>, D extends DefinitionItemImpl>
    Optional<D> getLastLocalDef(final C item) {
        return getLastDefinition(item, isLocalDef());
    }

    protected <C extends ConfigurationItemImpl<D>, D extends DefinitionItemImpl>
    Optional<D> getLastUpstreamDef(final C item) {
        return getLastDefinition(item, isUpstreamDef());
    }

    protected <C extends ConfigurationItemImpl<D>, D extends DefinitionItemImpl>
    Optional<D> getLastDefinition(final C item, Predicate<DefinitionItemImpl> condition) {
        final List<D> existingDefs = item.getDefinitions();
        return Lists.reverse(existingDefs).stream()
                .filter(condition)
                .findFirst();
    }

    protected <D extends DefinitionItemImpl> List<D> getLocalDefs(final List<D> defsForNode) {
        return defsForNode.stream()
                .filter(isLocalDef()).collect(toList());
    }

    protected boolean isLastDefLocal(final List<? extends DefinitionItemImpl> definitionItems) {
        return isLocalDef().test(definitionItems.get(definitionItems.size()-1));
    }

    protected Predicate<DefinitionItemImpl> isUpstreamDef() {
        return def -> !toExport.containsKey(getMvnPathFromDefinitionItem(def));
    }

    protected Predicate<DefinitionItemImpl> isLocalDef() {
        return def -> toExport.containsKey(getMvnPathFromDefinitionItem(def));
    }

    protected static String getMvnPathFromDefinitionItem(final DefinitionItemImpl item) {
        return item.getDefinition().getSource().getModule().getMvnPath();
    }

    protected void mergeContentDefinitions(final Set<String> contentAdded,
                                           final Set<String> contentChanged,
                                           final Set<String> contentDeleted) {

        // set of content change paths in lexical order, so that shorter common sub-paths come first
        // use a PATRICIA Trie, which stores strings efficiently when there are common prefixes
        final Set<String> contentChangesByPath = Collections.newSetFromMap(new PatriciaTrie<>());
        contentChangesByPath.addAll(contentAdded);
        contentChangesByPath.addAll(contentChanged);

        // set of existing sources in reverse lexical order, so that longer paths come first
        // note: we can use an ordinary TreeMap here, because we don't expect as many sources as raw paths
        final SortedMap<JcrPath, ContentDefinitionImpl> existingSourcesByNodePath =
                collectContentSourcesByNodePath(true);

        // process deletes, including resource removal
        for (final String deletePath : contentDeleted) {
            // if a delete path is -above- a content root path, we need to delete one or more entire sources
            final Set<JcrPath> toRemoveByNodePath = new HashSet<>();
            for (final JcrPath sourceNodePath : existingSourcesByNodePath.keySet()) {
                if (sourceNodePath.startsWith(deletePath)) {
                    final ContentDefinitionImpl contentDef = existingSourcesByNodePath.get(sourceNodePath);
                    final SourceImpl source = contentDef.getSource();
                    final ModuleImpl module = source.getModule();

                    // mark all referenced resources for delete
                    removeResources(contentDef.getNode());

                    // remove the source from its module
                    module.getModifiableSources().remove(source);
                    module.addContentResourceToRemove("/" + source.getPath());
                    toRemoveByNodePath.add(contentDef.getNode().getJcrPath());

                    // mark the parent as needing reordering
                    reorderRegistry.add(sourceNodePath.getParent());
                }
            }
            // if a delete path is -below- one of the sources that remains, treat it as a change
            for (final JcrPath sourceNodePath : Sets.difference(existingSourcesByNodePath.keySet(), toRemoveByNodePath)) {
                if (deletePath.startsWith(sourceNodePath.suppressIndices().toString())) {
                    contentChangesByPath.add(deletePath);
                }
            }
        }

        // find allContentSourcesByNodePath (in reverse lexical order of node path, so deeper paths are before ancestor paths)
        // but use the full set of Modules, not just the toExport modules
        final SortedMap<JcrPath, ContentDefinitionImpl> allContentSourcesByNodePath =
                // except only do the work if we actually plan to use it
                !contentChangesByPath.isEmpty()?
                        collectContentSourcesByNodePath(false):
                        Collections.emptySortedMap();

        for (final String changePath : contentChangesByPath) {
            // is there an existing source for this exact path? if so, use that
            final JcrPath changeNodePath = JcrPaths.getPath(changePath);
            if (existingSourcesByNodePath.containsKey(changeNodePath)) {
                // mark it changed for later re-export, and then we're done with this path
                existingSourcesByNodePath.get(changeNodePath).getSource().markChanged();
                continue;
            }

            // there was no exactly-matching source, so we need to decide whether to reuse or create new
            // if LocationMapper tells us we should have a new source file...
            if (shouldPathCreateNewSource(changeNodePath)) {
                // create a new source file
                final ContentDefinitionImpl newDef = createNewContentSource(changeNodePath, allContentSourcesByNodePath);

                // keep our internal data structures up to date
                existingSourcesByNodePath.put(changeNodePath, newDef);
                allContentSourcesByNodePath.put(changeNodePath, newDef);

                // REPO-1715 We have a potential for a race condition where child nodes can be accidentally
                //           exported to source files for an ancestor node before we process the add events
                //           for the child nodes. To clean up this state, we also need to re-export any
                //           source on the direct ancestor path for the change path.
                for (ContentDefinitionImpl def : existingSourcesByNodePath.values()) {
                    if (changeNodePath.startsWith(def.getNode().getJcrPath())) {
                        def.getSource().markChanged();
                    }
                }
            }
            else {
                // check if there's an existing file that is an ancestor of the changed path
                // find the source with the longest matching substring of the changed path
                final Optional<ContentDefinitionImpl> maybeDef = existingSourcesByNodePath.entrySet().stream()
                        .filter(e -> changeNodePath.startsWith(e.getKey()))
                        .map(Map.Entry::getValue)
                        .findFirst();
                if (maybeDef.isPresent()) {
                    // there's an existing matching source, so just mark it changed for later re-export
                    maybeDef.get().getSource().markChanged();
                }
                else {
                    // otherwise, create a new source file
                    // REPO-1715 We don't have to walk up the tree in this case, since we know there's
                    //           no source on an ancestor path that might have picked up these changes.
                    final ContentDefinitionImpl newDef = createNewContentSource(changeNodePath, allContentSourcesByNodePath);

                    // keep our internal data structures up to date
                    existingSourcesByNodePath.put(changeNodePath, newDef);
                    allContentSourcesByNodePath.put(changeNodePath, newDef);
                }
            }
        }

        // we've added new defs, so we need to update the modules to reflect that
        for (ModuleImpl module : toExport.values()) {
            module.build();
        }

        getChangedContentSourcesStream().forEach(source ->
                reorderRegistry.add(source.getContentDefinition().getNode().getJcrPath().getParent())
        );
    }

    private Stream<ContentSourceImpl> getChangedContentSourcesStream() {
        return toExport.values().stream()
                .flatMap(m -> m.getContentSources().stream())
                .filter(SourceImpl::hasChangedSinceLoad);
    }

    /**
     * Helper to collect all content sources of modules by root path in reverse lexical order of root paths.
     * @param toExportOnly if true, only include the modules in toExport; otherwise, include the full model
     */
    protected SortedMap<JcrPath, ContentDefinitionImpl> collectContentSourcesByNodePath(final boolean toExportOnly) {
        final Function<ContentDefinitionImpl, JcrPath> cdPath = cd -> cd.getNode().getJcrPath();
        // TODO: this will silently ignore a situation where multiple sources define the same content path!
        // if there are multiple modules with the same content path, use the first one we encounter
        final BinaryOperator<ContentDefinitionImpl> pickOne = (l, r) -> l;
        final Supplier<TreeMap<JcrPath, ContentDefinitionImpl>> reverseTreeMapper =
                () -> new TreeMap<>(Comparator.reverseOrder());

        final Stream<ModuleImpl> modulesStream = toExportOnly? toExport.values().stream(): model.getModulesStream();
        return modulesStream
                .flatMap(m -> Lists.reverse(m.getContentDefinitions()).stream())
                .collect(Collectors.toMap(cdPath, Function.identity(), pickOne, reverseTreeMapper));
    }

    /**
     * Recursively find all resource paths in this node or descendants, then tell the containing module to remove
     * the resources at those paths.
     */
    protected void removeResources(final DefinitionNodeImpl node) {
        // find resource values
        for (final DefinitionPropertyImpl dp : node.getProperties()) {
            removeResources(dp);
        }

        // recursively visit child definition nodes
        for (final DefinitionNodeImpl childNode : node.getNodes()) {
            removeResources(childNode);
        }
    }

    /**
     * Find all resource paths in this property, then tell the containing module to remove
     * the resources at those paths.
     */
    protected void removeResources(final DefinitionPropertyImpl dp) {
        switch (dp.getKind()) {
            case SINGLE:
                removeResourceIfNecessary(dp.getValue());
                break;
            case SET:
            case LIST:
                for (final ValueImpl value : dp.getValues()) {
                    removeResourceIfNecessary(value);
                }
                break;
        }
    }

    /**
     * If the given value represents a resource, tell the containing module to remove the resource at the path
     * represented by value.getString().
     */
    protected void removeResourceIfNecessary(final ValueImpl value) {
        if (value.isResource()) {
            final SourceImpl source = value.getDefinition().getSource();
            final String resourcePath = source.toModulePath(value.getString());

            if (source.getType() == SourceType.CONFIG) {
                source.getModule().addConfigResourceToRemove(resourcePath);
            }
            else {
                source.getModule().addContentResourceToRemove(resourcePath);
            }
        }
    }

    /**
     * Create a new ContentSourceImpl within one of the toExport modules to store content for the provided contentPath.
     * Note: this method performs a three-step check for what module to use similar to
     * {@link #getOrCreateLocalDef(JcrPath, ModuleImpl)}.
     * @param changePath the path whose content we want to store in the new source
     * @param allContentSourcesByNodePath all existing content sources in the full model, not just in toExport modules
     */
    protected ContentDefinitionImpl createNewContentSource(final JcrPath changePath,
                                                           final SortedMap<JcrPath, ContentDefinitionImpl> allContentSourcesByNodePath) {

        // what module does the auto-export config tell us to use?
        final ModuleImpl defaultModule = getModuleByAutoExportConfig(changePath);

        // where is the nearest ancestor node defined?
        // we should scan allContentSourcesByNodePath first, and then possibly the config model
        final Optional<ModuleImpl> maybeAncestorModule = allContentSourcesByNodePath.entrySet().stream()
                .filter(entry -> changePath.startsWith(entry.getKey())).findFirst()
                .map(entry -> entry.getValue().getSource().getModule());

        // if there's no ancestor content source, the parent of the changePath must be a config node
        final ModuleImpl ancestorModule =
                maybeAncestorModule.orElseGet(()-> findModuleOfParent(changePath, defaultModule));

        // is the nearest ancestor defined downstream of the suggested module? if so, use the ancestor's module
        final ModuleImpl destModule = chooseMostDownstream(defaultModule, ancestorModule);

        // there's no existing source, so we need to create one -- on what path within the module?
        final String sourcePath = getFilePathByLocationMapper(changePath);

        // TODO should we export the changePath into this new source, or the LocationMapper contextPath?
        // TODO ... we want the source root def to match the node expected from the source file name, right?

        // if there's already a source with this path, generate a unique name
        final Predicate<String> sourceExists = s ->
                destModule.getModifiableSources().stream()
                        .filter(SourceType.CONTENT::isOfType)
                        .anyMatch(source -> source.getPath().equals(s));

        final String uniqueSourcePath =
                FilePathUtils.generateUniquePath(sourcePath, sourceExists, 0);

        // create a new source and content definition with change path
        return destModule.addContentSource(uniqueSourcePath).addContentDefinition(changePath);
    }

}
