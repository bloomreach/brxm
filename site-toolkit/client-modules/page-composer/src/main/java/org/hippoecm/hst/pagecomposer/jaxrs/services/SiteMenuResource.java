/*
 * Copyright 2014-2016 Hippo B.V. (http://www.onehippo.com)
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
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.base.Predicate;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
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
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.Validator;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.ValidatorBuilder;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.ValidatorFactory;

@Path("/" + HstNodeTypes.NODETYPE_HST_SITEMENU + "/")
@Produces(MediaType.APPLICATION_JSON)
public class SiteMenuResource extends AbstractConfigResource {

    private SiteMenuHelper siteMenuHelper;
    private SiteMenuItemHelper siteMenuItemHelper;
    private Predicate<String> uriValidator;
    private ValidatorFactory validatorFactory = new ValidatorFactory();

    public void setSiteMenuHelper(final SiteMenuHelper siteMenuHelper) {
        this.siteMenuHelper = siteMenuHelper;
    }

    public void setSiteMenuItemHelper(final SiteMenuItemHelper siteMenuItemHelper) {
        this.siteMenuItemHelper = siteMenuItemHelper;
    }

    public void setUriValidator(final Predicate<String> uriValidator) {
        this.uriValidator = uriValidator;
    }

    public void setValidatorFactory(final ValidatorFactory validatorFactory) {
        this.validatorFactory = validatorFactory;
    }

    @GET
    @Path("/")
    public Response getMenu() {
        return tryGet(() -> {
            final HstSiteMenuConfiguration menu = getHstSiteMenuConfiguration();
            final Mount mount = getPageComposerContextService().getEditingMount();
            final String siteContentIdentifier = getPageComposerContextService().getSiteContentIdentifier(mount);
            final SiteMenuRepresentation representation = new SiteMenuRepresentation(menu, mount, siteContentIdentifier);
            return ok("Menu item loaded successfully", representation);
        });
    }

    @GET
    @Path("/{menuItemId}")
    public Response getMenuItem(final @PathParam("menuItemId") String menuItemId) {
        return tryGet(() -> {
            final HstSiteMenuConfiguration menu = getHstSiteMenuConfiguration();
            final Mount mount = getPageComposerContextService().getEditingMount();
            final HstSiteMenuItemConfiguration menuItem = siteMenuHelper.getMenuItem(menu, menuItemId);
            final SiteMenuItemRepresentation representation = new SiteMenuItemRepresentation(menuItem, mount);
            return ok("Menu item loaded successfully", representation);
        });
    }

    @POST
    @Path("/{parentId}")
    public Response create(final @PathParam("parentId") String parentId,
                           final @DefaultValue(Position.LAST_AS_STRING) @QueryParam("position") String position,
                           final @DefaultValue("") @QueryParam("sibling") String after,
                           final SiteMenuItemRepresentation newMenuItem) {
        final Validator preValidator = ValidatorBuilder.builder()
                .add(getDefaultMenuModificationValidator())
                .add(validatorFactory.getNotNullValidator(newMenuItem.getName(), ClientError.ITEM_NO_NAME))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(), parentId, null))
                .add(validatorFactory.getSiteMenuItemRepresentationValidator(getPageComposerContextService(), uriValidator, newMenuItem))
                .build();
        return tryExecute(() -> {
            final Session session = getPageComposerContextService().getRequestContext().getSession();
            final HstSiteMenuConfiguration menu = getHstSiteMenuConfiguration();
            final Node parentNode = getParentNode(parentId, session, menu);
            Node menuItemNode = siteMenuItemHelper.create(parentNode, newMenuItem, Position.fromString(position), after);
            return ok("Item created successfully", menuItemNode.getIdentifier());
        }, preValidator);
    }


    @POST
    @Path("/")
    public Response update(final SiteMenuItemRepresentation modifiedItem) {

        final Validator preValidator = ValidatorBuilder.builder()
                .add(getDefaultMenuModificationValidator())
                .add(validatorFactory.getHasPreviewConfigurationValidator(getPageComposerContextService()))
                .add(validatorFactory.getNotNullValidator(modifiedItem.getName(), ClientError.ITEM_NO_NAME))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(), modifiedItem.getId(), null))
                .add(validatorFactory.getSiteMenuItemRepresentationValidator(getPageComposerContextService(), uriValidator, modifiedItem))
                .build();
        return tryExecute(() -> {
            final Session session = getPageComposerContextService().getRequestContext().getSession();
            final Node menuItemNode = session.getNodeByIdentifier(modifiedItem.getId());
            siteMenuItemHelper.update(menuItemNode, modifiedItem);
            return ok("Item updated successfully", modifiedItem.getId());
        }, preValidator);
    }


    /**
     * Move an item from <code>sourceId</code> to become a child of the parent <code>parentId</code> at the position
     * <code>childIndex></code>
     *
     * @param sourceId ID of the menu item to move
     * @param parentId ID of the menu item that will become the parent of the moved item.
     * @param childIndex Index (zero-based) of the moved menu item within the new parent.
     * @return A response with the ID of the moved item, or an error.
     */
    @PUT
    @Path("/{parentId}/{index}")
    public Response move(final @HeaderParam("Move-From") String sourceId,
                         final @PathParam("parentId") String parentId,
                         final @PathParam("index") Integer childIndex) {

        final Validator preValidator = ValidatorBuilder.builder()
                .add(getDefaultMenuModificationValidator())
                .add(validatorFactory.getHasPreviewConfigurationValidator(getPageComposerContextService()))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(), sourceId, null))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(), parentId, null))
                .build();
        return tryExecute(() -> {
            final Session session = getPageComposerContextService().getRequestContext().getSession();
            final Node parent = session.getNodeByIdentifier(parentId);
            final Node source = session.getNodeByIdentifier(sourceId);
            siteMenuItemHelper.move(parent, source, childIndex);
            return ok("Item moved successfully", sourceId);
        }, preValidator);
    }

    @DELETE
    @Path("/{menuItemId}")
    public Response delete(final @PathParam("menuItemId") String menuItemId) {
        final Validator preValidator = ValidatorBuilder.builder()
                .add(getDefaultMenuModificationValidator())
                .add(validatorFactory.getHasPreviewConfigurationValidator(getPageComposerContextService()))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(), menuItemId, null))
                .build();
        return tryExecute(() -> {
            final Session session = getPageComposerContextService().getRequestContext().getSession();
            siteMenuItemHelper.delete(session.getNodeByIdentifier(menuItemId));
            return ok("Item deleted successfully", menuItemId);
        }, preValidator);
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


    private Validator getDefaultMenuModificationValidator() {
        final String requestConfigIdentifier = getPageComposerContextService().getRequestConfigIdentifier();
        final String path = getPreviewConfigurationWorkspacePath();
        return validatorFactory.getNodePathPrefixValidator(path, requestConfigIdentifier, HstNodeTypes.NODETYPE_HST_SITEMENU);
    }

}
