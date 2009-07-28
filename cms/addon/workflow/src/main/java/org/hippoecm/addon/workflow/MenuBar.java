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

import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.behaviors.IContextMenu;

class MenuBar extends Panel implements MenuComponent {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private List<IContextMenu> buttons;

    public MenuBar(String id, MenuHierarchy list) {
        super(id);
        buttons = new LinkedList<IContextMenu>();
        add(new DataView("list", new ListDataProvider(list.list(this))) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Item newItem(String id, int index, IModel model) {
                return super.newItem(String.valueOf(index), index, model);
            }

            @Override
            public void populateItem(final Item item) {
                Component button = (Component) item.getModelObject();
                if (button instanceof MenuButton) {
                    buttons.add((MenuButton) button);
                }
                item.add(button);
            }
        });
    }

    public void collapse(IContextMenu current, AjaxRequestTarget target) {
        for (IContextMenu button : buttons) {
            if (button != current) {
                button.collapse(target);
            }
        }
    }
}
