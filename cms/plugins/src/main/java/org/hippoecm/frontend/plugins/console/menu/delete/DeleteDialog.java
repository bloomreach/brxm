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
package org.hippoecm.frontend.plugins.console.menu.delete;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.IModelReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;

import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;

public class DeleteDialog extends AbstractDialog<Node> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(DeleteDialog.class);
    private final IModelReference modelReference;

    public DeleteDialog(IModelReference modelReference) {
        this.modelReference = modelReference;
        JcrNodeModel model = (JcrNodeModel) modelReference.getModel();
        setModel(model);

        String path;
        try {
            path = model.getNode().getPath();
        } catch (RepositoryException e) {
            path = e.getMessage();
        }
        add(new Label("message", new StringResourceModel("delete.message", this, null, new Object[] {path})));
        
        setFocusOnOk();
    }

    @Override
    public void onOk() {
        try {
            JcrNodeModel nodeModel = (JcrNodeModel) getModel();
            JcrNodeModel parentModel = nodeModel.getParentModel();
            
            //The actual JCR remove
            nodeModel.getNode().remove();

            //set the parent model as current model
            modelReference.setModel(parentModel);
        } catch (RepositoryException ex) {
            log.error("Error while deleting document", ex);
            error("Error while deleting document " + ex.getMessage());
        }
    }

    public IModel getTitle() {
        return new Model(getString("dialog.title"));
    }
    
    @Override
    public IValueMap getProperties() {
        return SMALL;
    }

}
