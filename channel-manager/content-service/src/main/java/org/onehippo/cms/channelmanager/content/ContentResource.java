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

import java.util.Locale;
import java.util.Optional;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.onehippo.cms.channelmanager.content.document.DocumentNotFoundException;
import org.onehippo.cms.channelmanager.content.document.OperationFailedException;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypeNotFoundException;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.document.model.EditingInfo;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypesService;
import org.onehippo.cms.channelmanager.content.document.DocumentsService;

@Produces("application/json")
@Path("/")
public class ContentResource {
    private final SessionDataProvider sessionDataProvider;

    public ContentResource(final SessionDataProvider userSessionProvider) {
        this.sessionDataProvider = userSessionProvider;
    }

    @POST
    @Path("documents/{id}/draft")
    public Response createDraftDocument(@PathParam("id") String id, @Context HttpServletRequest servletRequest) {
        final Session userSession = sessionDataProvider.getJcrSession(servletRequest);
        final DocumentsService documentsService = DocumentsService.get();
        try {
            final Document document = documentsService.createDraft(id, userSession);
            if (document.getInfo().getEditingInfo().getState() == EditingInfo.State.AVAILABLE) {
                return Response.status(Response.Status.CREATED).entity(document).build();
            } else {
                return Response.status(Response.Status.FORBIDDEN).entity(document).build();
            }
        } catch (DocumentNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @PUT
    @Path("documents/{id}/draft")
    public Response updateDraftDocument(@PathParam("id") String id, Document document,
                                        @Context HttpServletRequest servletRequest) {
        final Session userSession = sessionDataProvider.getJcrSession(servletRequest);
        final DocumentsService documentsService = DocumentsService.get();
        try {
            documentsService.updateDraft(id, document, userSession);
            return Response.ok().entity(document).build();
        } catch (DocumentNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (OperationFailedException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getErrorInfo()).build();
        }
    }

    @DELETE
    @Path("documents/{id}/draft")
    public Response deleteDraftDocument(@PathParam("id") String id, @Context HttpServletRequest servletRequest) {
        final Session userSession = sessionDataProvider.getJcrSession(servletRequest);
        final DocumentsService documentsService = DocumentsService.get();
        try {
            documentsService.deleteDraft(id, userSession);
            return Response.ok().build();
        } catch (DocumentNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (OperationFailedException e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getErrorInfo()).build();
        }
    }

    // for easy debugging:
    @GET
    @Path("documents/{id}")
    public Response getPublishedDocument(@PathParam("id") String id, @Context HttpServletRequest servletRequest) {
        final Session userSession = sessionDataProvider.getJcrSession(servletRequest);
        final DocumentsService documentsService = DocumentsService.get();
        try {
            final Document document = documentsService.getPublished(id, userSession);
            return Response.ok().entity(document).build();
        } catch (DocumentNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("documenttypes/{id}")
    public Response getDocumentType(@PathParam("id") String id, @Context HttpServletRequest servletRequest) {
        final Session userSession = sessionDataProvider.getJcrSession(servletRequest);
        final Locale locale = sessionDataProvider.getLocale(servletRequest);
        final DocumentTypesService documentTypesService = DocumentTypesService.get();
        try {
            final DocumentType docType = documentTypesService.getDocumentType(id, userSession, Optional.of(locale));
            return Response.ok().entity(docType).build();
        } catch (DocumentTypeNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
