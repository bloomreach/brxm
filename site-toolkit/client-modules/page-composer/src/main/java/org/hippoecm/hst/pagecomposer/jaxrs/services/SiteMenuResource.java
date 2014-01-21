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

import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/hst:sitemenu/")
public class SiteMenuResource extends AbstractConfigResource {
    private static Logger log = LoggerFactory.getLogger(SiteMenuResource.class);

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPageModelRepresentation(@Context HttpServletRequest servletRequest) {
        try {
            final HstRequestContext requestContext = getRequestContext(servletRequest);
            final HstSite editingPreviewHstSite = getEditingPreviewSite(requestContext);
            if (editingPreviewHstSite == null) {
                log.error("Could not get the editing site to create the page model representation.");
                return error("Could not get the editing site to create the page model representation.");
            }
            HstSiteMenuConfiguration menu = getMenu(editingPreviewHstSite, requestContext);
            if (menu == null) {
                log.warn("Sitemenu with id '{}' is not part of currently editted preview site.", getRequestConfigIdentifier(requestContext));
                return error("Sitemenu is not part of currently edited preview site.");
            }

            SiteMenuRepresentation siteMenuRepresentation = new SiteMenuRepresentation();
            siteMenuRepresentation.represent(menu);
            return ok("Menu loaded successfully", siteMenuRepresentation);
        } catch (Exception e) {
            log.warn("Failed to retrieve menu.", e);
            return error("Failed to retrieve menu: " + e.toString());
        }
    }

    /**
     * @return the {@link HstSiteMenuConfiguration} from currently editted preview hst site and when not found, return null
     */
    private HstSiteMenuConfiguration getMenu(final HstSite editingPreviewHstSite,
                                                                       final HstRequestContext requestContext) {
        final Map<String,HstSiteMenuConfiguration> siteMenuConfigurations = editingPreviewHstSite.getSiteMenusConfiguration().getSiteMenuConfigurations();
        final String requestConfigIdentifier = getRequestConfigIdentifier(requestContext);
        for (HstSiteMenuConfiguration menuConfiguration : siteMenuConfigurations.values()) {
            if (menuConfiguration.getCanonicalIdentifier().equals(requestConfigIdentifier)) {
                return menuConfiguration;
            }
        }
        return null;
    }


}
