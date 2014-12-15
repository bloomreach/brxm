/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tree.DefaultTreeState;
import org.apache.wicket.extensions.markup.html.tree.ITreeState;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.hippoecm.frontend.widgets.ContextMenuTree;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.plugin.api.TaxonomyHelper;

public class TaxonomyTree extends ContextMenuTree {
    private static final long serialVersionUID = 1L;

    private String preferredLanguage;

    public TaxonomyTree(String id, TreeModel model, String preferredLanguage) {
        super(id, model);
        this.preferredLanguage = preferredLanguage;
        getTreeState().expandNode(getModelObject().getRoot());
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
}