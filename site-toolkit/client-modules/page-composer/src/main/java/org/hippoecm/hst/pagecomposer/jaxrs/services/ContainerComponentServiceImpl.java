/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItem;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemImpl;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.util.ContainerUtils;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT;
import static org.hippoecm.hst.pagecomposer.jaxrs.util.UUIDUtils.isValidUUID;

public class ContainerComponentServiceImpl implements ContainerComponentService {
    private static Logger log = LoggerFactory.getLogger(ContainerComponentServiceImpl.class);

    private PageComposerContextService pageComposerContextService;
    private ContainerHelper containerHelper;

    public ContainerComponentServiceImpl(PageComposerContextService pageComposerContextService, ContainerHelper containerHelper) {
        this.pageComposerContextService = pageComposerContextService;
        this.containerHelper = containerHelper;
    }

    @Override
    public ContainerItem createContainerItem(final Session session, final String catalogItemUUID, final long versionStamp)
            throws RepositoryException {
        try {
            final Node catalogItem = ContainerUtils.getContainerItem(session, catalogItemUUID);
            final Node containerNode = lockAndGetContainer(versionStamp);

            // now we have the catalogItem that contains 'how' to create the new containerItem and we have the
            // containerNode. Find a correct newName and create a new node.
            final String newItemNodeName = ContainerUtils.findNewName(catalogItem.getName(), containerNode);
            final Node newItem = JcrUtils.copy(session, catalogItem.getPath(), containerNode.getPath() + "/" + newItemNodeName);

            HstConfigurationUtils.persistChanges(session);

            final long newVersionStamp =  JcrUtils.getLongProperty(containerNode, HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED, 0L);

            return new ContainerItemImpl(newItem, newVersionStamp);
        } catch (RepositoryException e) {
            log.warn("Exception during creating new container item: {}", catalogItemUUID);
            throw e;
        }
    }

    @Override
    public ContainerItem createContainerItem(final Session session, final String catalogItemUUID, final String siblingItemUUID, final long versionStamp) throws RepositoryException {

        final ContainerItem containerItem = createContainerItem(session, catalogItemUUID, versionStamp);
        final String newItemName = containerItem.getContainerItem().getName();
        final Node siblingItem = ContainerUtils.getContainerItem(session, siblingItemUUID);
        final String siblingItemName = siblingItem.getName();

        final Node containerNode = lockAndGetContainer(versionStamp);
        if (siblingItem.getParent().isSame(containerNode)) {
            containerNode.orderBefore(newItemName, siblingItemName);
            HstConfigurationUtils.persistChanges(session);
        } else {
            log.warn("Cannot order new item '{}' before '{}' because container '{}' does not contain '{}'.",
                    newItemName, siblingItemName, containerNode.getPath(), siblingItemName);
        }
        return containerItem;
    }

    @Override
    public void updateContainer(final Session session, final ContainerRepresentation container) throws RepositoryException {
        try {

            validateContainerItems(session, container.getChildren());

            final Node containerNode = lockAndGetContainer(container.getLastModifiedTimestamp());
            ContainerUtils.updateContainerOrder(session, container.getChildren(), containerNode, nodeToBeLocked -> {
                try {
                    containerHelper.acquireLock(nodeToBeLocked, 0);
                } catch (RepositoryException e) {
                    throw new RuntimeRepositoryException(e);
                }
            });
            HstConfigurationUtils.persistChanges(session);
        } catch (RepositoryException e) {
            log.warn("Exception during updating container item: {}", container);
            throw e;
        }
    }

    @Override
    public void deleteContainerItem(final Session session, final String itemUUID, final long versionStamp) throws RepositoryException {
        try {
            final Node containerItem = ContainerUtils.getContainerItem(session, itemUUID);

            lockAndGetContainer(versionStamp);
            containerItem.remove();
            HstConfigurationUtils.persistChanges(session);
        } catch (RepositoryException e) {
            log.warn("Failed to delete node with id '{}'.", itemUUID);
            throw e;
        }
    }

    private Node lockAndGetContainer(final long versionStamp) throws RepositoryException {
        final Node containerNode = pageComposerContextService.getRequestConfigNode(HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT);
        if (containerNode == null) {
            log.warn("Exception during creating new container item : Could not find container node to add item to.");
            throw new ClientException("Could not find container node to add item to", ClientError.ITEM_NOT_FOUND);
        }
        try {
            // the acquireLock also checks all ancestors whether they are not locked by someone else
            containerHelper.acquireLock(containerNode, versionStamp);
        } catch (ClientException e) {
            log.info("Exception while trying to lock '{}'", containerNode.getPath());
            throw e;
        }
        return containerNode;
    }

    /**
     * <p>
     *     All the childIds that should be placed below this container MUST be part of the current HST configuration its
     *     hst:workspsace! It is not allowed to move container items from
     *     <ul>
     *         <li>outside the hst:workspace</li>
     *         <li>other HST Configurations </li>
     *         <li>XPages</li>
     *     </ul>
     *     to the current container
     * </p>
     * @throws ClientException in case the {@code childsIds} are not valid
     */
    private void validateContainerItems(final Session session, final List<String> childIds) throws RepositoryException, ClientException {
        final String requiredWorkspacePrefix = pageComposerContextService.getEditingPreviewConfigurationPath() + "/hst:workspace/";

        for (String childId : childIds) {
            if (!isValidUUID(childId)) {
                throw new ClientException(String.format("Invalid child id '%s'", childId), ClientError.INVALID_UUID);
            }
            try {
                final Node componentItem = session.getNodeByIdentifier(childId);
                if (!componentItem.getPath().startsWith(requiredWorkspacePrefix)) {
                    throw new ClientException(String.format("Child '%s' is not allowed to be moved to a different " +
                                    "hst:workspace",
                            componentItem.getPath()), ClientError.ITEM_NOT_CORRECT_LOCATION);
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

}
