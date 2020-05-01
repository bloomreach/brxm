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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT;

public class ContainerUtils {

    private static final Logger log = LoggerFactory.getLogger(ContainerUtils.class);

    private ContainerUtils() {
    }

    public static Node getContainerItem(final Session session, final String itemUUID) throws RepositoryException {

        try {
            final Node containerItem = session.getNodeByIdentifier(itemUUID);

            if (!containerItem.isNodeType(NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
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

}
