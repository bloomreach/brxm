/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.util.ISO9075;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;

public class SiteMenuHelper extends AbstractHelper {

    @Override
    public <T> T getConfigObject(final String itemId) {
        final HstSite editingPreviewSite = pageComposerContextService.getEditingPreviewSite();
        return (T) getMenu(editingPreviewSite, itemId);
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
        final String msg = "%s with id '%s' is not part of currently edited preview site.";
        throw new ClientException(ClientError.ITEM_NOT_IN_PREVIEW, msg, "Site menu", menuId);
    }

    public HstSiteMenuItemConfiguration getMenuItem(HstSiteMenuConfiguration menu, String menuItemId) {
        for (HstSiteMenuItemConfiguration itemConfiguration : menu.getSiteMenuConfigurationItems()) {
            HstSiteMenuItemConfiguration menuItem = getMenuItem(itemConfiguration, menuItemId);
            if (menuItem != null) {
                return menuItem;
            }
        }
        final String msg = "%s with id '%s' is not part of currently edited preview site.";
        throw new ClientException(ClientError.ITEM_NOT_IN_PREVIEW, msg, "Site menu item", menuItemId);
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


    @Override
    protected void unlock(final Node node) throws RepositoryException {
        // site menus never have descendant sitemenu items locked
        return;
    }

    @Override
    protected String buildXPathQueryLockedWorkspaceNodesForUsers(final String previewWorkspacePath,
                                                                 final List<String> userIds) {
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("List of user IDs cannot be empty");
        }

        StringBuilder xpath = new StringBuilder("/jcr:root");
        xpath.append(ISO9075.encodePath(previewWorkspacePath + "/" + HstNodeTypes.NODENAME_HST_SITEMENUS));
        // /element to get direct children below pages and *not* //element
        xpath.append("/element(*,");
        xpath.append(HstNodeTypes.NODETYPE_HST_SITEMENU);
        xpath.append(")[");

        String concat = "";
        for (String userId : userIds) {
            xpath.append(concat);
            xpath.append('@');
            xpath.append(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY);
            xpath.append(" = '");
            xpath.append(userId);
            xpath.append("'");
            concat = " or ";
        }
        xpath.append("]");

        return xpath.toString();
    }

}
