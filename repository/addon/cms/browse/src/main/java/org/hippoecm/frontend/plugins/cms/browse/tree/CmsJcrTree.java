/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.cms.browse.tree;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.model.tree.LabelTreeNode;
import org.hippoecm.frontend.widgets.ContextMenuTree;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CmsJcrTree extends ContextMenuTree {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    static final Logger log = LoggerFactory.getLogger(CmsJcrTree.class);
    private static final long serialVersionUID = 1L;

    public static interface ITreeNodeTranslator extends IClusterable {
        String getName(TreeNode treeNode, int level);
        String getTitleName(TreeNode treeNode);
        boolean hasTitle(TreeNode treeNode, int level);
    }
    
    public static class TreeNodeTranslator implements ITreeNodeTranslator {
        private static final long serialVersionUID = 1L;

        public String getName(TreeNode treeNode, int level) {
            return getTitleName(treeNode);
        }

        private NodeTranslator newTranslator(TreeNode treeNode) {
            return new NodeTranslator(((IJcrTreeNode) treeNode).getNodeModel());
        }

        public String getTitleName(TreeNode treeNode) {
            if (treeNode instanceof IJcrTreeNode) {
                return newTranslator(treeNode).getNodeName().getObject();
            } else if (treeNode instanceof LabelTreeNode) {
                return ((LabelTreeNode) treeNode).getLabel();
            }
            return null;
        }

        public boolean hasTitle(TreeNode treeNode, int level) {
            return (treeNode instanceof IJcrTreeNode || treeNode instanceof LabelTreeNode);
        }
    }

    private ITreeNodeTranslator treeNodeTranslator;

    public CmsJcrTree(String id, TreeModel treeModel, ITreeNodeTranslator treeNodeTranslator) {
        super(id, treeModel);
        this.treeNodeTranslator = treeNodeTranslator;
    }

    @Override
    protected Component newNodeIcon(MarkupContainer parent, String id, final TreeNode node) {
        return new WebMarkupContainer(id) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                tag.put("class", getCss(node));
            }
        };
    }

    private String getCss(TreeNode treeNode) {
        if (treeNode instanceof IJcrTreeNode) {
            String css = "icon-16 ";
            if (treeNode.isLeaf() == true) {
                if (isVirtual((IJcrTreeNode) treeNode)) {
                    css += "leaf-virtual-16";
                } else {
                    css += "leaf-16";
                }
            } else {
                if (isVirtual((IJcrTreeNode) treeNode)) {
                    if (isNodeExpanded(treeNode)) {
                        css += "folder-virtual-open-16";
                    } else {
                        css += "folder-virtual-16";
                    }
                } else {
                    if (isNodeExpanded(treeNode)) {
                        css += "folder-open-16";
                    } else {
                        css += "folder-16";
                    }
                }
            }
            return css;
        } else {
            return "";
        }
    }

    /**
     * Checks if the wrapped jcr node is a virtual node
     * @return true if the node is virtual else false
     */
    public boolean isVirtual(IJcrTreeNode node) {
        IModel<Node> nodeModel = node.getNodeModel();
        if (nodeModel == null) {
            return false;
        }
        Node jcrNode = nodeModel.getObject();
        if (jcrNode == null || !(jcrNode instanceof HippoNode)) {
            return false;
        }
        try {
            HippoNode hippoNode = (HippoNode) jcrNode;
            Node canonical = hippoNode.getCanonicalNode();
            if (canonical == null) {
                return true;
            }
            return !canonical.isSame(hippoNode);
        } catch (ItemNotFoundException e) {
            // canonical node no longer exists
            return true;
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String renderNode(TreeNode treeNode, int level) {
        return treeNodeTranslator.getName(treeNode, level);
    }

    @Override
    protected void decorateNodeLink(MarkupContainer nodeLink, TreeNode node, int level) {
        if (treeNodeTranslator.hasTitle(node, level)) {
            nodeLink.add(new AttributeAppender("title", true, new Model(treeNodeTranslator.getTitleName(node)), " "));
        }
    }

}
