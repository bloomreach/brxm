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
package org.hippocms.repository.webapp.menu.property;

import javax.jcr.RepositoryException;

import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableMultiLineLabel;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.PropertyModel;
import org.hippocms.repository.webapp.menu.AbstractDialog;
import org.hippocms.repository.webapp.menu.DialogWindow;
import org.hippocms.repository.webapp.model.JcrNodeModel;

public class PropertyDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    private String name;
    private String value;
    private Boolean isMultiple = Boolean.FALSE;

    public PropertyDialog(final DialogWindow dialogWindow, JcrNodeModel model) {
        super(dialogWindow, model);
        add(new CheckBox("isMultiple", new PropertyModel(this, "isMultiple")) {
            private static final long serialVersionUID = 1L;
            protected boolean wantOnSelectionChangedNotifications() {
                return true;
            }
            protected void onSelectionChanged(Object newSelection) {
                PropertyDialog.this.isMultiple = (Boolean)newSelection;
            }
        });
        add(new AjaxEditableLabel("name", new PropertyModel(this, "name")));
        add(new AjaxEditableMultiLineLabel("value", new PropertyModel(this, "value")));
        if (model.getNode() == null) {
            ok.setVisible(false);
        }
    }

    public void ok() throws RepositoryException {
        PropertyDialog page = (PropertyDialog) getPage();
        if (isMultiple.booleanValue()) {
            String value = page.getValue();
            if (value == null || value.equals("")) {
                value = "...";
            }
            model.getNode().setProperty(page.getName(), new String[] { value });
        } else {
            model.getNode().setProperty(page.getName(), page.getValue());
        }
    }

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
