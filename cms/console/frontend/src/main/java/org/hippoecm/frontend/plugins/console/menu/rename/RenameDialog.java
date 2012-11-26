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
package org.hippoecm.frontend.plugins.console.menu.rename;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenameDialog extends AbstractDialog<Node> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(RenameDialog.class);

    private String name;
    private final IModelReference modelReference;

    public RenameDialog(IModelReference modelReference) {
        this.modelReference = modelReference;

        JcrNodeModel model = (JcrNodeModel) modelReference.getModel();
        try {
            // get name of current node
            name = model.getNode().getName();
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }

        add(setFocus(new TextField("name", new PropertyModel(this, "name"))));
        if (model.getNode() == null) {
            setOkVisible(false);
        }
    }

    @Override
    protected void onOk() {
        try {
            JcrNodeModel nodeModel = (JcrNodeModel) modelReference.getModel();

            if (nodeModel.getParentModel() != null) {
                JcrNodeModel parentModel = nodeModel.getParentModel();

                //The actual JCR move
                String oldPath = nodeModel.getNode().getPath();
                String newPath = parentModel.getNode().getPath();
                if (!newPath.endsWith("/")) {
                    newPath += "/";
                }
                newPath += getName();
                Session jcrSession = UserSession.get().getJcrSession();
                jcrSession.move(oldPath, newPath);

                JcrNodeModel newNodeModel = new JcrNodeModel(parentModel.getNode().getNode(getName()));
                modelReference.setModel(newNodeModel);
            }
        } catch (RepositoryException ex) {
            error(ex.getMessage());
        }
    }

    public IModel getTitle() {
        return new Model("Rename Node");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public IValueMap getProperties() {
        return DialogConstants.SMALL;
    }
}
