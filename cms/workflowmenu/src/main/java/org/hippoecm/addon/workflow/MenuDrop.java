/*
 *  Copyright 2009-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.addon.workflow;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.StringResourceModel;

class MenuDrop extends Panel implements MenuComponent {

    private static final long serialVersionUID = 1L;

    MenuDrop(String id, ActionDescription wf, MenuHierarchy menu) {
        super(id);

        WebMarkupContainer headerItem;
        add(headerItem = new WebMarkupContainer("headerItem"));
        headerItem.add(new Label("header", new StringResourceModel("empty-menu", this)));

        List<Component> components = menu.list(this);
        add(new DataView("list", new ListDataProvider(components)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(final Item item) {
                MenuItem menuItem = (MenuItem) item.getModelObject();
                item.add(menuItem);
            }
        });

        if (components.size() > 0) {
            headerItem.setVisible(false);
        }
    }
}
