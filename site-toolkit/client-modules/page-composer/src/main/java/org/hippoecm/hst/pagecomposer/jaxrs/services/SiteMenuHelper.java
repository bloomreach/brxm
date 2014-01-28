/*
 * Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.Map;

import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;

class SiteMenuHelper {

    public HstSite getEditingPreviewHstSite(HstSite editingPreviewHstSite) throws IllegalStateException {
        if (editingPreviewHstSite == null) {
            throw new IllegalStateException("Could not get the editing site to create the page model representation.");
        }
        return editingPreviewHstSite;
    }

    public HstSiteMenuConfiguration getMenu(HstSite site, String menuId) {
        final Map<String,HstSiteMenuConfiguration> siteMenuConfigurations = site.getSiteMenusConfiguration().getSiteMenuConfigurations();
        for (HstSiteMenuConfiguration menuConfiguration : siteMenuConfigurations.values()) {
            if (!(menuConfiguration instanceof CanonicalInfo)) {
                continue;
            }
            if (((CanonicalInfo)menuConfiguration).getCanonicalIdentifier().equals(menuId)) {
                return menuConfiguration;
            }
        }
        throw new IllegalStateException(String.format("Site menu with id '%s' is not part of currently edited preview site.", menuId));
    }

    public HstSiteMenuItemConfiguration getMenuItem(HstSiteMenuConfiguration menu, String menuItemId) {
        for (HstSiteMenuItemConfiguration itemConfiguration : menu.getSiteMenuConfigurationItems()) {
            HstSiteMenuItemConfiguration menuItem = getMenuItem(itemConfiguration, menuItemId);
            if (menuItem != null) {
                return menuItem;
            }
        }
        throw new IllegalStateException(String.format("Site menu item with id '%s' is not part of currently edited preview site.", menuItemId));
    }

    private HstSiteMenuItemConfiguration getMenuItem(HstSiteMenuItemConfiguration menuItem, String menuItemId) {
        if (!(menuItem instanceof CanonicalInfo)) {
            return null;
        }
        if (((CanonicalInfo)menuItem).getCanonicalIdentifier().equals(menuItemId)) {
            return menuItem;
        }
        for (HstSiteMenuItemConfiguration child : menuItem.getChildItemConfigurations()) {
            HstSiteMenuItemConfiguration o = getMenuItem(child, menuItemId);
            if (o != null) {
                return o;
            }
        }
        return null;
    }

}
