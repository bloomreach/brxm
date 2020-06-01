/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.helpers;

import java.util.Map;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_SITEMENU;

public class SiteMenuHelper extends AbstractHelper {

    @SuppressWarnings("unchecked")
    @Override
    public HstSiteMenuConfiguration getConfigObject(final String itemId) {
        final HstSite editingPreviewSite = pageComposerContextService.getEditingPreviewSite();
        return getMenu(editingPreviewSite, itemId);
    }

    @SuppressWarnings("unchecked")
    @Override
    public HstSiteMenuConfiguration getConfigObject(final String itemId, final Mount mount) {
        return getMenu(mount.getHstSite(), itemId);
    }

    @Override
    protected String getNodeType() {
        return NODETYPE_HST_SITEMENU;
    }

    public HstSiteMenuConfiguration getMenu(HstSite site, String menuId) {
        final Map<String, HstSiteMenuConfiguration> siteMenuConfigurations = site.getSiteMenusConfiguration().getSiteMenuConfigurations();
        for (HstSiteMenuConfiguration menuConfiguration : siteMenuConfigurations.values()) {
            if (!(menuConfiguration instanceof CanonicalInfo)) {
                continue;
            }
            if (((CanonicalInfo) menuConfiguration).getCanonicalIdentifier().equals(menuId)) {
                return menuConfiguration;
            }
        }
        final String message = String.format("%s with id '%s' is not part of currently edited preview site.", "Site menu", menuId);
        throw new ClientException(message, ClientError.ITEM_NOT_IN_PREVIEW);
    }

    public HstSiteMenuItemConfiguration getMenuItem(HstSiteMenuConfiguration menu, String menuItemId) {
        for (HstSiteMenuItemConfiguration itemConfiguration : menu.getSiteMenuConfigurationItems()) {
            HstSiteMenuItemConfiguration menuItem = getMenuItem(itemConfiguration, menuItemId);
            if (menuItem != null) {
                return menuItem;
            }
        }
        final String message = String.format("%s with id '%s' is not part of currently edited preview site.", "Site menu item", menuItemId);
        throw new ClientException(message, ClientError.ITEM_NOT_IN_PREVIEW);
    }

    private HstSiteMenuItemConfiguration getMenuItem(HstSiteMenuItemConfiguration menuItem, String menuItemId) {
        if (!(menuItem instanceof CanonicalInfo)) {
            return null;
        }
        if (((CanonicalInfo) menuItem).getCanonicalIdentifier().equals(menuItemId)) {
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
