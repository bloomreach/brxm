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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.DefaultCssAutocompleteTextField;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyDialog extends AbstractDialog<Node> {

    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(PropertyDialog.class);
    private static final List<String> ALL_TYPES = new ArrayList<String>(8);
    static {
        ALL_TYPES.add(PropertyType.TYPENAME_BOOLEAN);
        ALL_TYPES.add(PropertyType.TYPENAME_DATE);
        ALL_TYPES.add(PropertyType.TYPENAME_DOUBLE);
        ALL_TYPES.add(PropertyType.TYPENAME_LONG);
        ALL_TYPES.add(PropertyType.TYPENAME_NAME);
        ALL_TYPES.add(PropertyType.TYPENAME_PATH);
        ALL_TYPES.add(PropertyType.TYPENAME_REFERENCE);
        ALL_TYPES.add(PropertyType.TYPENAME_STRING);
        ALL_TYPES.add(PropertyType.TYPENAME_DECIMAL);
        ALL_TYPES.add(PropertyType.TYPENAME_URI);
        ALL_TYPES.add(PropertyType.TYPENAME_WEAKREFERENCE);

    }

    private String name;
    private String value;
    private Boolean isMultiple = Boolean.FALSE;
    private String type = PropertyType.TYPENAME_STRING;
    private MenuPlugin plugin;

    public PropertyDialog(MenuPlugin plugin) {
        this.plugin = plugin;

        // list defined properties for automatic completion
        final Map<String, PropertyDefinition> choices = new HashMap<String, PropertyDefinition>();
        Node node = ((JcrNodeModel) plugin.getDefaultModel()).getNode();
        try {
            NodeType pnt = node.getPrimaryNodeType();
            for (PropertyDefinition pd : pnt.getPropertyDefinitions()) {
                choices.put(pd.getName(), pd);
            }
            for (NodeType nt : node.getMixinNodeTypes()) {
                for (PropertyDefinition pd : nt.getPropertyDefinitions()) {
                    choices.put(pd.getName(), pd);
                }
            }
        } catch (RepositoryException e) {
            log.warn("Unable to populate autocomplete list for property names", e);
        }

        // checkbox for property ismultiple
        final CheckBox checkBox = new CheckBox("isMultiple", new Model<Boolean>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void setObject(Boolean multiple) {
                PropertyDialog.this.isMultiple = multiple;
            }

            @Override
            public Boolean getObject() {
                if (PropertyDialog.this.name != null) {
                    PropertyDefinition pd = choices.get(PropertyDialog.this.name);
                    if (pd != null) {
                        // somehow need to set isMultiple here, otherwise it doesn't get picked up...
                        PropertyDialog.this.isMultiple = pd.isMultiple();
                        return pd.isMultiple();
                    }
                }
                return PropertyDialog.this.isMultiple;
            }
        });
        checkBox.setOutputMarkupId(true);
        checkBox.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = 1L;
            @Override
            protected void onUpdate(AjaxRequestTarget target) {}
        });
        add(checkBox);

        // dropdown for property type
        final DropDownChoice<String> ddChoice = new DropDownChoice<String>("types", new PropertyModel<String>(this, "propertyType"), 
                new AbstractReadOnlyModel<List<? extends String>>() {
            private static final long serialVersionUID = 1L;

            @Override
            public List<? extends String> getObject() {
                if (PropertyDialog.this.name != null) {
                    PropertyDefinition pd = choices.get(PropertyDialog.this.name);
                    if (pd != null) {
                        List<String> result = new ArrayList<String>(1);
                        result.add(PropertyType.nameFromValue(pd.getRequiredType()));
                        return result;
                    }
                }
                return ALL_TYPES;
            }
        });
        ddChoice.setRequired(true);
        ddChoice.setOutputMarkupId(true);
        ddChoice.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = 1L;
            @Override
            protected void onUpdate(AjaxRequestTarget target) {}
        });
        add(ddChoice);

        // text field for property name
        AutoCompleteSettings settings = new AutoCompleteSettings();
        settings.setAdjustInputWidth(false);
        settings.setUseSmartPositioning(true);
        settings.setShowCompleteListOnFocusGain(true);

        final AutoCompleteTextField<String> nameField = new AutoCompleteTextField<String>("name",
                new PropertyModel<String>(this, "name"), settings) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Iterator<String> getChoices(String input) {
                if (Strings.isEmpty(input)) {
                    return Collections.EMPTY_LIST.iterator();
                }
                List<String> result = new ArrayList<String>();
                for (String propName : choices.keySet()) {
                    if (propName.startsWith(input)) {
                        result.add(propName);
                    }
                }
                return result.iterator();
            }
        };
        nameField.add(CSSPackageResource.getHeaderContribution(DefaultCssAutocompleteTextField.class,
                "DefaultCssAutocompleteTextField.css"));

        // dynamic update of related components when name is updated
        nameField.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.addComponent(ddChoice);
                target.addComponent(checkBox);
            }
        });

        add(setFocus(nameField));
        add(new TextArea<String>("value", new PropertyModel<String>(this, "value")));
    }

    @Override
    public void onOk() {
        try {
            JcrNodeModel nodeModel = (JcrNodeModel) plugin.getDefaultModel();
            Node node = nodeModel.getNode();

            Value jcrValue = getJcrValue();
            if (isMultiple.booleanValue()) {
                if (jcrValue == null || value == null || value.equals("")) {
                    jcrValue = getValueFactory().createValue("...", PropertyType.STRING);
                }
                node.setProperty(name, new Value[] { jcrValue });
            } else {
                node.setProperty(name, jcrValue);
            }

            JcrNodeModel newNodeModel = new JcrNodeModel(node);
            plugin.setDefaultModel(newNodeModel);
        } catch (RepositoryException ex) {
            error(ex.toString());
        }
    }

    public IModel<String> getTitle() {
        return new Model<String>("Add a new Property");
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

    private Value getJcrValue() {
        try {
            return getValueFactory().createValue(value, PropertyType.valueFromName(type));
        } catch (RepositoryException ex) {
            log.info(ex.getMessage());
        }
        return null;
    }

    private ValueFactory getValueFactory() throws UnsupportedRepositoryOperationException, RepositoryException {
        return ((UserSession) Session.get()).getJcrSession().getValueFactory();
    }

    @Override
    public IValueMap getProperties() {
        return SMALL;
    }

}
