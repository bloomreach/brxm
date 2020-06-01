/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.InvalidNodeTypeException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ItemNotFoundException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT;

public class ContainerComponentServiceImpl implements ContainerComponentService {
    private static Logger log = LoggerFactory.getLogger(ContainerComponentServiceImpl.class);

    private PageComposerContextService pageComposerContextService;
    private ContainerHelper containerHelper;

    class ContainerItemImpl implements ContainerItem {

        private final Node containerItem;
        private final long timeStamp;

        public ContainerItemImpl(Node containerItem, long timeStamp) {
            this.containerItem = containerItem;
            this.timeStamp = timeStamp;
        }

        @Override
        public Node getContainerItem() {
            return containerItem;
        }

        @Override
        public long getTimeStamp() {
            return timeStamp;
        }
    }

    public ContainerComponentServiceImpl(PageComposerContextService pageComposerContextService, ContainerHelper containerHelper) {
        this.pageComposerContextService = pageComposerContextService;
        this.containerHelper = containerHelper;
    }

    @Override
    public ContainerItem createContainerItem(final Session session, final String catalogItemUUID, final long versionStamp)
            throws RepositoryException, ClientException {
        try {
            final Node catalogItem = getContainerItem(session, catalogItemUUID);
            final Node containerNode = lockAndGetContainer(versionStamp);

            // now we have the catalogItem that contains 'how' to create the new containerItem and we have the
            // containerNode. Find a correct newName and create a new node.
            final String newItemNodeName = findNewName(catalogItem.getName(), containerNode);
            final Node newItem = JcrUtils.copy(session, catalogItem.getPath(), containerNode.getPath() + "/" + newItemNodeName);

            HstConfigurationUtils.persistChanges(session);

            final long newVersionStamp = getVersionStamp(containerNode);
            return new ContainerItemImpl(newItem, newVersionStamp);
        } catch (RepositoryException e) {
            log.warn("Exception during creating new container item: {}", e);
            throw e;
        }
    }

    @Override
    public void updateContainer(final Session session, final ContainerRepresentation container) throws ClientException, RepositoryException {
        try {
            final Node containerNode = lockAndGetContainer(container.getLastModifiedTimestamp());

            updateContainerOrder(session, container, containerNode);
            HstConfigurationUtils.persistChanges(session);
        } catch (RepositoryException e) {
            log.warn("Exception during updating container item: {}", e);
            throw e;
        }
    }

    @Override
    public void deleteContainerItem(final Session session, final String itemUUID, final long versionStamp) throws ClientException, RepositoryException {
        try {
            final Node containerItem = getContainerItem(session, itemUUID);

            lockAndGetContainer(versionStamp);
            containerItem.remove();
            HstConfigurationUtils.persistChanges(session);
        } catch (RepositoryException e) {
            log.warn("Failed to delete node with id '" + itemUUID + "':", e);
            throw e;
        }
    }

    private void updateContainerOrder(final Session session,
                                      final ContainerRepresentation container,
                                      final Node containerNode) throws RepositoryException {
        final List<String> children = container.getChildren();
        int childCount = (children != null ? children.size() : 0);
        if (childCount > 0) {
            try {
                for (String childId : children) {
                    moveIfNeeded(containerNode, childId, session);
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
                log.warn("ItemNotFoundException: Cannot update containerNode '{}'.", containerNode.getPath(), e);
                throw e;
            }
        }
    }

    private long getVersionStamp(final Node node) throws RepositoryException {
        final long versionStamp;
        if (node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED)) {
            versionStamp = node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED).getDate().getTimeInMillis();
        } else {
            versionStamp = 0;
        }
        return versionStamp;
    }

    private Node lockAndGetContainer(final long versionStamp) throws ClientException, RepositoryException {
        final Node containerNode = pageComposerContextService.getRequestConfigNode(HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT);
        if (containerNode == null) {
            log.warn("Exception during creating new container item : Could not find container node to add item to.");
            throw new ItemNotFoundException("Could not find container node to add item to");
        }
        try {
            // the acquireLock also checks all ancestors whether they are not locked by someone else
            containerHelper.acquireLock(containerNode, versionStamp);
        } catch (ClientException e) {
            log.info("Exception while trying to lock '" + containerNode.getPath() + "': ", e);
            throw e;
        }
        return containerNode;
    }

    private Node getContainerItem(final Session session, final String itemUUID) throws RepositoryException, ClientException {
        final Node containerItem = session.getNodeByIdentifier(itemUUID);

        if (!containerItem.isNodeType(NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
            log.warn("The container component '{}' does not have the correct type. ", itemUUID);
            throw new InvalidNodeTypeException("The container component does not have the correct type.", itemUUID);
        }
        return containerItem;
    }

    /**
     * Move the node identified by {@code childId} to node {@code parent} if it has a different parent.
     */
    private void moveIfNeeded(final Node parent,
                              final String childId,
                              final Session session) throws RepositoryException {
        String parentPath = parent.getPath();
        Node childNode = session.getNodeByIdentifier(childId);
        if (!childNode.isNodeType(NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
            final String msg = String.format("Expected a move of a node of type '%s' but was '%s'.", NODETYPE_HST_CONTAINERITEMCOMPONENT,
                    childNode.getPrimaryNodeType().getName());
            throw new IllegalArgumentException(msg);
        }
        String childPath = childNode.getPath();
        String childParentPath = childPath.substring(0, childPath.lastIndexOf('/'));
        if (!parentPath.equals(childParentPath)) {
            // lock the container from which the node gets removed
            // note that the 'timestamp' check must not be the timestamp of the 'target' container
            // since this one can be different. We do not need a 'source' timestamp check, since, if the source
            // has changed it is either locked, or if the child item does not exist any more on the server, another
            // error occurs already
            containerHelper.acquireLock(childNode.getParent(), 0);
            String name = childPath.substring(childPath.lastIndexOf('/') + 1);
            name = findNewName(name, parent);
            String newChildPath = parentPath + "/" + name;
            log.debug("Move needed from '{}' to '{}'.", childPath, newChildPath);
            session.move(childPath, newChildPath);
        } else {
            log.debug("No Move needed for '{}' below '{}'", childId, parentPath);
        }
    }

    private static String findNewName(String base, Node parent) throws RepositoryException {
        String newName = base;
        int counter = 0;
        while (parent.hasNode(newName)) {
            newName = base + ++counter;
        }
        log.debug("New child name '{}' for parent '{}'", newName, parent.getPath());
        return newName;
    }
}
