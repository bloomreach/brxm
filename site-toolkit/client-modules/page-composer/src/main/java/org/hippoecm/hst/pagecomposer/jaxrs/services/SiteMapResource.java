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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMapHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validaters.CurrentPreviewValidator;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validaters.PreviewWorkspaceNodeValidator;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validaters.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/" + HstNodeTypes.NODETYPE_HST_SITEMAP + "/")
@Produces(MediaType.APPLICATION_JSON)
public class SiteMapResource extends AbstractConfigResource {

    private static Logger log = LoggerFactory.getLogger(SiteMapResource.class);

    private final SiteMapHelper siteMapHelper = new SiteMapHelper();

    @GET
    @Path("/")
    public Response getSiteMap(final @Context HstRequestContext requestContext) {
        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final HstSiteMap siteMap = getEditingPreviewSite(requestContext).getSiteMap();
                final SiteMapRepresentation representation = new SiteMapRepresentation().represent(siteMap);
                return ok("Sitemap loaded successfully", representation);
            }
        });
    }


    @POST
    @Path("/update")
    public Response update(final SiteMapItemRepresentation siteMapItem) {
        final List<Validator> preValidators = new ArrayList<>();
        preValidators.add(new CurrentPreviewValidator(siteMapItem.getId(), siteMapHelper));
        preValidators.add(new PreviewWorkspaceNodeValidator(siteMapItem.getId(), HstNodeTypes.NODETYPE_HST_SITEMAPITEM));

        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                siteMapHelper.update(siteMapItem);
                return ok("Item updated successfully", siteMapItem.getId());
            }
        }, preValidators);
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

        final  List<Validator> preValidators = new ArrayList<>();

        if (parentId != null) {
            preValidators.add(new PreviewWorkspaceNodeValidator(parentId, HstNodeTypes.NODETYPE_HST_SITEMAPITEM));
        }

        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final String finalParentId;
                if (parentId == null) {
                    final HstRequestContext requestContext = RequestContextProvider.get();
                    final HstSite editingPreviewSite = AbstractConfigResource.getEditingPreviewSite(requestContext);
                    final HstSiteMap siteMap = editingPreviewSite.getSiteMap();
                    if (!(siteMap instanceof CanonicalInfo)) {
                        throw new IllegalStateException("Only sitemap that is instance of CanonicalInfo can be edited");
                    }
                    String siteMapId = ((CanonicalInfo)siteMap).getCanonicalIdentifier();
                    Node siteMapNode = requestContext.getSession().getNodeByIdentifier(siteMapId);
                    if (siteMapNode.getParent().isNodeType(HstNodeTypes.NODETYPE_HST_WORKSPACE)) {
                        finalParentId = siteMapId;
                    } else {
                    // not the workspace sitemap node. Take the workspace sitemap. If not existing, an exception is thrown
                        final String relSiteMapPath = HstNodeTypes.NODENAME_HST_WORKSPACE + "/" + HstNodeTypes.NODENAME_HST_SITEMAP;
                        final Node configNode = siteMapNode.getParent();
                        if (!configNode.hasNode(relSiteMapPath)) {
                            throw new IllegalStateException("Cannot add new sitemap items because there is no workspace sitemap at " +
                                    "'"+configNode.getPath() + "/" + relSiteMapPath +"'.");
                        }
                        finalParentId = configNode.getNode(relSiteMapPath).getIdentifier();
                    }
                } else {
                    finalParentId = parentId;
                }
                Node newSiteMapItem = siteMapHelper.create(siteMapItem, finalParentId);
                return ok("Item created successfully", newSiteMapItem.getIdentifier());
            }
        }, preValidators);
    }


    @POST
    @Path("/move/{id}/{parentId}")
    public Response move(final @PathParam("id") String id,
                         final @PathParam("parentId") String parentId) {

//        String oldPath;
//        try {
//            oldPath = RequestContextProvider.get().getSession().getNode(id).getPath();
//        } catch (RepositoryException e) {
//            return logAndReturnClientError(e);
//        }
        final List<Validator> preValidators = new ArrayList<>();
        preValidators.add(new CurrentPreviewValidator(id, siteMapHelper));
        preValidators.add(new CurrentPreviewValidator(parentId, siteMapHelper));
        preValidators.add(new PreviewWorkspaceNodeValidator(id, HstNodeTypes.NODETYPE_HST_SITEMAPITEM));
        preValidators.add(new PreviewWorkspaceNodeValidator(parentId, HstNodeTypes.NODETYPE_HST_SITEMAPITEM));

       return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                siteMapHelper.move(id, parentId);
                return ok("Item moved successfully", id);
            }
        }, preValidators);
    }

    @POST
    @Path("/delete/{id}")
    public Response delete(final @PathParam("id") String id) {
        final  List<Validator> preValidators = new ArrayList<>();

        preValidators.add(new PreviewWorkspaceNodeValidator(id, HstNodeTypes.NODETYPE_HST_SITEMAPITEM));

        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                siteMapHelper.delete(id);
                return ok("Item deleted successfully", id);
            }
        }, preValidators);
    }

}
