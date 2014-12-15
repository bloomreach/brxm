/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.forge.sitemap.components;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.jaxrs.services.AbstractResource;
import org.onehippo.forge.sitemap.components.model.Urlset;
import org.onehippo.forge.sitemap.components.splitter.SitemapSplitter;
import org.onehippo.forge.sitemap.components.splitter.TarGzipSitemapSplitter;
import org.onehippo.forge.sitemap.generator.DefaultUrlInformationProvider;
import org.onehippo.forge.sitemap.generator.SitemapGenerator;
import org.springframework.beans.factory.annotation.Required;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;

/**
 * This is a JAX-RS resource to be used in the Hippo REST-interface. It outputs a site map in different formats,
 * including a tar.gz archive.
 */
@Path("sitemap")
public class SiteMapBasedOnHstSiteMapResource extends AbstractResource {

    private List<String> exclusionsByRefId = Collections.emptyList();
    private List<String> exclusionsByComponentConfigurationId = Collections.emptyList();
    private List<String> exclusionsBySiteMapPath = Collections.emptyList();
    private UrlInformationProvider urlInformationProvider = DefaultUrlInformationProvider.INSTANCE;
    private int amountOfWorkers = -1;
    private String mountAlias;

    @GET
    @Path("sitemap-archive.tar.gz")
    @Produces({"application/zip", "application/x-gzip", "application/octet-stream", MediaType.APPLICATION_XML})
    public Response createSiteMapTarGzArchive(@Context HttpServletRequest request,
                                            @Context HttpServletResponse response) {
        Urlset siteMap = obtainSiteMap(request);
        final ByteArrayOutputStream siteMapArchiveStream = new ByteArrayOutputStream();

        SitemapSplitter splitter = new TarGzipSitemapSplitter(siteMap, siteMapArchiveStream);
        boolean splittingExecuted = splitter.split();

        if (!splittingExecuted) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }

        response.setHeader("Content-Disposition", "attachment; filename=sitemap-archive.tar.gz");
        response.setContentType("application/x-gzip");
        final Response.ResponseBuilder okResponse = Response.ok(siteMapArchiveStream.toByteArray(), MediaType.APPLICATION_OCTET_STREAM_TYPE);

        return okResponse.build();
    }

    private Urlset obtainSiteMap(final HttpServletRequest request) {
        HstRequestContext requestContext = getRequestContext(request);
        ObjectConverter objectConverter = getObjectConverter(requestContext);

        Mount mount = requestContext.getMount(mountAlias);

        SitemapGenerator generator =
                new SitemapGenerator(requestContext, objectConverter, urlInformationProvider, mount);
        generator.addSitemapRefIdExclusions(exclusionsByRefId);
        generator.addComponentConfigurationIdExclusions(exclusionsByComponentConfigurationId);
        generator.addSitemapPathExclusions(exclusionsBySiteMapPath);
        if (amountOfWorkers > 0) {
            generator.setAmountOfWorkers(amountOfWorkers);
        }
        return generator.createUrlSetBasedOnHstSiteMap();
    }

    public void setExclusionsByRefId(final List<String> exclusionsByRefId) {
        this.exclusionsByRefId = exclusionsByRefId != null ? exclusionsByRefId : Collections.<String>emptyList();
    }

    public void setExclusionsByComponentConfigurationId(final List<String> exclusionsByComponentConfigurationId) {
        this.exclusionsByComponentConfigurationId = exclusionsByComponentConfigurationId != null ?
                exclusionsByComponentConfigurationId : Collections.<String>emptyList();
    }

    public void setExclusionsBySiteMapPath(List<String> exclusionsBySiteMapPath) {
        this.exclusionsBySiteMapPath = exclusionsBySiteMapPath != null ?
                exclusionsBySiteMapPath : Collections.<String>emptyList();
    }

    public void setUrlInformationProvider(UrlInformationProvider urlInformationProvider) {
        this.urlInformationProvider = urlInformationProvider;
    }

    public void setAmountOfWorkers(int amountOfWorkers) {
        this.amountOfWorkers = amountOfWorkers;
    }

    @Required
    public void setMountAlias(String mountAlias) {
        this.mountAlias = mountAlias;
    }
}
