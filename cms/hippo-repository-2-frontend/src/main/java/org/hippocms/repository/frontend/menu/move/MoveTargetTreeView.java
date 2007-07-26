package org.hippocms.repository.frontend.menu.move;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippocms.repository.frontend.tree.TreeView;

public class MoveTargetTreeView extends TreeView {
	private static final long serialVersionUID = 1L;
	private TreeNode selectedNode;


	public MoveTargetTreeView(String id, TreeModel treeModel) {
        super(id, treeModel);
	}
	
    protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode treeNode) {
    	System.out.println("onNodeLinkClicked");
    	selectedNode = treeNode;
    }

	/**
	 * @return the selectedNode
	 */
	public TreeNode getSelectedNode() {
		return selectedNode;
	}

}
