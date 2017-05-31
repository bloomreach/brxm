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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.collections4.iterators.IteratorIterable;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.NodeNameCodec;
import org.onehippo.cm.model.ActionItem;
import org.onehippo.cm.model.ActionType;
import org.onehippo.cm.model.ConfigurationItemCategory;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.ConfigurationNode;
import org.onehippo.cm.model.DefinitionNode;
import org.onehippo.cm.model.Module;
import org.onehippo.cm.model.impl.ConfigurationModelImpl;
import org.onehippo.cm.model.impl.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.MapUtils.isNotEmpty;
import static org.onehippo.cm.engine.Constants.HCM_BASELINE;
import static org.onehippo.cm.engine.Constants.HCM_CONTENT_PATHS_APPLIED;
import static org.onehippo.cm.engine.Constants.HCM_MODULE_SEQUENCE;
import static org.onehippo.cm.engine.Constants.HCM_ROOT;
import static org.onehippo.cm.engine.Constants.NT_HCM_CONTENT;
import static org.onehippo.cm.engine.Constants.NT_HCM_ROOT;

/**
 * Applies content definitions to repository
 */
public class ConfigurationContentService {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationContentService.class);
    public static final String SEPARATOR = "/";

    private final ValueProcessor valueProcessor = new ValueProcessor();
    private final JcrContentProcessingService contentProcessingService = new JcrContentProcessingService(valueProcessor);

    /**
     * TODO     current operations cast to/require ModuleImpl, which first needs to be corrected
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
    public Module exportNode(Node node) throws RepositoryException {
        return contentProcessingService.exportNode(node);
    }

    /**
     * Apply content definitions from module
     *
     * @param module target {@link Module}
     * @param session active {@link Session}
     */
    private void apply(final ModuleImpl module, final ConfigurationModel model, final Session session) throws RepositoryException {

        final double currentVersion = getModuleVersion(module, session);

        final Map<Double, Set<ActionItem>> actionsMap = module.getActionsMap();

        final List<ActionItem> actionsToProcess = collectNewActions(currentVersion, actionsMap);

        final List<ActionItem> itemsToDelete = actionsToProcess.stream().filter(x -> x.getType() == ActionType.DELETE).collect(toList());

        validateDeleteActions(model, itemsToDelete);
        processItemsToDelete(itemsToDelete, session);
        session.save();

        sortContentDefinitions(module);

        for (final ContentDefinitionImpl contentDefinition : module.getContentDefinitions()) {
            final DefinitionNode contentNode = contentDefinition.getNode();

            final Optional<ActionType> actionType = findLastActionToApply(contentNode.getPath(), actionsToProcess);
            final boolean nodeAlreadyProcessed = nodeAlreadyProcessed(contentNode, session);

            if (actionType.isPresent() || !nodeAlreadyProcessed) {

                if (isContentNodePathValid(contentNode.getPath(), model)) {
                    throw new ConfigurationRuntimeException(String.format("Content node '%s' creation is not allowed within configuration path", contentNode.getPath()));
                };

                log.debug("Processing {} action for node: {}", actionType, contentNode.getPath());
                contentProcessingService.apply(contentNode, actionType.orElse(ActionType.APPEND), session);

                if (!nodeAlreadyProcessed) {
                    updateProcessedDefinition(contentNode.getPath(), session);
                }

                session.save();
            }
        }

        final Optional<Double> latestVersion = actionsMap.keySet().stream().max(Double::compareTo);
        if (latestVersion.isPresent()) {
            module.setSequenceNumber(latestVersion.get());
            session.save();
        }
    }

    /**
     * Check if content node is allowed to be within configuration path
     * @param contentNodePath node's path
     * @param model {@link ConfigurationModel}
     */
    boolean isContentNodePathValid(final String contentNodePath, final ConfigurationModel model) {

        final ConfigurationNode closestConfigNode = findClosestConfigNode(Paths.get(contentNodePath), model);

        if (closestConfigNode != null) {
            final String[] configPathSplit = closestConfigNode.getPath().split(SEPARATOR);
            final String[] contentPathSplit = contentNodePath.split(SEPARATOR);

            final Optional<String> nodeToCheck = Arrays.stream(contentPathSplit).limit(configPathSplit.length + 1).reduce((a, b) -> a + SEPARATOR + b);
            if (nodeToCheck.isPresent()) {
                final String nodeName = StringUtils.substringAfterLast(nodeToCheck.get(), SEPARATOR);
                final ConfigurationItemCategory childNodeCategory = closestConfigNode.getChildNodeCategory(nodeName);
                if (childNodeCategory != ConfigurationItemCategory.CONTENT) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Find the deepest configuration node which shares the path with content node
     * @param contentNodePath content node's path
     * @param model reference to {@link ConfigurationModel}
     * @return reference to configuration node or null if nodes do not have common root
     */
    private ConfigurationNode findClosestConfigNode(final Path contentNodePath, final ConfigurationModel model) {
        if (contentNodePath == null) {
            return null;
        }

        final ConfigurationNode configurationNode = model.resolveNode(contentNodePath.toString());
        return configurationNode != null ? configurationNode : findClosestConfigNode(contentNodePath.getParent(), model);
    }

    /**
     * Validate if DELETE action is not related to configuration node
     *
     * @param model {@link ConfigurationModel}
     * @param itemsToDelete collection of action items to delete
     */
     void validateDeleteActions(final ConfigurationModel model, final List<ActionItem> itemsToDelete) {

        itemsToDelete.forEach(item -> {
            if (!isContentNodePathValid(item.getPath(), model)) {
                throw new ConfigurationRuntimeException(String.format("Config definitions are not allowed to be deleted: %s", item.getPath()));
            }
        });
    }

    /**
     * Update processed definition lists (TODO SS: move to baseline service?)
     *
     * @param path node path
     * @param session active {@link Session}
     */
    private void updateProcessedDefinition(final String path, final Session session) throws RepositoryException {

        log.debug("Saving processed definition path: {}", path);
        final Node contentNode = createContentNodeIfNecessary(session);

        final List<Value> valueList = new ArrayList<>();
        final Value newValue = session.getValueFactory().createValue(path);
        valueList.add(newValue);

        if (contentNode.hasProperty(HCM_CONTENT_PATHS_APPLIED)) {
            final Value[] values = contentNode.getProperty(HCM_CONTENT_PATHS_APPLIED).getValues();
            valueList.addAll(Arrays.asList(values));
        }

        contentNode.setProperty(HCM_CONTENT_PATHS_APPLIED, valueList.toArray(new Value[0]));
    }

    private Node createContentNodeIfNecessary(final Session session) throws RepositoryException {
        final Node rootNode = session.getRootNode();
        final boolean hcmNodeExisted = rootNode.hasNode(HCM_ROOT);
        final Node hcmRootNode = createNodeIfNecessary(rootNode, HCM_ROOT, NT_HCM_ROOT, false);
        if (!hcmNodeExisted) {
            session.save();
        }

        return createNodeIfNecessary(hcmRootNode, NT_HCM_CONTENT, NT_HCM_CONTENT, false);
    }


    /**
     * TODO SS: this method should be used from baseline service
     *
     * @param parent
     * @param name
     * @param type
     * @param encode
     * @return
     * @throws RepositoryException
     */
    private Node createNodeIfNecessary(Node parent, String name, String type, boolean encode) throws RepositoryException {
        if (encode) {
            name = NodeNameCodec.encode(name);
        }
        if (!parent.hasNode(name)) {
            parent.addNode(name, type);
        }
        return parent.getNode(name);
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
     * Check if node was already processed and it's path is saved withing baseline
     *
     * @param contentNode {@link DefinitionNode}
     * @param session active {@link Session}
     * @return true if node was already processed, false otherwise
     */
    private boolean nodeAlreadyProcessed(final DefinitionNode contentNode, final Session session) throws RepositoryException {

        try {
            final String hcmContentNodePath = String.format("/%s/%s", HCM_ROOT, NT_HCM_CONTENT);
            if (!session.nodeExists(hcmContentNodePath)) {
                return false;
            }
            final Node node = session.getNode(hcmContentNodePath);
            final Value[] values = node.getProperty(HCM_CONTENT_PATHS_APPLIED).getValues();
            for (final Value value : values) {
                final String strValue = value.getString();
                if (contentNode.getPath().equals(strValue)) {
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
     *
     * @param deleteItems items to delete
     * @param session active {@link Session}
     */
    private void processItemsToDelete(final List<ActionItem> deleteItems, final Session session) throws RepositoryException {

        for (final ActionItem deleteItem : deleteItems) {
            final DefinitionNode deleteNode = new DefinitionNodeImpl(deleteItem.getPath(), StringUtils.substringAfterLast(deleteItem.getPath(), SEPARATOR), null);
            log.debug("Processing delete action for node: {}", deleteItem.getPath());
            contentProcessingService.apply(deleteNode, ActionType.DELETE, session);
        }
    }

    /**
     * Get module's version from baseline
     *
     * @param module  target module
     * @param session Active session
     * @return Current module's version or MINIMAL value of double
     */
    private double getModuleVersion(final Module module, final Session session) throws RepositoryException {
        try {
            final String moduleNodePath = String.format("/%s/%s/%s", HCM_ROOT, HCM_BASELINE, ((ModuleImpl) module).getFullName());
            Node node = session.getNode(moduleNodePath);
            return node.getProperty(HCM_MODULE_SEQUENCE).getDouble();
        } catch (PathNotFoundException ignored) {
        }

        return Double.MIN_VALUE;
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
