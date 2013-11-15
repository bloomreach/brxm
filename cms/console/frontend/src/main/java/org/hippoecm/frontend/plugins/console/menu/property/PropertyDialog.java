/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.DefaultCssAutoCompleteTextField;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyDialog extends AbstractDialog<Node> {

    
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

    private String name = "";
    private String value = "";
    private Boolean isMultiple = Boolean.FALSE;
    private String type = PropertyType.TYPENAME_STRING;
    private IModel<Map<String, List<PropertyDefinition>>> choiceModel;
    private final IModelReference modelReference;

    public PropertyDialog(IModelReference modelReference) {
        this.modelReference = modelReference;
        final JcrNodeModel model = (JcrNodeModel) modelReference.getModel();

        // list defined properties for automatic completion
        choiceModel = new LoadableDetachableModel<Map<String, List<PropertyDefinition>>>() {
            private static final long serialVersionUID = 1L;

            protected Map<String, List<PropertyDefinition>> load() {
                Map<String, List<PropertyDefinition>> choices = new HashMap<String, List<PropertyDefinition>>();
                Node node = model.getNode();
                try {
                    NodeType pnt = node.getPrimaryNodeType();
                    for (PropertyDefinition pd : pnt.getPropertyDefinitions()) {
                        List<PropertyDefinition> list = choices.get(pd.getName());
                        if (list == null) {
                            list = new ArrayList<PropertyDefinition>(5);
                        }
                        list.add(pd);
                        choices.put(pd.getName(), list);
                    }
                    for (NodeType nt : node.getMixinNodeTypes()) {
                        for (PropertyDefinition pd : nt.getPropertyDefinitions()) {
                            List<PropertyDefinition> list = choices.get(pd.getName());
                            if (list == null) {
                                list = new ArrayList<PropertyDefinition>(5);
                            }
                            list.add(pd);
                            choices.put(pd.getName(), list);
                        }
                    }
                } catch (RepositoryException e) {
                    log.warn("Unable to populate autocomplete list for property names", e);
                }
                return choices;
            }
        };

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
                    List<PropertyDefinition> propdefs = choiceModel.getObject().get(PropertyDialog.this.name);
                    if (propdefs != null) {
                        for (PropertyDefinition pd : propdefs) {
                            if (PropertyType.nameFromValue(pd.getRequiredType()).equals(type)) {
                                // somehow need to set isMultiple here, otherwise it doesn't get picked up...
                                PropertyDialog.this.isMultiple = pd.isMultiple();
                                return pd.isMultiple();
                            }
                        }
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
        final DropDownChoice<String> ddChoice = new DropDownChoice<String>("types") {
            private static final long serialVersionUID = 1L;
            @Override
            public List<? extends String> getChoices() {
                if (PropertyDialog.this.name != null) {
                    List<PropertyDefinition> propdefs = choiceModel.getObject().get(PropertyDialog.this.name);
                    if (propdefs != null) {
                        List<String> result = new ArrayList<String>(propdefs.size());
                        for (PropertyDefinition pd : propdefs) {
                            result.add(PropertyType.nameFromValue(pd.getRequiredType()));
                        }
                        return result;
                    }
                }
                return ALL_TYPES;

            }
        };
        ddChoice.setModel(new Model<String>() {
            private static final long serialVersionUID = 1L;
            @Override
            public void setObject(String object) {
                type = object;
            }
            
            @Override
            public String getObject() {
                List<? extends String> choices = ddChoice.getChoices();
                if (choices.size() == 1) {
                    type = choices.iterator().next();
                }
                return type;
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
        settings.setShowListOnEmptyInput(true);

        final AutoCompleteTextField<String> nameField = new AutoCompleteTextField<String>("name",
                new PropertyModel<String>(this, "name"), settings) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Iterator<String> getChoices(String input) {
                List<String> result = new ArrayList<String>();
                for (String propName : choiceModel.getObject().keySet()) {
                    if (propName.startsWith(input)) {
                        result.add(propName);
                    }
                }
                return result.iterator();
            }

            @Override
            public void renderHead(final IHeaderResponse response) {
                super.renderHead(response);
                response.render(CssHeaderItem.forReference(new CssResourceReference(
                        DefaultCssAutoCompleteTextField.class, "DefaultCssAutoCompleteTextField.css")));
            }
        };

        // dynamic update of related components when name is updated
        nameField.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(ddChoice);
                target.add(checkBox);
            }
        });

        add(setFocus(nameField));
        add(new TextArea<String>("value", new PropertyModel<String>(this, "value")));
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(new CssResourceReference(DefaultCssAutoCompleteTextField.class,
                "DefaultCssAutocompleteTextField.css")));
    }

    @Override
    public void onOk() {
        try {
            JcrNodeModel nodeModel = (JcrNodeModel) modelReference.getModel();
            Node node = nodeModel.getNode();

            final int propertyType = PropertyType.valueFromName(type);
            final Value value = getJcrValue(propertyType);
            if (isMultiple) {
                Value[] values = value != null ? new Value[] { value } : new Value[] {};
                node.setProperty(name, values, propertyType);
            } else {
                node.setProperty(name, value, propertyType);
            }
            JcrNodeModel newNodeModel = new JcrNodeModel(node);
            modelReference.setModel(newNodeModel);
        } catch (RepositoryException e) {
            error(e.toString());
            log.error(e.getClass().getName() + " : " + e.getMessage(), e);
        }
    }

    public IModel<String> getTitle() {
        return new Model<String>("Add a new Property");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null)
            name = "";
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        if (value == null)
            value = "";
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

    private Value getJcrValue(final int propertyType) {
        try {
            return getValueFactory().createValue(value, propertyType);
        } catch (RepositoryException ex) {
            log.info(ex.getMessage());
        }
        return null;
    }

    private ValueFactory getValueFactory() throws RepositoryException {
        return UserSession.get().getJcrSession().getValueFactory();
    }

    @Override
    public IValueMap getProperties() {
        return DialogConstants.SMALL;
    }

    @Override
    protected void onDetach() {
        choiceModel.detach();
        super.onDetach();
    }

}
