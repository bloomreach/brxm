/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services.helpers;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuItemRepresentation;

import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMENUITEM_PROPERTY_EXTERNALLINK;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMENUITEM_PROPERTY_REPOBASED;

public class SiteMenuItemHelper {

    /**
     * Saves the properties of the new item into the node, provided that the names of the node and the item are equal.
     *
     * @param node    a newly created node
     * @param newItem an item containing the property values for the node
     * @throws RepositoryException
     */
    public void save(Node node, SiteMenuItemRepresentation newItem) throws RepositoryException {
        assert node.getName().equals(newItem.getName()) : "Precondition violated: node and item name must be equal";
        update(node, newItem);
    }

    /**
     * Updates the properties of the given node with those of the modified item.
     *
     * @param node         a node
     * @param modifiedItem an item containing the property values of the new item
     * @throws RepositoryException
     */
    public void update(Node node, SiteMenuItemRepresentation modifiedItem) throws RepositoryException {
        final String newName = modifiedItem.getName();
        if (newName != null && !newName.equals(node.getName())) {
            rename(node, newName);
        }
        final String newExternalLink = modifiedItem.getExternalLink();
        if (newExternalLink != null) {
            node.setProperty(SITEMENUITEM_PROPERTY_EXTERNALLINK, newExternalLink);
        }
        final String newSiteMapItemPath = modifiedItem.getSiteMapItemPath();
        if (newSiteMapItemPath != null) {
            node.setProperty(SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM, newSiteMapItemPath);
        }
        final boolean newRepositoryBased = modifiedItem.isRepositoryBased();
        node.setProperty(SITEMENUITEM_PROPERTY_REPOBASED, newRepositoryBased);
        // TODO (meggermont) add all other properties too.
    }

    /**
     * Move the given node by appending it as the last child of the new parent and assigning it the give new node name.
     *
     * @param node        the node to move
     * @param newNodeName the new name of the node
     * @param newParent   the new parent of the node
     * @throws RepositoryException
     */
    public void move(Node node, String newNodeName, Node newParent) throws RepositoryException {
        node.getSession().move(node.getPath(), newParent.getPath() + "/" + newNodeName);
    }

    /**
     * Move the given node by appending it as the last child of the new parent.
     *
     * @param node      the node to move
     * @param newParent the new parent of the node
     * @throws RepositoryException
     */
    public void move(Node node, Node newParent) throws RepositoryException {
        move(node, node.getName(), newParent);
    }

    private void rename(Node node, String newName) throws RepositoryException {
        final Node parent = node.getParent();
        // remember the successor name, to be able to restore the node's position
        final String successorName = getSuccessorName(node, parent);
        // rename the node by moving it within the same parent
        move(node, newName, parent);
        // restore the position
        parent.orderBefore(newName, successorName);
    }

    private String getSuccessorName(final Node node, final Node parent) throws RepositoryException {
        final String currentName = node.getName();
        final Iterator<Node> children = parent.getNodes();
        while (children.hasNext() && !children.next().getName().equals(currentName)) {
        }
        if (children.hasNext()) {
            return children.next().getName();
        } else {
            return null;
        }
    }

}
