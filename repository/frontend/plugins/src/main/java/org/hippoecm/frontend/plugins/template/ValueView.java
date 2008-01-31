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
package org.hippoecm.frontend.plugins.template;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.markup.repeater.data.DataView;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueView extends DataView {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ValueView.class);

    private boolean multiple;

    public ValueView(String id, JcrPropertyModel dataProvider, boolean multiple) {
        super(id, dataProvider);
        this.multiple = multiple;
        setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
    }

    // Implement DataView
    @Override
    protected void populateItem(Item item) {
        final JcrPropertyValueModel valueModel = (JcrPropertyValueModel) item.getModel();
        item.add(new TextFieldWidget("value", valueModel));

        //Remove value link
        if (multiple) {
            item.add(new AjaxLink("remove", valueModel) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    PropertyTemplatePlugin template = (PropertyTemplatePlugin) findParent(PropertyTemplatePlugin.class);
                    if (template != null) {
                        template.onRemoveValue(target, valueModel);
                    }
                }
            });
        } else {
            item.add(new Label("remove", ""));
        }
    }
}
