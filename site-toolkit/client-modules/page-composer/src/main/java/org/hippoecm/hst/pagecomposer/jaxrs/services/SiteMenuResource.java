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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
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
    public Response getMenu(@Context HttpServletRequest servletRequest) {
        try {
            final HstRequestContext requestContext = getRequestContext(servletRequest);
            final HstSiteMenuConfiguration menu = getHstSiteMenuConfiguration(requestContext);
            final SiteMenuRepresentation representation = new SiteMenuRepresentation().represent(menu);
            return ok("Menu item loaded successfully", representation);
        } catch (RepositoryException e) {
            return logAndReturnError(e.getMessage());
        } catch (IllegalStateException e) {
            return logAndReturnClientError(e.getMessage());
        }
    }

    @GET
    @Path("/{menuItemId}")
    public Response getMenuItem(@Context HttpServletRequest servletRequest,
                                @PathParam("menuItemId") String menuItemId) {
        try {
            final HstRequestContext requestContext = getRequestContext(servletRequest);
            final HstSiteMenuConfiguration menu = getHstSiteMenuConfiguration(requestContext);
            final HstSiteMenuItemConfiguration menuItem = siteMenuHelper.getMenuItem(menu, menuItemId);
            final SiteMenuItemRepresentation representation = new SiteMenuItemRepresentation().represent(menuItem);
            return ok("Menu item loaded successfully", representation);
        } catch (RepositoryException e) {
            return logAndReturnError(e.getMessage());
        } catch (IllegalStateException e) {
            return logAndReturnClientError(e.getMessage());
        }
    }

    @POST
    @Path("/update")
    public Response update(@Context HttpServletRequest servletRequest, SiteMenuItemRepresentation newMenuItem) {
        try {
            final HstRequestContext requestContext = getRequestContext(servletRequest);
            final Session session = requestContext.getSession();

            final HstSiteMenuConfiguration menu = getHstSiteMenuConfiguration(requestContext);
            final String itemId = newMenuItem.getId();
            final HstSiteMenuItemConfiguration menuItem = siteMenuHelper.getMenuItem(menu, itemId);
            final SiteMenuItemRepresentation currentMenuItem = new SiteMenuItemRepresentation().represent(menuItem);

            final Node menuItemNode = session.getNodeByIdentifier(currentMenuItem.getId());
            siteMenuItemHelper.update(menuItemNode, currentMenuItem, newMenuItem);
            HstConfigurationUtils.persistChanges(session);

            return ok("Item updated successfully", itemId);
        } catch (RepositoryException e) {
            return logAndReturnError(e.getMessage());
        } catch (IllegalStateException e) {
            return logAndReturnClientError(e.getMessage());
        }
    }


    @POST
    @Path("/move/{sourceId}/{parentTargetId}")
    public Response move(@Context HttpServletRequest servletRequest,
                         @PathParam("sourceId") String sourceId,
                         @PathParam("parentTargetId") String parentTargetId) {
        return move(servletRequest, sourceId, parentTargetId, null);
    }

    @POST
    @Path("/move/{sourceId}/{parentTargetId}/{childTargetId}")
    public Response move(@Context HttpServletRequest servletRequest,
                         @PathParam("sourceId") String sourceId,
                         @PathParam("parentTargetId") String parentTargetId,
                         @PathParam("childTargetId") String childTargetId) {
        try {
            final HstRequestContext requestContext = getRequestContext(servletRequest);
            final Session session = requestContext.getSession();

            final HstSiteMenuConfiguration menu = getHstSiteMenuConfiguration(requestContext);
            final Node parent = getParentNode(parentTargetId, session, menu);

            final HstSiteMenuItemConfiguration sourceItem = siteMenuHelper.getMenuItem(menu, sourceId);
            assertCanonicalInfoInstance(sourceItem);
            final Node source = session.getNodeByIdentifier(((CanonicalInfo) sourceItem).getCanonicalIdentifier());

            if (!parentTargetId.equals(source.getParent().getIdentifier())) {
                siteMenuItemHelper.move(source, parent);
            }
            if (StringUtils.isNotBlank(childTargetId)) {
                final HstSiteMenuItemConfiguration targetChildItem = siteMenuHelper.getMenuItem(menu, childTargetId);
                assertCanonicalInfoInstance(targetChildItem);
                final Node child = session.getNodeByIdentifier(((CanonicalInfo) targetChildItem).getCanonicalIdentifier());
                parent.orderBefore(source.getName(), child.getName());
            } else {
                parent.orderBefore(source.getName(), null);
            }
            HstConfigurationUtils.persistChanges(session);
            return ok("Item moved successfully", sourceId);

        } catch (RepositoryException e) {
            return logAndReturnError(e.getMessage());
        } catch (IllegalStateException e) {
            return logAndReturnClientError(e.getMessage());
        }
    }

    @POST
    @Path("/delete/{menuItemId}")
    public Response delete(@Context HttpServletRequest servletRequest,
                           @PathParam("menuItemId") String menuItemId) {
        try {
            final HstRequestContext requestContext = getRequestContext(servletRequest);
            final Session session = requestContext.getSession();

            final HstSiteMenuConfiguration menu = getHstSiteMenuConfiguration(requestContext);

            final HstSiteMenuItemConfiguration sourceItem = siteMenuHelper.getMenuItem(menu, menuItemId);

            assertCanonicalInfoInstance(sourceItem);
            final Node source = session.getNodeByIdentifier(((CanonicalInfo) sourceItem).getCanonicalIdentifier());
            source.getSession().removeItem(source.getPath());
            HstConfigurationUtils.persistChanges(session);
            return ok("Item deleted successfully", menuItemId);

        } catch (RepositoryException e) {
            return logAndReturnError(e.getMessage());
        } catch (IllegalStateException e) {
            return logAndReturnClientError(e.getMessage());
        }
    }

    private Node getParentNode(String parentTargetId, Session session, HstSiteMenuConfiguration menu) throws RepositoryException {
        assertCanonicalInfoInstance(menu);
        if (((CanonicalInfo) menu).getCanonicalIdentifier().equals(parentTargetId)) {
            return session.getNodeByIdentifier(parentTargetId);
        } else {
            final HstSiteMenuItemConfiguration targetParentItem = siteMenuHelper.getMenuItem(menu, parentTargetId);
            assertCanonicalInfoInstance(targetParentItem);
            return session.getNodeByIdentifier(((CanonicalInfo) targetParentItem).getCanonicalIdentifier());
        }
    }

    private HstSiteMenuConfiguration getHstSiteMenuConfiguration(final HstRequestContext requestContext) throws RepositoryException {
        final HstSite editingPreviewHstSite = siteMenuHelper.getEditingPreviewHstSite(getEditingPreviewSite(requestContext));
        final String menuId = getRequestConfigIdentifier(requestContext);
        return siteMenuHelper.getMenu(editingPreviewHstSite, menuId);
    }

    private Response logAndReturnError(String errorMessage) {
        log.error(errorMessage);
        return error(errorMessage);
    }

    private Response logAndReturnClientError(String errorMessage) {
        log.warn(errorMessage);
        final ExtResponseRepresentation entity = new ExtResponseRepresentation();
        entity.setSuccess(false);
        entity.setMessage(errorMessage);
        return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
    }

    private void assertCanonicalInfoInstance(final Object o) throws IllegalStateException {
        if (!(o instanceof CanonicalInfo)) {
            throw new IllegalStateException("HstSiteMenuItemConfiguration not instanceof CanonicalInfo");
        }
    }

}
