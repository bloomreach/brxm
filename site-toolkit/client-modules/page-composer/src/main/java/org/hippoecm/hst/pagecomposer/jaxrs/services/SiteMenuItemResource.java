/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/hst:sitemenuitem/")
public class SiteMenuItemResource extends AbstractConfigResource {
    private static Logger log = LoggerFactory.getLogger(SiteMenuItemResource.class);

    @GET
    @Path("/{menuId}/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPageModelRepresentation(@Context HttpServletRequest servletRequest,
                                               @PathParam("menuId") String menuId) {
        try {
            final HstRequestContext requestContext = getRequestContext(servletRequest);
            final HstSite editingPreviewHstSite = getEditingPreviewSite(requestContext);
            if (editingPreviewHstSite == null) {
                log.error("Could not get the editing site to create the page model representation.");
                return error("Could not get the editing site to create the page model representation.");
            }

            HstSiteMenuItemConfiguration menuItem = getMenuItem(editingPreviewHstSite, menuId, requestContext);
            if (menuItem == null) {
                log.warn("Sitemenuitem with id '{}' is not part of currently editted preview site.", getRequestConfigIdentifier(requestContext));
                return error("Sitemenuitem is not part of currently edited preview site.");
            }

            SiteMenuItemRepresentation siteMenuItemRepresentation = new SiteMenuItemRepresentation();
            siteMenuItemRepresentation.represent(menuItem);
            return ok("Menu loaded successfully", siteMenuItemRepresentation);
        } catch (Exception e) {
            log.warn("Failed to retrieve menu item.", e);
            return error("Failed to retrieve menuitem : " + e.toString());
        }
    }

    /**
     * @return the {@link HstSiteMenuConfiguration} from currently editted preview hst site and when not found, return null
     */
    private HstSiteMenuItemConfiguration getMenuItem(final HstSite editingPreviewHstSite,
                                                                           final String menuId,
                                                                   final HstRequestContext requestContext) {

        HstSiteMenuConfiguration menu = getMenu(editingPreviewHstSite, menuId);
        if (menu == null) {
            return null;
        }

        final String menuItemId = getRequestConfigIdentifier(requestContext);

        return getMenuItem(menu, menuItemId);
    }

    /**
     * @return HstSiteMenuConfiguration for <code>menuId</code> if it is part of <code>editingPreviewHstSite</code> and otherwise
     * <code>null</code>
     */
    private HstSiteMenuConfiguration getMenu(final HstSite editingPreviewHstSite,
                                                                   final String menuId) {
        final Map<String,HstSiteMenuConfiguration> siteMenuConfigurations = editingPreviewHstSite.getSiteMenusConfiguration().getSiteMenuConfigurations();
        for (HstSiteMenuConfiguration menuConfiguration : siteMenuConfigurations.values()) {
            if (menuConfiguration.getCanonicalIdentifier().equals(menuId)) {
                return menuConfiguration;
            }
        }
        return null;
    }

    private HstSiteMenuItemConfiguration getMenuItem(final HstSiteMenuConfiguration menu, final String menuItemId) {
        for (HstSiteMenuItemConfiguration itemConfiguration : menu.getSiteMenuConfigurationItems()) {
            HstSiteMenuItemConfiguration menuItem = getMenuItem(itemConfiguration, menuItemId);
            if (menuItem != null) {
                return menuItem;
            }
        }
        return null;
    }

    private HstSiteMenuItemConfiguration getMenuItem(final HstSiteMenuItemConfiguration menuItem, final String menuItemId) {
        if (menuItem.getCanonicalIdentifier().equals(menuItemId)) {
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
