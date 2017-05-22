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
package org.onehippo.cm.backend;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cm.api.model.ConfigurationModel;
import org.onehippo.cm.api.model.ContentDefinition;
import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.api.model.Module;
import org.onehippo.cm.api.model.action.ActionItem;
import org.onehippo.cm.api.model.action.ActionType;
import org.onehippo.cm.impl.model.ContentDefinitionImpl;
import org.onehippo.cm.impl.model.DefinitionNodeImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.MapUtils.isNotEmpty;
import static org.onehippo.cm.engine.Constants.BASELINE_NODE;
import static org.onehippo.cm.engine.Constants.CONTENT_PATH_PROPERTY;
import static org.onehippo.cm.engine.Constants.HCM_CONTENT_FOLDER;
import static org.onehippo.cm.engine.Constants.HCM_ROOT_NODE;
import static org.onehippo.cm.engine.Constants.MODULE_SEQUENCE_NUMBER;

/**
 * Applies content definitions to repository
 */
public class ContentService {

    private static final Logger log = LoggerFactory.getLogger(ContentService.class);

    private final ValueConverter valueConverter = new ValueConverter();
    private final ContentProcessingService contentProcessingService = new JcrContentProcessingService(valueConverter);

    /**
     * Apply content definitions from modules contained within configuration model
     * @param model
     * @param session
     * @throws RepositoryException
     */
    public void apply(final ConfigurationModel model, final Session session) throws RepositoryException {

        final List<Module> modules = model.getSortedGroups().stream()
                .flatMap(g -> g.getProjects().stream())
                .flatMap(p -> p.getModules().stream())
                .collect(toList());
        for (final Module module : modules) {
            final ModuleImpl moduleImpl = (ModuleImpl) module;
            if (isNotEmpty(module.getActionsMap()) || isNotEmpty(moduleImpl.getContentDefinitions())) {
                apply(moduleImpl, session);
            }
        }
    }

    /**
     * Import ContentDefinition
     * @param definition
     * @param parentNode parent node
     * @param actionType action type
     * @throws Exception
     */
    public void importNode(ContentDefinition definition, Node parentNode, ActionType actionType) throws RepositoryException, IOException {
        contentProcessingService.importNode(definition.getNode(), parentNode, actionType);
    }

    /**
     * Export node to module
     * @param node node to export
     * @return Module containing single content definition
     */
    public Module exportNode(Node node) {
        return contentProcessingService.exportNode(node);
    }

    /**
     * Apply content definitions from module
     * @param module
     * @param session
     * @throws RepositoryException
     */
    private void apply(final ModuleImpl module, final Session session) throws RepositoryException {

        final double currentVersion = getModuleVersion(module, session);

        final Map<Double, Set<ActionItem>> actionsMap = module.getActionsMap();

        final List<ActionItem> actionsToProcess = actionsMap.entrySet().stream().filter(e -> e.getKey() > currentVersion)
                .flatMap(e -> e.getValue().stream()).collect(toList());

        final List<ActionItem> itemsToDelete = actionsToProcess.stream().filter(x -> x.getType() == ActionType.DELETE).collect(toList());
        processItemsToDelete(itemsToDelete, session);

        module.getContentDefinitions().sort(Comparator.comparing(o -> o.getNode().getPath()));
        for (final ContentDefinitionImpl contentDefinition : module.getContentDefinitions()) {
            final DefinitionNode contentNode = contentDefinition.getNode();
            final Optional<ActionType> actionType = findActionTypeToApply(contentNode.getPath(), actionsToProcess);
            if (actionType.isPresent() || !nodeAlreadyProcessed(contentNode, module, session)) {
                log.debug("Processing {} action for node: {}", actionType, contentNode.getPath());
                contentProcessingService.apply(contentNode, actionType.orElse(ActionType.APPEND), session);
            }
        }

        final Optional<Double> latestVersion = actionsMap.keySet().stream().max(Double::compareTo);
        latestVersion.ifPresent(module::setSequenceNumber);
    }

    /**
     * Check if node was already processed and it's path is saved withing baseline
     * @param contentNode
     * @param module
     * @param session
     * @return
     * @throws RepositoryException
     */
    private boolean nodeAlreadyProcessed(final DefinitionNode contentNode, final ModuleImpl module, final Session session) throws RepositoryException {

        try {
            final String moduleNodePath = String.format("/%s/%s/%s/%s", HCM_ROOT_NODE, BASELINE_NODE, module.getFullName(), HCM_CONTENT_FOLDER);
            final Node node = session.getNode(moduleNodePath);
            for (Node childNode : new NodeIterable(node.getNodes())) {
                final String jcrNodeContentPath = childNode.getProperty(CONTENT_PATH_PROPERTY).getString();
                if (contentNode.getPath().equals(jcrNodeContentPath)) {
                    return true;
                }
            }
        } catch (PathNotFoundException e) {
            log.debug("Error while accessing to content node", e);
        }

        return false;
    }

    /**
     * Delete nodes from action list
     * @param deleteItems
     * @param session
     * @throws RepositoryException
     */
    private void processItemsToDelete(final List<ActionItem> deleteItems, final Session session) throws RepositoryException {

        for (final ActionItem deleteItem : deleteItems) {
            final DefinitionNode deleteNode = new DefinitionNodeImpl(deleteItem.getPath(), StringUtils.substringAfterLast(deleteItem.getPath(), "/"), null);
            log.debug("Processing delete action for node: {}", deleteItem.getPath());
            contentProcessingService.apply(deleteNode, ActionType.DELETE, session);
        }
    }

    /**
     * Get module's version from baseline
     * @param module target module
     * @param session
     * @return Current module's version or MINIMAL value of double
     * @throws RepositoryException
     */
    private double getModuleVersion(final Module module, final Session session) throws RepositoryException {
        try {
            final String moduleNodePath = String.format("/%s/%s/%s", HCM_ROOT_NODE, BASELINE_NODE, ((ModuleImpl) module).getFullName());
            Node node = session.getNode(moduleNodePath);
            return node.getProperty(MODULE_SEQUENCE_NUMBER).getDouble();
        } catch (PathNotFoundException ignored) {}

        return Double.MIN_VALUE;
    }

    /**
     * Find an action type for the node, DELETE action type is excluded
     * @param absolutePath full path of the node
     * @param actions available actions
     * @return If found, contains action type for specified node
     */
    private Optional<ActionType> findActionTypeToApply(final String absolutePath, List<ActionItem> actions) {
        return actions.stream()
                .filter(x -> x.getPath().equals(absolutePath) && x.getType() != ActionType.DELETE)
                .map(ActionItem::getType)
                .findFirst();
    }
}
