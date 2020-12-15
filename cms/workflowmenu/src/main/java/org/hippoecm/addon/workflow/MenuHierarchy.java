/*
 *  Copyright 2009-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Form;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class MenuHierarchy implements Serializable {

    private final List<String> categories;
    private final List<String> submenuOrder;
    private final Map<String, List<MenuDescription>> menus = new LinkedHashMap<>();
    private final Form<?> form;
    private final IPluginConfig config;

    private Map<String, MenuHierarchy> submenus = new LinkedHashMap<>();
    private List<ActionDescription> items = new LinkedList<>();

    MenuHierarchy(final Form<?> form, final IPluginConfig config) {
        this(Collections.emptyList(), form, config);
    }

    MenuHierarchy(final List<String> categories, final Form<?> form, final IPluginConfig config) {
        this(categories, categories, form, config);
    }

    MenuHierarchy(final List<String> categories, final List<String> submenuOrder, final Form<?> form, final IPluginConfig config) {
        this.categories = categories;
        this.submenuOrder = submenuOrder;
        this.form = form;
        this.config = config;
    }

    public void put(String category, ActionDescription action) {
        if (!submenus.containsKey(category)) {
            submenus.put(category, new MenuHierarchy(form, this.config));
        }
        submenus.get(category).put(action);
    }

    public void put(String category, MenuDescription menu) {
        if (!menus.containsKey(category)) {
            menus.put(category, new ArrayList<>(1));
        }
        menus.get(category).add(menu);
    }

    private void put(ActionDescription action) {
        if (!(items.contains(action) && action.getId().startsWith("info"))) {
            items.add(action);
        }
    }

    public void restructure() {
        Map<String, MenuHierarchy> submenus = this.submenus;
        this.submenus = new LinkedHashMap<>();
        this.items = new LinkedList<>();
        if (submenus.containsKey("top")) {
            MenuHierarchy submenu = submenus.remove("top");
            for (ActionDescription action : submenu.items) {
                put(action);
            }
        }
        for (Map.Entry<String, MenuHierarchy> entry : submenus.entrySet()) {
            MenuHierarchy submenu = new MenuHierarchy(form, this.config);
            for (ActionDescription action : entry.getValue().items) {
                if (action.isVisible()) {
                    submenu.put(action);
                }
            }
            if (submenu.items.size() > 0) {
                this.submenus.put(entry.getKey(), submenu);
            }
        }
    }

    public void flatten() {
        Map<String, MenuHierarchy> submenus = this.submenus;
        this.submenus = new LinkedHashMap<>();
        this.items = new LinkedList<>();
        for (MenuHierarchy submenu : submenus.values()) {
            for (ActionDescription action : submenu.items) {
                if (action.isVisible()) {
                    put(action);
                }
            }
        }
    }

    List<Component> list(MenuComponent context) {
        List<Component> list = new LinkedList<>();
        if (context instanceof MenuBar) {
            for (ActionDescription item : items) {
                if (!(item.getId().startsWith("info"))) {
                    MenuAction menuAction = new MenuAction("item", item, form);
                    if (menuAction.isVisible()) {
                        list.add(menuAction);
                    }
                }
            }

            List<String> categories = new ArrayList<>(submenuOrder);
            categories.remove("default");

            for (String subMenuKey : submenus.keySet()) {
                if ("info".equals(subMenuKey)) {
                    continue;
                }
                if (!categories.contains(subMenuKey)) {
                    categories.add(subMenuKey);
                }
            }
            for (String menuKey : menus.keySet()) {
                if (!categories.contains(menuKey)) {
                    categories.add(menuKey);
                }
            }
            for (String category : categories) {
                if (menus.containsKey(category) && submenus.containsKey(category)) {
                    MenuHierarchy hierarchy = submenus.get(category);
                    list.add(new MenuButton("item", category, hierarchy, menus.get(category), this.config));
                } else if (menus.containsKey(category)) {
                    list.add(new MenuButton("item", category, menus.get(category), form, this.config));
                } else if (submenus.containsKey(category)) {
                    list.add(new MenuButton("item", category, submenus.get(category), this.config));
                }
            }

            if (submenus.containsKey("info")) {
                MenuHierarchy info = submenus.get("info");
                for (ActionDescription item : info.items) {
                    if (item.getId().startsWith("info")) {
                        list.add(new MenuLabel("item", item));
                    }
                }
            }
        } else {
            for (ActionDescription item : items) {
                list.add(new MenuItem("item", item, form));
            }
        }
        return list;
    }

    public void clear() {
        items.clear();
        menus.clear();
        submenus.clear();
    }

    public MenuHierarchy getSubmenu(final String category) {
        return submenus.get(category);
    }

    public List<ActionDescription> getItems() {
        return items;
    }
}
