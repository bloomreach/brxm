/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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

import javax.swing.tree.TreeNode;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tree.DefaultTreeState;
import org.apache.wicket.extensions.markup.html.tree.ITreeState;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.model.tree.LabelTreeNode;
import org.hippoecm.frontend.plugins.standards.tree.icon.ITreeNodeIconProvider;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.widgets.ContextMenuTree;

public abstract class CmsJcrTree extends ContextMenuTree {

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

    private final ITreeNodeTranslator treeNodeTranslator;
    private final ITreeNodeIconProvider treeNodeIconService;

    public CmsJcrTree(String id, JcrTreeModel treeModel, ITreeNodeTranslator treeNodeTranslator,
            ITreeNodeIconProvider iconService) {
        super(id, treeModel);
        this.treeNodeTranslator = treeNodeTranslator;
        this.treeNodeIconService = iconService;
    }

    @Override
    protected ITreeState newTreeState() {
        DefaultTreeState state = new DefaultTreeState();
        JcrTreeModel model = (JcrTreeModel) getModelObject();
        model.setTreeState(state);
        return state;
    }

    @Override
    protected Component newNodeIcon(final MarkupContainer parent, final String id, final TreeNode node) {
        if (treeNodeIconService != null) {
            Component icon = treeNodeIconService.getNodeIcon(id, node, getTreeState());
            if (icon != null) {
                return icon;
            }
        }
        return super.newNodeIcon(parent, id, node);
    }

    @Override
    public String renderNode(TreeNode treeNode, int level) {
        return treeNodeTranslator.getName(treeNode, level);
    }

    @Override
    protected void decorateNodeLink(MarkupContainer nodeLink, final TreeNode node, int level) {
        if (treeNodeTranslator.hasTitle(node, level)) {
            IModel<String> titleModel = new Model<String>(treeNodeTranslator.getTitleName(node));
            nodeLink.add(new AttributeAppender("title", true, titleModel, " "));
        }

        nodeLink.add(new AttributeAppender("class", new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                // Embed node state for testing
                return getTreeState().isNodeExpanded(node) ? "hippo-tree-node-expanded" : "hippo-tree-node-collapsed";
            }
        }, " "));
    }

    protected MarkupContainer newJunctionImage(final MarkupContainer parent, final String id,
                                               final TreeNode node)
    {
        return (MarkupContainer)new WebMarkupContainer(id)
        {
            private static final long serialVersionUID = 1L;

            /**
             * {@inheritDoc}
             */
            @Override
            protected void onComponentTag(final ComponentTag tag)
            {
                super.onComponentTag(tag);

                final Icon icon = node.isLeaf() ? Icon.BULLET_LARGE : 
                        isNodeExpanded(node) ? Icon.CARET_DOWN_TINY : Icon.CARET_RIGHT_TINY;
                final String cssClassOuter = isNodeLast(node) ? "junction-last" : "junction";
  
                final Response response = RequestCycle.get().getResponse();
                response.write("<span class=\"" + cssClassOuter + "\">");
                response.write(icon.getSpriteReference());
                response.write("</span>");
            }
        }.setRenderBodyOnly(true);
    }

    private boolean isNodeLast(TreeNode node) {
        TreeNode parent = node.getParent();
        return parent == null || parent.getChildAt(parent.getChildCount() - 1).equals(node);
    }
    
}
