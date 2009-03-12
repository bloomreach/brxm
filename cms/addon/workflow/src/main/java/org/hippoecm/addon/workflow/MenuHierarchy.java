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
    }

    List<Component> list(int context) {
        List<Component> list = new LinkedList<Component>();
        switch (context) {
            case 0:
                for (Map.Entry<String, MenuHierarchy> submenu : submenus.entrySet()) {
                    list.add(new MenuButton("item", submenu.getKey(), submenu.getValue()));
                }
                break;
            case 1:
                for(ActionDescription item : items) {
                    list.add(new MenuItem("item", item));
                }
                break;
        }
        return list;
    }
}
