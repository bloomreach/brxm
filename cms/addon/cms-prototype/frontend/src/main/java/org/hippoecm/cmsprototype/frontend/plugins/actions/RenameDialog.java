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
package org.hippoecm.cmsprototype.frontend.plugins.actions;

import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.cmsprototype.frontend.model.content.Document;
import org.hippoecm.cmsprototype.frontend.model.content.Folder;
import org.hippoecm.cmsprototype.frontend.model.exception.ModelWrapException;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.HippoSession;

public class RenameDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    private String name;
    
    public RenameDialog(DialogWindow dialogWindow, Channel channel) {
        super(dialogWindow, channel);
        String title;
        try {
            Document document = new Document(dialogWindow.getNodeModel());
            name = document.getName();
            title = "Rename Document " + name;
        } catch (ModelWrapException e) {
            try {
                Folder folder = new Folder(dialogWindow.getNodeModel());
                name = folder.getName();
                title = "Rename Folder " + name;
            } catch (ModelWrapException e1) {
                title = e.getMessage();
            }
        }
        add(new TextFieldWidget("name", new PropertyModel(this, "name")));
        dialogWindow.setTitle(title);
    }

    @Override
    public void ok() throws RepositoryException {
        if (dialogWindow.getNodeModel().getParentModel() != null) {
            JcrNodeModel toBeRenamed = null;
            try {
                toBeRenamed = new Document(dialogWindow.getNodeModel()).getNodeModel();
            } catch (ModelWrapException e) {
                try {
                    toBeRenamed = new Folder(dialogWindow.getNodeModel()).getNodeModel();
                } catch (ModelWrapException e1) {
                    //Node to be deleted isn't a Document or a Folder
                }
            }
            
            if (toBeRenamed != null) {                
                JcrNodeModel parent = toBeRenamed.findRootModel();
                try {
                    parent = new Document(toBeRenamed.getParentModel()).getNodeModel();
                } catch (ModelWrapException e) {
                    try {
                        parent = new Folder(toBeRenamed.getParentModel()).getNodeModel();
                    } catch (ModelWrapException e1) {
                        //Parent isn't a Document or a Folder,
                        //fall back to root node
                    }
                }
                
                UserSession wicketSession = (UserSession) getSession();
                HippoSession jcrSession = (HippoSession) wicketSession.getJcrSession();
                
               
                jcrSession.save();

                if (channel != null) {
                    Request request = channel.createRequest("select", parent);
                    channel.send(request);

                    request = channel.createRequest("flush", parent.findRootModel());
                    channel.send(request);
                }
            }
        }
    }

    @Override
    public void cancel() {
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
