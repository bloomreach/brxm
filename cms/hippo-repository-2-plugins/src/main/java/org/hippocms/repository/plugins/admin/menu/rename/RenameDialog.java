package org.hippocms.repository.plugins.admin.menu.rename;

import javax.jcr.RepositoryException;

import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.model.PropertyModel;
import org.hippocms.repository.frontend.dialog.AbstractDialog;
import org.hippocms.repository.frontend.dialog.DialogWindow;
import org.hippocms.repository.frontend.model.JcrNodeModel;

public class RenameDialog extends AbstractDialog {

    private static final long serialVersionUID = 1L;

    /**
     * The name of the current node represented in the dialog
     */
    private String name;

    public RenameDialog(DialogWindow dialogWindow, JcrNodeModel model) {
        super(dialogWindow, model);
        dialogWindow.setTitle("Rename Node");
        
        try {
            // get name of current node
            name = model.getNode().getName();
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        add(new AjaxEditableLabel("name", new PropertyModel(this, "name")));
        if (model.getNode() == null) {
            ok.setVisible(false);
        }
    }

    @Override
    protected void cancel() {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void ok() throws RepositoryException {
        if (model.getNode() != null) {
            RenameDialog page = (RenameDialog) getPage();
            String parentPath = model.getNode().getParent().getPath();
            String destination = parentPath + "/" + page.getName();
            model.getNode().getSession().move(model.getNode().getPath(), destination);
        }
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    
    
}
