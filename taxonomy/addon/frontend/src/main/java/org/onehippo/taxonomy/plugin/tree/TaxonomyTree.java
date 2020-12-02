/*
 *  Copyright 2009-2019 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tree.DefaultTreeState;
import org.apache.wicket.extensions.markup.html.tree.ITreeState;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.tree.CmsJcrTree;
import org.hippoecm.frontend.plugins.cms.browse.tree.yui.WicketTreeHelperBehavior;
import org.hippoecm.frontend.plugins.cms.browse.tree.yui.WicketTreeHelperSettings;
import org.hippoecm.frontend.plugins.standards.tree.icon.ITreeNodeIconProvider;
import org.hippoecm.frontend.widgets.ContextMenuTree;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.plugin.api.TaxonomyHelper;

public class TaxonomyTree extends ContextMenuTree {

    private Locale preferredLocale;
    private final ITreeNodeIconProvider treeNodeIconService;
    private final WicketTreeHelperBehavior treeHelperBehavior;

    public TaxonomyTree(String id, TreeModel model, Locale preferredLocale, ITreeNodeIconProvider treeNodeIconService) {
        super(id, model);
        this.preferredLocale = preferredLocale;
        this.treeNodeIconService = treeNodeIconService;
        this.treeHelperBehavior = createTreeHelperWithoutWorkflow();

        add(treeHelperBehavior);

        final AbstractNode rootNode = (AbstractNode) getModelObject().getRoot();
        expandNode(rootNode);
        getTreeState().selectNode(rootNode, true);
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
    protected ITreeState newTreeState() {
        return new DefaultTreeState() {
            @Override
            public void selectNode(Object node, boolean selected) {
                super.selectNode(node, true);
            }
        };
    }

    @Override
    protected MarkupContainer newLink(MarkupContainer parent, String id, final ILinkCallback callback) {
        MarkupContainer link = super.newLink(parent, id, (ILinkCallback) target -> {
            if (TaxonomyTree.this.isEnabled()) {
                callback.onClick(target);
            }
        });

        parent.add(ClassAttribute.append(() -> {
            final Object object = parent.getDefaultModelObject();
            if (!(object instanceof CategoryNode)) {
                return StringUtils.EMPTY;
            }

            final CategoryNode categoryNode = (CategoryNode) object;
            final CategoryState categoryState = getCategoryState(categoryNode.getCategory());
            if (categoryState == CategoryState.DISABLED) {
                return "taxonomy-item-disabled";
            }

            if (categoryState == CategoryState.HIDDEN) {
                return "taxonomy-item-hidden";
            }

            return StringUtils.EMPTY;
        }));

        link.setEnabled(isEnabled());
        return link;
    }

    protected CategoryState getCategoryState(final Category category) {
        return CategoryState.VISIBLE;
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
        return preferredLocale;
    }

    @Override
    protected Component newNodeIcon(final MarkupContainer parent, final String id, final TreeNode node) {
        if (treeNodeIconService != null) {
            return new CmsJcrTree.NodeIconContainer(id, node, this, treeNodeIconService);
        }
        return super.newNodeIcon(parent, id, node);
    }

    @Override
    protected MarkupContainer newJunctionImage(final MarkupContainer parent, final String id, final TreeNode node) {
        return new CmsJcrTree.CaretJunctionImage(id, node) {
            @Override
            public boolean isExpanded(final TreeNode node) {
                return isNodeExpanded(node);
            }
        };
    }

    @Override
    public void onTargetRespond(final AjaxRequestTarget target, boolean dirty) {
        if (dirty) {
            target.appendJavaScript(treeHelperBehavior.getRenderString());
        }
    }

    private WicketTreeHelperBehavior createTreeHelperWithoutWorkflow() {
        final IPluginConfig treeHelperDummyConfig = new JavaPluginConfig();
        treeHelperDummyConfig.put("workflow.enabled", false);

        return new WicketTreeHelperBehavior(new WicketTreeHelperSettings(treeHelperDummyConfig));
    }
}
