/*
 *  Copyright 2009 Hippo.
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
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;

class MenuList extends Panel implements MenuComponent {

    private static final long serialVersionUID = 1L;
    private final List<Component> list;
    private final MenuHierarchy menu;

    MenuList(String id, ActionDescription wf, MenuHierarchy menu) {
        super(id);
        /*if (wf != null) {
            Component fragment = wf.get("text");
            if (fragment instanceof ActionDescription.ActionDisplay) {
                ((ActionDescription.ActionDisplay)fragment).substantiate();
                add(fragment);
            } else if (fragment instanceof Fragment) {
                add(fragment);
            } else {
                // wf.setVisible(true);
            }

            fragment = wf.get("icon");
            if (fragment instanceof ActionDescription.ActionDisplay) {
                ((ActionDescription.ActionDisplay)fragment).substantiate();
                add(fragment);
            } else if (fragment instanceof Fragment) {
                add(fragment);
            }
        } else {
            Label label = new Label("text");
            label.setVisible(false);
            add(label);
            Image image = new Image("icon");
            image.setVisible(false);
            add(image);
        }*/
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
}
