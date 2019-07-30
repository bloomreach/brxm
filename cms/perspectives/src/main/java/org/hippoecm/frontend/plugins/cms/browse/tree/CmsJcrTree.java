/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.swing.tree.TreeNode;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.extensions.markup.html.tree.AbstractTree;
import org.apache.wicket.extensions.markup.html.tree.DefaultTreeState;
import org.apache.wicket.extensions.markup.html.tree.ITreeState;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.attributes.TitleAttribute;
import org.hippoecm.frontend.model.NodeNameModel;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.model.tree.LabelTreeNode;
import org.hippoecm.frontend.plugins.standards.tree.icon.ITreeNodeIconProvider;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.widgets.ContextMenuTree;

public abstract class CmsJcrTree extends ContextMenuTree {

    public interface ITreeNodeTranslator extends IClusterable {
        String getName(TreeNode treeNode, int level);

        String getTitleName(TreeNode treeNode);

        boolean hasTitle(TreeNode treeNode, int level);
    }

    public static class TreeNodeTranslator implements ITreeNodeTranslator {

        public String getName(TreeNode treeNode, int level) {
            return getTitleName(treeNode);
        }

        public String getTitleName(TreeNode treeNode) {
            if (treeNode instanceof IJcrTreeNode) {
                final IModel<Node> nodeModel = ((IJcrTreeNode) treeNode).getNodeModel();
                return new NodeNameModel(nodeModel).getObject();
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
            return new NodeIconContainer(id, node, this, treeNodeIconService);
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
            IModel<String> titleModel = new Model<>(treeNodeTranslator.getTitleName(node));
            nodeLink.add(TitleAttribute.append(titleModel));
        }

        // Embed node state for testing
        nodeLink.add(ClassAttribute.append(() -> getTreeState().isNodeExpanded(node)
                ? "hippo-tree-node-expanded"
                : "hippo-tree-node-collapsed"));
    }

    protected MarkupContainer newJunctionImage(final MarkupContainer parent, final String id,
                                               final TreeNode node)
    {
        return new CaretJunctionImage(id, node) {

            @Override
            public boolean isExpanded(final TreeNode node) {
                return isNodeExpanded(node);
            }
        };
    }

    public static class NodeIconContainer extends Panel {

        private final TreeNode node;
        private final AbstractTree tree;
        private final ITreeNodeIconProvider treeNodeIconService;

        public NodeIconContainer(final String id, final TreeNode node, final AbstractTree tree,
                                  final ITreeNodeIconProvider treeNodeIconService) {
            super(id);
            this.node = node;
            this.tree = tree;
            this.treeNodeIconService = treeNodeIconService;
        }

        @Override
        protected void onBeforeRender() {
            addOrReplace(treeNodeIconService.getNodeIcon("icon", node, tree.getTreeState()));
            super.onBeforeRender();
        }
    }

    public abstract static class CaretJunctionImage extends WebMarkupContainer {

        private final TreeNode node;

        public CaretJunctionImage(final String id, final TreeNode node) {
            super(id);
            this.node = node;
            setRenderBodyOnly(true);
        }

        @Override
        protected void onComponentTag(final ComponentTag tag) {
            super.onComponentTag(tag);

            final Icon icon = node.isLeaf() ? Icon.BULLET :
                    isExpanded(node) ? Icon.CARET_DOWN : Icon.CARET_RIGHT;
            final String cssClassOuter = isNodeLast() ? "junction-last" : "junction";

            final Response response = RequestCycle.get().getResponse();
            response.write("<span class=\"" + cssClassOuter + "\">");
            response.write(icon.getSpriteReference(IconSize.S));
            response.write("</span>");
        }

        private boolean isNodeLast() {
            TreeNode parent = node.getParent();
            return parent == null || parent.getChildAt(parent.getChildCount() - 1).equals(node);
        }

        public abstract boolean isExpanded(TreeNode node);
    }
}
