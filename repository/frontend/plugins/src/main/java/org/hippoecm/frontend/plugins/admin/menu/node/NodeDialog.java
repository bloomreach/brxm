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
package org.hippoecm.frontend.plugins.admin.menu.node;

import javax.jcr.RepositoryException;

import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.JcrEvent;
import org.hippoecm.frontend.plugin.PluginEvent;
import org.hippoecm.frontend.widgets.TextFieldWidget;

public class NodeDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    private String name;
    private String type = "nt:unstructured";

    public NodeDialog(DialogWindow dialogWindow) {
        super(dialogWindow);
        dialogWindow.setTitle("Add a new Node");

        add(new TextFieldWidget("name", new PropertyModel(this, "name")));
        add(new TextFieldWidget("type", new PropertyModel(this, "type")));
        if (dialogWindow.getNodeModel().getNode() == null) {
            ok.setVisible(false);
        }
    }

    @Override
    public PluginEvent ok() throws RepositoryException {
        JcrNodeModel nodeModel = dialogWindow.getNodeModel();
        
        //The actual JCR add node
        nodeModel.getNode().addNode(getName(), getType());
        
        PluginEvent result = new PluginEvent(getOwningPlugin(), JcrEvent.NEW_MODEL, nodeModel);
        result.chainEvent(JcrEvent.NEEDS_RELOAD, nodeModel);
        return result;
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

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

}
