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
package org.hippoecm.frontend.plugins.console.menu.delete;

import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;

public class DeleteDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    private IServiceReference<MenuPlugin> pluginRef;
    
    public DeleteDialog(MenuPlugin plugin, IPluginContext context, IDialogService dialogWindow) {
        super(context, dialogWindow);    
        this.pluginRef = context.getReference(plugin);
        add(new Label("message", getTitle()));
    }

    @Override
    public void ok() throws RepositoryException {
        MenuPlugin plugin = pluginRef.getService();
        JcrNodeModel nodeModel = (JcrNodeModel)plugin.getModel();
        JcrNodeModel parentModel = nodeModel.getParentModel();

        //The actual JCR remove
        nodeModel.getNode().remove();
        
        //set the parent model as current model
        plugin.setModel(parentModel);
        
        //flush the JCR tree
        plugin.flushNodeModel(parentModel.findRootModel());
    }

    @Override
    public void cancel() {
    }

    public String getTitle() {
        MenuPlugin plugin = pluginRef.getService();
        JcrNodeModel nodeModel = (JcrNodeModel)plugin.getModel();
        String title;
        try {
            title = "Delete " + nodeModel.getNode().getPath();
        } catch (RepositoryException e) {
            title = e.getMessage();
        }
        return title;
    }

}
