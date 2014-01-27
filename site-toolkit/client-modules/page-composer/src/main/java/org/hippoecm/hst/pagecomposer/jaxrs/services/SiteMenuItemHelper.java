/*
 * Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import com.google.common.collect.Lists;

import org.apache.jackrabbit.commons.JcrUtils;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuItemRepresentation;

class SiteMenuItemHelper {

    public static final String HST_EXTERNALLINK = "hst:externallink";
    public static final String HST_REFERENCESITEMAPITEM = "hst:referencesitemapitem";

    public void update(Node node, SiteMenuItemRepresentation currentItem, SiteMenuItemRepresentation newItem) throws RepositoryException {
        final String newName = newItem.getName();
        if (newName != null && !newName.equals(currentItem.getName())) {
            moveAndRestorePosition(node, newName);
            currentItem.setName(newName);
        }
        final String newExternalLink = newItem.getExternalLink();
        if (newExternalLink != null && !newExternalLink.equals(currentItem.getExternalLink())) {
            node.setProperty(HST_EXTERNALLINK, newExternalLink);
            currentItem.setExternalLink(newExternalLink);
        }
        final String newSiteMapItemPath = newItem.getSiteMapItemPath();
        if (newSiteMapItemPath != null && !newSiteMapItemPath.equals(currentItem.getSiteMapItemPath())) {
            node.setProperty(HST_REFERENCESITEMAPITEM, newSiteMapItemPath);
            currentItem.setSiteMapItemPath(newSiteMapItemPath);
        }
        // TODO (meggermont) add all other properties too.
    }

    private void moveAndRestorePosition(Node node, String newName) throws RepositoryException {

        final int indexOfNextSibling = node.getIndex() + 1;
        final Node parent = node.getParent();
        final List<Node> children = Lists.newArrayList(JcrUtils.getChildNodes(parent));

        final String beforeNodeName;
        if (indexOfNextSibling == children.size()) {
            beforeNodeName = newName;
        } else {
            beforeNodeName = children.get(indexOfNextSibling).getName();
        }

        final String currentPath = node.getPath();
        final String currentName = node.getName();
        final String newPath = currentPath.substring(0, currentPath.lastIndexOf(currentName)) + newName;
        node.getSession().move(currentPath, newPath);
        parent.orderBefore(newName, beforeNodeName);

    }
}
