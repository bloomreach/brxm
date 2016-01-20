/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.restapi.content;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.restapi.AbstractResource;
import org.hippoecm.hst.restapi.content.search.SearchResult;
import org.hippoecm.hst.restapi.content.visitors.NodeVisitor;
import org.hippoecm.hst.util.SearchInputParsingUtils;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.services.search.query.Query;
import org.onehippo.cms7.services.search.query.QueryUtils;
import org.onehippo.cms7.services.search.result.QueryResult;
import org.onehippo.cms7.services.search.service.SearchService;
import org.onehippo.cms7.services.search.service.SearchServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.NT_DOCUMENT;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;

@Produces("application/json")
public class DocumentsResource extends AbstractResource {

    private static final Logger log = LoggerFactory.getLogger(DocumentsResource.class);

    @Override
    public Logger getLogger() {
        return log;
    }

    private int parseMax(final String maxString) throws IllegalArgumentException {
        int max = 100;

        if (maxString != null) {
            try {
                max = Integer.parseInt(maxString);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("_max must be a number, greater than zero, it was: '" + maxString + "'");
            }
            if (max <= 0) {
                throw new IllegalArgumentException("_max must be greater than zero, it was: '" + maxString + "'");
            }
        }

        return max;
    }

    private int parseOffset(final String offsetString) throws IllegalArgumentException {
        int offset = 0;

        if (offsetString != null) {
            try {
                offset = Integer.parseInt(offsetString);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("_offset must be a number, greater than or equal to zero, it was: '" + offsetString + "'");
            }
            if (offset < 0) {
                throw new IllegalArgumentException("_offset must be greater than or equal to zero, it was: '" + offsetString + "'");
            }
        }

        return offset;
    }

    private String parseQuery(final String queryString) {
        return SearchInputParsingUtils.parse(queryString, true);
    }

    @GET
    @Path("/documents")
    public Response getDocuments(@QueryParam("_offset") final String offsetString,
                                 @QueryParam("_max") final String maxString,
                                 @QueryParam("_query") final String queryString) {
        try {
            ResourceContext context = getResourceContextFactory().createResourceContext();
            final int offset = parseOffset(offsetString);
            final int max = parseMax(maxString);
            final String parsedQuery = parseQuery(queryString);

            final String availability;
            if (RequestContextProvider.get().isPreview() ) {
                availability = "preview";
            } else {
                availability = "live";
            }
            final SearchService searchService = getSearchService(context);
            final Query query = searchService.createQuery()
                    .from(RequestContextProvider.get().getResolvedMount().getMount().getContentPath())
                    .ofType(HippoNodeType.NT_DOCUMENT)
                    .where(QueryUtils.text().contains(parsedQuery == null ? "" : parsedQuery))
                    .and(QueryUtils.text(HippoNodeType.HIPPO_AVAILABILITY).isEqualTo(availability))
                    .returnParentNode()
                    .orderBy(HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE)
                    .descending()
                    .offsetBy(offset)
                    .limitTo(max);
            final QueryResult queryResult = searchService.search(query);
            final SearchResult result = new SearchResult();
            result.populate(offset, max, queryResult, context.getRequestContext().getSession(), NT_HANDLE);

            return Response.status(200).entity(result).build();

        } catch (SearchServiceException sse) {
            logException("Exception while fetching documents", sse);
            if (sse.getCause() instanceof InvalidQueryException) {
                return buildErrorResponse(400, sse.toString());
            }
            return buildErrorResponse(500, sse);
        } catch (IllegalArgumentException iae) {
            logException("Exception while fetching documents", iae);
            return buildErrorResponse(400, iae);
        } catch (Exception e) {
            logException("Exception while fetching documents", e);
            return buildErrorResponse(500, e);
        }
    }

    @GET
    @Path("/documents/{uuid}")
    public Response getDocumentsByUUID(@PathParam("uuid") final String uuidString) {
        try {
            final ResourceContext context = getResourceContextFactory().createResourceContext();
            final Session session = context.getRequestContext().getSession();

            // throws an IllegalArgumentException in case the uuid is not correctly formed
            final UUID uuid = parseUUID(uuidString);

            // throws an ItemNotFoundException in case the uuid does not exist or is not readable
            final Node node = session.getNodeByIdentifier(uuid.toString());

            if (!isNodePartOfApiContent(context, node)) {
                // documents not within context of the mount content path "don't exist"
                throw new ItemNotFoundException(String.format("Item '%s' not found below scope '%s'",  uuidString,
                        context.getRequestContext().getResolvedMount().getMount().getMountPath()));
            }

            if (!node.isNodeType(NT_HANDLE)) {
                throw new ItemNotFoundException(String.format("Item '%s' not found below scope '%s'",  uuidString,
                        context.getRequestContext().getResolvedMount().getMount().getMountPath()));
            }

            // throws a PathNotFoundException in case there is no live variant or it is not readable
            final Node doc = node.getNode(node.getName());
            if (!doc.isNodeType(NT_DOCUMENT)) {
                throw new ItemNotFoundException(String.format("Item '%s' not found below scope '%s'",  uuidString,
                        context.getRequestContext().getResolvedMount().getMount().getMountPath()));
            }

            final Map<String, Object> response = new LinkedHashMap<>();

            final NodeVisitor visitor = context.getVisitor(node);
            visitor.visit(context, node, response);

            return Response.status(200).entity(response).build();

        } catch (IllegalArgumentException iae) {
            return buildErrorResponse(400, iae);
        } catch (ItemNotFoundException|PathNotFoundException nfe) {
            return buildErrorResponse(404, nfe);
        } catch (RepositoryException re) {
            return buildErrorResponse(500, re);
        }
    }

}