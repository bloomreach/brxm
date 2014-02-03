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
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMapHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validaters.CurrentPreviewSiteItemValidator;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validaters.LockValidator;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validaters.Validator;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/" + HstNodeTypes.NODETYPE_HST_SITEMAP + "/")
@Produces(MediaType.APPLICATION_JSON)
public class SiteMapResource extends AbstractConfigResource {

    private static Logger log = LoggerFactory.getLogger(SiteMapResource.class);

    private final SiteMapHelper siteMapHelper;
    public SiteMapResource(final SiteMapHelper siteMapHelper) {
        this.siteMapHelper = siteMapHelper;
    }

    public SiteMapResource() {
        this(new SiteMapHelper());
    }

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
    public Response update(final @Context HstRequestContext requestContext, final SiteMapItemRepresentation siteMapItem) {

        List<Validator> validators = new ArrayList<>();
        validators.add(new CurrentPreviewSiteItemValidator(siteMapItem.getId(), siteMapHelper));
        validators.add(new LockValidator(requestContext, siteMapItem.getId(), LockValidator.Operation.UPDATE,
                HstNodeTypes.NODETYPE_HST_SITEMAPITEM, HstNodeTypes.NODETYPE_HST_SITEMAP));

        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                siteMapHelper.update(siteMapItem);
                return ok("Item updated successfully", siteMapItem.getId());
            }
        }, validators.toArray(new Validator[validators.size()]));
    }

    @POST
    @Path("/move/{sourceId}/{parentTargetId}")
    public Response move(@Context HstRequestContext requestContext,
                         @PathParam("sourceId") String sourceId,
                         @PathParam("parentTargetId") String parentTargetId) {
        return move(requestContext, sourceId, parentTargetId, null);
    }

    @POST
    @Path("/move/{sourceId}/{parentTargetId}/{childTargetId}")
    public Response move(final @Context HstRequestContext requestContext,
                         final @PathParam("sourceId") String sourceId,
                         final @PathParam("parentTargetId") String parentTargetId,
                         final @PathParam("childTargetId") String childTargetId) {

        List<Validator> validators = new ArrayList<>();
        validators.add(new CurrentPreviewSiteItemValidator(sourceId, siteMapHelper));
        validators.add(new CurrentPreviewSiteItemValidator(parentTargetId, siteMapHelper));
        if (childTargetId != null) {
            validators.add(new CurrentPreviewSiteItemValidator(childTargetId, siteMapHelper));
        }
        validators.add(new LockValidator(requestContext, sourceId, LockValidator.Operation.MOVE,
                HstNodeTypes.NODETYPE_HST_SITEMAPITEM, HstNodeTypes.NODETYPE_HST_SITEMAP));
        validators.add(new LockValidator(requestContext, parentTargetId, LockValidator.Operation.MOVE,
                HstNodeTypes.NODETYPE_HST_SITEMAPITEM, HstNodeTypes.NODETYPE_HST_SITEMAP));

        return tryExecute(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                siteMapHelper.move(sourceId, parentTargetId, childTargetId);
                return ok("Item updated successfully", sourceId);
            }
        }, validators.toArray(new Validator[validators.size()]));
    }


}
