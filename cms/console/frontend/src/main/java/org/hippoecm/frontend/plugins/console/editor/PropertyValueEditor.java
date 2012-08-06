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
package org.hippoecm.frontend.plugins.console.editor;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.markup.repeater.data.DataView;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.model.properties.StringConverter;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PropertyValueEditor extends DataView {
    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(PropertyValueEditor.class);

    protected JcrPropertyModel propertyModel;
    private int textAreaMaxColumns = 100;

    PropertyValueEditor(String id, JcrPropertyModel dataProvider) {
        super(id, dataProvider);
        this.propertyModel = dataProvider;
        setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
    }

    @Override
    protected void populateItem(Item item) {
        try {
            final JcrPropertyValueModel valueModel = (JcrPropertyValueModel) item.getModel();

            if (propertyModel.getProperty().getType() == PropertyType.BINARY) {
                Node node = propertyModel.getProperty().getParent();
                if (node.isNodeType("hippo:resource") || node.isNodeType("nt:resource")) {
                    item.add(new ResourceEditor("value", new JcrNodeModel(node)));
                } else {
                    long size = propertyModel.getProperty().getLength();
                    item.add(new Label("value", "binary data (" + size + " bytes)"));
                }

            } else if (ReferenceEditor.isReference(valueModel)) {
                item.add(new ReferenceEditor("value", propertyModel, valueModel));

            } else if (propertyModel.getProperty().getDefinition().isProtected()) {
                item.add(new Label("value", valueModel));

            } else {
                StringConverter stringModel = new StringConverter(valueModel);
                String asString = stringModel.getObject();
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

            //Remove value link
            if (propertyModel.getProperty().getDefinition().isMultiple()
                    && !propertyModel.getProperty().getDefinition().isProtected()) {
                item.add(new AjaxLink("remove") {
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
                            target.addComponent(editor);
                        }
                    }
                });
            } else {
                item.add(new Label("remove").setVisible(false));
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            item.add(new Label("value", e.getClass().getName() + ":" + e.getMessage()));
            item.add(new Label("remove", ""));
        }
    }

}
