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

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.widgets.JcrTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CmsJcrTree extends JcrTree {
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
            return (String) newTranslator(treeNode).getNodeName().getObject();
        }

        public boolean hasTitle(TreeNode treeNode, int level) {
            return true;
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
