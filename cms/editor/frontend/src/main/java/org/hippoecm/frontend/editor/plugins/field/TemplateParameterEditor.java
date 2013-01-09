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
package org.hippoecm.frontend.editor.plugins.field;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.widgets.TextFieldWidget;

public class TemplateParameterEditor extends Panel {

    private static final long serialVersionUID = 1L;

    public TemplateParameterEditor(String id, final IModel<IPluginConfig> model, IClusterConfig cluster, final boolean editable) {
        super(id, model);

        List<String> properties = new ArrayList<String>(cluster.getProperties());
        properties.remove("mode");
        add(new ListView<String>("properties", properties) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<String> item) {
                final String property = item.getModelObject();
                item.add(new Label("label", property));

                IModel<String> valueModel = new PropertyModel<String>(TemplateParameterEditor.this.getDefaultModel(), "[" + property + "]");
                if (editable) {
                    item.add(new TextFieldWidget("value", valueModel));
                } else {
                    item.add(new Label("value", valueModel));
                }
            }

        });
    }

}
