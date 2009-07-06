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
package org.hippoecm.frontend.plugins.console.menu.property;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyDialog extends AbstractDialog {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(PropertyDialog.class);

    private String name;
    private String value;
    private Boolean isMultiple = Boolean.FALSE;
    private String type;
    private MenuPlugin plugin;

    public PropertyDialog(MenuPlugin plugin) {
        this.plugin = plugin;

        add(new CheckBox("isMultiple", new PropertyModel(this, "isMultiple")));

        type = PropertyType.TYPENAME_STRING;

        DropDownChoice ddChoice = new DropDownChoice("types", new PropertyModel(this, "propertyType"), getTypes());
        ddChoice.setRequired(true);
        add(ddChoice);

        add(new TextField("name", new PropertyModel(this, "name")));
        add(new TextArea("value", new PropertyModel(this, "value")));
    }

    @Override
    public void onOk() {
        try {
            JcrNodeModel nodeModel = (JcrNodeModel) plugin.getModel();
            Node node = nodeModel.getNode();

            Value jcrValue = getJcrValue();
            if (isMultiple.booleanValue()) {
                if (jcrValue == null || value == null || value.equals("")) {
                    jcrValue = ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue("...",
                            PropertyType.STRING);
                }
                node.setProperty(name, new Value[] { jcrValue });
            } else {
                node.setProperty(name, jcrValue);
            }

            JcrNodeModel newNodeModel = new JcrNodeModel(node);
            plugin.setModel(newNodeModel);
        } catch (RepositoryException ex) {
            error(ex.getMessage());
        }
    }

    public IModel getTitle() {
        return new Model("Add a new Property");
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
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue(value,
                        PropertyType.BOOLEAN);
            } else if (type.equals(PropertyType.TYPENAME_DATE)) {
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue(value,
                        PropertyType.DATE);
            } else if (type.equals(PropertyType.TYPENAME_DOUBLE)) {
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue(value,
                        PropertyType.DOUBLE);
            } else if (type.equals(PropertyType.TYPENAME_LONG)) {
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue(value,
                        PropertyType.LONG);
            } else if (type.equals(PropertyType.TYPENAME_NAME)) {
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue(value,
                        PropertyType.NAME);
            } else if (type.equals(PropertyType.TYPENAME_PATH)) {
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue(value,
                        PropertyType.PATH);
            } else if (type.equals(PropertyType.TYPENAME_REFERENCE)) {
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue(value,
                        PropertyType.REFERENCE);
            } else if (type.equals(PropertyType.TYPENAME_STRING)) {
                return ((UserSession) Session.get()).getJcrSession().getValueFactory().createValue(value,
                        PropertyType.STRING);
            }
            log.info("Unsupported type " + type);
            return null;
        } catch (RepositoryException ex) {
            log.info(ex.getMessage());
        }
        return null;
    }

}
