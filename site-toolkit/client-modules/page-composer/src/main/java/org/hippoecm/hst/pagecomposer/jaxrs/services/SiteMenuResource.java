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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.Position;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMenuHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMenuItemHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.NodePathPrefixValidator;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.NotNullValidator;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.Validator;

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
                           final @DefaultValue(Position.LAST_AS_STRING) @QueryParam("position") String position,
                           final SiteMenuItemRepresentation newMenuItem) {
        List<Validator> preValidators = getDefaultMenuModificationValidators();
        preValidators.add(new NotNullValidator(newMenuItem.getName(), ClientError.ITEM_NO_NAME));
        preValidators.add(new NodePathPrefixValidator(getPreviewConfigurationWorkspacePath(),
                parentId, null));
        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final Session session = getPageComposerContextService().getRequestContext().getSession();
                final HstSiteMenuConfiguration menu = getHstSiteMenuConfiguration();
                final Node parentNode = getParentNode(parentId, session, menu);
                Node menuItemNode = siteMenuItemHelper.create(parentNode, newMenuItem, Position.fromString(position));
                return ok("Item created successfully", menuItemNode.getIdentifier());
            }
        }, preValidators);
    }


    @POST
    @Path("/")
    public Response update(final SiteMenuItemRepresentation modifiedItem) {

        List<Validator> preValidators = getDefaultMenuModificationValidators();
        preValidators.add(new NotNullValidator(modifiedItem.getName(), ClientError.ITEM_NO_NAME));
        preValidators.add(new NodePathPrefixValidator(getPreviewConfigurationWorkspacePath(),
                modifiedItem.getId(), null));
        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final Session session = getPageComposerContextService().getRequestContext().getSession();
                final Node menuItemNode = session.getNodeByIdentifier(modifiedItem.getId());
                siteMenuItemHelper.update(menuItemNode, modifiedItem);
                return ok("Item updated successfully", modifiedItem.getId());
            }
        }, preValidators);
    }

    @POST
    @Path("/move/{sourceId}/{parentId}/{childIndex}")
    public Response move(final @PathParam("sourceId") String sourceId,
                         final @PathParam("parentId") String parentId,
                         final @PathParam("childIndex") Integer childIndex) {

        List<Validator> preValidators = getDefaultMenuModificationValidators();
        preValidators.add(new NodePathPrefixValidator(getPreviewConfigurationWorkspacePath(), sourceId, null));
        preValidators.add(new NodePathPrefixValidator(getPreviewConfigurationWorkspacePath(), parentId, null));
        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final Session session = getPageComposerContextService().getRequestContext().getSession();
                final Node parent = session.getNodeByIdentifier(parentId);
                final Node source = session.getNodeByIdentifier(sourceId);
                siteMenuItemHelper.move(parent, source, childIndex);
                return ok("Item moved successfully", sourceId);
            }
        }, preValidators);
    }

    @POST
    @Path("/delete/{menuItemId}")
    public Response delete(final @PathParam("menuItemId") String menuItemId) {
        List<Validator> preValidators = getDefaultMenuModificationValidators();
        preValidators.add(new NodePathPrefixValidator(getPreviewConfigurationWorkspacePath(), menuItemId, null));
        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final Session session = getPageComposerContextService().getRequestContext().getSession();
                siteMenuItemHelper.delete(session.getNodeByIdentifier(menuItemId));
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
        final HstSite editingPreviewHstSite = getPageComposerContextService().getEditingPreviewSite();
        final String menuId = getPageComposerContextService().getRequestConfigIdentifier();
        return siteMenuHelper.getMenu(editingPreviewHstSite, menuId);
    }


    private List<Validator> getDefaultMenuModificationValidators() {
        List<Validator> preValidators = new ArrayList<>();
        final String requestConfigIdentifier = getPageComposerContextService().getRequestConfigIdentifier();
        preValidators.add(new NodePathPrefixValidator(getPreviewConfigurationWorkspacePath(),
                requestConfigIdentifier, HstNodeTypes.NODETYPE_HST_SITEMENU));
        return preValidators;
    }

}
