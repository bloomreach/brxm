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

package org.hippoecm.hst.jaxrs.contentrestapi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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

import com.fasterxml.jackson.annotation.JsonProperty;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.jaxrs.contentrestapi.visitors.DefaultVisitorFactory;
import org.hippoecm.hst.jaxrs.contentrestapi.visitors.Visitor;
import org.hippoecm.hst.jaxrs.contentrestapi.visitors.VisitorFactory;
import org.hippoecm.hst.util.SearchInputParsingUtils;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.onehippo.cms7.services.search.jcr.service.HippoJcrSearchService;
import org.onehippo.cms7.services.search.query.Query;
import org.onehippo.cms7.services.search.query.QueryUtils;
import org.onehippo.cms7.services.search.result.Hit;
import org.onehippo.cms7.services.search.result.HitIterator;
import org.onehippo.cms7.services.search.result.QueryResult;
import org.onehippo.cms7.services.search.service.SearchService;
import org.onehippo.cms7.services.search.service.SearchServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Produces("application/json")
public class ContentRestApiResource {

    private static final Logger log = LoggerFactory.getLogger(ContentRestApiResource.class);

    public static final String NAMESPACE_PREFIX = "hipporest";

    private interface Context {
        Session getSession() throws RepositoryException;
    }

    // TODO rename Context to ContextProvider
    private final Context context;
    private final List<String> ignoredVariantProperties = Collections.unmodifiableList(Arrays.asList("jcr:uuid"));

    public ContentRestApiResource() {
        this.context = new Context() {
            public Session getSession() throws RepositoryException {
                return RequestContextProvider.get().getSession();
            }
        };
    }

    ContentRestApiResource(Session session) {
        this.context = new Context() {
            public Session getSession() throws RepositoryException {
                return session;
            }
        };
    }

    private final class Link {
        @JsonProperty(NAMESPACE_PREFIX + ":url")
        public final String url;

        public Link(String url) {
            this.url = url;
        }
    }

    private final class SearchResultItem {
        @JsonProperty("jcr:name")
        public final String name;

        @JsonProperty("jcr:uuid")
        public final String uuid;

        @JsonProperty(NAMESPACE_PREFIX + ":links")
        public final Link[] links;

        public SearchResultItem(String name, String uuid, Link[] links) {
            this.name = name;
            this.uuid = uuid;
            this.links = links;
        }
    }

    private final class SearchResult {
        @JsonProperty(NAMESPACE_PREFIX + ":offset")
        public long offset;

        @JsonProperty(NAMESPACE_PREFIX + ":max")
        public long max;

        @JsonProperty(NAMESPACE_PREFIX + ":count")
        public long count;

        @JsonProperty(NAMESPACE_PREFIX + ":total")
        public long total;

        @JsonProperty(NAMESPACE_PREFIX + ":more")
        public boolean more;

        @JsonProperty(NAMESPACE_PREFIX + ":items")
        public SearchResultItem[] items;

        void initialize(int offset, int max, QueryResult queryResult, Session session) throws RepositoryException {
            final List<SearchResultItem> itemArrayList = new ArrayList<>();
            final HitIterator iterator = queryResult.getHits();
            while (iterator.hasNext()) {
                final Hit hit = iterator.nextHit();
                final String uuid = hit.getSearchDocument().getContentId().toIdentifier();
                final Node node = session.getNodeByIdentifier(uuid);
                // TODO link rewriting - use generic HST methods to construct URL
                final SearchResultItem item = new SearchResultItem(node.getName(), uuid,
                        new Link[] { new Link("http://localhost:8080/site/api/documents/" + uuid) });
                itemArrayList.add(item);
            }

            this.offset = offset;
            this.max = max;
            count = itemArrayList.size();
            total = queryResult.getTotalHitCount();
            more = (offset + count) < total;
            items = new SearchResultItem[(int)count];
            itemArrayList.toArray(items);
        }
    }

    private final class Error {
        public final int status;
        public final String description;
        Error(int status, String description) {
            this.status = status;
            this.description = description;
        }
    }

    private int parseMax(String maxString) throws IllegalArgumentException {
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

    private int parseOffset(String offsetString) throws IllegalArgumentException {
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
    public Response getDocuments(@QueryParam("_offset") String offsetString, @QueryParam("_max") String maxString, @QueryParam("_query") String queryString) {
        try {
            final int offset = parseOffset(offsetString);
            final int max = parseMax(maxString);
            final String parsedQuery = parseQuery(queryString);

            final SearchService searchService = getSearchService();
            final Query query = searchService.createQuery()
                    // TODO the 'from' should be from the current its mount its 'content path'
                    .from("/content/documents")
                            // TODO why not for now just hippo:document AND filter folders
                    .ofType("myhippoproject:basedocument") // TODO change to blacklisting mechanism
                    .where(QueryUtils.text().contains(parsedQuery == null ? "" : parsedQuery))
                    .returnParentNode()
                    .orderBy(HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE)
                    .descending()
                    .offsetBy(offset)
                    .limitTo(max);
            final QueryResult queryResult = searchService.search(query);
            final SearchResult returnValue = new SearchResult();
            returnValue.initialize(offset, max, queryResult, context.getSession());

            return Response.status(200).entity(returnValue).build();

        } catch (SearchServiceException e) {
            logException("Exception while fetching documents",e);
            if (e.getCause() instanceof InvalidQueryException) {
                return buildErrorResponse(400, e.toString());
            }
            return buildErrorResponse(500, e);
        } catch (IllegalArgumentException iae) {
            logException("Exception while fetching documents",iae);
            return buildErrorResponse(400, iae);
        } catch (Exception re) {
            logException("Exception while fetching documents",re);
            return buildErrorResponse(500, re);
        }
    }

    private void logException(final String message, final Exception e) {
        if (log.isDebugEnabled()) {
            log.info(message, e);
        } else {
            log.info(message + ": '{}'", e.toString());
        }
    }

    private UUID parseUUID(String uuid) throws IllegalArgumentException {
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("The string '" + uuid + "' is not a valid UUID");
        }
    }

    @GET
    @Path("/documents/{uuid}")
    public Response getDocumentsByUUID(@PathParam("uuid") String uuidString) {
        try {
            final Session session = context.getSession();

            // throws an IllegalArgumentException in case the uuid is not correctly formed
            final UUID uuid = parseUUID(uuidString);

            // throws an ItemNotFoundException in case the uuid does not exist or is not readable
            final Node node = session.getNodeByIdentifier(uuid.toString());

            // throws a PathNotFoundException in case there is no live variant or it is not readable
            node.getNode(node.getName());

            final Map<String, Object> response = new TreeMap<>();
            final VisitorFactory factory = new DefaultVisitorFactory();
            final Visitor visitor = factory.getVisitor(node);
            visitor.visit(node, response);

            return Response.status(200).entity(response).build();
        } catch (IllegalArgumentException iae) {
            return buildErrorResponse(400, iae);
        } catch (ItemNotFoundException|PathNotFoundException nfe) {
            return buildErrorResponse(404, nfe);
        } catch (RepositoryException re) {
            return buildErrorResponse(500, re);
        }
    }

    private Response buildErrorResponse(int status, Exception exception) {
        return buildErrorResponse(status, exception.toString());
    }

    private Response buildErrorResponse(int status, String description) {
        return Response.status(status).entity(new Error(status, description)).build();
    }

    private SearchService getSearchService() throws RepositoryException {
        final HippoJcrSearchService searchService = new HippoJcrSearchService();
        searchService.setSession(context.getSession());
        return searchService;
    }

}