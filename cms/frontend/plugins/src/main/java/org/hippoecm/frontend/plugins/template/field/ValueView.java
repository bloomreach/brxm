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
package org.hippoecm.frontend.plugins.template.field;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.template.TemplateEngine;
import org.hippoecm.frontend.template.model.TemplateModel;
import org.hippoecm.frontend.template.model.ValueTemplateProvider;
import org.hippoecm.frontend.widgets.AbstractView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueView extends AbstractView {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ValueView.class);

    public ValueView(String id, ValueTemplateProvider dataProvider, PropertyFieldPlugin plugin) {
        super(id, dataProvider, plugin);
    }

    // Implement DataView
    @Override
    protected void populateItem(Item item) {
        final TemplateModel templateModel = (TemplateModel) item.getModel();
        ValueTemplateProvider provider = (ValueTemplateProvider) getDataProvider();

        PropertyFieldPlugin plugin = (PropertyFieldPlugin) getPlugin();
        TemplateEngine engine = plugin.getPluginManager().getTemplateEngine();
        item.add(engine.createTemplate("value", templateModel, plugin));

        //Remove value link
        if (!provider.getDescriptor().isMandatory() || (provider.size() > 0)) {
            item.add(new AjaxLink("remove") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    PropertyFieldPlugin fieldPlugin = (PropertyFieldPlugin) findParent(PropertyFieldPlugin.class);
                    if (fieldPlugin != null) {
                        fieldPlugin.onRemoveValue(target, templateModel);
                    }
                }
            });
        } else {
            item.add(new Label("remove", ""));
        }
    }

    @Override
    public void destroyItem(Item item) {
        Plugin template = (Plugin) item.get("value");
        template.destroy();
        super.destroyItem(item);
    }
}
