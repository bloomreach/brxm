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
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang.ArrayUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.markup.repeater.data.DataView;

import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.widgets.TextAreaWidget;
import org.hippoecm.frontend.widgets.TextFieldWidget;

public class PropertyValueEditor extends DataView {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(PropertyValueEditor.class);

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
            boolean isBinary = isBinary(propertyModel.getProperty());

            final JcrPropertyValueModel valueModel = (JcrPropertyValueModel) item.getModel();
            if (isBinary) {
                Label label = new Label("value", "(binary)");
                item.add(label);
            } else if (isProtected) {
                Label label = new Label("value", valueModel);
                item.add(label);
            } else {
                String value = valueModel.getObject().toString();
                if (value.contains("\n") || value.length() > 80 || value.contains("<html>")) {
		    /* HREPTWO-334: Ugly, but effective way to get a Xinha add-on in place at this
		     * time.  This should be merged with the template engine, as the template
		     * engine is the one which determins which Widget to use.
		     */
                    if (value.contains("<html>")) {
                        try {
                            // HtmlEditorFactory.createHtmlEditor(, item.getPage()
                            // XinhaEditor editor = new XinhaEditor("value", valueModel);
                            Class clazz = Class.forName("org.hippoecm.repository.frontend.wysiwyg.xinha.XinhaEditor");
                            java.lang.reflect.Constructor constructor = clazz.getConstructor(new Class[] { String.class, Class.forName("org.hippoecm.repository.frontend.wysiwyg.xinha.XinhaEditorConfigurationBehaviour") });
                            Panel editor = (Panel) constructor.newInstance(new Object[] { "value", valueModel });
                            item.add(editor);
                        } catch(ClassNotFoundException ex) {
                            System.err.println(ex.getMessage());
                            ex.printStackTrace(System.err);
                        } catch(InstantiationException ex) {
                            System.err.println(ex.getMessage());
                            ex.printStackTrace(System.err);
                        } catch(NoSuchMethodException ex) {
                            System.err.println(ex.getMessage());
                            ex.printStackTrace(System.err);
                        } catch(IllegalAccessException ex) {
                            System.err.println(ex.getMessage());
                            ex.printStackTrace(System.err);
                        } catch(java.lang.reflect.InvocationTargetException ex) {
                            System.err.println(ex.getMessage());
                            ex.printStackTrace(System.err);
                        }
                    } else {
                        TextAreaWidget editor = new TextAreaWidget("value", valueModel);
                        item.add(editor);
                    }
                } else {
                    TextFieldWidget editor = new TextFieldWidget("value", valueModel);
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
                            log.error(e.getMessage());
                        }
                        NodeEditor editor = (NodeEditor) findParent(NodeEditor.class);
                        if (editor != null) {
                            target.addComponent(editor);
                        }
                    }
                });
            } else {
                item.add(new Label("remove", ""));
            }

        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
    }

    private boolean isBinary(Property property) throws RepositoryException {
        boolean isBinary;
        if (property.getDefinition().isMultiple()) {
            Value[] values = propertyModel.getProperty().getValues();
            if (values.length > 0) {
                isBinary = values[0].getType() == PropertyType.BINARY;
            } else {
                isBinary = false;
            }
        } else {
            isBinary = (propertyModel.getProperty().getValue().getType() == PropertyType.BINARY);
        }
        return isBinary;
    }

}
