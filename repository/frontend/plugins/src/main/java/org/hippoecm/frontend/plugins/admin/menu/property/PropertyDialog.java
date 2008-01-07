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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.value.BooleanValue;
import org.apache.jackrabbit.value.DateValue;
import org.apache.jackrabbit.value.DoubleValue;
import org.apache.jackrabbit.value.LongValue;
import org.apache.jackrabbit.value.NameValue;
import org.apache.jackrabbit.value.PathValue;
import org.apache.jackrabbit.value.ReferenceValue;
import org.apache.jackrabbit.value.StringValue;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.PropertyModel;

import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.Request;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(PropertyDialog.class);

    private String name;
    private String value;
    private Boolean isMultiple = Boolean.FALSE;
    private String type;

    public PropertyDialog(DialogWindow dialogWindow, Channel channel) {
        super(dialogWindow, channel);
        dialogWindow.setTitle("Add a new Property");

        add(new CheckBox("isMultiple", new PropertyModel(this, "isMultiple")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean wantOnSelectionChangedNotifications() {
                return true;
            }

            @Override
            protected void onSelectionChanged(Object newSelection) {
                setMultiple((Boolean) newSelection);
            }
        });

        type = PropertyType.TYPENAME_STRING;
        add(new DropDownChoice("types", new PropertyModel(this, "propertyType"), getTypes()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean wantOnSelectionChangedNotifications() {
                return true;
            }

            @Override
            protected void onSelectionChanged(Object newSelection) {
                setPropertyType((String) newSelection);
            }
        }.setRequired(true));

        add(new TextFieldWidget("name", new PropertyModel(this, "name")));
        add(new TextAreaWidget("value", new PropertyModel(this, "value")));
        if (dialogWindow.getNodeModel().getNode() == null) {
            ok.setVisible(false);
        }
    }

    @Override
    public void ok() throws RepositoryException {
        JcrNodeModel nodeModel = dialogWindow.getNodeModel();

        Value jcrValue = getJcrValue();
        if (isMultiple.booleanValue()) {
            if (jcrValue == null || value.equals("")) {
                jcrValue = new StringValue("...");
            }
            nodeModel.getNode().setProperty(name, new Value[] { jcrValue });
        } else {
            nodeModel.getNode().setProperty(name, jcrValue);
        }

        Channel channel = getIncoming();
        if (channel != null) {
            Request request = channel.createRequest("select", nodeModel.getMapRepresentation());
            channel.send(request);
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

    public void setPropertyType(String type) {
        this.type = type;
    }

    public String getPropertyType() {
        return type;
    }

    private static List<String> getTypes() {
        List<String> types = new ArrayList<String>(8);
        types.add(PropertyType.TYPENAME_BOOLEAN);
        types.add(PropertyType.TYPENAME_DATE);
        types.add(PropertyType.TYPENAME_DOUBLE);
        types.add(PropertyType.TYPENAME_LONG);
        types.add(PropertyType.TYPENAME_NAME);
        types.add(PropertyType.TYPENAME_PATH);
        types.add(PropertyType.TYPENAME_REFERENCE);
        types.add(PropertyType.TYPENAME_STRING);
        return types;
    }

    private Value getJcrValue() {
        try {
            if (type.equals(PropertyType.TYPENAME_BOOLEAN)) {
                return BooleanValue.valueOf(value);
            } else if (type.equals(PropertyType.TYPENAME_DATE)) {
                return DateValue.valueOf(value);
            } else if (type.equals(PropertyType.TYPENAME_DOUBLE)) {
                return DoubleValue.valueOf(value);
            } else if (type.equals(PropertyType.TYPENAME_LONG)) {
                return LongValue.valueOf(value);
            } else if (type.equals(PropertyType.TYPENAME_NAME)) {
                return NameValue.valueOf(value);
            } else if (type.equals(PropertyType.TYPENAME_PATH)) {
                return PathValue.valueOf(value);
            } else if (type.equals(PropertyType.TYPENAME_REFERENCE)) {
                return ReferenceValue.valueOf(value);
            } else if (type.equals(PropertyType.TYPENAME_STRING)) {
                return new StringValue(value);
            }
            log.info("Unsupported type " + type);
            return null;
        } catch (ValueFormatException ex) {
            log.info(ex.getMessage());
        }
        return null;
    }
}
