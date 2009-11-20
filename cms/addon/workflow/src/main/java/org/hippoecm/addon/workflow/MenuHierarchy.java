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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;

class MenuHierarchy {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private Map<String, MenuHierarchy> submenus = new LinkedHashMap<String, MenuHierarchy>();
    private List<ActionDescription> items = new LinkedList<ActionDescription>();

    MenuHierarchy() {
    }

    public void put(String[] classifiers, ActionDescription action) {
        if (!submenus.containsKey(classifiers[0])) {
            submenus.put(classifiers[0], new MenuHierarchy());
        }
        submenus.get(classifiers[0]).put(action);
    }

    private void put(ActionDescription action) {
        items.add(action);
    }

    public void restructure() {
        Map<String, MenuHierarchy> submenus = this.submenus;
        this.submenus = new LinkedHashMap<String, MenuHierarchy>();
        this.items = new LinkedList<ActionDescription>();
        if(submenus.containsKey("default")) {
            MenuHierarchy submenu = submenus.get("default");
            for(ActionDescription action : submenu.items) {
                if(!action.isVisible()) {
                    continue;
                }
                if(action.getId().startsWith("info")) {
                    // processed in second round
                } else if(action.getId().equals("edit")) {
                    put(action);
                } else if(action.getId().equals("delete")) {
                    put(new String[] { "document" }, action);
                } else if(action.getId().equals("copy")) {
                    put(new String[] { "document" }, action);
                } else if(action.getId().equals("move")) {
                    put(new String[] { "document" }, action);
                } else if(action.getId().equals("rename")) {
                    put(new String[] { "document" }, action);
                } else if(action.getId().equals("where-used")) {
                    put(new String[] { "document" }, action);
                } else if(action.getId().equals("history")) {
                    put(new String[] { "document" }, action);
                } else if(action.getId().toLowerCase().contains("publi")) {
                    put(new String[] { "publication" }, action);
                } else if(action.getId().equals("cancel") || action.getId().equals("accept") || action.getId().equals("reject")) {
                    put(new String[] { "request" }, action);
                } else {
                    put(new String[] { "miscellaneous" }, action);
                }
            }
        }
        if(submenus.containsKey("editing")) {
            MenuHierarchy submenu = submenus.get("editing");
            for(ActionDescription action : submenu.items) {
                put(action);
            }
        }
        if(submenus.containsKey("threepane")) {
            MenuHierarchy submenu = submenus.get("editing");
            for(ActionDescription action : submenu.items) {
                put(action);
            }
        }
        if(submenus.containsKey("versioning")) {
            MenuHierarchy submenu = submenus.get("versioning");
            for(ActionDescription action : submenu.items) {
                put(action);
            }
        }
        if(submenus.containsKey("default")) {
            MenuHierarchy submenu = submenus.get("default");
            /* [AC] skipping spacer - not used anywhere yet and it causes esthetics problems with the workflow toolbar
            put(new ActionDescription("spacer") {
                @Override
                public void invoke() {
                }
            });
            */
            for(ActionDescription action : submenu.items) {
                if(!action.isVisible()) {
                    continue;
                }
                if(action.getId().startsWith("info")) {
                    put(action);
                }
            }
        }
        if(submenus.containsKey("custom")) {
            MenuHierarchy submenu = submenus.get("custom");
            for(ActionDescription action : submenu.items) {
                put(action);
            }
        }
    }

    public void flatten() {
        Map<String, MenuHierarchy> submenus = this.submenus;
        this.submenus = new LinkedHashMap<String, MenuHierarchy>();
        this.items = new LinkedList<ActionDescription>();
        for(MenuHierarchy submenu : submenus.values()) {
            for(ActionDescription action : submenu.items) {
                if(action.isVisible()) {
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
                    MenuAction menuAction = new MenuAction("item", item);
                    if (menuAction.isVisible()) {
                        list.add(menuAction);
                    }
                }
            }
            for (Map.Entry<String, MenuHierarchy> submenu : submenus.entrySet()) {
                list.add(new MenuButton("item", submenu.getKey(), submenu.getValue()));
            }
            for (ActionDescription item : items) {
                if (item.getId().startsWith("info")) {
                    list.add(new MenuLabel("item", item));
                } else if(item.getId().equals("spacer")) {
                    list.add(new MenuSpacer("item"));
                }
            }
        } else {
            for (ActionDescription item : items) {
                list.add(new MenuItem("item", item));
            }
        }
        return list;
    }

    int size(MenuComponent context) {
        if (context instanceof MenuBar) {
            return items.size() + submenus.size();
        } else {
            return items.size();
        }
    }
}
