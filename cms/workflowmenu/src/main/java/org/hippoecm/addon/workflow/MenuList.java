/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.Model;

class MenuList extends Panel implements MenuComponent {

    private static final long serialVersionUID = 1L;

    private static final int TWO_COL_THRESHOLD = 20;
    private static final int THREE_COL_THRESHOLD = 40;

    private final List<Component> list;
    private final MenuHierarchy menu;

    MenuList(String id, MenuHierarchy menu) {
        super(id);
        this.menu = menu;
        this.list = menu.list(this);

        add(new DataView<Component>("list", new ListDataProvider<Component>(list)) {

            public void populateItem(final Item<Component> item) {
                Component menuItem = item.getModelObject();
                item.add(menuItem);
            }
        });
    }

    public void update() {
        this.list.clear();
        this.list.addAll(menu.list(this));
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        if (list.size() > THREE_COL_THRESHOLD) {
            add(new AttributeAppender("class", true, Model.of("hippo-toolbar-three-col"), " "));
        } else if (list.size() > TWO_COL_THRESHOLD) {
            add(new AttributeAppender("class", true, Model.of("hippo-toolbar-two-col"), " "));
        }
    }
}
