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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/hst:sitemenu/")
@Produces(MediaType.APPLICATION_JSON)
public class SiteMenuResource extends AbstractConfigResource {

    private static Logger log = LoggerFactory.getLogger(SiteMenuResource.class);

    private final SiteMenuHelper siteMenuHelper;
    private final SiteMenuItemHelper siteMenuItemHelper;

    public SiteMenuResource(final SiteMenuHelper siteMenuHelper, final SiteMenuItemHelper siteMenuItemHelper) {
        this.siteMenuHelper = siteMenuHelper;
        this.siteMenuItemHelper = siteMenuItemHelper;
    }

    public SiteMenuResource() {
        this(new SiteMenuHelper(), new SiteMenuItemHelper());
    }

    @GET
    @Path("/")
    public Response getPageModelRepresentationV2(@Context HttpServletRequest servletRequest) {
        try {
            final HstRequestContext requestContext = getRequestContext(servletRequest);

            final HstSiteMenuConfiguration menuConfig = getHstSiteMenuConfiguration(requestContext);
            final SiteMenuRepresentation siteMenuRepresentation = new SiteMenuRepresentation().represent(menuConfig);

            return ok("Menu loaded successfully", siteMenuRepresentation);
        } catch (RepositoryException e) {
            return logAndReturnError(e.getMessage());
        }
    }

    @POST
    @Path("/update")
    public Response update(@Context HttpServletRequest servletRequest, SiteMenuItemRepresentation newMenuItem) {
        try {
            final HstRequestContext requestContext = getRequestContext(servletRequest);
            final Session session = requestContext.getSession();

            final HstSiteMenuConfiguration menuConfig = getHstSiteMenuConfiguration(requestContext);
            final String itemId = newMenuItem.getId();
            final HstSiteMenuItemConfiguration menuItemConfig = siteMenuHelper.getMenuItemConfig(itemId, menuConfig);
            final SiteMenuItemRepresentation currentMenuItem = new SiteMenuItemRepresentation().represent(menuItemConfig, menuItemConfig.getParentItemConfiguration());

            final Node menuItemNode = session.getNodeByIdentifier(currentMenuItem.getId());
            siteMenuItemHelper.update(menuItemNode, currentMenuItem, newMenuItem);
            HstConfigurationUtils.persistChanges(session);

            // TODO (meggermont): Get the menu item from the repository instead of echoing the client's representation
            // I tried the following, but it seems that I query the stale model.
            // final HstSiteMenuConfiguration updatedMenuConfig = getHstSiteMenuConfiguration(requestContext);
            // final SiteMenuItemRepresentation updatedMenuItem = new SiteMenuItemRepresentation().represent(SiteMenuHelper.getMenuItemConfig(itemId, updatedMenuConfig));
            return ok("Item updated successfully", newMenuItem);
        } catch (RepositoryException e) {
            // TODO (meggermont): should we always return 500?
            return logAndReturnError(e.getMessage());
        }
    }


    private HstSiteMenuConfiguration getHstSiteMenuConfiguration(final HstRequestContext requestContext) throws RepositoryException {
        final HstSite editingPreviewHstSite = siteMenuHelper.getEditingPreviewHstSite(getEditingPreviewSite(requestContext));
        final String menuId = getRequestConfigIdentifier(requestContext);
        return siteMenuHelper.getMenuConfig(menuId, editingPreviewHstSite);
    }

    private Response logAndReturnError(String errorMessage) {
        log.error(errorMessage);
        return error(errorMessage);
    }


}
