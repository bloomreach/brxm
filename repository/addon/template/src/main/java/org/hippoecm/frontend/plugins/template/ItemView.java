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

import org.apache.wicket.markup.repeater.Item;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.template.TemplateEngine;
import org.hippoecm.frontend.template.model.ItemModel;
import org.hippoecm.frontend.template.model.ItemProvider;
import org.hippoecm.frontend.widgets.AbstractView;

public class ItemView extends AbstractView {
    private static final long serialVersionUID = 1L;

    public ItemView(String wicketId, ItemProvider provider, Plugin plugin) {
        super(wicketId, provider, plugin);
        populate();
    }

    @Override
    protected void populateItem(Item item) {
        ItemModel itemModel = (ItemModel) item.getModel();
        TemplateEngine engine = getPlugin().getPluginManager().getTemplateEngine();
        item.add(engine.createItem("field", itemModel, getPlugin()));
    }

    @Override
    public void destroyItem(Item item) {
        Plugin field = (Plugin) item.get("field");
        field.destroy();
        super.destroyItem(item);
    }
}
