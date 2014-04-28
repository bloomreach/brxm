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
package org.hippoecm.frontend.plugins.console.editor;

import java.util.List;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.model.properties.StringConverter;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.widgets.BooleanFieldWidget;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PropertyValueEditor extends DataView {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PropertyValueEditor.class);

    private JcrPropertyModel propertyModel;

    PropertyValueEditor(String id, JcrPropertyModel dataProvider) {
        super(id, dataProvider);
        this.propertyModel = dataProvider;
        setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
    }

    @Override
    protected void populateItem(Item item) {
        EditorPlugin plugin = findParent(EditorPlugin.class);
        IPluginContext context = plugin.getPluginContext();
        List<ValueEditorFactory> factoryList = context.getServices(ValueEditorFactory.SERVICE_ID, ValueEditorFactory.class);
        try {
            final JcrPropertyValueModel valueModel = (JcrPropertyValueModel) item.getModel();

            Component valueEditor = null;
            for (ValueEditorFactory factory : factoryList) {
                if (factory.canEdit(valueModel)) {
                    valueEditor = factory.createEditor("value", valueModel);
                    item.add(valueEditor);
                    break;
                }
            }

            if (valueEditor == null) {
                if (propertyModel.getProperty().getType() == PropertyType.BINARY) {
                    item.add(new BinaryEditor("value", propertyModel));
                } else if (propertyModel.getProperty().getDefinition().isProtected()) {
                    item.add(new Label("value", valueModel));
                } else if (propertyModel.getProperty().getType() == PropertyType.BOOLEAN) {
                    item.add(new BooleanFieldWidget("value", valueModel));
                } else {
                    StringConverter stringModel = new StringConverter(valueModel);
                    String asString = stringModel.getObject();
                    final int textAreaMaxColumns = 100;
                    if (asString.contains("\n")) {
                        TextAreaWidget editor = new TextAreaWidget("value", stringModel);
                        String[] lines = StringUtils.splitByWholeSeparator(asString, "\n");
                        int rowCount = lines.length;
                        int columnCount = 1;
                        for (String line : lines) {
                            int length = line.length();
                            if (length > columnCount) {
                                if (length > textAreaMaxColumns) {
                                    columnCount = textAreaMaxColumns;
                                    rowCount += (length / textAreaMaxColumns) + 1;
                                } else {
                                    columnCount = length;
                                }
                            }
                        }
                        editor.setCols(String.valueOf(columnCount + 1));
                        editor.setRows(String.valueOf(rowCount + 1));
                        item.add(editor);

                    } else if (asString.length() > textAreaMaxColumns) {
                        TextAreaWidget editor = new TextAreaWidget("value", stringModel);
                        editor.setCols(String.valueOf(textAreaMaxColumns));
                        editor.setRows(String.valueOf((asString.length() / 80)));
                        item.add(editor);

                    } else {
                        TextAreaWidget editor = new TextAreaWidget("value", stringModel);
                        editor.setCols(String.valueOf(textAreaMaxColumns));
                        editor.setRows("1");
                        item.add(editor);
                    }
                }
            }

            final AjaxLink removeLink = new AjaxLink("remove") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    try {
                        Property prop = propertyModel.getProperty();
                        Value[] values = prop.getValues();
                        values = (Value[]) ArrayUtils.remove(values, valueModel.getIndex());
                        prop.getParent().setProperty(prop.getName(), values, prop.getType());
                    } catch (RepositoryException e) {
                        log.error(e.getMessage());
                    }
                    NodeEditor editor = findParent(NodeEditor.class);
                    if (editor != null) {
                        target.add(editor);
                    }
                }
            };
            removeLink.add(new Image("remove-icon", new PackageResourceReference(PropertiesEditor.class, "edit-delete-16.png")));
            removeLink.add(new AttributeModifier("title", getString("property.value.remove")));
            
            PropertyDefinition definition = propertyModel.getProperty().getDefinition();
            removeLink.setVisible(definition.isMultiple() && !definition.isProtected());

            item.add(removeLink);
        }
        catch (RepositoryException e) {
            log.error(e.getMessage());
            item.add(new Label("value", e.getClass().getName() + ":" + e.getMessage()));
            item.add(new Label("remove", ""));
        }
    }

}
