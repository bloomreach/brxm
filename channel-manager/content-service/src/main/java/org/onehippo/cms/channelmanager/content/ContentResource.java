/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.onehippo.cms.channelmanager.content.document.DocumentsService;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypesService;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.repository.jaxrs.api.SessionDataProvider;

@Produces("application/json")
@Path("/")
public class ContentResource {

    private static final CacheControl NO_CACHE = new CacheControl();
    static {
        NO_CACHE.setNoCache(true);
    }

    private final SessionDataProvider sessionDataProvider;

    public ContentResource(final SessionDataProvider userSessionProvider) {
        this.sessionDataProvider = userSessionProvider;
    }

    @POST
    @Path("documents/{id}/draft")
    public Response createDraftDocument(@PathParam("id") String id, @Context HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Response.Status.CREATED,
                (session, locale) -> DocumentsService.get().createDraft(id, session, locale));
    }

    @PUT
    @Path("documents/{id}/draft")
    public Response updateDraftDocument(@PathParam("id") String id, Document document,
                                        @Context HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Response.Status.OK,
                (session, locale) -> DocumentsService.get().updateDraft(id, document, session, locale));
    }

    @DELETE
    @Path("documents/{id}/draft")
    public Response deleteDraftDocument(@PathParam("id") String id, @Context HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Response.Status.OK, (session, locale) -> {
            DocumentsService.get().deleteDraft(id, session, locale);
            return null; // no response data
        });
    }

    // for easy debugging:
    @GET
    @Path("documents/{id}")
    public Response getPublishedDocument(@PathParam("id") String id, @Context HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Response.Status.OK,
                (session, locale) -> DocumentsService.get().getPublished(id, session, locale));
    }

    @GET
    @Path("documenttypes/{id}")
    public Response getDocumentType(@PathParam("id") String id, @Context HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Response.Status.OK, NO_CACHE,
                (session, locale) -> DocumentTypesService.get().getDocumentType(id, session, locale));
    }

    private Response executeTask(final HttpServletRequest servletRequest,
                                 final Response.Status successResponse,
                                 final EndPointTask task) {
        return executeTask(servletRequest, successResponse, null, task);
    }

    /**
     * Shared logic for providing the EndPointTask with contextual input and handling the packaging of its response
     * (which may be an error, encapsulated in an Exception).
     *
     * @param servletRequest  current HTTP servlet request to derive contextual input
     * @param successResponse HTTP status code in case of success
     * @param cacheControl    HTTP Cache-Control response header
     * @param task            the EndPointTask to execute
     * @return                a JAX-RS response towards the client
     */
    private Response executeTask(final HttpServletRequest servletRequest,
                                 final Response.Status successResponse,
                                 final CacheControl cacheControl,
                                 final EndPointTask task) {
        final Session session = sessionDataProvider.getJcrSession(servletRequest);
        final Locale locale = sessionDataProvider.getLocale(servletRequest);
        try {
            final Object result = task.execute(session, locale);
            return Response.status(successResponse).cacheControl(cacheControl).entity(result).build();
        } catch (ErrorWithPayloadException e) {
            return Response.status(e.getStatus()).cacheControl(cacheControl).entity(e.getPayload()).build();
        }
    }

    @FunctionalInterface
    private interface EndPointTask {
        Object execute(Session session, Locale locale) throws ErrorWithPayloadException;
    }
}
