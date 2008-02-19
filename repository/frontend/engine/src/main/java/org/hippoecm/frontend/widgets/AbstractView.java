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
package org.hippoecm.frontend.widgets;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.markup.repeater.IItemFactory;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugin.Plugin;

public abstract class AbstractView extends DataView {

    private Plugin plugin;

    public AbstractView(String wicketId, IDataProvider provider, Plugin plugin) {
        super(wicketId, provider);

        this.plugin = plugin;

        setItemReuseStrategy(new ReuseIfModelsEqualStrategy() {
            private static final long serialVersionUID = 1L;

            public Iterator getItems(final IItemFactory factory, final Iterator newModels, final Iterator existingItems) {
                List<IModel> models = new LinkedList<IModel>();
                while (newModels.hasNext()) {
                    models.add((IModel) newModels.next());
                }

                List<Item> items = new LinkedList<Item>();
                while (existingItems.hasNext()) {
                    Item item = (Item) existingItems.next();
                    if (!models.contains(item.getModel())) {
                        destroyItem(item);
                    } else {
                        items.add(item);
                    }
                }
                return super.getItems(factory, models.iterator(), items.iterator());
            }
        });
    }

    public void populate() {
        super.onPopulate();
    }

    public Plugin getPlugin() {
        return plugin;
    }

    protected void destroyItem(Item item) {
    }
}
