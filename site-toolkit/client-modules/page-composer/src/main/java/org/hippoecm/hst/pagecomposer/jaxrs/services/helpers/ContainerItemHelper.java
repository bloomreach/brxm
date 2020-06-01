/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.services.helpers;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError.ITEM_NOT_FOUND;

public class ContainerItemHelper extends AbstractHelper {

    private static final Logger log = LoggerFactory.getLogger(ContainerItemHelper.class);

    /**
     * @throws ClientException in case no <code>HstComponentConfiguration</code> can be found for <code>itemId</code>
     * in <code>pageComposerContextService.getEditingPreviewSite()</code>
     */
    @SuppressWarnings("unchecked")
    @Override
    public HstComponentConfiguration getConfigObject(final String itemId) {
        final HstSite editingPreviewSite = pageComposerContextService.getEditingPreviewSite();
        return getHstComponentConfiguration(editingPreviewSite.getComponentsConfiguration(), itemId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public HstComponentConfiguration getConfigObject(final String itemId, final Mount mount) {
        final HstSite site = mount.getHstSite();
        return getHstComponentConfiguration(site.getComponentsConfiguration(), itemId);
    }

    @Override
    protected String getNodeType() {
        return NODETYPE_HST_CONTAINERITEMCOMPONENT;
    }

    /**
     * @throws ClientException if not found
     */
    private HstComponentConfiguration getHstComponentConfiguration(final HstComponentsConfiguration componentsConfiguration,
                                                                   final String itemId) {

        for (HstComponentConfiguration hcc : componentsConfiguration.getComponentConfigurations().values()) {
            HstComponentConfiguration found = getHstComponentConfiguration(hcc, itemId);
            if (found != null) {
                return found;
            }
        }
        final String message = String.format("HstComponentConfiguration item with id '%s' is not part of currently edited preview site.", itemId);
        throw new ClientException(message, ClientError.ITEM_NOT_IN_PREVIEW);

    }

    private HstComponentConfiguration getHstComponentConfiguration(final HstComponentConfiguration hcc, final String itemId) {
        if (hcc.getCanonicalIdentifier().equals(itemId)) {
            return hcc;
        }
        for (HstComponentConfiguration child : hcc.getChildren().values()) {
            HstComponentConfiguration found = getHstComponentConfiguration(child, itemId);
            if (found != null) {
                log.debug("Found '{}' for '{}'", found, itemId);
                return found;
            }
        }
        return null;
    }

    /**
     * if the container of <code>containerItem</code> is already locked for the user <code>containerItem.getSession()</code> this method does not do
     * anything. If there is no lock yet, a lock for the current session userID gets set on the node. If there is
     * already a lock by another user a ClientException is thrown,
     *
     * Note this method does *not* persist the changes.
     */
    public void acquireLock(final Node containerItem, final long versionStamp) throws RepositoryException {
        acquireLock(containerItem, versionStamp, containerItem.getSession().getUserID());
    }

    /**
     * Locks the container item identified by containerItemId with the given time stamp. Will throw a {@link
     * ClientException} if the item cannot be found, is not a container item or is already locked.
     *
     * Note this method does *not* persist the changes.
     *
     * @param containerItemId the identifier of the container item to be locked
     * @param lockFor         the user to lock the container for
     * @param versionStamp    timestamp used for the lock
     * @throws ClientException
     * @throws RepositoryException
     */
    public void acquireLock(final String containerItemId, final String lockFor, long versionStamp) throws RepositoryException, ClientException {
        try {
            Session session = RequestContextProvider.get().getSession();
            final Node containerItem = pageComposerContextService.getRequestConfigNodeById(containerItemId, NODETYPE_HST_CONTAINERITEMCOMPONENT, session);
            acquireLock(containerItem, versionStamp, lockFor);
            log.info("Component locked successfully.");
        } catch (ItemNotFoundException e) {
            throw new ClientException("container item with id " + containerItemId + " not found", ITEM_NOT_FOUND);
        }
    }

    private void acquireLock(final Node containerItem, final long versionStamp, final String lockFor) throws RepositoryException {
        Node container = getContainerNode(containerItem);
        lockHelper.acquireLock(container, lockFor, versionStamp);
    }

    /**
     * if the container of <code>containerItem</code> is locked by the user, it is unlocked.
     * When the container is not locked, or another user has the lock, a ClientException is thrown.
     *
     * Note this method does *not* persist the changes.
     */
    public void releaseLock(final Node containerItem) throws RepositoryException {
        releaseLock(containerItem, containerItem.getSession().getUserID());
    }

    /**
     * Unlocks the container item identified by containerItemId.  Will throw an exception if the container item
     * is not locked, or is locked by another user.
     *
     * Note this method does *not* persist the changes.
     *
     * @param containerItemId   the identifier of the container item to be unlocked
     * @param lockOwner         the user owning the lock
     * @throws RepositoryException
     */
    @SuppressWarnings("UnusedDeclaration")
    public void releaseLock(final String containerItemId, final String lockOwner) throws RepositoryException, ClientException {
        try {
            Session session = RequestContextProvider.get().getSession();
            final Node containerItem = pageComposerContextService.getRequestConfigNodeById(containerItemId, NODETYPE_HST_CONTAINERITEMCOMPONENT, session);
            releaseLock(containerItem, lockOwner);
            log.info("Component unlocked successfully.");
        } catch (ItemNotFoundException e) {
            throw new ClientException("container item with id " + containerItemId + " not found", ITEM_NOT_FOUND);
        }
    }

    private void releaseLock(final Node containerItem, final String lockOwner) throws RepositoryException {
        Node container = getContainerNode(containerItem);
        lockHelper.acquireLock(container, lockOwner, 0);
        lockHelper.unlock(container);
    }

    private static Node getContainerNode(final Node containerItem) throws RepositoryException, ClientException {
        if (!containerItem.isNodeType(NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
            throw new ClientException(String.format("Expected node of type '%s' but was of type '%s'",
                NODETYPE_HST_CONTAINERITEMCOMPONENT, containerItem.getPrimaryNodeType().getName()),
                ClientError.INVALID_NODE_TYPE);
        }
        Node container = findContainerNode(containerItem);
        if (container == null) {
            throw new ClientException(String.format("Expected to find an ancestor of type '%s' for '%s' but non found",
                HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT, containerItem.getPath()),
                ClientError.ITEM_NOT_FOUND);
        }
        return container;
    }

    /**
     * @return Returns the ancestor node (or itself) of type <code>hst:componentcontainer</code> for <code>configNode</code> and <code>null</code> if
     * <code>configNode</code> does not have an ancestor of type <code>hst:componentcontainer</code>
     */
    public static Node findContainerNode(final Node containerItem) throws RepositoryException, ClientException {
        return findAncestorContainer(containerItem.getParent(), HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT);
    }

    /**
     * @return Returns the ancestor node (or itself) of type <code>requiredNodeType</code> for <code>configNode</code> and <code>null</code> if
     * <code>configNode</code> does not have an ancestor of type <code>requiredNodeType</code>
     */
    public static Node findAncestorContainer(final Node node, final String requiredNodeType) throws RepositoryException {
        Node rootNode = node.getSession().getRootNode();
        Node current = node;
        while (true) {
            if (current.equals(rootNode)) {
                return null;
            }
            if (current.isNodeType(requiredNodeType)) {
                return current;
            }
            current = current.getParent();
        }
    }
}
