/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.DefaultCssAutoCompleteTextField;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.console.editor.PropertiesEditor;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

public class PropertyDialog extends AbstractDialog<Node> {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PropertyDialog.class);

    private static final List<String> ALL_TYPES = new ArrayList<>(8);
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

    @SuppressWarnings("unused")
    private String name;
    private String type = PropertyType.TYPENAME_STRING;
    private Boolean isMultiple = Boolean.FALSE;
    private List<String> values;
    private IModel<Map<String, List<PropertyDefinition>>> choiceModel;
    private final IModelReference<Node> modelReference;
    
    private boolean focusOnLatestValue;

    public PropertyDialog(IModelReference<Node> modelReference) {
        this.modelReference = modelReference;
        final IModel<Node> model = modelReference.getModel();
        
        getParent().add(new AttributeAppender("class", Model.of("property-dialog"), " "));

        // list defined properties for automatic completion
        choiceModel = new LoadableDetachableModel<Map<String, List<PropertyDefinition>>>() {
            private static final long serialVersionUID = 1L;

            protected Map<String, List<PropertyDefinition>> load() {
                Map<String, List<PropertyDefinition>> choices = new HashMap<>();
                Node node = model.getObject();
                try {
                    NodeType pnt = node.getPrimaryNodeType();
                    for (PropertyDefinition pd : pnt.getPropertyDefinitions()) {
                        List<PropertyDefinition> list = choices.get(pd.getName());
                        if (list == null) {
                            list = new ArrayList<>(5);
                        }
                        list.add(pd);
                        choices.put(pd.getName(), list);
                    }
                    for (NodeType nt : node.getMixinNodeTypes()) {
                        for (PropertyDefinition pd : nt.getPropertyDefinitions()) {
                            List<PropertyDefinition> list = choices.get(pd.getName());
                            if (list == null) {
                                list = new ArrayList<>(5);
                            }
                            list.add(pd);
                            choices.put(pd.getName(), list);
                        }
                    }
                    // remove already set properties from suggestions:
                    final Set<String> properties = new HashSet<>(choices.keySet());
                    for (String property : properties) {
                        if (node.hasProperty(property)) {
                            choices.remove(property);
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
        add(checkBox);

        // dropdown for property type
        final DropDownChoice<String> ddChoice = new DropDownChoice<String>("types") {
            private static final long serialVersionUID = 1L;
            @Override
            public List<? extends String> getChoices() {
                if (PropertyDialog.this.name != null) {
                    List<PropertyDefinition> propdefs = choiceModel.getObject().get(PropertyDialog.this.name);
                    if (propdefs != null) {
                        List<String> result = new ArrayList<>(propdefs.size());
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
                List<String> result = new ArrayList<>();
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

        nameField.setRequired(true);
        add(nameField);

        values = new LinkedList<>();
        values.add("");

        final WebMarkupContainer valuesContainer = new WebMarkupContainer("valuesContainer");
        valuesContainer.setOutputMarkupId(true);
        add(valuesContainer);
        
        valuesContainer.add(new ListView<String>("values", values) {

            @Override
            protected void populateItem(final ListItem<String> item) {
                final TextField textField = new TextField<>("val", item.getModel());
                textField.add(new OnChangeAjaxBehavior() {

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                    }

                });
                item.add(textField);

                if (focusOnLatestValue && item.getIndex() == (values.size() - 1)) {
                    AjaxRequestTarget ajax = RequestCycle.get().find(AjaxRequestTarget.class);
                    if (ajax != null) {
                        ajax.focusComponent(textField);
                    }
                    focusOnLatestValue = false;
                }

                final AjaxLink deleteLink = new AjaxLink("removeLink") {
                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        values.remove(item.getIndex());
                        target.add(valuesContainer);
                    }

                    @Override
                    public boolean isVisible() {
                        return super.isVisible() && item.getIndex() > 0;
                    }
                };
                
//                deleteLink.add(new Image("removeIcon", new PackageResourceReference(PropertiesEditor.class, "edit-delete-16.png")));
                deleteLink.add(new AttributeModifier("title", getString("property.value.remove")));
                deleteLink.add(new InputBehavior(new KeyType[] {KeyType.Enter}, EventType.click) {
                    @Override
                    protected String getTarget() {
                        return "'" + deleteLink.getMarkupId() + "'";
                    }
                });
                item.add(deleteLink);
            }
        });

        final AjaxLink addLink = new AjaxLink("addLink") {
            @Override
            public void onClick(final AjaxRequestTarget target) {
                values.add("");
                target.add(valuesContainer);
                focusOnLatestValue = true;
            }

            @Override
            public boolean isVisible() {
                return isMultiple;
            }
        };
        addLink.add(new AttributeModifier("title", getString("property.value.add")));
        addLink.add(new Image("addIcon", new PackageResourceReference(PropertiesEditor.class, "list-add-16.png")));
        addLink.add(new InputBehavior(new KeyType[] {KeyType.Enter}, EventType.click) {
            @Override
            protected String getTarget() {
                return "'" + addLink.getMarkupId() + "'";
            }
        });
        valuesContainer.add(addLink);

        checkBox.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = 1L;
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(valuesContainer);
                if (!isMultiple && values.size() > 1) {
                    String first = values.get(0);
                    values.clear();
                    values.add(first);
                }
            }
        });

        // dynamic update of related components when name is updated
        nameField.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(ddChoice);
                target.add(checkBox);
                target.add(valuesContainer);
                focusOnLatestValue = true;
            }
        });

        setFocus(nameField);
    }

    @Override
    public void onOk() {
        try {
            final IModel<Node> nodeModel = modelReference.getModel();
            final Node node = nodeModel.getObject();
            final int propertyType = PropertyType.valueFromName(type);
            
            if (isMultiple) {
                node.setProperty(name, getJcrValues(propertyType), propertyType);
            } else {
                node.setProperty(name, getJcrValue(propertyType), propertyType);
            }
            
            modelReference.setModel(new JcrNodeModel(node));

        } catch (ConstraintViolationException e) {
            error("It is not allowed to add the property '" + name + "' on this node.");
            log.error(e.getClass().getName() + " : " + e.getMessage());
        } catch (RepositoryException e) {
            error(e.toString());
            log.error(e.getClass().getName() + " : " + e.getMessage(), e);
        }
    }

    public IModel<String> getTitle() {
        return Model.of("Add a new Property");
    }

    private Value getJcrValue(final int propertyType) {
        try {
            String value = values.get(0);
            return getValueFactory().createValue(value == null ? "" : value, propertyType);
        } catch (RepositoryException ex) {
            log.info(ex.getMessage());
        }
        return null;
    }

    private Value[] getJcrValues(final int propertyType) {
        try {
            ValueFactory factory = getValueFactory();
            Value[] jcrValues = new Value[values.size()];
            for (int i = 0; i < jcrValues.length; i++) {
                String value = values.get(i);
                jcrValues[i] = factory.createValue(value == null ? "" : value, propertyType);
            }
            return jcrValues;
        } catch (RepositoryException e) {
            log.info(e.getMessage());
        }
        return null;
    }

    private ValueFactory getValueFactory() throws RepositoryException {
        return UserSession.get().getJcrSession().getValueFactory();
    }

    @Override
       public IValueMap getProperties() {
        return new ValueMap("width=420,height=300").makeImmutable();
    }

    @Override
    protected void onDetach() {
        choiceModel.detach();
        super.onDetach();
    }
    
}
