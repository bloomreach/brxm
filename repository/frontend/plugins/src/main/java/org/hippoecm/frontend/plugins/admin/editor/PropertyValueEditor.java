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
package org.hippoecm.frontend.plugins.admin.editor;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.lang.ArrayUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableMultiLineLabel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.markup.repeater.data.DataView;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;

public class PropertyValueEditor extends DataView {
    private static final long serialVersionUID = 1L;

    protected JcrPropertyModel propertyModel;

    public PropertyValueEditor(String id, JcrPropertyModel dataProvider) {
        super(id, dataProvider);
        this.propertyModel = dataProvider;
        setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
    }
    
    // Implement DataView
    @Override
    protected void populateItem(Item item) {
        try {
            boolean isProtected = propertyModel.getProperty().getDefinition().isProtected();
            boolean isMultiple = propertyModel.getProperty().getDefinition().isMultiple();
            final JcrPropertyValueModel valueModel = (JcrPropertyValueModel) item.getModel();

            //Value editor
            if (isProtected) {
                Label label = new Label("value", valueModel);
                item.add(label);
            } else {
                String value = valueModel.getObject().toString();
                if (value.contains("\n") || value.length() > 80) {
                    AjaxEditableMultiLineLabel editor = new AjaxEditableMultiLineLabel("value", valueModel) {
                        private static final long serialVersionUID = 1L;
                        @Override
                        protected void onSubmit(AjaxRequestTarget target) {
                            super.onSubmit(target);
                        }
                    };
                    editor.setCols(80);
                    editor.setRows(25);
                    item.add(editor);
                } else {
                    AjaxEditableLabel editor = new AjaxEditableLabel("value", valueModel) {
                        private static final long serialVersionUID = 1L;
                        @Override
                        protected void onSubmit(AjaxRequestTarget target) {
                            super.onSubmit(target);
                        }
                    };
                    item.add(editor);
                }
            }

            //Remove value link
            if (isMultiple) {
                item.add(new AjaxLink("remove", valueModel) {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        try {
                            Property prop = propertyModel.getProperty();
                            Value[] values = prop.getValues();
                            values = (Value[]) ArrayUtils.remove(values, valueModel.getIndex());
                            prop.setValue(values);
                        } catch (RepositoryException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        NodeEditor editor = (NodeEditor) findParent(NodeEditor.class);
                        target.addComponent(editor);
                    }
                });
            } else {
                item.add(new Label("remove", ""));
            }

        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
