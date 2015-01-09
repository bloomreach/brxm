/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.widgets;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tree.ITreeState;
import org.apache.wicket.extensions.markup.html.tree.LinkType;
import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.model.tree.ILabelTreeNode;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;

public abstract class JcrTree extends Tree {
    private static final long serialVersionUID = 1L;

    /**
     * use own styling
     */
    private static final ResourceReference TREE_STYLE = new CssResourceReference(JcrTree.class, "res/tree.css");

    /**
     * Reference to the icon of open tree folder
     */
    private static final ResourceReference VIRTUAL_FOLDER_OPEN = new PackageResourceReference(JcrTree.class,
            "icons/folder-open-virtual.gif");

    private static final ResourceReference VIRTUAL_FOLDER_CLOSED = new PackageResourceReference(JcrTree.class,
            "icons/folder-closed-virtual.gif");

    /**
     * Reference to the icon of tree item (not a folder)
     */
    private static final ResourceReference VIRTUAL_ITEM = new PackageResourceReference(JcrTree.class, "icons/item-virtual.gif");

    private static final Map<String, String> pathColors;

    static final Logger log = LoggerFactory.getLogger(JcrTree.class);

    static {
        pathColors = new HashMap<>();
        pathColors.put("/hst:hst", "#673AB7");
        pathColors.put("/hippo:configuration", "#009688");
        pathColors.put("/content", "#4CAF50");
        pathColors.put("/hippo:namespaces", "#FFA000");
        pathColors.put("/formdata", "#9E9E9E");
        pathColors.put("/webresources", "#00BCD4");
        pathColors.put("/hippo:reports", "#795548");
        pathColors.put("/hippo:log", "#607D8B");
    }

    public JcrTree(String id, TreeModel treeModel) {
        super(id, treeModel);
        setLinkType(LinkType.AJAX);

        ITreeState treeState = getTreeState();
        treeState.setAllowSelectMultiple(false);
        treeState.expandNode(treeModel.getRoot());
    }

    @Override
    protected ResourceReference getCSS() {
        return TREE_STYLE;
    }

    @Override
    protected abstract void onNodeLinkClicked(AjaxRequestTarget target, TreeNode clickedNode);

    @Override
    public String renderNode(TreeNode treeNode) {
        String result = "unknown";
        if (treeNode instanceof IJcrTreeNode) {
            Node node = ((IJcrTreeNode) treeNode).getNodeModel().getObject();
            if (node != null) {
                try {
                    result = node.getName();
                    if ((node instanceof HippoNode) && !node.isNodeType(HippoNodeType.NT_FACETSEARCH)) {
                        if (node.hasProperty(HippoNodeType.HIPPO_COUNT)) {
                            result += " [" + node.getProperty(HippoNodeType.HIPPO_COUNT).getLong() + "]";
                        }
                    }
                } catch (RepositoryException e) {
                    result = e.getMessage();
                }
            }
        } else if (treeNode instanceof ILabelTreeNode) {
            return ((ILabelTreeNode) treeNode).getLabel();
        }
        return result;
    }

    protected Component newNodeIcon(final MarkupContainer parent, final String id, final TreeNode node) {
        final IModel<Node> nodeModel = ((IJcrTreeNode) node).getNodeModel();
        if (nodeModel == null) {
            return new Icon(id, JcrNodeIcon.getDefaultIconType());
        }
        Node jcrNode = nodeModel.getObject();
        if (jcrNode == null) {
            return new Icon(id, JcrNodeIcon.getDefaultIconType());
        }
        Icon icon = new Icon(id, JcrNodeIcon.getIcon(jcrNode));
        icon.add(new AttributeAppender("style", "color:" + getIconColor(jcrNode) + ";"));

        final String tooltip = getTooltip(jcrNode);
        if(StringUtils.isNotBlank(tooltip)) {
            icon.add(new AttributeAppender("title", tooltip));
        }
        return icon;
    }

    private String getTooltip(final Node jcrNode) {
        try {
            if (jcrNode.hasProperty("hippostd:state")) {
                return jcrNode.getProperty("hippostd:state").getString();
            }
        } catch (RepositoryException e) {
            // ignore
        }
        return null;
    }

    private String getIconColor(final Node jcrNode) {
        try {
            final String path = jcrNode.getPath();

            if(path.startsWith("/content") && jcrNode.hasProperty("hippostd:state")) {
                final String state = jcrNode.getProperty("hippostd:state").getString();
                switch (state) {
                    case "published":
                        return "#4CAF50";
                    case "unpublished":
                        return "#3F51B5";
                    case "draft":
                        return "#795548";
                }
            }

            if(JcrNodeIcon.isNodeType(jcrNode, "hippofacnav:facetnavigation")) {
                return "#00BCD4";
            }

            if(isVirtual(jcrNode)) {
                return "#FF9800";
            }

            for (Map.Entry<String, String> pathColor : pathColors.entrySet()) {
                if (path.startsWith(pathColor.getKey())) {
                    return pathColor.getValue();
                }
            }
        } catch (RepositoryException e) {
            // ignore, use default color
        }
        return "#90CAF9";
    }

    /**
     * Checks if the wrapped jcr node is a virtual node
     *
     * @return true if the node is virtual else false
     */
    public boolean isVirtual(Node jcrNode) {
        try {
            HippoNode hippoNode = (HippoNode) jcrNode;
            return hippoNode.isVirtual();
        } catch (RepositoryException e) {
            log.info("Cannot determine whether node '{}' is virtual, assuming it's not", JcrUtils.getNodePathQuietly(jcrNode), e);
            return false;
        }
    }

    /**
     * Returns the resource reference of default closed tree folder.
     *
     * @return The package resource reference
     */
    protected ResourceReference getVirtualFolderClosed() {
        return VIRTUAL_FOLDER_CLOSED;
    }

    /**
     * Returns the resource reference of default open tree folder.
     *
     * @return The package resource reference
     */
    protected ResourceReference getVirtualFolderOpen() {
        return VIRTUAL_FOLDER_OPEN;
    }

    /**
     * Returns the resource reference of default tree item (not folder).
     *
     * @return The package resource reference
     */
    protected ResourceReference getVirtualItem() {
        return VIRTUAL_ITEM;
    }

}
