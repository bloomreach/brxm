/*
 * Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
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

import org.onehippo.cms.channelmanager.content.document.DocumentVersionService;
import org.onehippo.cms.channelmanager.content.document.DocumentsService;
import org.onehippo.cms.channelmanager.content.document.model.Document;
import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.document.model.NewDocumentInfo;
import org.hippoecm.hst.core.internal.BranchSelectionService;
import org.onehippo.cms.channelmanager.content.document.model.OrderState;
import org.onehippo.cms.channelmanager.content.document.model.Version;
import org.onehippo.cms.channelmanager.content.document.util.FieldPath;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypesService;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;
import org.onehippo.cms.channelmanager.content.folder.FoldersService;
import org.onehippo.cms.channelmanager.content.slug.SlugFactory;
import org.onehippo.cms.channelmanager.content.templatequery.DocumentTemplateQueryService;
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
    private final BranchSelectionService branchSelectionService;
    private final DocumentVersionService documentVersionService;

    public ContentResource(final SessionRequestContextProvider userSessionProvider,
                           final DocumentsService documentsService,
                           final WorkflowService workflowService,
                           final Function<HttpServletRequest, Map<String, Serializable>> contextPayloadService,
                           final BranchSelectionService branchSelectionService,
                           final DocumentVersionService documentVersionService
    ) {
        this.sessionRequestContextProvider = userSessionProvider;
        this.documentService = documentsService;
        this.workflowService = workflowService;
        this.contextPayloadService = contextPayloadService;
        this.branchSelectionService = branchSelectionService;
        this.documentVersionService = documentVersionService;
    }

    @POST
    @Path("documents/{documentId}/branch")
    public Response branchDocument(@PathParam("documentId") final String id, @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.OK,
                userContext -> documentService.branchDocument(id, getBranchId(servletRequest), userContext));
    }

    @POST
    @Path("documents/{documentId}/editable")
    public Response obtainEditableDocument(@PathParam("documentId") final String id, @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.OK,
                userContext -> documentService.obtainEditableDocument(id, getBranchId(servletRequest), userContext));
    }

    @PUT
    @Path("documents/{documentId}/editable")
    public Response updateEditableDocument(@PathParam("documentId") final String id,
                                           final Document document,
                                           @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.OK,
                userContext -> documentService.updateEditableDocument(id, document, userContext));
    }

    @PUT
    @Path("documents/{documentId}/editable/{fieldPath:.*}")
    public Response updateEditableField(@PathParam("documentId") final String documentId,
                                        @PathParam("fieldPath") final String fieldPath,
                                        final List<FieldValue> fieldValues,
                                        @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.OK, userContext ->
            documentService.updateEditableField(documentId, getBranchId(servletRequest), new FieldPath(fieldPath), fieldValues, userContext)
        );
    }

    @DELETE
    @Path("documents/{documentId}/editable")
    public Response discardEditableDocument(@PathParam("documentId") final String id, @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.NO_CONTENT, userContext -> {
            documentService.discardEditableDocument(id, getBranchId(servletRequest), userContext);
            return null;
        });
    }

    @POST
    @Path("documents/{documentId}/editable/{fieldPath:.*}/{type}")
    public Response addNodeField(@PathParam("documentId") final String documentId,
                                 @PathParam("fieldPath") final String fieldPath,
                                 @PathParam("type") final String type,
                                 @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.OK,
                userContext -> documentService.addNodeField(documentId, getBranchId(servletRequest),
                        new FieldPath(fieldPath), type, userContext));
    }

    @PATCH
    @Path("documents/{documentId}/editable/{fieldPath:.*}")
    public Response orderNodeField(@PathParam("documentId") final String id,
                                   @PathParam("fieldPath") final String fieldPath,
                                   final OrderState order,
                                   @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.NO_CONTENT, userContext -> {
            documentService.reorderNodeField(id, getBranchId(servletRequest), new FieldPath(fieldPath), order.getOrder(),
                    userContext);
            return null;
        });
    }

    @DELETE
    @Path("documents/{documentId}/editable/{fieldPath:.*}")
    public Response removeNodeField(@PathParam("documentId") final String id,
                                    @PathParam("fieldPath") final String fieldPath,
                                    @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.NO_CONTENT, userContext -> {
            documentService.removeNodeField(id, getBranchId(servletRequest), new FieldPath(fieldPath), userContext);
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
                userContext -> documentService.getDocument(id, branchId, userContext)
        );
    }

    @GET
    @Path("documents/{handleId}/{branchId}/versions")
    public Response getDocumentVersionInfos(
            @PathParam("handleId") final String handleId,
            @PathParam("branchId") final String branchId,
            @QueryParam("campaignVersionOnly") final boolean campaignVersionOnly,
            @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.OK,
                userContext -> documentVersionService.getVersionInfo(handleId, branchId, userContext, campaignVersionOnly)
        );
    }

    @PUT
    @Path("workflows/documents/{handleId}/{branchId}/versions/{frozenNodeId}")
    public Response setCampaign(
            @PathParam("handleId") final String handleId,
            @PathParam("branchId") final String branchId,
            @PathParam("frozenNodeId") final String frozenNodeId,
            @Context final HttpServletRequest servletRequest,
            final Version version) {
        return executeTask(servletRequest, Status.OK,
                userContext -> documentVersionService.updateVersion(handleId, branchId, frozenNodeId, version, userContext)
        );
    }

    @GET
    @Path("documenttypes/{documentId}")
    public Response getDocumentType(@PathParam("documentId") final String id, @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.OK, NO_CACHE,
                userContext -> DocumentTypesService.get().getDocumentType(id, userContext));
    }

    @POST
    @Path("slugs")
    @Produces(MediaType.TEXT_PLAIN)
    public Response createSlug(final String contentName, @QueryParam("locale") final String contentLocale, @Context final HttpServletRequest servletRequest) {
        final String slug = SlugFactory.createSlug(contentName, contentLocale);
        return Response.status(Status.OK).entity(slug).build();
    }

    @GET
    @Path("documenttemplatequery/{documentId}")
    public Response getDocumentTemplateQuery(@PathParam("documentId") final String id, @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.OK, NO_CACHE,
                userContext -> DocumentTemplateQueryService.get().getDocumentTemplateQuery(id, userContext));
    }

    @GET
    @Path("folders/{path:.*}")
    public Response getFolders(@PathParam("path") final String path, @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.OK, NO_CACHE,
                userContext -> FoldersService.get().getFolders(path, userContext));
    }

    @POST
    @Path("documents")
    public Response createDocument(final NewDocumentInfo newDocumentInfo, @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.CREATED,
                userContext -> documentService.createDocument(newDocumentInfo, userContext));
    }

    @PUT
    @Path("documents/{documentId}")
    public Response updateDocumentNames(@PathParam("documentId") final String id, final Document document,
                                        @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.OK,
                userContext -> documentService.updateDocumentNames(id, getBranchId(servletRequest), document, userContext));
    }

    @DELETE
    @Path("documents/{documentId}")
    public Response deleteDocument(@PathParam("documentId") final String id, @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.NO_CONTENT, userContext -> {
            documentService.deleteDocument(id, getBranchId(servletRequest), userContext);
            return null;
        });
    }

    @POST
    @Path("workflows/documents/{handleId}/version")
    public Response createVersion(@PathParam("handleId") final String handleId,
                                  @Context final HttpServletRequest servletRequest,
                                  final Version version) {

        return executeTask(servletRequest, Status.NO_CONTENT, userContext -> {
            workflowService.executeDocumentWorkflowAction(handleId, "version", userContext.getSession(),
                    getBranchId(servletRequest), Optional.of(version));
            return null;
        });
    }

    @POST
    @Path("workflows/documents/{handleId}/{action}")
    public Response executeDocumentWorkflowAction(@PathParam("handleId") final String handleId,
                                                  @PathParam("action") final String action,
                                                  @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.NO_CONTENT, userContext -> {
            workflowService.executeDocumentWorkflowAction(handleId, action, userContext.getSession(),
                    getBranchId(servletRequest));
            return null;
        });
    }

    @POST
    @Path("workflows/documents/{documentId}/restore/{frozenNodeId}")
    public Response restoreDocumentWorkflowAction(@PathParam("documentId") final String documentId,
                                                  @PathParam("frozenNodeId") final String frozenNodeId,
                                                  @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Status.NO_CONTENT, userContext -> {
            workflowService.restoreDocumentWorkflowAction(documentId, frozenNodeId, userContext.getSession(),
                    getBranchId(servletRequest));
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
        final TimeZone timeZone = sessionRequestContextProvider.getTimeZone(servletRequest);
        final UserContext userContext = new UserContext(session, locale, timeZone);
        try {
            final Object result = task.execute(userContext);
            return Response.status(successStatus).cacheControl(cacheControl).entity(result).build();
        } catch (final ErrorWithPayloadException e) {
            return Response.status(e.getStatus()).cacheControl(cacheControl).entity(e.getPayload()).build();
        }
    }

    private String getBranchId(final HttpServletRequest servletRequest) {
        return branchSelectionService.getSelectedBranchId(contextPayloadService.apply(servletRequest));
    }

    @FunctionalInterface
    private interface EndPointTask {
        Object execute(UserContext userContext) throws ErrorWithPayloadException;
    }
}
