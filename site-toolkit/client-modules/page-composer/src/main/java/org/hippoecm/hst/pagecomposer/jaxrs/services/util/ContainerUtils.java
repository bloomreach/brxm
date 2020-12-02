/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.util;

import java.util.List;
import java.util.function.Consumer;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.COMPONENT_PROPERTY_COMPONENTDEFINITION;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_COMPONENTDEFINITION;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT;
import static org.hippoecm.hst.platform.utils.UUIDUtils.isValidUUID;

public class ContainerUtils {

    private static final Logger log = LoggerFactory.getLogger(ContainerUtils.class);

    private ContainerUtils() {
    }

    public static Node getContainerItem(final Session session, final String itemUUID) throws RepositoryException {

        try {
            final Node containerItem = session.getNodeByIdentifier(itemUUID);

            if (!containerItem.isNodeType(NODETYPE_HST_CONTAINERITEMCOMPONENT) && !containerItem.isNodeType(NODETYPE_HST_COMPONENTDEFINITION)) {
                log.info("The container component '{}' does not have the correct type. ", itemUUID);
                throw new ClientException(String.format("The container item '%s' does not have the correct type",
                        itemUUID), ClientError.INVALID_NODE_TYPE);
            }
            return containerItem;
        } catch (ItemNotFoundException e) {
            log.info("Cannot find container item '{}'.", itemUUID);
            throw new ClientException(String.format("Cannot find container item '%s'",
                    itemUUID), ClientError.INVALID_UUID);
        }
    }

    public static String findNewName(String base, Node parent) throws RepositoryException {
        String newName = base;
        int counter = 0;
        while (parent.hasNode(newName)) {
            newName = base + ++counter;
        }
        log.debug("New child name '{}' for parent '{}'", newName, parent.getPath());
        return newName;
    }

    public static Node createComponentItem(final Node containerNode, final Node catalogItem,
                                           final HstComponentConfiguration componentDefinition) throws RepositoryException {

        // now we have the catalogItem that contains 'how' to create the new containerItem and we have the
        // containerNode. Find a correct newName and create a new node.
        final String newItemNodeName = findNewName(catalogItem.getName(), containerNode);

        final Session session = containerNode.getSession();
        final Node newItem;
        if (catalogItem.getPrimaryNodeType().getName().equals(NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
            //If it is a legacy catalog item, i.e. it's type is hst:containeritemcomponent, copy the whole catalog item node
            newItem = JcrUtils.copy(session, catalogItem.getPath(), containerNode.getPath() + "/" + newItemNodeName);
        } else {
            newItem = containerNode.addNode(newItemNodeName, NODETYPE_HST_CONTAINERITEMCOMPONENT);
            newItem.setProperty(COMPONENT_PROPERTY_COMPONENTDEFINITION, componentDefinition.getId());
        }
        return newItem;
    }


    public static void updateContainerOrder(final Session session,
                                            final List<String> children,
                                            final Node containerNode,
                                            final Consumer<Node> locker) throws RepositoryException {
        int childCount = (children != null ? children.size() : 0);
        if (childCount > 0) {
            try {
                for (String childId : children) {
                    moveIfNeeded(containerNode, childId, session, locker);
                }
                int index = childCount - 1;

                while (index > -1) {
                    String childId = children.get(index);
                    Node childNode = session.getNodeByIdentifier(childId);
                    String nodeName = childNode.getName();

                    int next = index + 1;
                    if (next == childCount) {
                        containerNode.orderBefore(nodeName, null);
                    } else {
                        Node nextChildNode = session.getNodeByIdentifier(children.get(next));
                        containerNode.orderBefore(nodeName, nextChildNode.getName());
                    }
                    --index;
                }
            } catch (javax.jcr.ItemNotFoundException e) {
                log.warn("ItemNotFoundException: Cannot update containerNode '{}'.", containerNode.getPath());
                throw e;
            }
        }
    }

    /**
     * Move the node identified by {@code childId} to node {@code targetParent} if it has a different targetParent.
     */
    public static void moveIfNeeded(final Node targetParent,
                                    final String childId,
                                    final Session session,
                                    final Consumer<Node> locker) throws RepositoryException {
        final String parentPath = targetParent.getPath();
        final Node childNode = session.getNodeByIdentifier(childId);
        if (!childNode.isNodeType(NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
            final String msg = String.format("Expected a move of a node of type '%s' but was '%s'.", NODETYPE_HST_CONTAINERITEMCOMPONENT,
                    childNode.getPrimaryNodeType().getName());
            throw new IllegalArgumentException(msg);
        }
        final String childPath = childNode.getPath();
        final String childParentPath = childPath.substring(0, childPath.lastIndexOf('/'));
        if (!parentPath.equals(childParentPath)) {
            // lock the container from which the node gets removed
            // note that the 'timestamp' check must not be the timestamp of the 'target' container
            // since this one can be different. We do not need a 'source' timestamp check, since, if the source
            // has changed it is either locked, or if the child item does not exist any more on the server, another
            // error occurs already
            locker.accept(childNode.getParent());
            String name = childPath.substring(childPath.lastIndexOf('/') + 1);
            name = findNewName(name, targetParent);
            String newChildPath = parentPath + "/" + name;
            log.debug("Move needed from '{}' to '{}'.", childPath, newChildPath);
            session.move(childPath, newChildPath);
        } else {
            log.debug("No Move needed for '{}' below '{}'", childId, parentPath);
        }
    }

    public static void validateChildren(final Session session, final List<String> childIds, final String pathPrefix) throws RepositoryException {
        for (String childId : childIds) {
            if (!isValidUUID(childId)) {
                throw new ClientException(String.format("Invalid child id '%s'", childId), ClientError.INVALID_UUID);
            }
            try {
                final Node componentItem = session.getNodeByIdentifier(childId);
                if (!componentItem.getPath().startsWith(pathPrefix)) {
                    throw new ClientException(String.format("Child '%s' is not allowed to be moved because does not start " +
                            "with '%s'", componentItem.getPath(), pathPrefix), ClientError.ITEM_NOT_CORRECT_LOCATION);
                }
                if (!componentItem.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
                    throw new ClientException(String.format("Child '%s' has not a valid nodetype for a container",
                            componentItem.getPath()), ClientError.INVALID_NODE_TYPE);
                }
            } catch (ItemNotFoundException e) {
                throw new ClientException("Could not find one of the children in the container", ClientError.INVALID_UUID);
            }
        }
    }

    public static HstComponentConfiguration getCatalogItem(final PageComposerContextService pageComposerContextService,
                                                           final Node catalogItem) throws RepositoryException {
        final HstSite hstSite = pageComposerContextService.getEditingMount().getHstSite();
        final List<HstComponentConfiguration> availableContainerItems = hstSite.getComponentsConfiguration().getAvailableContainerItems();
        final String catalogItemPath = catalogItem.getPath();
        final HstComponentConfiguration catalogItemConfiguration =
                availableContainerItems.stream()
                        .filter(item -> item.getCanonicalStoredLocation().equals(catalogItemPath))
                        .findFirst().orElse(null);

        if (catalogItemConfiguration == null) {
            throw new RepositoryException(String.format("Catalog item '%s' at path '%s' could not be found",
                    catalogItem.getName(), catalogItem.getPath()));
        }

        return catalogItemConfiguration;
    }

    /**
     * returns the HstComponentConfiguration templateConfig (componentDefinition) in case the {@code componentItem} links
     * back to catalog item. For the old-style component items where the catalog item is not back-referenced, {@code null}
     * is returned
     */
    public static HstComponentConfiguration getComponentDefinitionByComponentItem(final PageComposerContextService pageComposerContextService,
                                                                                  final Node componentItem) throws RepositoryException {
        if (!componentItem.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
            throw new IllegalArgumentException(String.format("Can only get catalog item for a "));
        }

        final HstSite hstSite = pageComposerContextService.getEditingMount().getHstSite();

        final String componentDefinition = JcrUtils.getStringProperty(componentItem, NODETYPE_HST_COMPONENTDEFINITION, null);
        if (componentDefinition == null) {
            // old style component item not linked back to catalog item
            return null;
        }

        final List<HstComponentConfiguration> availableContainerItems = hstSite.getComponentsConfiguration().getAvailableContainerItems();

        final HstComponentConfiguration catalogItemConfiguration =
                availableContainerItems.stream()
                        .filter(item -> item.getId().equals(componentDefinition))
                        .findFirst().orElse(null);

        if (catalogItemConfiguration == null) {
            throw new RepositoryException(String.format("Catalog item for component definition '%s' not found for component " +
                    "'%'", componentDefinition, componentItem.getPath()));
        }

        return catalogItemConfiguration;
    }

}
