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
package org.hippoecm.frontend.plugins.template;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.repeater.Item;
import org.hippoecm.frontend.template.TemplateEngine;
import org.hippoecm.frontend.template.model.TemplateModel;
import org.hippoecm.frontend.template.model.TemplateProvider;

public class TemplateView extends AbstractView {
    private static final long serialVersionUID = 1L;

    public TemplateView(String wicketId, TemplateProvider provider, MultipleFieldPlugin template) {
        super(wicketId, provider, template);
    }

    protected void populate() {
        super.onPopulate();
    }

    @Override
    protected void populateItem(Item item) {
        final TemplateModel model = (TemplateModel) item.getModel();
        final MultipleFieldPlugin plugin = (MultipleFieldPlugin) getPlugin();

        TemplateEngine engine = plugin.getPluginManager().getTemplateEngine();
        item.add(engine.createTemplate("template", model, plugin));

        TemplateProvider provider = (TemplateProvider) getDataProvider();

        MarkupContainer remove = new AjaxLink("remove") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                plugin.onRemoveNode(model, target);
            }
        };
        if (provider.getDescriptor().isMandatory()) {
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
        if (!provider.getDescriptor().isOrdered()) {
            upLink.setVisible(false);
        }
        if (item.getIndex() == 0) {
            upLink.setEnabled(false);
        }
        item.add(upLink);
    }
}
