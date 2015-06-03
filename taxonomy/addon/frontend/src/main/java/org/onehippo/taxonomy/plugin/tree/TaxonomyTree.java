/*
 *  Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin.tree;

import java.util.Locale;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tree.DefaultTreeState;
import org.apache.wicket.extensions.markup.html.tree.ITreeState;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.plugins.cms.browse.tree.CmsJcrTree;
import org.hippoecm.frontend.plugins.standards.tree.icon.ITreeNodeIconProvider;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.frontend.widgets.ContextMenuTree;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.plugin.api.TaxonomyHelper;

public class TaxonomyTree extends ContextMenuTree {
    private static final long serialVersionUID = 1L;

    private String preferredLanguage;
    private final ITreeNodeIconProvider treeNodeIconService;

    public TaxonomyTree(String id, TreeModel model, String preferredLanguage, ITreeNodeIconProvider treeNodeIconService) {
        super(id, model);
        this.preferredLanguage = preferredLanguage;
        this.treeNodeIconService = treeNodeIconService;
        expandNode((AbstractNode) getModelObject().getRoot());
    }

    public void expandNode(final AbstractNode node) {
        getTreeState().expandNode(node);
    }

    public void expandAllToNode(final AbstractNode node) {
        expandNode(node);

        AbstractNode root = (AbstractNode) getModelObject().getRoot();
        AbstractNode parent = node;

        for (parent = (AbstractNode) parent.getParent(); parent != null && parent != root; parent = (AbstractNode) parent.getParent()) {
            expandNode(parent);
        }
    }

    @Override
    protected ResourceReference getFolderOpen() {
        if (isEnabled()) {
            return super.getFolderOpen();
        } else {
            return new PackageResourceReference(TaxonomyTree.class, "folder-open.gif");
        }
    }

    @Override
    protected ResourceReference getFolderClosed() {
        if (isEnabled()) {
            return super.getFolderClosed();
        } else {
            return new PackageResourceReference(TaxonomyTree.class, "folder-closed.gif");
        }
    }

    @Override
    protected ITreeState newTreeState() {
        return new DefaultTreeState() {
            private static final long serialVersionUID = 1L;

            @Override
            public void selectNode(Object node, boolean selected) {
                super.selectNode(node, true);
            }
        };
    }

    @Override
    protected MarkupContainer newLink(MarkupContainer parent, String id, final ILinkCallback callback) {
        MarkupContainer result = super.newLink(parent, id, new ILinkCallback() {
            private static final long serialVersionUID = 1L;

            public void onClick(AjaxRequestTarget target) {
                if (TaxonomyTree.this.isEnabled()) {
                    callback.onClick(target);
                }
            }

        });
        result.setEnabled(isEnabled());
        return result;
    }

    @Override
    protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode node) {
        if (isEnabled()) {
            ITreeState state = getTreeState();
            if (state.isNodeExpanded(node)) {
                // super has already switched selection.
                if (!state.isNodeSelected(node)) {
                    state.collapseNode(node);
                }
            } else {
                state.expandNode(node);
            }
        }
        super.onNodeLinkClicked(target, node);
    }

    @Override
    protected String renderNode(final TreeNode node, final int level) {
        if (node instanceof CategoryNode) {
            Category category = ((CategoryNode) node).getCategory();
            return TaxonomyHelper.getCategoryName(category, getLocale());
        } else if (node instanceof TaxonomyNode) {
            return ((TaxonomyNode) node).getTaxonomy().getName();
        }
        return "/";
    }

    @Override
    public Locale getLocale() {
        if (preferredLanguage != null) {
            try {
                return new Locale(preferredLanguage);
            } catch (Exception e) {
            }
        }

        return super.getLocale();
    }

    @Override
    protected Component newNodeIcon(final MarkupContainer parent, final String id, final TreeNode node) {
        if (treeNodeIconService != null) {
            return new CmsJcrTree.NodeIconContainer(id, node, this, treeNodeIconService);
        }
        return super.newNodeIcon(parent, id, node);
    }

    /**
     * code copied from org.hippoecm.frontend.plugins.cms.browse.tree.CmsJcrTree
     */
    @Override
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

                final Icon icon = node.isLeaf() ? Icon.BULLET :
                        isNodeExpanded(node) ? Icon.CARET_DOWN : Icon.CARET_RIGHT;
                final String cssClassOuter = isNodeLast(node) ? "junction-last" : "junction";

                final Response response = RequestCycle.get().getResponse();
                response.write("<span class=\"" + cssClassOuter + "\">");
                response.write(icon.getSpriteReference(IconSize.S));
                response.write("</span>");
            }

            private boolean isNodeLast(TreeNode node) {
                TreeNode parent = node.getParent();
                return parent == null || parent.getChildAt(parent.getChildCount() - 1).equals(node);
            }
        }.setRenderBodyOnly(true);
    }
}