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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMenuHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMenuItemHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validaters.ChildExistsValidator;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validaters.PreviewWorkspaceNodeValidator;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validaters.Validator;

@Path("/" + HstNodeTypes.NODETYPE_HST_SITEMENU + "/")
@Produces(MediaType.APPLICATION_JSON)
public class SiteMenuResource extends AbstractConfigResource {

    private SiteMenuHelper siteMenuHelper;
    private SiteMenuItemHelper siteMenuItemHelper;

    public void setSiteMenuHelper(final SiteMenuHelper siteMenuHelper) {
        this.siteMenuHelper = siteMenuHelper;
    }

    public void setSiteMenuItemHelper(final SiteMenuItemHelper siteMenuItemHelper) {
        this.siteMenuItemHelper = siteMenuItemHelper;
    }

    @GET
    @Path("/")
    public Response getMenu() {
        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final HstSiteMenuConfiguration menu = getHstSiteMenuConfiguration();
                final SiteMenuRepresentation representation = new SiteMenuRepresentation(menu);
                return ok("Menu item loaded successfully", representation);
            }
        });
    }

    @GET
    @Path("/{menuItemId}")
    public Response getMenuItem(final @PathParam("menuItemId") String menuItemId) {
        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final HstSiteMenuConfiguration menu = getHstSiteMenuConfiguration();
                final HstSiteMenuItemConfiguration menuItem = siteMenuHelper.getMenuItem(menu, menuItemId);
                final SiteMenuItemRepresentation representation = new SiteMenuItemRepresentation(menuItem);
                return ok("Menu item loaded successfully", representation);
            }
        });
    }

    @POST
    @Path("/create/{parentId}")
    public Response create(final @PathParam("parentId") String parentId,
                           final SiteMenuItemRepresentation newMenuItem) {
        List<Validator> preValidators = getDefaultMenuModificationValidators();
        preValidators.add(new PreviewWorkspaceNodeValidator(parentId));
        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final Session session = getHstRequestContextService().getRequestContext().getSession();
                final HstSiteMenuConfiguration menu = getHstSiteMenuConfiguration();
                final Node parentNode = getParentNode(parentId, session, menu);
                Node menuItemNode = siteMenuItemHelper.create(parentNode, newMenuItem);
                return ok("Item created successfully", menuItemNode.getIdentifier());
            }
        }, preValidators);
    }


    @POST
    @Path("/")
    public Response update(final SiteMenuItemRepresentation modifiedItem) {

        List<Validator> preValidators = getDefaultMenuModificationValidators();
        preValidators.add(new PreviewWorkspaceNodeValidator(modifiedItem.getId()));
        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final Session session = getHstRequestContextService().getRequestContext().getSession();
                final Node menuItemNode = session.getNodeByIdentifier(modifiedItem.getId());
                siteMenuItemHelper.update(menuItemNode, modifiedItem);
                return ok("Item updated successfully", modifiedItem.getId());
            }
        }, preValidators);
    }

    @POST
    @Path("/move/{sourceId}/{parentId}")
    public Response move(@PathParam("sourceId") String sourceId,
                         @PathParam("parentId") String parentId) {
        return move(sourceId, parentId, null);
    }

    @POST
    @Path("/move/{sourceId}/{parentId}/{beforeChildId}")
    public Response move(final @PathParam("sourceId") String sourceId,
                         final @PathParam("parentId") String parentId,
                         final @PathParam("beforeChildId") String beforeChildId) {
        List<Validator> preValidators = getDefaultMenuModificationValidators();
        preValidators.add(new PreviewWorkspaceNodeValidator(sourceId));
        preValidators.add(new PreviewWorkspaceNodeValidator(parentId));
        if (StringUtils.isNotBlank(beforeChildId)) {
            preValidators.add(new ChildExistsValidator(parentId, beforeChildId));
        }
        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final Session session = getHstRequestContextService().getRequestContext().getSession();
                final Node parent = session.getNodeByIdentifier(parentId);
                final Node source = session.getNodeByIdentifier(sourceId);

                if (!source.getParent().isSame(parent)) {
                    siteMenuItemHelper.move(source, parent);
                }
                if (StringUtils.isNotBlank(beforeChildId)) {
                    final Node child = session.getNodeByIdentifier(beforeChildId);
                    parent.orderBefore(source.getName(), child.getName());
                } else {
                    parent.orderBefore(source.getName(), null);
                }
                return ok("Item moved successfully", sourceId);
            }
        }, preValidators);
    }

    @POST
    @Path("/delete/{menuItemId}")
    public Response delete(final @PathParam("menuItemId") String menuItemId) {
        List<Validator> preValidators = getDefaultMenuModificationValidators();
        preValidators.add(new PreviewWorkspaceNodeValidator(menuItemId));
        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final Session session = getHstRequestContextService().getRequestContext().getSession();
                session.getNodeByIdentifier(menuItemId).remove();
                return ok("Item deleted successfully", menuItemId);
            }
        }, preValidators);
    }


    private Node getParentNode(String parentId, Session session, HstSiteMenuConfiguration menu) throws RepositoryException {
        final CanonicalInfo menuInfo = getCanonicalInfo(menu);
        if (menuInfo.getCanonicalIdentifier().equals(parentId)) {
            return session.getNodeByIdentifier(parentId);
        } else {
            final HstSiteMenuItemConfiguration targetParentItem = siteMenuHelper.getMenuItem(menu, parentId);
            final CanonicalInfo targetParentItemInfo = getCanonicalInfo(targetParentItem);
            return session.getNodeByIdentifier(targetParentItemInfo.getCanonicalIdentifier());
        }
    }

    private HstSiteMenuConfiguration getHstSiteMenuConfiguration() throws RepositoryException {
        final HstSite editingPreviewHstSite = getHstRequestContextService().getEditingPreviewSite();
        final String menuId = getHstRequestContextService().getRequestConfigIdentifier();
        return siteMenuHelper.getMenu(editingPreviewHstSite, menuId);
    }


    private List<Validator> getDefaultMenuModificationValidators() {
        List<Validator> preValidators = new ArrayList<>();
        final String requestConfigIdentifier = getHstRequestContextService().getRequestConfigIdentifier();
        preValidators.add(new PreviewWorkspaceNodeValidator(requestConfigIdentifier, HstNodeTypes.NODETYPE_HST_SITEMENU));
        return preValidators;
    }

}
