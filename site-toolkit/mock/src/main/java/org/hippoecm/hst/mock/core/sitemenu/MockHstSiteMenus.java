/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.mock.core.sitemenu;

import org.hippoecm.hst.core.sitemenu.HstSiteMenu;
import org.hippoecm.hst.core.sitemenu.HstSiteMenus;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Mock implementation of {@link org.hippoecm.hst.core.sitemenu.HstSiteMenus}.
 */
public class MockHstSiteMenus implements HstSiteMenus {

    private final Map<String, HstSiteMenu> menuNamesToSiteMenus = new HashMap<String, HstSiteMenu>();

    public Map<String, HstSiteMenu> getSiteMenus() {
        return Collections.unmodifiableMap(menuNamesToSiteMenus);
    }

    public HstSiteMenu getSiteMenu(String name) {
        return menuNamesToSiteMenus.get(name);
    }

    // Methods supporting org.hippoecm.hst.mock configuration

    public void addSiteMenu(String name, HstSiteMenu menu) {
        menuNamesToSiteMenus.put(name, menu);
    }
}
