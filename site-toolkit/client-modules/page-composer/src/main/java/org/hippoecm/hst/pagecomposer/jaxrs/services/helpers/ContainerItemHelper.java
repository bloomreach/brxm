/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstComponentParameters;
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
     */
    public void acquireLock(final Node containerItem, final long versionStamp) throws RepositoryException {
        if (!containerItem.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
            throw new ClientException(String.format("Expected node of type '%s' but was of type '%s'",
                    HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT, containerItem.getPrimaryNodeType().getName()),
                    ClientError.INVALID_NODE_TYPE);
        }
        Node container = findContainerNode(containerItem);
        if (container == null) {
            throw new ClientException(String.format("Expected to find an ancestor of type '%s' for '%s' but non found",
                    HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT, containerItem.getPath()),
                    ClientError.ITEM_NOT_FOUND);
        }
        lockHelper.acquireLock(container, versionStamp);
    }

    /**
     * Locks the container item identified by containerItemId with the given time stamp. Will throw a {@link
     * ClientException} if the item cannot be found, is not a container item or is already locked.
     *
     * @param containerItemId the identifier of the container item to be locked
     * @param versionStamp    timestamp used for the lock
     * @param session         a user bound session
     * @throws ClientException
     * @throws RepositoryException
     */
    public void lock(String containerItemId, long versionStamp, Session session) throws RepositoryException, ClientException {
        try {
            final Node containerItem = pageComposerContextService.getRequestConfigNodeById(containerItemId, NODETYPE_HST_CONTAINERITEMCOMPONENT, session);
            new HstComponentParameters(containerItem, this).lock(versionStamp);
            log.info("Component locked successfully.");
        } catch (ItemNotFoundException e) {
            throw new ClientException("container item with id " + containerItemId + " not found", ITEM_NOT_FOUND);
        }
    }

    /**
     * if the container of <code>containerItem</code> is locked by the user, it is unlocked.
     * When the container is not locked, or another user has the lock, a ClientException is thrown.
     */
    public void releaseLock(Node containerItem) throws RepositoryException {
        if (!containerItem.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
            throw new ClientException(String.format("Expected node of type '%s' but was of type '%s'",
                HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT, containerItem.getPrimaryNodeType().getName()),
                ClientError.INVALID_NODE_TYPE);
        }
        Node container = findContainerNode(containerItem);
        if (container == null) {
            throw new ClientException(String.format("Expected to find an ancestor of type '%s' for '%s' but non found",
                HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT, containerItem.getPath()),
                ClientError.ITEM_NOT_FOUND);
        }
        if (!lockHelper.canLock(container)) {
            throw new ClientException(String.format("Expected an ancester of '%s' to be locked by user '%s'",
                containerItem.getPath(), containerItem.getSession().getUserID()),
                ClientError.ITEM_ALREADY_LOCKED);
        }
        lockHelper.unlock(container);
    }

    /**
     * Unlocks the container item identified by containerItemId.  Will throw an exception if the container item
     * is not locked, or is locked by another user.
     *
     * @param containerItemId   the identifier of the container item to be unlocked
     * @param session           a user bound session
     * @throws RepositoryException
     */
    public void unlock(String containerItemId, Session session) throws RepositoryException, ClientException {
        try {
            final Node containerItem = pageComposerContextService.getRequestConfigNodeById(containerItemId, NODETYPE_HST_CONTAINERITEMCOMPONENT, session);
            new HstComponentParameters(containerItem, this).unlock();
            log.info("Component locked successfully.");
        } catch (ItemNotFoundException e) {
            throw new ClientException("container item with id " + containerItemId + " not found", ITEM_NOT_FOUND);
        }
    }

    /**
     * @return Returns the ancestor node (or itself) of type <code>hst:componentcontainer</code> for <code>configNode</code> and <code>null</code> if
     * <code>configNode</code> does not have an ancestor of type <code>hst:componentcontainer</code>
     */
    public static Node findContainerNode(final Node containerItem) throws RepositoryException {
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
