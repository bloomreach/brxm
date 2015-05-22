/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins.field;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.PropertyDescriptor;
import org.hippoecm.frontend.widgets.TextFieldWidget;

public class TemplateParameterEditor extends Panel {

    private static final long serialVersionUID = 1L;

    public TemplateParameterEditor(String id, final IModel<IPluginConfig> model, final IClusterConfig cluster, final boolean editable) {
        super(id, model);
        List<PropertyDescriptor> properties = new ArrayList<>(Collections2.filter(cluster.getPropertyDescriptors(), new Predicate<PropertyDescriptor>() {
            @Override
            public boolean apply(PropertyDescriptor descriptor) {
                return !"mode".equals(descriptor.getName());
            }
        }));

        add(new ListView<PropertyDescriptor>("properties", properties) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<PropertyDescriptor> item) {
                final PropertyDescriptor property = item.getModelObject();
                item.add(new Label("label", property.getName()));

                if (property.isMultiple()) {
                    item.add(new MultipleTextFieldWidget("value", model, cluster, property.getName(), editable));
                } else {
                    final IModel<String> valueModel = new PropertyModel<String>(TemplateParameterEditor.this.getDefaultModel(), "[" + property.getName() + "]");
                    if (editable) {
                        item.add(new TextFieldWidget("value", new IModel<String>() {

                            @Override
                            public String getObject() {
                                String value = valueModel.getObject();

                                if (value != null) {
                                    return value;
                                }
                                return cluster.getString(property.getName());
                            }

                            @Override
                            public void setObject(final String value) {
                                if (!Strings.isEmpty(value) || valueModel.getObject() != null) {
                                    valueModel.setObject(value);
                                }
                            }

                            @Override
                            public void detach() {
                                valueModel.detach();
                            }
                        }));
                    } else {
                        item.add(new Label("value", valueModel));
                    }
                }

            }
        });
    }

}
