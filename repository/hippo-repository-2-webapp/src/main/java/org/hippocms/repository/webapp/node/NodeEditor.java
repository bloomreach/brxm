package org.hippocms.repository.webapp.node;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.swing.tree.TreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.tree.ITreeStateListener;
import org.hippocms.repository.webapp.model.JcrNodeModel;

public class NodeEditor extends Form implements ITreeStateListener {
    private static final long serialVersionUID = 1L;

    public NodeEditor(String id, JcrNodeModel model) {
        super(id, model);

        add(new PropertiesEditor("properties", model));

        final NewPropertyDialog newPropertyDialog = new NewPropertyDialog("dialog");
        add(newPropertyDialog);
        
        AjaxLink newPropertyDialogLink = new AjaxLink("new") {
            private static final long serialVersionUID = 1L;
            public void onClick(AjaxRequestTarget target) {
                newPropertyDialog.show(target);
            }
        };
        add(newPropertyDialogLink);
    }

    public Node getNode() {
        return (Node) getModelObject();
    }

    public void save() {
        try {
            Node node = (Node) getModelObject();
            node.save();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
    }
    
    // Components
    
//    private AjaxLink newPropertyLink(String id, final ModalWindow popup) {
//        return new AjaxLink(id) {
//            private static final long serialVersionUID = 1L;
//            public void onClick(AjaxRequestTarget target) {
//                popup.show(target);
//            }
//        };
//    }
    
//    private ModalWindow newPropertyDialog(String id) {
//        ModalWindow result = new ModalWindow(id);
//        
//        result.setContent(new NewPropertyDialogPanel(result.getContentId()));
//        result.setTitle("This is modal window with panel content.");
//        result.setCookieName("newPropertyDialog");
//
//        result.setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {
//            private static final long serialVersionUID = 1L;
//            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
//                // setResult("Modal window 2 - close button");
//                return true;
//            }
//        });
//        
//        result.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
//            private static final long serialVersionUID = 1L;
//
//            public void onClose(AjaxRequestTarget target) {
//                //  target.addComponent(result);
//            }
//        });
//        
//        return result;
//    }

    // ITreeStateListener

    public void nodeSelected(TreeNode treeNode) {
        if (treeNode != null) {
            JcrNodeModel treeNodeModel = (JcrNodeModel) treeNode;
            Node jcrNode = treeNodeModel.getNode();

            JcrNodeModel editorNodeModel = (JcrNodeModel) getModel();
            editorNodeModel.setNode(jcrNode);
        }
    }

    public void nodeUnselected(TreeNode treeNode) {
    }

    public void nodeCollapsed(TreeNode treeNode) {
    }

    public void nodeExpanded(TreeNode treeNode) {
    }

    public void allNodesCollapsed() {
    }

    public void allNodesExpanded() {
    }

}
