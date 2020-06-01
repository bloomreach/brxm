/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;

class MenuVisitor implements IVisitor<Panel, Void> {

    private final MenuHierarchy menu;
    private final String category;

    public MenuVisitor(final MenuHierarchy menu, final String category) {
        this.menu = menu;
        this.category = category;
    }

    public void component(Panel component, IVisit<Void> visit) {
        if (component instanceof ActionDescription) {
            ActionDescription action = (ActionDescription) component;
            String subMenu = action.getSubMenu();
            if (subMenu == null) {
                menu.put(category, action);
            } else {
                menu.put(subMenu, action);
            }
        } else if (component instanceof MenuDescription) {
            menu.put(category, (MenuDescription) component);
        }
    }
}
