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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.collections4.iterators.IteratorIterable;
import org.apache.commons.lang.StringUtils;
import org.onehippo.cm.model.ActionItem;
import org.onehippo.cm.model.ActionType;
import org.onehippo.cm.model.ConfigurationItemCategory;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.DefinitionNode;
import org.onehippo.cm.model.Module;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.util.ConfigurationModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.MapUtils.isNotEmpty;

/**
 * Applies content definitions to the repository
 */
public class ConfigurationContentService {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationContentService.class);
    public static final String SEPARATOR = "/";

    private final ValueProcessor valueProcessor = new ValueProcessor();
    private final JcrContentProcessingService contentProcessingService = new JcrContentProcessingService(valueProcessor);
    private final ConfigurationBaselineService configurationBaselineService;

    ConfigurationContentService(final ConfigurationBaselineService configurationBaselineService) {
        this.configurationBaselineService = configurationBaselineService;
    }

    /**
     * Apply content definitions from modules contained within configuration model
     *
     * @param model {@link ConfigurationModel}
     * @param session active {@link Session}
     */
    public void apply(final ConfigurationModelImpl model, final Session session) throws RepositoryException {

        final Stream<ModuleImpl> modulesStream = model.getSortedGroups().stream()
                .flatMap(g -> g.getProjects().stream())
                .flatMap(p -> p.getModules().stream());
        for (ModuleImpl module : new IteratorIterable<>(modulesStream.iterator())) {
            if (isNotEmpty(module.getActionsMap()) || isNotEmpty(module.getContentDefinitions())) {
                apply(module, model, session);
            }
        }
    }

    /**
     * Import ContentDefinition
     *
     * @param node {@link DefinitionNode} to import
     * @param parentNode parent node
     * @param actionType action type
     */
    public void importNode(DefinitionNode node, Node parentNode, ActionType actionType) throws RepositoryException, IOException {
        contentProcessingService.importNode(node, parentNode, actionType);
    }

    /**
     * Export node to module
     *
     * @param node node to export
     * @return Module containing single content definition
     */
    public ModuleImpl exportNode(Node node) throws RepositoryException {
        return contentProcessingService.exportNode(node);
    }

    /**
     * Apply content definitions from module
     *
     * @param module target {@link Module}
     * @param session active {@link Session}
     */
    private void apply(final ModuleImpl module, final ConfigurationModel model, final Session session) throws RepositoryException {
        final double moduleSequenceNumber = module.getSequenceNumber() != null ? module.getSequenceNumber() : Double.MIN_VALUE;
        final List<ActionItem> actionsToProcess = collectNewActions(moduleSequenceNumber, module.getActionsMap());
        processItemsToDelete(actionsToProcess, model, session);
        session.save();

        sortContentDefinitions(module);
        for (final ContentDefinitionImpl contentDefinition : module.getContentDefinitions()) {
            final DefinitionNode contentNode = contentDefinition.getNode();
            final String baseNodePath = contentNode.getPath();
            final Optional<ActionType> optionalAction = findLastActionToApply(baseNodePath, actionsToProcess);
            final boolean nodeAlreadyProcessed = configurationBaselineService.getAppliedContentPaths().contains(baseNodePath);

            if (optionalAction.isPresent() || !nodeAlreadyProcessed) {
                final ActionType action = optionalAction.orElse(ActionType.APPEND);

                if (ConfigurationModelUtils.getCategoryForNode(baseNodePath, model) == ConfigurationItemCategory.CONTENT) {
                    log.debug("Processing {} action for node: {}", action, baseNodePath);
                    contentProcessingService.apply(contentNode, action, session);

                    if (!nodeAlreadyProcessed) {
                        configurationBaselineService.addAppliedContentPath(baseNodePath);
                    }
                    session.save();
                } else {
                    log.warn(String.format("Base node '%s' is not categorized as content, skipping action '%s'.",
                            baseNodePath, action));
                }
            }
        }

        configurationBaselineService.updateModuleSequenceNumber(module);
    }

    /**
     * Collect new items that were not processed yet
     *
     * @param currentVersion Latest sequence number that was processed
     * @param actionsMap     Action items per version map
     * @return New action items
     */
    private List<ActionItem> collectNewActions(final double currentVersion, final Map<Double, Set<ActionItem>> actionsMap) {
        return actionsMap.entrySet().stream().filter(e -> e.getKey() > currentVersion)
                .flatMap(e -> e.getValue().stream()).collect(toList());
    }

    /**
     * Sort content definitions in natural order of their root node paths,
     * i.e. node with a shortest hierarchy path goes first
     *
     * @param module target module
     */
    private void sortContentDefinitions(final ModuleImpl module) {
        module.getContentDefinitions().sort(Comparator.comparing(o -> o.getNode().getPath()));
    }

    /**
     * Delete nodes from action list
     *
     * @param items items to delete
     * @param session active {@link Session}
     */
    private void processItemsToDelete(final List<ActionItem> items, final ConfigurationModel model,
                                      final Session session) throws RepositoryException {
        for (final ActionItem item : items) {
            if (item.getType() == ActionType.DELETE) {
                final String baseNodePath = item.getPath();
                if (ConfigurationModelUtils.getCategoryForNode(baseNodePath, model) == ConfigurationItemCategory.CONTENT) {
                    log.debug("Processing delete action for node: {}", baseNodePath);

                    final DefinitionNode deleteNode = new DefinitionNodeImpl(baseNodePath,
                            StringUtils.substringAfterLast(baseNodePath, SEPARATOR), null);
                    contentProcessingService.apply(deleteNode, ActionType.DELETE, session);
                } else {
                    log.warn(String.format("Base node '%s' is not categorized as content, skipping delete action.",
                            baseNodePath));
                }
            }
        }
    }

    /**
     * Find last action for the path specified, DELETE action type is excluded
     *
     * @param absolutePath full path of the node
     * @param actions      available actions
     * @return If found, contains last action for specified path
     */
    private Optional<ActionType> findLastActionToApply(final String absolutePath, final List<ActionItem> actions) {
        return actions.stream()
                .filter(x -> x.getPath().equals(absolutePath) && x.getType() != ActionType.DELETE)
                .map(ActionItem::getType)
                .reduce((__, second) -> second);
    }
}
