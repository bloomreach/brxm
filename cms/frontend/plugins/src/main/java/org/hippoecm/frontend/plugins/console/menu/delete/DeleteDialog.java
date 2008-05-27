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
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;
import org.hippoecm.frontend.sa.dialog.AbstractDialog;
import org.hippoecm.frontend.sa.dialog.IDialogService;
import org.hippoecm.frontend.sa.plugin.IPluginContext;

public class DeleteDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    private MenuPlugin plugin;
    
    public DeleteDialog(MenuPlugin plugin, IPluginContext context, IDialogService dialogWindow) {
        super(context, dialogWindow);    
        this.plugin = plugin;
        add(new Label("message", getTitle()));
    }

    @Override
    public void ok() throws RepositoryException {
        JcrNodeModel nodeModel = (JcrNodeModel)plugin.getModel();
        JcrNodeModel parentModel = nodeModel.getParentModel();

        //The actual JCR remove
        nodeModel.getNode().remove();
        
        //set the parent model as current model
        plugin.setModel(parentModel);
        
//        Channel channel = getChannel();
//        if (channel != null) {
//            Request request = channel.createRequest("flush", parentModel.findRootModel());
//            channel.send(request);
//            request = channel.createRequest("select", parentModel);
//            channel.send(request);
//        }
    }

    @Override
    public void cancel() {
    }

    public String getTitle() {
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
