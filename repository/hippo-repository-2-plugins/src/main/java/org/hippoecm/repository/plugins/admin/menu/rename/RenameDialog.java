/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.plugins.admin.menu.rename;

import javax.jcr.RepositoryException;

import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.repository.frontend.dialog.AbstractDialog;
import org.hippoecm.repository.frontend.dialog.DialogWindow;
import org.hippoecm.repository.frontend.model.JcrNodeModel;

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
