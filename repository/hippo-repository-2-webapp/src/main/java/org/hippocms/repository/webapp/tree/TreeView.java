package org.hippocms.repository.webapp.tree;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.apache.wicket.markup.html.tree.ITreeStateListener;
import org.hippocms.repository.webapp.Browser;
import org.hippocms.repository.webapp.editor.NodeEditor;
import org.hippocms.repository.webapp.model.JcrNodeModel;

public class TreeView extends Tree {
    private static final long serialVersionUID = 1L;

    public TreeView(String id, TreeModel model) {
        super(id, model);
    }

    protected String renderNode(TreeNode treeNode) {
        JcrNodeModel nodeModel = (JcrNodeModel) treeNode;
        Node node = nodeModel.getNode();
        String result = "null";
        if (node != null) {
            try {
                result = node.getName();
            } catch (RepositoryException e) {
                result = e.getMessage();
            }
        }
        return result;
    }

    public void addTreeStateListener(ITreeStateListener listener) {
        getTreeState().addTreeStateListener(listener);
    }
       
    protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode treeNode) {
        Browser browser = (Browser) findParent(Browser.class);
        NodeEditor editor = browser.getEditorPanel().getEditor();
        target.addComponent(editor);
    }

}
