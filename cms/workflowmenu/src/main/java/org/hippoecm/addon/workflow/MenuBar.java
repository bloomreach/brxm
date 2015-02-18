/*
 *  Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
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

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.behaviors.IContextMenu;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;

class MenuBar extends Panel implements MenuComponent {

    private List<IContextMenu> buttons;

    public MenuBar(String id, MenuHierarchy list) {
        super(id);
        buttons = new LinkedList<>();

        setRenderBodyOnly(true);

        final List<Component> all = list.list(this);
        final List<Component> labels = all.stream().filter(component -> component instanceof MenuLabel).collect(Collectors.toList());
        final List<Component> menus = all.stream().filter(component -> !labels.contains(component)).collect(Collectors.toList());

        add(new ListView("list", new ListDataProvider<>(menus), "menu-item") {
            @Override
            public void populateItem(final Item<Component> item) {
                Component component = item.getModelObject();
                if (component instanceof MenuButton) {
                    buttons.add((MenuButton) component);
                }
                super.populateItem(item);
            }
        });

        WebMarkupContainer statusContainer = new WebMarkupContainer("hippo-toolbar-status");
        statusContainer.setVisible(!labels.isEmpty());
        statusContainer.add(new ListView("status", new ListDataProvider<>(labels), "menu-label-item"));
        add(statusContainer);
    }

    public void collapse(IContextMenu current, AjaxRequestTarget target) {
        for (IContextMenu button : buttons) {
            if (button != current) {
                button.collapse(target);
            }
        }
    }

    private static class ListView extends DataView<Component> {

        private final String cssClass;

        protected ListView(final String id, final IDataProvider<Component> dataProvider, final String cssClass) {
            super(id, dataProvider);
            this.cssClass = cssClass;
        }

        @Override
        protected Item<Component> newItem(String id, int index, IModel<Component> model) {
            return super.newItem(String.valueOf(index), index, model);
        }

        @Override
        protected void populateItem(final Item<Component> item) {
            final Component component = item.getModelObject();
            item.add(CssClass.append(cssClass));
            item.add(component);
        }
    }
}
