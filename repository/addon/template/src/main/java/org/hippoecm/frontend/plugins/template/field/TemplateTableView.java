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
package org.hippoecm.frontend.plugins.template.field;

import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.parameters.ParameterValue;
import org.hippoecm.frontend.legacy.template.TemplateEngine;
import org.hippoecm.frontend.legacy.template.model.AbstractProvider;
import org.hippoecm.frontend.legacy.template.model.TemplateModel;
import org.hippoecm.frontend.legacy.widgets.AbstractView;

public class TemplateTableView extends AbstractView {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private Map<String, ParameterValue> config;

    public TemplateTableView(String wicketId, AbstractProvider provider, Plugin template,
            Map<String, ParameterValue> config) {
        super(wicketId, provider, template);
        this.config = config;
        populate();
    }

    @Override
    protected void populateItem(Item item) {
        final TemplateModel model = (TemplateModel) item.getModel();
        final NodeFieldTablePlugin plugin = (NodeFieldTablePlugin) getPlugin();

        TemplateEngine engine = plugin.getPluginManager().getTemplateEngine();
        item.add(engine.createTemplate("template", model, plugin, config));

        final Boolean bool = new Boolean(false);
        item.add(new AjaxCheckBox("check", new Model(bool)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (((Boolean) getModelObject()).booleanValue()) {
                    plugin.onSelectNode(model, target);
                } else {
                    plugin.onDeselectNode(model, target);
                }
            }
        });
    }

    public void destroyItem(Item item) {
        Plugin template = (Plugin) item.get("template");
        template.destroy();
        super.destroyItem(item);
    }
}
