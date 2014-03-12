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

import java.util.UUID;
import java.util.concurrent.Callable;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapPagesRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMapHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.NotNullValidator;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.Validator;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.ValidatorBuilder;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.ValidatorFactory;

@Path("/" + HstNodeTypes.NODETYPE_HST_SITEMAP + "/")
@Produces(MediaType.APPLICATION_JSON)
public class SiteMapResource extends AbstractConfigResource {

    private SiteMapHelper siteMapHelper;
    private ValidatorFactory validatorFactory;

    public void setSiteMapHelper(final SiteMapHelper siteMapHelper) {
        this.siteMapHelper = siteMapHelper;
    }

    public void setValidatorFactory(final ValidatorFactory validatorFactory) {
        this.validatorFactory = validatorFactory;
    }

    @GET
    @Path("/")
    public Response getSiteMap() {
        final Validator preValidator = ValidatorBuilder.builder()
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationPath(), getPageComposerContextService().getRequestConfigIdentifier(),
                        HstNodeTypes.NODETYPE_HST_SITEMAP))
                .build();
        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final HstSiteMap siteMap = getPageComposerContextService().getEditingPreviewSite().getSiteMap();
                final SiteMapRepresentation representation = new SiteMapRepresentation().represent(siteMap, getPreviewConfigurationPath());
                return ok("Sitemap loaded successfully", representation);
            }
        }, preValidator, validatorFactory.getVoidValidator());
    }

    @GET
    @Path("/pages")
    public Response getSiteMapPages() {
        final Validator preValidator = ValidatorBuilder.builder()
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationPath(), getPageComposerContextService().getRequestConfigIdentifier(),
                        HstNodeTypes.NODETYPE_HST_SITEMAP))
                .build();
        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final HstSiteMap siteMap = getPageComposerContextService().getEditingPreviewSite().getSiteMap();
                final Mount mount = getPageComposerContextService().getEditingMount();
                final SiteMapRepresentation sitemap = new SiteMapRepresentation().represent(siteMap, getPreviewConfigurationPath());
                final SiteMapPagesRepresentation pages = new SiteMapPagesRepresentation().represent(sitemap, mount);
                return ok("Sitemap loaded successfully", pages);
            }
        }, preValidator, validatorFactory.getVoidValidator());
    }

    @POST
    @Path("/update")
    public Response update(final SiteMapItemRepresentation siteMapItem) {
        final ValidatorBuilder preValidatorBuilder = ValidatorBuilder.builder()
                .add(validatorFactory.getCurrentPreviewConfigurationValidator(siteMapItem.getId(), siteMapHelper))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(),
                        siteMapItem.getId(), HstNodeTypes.NODETYPE_HST_SITEMAPITEM))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationPath(), getPageComposerContextService().getRequestConfigIdentifier(),
                        HstNodeTypes.NODETYPE_HST_SITEMAP));

        // if the update has a uuid for componenent id, we need to re-apply a prototype. In that case we also need to
        // validate the prototype page
        if (siteMapItem.getComponentConfigurationId() != null) {
            try {
                UUID.fromString(siteMapItem.getComponentConfigurationId());
                // new page id (re-prototype)
                preValidatorBuilder.add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationPrototypePath(),
                        siteMapItem.getComponentConfigurationId(), HstNodeTypes.NODETYPE_HST_ABSTRACT_COMPONENT));
            } catch (IllegalArgumentException e) {
                // no problem: no new page id has been set
            }
        }

        preValidatorBuilder.add(new NotNullValidator(siteMapItem.getName(), ClientError.ITEM_NO_NAME));
        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                siteMapHelper.update(siteMapItem);
                return ok("Item updated successfully", siteMapItem.getId());
            }
        }, preValidatorBuilder.build(), validatorFactory.getVoidValidator());
    }

    @POST
    @Path("/create")
    public Response create(final SiteMapItemRepresentation siteMapItem) {
        return create(siteMapItem, null);
    }

    @POST
    @Path("/create/{parentId}")
    public Response create(final SiteMapItemRepresentation siteMapItem,
                           final @PathParam("parentId") String parentId) {
        final ValidatorBuilder preValidators = ValidatorBuilder.builder()
                .add(validatorFactory.getNotNullValidator(siteMapItem.getName(), ClientError.ITEM_NO_NAME))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationPath(), getPageComposerContextService().getRequestConfigIdentifier(),
                        HstNodeTypes.NODETYPE_HST_SITEMAP))
                .add(validatorFactory.getPrototypePageValidator(siteMapItem.getComponentConfigurationId()));

        if (parentId != null) {
            preValidators.add(validatorFactory.getCurrentPreviewConfigurationValidator(parentId, siteMapHelper));
            preValidators.add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(),
                    parentId, HstNodeTypes.NODETYPE_HST_SITEMAPITEM));
        }

        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final String finalParentId;
                if (parentId == null) {
                    finalParentId = getWorkspaceSiteMapId();
                } else {
                    finalParentId = parentId;
                }
                Node newSiteMapItem = siteMapHelper.create(siteMapItem, finalParentId);
                return ok("Item created successfully", newSiteMapItem.getIdentifier());
            }
        }, preValidators.build(), validatorFactory.getVoidValidator());
    }

    /**
     * if <code>parentId</code> is <code>null</code> the move will be done to the root sitemap
     */
    @POST
    @Path("/move/{id}/{parentId}")
    public Response move(final @PathParam("id") String id,
                         final @PathParam("parentId") String parentId) {
        final ValidatorBuilder preValidators = ValidatorBuilder.builder()
                .add(validatorFactory.getCurrentPreviewConfigurationValidator(id, siteMapHelper))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(),
                        id, HstNodeTypes.NODETYPE_HST_SITEMAPITEM))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationPath(), getPageComposerContextService().getRequestConfigIdentifier(),
                        HstNodeTypes.NODETYPE_HST_SITEMAP));

        if (parentId != null) {
            preValidators.add(validatorFactory.getCurrentPreviewConfigurationValidator(parentId, siteMapHelper));
            preValidators.add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(),
                    parentId, HstNodeTypes.NODETYPE_HST_SITEMAPITEM));
        }
        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final String finalParentId;
                if (parentId == null) {
                    finalParentId = getWorkspaceSiteMapId();
                } else {
                    finalParentId = parentId;
                }
                siteMapHelper.move(id, finalParentId);
                return ok("Item moved successfully", id);
            }
        }, preValidators.build(), validatorFactory.getVoidValidator());
    }

    @POST
    @Path("/delete/{id}")
    public Response delete(final @PathParam("id") String id) {
        final Validator preValidator = ValidatorBuilder.builder()
                .add(validatorFactory.getCurrentPreviewConfigurationValidator(id, siteMapHelper))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(),
                        id, HstNodeTypes.NODETYPE_HST_SITEMAPITEM))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationPath(), getPageComposerContextService().getRequestConfigIdentifier(),
                        HstNodeTypes.NODETYPE_HST_SITEMAP))
                .build();

        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                siteMapHelper.delete(id);
                return ok("Item deleted successfully", id);
            }
        }, preValidator, validatorFactory.getVoidValidator());
    }

    private String getWorkspaceSiteMapId() throws RepositoryException {
        final String workspaceSiteMapId;
        final HstRequestContext requestContext = getPageComposerContextService().getRequestContext();
        final HstSite editingPreviewSite = getPageComposerContextService().getEditingPreviewSite();
        final HstSiteMap siteMap = editingPreviewSite.getSiteMap();
        String siteMapId = getCanonicalInfo(siteMap).getCanonicalIdentifier();
        Node siteMapNode = requestContext.getSession().getNodeByIdentifier(siteMapId);
        if (siteMapNode.getParent().isNodeType(HstNodeTypes.NODETYPE_HST_WORKSPACE)) {
            workspaceSiteMapId = siteMapId;
        } else {
            // not the workspace sitemap node. Take the workspace sitemap. If not existing, an exception is thrown
            final String relSiteMapPath = HstNodeTypes.NODENAME_HST_WORKSPACE + "/" + HstNodeTypes.NODENAME_HST_SITEMAP;
            final Node configNode = siteMapNode.getParent();
            if (!configNode.hasNode(relSiteMapPath)) {
                throw new IllegalStateException("Cannot add new sitemap items because there is no workspace sitemap at " +
                        "'" + configNode.getPath() + "/" + relSiteMapPath + "'.");
            }
            workspaceSiteMapId = configNode.getNode(relSiteMapPath).getIdentifier();
        }
        return workspaceSiteMapId;
    }

}
