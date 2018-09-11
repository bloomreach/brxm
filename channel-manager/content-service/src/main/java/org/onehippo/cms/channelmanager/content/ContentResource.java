/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.onehippo.cms.channelmanager.content.document.ContextPayloadUtils;
import org.onehippo.cms.channelmanager.content.document.DocumentsService;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.model.NewDocumentInfo;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypesService;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.folder.FoldersService;
import org.onehippo.cms.channelmanager.content.slug.SlugFactory;
import org.onehippo.cms.channelmanager.content.templatequery.TemplateQueryService;
import org.onehippo.cms.channelmanager.content.workflows.WorkflowService;
import org.onehippo.repository.jaxrs.api.SessionRequestContextProvider;

@Produces("application/json")
@Path("/")
public class ContentResource {

    private static final CacheControl NO_CACHE = new CacheControl();

    static {
        NO_CACHE.setNoCache(true);
    }

    private final SessionRequestContextProvider sessionRequestContextProvider;
    private final DocumentsService documentService;
    private final WorkflowService workflowService;
    private final Function<HttpServletRequest, Map<String, Serializable>> contextPayloadService;

    public ContentResource(final SessionRequestContextProvider userSessionProvider,
                           final DocumentsService documentsService,
                           final WorkflowService workflowService,
                           final Function<HttpServletRequest, Map<String, Serializable>> contextPayloadService) {
        this.sessionRequestContextProvider = userSessionProvider;
        this.documentService = documentsService;
        this.workflowService = workflowService;
        this.contextPayloadService = contextPayloadService;
    }

    @POST
    @Path("documents/{documentId}/branch")
    public Response branchDocument(@PathParam("documentId") final String id, @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.OK,
                (session, locale) -> documentService.branchDocument(id, session, locale, getBranchId(servletRequest)));
    }

    @POST
    @Path("documents/{documentId}/editable")
    public Response obtainEditableDocument(@PathParam("documentId") final String id, @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.OK,
                (session, locale) -> documentService.obtainEditableDocument(id, session, locale, getBranchId(servletRequest)));
    }

    @PUT
    @Path("documents/{documentId}/editable")
    public Response updateEditableDocument(@PathParam("documentId") final String id,
                                           final Document document,
                                           @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.OK,
                (session, locale) -> documentService.updateEditableDocument(id, document, session, locale, getBranchId(servletRequest)));
    }

    @PUT
    @Path("documents/{documentId}/editable/{fieldPath:.*}")
    public Response updateEditableField(@PathParam("documentId") final String documentId,
                                        @PathParam("fieldPath") final String fieldPath,
                                        final List<FieldValue> fieldValues,
                                        @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.NO_CONTENT, (session, locale) -> {
            documentService.updateEditableField(documentId, new FieldPath(fieldPath), fieldValues, session, locale, getBranchId(servletRequest));
            return null;
        });
    }

    @DELETE
    @Path("documents/{documentId}/editable")
    public Response discardEditableDocument(@PathParam("documentId") final String id, @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.NO_CONTENT, (session, locale) -> {
            documentService.discardEditableDocument(id, session, locale, getBranchId(servletRequest));
            return null;
        });
    }

    @GET
    @Path("documents/{documentId}/{branchId}")
    public Response getDocument(
            @PathParam("documentId") final String id,
            @PathParam("branchId") final String branchId,
            @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.OK,
                (session, locale) -> documentService.getDocument(id, branchId, session, locale)
        );
    }

    @GET
    @Path("documenttypes/{documentId}")
    public Response getDocumentType(@PathParam("documentId") final String id, @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.OK, NO_CACHE,
                (session, locale) -> DocumentTypesService.get().getDocumentType(id, session, locale));
    }

    @POST
    @Path("slugs")
    @Produces(MediaType.TEXT_PLAIN)
    public Response createSlug(final String contentName, @QueryParam("locale") final String contentLocale, @Context final HttpServletRequest servletRequest) {
        final String slug = SlugFactory.createSlug(contentName, contentLocale);
        return Response.status(Status.OK).entity(slug).build();
    }

    @GET
    @Path("templatequery/{documentId}")
    public Response getTemplateQuery(@PathParam("documentId") final String id, @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.OK, NO_CACHE,
                (session, locale) -> TemplateQueryService.get().getTemplateQuery(id, session, locale));
    }

    @GET
    @Path("folders/{path:.*}")
    public Response getFolders(@PathParam("path") final String path, @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.OK, NO_CACHE,
                (session, locale) -> FoldersService.get().getFolders(path, session));
    }

    @POST
    @Path("documents")
    public Response createDocument(final NewDocumentInfo newDocumentInfo, @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.CREATED,
                (session, locale) -> documentService.createDocument(newDocumentInfo, session, locale));
    }

    @PUT
    @Path("documents/{documentId}")
    public Response updateDocumentNames(@PathParam("documentId") final String id, final Document document,
                                        @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.OK,
                (session, locale) -> documentService.updateDocumentNames(id, document, session, getBranchId(servletRequest)));
    }

    @DELETE
    @Path("documents/{documentId}")
    public Response deleteDocument(@PathParam("documentId") final String id, @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.NO_CONTENT, (session, locale) -> {
            documentService.deleteDocument(id, session, locale, getBranchId(servletRequest));
            return null;
        });
    }

    @POST
    @Path("workflows/documents/{documentId}/{action}")
    public Response executeDocumentWorkflowAction(@PathParam("documentId") final String documentId,
                                                  @PathParam("action") final String action,
                                                  @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.NO_CONTENT, (session, locale) -> {
            workflowService.executeDocumentWorkflowAction(documentId, action, session, getBranchId(servletRequest));
            return null;
        });
    }

    private Response executeTask(final HttpServletRequest servletRequest,
                                 final Status successStatus,
                                 final EndPointTask task) {
        return executeTask(servletRequest, successStatus, null, task);
    }

    /**
     * Shared logic for providing the EndPointTask with contextual input and handling the packaging of its response
     * (which may be an error, encapsulated in an Exception).
     *
     * @param servletRequest current HTTP servlet request to derive contextual input
     * @param successStatus  HTTP status code in case of success
     * @param cacheControl   HTTP Cache-Control response header
     * @param task           the EndPointTask to execute
     * @return a JAX-RS response towards the client
     */
    private Response executeTask(final HttpServletRequest servletRequest,
                                 final Status successStatus,
                                 final CacheControl cacheControl,
                                 final EndPointTask task) {
        final Session session = sessionRequestContextProvider.getJcrSession(servletRequest);
        final Locale locale = sessionRequestContextProvider.getLocale(servletRequest);
        try {
            final Object result = task.execute(session, locale);
            return Response.status(successStatus).cacheControl(cacheControl).entity(result).build();
        } catch (final ErrorWithPayloadException e) {
            return Response.status(e.getStatus()).cacheControl(cacheControl).entity(e.getPayload()).build();
        }
    }

    private String getBranchId(HttpServletRequest servletRequest) {
        return ContextPayloadUtils.getBranchId(contextPayloadService.apply(servletRequest));
    }

    @FunctionalInterface
    private interface EndPointTask {
        Object execute(Session session, Locale locale) throws ErrorWithPayloadException;
    }
}
