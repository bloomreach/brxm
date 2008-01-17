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

import java.util.Iterator;

import org.apache.wicket.Component;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.markup.repeater.IItemFactory;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugins.template.model.AbstractProvider;
import org.hippoecm.frontend.plugins.template.model.FieldModel;

public class FieldView extends DataView {
    private static final long serialVersionUID = 1L;

    TemplateEngine engine;

    public FieldView(String wicketId, AbstractProvider provider, TemplateEngine engine) {
        super(wicketId, provider);

        this.engine = engine;

        setItemReuseStrategy(new DefaultItemReuseStrategy() {
            private static final long serialVersionUID = 1L;

            @Override
            public Iterator getItems(final IItemFactory factory, final Iterator newModels, final Iterator existingItems) {
                // Wicket doesn't detach the items that are thrown away, so do
                // it ourselves.  Furthermore, plugins must be disconnected.
                while (existingItems.hasNext()) {
                    Item item = (Item) existingItems.next();
                    item.detach();
                    Component template = item.get("sub");
                    if (template instanceof Plugin) {
                        ((Plugin) template).destroy();
                    }
                }
                return super.getItems(factory, newModels, null);
            }
        });
    }

    @Override
    protected void populateItem(Item item) {
        FieldModel fieldModel = (FieldModel) item.getModel();
        item.add(engine.createTemplate("sub", fieldModel));
    }
}
