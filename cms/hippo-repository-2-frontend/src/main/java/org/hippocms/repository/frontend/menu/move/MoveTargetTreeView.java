package org.hippocms.repository.frontend.menu.move;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippocms.repository.frontend.model.JcrNodeModel;
import org.hippocms.repository.frontend.tree.TreeView;
import org.hippocms.repository.frontend.update.IUpdatable;

public class MoveTargetTreeView extends TreeView {
    private static final long serialVersionUID = 1L;
    private TreeNode selectedNode;
    private IUpdatable updatable;

    public MoveTargetTreeView(String id, TreeModel treeModel, IUpdatable updatable) {
        super(id, treeModel);
        this.updatable = updatable;
    }

    protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode treeNode) {
        selectedNode = treeNode;
        updatable.update(target, (JcrNodeModel) treeNode);
    }

    /**
     * @return the selectedNode
     */
    public TreeNode getSelectedNode() {
        return selectedNode;
    }
}
