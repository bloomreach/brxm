/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard.model.hst;

import java.util.ArrayList;
import java.util.List;

import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentCollection;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
@PersistentNode(type = "hst:sitemenus")
public class HstSiteMenus extends BaseJcrModel{

    @PersistentCollection
    private List<HstSiteMenu> menus = new ArrayList<>();

    public HstSiteMenus() {
        setName("hst:sitemenus");
    }

    public void addMenu(final HstSiteMenu menu){
        menu.setParentPath(getParentPath() +'/'+getName());
        menus.add(menu);
    }

    public List<HstSiteMenu> getMenus() {
        return menus;
    }

    public void setMenus(final List<HstSiteMenu> menus) {
        this.menus = menus;
    }
}
