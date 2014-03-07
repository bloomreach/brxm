/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;

public class ContainerItemHelper extends AbstractHelper {

    @SuppressWarnings("unchecked")
    @Override
    public HstComponentConfiguration getConfigObject(final String itemId) {
        throw new UnsupportedOperationException("not supported");
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
