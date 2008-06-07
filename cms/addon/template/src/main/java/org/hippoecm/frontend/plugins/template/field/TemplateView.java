/*
 * Copyright 2008 Hippo
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

import java.util.Map;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.repeater.Item;
import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.parameters.ParameterValue;
import org.hippoecm.frontend.legacy.template.FieldDescriptor;
import org.hippoecm.frontend.legacy.template.TemplateEngine;
import org.hippoecm.frontend.legacy.template.config.TemplateConfig;
import org.hippoecm.frontend.legacy.template.model.AbstractProvider;
import org.hippoecm.frontend.legacy.template.model.TemplateModel;
import org.hippoecm.frontend.legacy.widgets.AbstractView;

public class TemplateView extends AbstractView {
    private static final long serialVersionUID = 1L;

    private String mode;
    private FieldDescriptor descriptor;
    private Map<String, ParameterValue> config;

    public TemplateView(String wicketId, AbstractProvider provider, Plugin template, FieldDescriptor descriptor,
            Map<String, ParameterValue> config, String mode) {
        super(wicketId, provider, template);
        this.descriptor = descriptor;
        this.config = config;
        this.mode = mode;
        populate();
    }

    @Override
    protected void populateItem(Item item) {
        final TemplateModel model = (TemplateModel) item.getModel();
        final NodeFieldPlugin plugin = (NodeFieldPlugin) getPlugin();

        TemplateEngine engine = plugin.getPluginManager().getTemplateEngine();
        item.add(engine.createTemplate("template", model, plugin, config));

        MarkupContainer remove = new AjaxLink("remove") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                plugin.onRemoveNode(model, target);
            }
        };
        if (!TemplateConfig.EDIT_MODE.equals(mode) || descriptor.isMandatory()) {
            remove.setVisible(false);
        }
        item.add(remove);

        MarkupContainer upLink = new AjaxLink("up") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                plugin.onMoveNodeUp(model, target);
            }
        };
        if (!TemplateConfig.EDIT_MODE.equals(mode) || !descriptor.isOrdered()) {
            upLink.setVisible(false);
        }
        if (item.getIndex() == 0) {
            upLink.setEnabled(false);
        }
        item.add(upLink);
    }

    public void destroyItem(Item item) {
        Plugin template = (Plugin) item.get("template");
        template.destroy();
        super.destroyItem(item);
    }
}
