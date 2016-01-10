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
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.onehippo.cms7.services.search.jcr.service.HippoJcrSearchService;
import org.onehippo.cms7.services.search.query.Query;
import org.onehippo.cms7.services.search.result.Hit;
import org.onehippo.cms7.services.search.result.HitIterator;
import org.onehippo.cms7.services.search.result.QueryResult;
import org.onehippo.cms7.services.search.service.SearchService;

@Produces("application/json")
public class ContentRestApiResource {

    public static final String NAMESPACE_PREFIX = "hipporest";

    private interface Context {
        Session getSession() throws RepositoryException;
    }

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
        public String url;

        public Link(String url) {
            this.url = url;
        }
    }

    private final class SearchResultItem {
        @JsonProperty("jcr:name")
        public String name;

        @JsonProperty("jcr:uuid")
        public String uuid;

        @JsonProperty(NAMESPACE_PREFIX + ":links")
        public Link[] links;

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
            ArrayList<SearchResultItem> itemArrayList = new ArrayList<>();
            HitIterator iterator = queryResult.getHits();
            while (iterator.hasNext()) {
                Hit hit = iterator.nextHit();
                String uuid = hit.getSearchDocument().getContentId().toIdentifier();
                Node node = session.getNodeByIdentifier(uuid);
                SearchResultItem item = new SearchResultItem(node.getName(), uuid,
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
        public int status;
        public String description;
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

    @GET
    @Path("/documents")
    public Response getDocuments(@QueryParam("_offset") String offsetString, @QueryParam("_max") String maxString) {
        try {
            int offset = parseOffset(offsetString);
            int max = parseMax(maxString);

            SearchService searchService = getSearchService();
            Query query = searchService.createQuery()
                    .from("/content/documents")
                    .ofType("myhippoproject:basedocument") // TODO change to blacklisting mechanism
                    .returnParentNode()
                    .orderBy(HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE)
                    .descending()
                    .offsetBy(offset)
                    .limitTo(max);
            QueryResult queryResult = searchService.search(query);
            SearchResult returnValue = new SearchResult();
            returnValue.initialize(offset, max, queryResult, context.getSession());

            return Response.status(200).entity(returnValue).build();
        } catch (IllegalArgumentException iae) {
            return buildErrorResponse(400, iae);
        } catch (RepositoryException re) {
            return buildErrorResponse(500, re);
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
            Session session = context.getSession();

            // throws an IllegalArgumentException in case the uuid is not correctly formed
            UUID uuid = parseUUID(uuidString);

            // throws an ItemNotFoundException in case the uuid does not exist or is not readable
            Node node = session.getNodeByIdentifier(uuid.toString());

            // throws a PathNotFoundException in case there is no live variant or it is not readable
            node.getNode(node.getName());

            Map response = new TreeMap<>();
            VisitorFactory factory = new DefaultVisitorFactory();
            Visitor visitor = factory.getVisitor(node);
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