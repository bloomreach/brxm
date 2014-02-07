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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.Form;

class MenuHierarchy implements Serializable {

    private final List<String> categories;
    private Map<String, List<MenuDescription>> menus = new LinkedHashMap<>();
    private Map<String, MenuHierarchy> submenus = new LinkedHashMap<String, MenuHierarchy>();
    private List<ActionDescription> items = new LinkedList<ActionDescription>();

    private final Form form;

    MenuHierarchy(Form form) {
        this(Collections.<String>emptyList(), form);
    }

    MenuHierarchy(final List<String> categories, Form form) {
        this.categories = categories;
        this.form = form;
    }

    public void put(String category, ActionDescription action) {
        if (!submenus.containsKey(category)) {
            submenus.put(category, new MenuHierarchy(form));
        }
        submenus.get(category).put(action);
    }

    public void put(String category, MenuDescription menu) {
        if (!menus.containsKey(category)) {
            menus.put(category, new ArrayList<MenuDescription>(1));
        }
        menus.get(category).add(menu);
    }

    private void put(ActionDescription action) {
        items.add(action);
    }

    public void restructure() {
        Map<String, MenuHierarchy> submenus = this.submenus;
        this.submenus = new LinkedHashMap<String, MenuHierarchy>();
        this.items = new LinkedList<ActionDescription>();
        if (submenus.containsKey("default")) {
            MenuHierarchy submenu = submenus.get("default");
            for (ActionDescription action : submenu.items) {
                if (!action.isVisible()) {
                    continue;
                }
                if (action.getId().startsWith("info")) {
                    // processed in second round
                } else if (action.getId().equals("edit")) {
                    put(action);
                } else if (action.getId().equals("delete")) {
                    put("document", action);
                } else if (action.getId().equals("copy")) {
                    put("document", action);
                } else if (action.getId().equals("move")) {
                    put("document", action);
                } else if (action.getId().equals("rename")) {
                    put("document", action);
                } else if (action.getId().equals("where-used")) {
                    put("document", action);
                } else if (action.getId().equals("history")) {
                    put("document", action);
                } else if (action.getId().equals("docMetaData")) {
                    put("document", action);
                } else if (action.getId().toLowerCase().contains("publi")) {
                    put("publication", action);
                } else if (action.getId().equals("cancel") || action.getId().equals("accept")
                        || action.getId().equals("reject")) {
                    put("request", action);
                } else {
                    put("miscellaneous", action);
                }
            }
        }
        if (submenus.containsKey("editing")) {
            MenuHierarchy submenu = submenus.remove("editing");
            for (ActionDescription action : submenu.items) {
                put(action);
            }
        }
        if (submenus.containsKey("threepane")) {
            MenuHierarchy submenu = submenus.remove("editing");
            for (ActionDescription action : submenu.items) {
                put(action);
            }
        }
        if (submenus.containsKey("versioning")) {
            MenuHierarchy submenu = submenus.remove("versioning");
            for (ActionDescription action : submenu.items) {
                put(action);
            }
        }
        if (submenus.containsKey("default")) {
            MenuHierarchy submenu = submenus.remove("default");
            /* [AC] skipping spacer - not used anywhere yet and it causes aesthetics problems with the workflow toolbar
            put(new ActionDescription("spacer") {
                @Override
                public void invoke() {
                }
            });
            */
            for (ActionDescription action : submenu.items) {
                if (!action.isVisible()) {
                    continue;
                }
                if (action.getId().startsWith("info")) {
                    put(action);
                }
            }
        }
        if (submenus.containsKey("custom")) {
            MenuHierarchy submenu = submenus.remove("custom");
            for (ActionDescription action : submenu.items) {
                put(action);
            }
        }
        for (Map.Entry<String, MenuHierarchy> entry : submenus.entrySet()) {
            MenuHierarchy submenu = new MenuHierarchy(form);
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
        this.submenus = new LinkedHashMap<String, MenuHierarchy>();
        this.items = new LinkedList<ActionDescription>();
        for (MenuHierarchy submenu : submenus.values()) {
            for (ActionDescription action : submenu.items) {
                if (action.isVisible()) {
                    put(action);
                }
            }
        }
    }

    List<Component> list(MenuComponent context) {
        List<Component> list = new LinkedList<Component>();
        if (context instanceof MenuBar) {
            for (ActionDescription item : items) {
                if (!(item.getId().startsWith("info") || item.getId().equals("spacer"))) {
                    MenuAction menuAction = new MenuAction("item", item, form);
                    if (menuAction.isVisible()) {
                        list.add(menuAction);
                    }
                }
            }

            List<String> categories = new ArrayList<String>(this.categories);
            if (categories.contains("default")) {
                categories.remove("default");
                categories.add(0, "publication");
                categories.add(1, "request");
                categories.add(2, "document");
                categories.add(3, "miscellaneous");
            }
            for (String subMenuKey : submenus.keySet()) {
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
                    list.add(new MenuButton("item", category, hierarchy, menus.get(category)));
                } else if (menus.containsKey(category)) {
                    list.add(new MenuButton("item", category, menus.get(category), form));
                } else if (submenus.containsKey(category)) {
                    list.add(new MenuButton("item", category, submenus.get(category)));
                }
            }

            for (ActionDescription item : items) {
                if (item.getId().startsWith("info")) {
                    list.add(new MenuLabel("item", item));
                } else if (item.getId().equals("spacer")) {
                    list.add(new MenuSpacer("item"));
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
}
