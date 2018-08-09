/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ReadOnlyModel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.plugins.standards.list.resolvers.TitleAttribute;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.AutoCompleteTextFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

public class PropertyDialog extends AbstractDialog<Node> {

    private static final Logger log = LoggerFactory.getLogger(PropertyDialog.class);

    private static final List<String> ALL_TYPES = new ArrayList<>(11);
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

    private static final IValueMap DIALOG_PROPERTIES = new ValueMap("width=420,height=300").makeImmutable();

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

        getParent().add(CssClass.append("property-dialog"));

        // list defined properties for automatic completion
        choiceModel = new LoadableDetachableModel<Map<String, List<PropertyDefinition>>>() {

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
                        if (!isResidual(property) && node.hasProperty(property)) {
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
        final IModel<List<String>> typeChoicesModel = ReadOnlyModel.of(() -> {
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
        });

        final IModel<String> typeChoiceModel = new Model<String>() {
            @Override
            public String getObject() {
                List<? extends String> choices = typeChoicesModel.getObject();
                if (choices.size() == 1) {
                    type = choices.iterator().next();
                }
                return type;
            }

            @Override
            public void setObject(final String type) {
                PropertyDialog.this.type = type;
            }
        };

        final DropDownChoice<String> ddChoice = new DropDownChoice<>("types", typeChoiceModel, typeChoicesModel);

        ddChoice.setRequired(true);
        ddChoice.setOutputMarkupId(true);
        ddChoice.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {}
        });
        add(ddChoice);

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

                deleteLink.add(TitleAttribute.set(getString("property.value.remove")));
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
        addLink.add(TitleAttribute.set(getString("property.value.add")));
        addLink.add(new InputBehavior(new KeyType[] {KeyType.Enter}, EventType.click) {
            @Override
            protected String getTarget() {
                return "'" + addLink.getMarkupId() + "'";
            }
        });
        valuesContainer.add(addLink);

        checkBox.add(new AjaxFormComponentUpdatingBehavior("change") {
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

        // text field for property name
        AutoCompleteSettings settings = new AutoCompleteSettings();
        settings.setAdjustInputWidth(false);
        settings.setUseSmartPositioning(true);
        settings.setShowCompleteListOnFocusGain(true);
        settings.setShowListOnEmptyInput(true);
        // Setting a max height will trigger a correct recalculation of the height when the list of items is filtered
        settings.setMaxHeightInPx(400);

        final TextField<String> nameField = new AutoCompleteTextFieldWidget<String>("name",
                PropertyModel.of(this, "name"), settings) {

            @Override
            protected Iterator<String> getChoices(String input) {
                List<String> result = new ArrayList<>();
                for (String propName : choiceModel.getObject().keySet()) {
                    if (propName.toLowerCase().contains(input.toLowerCase())) {
                        result.add(propName);
                    }
                }
                return result.iterator();
            }

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                super.onUpdate(target);

                target.add(ddChoice);
                target.add(checkBox);
                target.add(valuesContainer);
                focusOnLatestValue = true;
            }
        };

        nameField.setRequired(true);
        add(nameField);
        setFocus(nameField);
    }

    @Override
    public IModel<String> getTitle() {
        return Model.of("Add a new Property");
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
            log.info(e.getClass().getName() + " : " + e.getMessage());
        } catch (ValueFormatException e) {
            error(e.toString());
            log.info(e.getClass().getName() + " : " + e.getMessage());
        } catch (RepositoryException e) {
            error(e.toString());
            log.error(e.getClass().getName() + " : " + e.getMessage(), e);
        }
    }

    @Override
    public IValueMap getProperties() {
        return DIALOG_PROPERTIES;
    }

    @Override
    protected void onDetach() {
        choiceModel.detach();
        super.onDetach();
    }

    private boolean isResidual(final String propertyName) {
        return propertyName.equals("*");
    }

    private Value getJcrValue(final int propertyType) throws RepositoryException {
        String value = values.get(0);
        return getValueFactory().createValue(value == null ? "" : value, propertyType);
    }

    private Value[] getJcrValues(final int propertyType) throws RepositoryException {
        ValueFactory factory = getValueFactory();
        Value[] jcrValues = new Value[values.size()];
        for (int i = 0; i < jcrValues.length; i++) {
            String value = values.get(i);
            jcrValues[i] = factory.createValue(value == null ? "" : value, propertyType);
        }
        return jcrValues;
    }

    private ValueFactory getValueFactory() throws RepositoryException {
        return UserSession.get().getJcrSession().getValueFactory();
    }
}
