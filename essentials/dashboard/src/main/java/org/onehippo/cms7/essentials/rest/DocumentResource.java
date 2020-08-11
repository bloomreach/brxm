/*
 * Copyright 2014-2019 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.apache.cxf.rs.security.cors.CrossOriginResourceSharing;
import org.hippoecm.repository.api.HippoNode;
import org.onehippo.cms7.essentials.sdk.api.model.rest.ContentType;
import org.onehippo.cms7.essentials.sdk.api.model.rest.ContentTypeInstance;
import org.onehippo.cms7.essentials.sdk.api.model.rest.TemplateQuery;
import org.onehippo.cms7.essentials.sdk.api.service.ContentTypeService;
import org.onehippo.cms7.essentials.sdk.api.service.JcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;

@CrossOriginResourceSharing(allowAllOrigins = true)
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/documents/")
public class DocumentResource {

    private static final Logger log = LoggerFactory.getLogger(DocumentResource.class);
    private static final String QUERY_STATEMENT_QUERIES = "hippo:configuration/hippo:queries/hippo:templates//element(*, hippostd:templatequery)";

    private final JcrService jcrService;
    private final ContentTypeService contentTypeService;

    @Inject
    public DocumentResource(final JcrService jcrService, final ContentTypeService contentTypeService) {
        this.jcrService = jcrService;
        this.contentTypeService = contentTypeService;
    }

    @Operation(
            summary = "Fetches all project document types (including compounds)",
            responses = {@ApiResponse(content = @Content(schema = @Schema(implementation = List.class)))})
    @GET
    @Path("/")
    public List<ContentType> getAllTypes() {
        return contentTypeService.fetchContentTypesFromOwnNamespace();
    }

    @Operation(
            summary = "Fetches all project document types (compounds are *excluded*)",
            responses = {@ApiResponse(content = @Content(schema = @Schema(implementation = List.class)))})
    @GET
    @Path("/documents")
    public List<ContentType> getDocumentTypes() {
        return contentTypeService.fetchContentTypesFromOwnNamespace()
                .stream()
                .filter(type -> !type.isCompoundType())
                .collect(Collectors.toList());
    }

    @Operation(
            summary = "Fetches all project compound types",
            responses = {@ApiResponse(content = @Content(schema = @Schema(implementation = List.class)))})
    @GET
    @Path("/compounds")
    public List<ContentType> getCompounds() {
        return contentTypeService.fetchContentTypesFromOwnNamespace()
                .stream()
                .filter(ContentType::isCompoundType)
                .collect(Collectors.toList());
    }


    @Operation(
            summary = "Returns all documents of the specified type",
            description = "Specify the document type as {namespace}:{typename}.",
            responses = {@ApiResponse(content = @Content(schema = @Schema(implementation = List.class)))})
    @Parameter(name = "docType", description = "Document type", required = true)
    @GET
    @Path("/{docType}")
    public List<ContentTypeInstance> getDocumentsByType(@PathParam("docType") String docType) {
        final List<ContentTypeInstance> instances = new ArrayList<>();
        final Session session = jcrService.createSession();
        if (session != null) {
            try {
                final QueryManager queryManager = session.getWorkspace().getQueryManager();
                final Query xpath = queryManager.createQuery("//content/documents//element(*, " + docType + ')', "xpath");
                final NodeIterator nodes = xpath.execute().getNodes();
                while (nodes.hasNext()) {
                    Node node = nodes.nextNode();
                    final Node parent = node.getParent();
                    if (parent.isNodeType(NT_HANDLE)) {
                        node = parent;
                    }
                    final String path = node.getPath();
                    final String displayName = ((HippoNode) node).getDisplayName();
                    instances.add(new ContentTypeInstance(path, docType, displayName));
                }
            } catch (RepositoryException e) {
                log.debug("Error fetching value lists", e);
            } finally {
                jcrService.destroySession(session);
            }
        }
        return instances;
    }

    @Operation(
            summary = "Returns all document / folder query types",
            description = "No pairing is done (e.g.: news-folder + news-document combinations. This is left to users themselves)",
            responses = {@ApiResponse(content = @Content(schema = @Schema(implementation = List.class)))})
    @Parameter(name = "docType", description = "Document type", required = true)
    @GET
    @Path("/templatequeries")
    public List<TemplateQuery> getQueries() {
        final List<TemplateQuery> templateList = new ArrayList<>();
        final Session session = jcrService.createSession();
        if (session != null) {
            try {
                final QueryManager queryManager = session.getWorkspace().getQueryManager();
                final Query query = queryManager.createQuery(QUERY_STATEMENT_QUERIES, "xpath");
                final QueryResult result = query.execute();
                final NodeIterator nodes = result.getNodes();
                while (nodes.hasNext()) {
                    final Node node = nodes.nextNode();
                    templateList.add(new TemplateQuery(node.getName()));
                }
            } catch (RepositoryException e) {
                log.debug("Error fetching value lists", e);
            } finally {
                jcrService.destroySession(session);
            }
        }
        return templateList;
    }
}
