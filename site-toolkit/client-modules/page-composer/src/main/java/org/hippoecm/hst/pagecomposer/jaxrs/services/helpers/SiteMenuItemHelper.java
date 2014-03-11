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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.commons.JcrUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.model.LinkType;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.repository.util.NodeIterable;

import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMENUITEM_PROPERTY_EXTERNALLINK;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMENUITEM_PROPERTY_REPOBASED;
import static org.hippoecm.repository.api.NodeNameCodec.decode;
import static org.hippoecm.repository.api.NodeNameCodec.encode;

public class SiteMenuItemHelper extends AbstractHelper {


    @SuppressWarnings("unchecked")
    @Override
    public Object getConfigObject(final String itemId) {
        throw new UnsupportedOperationException("Cannot fetch site menu item without menu id");
    }

    public Node create(Node parent, SiteMenuItemRepresentation newItem, Position position, String sibling) throws RepositoryException {
        lockHelper.acquireSimpleLock(getMenuAncestor(parent), 0);
        final String newItemName = newItem.getName();
        try {
            final Node newChild = parent.addNode(encode(newItemName, true), HstNodeTypes.NODETYPE_HST_SITEMENUITEM);
            repositionChildIfNecessary(newChild, position, sibling);
            update(newChild, newItem);
            return newChild;
        } catch (ItemExistsException e) {
            throw createClientException(parent, newItemName, e.getMessage());
        }
    }

    private void repositionChildIfNecessary(final Node newChild, final Position position, final String sibling) throws RepositoryException {
        final Node parent = newChild.getParent();
        final NodeIterator siblingIterator = parent.getNodes();
        if (siblingIterator.getSize() > 1) {
            switch (position) {
                case FIRST:
                    Node firstChild = siblingIterator.nextNode();
                    parent.orderBefore(newChild.getName(), firstChild.getName());
                    break;
                case AFTER:
                    if (!Strings.isNullOrEmpty(sibling)) {
                        boolean found = false;
                        for (Node siblingNode : new NodeIterable(siblingIterator)) {
                            if (found) {
                                parent.orderBefore(newChild.getName(), siblingNode.getName());
                                break;
                            } else if (siblingNode.getIdentifier().equals(sibling)) {
                                found = true;
                            }
                        }
                    }
                    break;
            }
        }
    }

    public void delete(final Node node) throws RepositoryException {
        lockHelper.acquireSimpleLock(getMenuAncestor(node), 0);
        node.remove();
    }

    /**
     * Updates the properties of the given node with those of the modified item.
     *
     * @param node         a node
     * @param modifiedItem an item containing the property values of the modified item
     * @throws RepositoryException
     */
    public void update(Node node, SiteMenuItemRepresentation modifiedItem) throws RepositoryException {

        lockHelper.acquireSimpleLock(getMenuAncestor(node), 0);

        final String modifiedName = modifiedItem.getName();
        if (modifiedName != null && !modifiedName.equals(decode(node.getName()))) {
            rename(node, modifiedName);
        }

        final String modifiedLink = modifiedItem.getLink();
        if (modifiedItem.getLinkType() == LinkType.NONE) {
            removeProperty(node, SITEMENUITEM_PROPERTY_EXTERNALLINK);
            removeProperty(node, SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM);
        } else if (modifiedItem.getLinkType() == LinkType.SITEMAPITEM) {
            node.setProperty(SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM, modifiedLink);
            removeProperty(node, SITEMENUITEM_PROPERTY_EXTERNALLINK);
        } else if (modifiedItem.getLinkType() == LinkType.EXTERNAL) {
            node.setProperty(SITEMENUITEM_PROPERTY_EXTERNALLINK, modifiedLink);
            removeProperty(node, SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM);
        }

        final boolean modifiedRepositoryBased = modifiedItem.isRepositoryBased();
        node.setProperty(SITEMENUITEM_PROPERTY_REPOBASED, modifiedRepositoryBased);

        final Map<String, String> modifiedLocalParameters = modifiedItem.getLocalParameters();
        setLocalParameters(node, modifiedLocalParameters);

        final Set<String> modifiedRoles = modifiedItem.getRoles();
        setRoles(node, modifiedRoles);

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
        lockHelper.acquireSimpleLock(getMenuAncestor(node), 0);
        try {
            node.getSession().move(node.getPath(), newParent.getPath() + "/" + encode(newNodeName, true));
        } catch (ItemExistsException e) {
            throw createClientException(newParent, newNodeName, e.getMessage());
        }
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

    /**
     * Move the source node into the parent at the given childIndex position.
     *
     * @param parent     the target parent
     * @param source     the target source
     * @param childIndex the index of child within parent
     * @throws RepositoryException
     */
    public void move(Node parent, Node source, Integer childIndex) throws RepositoryException {
        final String sourceName = source.getName();
        final String successorNodeName = getSuccessorOfSourceNodeName(parent, sourceName, childIndex);

        if (!source.getParent().isSame(parent)) {
            move(source, parent);
        }
        lockHelper.acquireSimpleLock(getMenuAncestor(source), 0);
        try {
            parent.orderBefore(encode(sourceName, true), successorNodeName);
        } catch (ItemExistsException e) {
            throw createClientException(parent, sourceName, e.getMessage());
        }
    }

    private void rename(Node node, String newName) throws RepositoryException {
        lockHelper.acquireSimpleLock(getMenuAncestor(node), 0);
        final Node parent = node.getParent();
        // remember the next sibling name, to be able to restore the node's position
        final String nextSiblingName = getNextSiblingName(node, parent);
        // rename the node by moving it within the same parent
        move(node, newName, parent);
        // restore the position
        parent.orderBefore(encode(newName, true), nextSiblingName);
    }

    private String getNextSiblingName(final Node node, final Node parent) throws RepositoryException {
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

    private Node getMenuAncestor(final Node node) throws RepositoryException {
        Node current = node;
        while (current.isNodeType(HstNodeTypes.NODETYPE_HST_SITEMENUITEM)) {
            current = current.getParent();
        }
        if (current.isNodeType(HstNodeTypes.NODETYPE_HST_SITEMENU)) {
            return current;
        }
        throw new IllegalStateException("No ancestor of type '" + HstNodeTypes.NODETYPE_HST_SITEMENU + "' " +
                "found for '" + node.getPath() + "'");
    }

    private String getSuccessorOfSourceNodeName(Node parent, String sourceName, Integer newIndex) throws RepositoryException {
        final List<Node> childNodes = Lists.newArrayList(JcrUtils.getChildNodes(parent));
        if (newIndex == 0) {
            // move to start
            return childNodes.isEmpty() ? null : childNodes.get(0).getName();
        }
        if (newIndex >= childNodes.size() - 1) {
            // move to end
            return null;
        }
        int currentIndex = 0;
        while (currentIndex < childNodes.size() && !sourceName.equals(childNodes.get(currentIndex).getName())) {
            currentIndex++;
        }
        if (currentIndex < newIndex) {
            // current index is before new index, so successor node is at position newIndex + 1
            return childNodes.get(newIndex + 1).getName();
        } else {
            // current index is at or after new index, so successor node is at position newIndex
            return childNodes.get(newIndex).getName();
        }
    }

    private ClientException createClientException(Node parent, String itemName, String message) throws RepositoryException {
        final String path = toPath(parent);
        final Map<?, ?> params = ImmutableMap.builder()
                .put("item", decode(itemName))
                .put("parentPath", decode(path))
                .build();
        if (path.isEmpty()) {
            return new ClientException(message, ClientError.ITEM_NAME_NOT_UNIQUE_IN_ROOT, params);
        } else {
            return new ClientException(message, ClientError.ITEM_NAME_NOT_UNIQUE, params);
        }
    }

    private String toPath(Node node) throws RepositoryException {
        final List<String> names = new ArrayList<>();
        while (!node.isNodeType(HstNodeTypes.NODETYPE_HST_SITEMENU)) {
            names.add(node.getName());
            node = node.getParent();
        }
        names.add("");
        Collections.reverse(names);
        return StringUtils.join(names, '/');
    }

}
