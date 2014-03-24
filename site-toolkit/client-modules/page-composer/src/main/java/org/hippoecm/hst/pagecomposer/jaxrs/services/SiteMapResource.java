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
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
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
        return tryGet(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final HstSite site = getPageComposerContextService().getEditingPreviewSite();
                final HstSiteMap siteMap = site.getSiteMap();
                final SiteMapRepresentation representation = new SiteMapRepresentation().represent(siteMap, getPreviewConfigurationPath(),
                        site.getComponentsConfiguration());
                return ok("Sitemap loaded successfully", representation);
            }
        });
    }

    @GET
    @Path("/hostname")
    public Response getHostName() {
        return tryGet(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final Mount mount = getPageComposerContextService().getEditingMount();
                return ok("Hostname loaded successfully", mount.getVirtualHost().getHostName());
            }
        });
    }

    @GET
    @Path("/pages")
    public Response getSiteMapPages() {
        return tryGet(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final HstSite site = getPageComposerContextService().getEditingPreviewSite();
                final HstSiteMap siteMap = site.getSiteMap();
                final Mount mount = getPageComposerContextService().getEditingMount();
                final SiteMapRepresentation sitemap = new SiteMapRepresentation().represent(siteMap, getPreviewConfigurationPath(), site.getComponentsConfiguration());
                final SiteMapPagesRepresentation pages = new SiteMapPagesRepresentation().represent(sitemap,
                        mount);
                return ok("Sitemap loaded successfully", pages);
            }
        });
    }

    @GET
    @Path("/item/{siteMapItemUuid}")
    public Response getSiteMapItem(@PathParam("siteMapItemUuid") final String siteMapItemUuid) {
        return tryGet(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final HstSiteMapItem siteMapItem = siteMapHelper.getConfigObject(siteMapItemUuid);
                final HstSite site = getPageComposerContextService().getEditingPreviewSite();
                final SiteMapItemRepresentation siteMapItemRepresentation = new SiteMapItemRepresentation()
                        .representShallow(siteMapItem, site.getConfigurationPath(), site.getComponentsConfiguration());

                return ok("Sitemap item loaded successfully", siteMapItemRepresentation);
            }
        });
    }

    @POST
    @Path("/update")
    public Response update(final SiteMapItemRepresentation siteMapItem) {
        final ValidatorBuilder preValidatorBuilder = ValidatorBuilder.builder()
                .add(validatorFactory.getHasPreviewConfigurationValidator(getPageComposerContextService()))
                .add(validatorFactory.getCurrentPreviewConfigurationValidator(siteMapItem.getId(), siteMapHelper))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(),
                        siteMapItem.getId(), HstNodeTypes.NODETYPE_HST_SITEMAPITEM))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationPath(), getPageComposerContextService().getRequestConfigIdentifier(),
                        HstNodeTypes.NODETYPE_HST_SITEMAP))
                .add(validatorFactory.getNameValidator(siteMapItem.getName()));

        // if the update has a uuid for componenent id, we need to re-apply a prototype. In that case we also need to
        // validate the prototype page
        boolean isCompIdUUID = false;
        if (siteMapItem.getComponentConfigurationId() != null) {
            try {
                UUID.fromString(siteMapItem.getComponentConfigurationId());
                // new page id: reapply prototype
                preValidatorBuilder.add(validatorFactory.getPrototypePageValidator(siteMapItem.getComponentConfigurationId()));
                isCompIdUUID = true;
            } catch (IllegalArgumentException e) {
                // no problem: no new page id has been set
            }
        }

        final boolean reapplyPrototype = isCompIdUUID;
        preValidatorBuilder.add(new NotNullValidator(siteMapItem.getName(), ClientError.ITEM_NO_NAME));
        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                siteMapHelper.update(siteMapItem, reapplyPrototype);
                return ok("Item updated successfully", siteMapItem.getId());
            }
        }, preValidatorBuilder.build());
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
                .add(validatorFactory.getHasPreviewConfigurationValidator(getPageComposerContextService()))
                .add(validatorFactory.getNotNullValidator(siteMapItem.getName(), ClientError.ITEM_NO_NAME))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationPath(), getPageComposerContextService().getRequestConfigIdentifier(),
                        HstNodeTypes.NODETYPE_HST_SITEMAP))
                .add(validatorFactory.getPrototypePageValidator(siteMapItem.getComponentConfigurationId()))
                .add(validatorFactory.getNameValidator(siteMapItem.getName()));

        if (parentId != null) {
            preValidators.add(validatorFactory.getCurrentPreviewConfigurationValidator(parentId, siteMapHelper));
            preValidators.add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationWorkspacePath(),
                    parentId, HstNodeTypes.NODETYPE_HST_SITEMAPITEM));
        }

        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                Node newSiteMapItem = siteMapHelper.create(siteMapItem, parentId);
                return ok("Item created successfully", newSiteMapItem.getIdentifier());
            }
        }, preValidators.build());
    }

    @POST
    @Path("/duplicate/{siteMapItemId}")
    public Response copy(final @PathParam("siteMapItemId") String siteMapItemId) {
        final ValidatorBuilder preValidators = ValidatorBuilder.builder()
                .add(validatorFactory.getHasPreviewConfigurationValidator(getPageComposerContextService()))
                .add(validatorFactory.getNodePathPrefixValidator(getPreviewConfigurationPath(), getPageComposerContextService().getRequestConfigIdentifier(),
                        HstNodeTypes.NODETYPE_HST_SITEMAP));
        preValidators.add(validatorFactory.getCurrentPreviewConfigurationValidator(siteMapItemId, siteMapHelper));

        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                Node copy = siteMapHelper.duplicate(siteMapItemId);
                return ok("Item created successfully", copy.getIdentifier());
            }
        }, preValidators.build());
    }


    /**
     * if <code>parentId</code> is <code>null</code> the move will be done to the root sitemap
     */
    @POST
    @Path("/move/{id}/{parentId}")
    public Response move(final @PathParam("id") String id,
                         final @PathParam("parentId") String parentId) {
        final ValidatorBuilder preValidators = ValidatorBuilder.builder()
                .add(validatorFactory.getHasPreviewConfigurationValidator(getPageComposerContextService()))
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
                siteMapHelper.move(id, parentId);
                return ok("Item moved successfully", id);
            }
        }, preValidators.build());
    }

    @POST
    @Path("/delete/{id}")
    public Response delete(final @PathParam("id") String id) {
        final Validator preValidator = ValidatorBuilder.builder()
                .add(validatorFactory.getHasPreviewConfigurationValidator(getPageComposerContextService()))
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
        }, preValidator);
    }

}
