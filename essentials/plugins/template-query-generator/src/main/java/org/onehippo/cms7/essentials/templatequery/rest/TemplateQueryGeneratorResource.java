/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.templatequery.rest;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.onehippo.cms7.essentials.sdk.api.model.rest.ContentType;
import org.onehippo.cms7.essentials.sdk.api.service.ContentTypeService;
import org.onehippo.cms7.essentials.sdk.api.service.JcrService;
import org.onehippo.cms7.essentials.sdk.api.service.RebuildService;


/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("templatequerygenerator/")
public class TemplateQueryGeneratorResource {

    private final RebuildService rebuildService;
    private final JcrService jcrService;
    private final ContentTypeService contentTypeService;

    @Inject
    public TemplateQueryGeneratorResource(final RebuildService rebuildService, final JcrService jcrService, final ContentTypeService contentTypeService) {
        this.rebuildService = rebuildService;
        this.jcrService = jcrService;
        this.contentTypeService = contentTypeService;
    }

    @GET
    @Path("/documenttypes")
    public List<ContentType> getDocumentTypes() throws Exception {
        return contentTypeService.fetchContentTypesFromOwnNamespace();
    }
}
