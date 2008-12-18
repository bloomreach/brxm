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

import javax.swing.tree.TreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.tree.ITreeState;
import org.hippoecm.frontend.model.IJcrNodeModelListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelService;
import org.hippoecm.frontend.model.tree.AbstractTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.DocumentListFilter;
import org.hippoecm.frontend.plugins.standards.FolderTreeNode;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.wicket1985.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderTreePlugin extends RenderPlugin implements IJcrNodeModelListener {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(FolderTreePlugin.class);

    protected Tree tree;
    protected JcrTreeModel treeModel;
    protected AbstractTreeNode rootNode;
    private JcrNodeModel rootModel;

    public FolderTreePlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        context.registerService(this, IJcrService.class.getName());

        String startingPath = config.getString("path", "/");
        rootModel = new JcrNodeModel(startingPath);

        ModelService modelService = new ModelService(context.getReference(this).getServiceId(), rootModel);
        modelService.init(context);

        DocumentListFilter folderTreeConfig = new DocumentListFilter(config);
        this.rootNode = new FolderTreeNode(rootModel, folderTreeConfig);

        treeModel = new JcrTreeModel(rootNode);
        tree = new CmsJcrTree("tree", treeModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode clickedNode) {
                AbstractTreeNode treeNodeModel = (AbstractTreeNode) clickedNode;
                FolderTreePlugin.this.setModel(treeNodeModel.getNodeModel());
                ITreeState state = getTreeState();
                if (state.isNodeExpanded(clickedNode)) {
                    // super has already switched selection.
                    if (!state.isNodeSelected(clickedNode)) {
                        state.collapseNode(clickedNode);
                    }
                } else {
                    state.expandNode(clickedNode);
                }
                redraw();
            }
        };
        add(tree);

        tree.setRootLess(config.getBoolean("rootless"));

        onModelChanged();
    }

    @Override
    public void onBeforeRender() {
        tree.detach();
        super.onBeforeRender();
    }

    public void onFlush(JcrNodeModel nodeModel) {
        AbstractTreeNode node = treeModel.lookup(nodeModel);
        if (node != null) {
            node.detach();
            treeModel.nodeStructureChanged(node);
            redraw();
        } else {
            rootNode.detach();
            treeModel.nodeStructureChanged(rootNode);
            redraw();
        }
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();

        JcrNodeModel model = (JcrNodeModel) getModel();
        AbstractTreeNode node = null;
        node = treeModel.lookup(model);
        if (node != null) {
            TreeNode parentNode = node.getParent();
            while (parentNode != null) {
                if (!tree.getTreeState().isNodeExpanded(parentNode)) {
                    tree.getTreeState().expandNode(parentNode);
                }
                parentNode = parentNode.getParent();
            }
            ITreeState state = tree.getTreeState();
            if (!state.isNodeSelected(node)) {
                tree.getTreeState().selectNode(node, true);
            }
            redraw();
        }
    }

}
