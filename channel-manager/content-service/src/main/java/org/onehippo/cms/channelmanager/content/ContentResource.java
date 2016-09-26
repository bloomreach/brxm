/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.onehippo.cms.channelmanager.content.model.Document;
import org.onehippo.cms.channelmanager.content.model.DocumentTypeSpec;
import org.onehippo.cms.channelmanager.content.util.MockResponse;

@Produces("application/json")
@Path("/")
public class ContentResource {
    private final UserSessionProvider userSessionProvider;
    private final ContentService contentService;

    public ContentResource(final UserSessionProvider userSessionProvider, final ContentService contentService) {
        this.userSessionProvider = userSessionProvider;
        this.contentService = contentService;
    }

    @GET
    @Path("documents/{id}")
    public Document getDocument(@PathParam("id") String id, @Context HttpServletRequest servletRequest) {
        return contentService.getDocument(userSessionProvider.get(servletRequest), id);
    }

    @GET
    @Path("documenttypes/{id}")
    public DocumentTypeSpec getDocumentTypeSpec(@PathParam("id") String id) {
        return MockResponse.createTestDocumentType();
    }
}
