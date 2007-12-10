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
package org.hippoecm.frontend.plugins.admin.menu.property;

import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.JcrEvent;
import org.hippoecm.frontend.plugin.PluginEvent;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.hippoecm.frontend.widgets.TextFieldWidget;

public class PropertyDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    private String name;
    private String value;
    private Boolean isMultiple = Boolean.FALSE;

    public PropertyDialog(DialogWindow dialogWindow) {
        super(dialogWindow);
        dialogWindow.setTitle("Add a new Property");
        
        add(new CheckBox("isMultiple", new PropertyModel(this, "isMultiple")) {
            private static final long serialVersionUID = 1L;
            @Override
            protected boolean wantOnSelectionChangedNotifications() {
                return true;
            }
            @Override
            protected void onSelectionChanged(Object newSelection) {
                setMultiple((Boolean)newSelection);
            }
        });
        add(new TextFieldWidget("name", new PropertyModel(this, "name")));
        add(new TextAreaWidget("value", new PropertyModel(this, "value")));
        if (dialogWindow.getNodeModel().getNode() == null) {
            ok.setVisible(false);
        }
    }

    @Override
    public PluginEvent ok() throws RepositoryException {
        JcrNodeModel nodeModel = dialogWindow.getNodeModel();
        if (isMultiple.booleanValue()) {
            if (value == null || value.equals("")) {
                value = "...";
            }
            nodeModel.getNode().setProperty(name, new String[] { value });
        } else {
            nodeModel.getNode().setProperty(name, value);
        }
        return new PluginEvent(getOwningPlugin(), JcrEvent.NEW_MODEL, nodeModel);
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setMultiple(Boolean isMultiple) {
        this.isMultiple = isMultiple;
    }

    public Boolean isMultiple() {
        return isMultiple;
    }

}
