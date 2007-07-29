package org.hippocms.repository.plugins.admin.menu.move;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippocms.repository.frontend.model.JcrNodeModel;
import org.hippocms.repository.frontend.tree.JcrTree;

public class MoveTargetTreeView extends JcrTree {
    private static final long serialVersionUID = 1L;
    private TreeNode selectedNode;
    private MoveDialog dialog;

    public MoveTargetTreeView(String id, TreeModel treeModel, MoveDialog dialog) {
        super(id, treeModel);
        this.dialog = dialog;
    }

    protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode treeNode) {
        selectedNode = treeNode;
        dialog.update(target, (JcrNodeModel) treeNode);
    }

    public TreeNode getSelectedNode() {
        return selectedNode;
    }
}
