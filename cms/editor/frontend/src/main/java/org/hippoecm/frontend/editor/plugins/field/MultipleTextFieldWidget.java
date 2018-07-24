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
package org.hippoecm.frontend.editor.plugins.field;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.skin.Icon;

public class MultipleTextFieldWidget extends Panel {

    public MultipleTextFieldWidget(String id, final IModel<IPluginConfig> model, final IClusterConfig cluster, final String name, final boolean editable) {
        super(id, model);
        setOutputMarkupId(true);

        add(CssClass.append("hippo-multiple-text-field-widget"));

        IPluginConfig config = model.getObject();
        final ListView<String> list = new MultipleValueListView(name, config, cluster, editable);
        add(list);

        AjaxButton button = new AjaxButton("add") {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                List<String> values = list.getModelObject();
                values.add("");
                list.setModelObject(values);
                target.add(MultipleTextFieldWidget.this);
            }
        };
        button.setVisible(editable);
        button.setDefaultFormProcessing(false);
        add(button);

        button.add(HippoIcon.fromSprite("icon", Icon.PLUS));
    }

    private class MultipleValueListView extends ListView<String> {

        private final boolean editable;

        public MultipleValueListView(final String name, final IPluginConfig config, final IClusterConfig cluster, final boolean editable) {
            super("values", new MultipleValueListModel(name, config, cluster));
            this.editable = editable;
        }

        @Override
        protected void populateItem(final ListItem<String> item) {
            final int index = item.getIndex();
            final IModel<List<String>> listModel = (IModel<List<String>>) getModel();
            final MultipleValueListItemModel itemModel = new MultipleValueListItemModel(index, listModel);
            item.add(new TextField<String>("value", itemModel) {
                {
                    setFlag(FLAG_CONVERT_EMPTY_INPUT_STRING_TO_NULL, false);
                    setType(String.class);
                    setEnabled(editable);
                    add(new AjaxFormComponentUpdatingBehavior("change") {
                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                            target.add(MultipleTextFieldWidget.this);
                        }
                    });
                }
            });

            AjaxButton button = new AjaxButton("delete") {
                @Override
                protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                    List<String> list = MultipleValueListView.this.getModelObject();
                    list.remove(item.getIndex());
                    MultipleValueListView.this.setModelObject(list);
                    target.add(MultipleTextFieldWidget.this);
                }
            };
            button.setVisible(editable);
            button.setDefaultFormProcessing(false);
            button.add(HippoIcon.fromSprite("icon", Icon.TIMES));
            item.add(button);
        }
    }

    private class MultipleValueListModel implements IModel<List<String>> {

        private final String name;
        private final IPluginConfig config;
        private final IClusterConfig cluster;

        public MultipleValueListModel(String name, IPluginConfig config, IClusterConfig cluster) {
            this.name = name;
            this.config = config;
            this.cluster = cluster;
        }

        @Override
        public List<String> getObject() {
            final List<String> values = new ArrayList<>();
            final String[] valuesArray = config.getStringArray(name);
            if (valuesArray != null) {
                Collections.addAll(values, valuesArray);
            } else {
                final String[] defaultsArray = cluster.getStringArray(name);
                if (defaultsArray != null) {
                    Collections.addAll(values, defaultsArray);
                }
            }
            return values;
        }

        @Override
        public void setObject(List<String> object) {
            config.put(name, object.toArray(new String[object.size()]));
        }

        @Override
        public void detach() {
            if (cluster != null && cluster instanceof IDetachable) {
                ((IDetachable) cluster).detach();
            }
            if (config != null && config instanceof IDetachable) {
                ((IDetachable) config).detach();
            }
        }
    }

    private class MultipleValueListItemModel implements IModel<String> {

        private final int index;
        private final IModel<List<String>> model;

        public MultipleValueListItemModel(int index, IModel<List<String>> model) {
            this.index = index;
            this.model = model;
        }

        @Override
        public String getObject() {
            return model.getObject().get(index);
        }

        @Override
        public void setObject(String object) {
            List<String> values = model.getObject();
            values.set(index, object);
            model.setObject(values);
        }

        @Override
        public void detach() {
            if (model != null) {
                model.detach();
            }
        }
    }

}
