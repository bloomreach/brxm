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
package org.hippoecm.frontend.editor.plugins.field;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.IClusterable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.AbstractRenderService;
import org.hippoecm.frontend.widgets.TextFieldWidget;

public class FieldPluginEditor extends Panel {

    private static final long serialVersionUID = 1L;

    private CssProvider cssProvider;

    public FieldPluginEditor(String id, IModel<IPluginConfig> model, final boolean editable) {
        super(id, model);

        setOutputMarkupId(true);

        cssProvider = new CssProvider();

        if (editable) {
            add(new TextFieldWidget("caption-editor", new PropertyModel<String>(model, "caption")));
        } else {
            add(new Label("caption-editor", new PropertyModel<String>(model, "caption")));
        }
        add(new RefreshingView<String>("css") {
            private static final long serialVersionUID = 1L;

            @Override
            protected Iterator<IModel<String>> getItemModels() {
                return cssProvider.iterator();
            }

            @Override
            protected void populateItem(final Item<String> item) {
                if (editable) {
                    item.add(new TextFieldWidget("editor", item.getModel()));
                } else {
                    item.add(new Label("editor", item.getModel()));
                }
                item.add(new AjaxLink<Void>("remove") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        cssProvider.remove(item.getIndex());
                        target.addComponent(FieldPluginEditor.this);
                    }
                }.setVisible(editable));
            }
        });
        add(new AjaxLink<Void>("add-css") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                cssProvider.addNew();
                target.addComponent(FieldPluginEditor.this);
            }
        }.setVisible(editable));
    }

    private class CssProvider implements IClusterable {
        private static final long serialVersionUID = 1L;

        final List<CssModel> cssModels;

        CssProvider() {
            IPluginConfig config = (IPluginConfig) getDefaultModelObject();
            final String[] classes = config.getStringArray(AbstractRenderService.CSS_ID);
            if (classes != null) {
                cssModels = new ArrayList<CssModel>(classes.length);
                for (String className : classes) {
                    cssModels.add(new CssModel(className));
                }
            } else {
                cssModels = new ArrayList<CssModel>();
            }
        }

        Iterator<IModel<String>> iterator() {
            final Iterator<CssModel> base = cssModels.iterator();
            return new Iterator<IModel<String>>() {

                public boolean hasNext() {
                    return base.hasNext();
                }

                public IModel<String> next() {
                    return base.next();
                }

                public void remove() {
                    base.remove();
                    save();
                }
            };
        }

        void remove(int index) {
            cssModels.remove(index);
            save();
        }

        void addNew() {
            cssModels.add(new CssModel(""));
            detach();
        }

        void save() {
            IPluginConfig config = (IPluginConfig) getDefaultModelObject();
            String[] values = new String[cssModels.size()];
            for (int i = 0; i < cssModels.size(); i++) {
                values[i] = (String) cssModels.get(i).getObject();
            }
            config.put(AbstractRenderService.CSS_ID, values);
        }

        private class CssModel implements IModel<String> {
            private static final long serialVersionUID = 1L;

            String className;

            public CssModel(String className) {
                this.className = className;
            }

            public void setObject(String object) {
                className = (String) object;
                save();
            }

            public String getObject() {
                return className;
            }

            public void detach() {
            }
        }
    }

}
